package com.aa.plexautodelete.plex

import org.xml.sax.ErrorHandler
import org.xml.sax.SAXParseException
import java.util.logging.Level.SEVERE
import java.util.logging.Level.WARNING
import java.util.logging.Logger

class StandardErrorHandler(private val logger: Logger?) : ErrorHandler {
  override fun warning(e: SAXParseException) {
    logger?.log(WARNING, "XML Warning", e) ?: e.printStackTrace()
  }

  override fun error(e: SAXParseException) {
    logger?.log(SEVERE, "XML Error", e) ?: e.printStackTrace()
  }

  override fun fatalError(e: SAXParseException) {
    logger?.log(SEVERE, "XML Fatal Error", e) ?: e.printStackTrace()
  }
}
