package org.zerosign.github4s.client

import cats.effect.{Resource, ConcurrentEffect}
import fs2.Stream
import org.http4s.Uri
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl

abstract class GithubClient[F[_]](pool: Resource[F, Client[F]], base: Uri, user: String, token: String)
  (implicit F: ConcurrentEffect[F]) extends Http4sClientDsl[F] {

  import java.util.Base64

  import cats.data._
  import io.circe._
  import org.log4s.Logger
  import org.http4s.{ DecodeFailure, EntityDecoder, Request }
  import org.http4s.circe._
  import org.http4s.{ Uri, Status => HttpStatus, Response, Header, Headers }
  import org.zerosign.github4s.data.{ Status, Timestamp, User }
  import org.zerosign.github4s.error._

  final protected val logger : Logger = org.log4s.getLogger

  final protected val encodedToken : String = Base64.getEncoder.encodeToString(s"${user}:${token}".getBytes)

  @inline protected[this] final def resolve(path: String): Uri =
    base.withPath(s"${path}")

  final protected val headers = Headers(
    Seq(
      Header("Accept", "application/vnd.github.v3+json"),
      Header("Authorization", s"Basic ${encodedToken}")
    ):_*
  )

  implicit protected val userDecoder = new Decoder[User] {
    final def apply(h: HCursor) : Decoder.Result[User] = {
      for {
        id <- h.downField("id").as[Int]
        name <- h.downField("login").as[String]
        `type` <- h.downField("type").as[String]
      } yield (User(id, name, `type`))
    }
  }


  implicit protected final val statusIntDecoder = new Decoder[Status[Int]] {
    final def apply(h: HCursor) : Decoder.Result[Status[Int]] = {
      for {
        id <- h.downField("id").as[Int]
        created <- h.downField("created_at").as[String]
        updated <- h.downField("updated_at").as[String]
        timestamp <- Timestamp.apply(updated, created).left.map {
          case e => DecodingFailure.fromThrowable(e, List.empty)
        }
      } yield (Status(id, timestamp))
    }
  }

  implicit protected final val statusStringDecoder = new Decoder[Status[String]] {
    final def apply(h: HCursor) : Decoder.Result[Status[String]] = {
      for {
        id <- h.downField("id").as[String]
        created <- h.downField("created_at").as[String]
        updated <- h.downField("updated_at").as[String]
        timestamp <- Timestamp.apply(updated, created).left.map {
          case e => DecodingFailure.fromThrowable(e, List.empty)
        }
      } yield (Status(id, timestamp))
    }
  }


  implicit protected final val statusIntEntityDecoder = jsonOf[F, Status[Int]]
  implicit protected final val statusStringEntityDecoder = jsonOf[F, Status[String]]

  @inline protected final def errorToStatus(status: HttpStatus) : ErrorKind = status match {
    case HttpStatus.Unauthorized => BadCredential
    case HttpStatus.Forbidden => RetryCredError
    case HttpStatus.BadRequest => InvalidRequest
    case HttpStatus.UnprocessableEntity => ValidationError
    case _ => UnknownError
  }

  @inline protected final def entityHandler[T]
    (implicit decoder: EntityDecoder[F, T]): Response[F] => F[Either[GithubError, T]] = {
    case HttpStatus.Successful(r) =>
      r.attemptAs[T]
          .leftMap { e: DecodeFailure =>
            GithubError(e, DecodeError).asInstanceOf[GithubError]
          }
          .value
    case r @ _ =>
      EitherT.left.apply(F.map(r.as[String])(message => GithubError(message, errorToStatus(r.status)))).value
  }

  // {
  //   "id": 1,
  //   "node_id": "MDEyOklzc3VlQ29tbWVudDE=",
  //   "url": "https://api.github.com/repos/octocat/Hello-World/issues/comments/1",
  //   "html_url": "https://github.com/octocat/Hello-World/issues/1347#issuecomment-1",
  //   "body": "Me too",
  //   "user": {
  //     "login": "octocat",
  //     "id": 1,
  //     "node_id": "MDQ6VXNlcjE=",
  //     "avatar_url": "https://github.com/images/error/octocat_happy.gif",
  //     "gravatar_id": "",
  //     "url": "https://api.github.com/users/octocat",
  //     "html_url": "https://github.com/octocat",
  //     "followers_url": "https://api.github.com/users/octocat/followers",
  //     "following_url": "https://api.github.com/users/octocat/following{/other_user}",
  //     "gists_url": "https://api.github.com/users/octocat/gists{/gist_id}",
  //     "starred_url": "https://api.github.com/users/octocat/starred{/owner}{/repo}",
  //     "subscriptions_url": "https://api.github.com/users/octocat/subscriptions",
  //     "organizations_url": "https://api.github.com/users/octocat/orgs",
  //     "repos_url": "https://api.github.com/users/octocat/repos",
  //     "events_url": "https://api.github.com/users/octocat/events{/privacy}",
  //     "received_events_url": "https://api.github.com/users/octocat/received_events",
  //     "type": "User",
  //     "site_admin": false
  //   },
  //   "created_at": "2011-04-14T16:00:49Z",
  //   "updated_at": "2011-04-14T16:00:49Z"
  // }

  @inline final def file(url: Uri) : F[Stream[F, Byte]] =
    pool.use { client =>
      F.delay(client.stream(
        Request[F](
          uri = url,
          headers = headers
        )
      ) flatMap { response : Response[F] =>
        logger.info(s"download file from: ${url} with status: ${response.status}")
        response.body
      })
    }
  // TODO: @zerosign migration from Stream[F, Client[F]] to Resource[F, Client[F]]
    // pool.use { client =>
    //   client.stream(
    //     Request[F](
    //       uri = url,
    //       headers = headers
    //     )
    //   ) flatMap { response: Response[F] =>
    //     logger.info(s"download file from: ${url} with status: ${response.status}")
    //     response.body
    //   }
    // }

}
