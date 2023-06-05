package com.aa.plexautodelete.pushover

import com.aa.plexautodelete.config.Config
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.URL
import java.net.URLEncoder

object Pushover {
  fun send(appToken: String, userToken: String, message: String) {
    val url = URL("https://api.pushover.net/1/messages.json")
    val postData = "token=$appToken&user=$userToken&message=${URLEncoder.encode(message, "UTF-8")}"

    val conn = url.openConnection()
    conn.doOutput = true
    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
    conn.setRequestProperty("Content-Length", postData.length.toString())

    DataOutputStream(conn.getOutputStream()).use { it.writeBytes(postData) }
    BufferedReader(InputStreamReader(conn.getInputStream())).use {
      it.readLines()
    }
  }
}

fun main() {
  val (appToken, userToken) = Config.loadFile("data/test-config.json").pushoverConfig
  Pushover.send(appToken, userToken, "Foo & Bar")
}
