package maweituo.tests.services

import scala.util.control.NoStackTrace

import cats.data.OptionT
import cats.effect.IO

import maweituo.domain.ads.images.{Image, ImageId}
import maweituo.domain.ads.repos.AdImageRepo
import maweituo.domain.ads.services.*
import maweituo.domain.errors.{AdModificationForbidden, InvalidImageId}
import maweituo.domain.services.*
import maweituo.domain.users.services.*
import maweituo.infrastructure.inmemory.InMemoryObjectStorage
import maweituo.interp.*
import maweituo.interp.ads.{AdImageServiceInterp, AdServiceInterp}
import maweituo.interp.users.UserServiceInterp
import maweituo.tests.generators.{createAdRequestGen, imageContentsGen, imageIdGen, registerGen, userIdGen}
import maweituo.tests.repos.*
import maweituo.tests.repos.inmemory.*
import maweituo.tests.services.stubs.TelemetryServiceStub

import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object ImageServiceSuite extends SimpleIOSuite with Checkers:

  given Logger[IO]           = NoOpLogger[IO]
  given TelemetryService[IO] = new TelemetryServiceStub[IO]

  private def makeTestServices(imageRepo: AdImageRepo[IO] = InMemoryRepoFactory.images)
      : (UserService[IO], AdService[IO], AdImageService[IO]) =
    val userRepo         = InMemoryRepoFactory.users
    val adRepo           = InMemoryRepoFactory.ads
    val os               = new InMemoryObjectStorage
    given IAMService[IO] = makeIAMService(adRepo, imageRepo)
    val users            = UserServiceInterp.make(userRepo)
    val ads              = AdServiceInterp.make(adRepo, RepoStubFactory.feed)
    val images           = AdImageServiceInterp.make(imageRepo, adRepo, os)
    (users, ads, images)

  test("create works") {
    val (users, ads, images) = makeTestServices()
    val gen =
      for
        reg <- registerGen
        ad  <- createAdRequestGen
        img <- imageContentsGen[IO]
      yield (reg, ad, img)
    forall(gen) { case (reg, createAd, imgCont) =>
      for
        u    <- users.create(reg)
        a    <- ads.create(u, createAd)
        i    <- images.upload(u, a, imgCont)
        data <- images.get(i)
        d1   <- data.data.compile.toVector
        d2   <- imgCont.data.compile.toVector
      yield expect.same(d1, d2)
    }
  }

  test("create internal error") {
    case class TestError() extends NoStackTrace
    class ImageRepo extends InMemoryAdImageRepo[IO]:
      override def create(image: Image) = IO.raiseError(TestError())
    val (users, ads, images) = makeTestServices(new ImageRepo)
    val gen =
      for
        reg <- registerGen
        ad  <- createAdRequestGen
        img <- imageContentsGen[IO]
      yield (reg, ad, img)
    forall(gen) { case (reg, createAd, imgCont) =>
      for
        u <- users.create(reg)
        a <- ads.create(u, createAd)
        x <- images.upload(u, a, imgCont).attempt
      yield expect.same(Left(TestError()), x)
    }
  }

  test("get invalid image") {
    val (_, _, images) = makeTestServices()
    forall(imageIdGen) { id =>
      for
        x <- images.get(id).attempt
      yield expect.same(Left(InvalidImageId(id)), x)
    }
  }

  test("get internal error") {
    case class TestError() extends NoStackTrace
    class ImageRepo extends InMemoryAdImageRepo[IO]:
      override def find(id: ImageId) = OptionT(IO.raiseError(TestError()))
    val (_, _, images) = makeTestServices(new ImageRepo)
    forall(imageIdGen) { id =>
      for
        x <- images.get(id).attempt
      yield expect.same(Left(TestError()), x)
    }
  }

  test("delete works") {
    val (users, ads, images) = makeTestServices()
    val gen =
      for
        reg <- registerGen
        ad  <- createAdRequestGen
        img <- imageContentsGen[IO]
      yield (reg, ad, img)
    forall(gen) { case (reg, createAd, imgCont) =>
      for
        u <- users.create(reg)
        a <- ads.create(u, createAd)
        i <- images.upload(u, a, imgCont)
        _ <- images.delete(i, u)
        x <- images.get(i).attempt
      yield expect.same(Left(InvalidImageId(i)), x)
    }
  }

  test("delete internal error") {
    case class TestError() extends NoStackTrace
    class ImageRepo extends InMemoryAdImageRepo[IO]:
      override def delete(id: ImageId) = IO.raiseError(TestError())
    val (_, _, images) = makeTestServices(new ImageRepo)
    val gen =
      for
        i <- imageIdGen
        u <- userIdGen
      yield i -> u
    forall(gen) { (id, uid) =>
      for
        x <- images.delete(id, uid).attempt
      yield expect.same(Left(InvalidImageId(id)), x)
    }
  }

  test("delete by not author is forbidden") {
    val (users, ads, images) = makeTestServices()
    val gen =
      for
        reg  <- registerGen
        reg1 <- registerGen
        ad   <- createAdRequestGen
        img  <- imageContentsGen[IO]
      yield (reg, reg1, ad, img)
    forall(gen) { case (reg, reg1, createAd, imgCont) =>
      for
        u  <- users.create(reg)
        u1 <- users.create(reg1)
        a  <- ads.create(u, createAd)
        i  <- images.upload(u, a, imgCont)
        x  <- images.delete(i, u1).attempt
      yield expect.same(Left(AdModificationForbidden(a, u1)), x)
    }
  }
