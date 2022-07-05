package com.aa.plexautodelete.util

import java.util.logging.Handler
import java.util.logging.Level.ALL
import java.util.logging.Level.INFO
import java.util.logging.LogManager
import java.util.logging.LogRecord
import java.util.logging.Logger


internal object AppLogger {
  val CONSOLE_LOGGER: Logger get() = createLogger(AppConsoleHandler())

  init {
    LogManager.getLogManager().reset()
  }

  private fun createLogger(handler: Handler): Logger {
    handler.level = ALL
    handler.formatter = CustomFormatter("%4\$s: %5\$s [%1\$tc]%n")
    handler.formatter = CustomFormatter("%1\$tY-%1\$tm-%1\$td -%1\$tT   %2\$-30s [%4\$s] %5\$s%n")
    return Logger.getLogger("Logger").apply {
      level = ALL
      addHandler(handler)
    }
  }

  private class AppConsoleHandler : Handler() {
    override fun publish(record: LogRecord) {
      val stream = if (record.level.intValue() > INFO.intValue()) System.err else System.out
      stream.print(formatter.format(record))
    }

    override fun flush() {
    }

    override fun close() {}
  }
}
