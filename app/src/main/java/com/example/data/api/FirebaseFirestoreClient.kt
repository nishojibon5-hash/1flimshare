package com.example.data.api

import android.util.Log
import com.example.data.model.Movie
import com.example.data.model.Advertisement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object FirebaseFirestoreClient {
    private const val TAG = "FirebaseFirestoreClient"
    
    private fun decodeB64(encoded: String): String {
        return try {
            String(android.util.Base64.decode(encoded, android.util.Base64.DEFAULT), Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }

    // Obfuscated string constants (decrypted dynamically at runtime to protect credentials against decompiler extraction)
    private const val OBF_PROJECT_ID = "ZmxpbXNoYXJlLWY2YjE1"
    private const val OBF_WORKER_URL = "aHR0cHM6Ly9nZHJpdmUtcHJveHkubmlzaG9qaWJvbjUud29ya2Vycy5kZXY="
    private const val OBF_FIRESTORE_PREFIX = "aHR0cHM6Ly9maXJlc3RvcmUuZ29vZ2xlYXBpcy5jb20vdjEvcHJvamVjdHMv"
    private const val OBF_FIRESTORE_SUFFIX = "L2RhdGFiYXNlcy8oZGVmYXVsdCkvZG9jdW1lbnRzLw=="

    // Default project ID configured for this app is 'flimshare-f6b15'
    var projectId: String = decodeB64(OBF_PROJECT_ID)
    
    // Cloudflare Worker URL for proxying Google Drive streams
    // Users can edit this in Settings inside the app
    var cloudflareWorkerUrl: String = decodeB64(OBF_WORKER_URL)

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private fun getBaseUrl(collection: String): String {
        val prefix = decodeB64(OBF_FIRESTORE_PREFIX)
        val suffix = decodeB64(OBF_FIRESTORE_SUFFIX)
        return "$prefix$projectId$suffix$collection"
    }

    private fun getDocUrl(collection: String, docId: String): String {
        val encodedId = URLEncoder.encode(docId, "UTF-8")
        return "${getBaseUrl(collection)}/$encodedId"
    }

    // --- Clean Production (No Local Dummy/Demo Cache) ---
    val backupMovies = emptyList<Movie>()

    val backupAds = emptyList<Advertisement>()

    // Helper to resolve standard direct streaming video URL from a file ID
    // If the file ID is an actual URL (like our backups), return it directly.
    // Otherwise, route it through the Cloudflare Worker proxy!
    fun getVideoStreamingUrl(fileId: String): String {
        if (fileId.startsWith("http://") || fileId.startsWith("https://") || fileId.startsWith("/") || fileId.startsWith("file://") || fileId.startsWith("content://")) {
            return fileId
        }
        val cleanId = fileId.trim()
        val proxyBase = cloudflareWorkerUrl.trim().removeSuffix("/")
        return "$proxyBase/stream?id=$cleanId"
    }

    private fun optStringField(fields: JSONObject, key: String, default: String = ""): String {
        val obj = fields.optJSONObject(key) ?: return default
        return obj.optString("stringValue", default)
    }

    private fun optBooleanField(fields: JSONObject, key: String, default: Boolean = false): Boolean {
        val obj = fields.optJSONObject(key) ?: return default
        return obj.optBoolean("booleanValue", default)
    }

    fun parseMovie(docJson: JSONObject): Movie? {
        val fields = docJson.optJSONObject("fields") ?: return null
        val id = docJson.optString("name", "").substringAfterLast("/")
        val title = optStringField(fields, "title")
        if (title.isEmpty()) return null
        
        val viewsStr = fields.optJSONObject("views")?.optString("integerValue") ?: ""
        val views = viewsStr.toIntOrNull() ?: 0
        
        val createdAtStr = fields.optJSONObject("createdAt")?.optString("integerValue") ?: ""
        val createdAt = createdAtStr.toLongOrNull() ?: System.currentTimeMillis()
        
        return Movie(
            id = id,
            title = title,
            description = optStringField(fields, "description", "No description available."),
            posterUrl = optStringField(fields, "posterUrl", "https://images.unsplash.com/photo-1536440136628-849c177e76a1"),
            genre = optStringField(fields, "genre", "General"),
            googleDriveFileId = optStringField(fields, "googleDriveFileId", ""),
            mirrorIds = optStringField(fields, "mirrorIds", ""),
            views = views,
            createdAt = createdAt
        )
    }

    fun parseAdvertisement(docJson: JSONObject): Advertisement? {
        val fields = docJson.optJSONObject("fields") ?: return null
        val id = docJson.optString("name", "").substringAfterLast("/")
        val videoUrl = optStringField(fields, "videoUrl")
        val htmlCode = optStringField(fields, "htmlCode")
        val isBanner = optBooleanField(fields, "isBanner", false)
        
        if (videoUrl.isEmpty() && htmlCode.isEmpty()) return null
        
        return Advertisement(
            id = id,
            videoUrl = videoUrl,
            clickThroughUrl = optStringField(fields, "clickThroughUrl", "https://www.google.com"),
            title = optStringField(fields, "title", "Sponsored Advertisement"),
            htmlCode = htmlCode,
            isBanner = isBanner
        )
    }

    // --- API Operations ---

    suspend fun checkIsAdminEmail(email: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = email.trim().lowercase()
            if (cleanEmail.isEmpty()) return@withContext false
            Log.d(TAG, "Querying Firestore to check if email is an admin: $cleanEmail")
            
            val request = Request.Builder()
                .url(getDocUrl("admins", cleanEmail))
                .get()
                .build()
            val response = client.newCall(request).execute()
            
            // If document exists, the response will be 200 OK. If it doesn't, it will be 404.
            val success = response.isSuccessful
            Log.d(TAG, "Admin status for $cleanEmail: $success (HTTP code: ${response.code})")
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error checking admin email: ${e.message}")
            false
        }
    }

    suspend fun fetchMovies(): List<Movie> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching movies from Firestore collection: 'movies' under project $projectId")
            val request = Request.Builder()
                .url(getBaseUrl("movies"))
                .get()
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful && response.body != null) {
                val bodyStr = response.body!!.string()
                val root = JSONObject(bodyStr)
                if (root.has("documents")) {
                    val documents = root.getJSONArray("documents")
                    val list = mutableListOf<Movie>()
                    for (i in 0 until documents.length()) {
                        val doc = documents.getJSONObject(i)
                        val movie = parseMovie(doc)
                        if (movie != null) {
                            list.add(movie)
                        }
                    }
                    Log.d(TAG, "Successfully fetched ${list.size} movies from Firestore REST")
                    if (list.isNotEmpty()) {
                        return@withContext list
                    }
                }
            } else {
                Log.e(TAG, "Failed fetchMovies: code=${response.code} response=${response.body?.string()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching movies: ${e.message}")
        }
        Log.w(TAG, "Returning backupMovies local catalog")
        return@withContext backupMovies
    }

    suspend fun fetchAdvertisements(): List<Advertisement> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching advertisements from Firestore collection: 'advertisements' under project $projectId")
            val request = Request.Builder()
                .url(getBaseUrl("advertisements"))
                .get()
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful && response.body != null) {
                val bodyStr = response.body!!.string()
                val root = JSONObject(bodyStr)
                if (root.has("documents")) {
                    val documents = root.getJSONArray("documents")
                    val list = mutableListOf<Advertisement>()
                    for (i in 0 until documents.length()) {
                        val doc = documents.getJSONObject(i)
                        val ad = parseAdvertisement(doc)
                        if (ad != null) {
                            list.add(ad)
                        }
                    }
                    Log.d(TAG, "Successfully fetched ${list.size} advertisements from Firestore REST")
                    if (list.isNotEmpty()) {
                        return@withContext list
                    }
                }
            } else {
                Log.e(TAG, "Failed fetchAdvertisements: code=${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching advertisements: ${e.message}")
        }
        Log.w(TAG, "Returning backupAds local catalog")
        return@withContext backupAds
    }

    // Helper to upload a movie to Firestore (for admin convenience if they want to publish directly in-app)
    suspend fun addMovieToCloud(movie: Movie): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            val fields = JSONObject().apply {
                put("title", JSONObject().put("stringValue", movie.title))
                put("description", JSONObject().put("stringValue", movie.description))
                put("posterUrl", JSONObject().put("stringValue", movie.posterUrl))
                put("genre", JSONObject().put("stringValue", movie.genre))
                put("googleDriveFileId", JSONObject().put("stringValue", movie.googleDriveFileId))
                put("mirrorIds", JSONObject().put("stringValue", movie.mirrorIds))
                put("views", JSONObject().put("integerValue", movie.views.toString()))
                put("createdAt", JSONObject().put("integerValue", movie.createdAt.toString()))
            }
            val payload = JSONObject().put("fields", fields).toString()
            val request = Request.Builder()
                .url(getDocUrl("movies", movie.id))
                .patch(payload.toRequestBody(JSON_MEDIA_TYPE))
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                return@withContext Pair(true, null)
            } else {
                val errorBody = response.body?.string() ?: ""
                Log.e(TAG, "Firestore write failed: HTTP ${response.code} - $errorBody")
                val friendlyMessage = try {
                    val errJson = JSONObject(errorBody)
                    val errorObj = errJson.optJSONObject("error")
                    errorObj?.optString("message", "HTTP ${response.code}") ?: "HTTP ${response.code}"
                } catch (e: Exception) {
                    "HTTP ${response.code}: $errorBody"
                }
                return@withContext Pair(false, friendlyMessage)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload movie: ${e.message}")
            return@withContext Pair(false, e.message)
        }
    }

    // Helper to increment view count on Firestore
    suspend fun incrementMovieViews(movie: Movie): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            val updatedViews = movie.views + 1
            val fields = JSONObject().apply {
                put("title", JSONObject().put("stringValue", movie.title))
                put("description", JSONObject().put("stringValue", movie.description))
                put("posterUrl", JSONObject().put("stringValue", movie.posterUrl))
                put("genre", JSONObject().put("stringValue", movie.genre))
                put("googleDriveFileId", JSONObject().put("stringValue", movie.googleDriveFileId))
                put("mirrorIds", JSONObject().put("stringValue", movie.mirrorIds))
                put("views", JSONObject().put("integerValue", updatedViews.toString()))
                put("createdAt", JSONObject().put("integerValue", movie.createdAt.toString()))
            }
            val payload = JSONObject().put("fields", fields).toString()
            val request = Request.Builder()
                .url(getDocUrl("movies", movie.id))
                .patch(payload.toRequestBody(JSON_MEDIA_TYPE))
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                return@withContext Pair(true, null)
            } else {
                val errorBody = response.body?.string() ?: ""
                Log.e(TAG, "Failed to increment views: code=${response.code} body=$errorBody")
                return@withContext Pair(false, "HTTP ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error incrementing views on cloud: ${e.message}")
            return@withContext Pair(false, e.message)
        }
    }

    // Helper to upload an advertisement to Firestore
    suspend fun addAdToCloud(ad: Advertisement): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            val fields = JSONObject().apply {
                put("videoUrl", JSONObject().put("stringValue", ad.videoUrl))
                put("clickThroughUrl", JSONObject().put("stringValue", ad.clickThroughUrl))
                put("title", JSONObject().put("stringValue", ad.title))
                put("htmlCode", JSONObject().put("stringValue", ad.htmlCode))
                put("isBanner", JSONObject().put("booleanValue", ad.isBanner))
            }
            val payload = JSONObject().put("fields", fields).toString()
            val request = Request.Builder()
                .url(getDocUrl("advertisements", ad.id))
                .patch(payload.toRequestBody(JSON_MEDIA_TYPE))
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                return@withContext Pair(true, null)
            } else {
                val errorBody = response.body?.string() ?: ""
                Log.e(TAG, "Firestore write failed: HTTP ${response.code} - $errorBody")
                val friendlyMessage = try {
                    val errJson = JSONObject(errorBody)
                    val errorObj = errJson.optJSONObject("error")
                    errorObj?.optString("message", "HTTP ${response.code}") ?: "HTTP ${response.code}"
                } catch (e: Exception) {
                    "HTTP ${response.code}: $errorBody"
                }
                return@withContext Pair(false, friendlyMessage)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload ad: ${e.message}")
            return@withContext Pair(false, e.message)
        }
    }

    // --- DELETION OPERATIONS FOR ADMIN CONTROL PANEL ---
    suspend fun deleteMovieFromCloud(id: String): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(getDocUrl("movies", id))
                .delete()
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful || response.code == 404) {
                return@withContext Pair(true, null)
            } else {
                return@withContext Pair(false, "HTTP ${response.code}")
            }
        } catch (e: Exception) {
            return@withContext Pair(false, e.message)
        }
    }

    suspend fun deleteAdFromCloud(id: String): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(getDocUrl("advertisements", id))
                .delete()
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful || response.code == 404) {
                return@withContext Pair(true, null)
            } else {
                return@withContext Pair(false, "HTTP ${response.code}")
            }
        } catch (e: Exception) {
            return@withContext Pair(false, e.message)
        }
    }

    // --- APP SETTINGS (COMING SOON & SPONSOR CONTROL) FOR ADMIN PANEL ---
    data class AppSettings(
        val comingSoonText: String = "",
        val comingSoonPosterUrl: String = "",
        val isComingSoonActive: Boolean = false,
        val sponsorText: String = "",
        val sponsorPosterUrl: String = "",
        val sponsorVideoUrl: String = "",
        val isSponsorActive: Boolean = false
    )

    suspend fun fetchAppSettings(): AppSettings = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(getDocUrl("app_settings", "general"))
                .get()
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful && response.body != null) {
                val bodyStr = response.body!!.string()
                val docJson = JSONObject(bodyStr)
                val fields = docJson.optJSONObject("fields")
                if (fields != null) {
                    val text = optStringField(fields, "comingSoonText", "")
                    val poster = optStringField(fields, "comingSoonPosterUrl", "")
                    val active = optBooleanField(fields, "isComingSoonActive", false)
                    val sText = optStringField(fields, "sponsorText", "")
                    val sPoster = optStringField(fields, "sponsorPosterUrl", "")
                    val sVideo = optStringField(fields, "sponsorVideoUrl", "")
                    val sActive = optBooleanField(fields, "isSponsorActive", false)
                    return@withContext AppSettings(text, poster, active, sText, sPoster, sVideo, sActive)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching app settings: ${e.message}")
        }
        return@withContext AppSettings()
    }

    suspend fun updateAppSettings(settings: AppSettings): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            val fields = JSONObject().apply {
                put("comingSoonText", JSONObject().put("stringValue", settings.comingSoonText))
                put("comingSoonPosterUrl", JSONObject().put("stringValue", settings.comingSoonPosterUrl))
                put("isComingSoonActive", JSONObject().put("booleanValue", settings.isComingSoonActive))
                put("sponsorText", JSONObject().put("stringValue", settings.sponsorText))
                put("sponsorPosterUrl", JSONObject().put("stringValue", settings.sponsorPosterUrl))
                put("sponsorVideoUrl", JSONObject().put("stringValue", settings.sponsorVideoUrl))
                put("isSponsorActive", JSONObject().put("booleanValue", settings.isSponsorActive))
            }
            val payload = JSONObject().put("fields", fields).toString()
            val request = Request.Builder()
                .url(getDocUrl("app_settings", "general"))
                .patch(payload.toRequestBody(JSON_MEDIA_TYPE))
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                return@withContext Pair(true, null)
            } else {
                return@withContext Pair(false, "HTTP ${response.code}")
            }
        } catch (e: Exception) {
            return@withContext Pair(false, e.message)
        }
    }
}
