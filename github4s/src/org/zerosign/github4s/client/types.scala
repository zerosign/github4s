package org.zerosign.github4s.client

trait GistClientF[F[_]] {
  import org.zerosign.github4s.error._

  import org.zerosign.github4s.data.{
    Gist, GistFile, Timestamp, Status, User
  }

  import org.zerosign.github4s.action.{
    CreateGist, UpdateGist, UpdatedGist
  }

  def create(files: Map[String, String], desc: Option[String] = None, public: Boolean = false) : F[Either[GithubError, Status[String]]]
  def fetch(id: String, version: Option[String]) : F[Either[GithubError, Gist]]
  def update(id: String, files: Map[String, UpdatedGist], desc: Option[String]) : F[Either[GithubError, Status[String]]]
}

trait IssueClientF[F[_]] {

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


  def fetch(id: Int) : F[Either[GithubError, IssueComment]]
  def comment(id: Int, content: String) : F[Either[GithubError, Status[Int]]]
}

trait PullRequestClientF[F[_]] {

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


  def fetch(id: Int) : F[Either[GithubError, ReviewComment]]

  def comment(
    id: Int, content: String,
    commit: String, path: String, position: Int
  ) : F[Either[GithubError, Status[Int]]]

  def reply(id: Int, content: String, reply_to: Int) : F[Either[GithubError, Status[Int]]]
}
