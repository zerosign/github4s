package org.zerosign.github4s.client

import cats.effect.{ ConcurrentEffect, Resource }
import fs2.Stream
import org.http4s.Uri
import org.http4s.client.Client
import org.zerosign.github4s.data.Repository

final class PullRequestClient[F[_]](pool: Resource[F, Client[F]], base: Uri, repository: Repository, user: String, token: String)
  (implicit F: ConcurrentEffect[F]) extends GithubClient[F](pool, base, user, token) {

  import io.circe._
  import org.http4s.circe._

  import org.http4s.{ Request, Method }

  import org.zerosign.github4s.data.{
    ReviewComment,
    User,
    Timestamp,
    Status
  }


  import org.zerosign.github4s.error._

  import org.zerosign.github4s.action.{
    CreateReviewComment,
    ReplyReviewComment
  }

  //
  // {
  //   "url": "https://api.github.com/repos/octocat/Hello-World/pulls/comments/1",
  //   "id": 10,
  //   "node_id": "MDI0OlB1bGxSZXF1ZXN0UmV2aWV3Q29tbWVudDEw",
  //   "pull_request_review_id": 42,
  //   "diff_hunk": "@@ -16,33 +16,40 @@ public class Connection : IConnection...",
  //   "path": "file1.txt",
  //   "position": 1,
  //   "original_position": 4,
  //   "commit_id": "6dcb09b5b57875f334f61aebed695e2e4193db5e",
  //   "original_commit_id": "9c48853fa3dc5c1c3d6f1f1cd1f2743e72652840",
  //   "in_reply_to_id": 8,
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
  //   "body": "Great stuff",
  //   "created_at": "2011-04-14T16:00:49Z",
  //   "updated_at": "2011-04-14T16:00:49Z",
  //   "html_url": "https://github.com/octocat/Hello-World/pull/1#discussion-diff-1",
  //   "pull_request_url": "https://api.github.com/repos/octocat/Hello-World/pulls/1",
  //   "_links": {
  //     "self": {
  //       "href": "https://api.github.com/repos/octocat/Hello-World/pulls/comments/1"
  //     },
  //     "html": {
  //       "href": "https://github.com/octocat/Hello-World/pull/1#discussion-diff-1"
  //     },
  //     "pull_request": {
  //       "href": "https://api.github.com/repos/octocat/Hello-World/pulls/1"
  //     }
  //   }
  // }
  //
  implicit private[this] val reviewCommentDecoder : Decoder[ReviewComment] =
    new Decoder[ReviewComment] {
      final def apply(h: HCursor) : Decoder.Result[ReviewComment] = {
        for {
          id <- h.downField("id").as[Int]
          user <- h.downField("user").as[User]
          content <- h.downField("body").as[String]
          updated <- h.downField("updated_at").as[String]
          created <- h.downField("created_at").as[String]
          timestamp <- Timestamp.apply(updated, created).left.map {
            case e => DecodingFailure.fromThrowable(e, List.empty)
          }
        } yield (ReviewComment(id, user, content, timestamp))
      }
    }

  implicit private[this] val reviewCommentEntityDecoder = jsonOf[F, ReviewComment]

  // somehow semi-automatic circe derivation doesn't work
  // deriveEncoder[CreateReviewComment]
  implicit private[this] val createReviewCommentEncoder : Encoder[CreateReviewComment] = new Encoder[CreateReviewComment] {
    final def apply(o: CreateReviewComment) : Json = Json.obj(
      ("body", Json.fromString(o.body)),
      ("commit_id", Json.fromString(o.commit)),
      ("path", Json.fromString(o.path)),
      ("position", Json.fromInt(o.position))
    )
  }

  // deriveEncoder[ReplyReviewComment]
  implicit private[this] val replyCommentEncoder : Encoder[ReplyReviewComment] = new Encoder[ReplyReviewComment] {
    final def apply(o: ReplyReviewComment) : Json = Json.obj(
      ("body", Json.fromString(o.body)),
      ("reply_to", Json.fromInt(o.reply_to))
    )
  }

  implicit private[this] val replyReviewCommentEntityEncoder = jsonEncoderOf[F, ReplyReviewComment]
  implicit private[this] val createReviewCommentEntityEncoder = jsonEncoderOf[F, CreateReviewComment]

  // GET /repos/:owner/:repo/pulls/comments/:comment_id
  @inline final def fetch(id: Int) : F[Either[GithubError, ReviewComment]] =
    pool.use { client =>
      client.fetch[Either[GithubError, ReviewComment]](
        Request[F](
          uri = resolve(s"/repos/${repository.owner}/${repository.name}/pulls/comments/${id}"),
          method = Method.GET
        ).withHeaders(headers)
      )(entityHandler[ReviewComment])
    }

  // request:
  // POST /repos/:owner/:repo/pulls/:number/comments
  //
  // {
  //    "body": "Nice change",
  //    "commit_id": "6dcb09b5b57875f334f61aebed695e2e4193db5e",
  //    "path": "file1.txt",
  //    "position": 4
  // }
  //
  @inline final def comment(
    id: Int, content: String,
    commit: String, path: String, position: Int) : F[Either[GithubError, Status[Int]]] =
    pool.use { client =>
      client.fetch[Either[GithubError, Status[Int]]](
        Request[F](
          uri = resolve(s"repos/${repository.owner}/${repository.name}/pulls/${id}/comments"),
          method = Method.POST
        ).withHeaders(headers).withEntity(CreateReviewComment(content, commit, path, position))
      )(entityHandler[Status[Int]])
    }

  // request:
  // POST /repos/:owner/:repo/pulls/:number/comments
  //
  // {
  //    "body": "Nice change",
  //    "in_reply_to": 4
  // }
  //
  // response:
  //
  //
  @inline final def reply(id: Int, content: String, reply_to: Int) : F[Either[GithubError, Status[Int]]] =
    pool.use { client =>
      client.fetch[Either[GithubError, Status[Int]]](
        Request[F](
          uri = resolve(s"repos/${repository.owner}/${repository.name}/pulls/${id}/comments"),
          method = Method.POST
        ).withHeaders(headers).withEntity(ReplyReviewComment(content, reply_to))
      )(entityHandler)
    }
}
