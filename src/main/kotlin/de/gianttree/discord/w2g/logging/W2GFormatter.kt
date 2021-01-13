package de.gianttree.discord.w2g.logging

import java.io.PrintWriter
import java.io.StringWriter
import java.text.DateFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.logging.LogRecord
import java.util.logging.SimpleFormatter

class W2GFormatter : SimpleFormatter() {
    private val sw = StringWriter()
    private val pw = PrintWriter(sw)
    private val dateFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    override fun format(record: LogRecord): String {
        return buildString {
            append(record.level.localizedName)
            append(' ')
            append(dateFormatter.format(Instant.ofEpochMilli(record.millis).atZone(ZoneId.systemDefault())))
            append(": ")
            append(formatMessage(record))
            if (record.thrown != null) {
                record.thrown.printStackTrace(pw)
                append(System.lineSeparator())
                append(sw)
                sw.flush()
            }
            append(System.lineSeparator())
        }
    }
}
