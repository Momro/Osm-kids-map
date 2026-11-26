package com.momro.weltentdecker.data

import com.momro.weltentdecker.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class SearchResult(val name: String, val lat: Double, val lon: Double)

class NominatimRepository {
    suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://nominatim.openstreetmap.org/search" +
            "?format=jsonv2&limit=8&countrycodes=de&accept-language=de&q=$encoded"
        try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty(
                "User-Agent",
                "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME} (Welt-Entdecker Kinder-App)"
            )
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return@withContext emptyList()
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val arr = JSONArray(body)
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                val name = obj.optString("display_name")
                val lat = obj.optString("lat").toDoubleOrNull()
                val lon = obj.optString("lon").toDoubleOrNull()
                if (name.isBlank() || lat == null || lon == null) null else SearchResult(name, lat, lon)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
