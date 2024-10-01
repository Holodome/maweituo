package maweituo.tests.services

import java.util.UUID

import maweituo.domain.errors.*
import maweituo.domain.services.IAMService
import maweituo.domain.users.UserId
import maweituo.domain.users.services.UserService
import maweituo.interpreters.*
import maweituo.interpreters.users.UserServiceInterpreter
import maweituo.tests.generators.{registerGen, updateUserGen, userIdGen}
import maweituo.tests.repos.*
import maweituo.tests.repos.inmemory.InMemoryRepositoryFactory
import maweituo.tests.services.makeIAMService

import cats.data.NonEmptyList
import cats.effect.IO
import cats.syntax.all.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers
import maweituo.domain.users.repos.UserRepository
import maweituo.domain.users.User
import scala.util.control.NoStackTrace
import cats.data.OptionT
import maweituo.domain.users.Username
import maweituo.domain.users.Email
import maweituo.tests.repos.inmemory.InMemoryUserRepository
import maweituo.domain.users.UpdateUserInternal

object UserServiceSuite extends SimpleIOSuite with Checkers:

  given Logger[IO]     = NoOpLogger[IO]
  given IAMService[IO] = makeIAMService

  private def makeTestUsers(repo: UserRepository[IO] = InMemoryRepositoryFactory.users): UserService[IO] =
    UserServiceInterpreter.make(repo)

  test("register user works") {
    val users = makeTestUsers()
    forall(registerGen) { register =>
      for
        _ <- users.create(register)
      yield success
    }
  }

  test("create internal error") {
    case class TestError() extends NoStackTrace
    val repo = new TestUserRepository:
      override def create(request: User): IO[Unit]               = IO.raiseError(TestError())
      override def findByEmail(emai: Email): OptionT[IO, User]   = OptionT(IO.raiseError(TestError()))
      override def findByName(name: Username): OptionT[IO, User] = OptionT(IO.raiseError(TestError()))
    val users = makeTestUsers(repo)
    forall(registerGen) { register =>
      for
        x <- users.create(register).attempt
      yield expect.same(Left(TestError()), x)
    }
  }

  test("can't register user with same email") {
    val users = makeTestUsers()
    val gen =
      for
        r1 <- registerGen
        r2 <- registerGen.map(_.copy(email = r1.email))
      yield r1 -> r2
    forall(gen) { (r1, r2) =>
      for
        _ <- users.create(r1)
        x <- users.create(r2).attempt
      yield expect.same(Left(UserEmailInUse(r1.email)), x)
    }
  }

  test("can't register user with same name") {
    val users = makeTestUsers()
    val gen =
      for
        r1 <- registerGen
        r2 <- registerGen.map(_.copy(name = r1.name))
      yield r1 -> r2
    forall(gen) { (r1, r2) =>
      for
        _ <- users.create(r1)
        x <- users.create(r2).attempt
      yield expect.same(Left(UserNameInUse(r1.name)), x)
    }
  }

  test("register and find") {
    val users = makeTestUsers()
    forall(registerGen) { register =>
      for
        id <- users.create(register)
        u  <- users.get(id)
      yield expect.same(id, u.id)
    }
  }

  test("get internal error") {
    case class TestError() extends NoStackTrace
    class UserRepo extends InMemoryUserRepository[IO]:
      override def find(id: UserId): OptionT[IO, User] = OptionT(IO.raiseError(TestError()))
    val users = makeTestUsers(new UserRepo)
    forall(registerGen) { register =>
      for
        id <- users.create(register)
        x  <- users.get(id).attempt
      yield expect.same(Left(TestError()), x)
    }
  }

  test("register and find by name work") {
    val users = makeTestUsers()
    forall(registerGen) { register =>
      for
        id <- users.create(register)
        u  <- users.getByName(register.name)
      yield expect.same(id, u.id)
    }
  }

  test("get by name internal error") {
    case class TestError() extends NoStackTrace
    class UserRepo extends InMemoryUserRepository[IO]:
      override def findByName(name: Username): OptionT[IO, User] = OptionT(IO.raiseError(TestError()))
    val users = makeTestUsers(new UserRepo)
    forall(registerGen) { register =>
      for
        x <- users.getByName(register.name).attempt
      yield expect.same(Left(TestError()), x)
    }
  }

  test("register and find by email work") {
    val users = makeTestUsers()
    forall(registerGen) { register =>
      for
        id <- users.create(register)
        u  <- users.getByEmail(register.email)
      yield expect.same(id, u.id)
    }
  }

  test("get by email internal error") {
    case class TestError() extends NoStackTrace
    class UserRepo extends InMemoryUserRepository[IO]:
      override def findByEmail(email: Email): OptionT[IO, User] = OptionT(IO.raiseError(TestError()))
    val users = makeTestUsers(new UserRepo)
    forall(registerGen) { register =>
      for
        x <- users.getByEmail(register.email).attempt
      yield expect.same(Left(TestError()), x)
    }
  }

  test("register and delete work") {
    val users = makeTestUsers()
    forall(registerGen) { register =>
      for
        id    <- users.create(register)
        _     <- users.delete(id, id)
        found <- users.get(id).attempt
      yield expect.same(Left(InvalidUserId(id)), found)
    }
  }

  test("user delete by other person is forbidden") {
    val users = makeTestUsers()
    val gen =
      for
        r     <- registerGen
        other <- registerGen
      yield r -> other
    forall(gen) { case (register, other) =>
      for
        newId   <- users.create(register)
        otherId <- users.create(other)
        x       <- users.delete(newId, otherId).attempt
        u       <- users.get(newId)
      yield NonEmptyList
        .of(
          expect.same(Left(UserModificationForbidden(otherId)), x),
          expect.same(newId, u.id),
          expect.same(register.name, u.name),
          expect.same(register.email, u.email)
        ).reduce
    }
  }

  test("delete internal error") {
    case class TestError() extends NoStackTrace
    class UserRepo extends InMemoryUserRepository[IO]:
      override def delete(id: UserId): IO[Unit] = IO.raiseError(TestError())
    val users = makeTestUsers(new UserRepo)
    forall(userIdGen) { id =>
      for
        x <- users.delete(id, id).attempt
      yield expect.same(Left(TestError()), x)
    }
  }

  test("user update works") {
    val users = makeTestUsers()
    val gen =
      for
        r   <- registerGen
        upd <- updateUserGen(UserId(UUID.randomUUID()))
      yield (r, upd)
    forall(gen) { case (register, upd) =>
      for
        newId <- users.create(register)
        newUpd = upd.copy(id = newId)
        prior   <- users.get(newId)
        _       <- users.update(newUpd, newId)
        updated <- users.get(newId)
      yield expect.all(
        newUpd.email.fold(true)(_ === updated.email),
        newUpd.name.fold(true)(_ === updated.name),
        newUpd.password.fold(true)(_ => prior.hashedPassword =!= updated.hashedPassword)
      )
    }
  }

  test("user update by other person is forbidden") {
    val users = makeTestUsers()
    val gen =
      for
        r   <- registerGen
        upd <- updateUserGen(UserId(UUID.randomUUID()))
        id  <- userIdGen
      yield (r, upd, id)
    forall(gen) { case (register, upd, id) =>
      for
        newId <- users.create(register)
        newUpd = upd.copy(id = newId)
        prior <- users.get(newId)
        x     <- users.update(newUpd, id).attempt
        got   <- users.get(newId)
      yield NonEmptyList.of(
        expect.same(Left(UserModificationForbidden(id)), x),
        expect.same(prior.hashedPassword, got.hashedPassword),
        expect.same(prior.name, got.name),
        expect.same(prior.email, got.email)
      ).reduce
    }
  }

  test("update internal error") {
    case class TestError() extends NoStackTrace
    class UserRepo extends InMemoryUserRepository[IO]:
      override def update(update: UpdateUserInternal): IO[Unit] = IO.raiseError(TestError())
    val users = makeTestUsers(new UserRepo)
    val gen =
      for
        reg <- registerGen
        upd <- updateUserGen(UserId(UUID.randomUUID()))
      yield reg -> upd
    forall(gen) { (reg, upd0) =>
      for
        uid <- users.create(reg)
        upd = upd0.copy(id = uid)
        x <- users.update(upd, upd.id).attempt
      yield expect.same(Left(TestError()), x)
    }
  }
