package maweituo.fa2

import cats.data.EitherT
import cats.derived.derived
import cats.effect.{Concurrent, IO, IOApp}
import cats.implicits.*
import cats.{MonadThrow, Show}

import io.circe.Codec
import com.comcast.ip4s.*
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.given
import org.http4s.circe.{CirceEntityCodec, JsonDecoder}
import org.http4s.dsl.Http4sDsl
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory
import cats.effect.kernel.Sync
import scala.collection.concurrent.TrieMap
import cats.data.OptionT
import java.util.UUID
import scala.util.control.NoStackTrace
import javax.mail.internet.MimeMessage
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.Message
import javax.mail.Transport
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.HttpApp
import org.http4s.server.middleware.RequestLogger
import org.http4s.server.middleware.ResponseLogger

enum AppError extends NoStackTrace derives Show:
  case InvalidEmail(email: String)
  case InvalidPassword(email: String)
  case VerificationNotFound(email: String)
  case InvalidVerificationCode(email: String)
end AppError

type AppF[F[_], A] = EitherT[F, AppError, A]

final case class LoginDTO(email: String, password: String) derives Codec.AsObject
final case class VerifyLoginDTO(email: String, code: String) derives Codec.AsObject
final case class ResetPasswordDTO(email: String, oldPassword: String) derives Codec.AsObject
final case class VerifyResetPasswordDTO(email: String, code: String, newPassword: String) derives Codec.AsObject

trait AppService[F[_]]:
  def login(dto: LoginDTO): AppF[F, Unit]
  def verifyLogin(dto: VerifyLoginDTO): AppF[F, Unit]
  def resetPassword(dto: ResetPasswordDTO): AppF[F, Unit]
  def verifyResetPassword(dto: VerifyResetPasswordDTO): AppF[F, Unit]
end AppService

trait UserRepo[F[_]]:
  def add(email: String, password: String): F[Unit]
  def find(email: String): OptionT[F, User]
  def updatePassword(email: String, newPassword: String): F[Unit]
end UserRepo

trait CodeRepo[F[_]]:
  def put(email: String, code: String): F[Unit]
  def get(email: String): OptionT[F, String]
  def rem(email: String): F[Unit]
end CodeRepo

trait Fa2[F[_]]:
  def send(code: String, to: String): F[Unit]
end Fa2

final case class AppRoutes[F[_]: MonadThrow: JsonDecoder: Concurrent](service: AppService[F])
    extends Http4sDsl[F]:
  val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "login" =>
      req.decode[LoginDTO] { dat =>
        service.login(dat).foldF(e => BadRequest(e.show), _ => Ok("Verification code sent to email"))
      }
    case req @ POST -> Root / "login_verify" =>
      req.decode[VerifyLoginDTO] { dat =>
        service.verifyLogin(dat).foldF(e => BadRequest(e.show), _ => Ok("Auth success!"))
      }
    case req @ POST -> Root / "reset_password" =>
      req.decode[ResetPasswordDTO] { dat =>
        service.resetPassword(dat).foldF(e => BadRequest(e.show), _ => Ok("Password reset code sent to email"))
      }
    case req @ POST -> Root / "verify_reset_password" =>
      req.decode[VerifyResetPasswordDTO] { dat =>
        service.verifyResetPassword(dat).foldF(e => BadRequest(e.show), _ => Ok("Password changed successfully!"))
      }
  }
end AppRoutes

final case class User(email: String, password: String)

class InMemoryUserRepo[F[_]: Sync] extends UserRepo[F]:
  private val map = new TrieMap[String, User]

  def add(email: String, password: String): F[Unit]   = Sync[F] delay map.addOne(email -> User(email, password))
  def find(email: String): OptionT[F, User]           = OptionT(Sync[F] delay map.get(email))
  def updatePassword(email: String, password: String) = add(email, password)

end InMemoryUserRepo

class InMemoryCodeRepo[F[_]: Sync] extends CodeRepo[F]:
  private val map = new TrieMap[String, String]

  def put(email: String, code: String): F[Unit] = Sync[F] delay map.addOne(email -> code)
  def get(email: String): OptionT[F, String]    = OptionT(Sync[F] delay map.get(email))
  def rem(email: String): F[Unit]               = Sync[F] delay map.remove(email)
end InMemoryCodeRepo

object AppServiceInterp:
  def make[F[_]: Sync: MonadThrow](repo: UserRepo[F], codeRepo: CodeRepo[F], fa2: Fa2[F]): AppService[F] = new:
    private def generateCode: F[String] = Sync[F].delay(UUID.randomUUID()).map(_.toString)

    def login(dto: LoginDTO): AppF[F, Unit] =
      EitherT.fromOptionF(repo.find(dto.email).value, AppError.InvalidEmail(dto.email))
        .flatMap {
          user =>
            if dto.password === user.password then
              EitherT.liftF(for
                code <- if sys.env("TEST_PASSWORD") != "" then Sync[F].pure("123") else generateCode
                _    <- codeRepo.put(dto.email, code)
                _    <- fa2.send(code, dto.email)
              yield ())
            else
              EitherT.leftT(AppError.InvalidPassword(dto.email))
        }

    def verifyLogin(dto: VerifyLoginDTO): AppF[F, Unit] =
      EitherT.fromOptionF(codeRepo.get(dto.email).value, AppError.VerificationNotFound(dto.email))
        .flatMap { code =>
          if code === dto.code then
            EitherT.liftF(codeRepo.rem(dto.email))
          else
            EitherT.leftT(AppError.InvalidVerificationCode(dto.code))
        }

    def resetPassword(dto: ResetPasswordDTO): AppF[F, Unit] =
      EitherT.fromOptionF(repo.find(dto.email).value, AppError.InvalidEmail(dto.email))
        .flatMap {
          user =>
            if user.password === dto.oldPassword then
              EitherT.liftF(
                for
                  code <- if sys.env("TEST_PASSWORD") != "" then Sync[F].pure("123") else generateCode
                  _    <- codeRepo.put(dto.email, code)
                  _    <- fa2.send(code, dto.email)
                yield ()
              )
            else EitherT.leftT(AppError.InvalidPassword(dto.email))
        }

    def verifyResetPassword(dto: VerifyResetPasswordDTO): AppF[F, Unit] =
      EitherT.fromOptionF(codeRepo.get(dto.email).value, AppError.VerificationNotFound(dto.email))
        .flatMap { code =>
          if code === dto.code then
            EitherT.liftF(codeRepo.rem(dto.email) *> repo.updatePassword(dto.email, dto.newPassword))
          else
            EitherT.leftT(AppError.InvalidVerificationCode(dto.code))
        }
end AppServiceInterp

class JavaMailFA2[F[_]: Sync: MonadThrow](session: Session) extends Fa2[F]:
  private def doSend(code: String, to: String) =
    val from    = "me@me.com";
    val subject = "Hello";
    val msg     = new MimeMessage(session);
    msg.setFrom(new InternetAddress(from));
    msg.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
    msg.setSubject(subject);
    msg.setText(code);
    Transport.send(msg);

  def send(code: String, to: String): F[Unit] =
    Sync[F] delay doSend(code, to)

end JavaMailFA2

object Main extends IOApp.Simple:

  given LoggerFactory[IO] = Slf4jFactory.create[IO]
  LoggerFactory[IO].getLogger

  private val loggers: HttpApp[IO] => HttpApp[IO] = { (http: HttpApp[IO]) =>
    RequestLogger.httpApp(logHeaders = true, logBody = true)(http)
  } andThen { (http: HttpApp[IO]) =>
    ResponseLogger.httpApp(logHeaders = true, logBody = true)(http)
  }

  override def run: IO[Unit] =
    val props = new java.util.Properties()
    props.put("mail.smtp.host", "localhost")
    props.put("mail.smtp.port", "8025")
    val session = Session.getDefaultInstance(props, null)
    val mail    = JavaMailFA2[IO](session)
    val repo    = InMemoryUserRepo[IO]()
    IO.pure(repo).flatTap { repo =>
      repo.add("admin@admin.com", "admin")
    }.flatMap { repo =>
      val codeRepo = InMemoryCodeRepo[IO]()
      val service  = AppServiceInterp.make[IO](repo, codeRepo, mail)
      val routes   = AppRoutes[IO](service).routes
      val server = EmberServerBuilder
        .default[IO]
        .withHost(ipv4"0.0.0.0")
        .withPort(port"8080")
        .withHttpApp(loggers(routes.orNotFound))
        .build
      server.useForever
    }

end Main
