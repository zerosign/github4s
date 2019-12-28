package org.zerosign.github4s.client

import cats.effect.{ ConcurrentEffect, Resource }
import fs2.Stream
import org.http4s.Uri
import org.http4s.client.Client

final class GistClient[F[_]](pool: Resource[F, Client[F]], base: Uri, user: String, token: String)
  (implicit F: ConcurrentEffect[F]) extends GithubClient[F](pool, base, user, token) {

  import cats.syntax.either._
  import org.http4s.{ Request, Method, Uri, ParseFailure }
  import io.circe.{ HCursor, Encoder, Decoder, DecodingFailure, Json }
  import org.zerosign.github4s.error._

  import org.zerosign.github4s.data.{
    Gist, GistFile, Timestamp, Status, User
  }

  import org.zerosign.github4s.action.{
    CreateGist, UpdateGist, UpdatedGist
  }

  import org.http4s.circe._
  import org.http4s.circe.CirceEntityEncoder._

  //
  // https://developer.github.com/v3/gists/#get-a-single-gist
  //
  implicit final val gistFileDecoder : Decoder[GistFile] = new Decoder[GistFile] {
    final def apply(h: HCursor) : Decoder.Result[GistFile] = {
      for {
        filename <- h.downField("filename").as[String]
        `type` <- h.downField("type").as[String]
        content <- h.downField("content").as[String]
        size <- h.downField("size").as[Int]
        url <- h.downField("raw_url").as[String].flatMap { raw =>
          Uri.fromString(raw).left.map {
            case ParseFailure(m1, m2) =>
              DecodingFailure.apply(s"${m1}, ${m2}", List.empty)
          }
        }
      } yield (GistFile(filename, filename, `type`, content, size, url))
    }
  }

  implicit final val gistFilesDecoder : Decoder[Seq[GistFile]] = new Decoder[Seq[GistFile]] {
    final def apply(h: HCursor) : Decoder.Result[Seq[GistFile]] = {
      h.keys.getOrElse(Seq.empty).map { key =>
        h.downField(key).as[GistFile]
      }.foldLeft(Either.right[DecodingFailure, Seq[GistFile]](Seq.empty[GistFile])){
        case (data, Left(e)) => {
          logger.info(s"1, data: ${data}, next: ${e}")
          data
        }
        case (Right(data), Right(next)) => {
          logger.info(s"2, data: ${data}, next: ${next}")
          Right(data :+ next)
        }
        case (Left(e), next) => {
          logger.info(s"3, data: ${e}, next: ${next}")
          Left(e)
        }
        case (data, next) => {
          logger.info(s"4, data: ${data}, next: ${next}")
          data
        }
      }
    }
  }

  implicit final val gistDecoder : Decoder[Gist] = new Decoder[Gist] {
    final def apply(h: HCursor) : Decoder.Result[Gist] = {
      for {
        id <- h.downField("id").as[String]
        user <- h.downField("owner").as[User]
        public <- h.downField("public").as[Boolean]
        created <- h.downField("created_at").as[String]
        updated <- h.downField("updated_at").as[String]
        timestamp <- Timestamp.apply(updated, created).left.map {
          case e => DecodingFailure.fromThrowable(e, List.empty)
        }
        files <- h.downField("files").as[Seq[GistFile]]
      } yield {
        Gist(id, user, public, files, timestamp)
      }
    }
  }

  implicit final val gistEntityDecoder = jsonOf[F, Gist]

  implicit final val createGistEncoder : Encoder[CreateGist] = new Encoder[CreateGist] {
    final def apply(o: CreateGist) : Json = Json.obj(
      ("description", Json.fromString(o.description)),
      ("public", Json.fromBoolean(o.public)),
      ("files", Json.fromFields(
        o.files.toIterable.map {
          case (key, value) => (key, Json.obj(
            ("content", Json.fromString(value))
          ))
        }
      ))
    )
  }

  implicit final val updateGistEncoder : Encoder[UpdateGist] = new Encoder[UpdateGist] {
    final def apply(o: UpdateGist) : Json = Json.obj(
      ("description", Json.fromString(o.description)),
      ("files", Json.fromFields(
        o.files.toIterable.map {
          case (key, file) => (key, Json.obj(
            ("content", Json.fromString(file.content)),
            ("filename", Json.fromString(file.filename))
          ))
        }
      ))
    )
  }

  //
  // without version:
  // request:
  // GET /gists/:gist_id
  //
  // with version:
  // request:
  // GET /gists/:gist_id/:sha
  //
  @inline final def fetch(id: String, version: Option[String] = None) : F[Either[GithubError, Gist]] =
    pool.use { client =>
      client.fetch[Either[GithubError, Gist]](
        Request[F](
          uri = version match {
            case Some(v) => resolve(s"/gists/${id}/${v}")
            case _       => resolve(s"/gists/${id}")
          },
          method = Method.GET
        ).withHeaders(headers)
      )(entityHandler[Gist])
    }

  //
  // request:
  // POST /gists
  //
  // {
  //   "description": "Hello World Examples",
  //   "public": true,
  //   "files": {
  //     "hello_world.rb": {
  //       "content": "class HelloWorld\n   def initialize(name)\n      @name = name.capitalize\n   end\n   def sayHi\n      puts \"Hello !\"\n   end\nend\n\nhello = HelloWorld.new(\"World\")\nhello.sayHi"
  //     },
  //     "hello_world.py": {
  //       "content": "class HelloWorld:\n\n    def __init__(self, name):\n        self.name = name.capitalize()\n       \n    def sayHi(self):\n        print \"Hello \" + self.name + \"!\"\n\nhello = HelloWorld(\"world\")\nhello.sayHi()"
  //     },
  //     "hello_world_ruby.txt": {
  //       "content": "Run `ruby hello_world.rb` to print Hello World"
  //     },
  //     "hello_world_python.txt": {
  //       "content": "Run `python hello_world.py` to print Hello World"
  //     }
  //   }
  // }
  //
  //
  @inline final def create(files: Map[String, String], desc: Option[String] = None, public: Boolean = false) :
      F[Either[GithubError, Status[String]]] = pool.use { client =>
    client.fetch[Either[GithubError, Status[String]]](
      Request[F](
        uri = resolve("/gists"),
        method = Method.POST
      ).withHeaders(headers).withEntity(CreateGist(files, public, desc.getOrElse("")))
    )(entityHandler)
  }

  //
  // request:
  // PATCH /gists/:gist_id
  //
  // {
  //  "description": "Hello World Examples",
  //   "files": {
  //     "hello_world_ruby.txt": {
  //       "content": "Run `ruby hello_world.rb` or `python hello_world.py` to print Hello World",
  //       "filename": "hello_world.md"
  //     },
  //     "hello_world_python.txt": null,
  //     "new_file.txt": {
  //       "content": "This is a new placeholder file."
  //     }
  //   }
  // }
  //
  @inline final def update(id: String, files: Map[String, UpdatedGist], desc: Option[String]) : F[Either[GithubError, Status[String]]] =
    pool.use { client =>
      client.fetch[Either[GithubError, Status[String]]](
        Request[F](
          uri = resolve(s"/gists/${id}"),
          method = Method.PATCH
        ).withHeaders(headers).withEntity(UpdateGist(id, files, desc.getOrElse("")))
      )(entityHandler)
    }
}
