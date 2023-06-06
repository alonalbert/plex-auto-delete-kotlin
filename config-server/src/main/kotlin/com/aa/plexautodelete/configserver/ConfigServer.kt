package com.aa.plexautodelete.configserver

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import java.nio.file.Paths
import kotlin.io.path.reader

private val DEFAULT_CONFIG_FILE = "${System.getProperty("user.home")}/.plex-auto-delete-config.json"

fun main(args: Array<String>) {
  val parser = ArgParser("Plex Auto Delete Config Server")
  val configFile by parser.option(ArgType.String, shortName = "c", description = "Config file").default(DEFAULT_CONFIG_FILE)
  val port by parser.option(ArgType.Int, shortName = "p", description = "Port number").default(8080)
  parser.parse(args)

  embeddedServer(Netty, port) {
    install(CallLogging)
    install(StatusPages) {
      exception<Throwable> { call, cause ->
        call.application.log.error("Internal error", cause)
        call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
      }
    }
    routing {
      get("/") {
        call.respondText(Paths.get(configFile).reader().readText(), ContentType.Application.Json)
      }
    }

  }.start(wait = true)
}
