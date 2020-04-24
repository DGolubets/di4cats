package ru.dgolubets.di4cats

import cats.Applicative
import cats.effect.concurrent.Ref
import cats.effect.{Resource, Sync}
import cats.implicits._

import scala.reflect.ClassTag

private case class DependencyCache(cache: Map[ClassTag[_], Any])

class Dependency[F[_] : Applicative : Sync, A](private val resource: Ref[F, DependencyCache] => Resource[F, A]) {

  def map[B: ClassTag](f: A => B): Dependency[F, B] = {
    new Dependency[F, B](s => resource(s).map(f))
  }

  def flatMap[B](f: A => Dependency[F, B]): Dependency[F, B] = {
    new Dependency[F, B](s => resource(s).flatMap(a => f(a).resource(s)))
  }

  def use[B](f: A => F[B]): F[B] = {
    Ref[F].of(DependencyCache(Map.empty)).flatMap { cache =>
      resource(cache).use(f)
    }
  }
}

object Dependency {

  private def cached[F[_] : Sync, A](cacheRef: Ref[F, DependencyCache], default: => Resource[F, A])(implicit t: ClassTag[A]): Resource[F, A] = {
    Resource.liftF {
      cacheRef.get.map { cache =>
        cache.cache.get(t) match {
          case Some(res) => Resource.pure[F, A](res.asInstanceOf[A])
          case _ => default.evalMap { a =>
            cacheRef.set(cache.copy(cache = cache.cache + (t -> a))).map(_ => a)
          }
        }
      }
    }.flatten
  }

  def apply[F[_] : Sync, A](resource: => A)(implicit t: ClassTag[A]): Dependency[F, A] = {
    managed(Resource.liftF(Sync[F].delay(resource)))
  }

  def managed[F[_] : Sync, A](resource: Resource[F, A])(implicit t: ClassTag[A]): Dependency[F, A] = {
    new Dependency(ref => cached(ref, resource))
  }

  implicit def convert[F[_] : Sync, A](implicit a: A, t: ClassTag[A]): Dependency[F, A] = {
    apply(a)
  }
}