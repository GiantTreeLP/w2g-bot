package de.gianttree.discord.w2g.logging

import java.io.PrintWriter
import java.io.StringWriter
import java.text.DateFormat
import java.util.*
import java.util.logging.LogRecord
import java.util.logging.SimpleFormatter

class W2GFormatter : SimpleFormatter() {
    private val sw = StringWriter()
    private val pw = PrintWriter(sw)
    private val date = Date()
    private val dateFormatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM)
    override fun format(record: LogRecord): String {
        return buildString {
            append(record.level.localizedName)
            append(' ')
            date.time = record.millis
            append(dateFormatter.format(date))
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
