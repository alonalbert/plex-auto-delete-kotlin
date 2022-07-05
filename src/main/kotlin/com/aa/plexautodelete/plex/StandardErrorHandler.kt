package com.aa.plexautodelete.plex

import org.xml.sax.ErrorHandler
import org.xml.sax.SAXParseException

object StandardErrorHandler : ErrorHandler {
  override fun warning(e: SAXParseException) {
    println(" XML Warning:")
    e.printStackTrace(System.out)
  }

  override fun error(e: SAXParseException) {
    println(" XML Error:")
    e.printStackTrace(System.out)
  }

  override fun fatalError(e: SAXParseException) {
    println(" XML Fatal Error:")
    e.printStackTrace(System.out)
  }
}