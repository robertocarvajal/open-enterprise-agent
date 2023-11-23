package io.iohk.atala.pollux.anoncreds

import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault}

import java.time.Instant
import java.time.temporal.ChronoUnit

object AnonCredsLibBenchmarking extends ZIOAppDefault {

  private val SCHEMA_ID = "mock:uri2"
  private val ISSUER_DID = "mock:issuer_id/path&q=bar"

  def genCredDef(schema: SchemaDef) =
    for {
      timeBefore <- ZIO.succeed(Instant.now)
      _ <- ZIO.attempt(AnoncredLib.createCredDefinition(ISSUER_DID, schema, "tag", supportRevocation = false))
      timeAfter <- ZIO.succeed(Instant.now)
      _ <- ZIO.logInfo(s"Credential Definition created")
    } yield timeBefore.until(timeAfter, ChronoUnit.MILLIS)

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {
    for {
      args <- getArgs
      count = args(0).toInt
      parallelism = args(1).toInt
      attrNum = args(2).toInt
      _ <- ZIO.logInfo(s"Creating Schema with $attrNum attributes...")
      schema <- ZIO.attempt(
        AnoncredLib.createSchema(
          SCHEMA_ID,
          "0.1.0",
          (1 to attrNum).map(i => s"attribute_$i").toSet,
          ISSUER_DID
        )
      )
      _ <- ZIO.logInfo(s"Schema created.")
      credDefs <- ZIO.foreachPar(1 to count)(i => genCredDef(schema)).withParallelism(parallelism)
      _ <- ZIO.logInfo(s"Generation times [${credDefs.size}]: $credDefs")
      _ <- ZIO.logInfo(s"Min: ${credDefs.min} - Max: ${credDefs.max}")
      _ <- ZIO.logInfo(s"Mean time: ${credDefs.sum / credDefs.size}")
    } yield ()
  }
}
