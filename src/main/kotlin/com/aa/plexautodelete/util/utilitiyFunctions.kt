package com.aa.plexautodelete.util

import com.aa.plexautodelete.config.Shows
import com.aa.plexautodelete.config.Shows.Type.EXCLUDE
import com.aa.plexautodelete.config.Shows.Type.INCLUDE
import java.text.CharacterIterator
import java.text.StringCharacterIterator


fun Long.toFileSize(): String {
  if (this < 1024) {
    return "$this B"
  }
  var value = this
  val ci: CharacterIterator = StringCharacterIterator("KMGTPE")
  var i = 40
  while (i >= 0 && this > 0xfffccccccccccccL shr i) {
    value = value shr 10
    ci.next()
    i -= 10
  }
  return String.format("%.1f %cB", value / 1024.0, ci.current())
}

fun Shows.isIncluded(title: String): Boolean = when (type) {
  INCLUDE -> title in titles
  EXCLUDE -> title !in titles
}

fun Shows.isExcluded(title: String): Boolean = !isIncluded(title)

