package maweituo
package tests
package it
package postgres
package repos

import maweituo.domain.all.*

import weaver.GlobalRead

class PostgresAdImageRepoITSuite(global: GlobalRead) extends PostgresITSuite(global):

  private def imgTest(name: String)(fn: (UserRepo[IO], AdRepo[IO], AdImageRepo[IO]) => F[Expectations]) =
    pgTest(name) { postgres =>
      val users  = PostgresUserRepo.make(postgres)
      val ads    = PostgresAdRepo.make(postgres)
      val images = PostgresAdImageRepo.make(postgres)
      fn(users, ads, images)
    }

  private val gen =
    for
      u   <- userGen
      ad0 <- adGen
      ad = ad0.copy(authorId = u.id)
      img <- imageGen(ad.id)
    yield (u, ad, img)

  imgTest("create and find") { (users, ads, images) =>
    forall(gen) { (u, ad, img) =>
      for
        _ <- users.create(u)
        _ <- ads.create(ad)
        _ <- images.create(img)
        x <- images.find(img.id).value
      yield expect.same(Some(img), x)
    }
  }

  imgTest("create and find by ad") { (users, ads, images) =>
    forall(gen) { (u, ad, img) =>
      for
        _ <- users.create(u)
        _ <- ads.create(ad)
        _ <- images.create(img)
        x <- images.findIdsByAd(img.adId)
      yield expect.same(List(img.id), x)
    }
  }

  imgTest("delete") { (users, ads, images) =>
    forall(gen) { (u, ad, img) =>
      for
        _ <- users.create(u)
        _ <- ads.create(ad)
        _ <- images.create(img)
        _ <- images.delete(img.id)
        x <- images.find(img.id).value
      yield expect.same(None, x)
    }
  }
