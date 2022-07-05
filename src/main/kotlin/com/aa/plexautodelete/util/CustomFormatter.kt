/*
 * Copyright (c) 2000, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.aa.plexautodelete.util

import java.io.PrintWriter
import java.io.StringWriter
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.logging.Formatter
import java.util.logging.LogRecord

/**
 * See [java.util.logging.SimpleFormatter] for format string spec.
 */
internal class CustomFormatter(private val format: String) : Formatter() {

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