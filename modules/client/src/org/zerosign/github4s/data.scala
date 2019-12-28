package org.zerosign.github4s.data

// object action {
//   sealed trait Action
//   case class Review(workspace: String*) extends Action
//   case class Snapshot(workspace: String*) extends Action
//   case class Lift(workspace: String, from: String, to: String) extends Action
// }

object Timestamp {
  import scala.util.Try
  import java.time.Instant
  import java.time.format.DateTimeFormatter

  private[this] lazy val formatter = DateTimeFormatter.ISO_INSTANT

  def parse(date: String) : Either[Throwable, Long] =
    Try(formatter.parse(date)).toEither.map(Instant.from(_).toEpochMilli)

  def apply(created: String, updated: String) : Either[Throwable, Timestamp] =
    for {
      created_at <- Timestamp.parse(created)
      updated_at <- Timestamp.parse(updated)
    } yield (Timestamp(created_at, updated_at))
}

final case class Timestamp(created: Long, updated: Long)
final case class User(id: Int, name: String, `type`: String)
final case class Repository(owner: String, name: String)
final case class IssueComment(id: Int, user: User, content: String, timestamp: Timestamp)
final case class ReviewComment(id: Int, user: User, content: String, timestamp: Timestamp)

import org.http4s.Uri

final case class Status[T](id: T, timestamp: Timestamp)
final case class GistFile(name: String, filename: String, `type`: String, content: String, size: Int, url: Uri)
final case class Gist(id: String, user: User, public: Boolean, files: Seq[GistFile], timestamp: Timestamp)
