package com.aa.plexautodelete.util

import java.io.PrintWriter
import java.io.StringWriter
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.logging.FileHandler
import java.util.logging.Formatter
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.Level.INFO
import java.util.logging.LogManager
import java.util.logging.LogRecord
import java.util.logging.Logger

private const val FILE_FORMAT = "%1\$tY-%1\$tm-%1\$td -%1\$tT   %2\$-30s [%4\$8s] %5\$s%6\$s%n"
private const val CONSOLE_FORMAT = "%5\$s%6\$s%n"
private const val MAX_LOG_FILE_SIZE = 1_000_000
private const val MAX_LOG_FILES = 5

internal object AppLogger {
  init {
    LogManager.getLogManager().reset()
  }

  internal fun createLogger(level: Level, file: String?): Logger {
    val format: String
    val handler = if (file != null) {
      format = FILE_FORMAT
      FileHandler(file, MAX_LOG_FILE_SIZE, MAX_LOG_FILES, /* append = */ true)
    } else {
      format = CONSOLE_FORMAT
      ConsoleHandler()
    }
    handler.level = level
    handler.formatter = CustomFormatter(format)
    return Logger.getLogger("Logger").apply {
      this.level = level
      addHandler(handler)
    }
  }

  private class ConsoleHandler : Handler() {
    override fun publish(record: LogRecord) {
      val stream = if (record.level.intValue() > INFO.intValue()) System.err else System.out
      stream.print(formatter.format(record))
    }

    override fun flush() {}

    override fun close() {}
  }

  private class CustomFormatter(private val format: String) : Formatter() {
    override fun format(record: LogRecord): String {
      val zdt = ZonedDateTime.ofInstant(record.instant, ZoneId.systemDefault())
      val source = when {
        record.sourceClassName == null -> record.loggerName
        record.sourceMethodName == null -> record.sourceClassName
        else -> "${record.sourceClassName.split('.').last()}.${record.sourceMethodName}()"
      }
      val message = formatMessage(record)
      var throwable = ""
      if (record.thrown != null) {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        pw.println()
        record.thrown.printStackTrace(pw)
        pw.close()
        throwable = sw.toString()
      }
      return String.format(
        format,
        zdt,
        source,
        record.loggerName,
        record.level.name,
        message,
        throwable
      )
    }
  }
}
