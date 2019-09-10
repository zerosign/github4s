package org.zerosign.github4s.action

final case class UpdatedGist(filename: String, content: String)

final case class CreateGist(files: Map[String, String], public: Boolean, description: String)
final case class UpdateGist(id: String, files: Map[String, UpdatedGist], description: String)

final case class CreateReviewComment(body: String, commit: String, path: String, position: Int)
final case class CreateIssueComment(body: String)
final case class ReplyReviewComment(body: String, reply_to: Int)
