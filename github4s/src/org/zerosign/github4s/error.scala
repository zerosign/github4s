package org.zerosign.github4s.error

sealed trait ErrorKind
// #Failed login limit
// 401 unauthorized
case object BadCredential extends ErrorKind
// 493 forbidden3
case object RetryCredError extends ErrorKind
// #Client error
// @see https://developer.github.com/v3/#client-errors
//
// - sending invalid json 400 bad request
// - sending wrong payload
// 400 Bad request
case object InvalidRequest extends ErrorKind
// 422 Unprocessable Entity
case object ValidationError extends ErrorKind
case object DecodeError extends ErrorKind
case object UnknownError extends ErrorKind

final case class GithubError(raw: Any, kind: ErrorKind)

// sealed trait GithubError
// sealed trait ErrorKind


// case class GenericError(message: String, kind: ErrorKind) extends GithubError

// // 422 Unprocessable Entity
// case class DetailError(resource: String, field: String, code: String)
// case class ValidationError(message: String, errors: Seq[DetailError]) extends GithubError

// import org.http4s.DecodeFailure
// case class DecodeError(error: DecodeFailure) extends GithubError
