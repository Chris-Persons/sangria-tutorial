package com.howtographql.scala.sangria

import sangria.schema.{Field, ListType, ObjectType}
import models._
import sangria.execution.deferred.HasId
import sangria.schema._
import sangria.macros.derive._
import sangria.execution.deferred.Fetcher
import sangria.execution.deferred.DeferredResolver

object GraphQLSchema {

  // 1 These are fields we want to expose in the DB
  implicit val LinkType = deriveObjectType[Unit, Link]()
  // to extract ids from entities
  implicit val linkHasId = HasId[Link, Int](_.id)

  val linksFetcher = Fetcher(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getLinks(ids)
  )

  val Resolver = DeferredResolver.fetchers(linksFetcher)

  val Id = Argument("id", IntType)
  val Ids = Argument("ids", ListInputType(IntType))

  // 2
  val QueryType = ObjectType(
    "Query",
    fields[MyContext, Unit](
      Field("allLinks", ListType(LinkType), resolve = c => c.ctx.dao.allLinks),
      Field(
        "link",
        OptionType(LinkType),
        arguments = Id :: Nil,
        resolve = c => linksFetcher.deferOpt(c.arg(Id))
      ),
      Field(
        "links",
        ListType(LinkType),
        arguments = List(Argument("ids", ListInputType(IntType))),
        resolve = c => linksFetcher.deferSeq(c.arg(Ids))
      )
    )
  )

  // 3
  val SchemaDefinition = Schema(QueryType)
}
