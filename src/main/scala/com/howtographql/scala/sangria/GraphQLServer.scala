package com.howtographql.scala.sangria

import akka.http.scaladsl.server.Route
import sangria.parser.QueryParser
import spray.json.{JsObject, JsString, JsValue}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}
import akka.http.scaladsl.server._
import sangria.ast.Document
import sangria.execution._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import sangria.marshalling.sprayJson._

import scala.concurrent.ExecutionContext

object GraphQLServer {

  /** 1: Access the db */
  private val dao = DBSchema.createDatabase

  /** Responds with Route, used directly in routing of HTTP server. */
  def endpoint(requestJSON: JsValue)(implicit ec: ExecutionContext): Route = {

    /** contains query: Object, variables: Object, and operationNames: String */
    val JsObject(fields) = requestJSON

    /** extract query from request */
    val JsString(query) = fields("query")

    /** Parse the query */
    QueryParser.parse(query) match {
      case Success(queryAst) =>
        // 6
        val operation = fields.get("operationName") collect {
          case JsString(op) => op
        }

        // 7
        val variables = fields.get("variables") match {
          case Some(obj: JsObject) => obj
          case _ => JsObject.empty
        }
        // 8
        complete(executeGraphQLQuery(queryAst, operation, variables))
      case Failure(error) =>
        complete(BadRequest, JsObject("error" -> JsString(error.getMessage)))
    }

  }

  private def executeGraphQLQuery(query: Document, operation: Option[String], vars: JsObject)(implicit ec: ExecutionContext) = {
    // 9
    Executor.execute(
      GraphQLSchema.SchemaDefinition, // 10
      query, // 11
      MyContext(dao), // 12
      variables = vars, // 13
      operationName = operation // 14
    ).map(OK -> _)
      .recover {
        case error: QueryAnalysisError => BadRequest -> error.resolveError
        case error: ErrorWithResolver => InternalServerError -> error.resolveError
      }
  }

}
