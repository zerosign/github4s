package org.zerosign.github4s.client

import cats.effect.{ ConcurrentEffect, Resource }
import fs2.Stream
import org.http4s.Uri
import org.http4s.client.Client
import org.zerosign.github4s.data.Repository

final class IssueClient[F[_]](
  pool: Resource[F, Client[F]],
  base: Uri,
  repository: Repository,
  user: String,
  token: String
) (implicit F: ConcurrentEffect[F]) extends GithubClient[F](pool, base, user, token) {

  import org.http4s.{ Request, Method }
  import io.circe.{ Decoder, Encoder, HCursor, DecodingFailure, Json }

  import org.zerosign.github4s.data.{
    IssueComment,
    Timestamp,
    User,
    Status
  }

  import org.zerosign.github4s.error._

  import org.zerosign.github4s.action.{
    CreateIssueComment
    // CreateReviewComment,
    // ReplyReviewComment
  }

  import org.http4s.circe._


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
  implicit private[this] val issueCommentDecoder : Decoder[IssueComment] =
    new Decoder[IssueComment] {
      final def apply(h: HCursor) : Decoder.Result[IssueComment] = {
        for {
          id <- h.downField("id").as[Int]
          user <- h.downField("user").as[User]
          content <- h.downField("body").as[String]
          updated <- h.downField("updated_at").as[String]
          created <- h.downField("created_at").as[String]
          timestamp <- Timestamp.apply(updated, created).left.map {
            case e => DecodingFailure.fromThrowable(e, List.empty)
          }
        } yield (IssueComment(id, user, content, timestamp))
      }
    }

  implicit private[this] val issueCommentEntityDecoder = jsonOf[F, IssueComment]

  implicit private[this] val createIssueCommentEncoder : Encoder[CreateIssueComment] =
    new Encoder[CreateIssueComment] {
      final def apply(o: CreateIssueComment) : Json = Json.obj(
        ("body", Json.fromString(o.body))
      )
    }

  implicit private[this] val createIssueCommentEntityEncoder = jsonEncoderOf[F, CreateIssueComment]

  // GET /repos/:owner/:repo/issues/comments/:comment_id
  @inline final def fetch(id: Int) : F[Either[GithubError, IssueComment]] =
    pool.use { client =>
      client.fetch[Either[GithubError, IssueComment]](
        Request[F](
          uri = base.withPath(s"/repos/${repository.owner}/${repository.name}/issues/comments/${id}"),
          method = Method.GET
        ).withHeaders(headers)
      )(entityHandler[IssueComment])
    }

  // request:
  // POST /repos/:owner/:repo/issues/:number/comments
  //
  // { "body": "Me too" }
  @inline final def comment(id: Int, content: String) : F[Either[GithubError, Status[Int]]] =
    pool.use { client =>
      client.fetch[Either[GithubError, Status[Int]]](
        Request[F](
          uri = base.withPath(s"repos/${repository.owner}/${repository.name}/issues/${id}/comments"),
          method = Method.POST
        ).withHeaders(headers).withEntity(CreateIssueComment(content))
      )(entityHandler)
    }
}
