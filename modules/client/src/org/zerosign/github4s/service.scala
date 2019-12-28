package org.zerosign.github4s.service

import cats.effect.{ ContextShift, ConcurrentEffect }
import fs2.Stream
import fs2.concurrent.Queue
import org.http4s.dsl.Http4sDsl

import org.zerosign.github4s.event.{ Event }

final class GithubHookService[F[_]](queue: Stream[F, Queue[F, Event]])
  (implicit F: ConcurrentEffect[F], cs: ContextShift[F]) extends Http4sDsl[F] {

  import io.circe.Decoder
  import org.http4s.HttpRoutes

  @inline final val routes  : HttpRoutes[F] = HttpRoutes.of[F] {
    case request @ GET -> Root / "events" => ???
      // request.headers("X-Github-Delivery")
      // request.headers("X-Hub-Signature")
      // request.headers("ETag")
      // request.headers("X-Poll-Interval")
      // check user-agent ~ Github-Hookshot
      // request.headers("X-Github-Event")
  }
}
