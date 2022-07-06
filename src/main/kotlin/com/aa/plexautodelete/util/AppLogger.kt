package com.aa.plexautodelete.util

import java.io.PrintWriter
import java.io.StringWriter
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.logging.*
import java.util.logging.Level.ALL
import java.util.logging.Level.INFO


internal object AppLogger {
  val CONSOLE_LOGGER: Logger get() = createLogger(ConsoleHandler())

  init {
    LogManager.getLogManager().reset()
  }

  internal fun createLogger(file: String): Logger {
    return createLogger(FileHandler(file, 1_000_000, 5, true))
  }

  internal fun createLogger(handler: Handler): Logger {
    handler.level = ALL
    handler.formatter = CustomFormatter("%1\$tY-%1\$tm-%1\$td -%1\$tT   %2\$-30s [%4\$8s] %5\$s%6\$s%n")
    return Logger.getLogger("Logger").apply {
      level = ALL
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
