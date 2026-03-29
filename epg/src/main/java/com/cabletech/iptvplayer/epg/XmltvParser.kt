package com.cabletech.iptvplayer.epg

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Pull-parser for XMLTV format EPG data.
 *
 * Supports both plain `.xml` and gzip-compressed `.xml.gz` streams (caller
 * must unwrap the GZIPInputStream before passing to [parse]).
 *
 * XMLTV date format: `yyyyMMddHHmmss Z`  (e.g. `20260330143000 +0000`)
 */
object XmltvParser {

    private val DATE_FORMAT = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US)

    fun parse(stream: InputStream): List<EpgProgramme> {
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(stream, "UTF-8")
        }

        val programmes = mutableListOf<EpgProgramme>()
        var eventType = parser.next()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "programme") {
                parseProgramme(parser)?.let { programmes += it }
            }
            eventType = parser.next()
        }
        return programmes
    }

    private fun parseProgramme(parser: XmlPullParser): EpgProgramme? {
        val channelId = parser.getAttributeValue(null, "channel") ?: return null
        val startStr  = parser.getAttributeValue(null, "start") ?: return null
        val stopStr   = parser.getAttributeValue(null, "stop") ?: return null

        val startMs = parseDate(startStr) ?: return null
        val stopMs  = parseDate(stopStr) ?: return null

        var title = ""
        var description: String? = null
        var depth = 1

        while (depth > 0) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> {
                    depth++
                    when (parser.name) {
                        "title" -> title = parser.nextText()
                        "desc"  -> description = parser.nextText()
                    }
                }
                XmlPullParser.END_TAG -> depth--
            }
        }

        if (title.isBlank()) return null

        return EpgProgramme(
            channelId = channelId,
            title = title,
            startUtcMs = startMs,
            stopUtcMs = stopMs,
            description = description,
        )
    }

    private fun parseDate(value: String): Long? {
        // XMLTV dates can omit the timezone offset; assume UTC if absent
        val normalised = if (value.length == 14) "$value +0000" else value
        return try {
            DATE_FORMAT.parse(normalised)?.time
        } catch (_: ParseException) {
            null
        }
    }
}
