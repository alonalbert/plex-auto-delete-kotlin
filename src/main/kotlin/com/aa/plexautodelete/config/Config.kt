package com.aa.plexautodelete.config

data class Config(val plexUrl: String, val plexToken: String, val days: Long,  val tvSections: Set<String>, val users: List<User>)

data class User(val name: String, val plexToken: String, val shows: Set<String>)
