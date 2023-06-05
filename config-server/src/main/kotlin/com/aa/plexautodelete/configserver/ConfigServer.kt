package com.aa.plexautodelete.configserver

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun main(args: Array<String>) {
  embeddedServer(Netty, 8080) {
    routing {
      get("/") {
        call.respondText("Hello, world!", ContentType.Text.Html)
      }
    }
  }.start(wait = true)
}
