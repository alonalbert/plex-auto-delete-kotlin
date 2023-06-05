package com.aa.plexautodelete.plex

import java.time.Instant

interface Video {
  val key: String
  val name: String
  val lastViewed: Instant
}
