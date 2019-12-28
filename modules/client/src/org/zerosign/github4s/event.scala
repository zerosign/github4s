package org.zerosign.github4s.event

import org.zerosign.github4s.data.{ IssueComment, User }

final case class Timestamp(updated: Long, created: Long)
final case class Hook(`type`: String, id: Int, active: Boolean, events: Array[String], appId: Int, timestamp: Timestamp)
final case class Issue(`id`: Int,  title: String, state: String, body: String, labels: Array[String], timestamp: Timestamp)

sealed trait Event

final case class PingEvent(zen: String, id: String, hook: Hook) extends Event
final case class IssuesEvent(action: String, sender: User, issue: Issue) extends Event
final case class IssueCommentEvent(action: String, sender: User, issue: Issue, comment: IssueComment) extends Event
