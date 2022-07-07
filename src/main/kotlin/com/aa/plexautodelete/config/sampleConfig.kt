package com.aa.plexautodelete.config

import com.google.gson.GsonBuilder

fun main() {
  GsonBuilder().setPrettyPrinting().create().toJson(
    Config(
      "http://192.168.1.1:32400",
      7,
      setOf("TV"),
      listOf(
        User(
          "User1",
          "token1",
          Shows(setOf("Show 1 Title"), Shows.Type.EXCLUDE)
        ),
        User(
          "User2",
          "token2",
          Shows(setOf("Show 1 Title"), Shows.Type.INCLUDE)
        ),
      ),
      PushoverConfig("app-token", "user-token")
    )
  ).let { println(it) }
}
