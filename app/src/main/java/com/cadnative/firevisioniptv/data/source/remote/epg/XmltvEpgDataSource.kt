package com.cadnative.firevisioniptv.data.source.remote.epg

import com.cadnative.firevisioniptv.domain.model.EpgProgram
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import java.io.PushbackInputStream
import java.util.zip.GZIPInputStream
import javax.inject.Inject

/**
 * Fetches an XMLTV guide directly over HTTP and parses it on-device — the resilient
 * client-side model used by TiviMate and Kodi. Handles gzip-compressed guides
 * (`.xml.gz`) transparently by sniffing the gzip magic bytes.
 *
 * Returns an empty list on any failure so the caller keeps its last-good cache.
 */
class XmltvEpgDataSource @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val parser: XmltvParser
) {
    fun fetch(url: String): List<EpgProgram> {
        if (url.isBlank()) return emptyList()
        return try {
            val request = Request.Builder().url(url).get().build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val body = response.body ?: return emptyList()
                parser.parse(maybeGunzip(body.byteStream()))
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun maybeGunzip(raw: InputStream): InputStream {
        val pushback = PushbackInputStream(raw, 2)
        val signature = ByteArray(2)
        val read = pushback.read(signature, 0, 2)
        if (read > 0) pushback.unread(signature, 0, read)
        val isGzip = read == 2 &&
            signature[0] == 0x1f.toByte() &&
            signature[1] == 0x8b.toByte()
        return if (isGzip) GZIPInputStream(pushback) else pushback
    }
}
