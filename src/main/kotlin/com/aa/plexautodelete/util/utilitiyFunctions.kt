package com.aa.plexautodelete.util

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

fun main() {
  println(10000L.toFileSize())
}
