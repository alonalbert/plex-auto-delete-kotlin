package com.aa.plexautodelete.configserver

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import java.nio.file.Paths
import kotlin.io.path.reader

val DEFAULT_CONFIG_FILE = "${System.getProperty("user.home")}/.plex-auto-delete-config.json"

fun main(args: Array<String>) {
  val parser = ArgParser("Plex Auto Delete Config Server")
  val configFile by parser.option(ArgType.String, shortName = "c", description = "Config file").default(DEFAULT_CONFIG_FILE)
  val port by parser.option(ArgType.Int, shortName = "p", description = "Port number").default(8080)
  parser.parse(args)

  embeddedServer(Netty, port) {
    install(StatusPages) {
      exception<Throwable> { call, cause ->
        call.respondText(text = "500: $cause" , status = HttpStatusCode.InternalServerError)
      }
    }
    routing {
      get("/") {
        call.respondText(Paths.get(configFile).reader().readText(), ContentType.Application.Json)
//        try {
//        } catch (e: Throwable) {
//          this@embeddedServer.log.error("Error loading file", e)
//        }
      }
    }

  }.start(wait = true)
}
