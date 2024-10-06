package maweituo.tests.properties.services

import cats.data.NonEmptyList
import cats.effect.IO
import cats.syntax.all.*

import maweituo.domain.Identity
import maweituo.domain.errors.{InvalidUserId, UserEmailInUse, UserModificationForbidden, UserNameInUse}
import maweituo.domain.users.UserId
import maweituo.domain.users.services.UserService
import maweituo.tests.generators.{registerGen, updateUserGen, userIdGen}

import weaver.scalacheck.Checkers
import weaver.{Expectations, MutableIOSuite}

trait UserServiceProperties:
  this: MutableIOSuite & Checkers =>

  protected final case class Property(
      name: String,
      exp: UserService[IO] => IO[Expectations]
  )

  protected val properties = List(
    Property(
      "register user works",
      users =>
        forall(registerGen) { register =>
          for
            _ <- users.create(register)
          yield success
        }
    ),
    Property(
      "can't register user with same email",
      users =>
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
    ),
    Property(
      "can't register user with same name",
      users =>
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
    ),
    Property(
      "register and find",
      users =>
        forall(registerGen) { register =>
          for
            id <- users.create(register)
            u  <- users.get(id)
          yield expect.same(id, u.id)
        }
    ),
    Property(
      "register and find by name work",
      users =>
        forall(registerGen) { register =>
          for
            id <- users.create(register)
            u  <- users.getByName(register.name)
          yield expect.same(id, u.id)
        }
    ),
    Property(
      "register and find by email work",
      users =>
        forall(registerGen) { register =>
          for
            id <- users.create(register)
            u  <- users.getByEmail(register.email)
          yield expect.same(id, u.id)
        }
    ),
    Property(
      "register and delete work",
      users =>
        forall(registerGen) { register =>
          for
            id    <- users.create(register)
            _     <- users.delete(id)(using Identity(id))
            found <- users.get(id).attempt
          yield expect.same(Left(InvalidUserId(id)), found)
        }
    ),
    Property(
      "user delete by other person is forbidden",
      users =>
        val gen =
          for
            r     <- registerGen
            other <- registerGen
          yield r -> other
        forall(gen) { case (register, other) =>
          for
            newId   <- users.create(register)
            otherId <- users.create(other)
            x       <- users.delete(newId)(using Identity(otherId)).attempt
            u       <- users.get(newId)
          yield NonEmptyList
            .of(
              expect.same(Left(UserModificationForbidden(otherId)), x),
              expect.same(newId, u.id),
              expect.same(register.name, u.name),
              expect.same(register.email, u.email)
            ).reduce
        }
    ),
    Property(
      "user update works",
      users =>
        val gen =
          for
            r     <- registerGen
            other <- userIdGen
            upd   <- updateUserGen(other)
          yield (r, upd)
        forall(gen) { case (register, upd) =>
          for
            newId <- users.create(register)
            newUpd = upd.copy(id = newId)
            prior   <- users.get(newId)
            _       <- users.update(newUpd)(using Identity(newId))
            updated <- users.get(newId)
          yield expect.all(
            newUpd.email.fold(true)(_ === updated.email),
            newUpd.name.fold(true)(_ === updated.name),
            newUpd.password.fold(true)(_ => prior.hashedPassword =!= updated.hashedPassword)
          )
        }
    ),
    Property(
      "user update by other person is forbidden",
      users =>
        val gen =
          for
            r     <- registerGen
            other <- userIdGen
            upd   <- updateUserGen(other)
            id    <- userIdGen
          yield (r, upd, id)
        forall(gen) { case (register, upd, id) =>
          for
            newId <- users.create(register)
            newUpd = upd.copy(id = newId)
            prior <- users.get(newId)
            x     <- users.update(newUpd)(using Identity(id)).attempt
            got   <- users.get(newId)
          yield NonEmptyList.of(
            expect.same(Left(UserModificationForbidden(id)), x),
            expect.same(prior.hashedPassword, got.hashedPassword),
            expect.same(prior.name, got.name),
            expect.same(prior.email, got.email)
          ).reduce
        }
    )
  )
