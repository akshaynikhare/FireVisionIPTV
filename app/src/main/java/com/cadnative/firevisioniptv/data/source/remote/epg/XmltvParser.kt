package com.cadnative.firevisioniptv.data.source.remote.epg

import android.util.Xml
import com.cadnative.firevisioniptv.domain.model.EpgProgram
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Streaming XMLTV parser (pull-based, low memory — avoids holding the whole document
 * in memory the way the server does). Parses `<programme>` elements into [EpgProgram]s,
 * keyed by the XMLTV `channel` attribute (the tvg-id).
 */
class XmltvParser @Inject constructor() {

    private val basicFormat = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

    fun parse(input: InputStream): List<EpgProgram> {
        val programs = ArrayList<EpgProgram>()
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(input, null)

        var inProgramme = false
        var channel: String? = null
        var start: Instant? = null
        var end: Instant? = null
        var icon: String? = null
        var currentTag: String? = null
        val title = StringBuilder()
        val desc = StringBuilder()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "programme" -> {
                        inProgramme = true
                        channel = parser.getAttributeValue(null, "channel")?.trim()
                        start = parseXmltvTime(parser.getAttributeValue(null, "start"))
                        end = parseXmltvTime(parser.getAttributeValue(null, "stop"))
                        icon = null
                        title.setLength(0)
                        desc.setLength(0)
                    }
                    "title" -> if (inProgramme) currentTag = "title"
                    "desc" -> if (inProgramme) currentTag = "desc"
                    "icon" -> if (inProgramme && icon == null) {
                        icon = parser.getAttributeValue(null, "src")
                    }
                }
                XmlPullParser.TEXT -> if (inProgramme && currentTag != null) {
                    when (currentTag) {
                        "title" -> title.append(parser.text)
                        "desc" -> desc.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "programme" -> {
                        val ch = channel
                        val s = start
                        val e = end
                        if (ch != null && s != null && e != null && title.isNotBlank()) {
                            programs.add(
                                EpgProgram(
                                    channelEpgId = ch,
                                    title = title.toString().trim(),
                                    description = desc.toString().trim().ifEmpty { null },
                                    startTime = s,
                                    endTime = e,
                                    icon = icon
                                )
                            )
                        }
                        inProgramme = false
                        currentTag = null
                    }
                    "title", "desc" -> currentTag = null
                }
            }
            event = parser.next()
        }
        return programs
    }

    /** XMLTV time is `yyyyMMddHHmmss` optionally followed by a `+HHmm` offset. */
    private fun parseXmltvTime(raw: String?): Instant? {
        if (raw.isNullOrBlank()) return null
        return try {
            val parts = raw.trim().split(Regex("\\s+"), limit = 2)
            val local = LocalDateTime.parse(parts[0], basicFormat)
            val offset = if (parts.size > 1) ZoneOffset.of(parts[1]) else ZoneOffset.UTC
            local.toInstant(offset)
        } catch (_: Exception) {
            null
        }
    }
}
