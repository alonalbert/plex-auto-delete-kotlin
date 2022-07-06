package com.aa.plexautodelete.config

import com.google.gson.Gson
import java.io.FileReader

data class Config(
  val plexUrl: String,
  val plexToken: String,
  val days: Long,
  val tvSections: Set<String>,
  val users: List<User>,
  val pushoverConfig: PushoverConfig,
) {
  companion object {
    fun loadFile(configFile: String): Config = Gson().fromJson(FileReader(configFile), Config::class.java)
  }
}

data class User(val name: String, val plexToken: String, val shows: Set<String>)

data class PushoverConfig(val appToken: String, val userToken: String)
