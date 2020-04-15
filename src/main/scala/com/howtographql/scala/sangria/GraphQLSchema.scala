package com.howtographql.scala.sangria

import sangria.schema.{Field, ListType, ObjectType}
import models._
// #
import sangria.schema._
import sangria.macros.derive._


object GraphQLSchema {
  // 1 These are fields we want to expose in the DB
  val LinkType = ObjectType[Unit, Link](
    "Link",
    fields[Unit, Link](
      Field("id", IntType, resolve = _.value.id),
      Field("url", StringType, resolve = _.value.url),
      Field("description", StringType, resolve = _.value.description)
    )
  )
  // sangria.macros.derive._
  // implicit val LinkType = deriveObjectType[Unit, Link]() //  macro way of defining LinkType

  val Id = Argument("id", IntType)
  val Ids = Argument("ids", ListInputType(IntType))

  // 2
  val QueryType = ObjectType(
    "Query",
    fields[MyContext, Unit](
      Field("allLinks", ListType(LinkType), resolve = c => c.ctx.dao.allLinks),
      Field(
        "link", OptionType(LinkType), arguments = Id :: Nil,
        resolve = c => c.ctx.dao.getLink(c.arg[Int]("id"))),
      Field(
        "links", ListType(LinkType), arguments = Ids :: Nil,
        resolve = c => c.ctx.dao.getLinks(c.arg[Seq[Int]]("ids")))
    )
  )

  // 3
  val SchemaDefinition = Schema(QueryType)
}
