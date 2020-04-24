package ru.dgolubets.di4cats

import cats.effect.{IO, Resource}
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class DependencySpec extends AnyWordSpec with Matchers with MockFactory {
  "Dependency" should {

    "be lazy" in {
      val dependencyFactory = mockFunction[String]("dependencyFactory")

      implicit val dependency: Dependency[IO, String] = Dependency {
        dependencyFactory()
      }

      dependencyFactory.expects()
      dependency.use(_ => IO()).unsafeRunSync()
    }

    "convert implicitly" in {
      implicit val strDep: String = "test"

      implicitly[Dependency[IO, String]].use(IO(_)).unsafeRunSync() shouldBe strDep
    }

    "use singletons" in {

      case class A(value: Int)
      case class B(a: A)
      case class C(a: A)
      case class D(b: B, c: C)

      implicit val depA: Dependency[IO, A] = Dependency {
        A(42)
      }

      implicit val depB: Dependency[IO, B] = depA.map { a =>
        B(a)
      }

      implicit val depC: Dependency[IO, C] = depA.map { a =>
        C(a)
      }

      implicit val depD: Dependency[IO, D] = depB.flatMap { b =>
        depC.map { c =>
          D(b, c)
        }
      }

      val d = depD.use(IO(_)).unsafeRunSync()

      assert(d.b.a eq d.c.a, "dependency A should be the same reference")
    }

    "manage resources" in {

      case class A(value: Int)
      case class B(a: A)
      case class C(a: A)
      case class D(b: B, c: C)

      val acquireA = mockFunction[A]("acquireA")
      val releaseA = mockFunction[Unit]("releaseA")
      val acquireB = mockFunction[B]("acquireB")
      val releaseB = mockFunction[Unit]("releaseB")
      val acquireC = mockFunction[C]("acquireC")
      val releaseC = mockFunction[Unit]("releaseC")
      val acquireD = mockFunction[D]("acquireD")
      val releaseD = mockFunction[Unit]("releaseD")

      implicit val depA: Dependency[IO, A] = Dependency.managed {
        Resource.make(IO(acquireA()))(_ => IO(releaseA()))
      }

      implicit val depB: Dependency[IO, B] = depA.flatMap { _ =>
        Dependency.managed {
          Resource.make(IO(acquireB()))(_ => IO(releaseB()))
        }
      }

      implicit val depC: Dependency[IO, C] = depA.flatMap { _ =>
        Dependency.managed {
          Resource.make(IO(acquireC()))(_ => IO(releaseC()))
        }
      }

      implicit val depD: Dependency[IO, D] = depB.flatMap { _ =>
        depC.flatMap { _ =>
          Dependency.managed {
            Resource.make(IO(acquireD()))(_ => IO(releaseD()))
          }
        }
      }

      acquireA.expects().once()
      acquireB.expects().once()
      acquireC.expects().once()
      acquireD.expects().once()

      releaseD.expects().once()
      releaseC.expects().once()
      releaseB.expects().once()
      releaseA.expects().once()

      val d = depD.use(IO(_)).unsafeRunSync()
    }
  }
}
