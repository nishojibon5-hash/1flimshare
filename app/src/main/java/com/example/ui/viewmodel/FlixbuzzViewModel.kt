package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.FirebaseFirestoreClient
import com.example.data.model.Movie
import com.example.data.model.Advertisement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FlixbuzzViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "FlixbuzzViewModel"
    private val sharedPrefs = application.getSharedPreferences("flixbuzz_prefs", Context.MODE_PRIVATE)

    private val _movies = MutableStateFlow<List<Movie>>(emptyList())
    val movies: StateFlow<List<Movie>> = _movies.asStateFlow()

    private val _advertisements = MutableStateFlow<List<Advertisement>>(emptyList())
    val advertisements: StateFlow<List<Advertisement>> = _advertisements.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Active player states
    private val _selectedMovie = MutableStateFlow<Movie?>(null)
    val selectedMovie: StateFlow<Movie?> = _selectedMovie.asStateFlow()

    private val _isAdPlaying = MutableStateFlow(false)
    val isAdPlaying: StateFlow<Boolean> = _isAdPlaying.asStateFlow()

    private val _activeAd = MutableStateFlow<Advertisement?>(null)
    val activeAd: StateFlow<Advertisement?> = _activeAd.asStateFlow()

    // In-app editable configuration variables for user convenience
    private val _projectId = MutableStateFlow(FirebaseFirestoreClient.projectId)
    val projectId: StateFlow<String> = _projectId.asStateFlow()

    private val _cloudflareWorkerUrl = MutableStateFlow(FirebaseFirestoreClient.cloudflareWorkerUrl)
    val cloudflareWorkerUrl: StateFlow<String> = _cloudflareWorkerUrl.asStateFlow()

    private val _appSettings = MutableStateFlow<FirebaseFirestoreClient.AppSettings>(FirebaseFirestoreClient.AppSettings())
    val appSettings: StateFlow<FirebaseFirestoreClient.AppSettings> = _appSettings.asStateFlow()

    // --- Admin Authentication State ---
    private val _isAdminLoggedIn = MutableStateFlow(sharedPrefs.getBoolean("is_admin_logged_in", false))
    val isAdminLoggedIn: StateFlow<Boolean> = _isAdminLoggedIn.asStateFlow()

    private val _loggedInEmail = MutableStateFlow(sharedPrefs.getString("logged_in_admin_email", "") ?: "")
    val loggedInEmail: StateFlow<String> = _loggedInEmail.asStateFlow()

    private val _isOwnerAdmin = MutableStateFlow(
        sharedPrefs.getBoolean("is_admin_logged_in", false) && 
        (sharedPrefs.getString("logged_in_admin_email", "") ?: "").trim().lowercase() == "salman016500@gmail.com"
    )
    val isOwnerAdmin: StateFlow<Boolean> = _isOwnerAdmin.asStateFlow()

    fun setAdminLoggedIn(loggedIn: Boolean, email: String = "") {
        _isAdminLoggedIn.value = loggedIn
        _loggedInEmail.value = email
        _isOwnerAdmin.value = loggedIn && email.trim().lowercase() == "salman016500@gmail.com"
        sharedPrefs.edit()
            .putBoolean("is_admin_logged_in", loggedIn)
            .putString("logged_in_admin_email", email)
            .apply()
    }

    fun verifyAdminEmailOnFirestore(email: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val isOwner = email.trim().lowercase() == "salman016500@gmail.com"
                setAdminLoggedIn(true, email)
                onComplete(isOwner)
            } catch (e: Exception) {
                Log.e(TAG, "Error logging in email: ${e.message}")
                setAdminLoggedIn(true, email)
                onComplete(email.trim().lowercase() == "salman016500@gmail.com")
            } finally {
                _isLoading.value = false
            }
        }
    }

    init {
        // Load persisted config
        val persistedProjectId = sharedPrefs.getString("project_id", "flimshare-f6b15") ?: "flimshare-f6b15"
        val persistedWorkerUrl = sharedPrefs.getString("worker_url", "https://gdrive-proxy.nishojibon5.workers.dev") ?: "https://gdrive-proxy.nishojibon5.workers.dev"
        
        FirebaseFirestoreClient.projectId = persistedProjectId
        FirebaseFirestoreClient.cloudflareWorkerUrl = persistedWorkerUrl
        
        _projectId.value = persistedProjectId
        _cloudflareWorkerUrl.value = persistedWorkerUrl
        _appSettings.value = loadLocalSettings()

        // Initial catalog load
        refreshCatalog()
    }

    // --- SharedPreferences Helpers for Coming Soon & Sponsor Settings ---
    private fun loadLocalSettings(): FirebaseFirestoreClient.AppSettings {
        val text = sharedPrefs.getString("coming_soon_text", "Upcoming Release: Spider-Man 4! Coming this winter.") ?: "Upcoming Release: Spider-Man 4! Coming this winter."
        val poster = sharedPrefs.getString("coming_soon_poster", "https://images.unsplash.com/photo-1635805737707-575885ab0820?auto=format&fit=crop&w=800&q=80") ?: "https://images.unsplash.com/photo-1635805737707-575885ab0820?auto=format&fit=crop&w=800&q=80"
        val active = sharedPrefs.getBoolean("coming_soon_active", true)
        
        val sText = sharedPrefs.getString("sponsor_text", "Sponsor: Samsung Galaxy S26 Ultra - Epic, Just Like That.") ?: "Sponsor: Samsung Galaxy S26 Ultra - Epic, Just Like That."
        val sPoster = sharedPrefs.getString("sponsor_poster", "https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?auto=format&fit=crop&w=800&q=80") ?: "https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?auto=format&fit=crop&w=800&q=80"
        val sVideo = sharedPrefs.getString("sponsor_video", "") ?: ""
        val sActive = sharedPrefs.getBoolean("sponsor_active", false)
        
        return FirebaseFirestoreClient.AppSettings(text, poster, active, sText, sPoster, sVideo, sActive)
    }

    private fun saveLocalSettings(settings: FirebaseFirestoreClient.AppSettings) {
        sharedPrefs.edit()
            .putString("coming_soon_text", settings.comingSoonText)
            .putString("coming_soon_poster", settings.comingSoonPosterUrl)
            .putBoolean("coming_soon_active", settings.isComingSoonActive)
            .putString("sponsor_text", settings.sponsorText)
            .putString("sponsor_poster", settings.sponsorPosterUrl)
            .putString("sponsor_video", settings.sponsorVideoUrl)
            .putBoolean("sponsor_active", settings.isSponsorActive)
            .apply()
    }

    // --- SharedPreferences Helpers for local Offline/Instant-Play Fallbacks ---
    
    private fun saveLocalMovies(moviesList: List<Movie>) {
        try {
            val jsonArray = org.json.JSONArray()
            for (movie in moviesList) {
                val obj = org.json.JSONObject().apply {
                    put("id", movie.id)
                    put("title", movie.title)
                    put("description", movie.description)
                    put("posterUrl", movie.posterUrl)
                    put("genre", movie.genre)
                    put("googleDriveFileId", movie.googleDriveFileId)
                    put("mirrorIds", movie.mirrorIds)
                    put("views", movie.views)
                    put("createdAt", movie.createdAt)
                }
                jsonArray.put(obj)
            }
            sharedPrefs.edit().putString("local_movies", jsonArray.toString()).apply()
            Log.d(TAG, "Saved ${moviesList.size} custom movies locally.")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving local movies: ${e.message}")
        }
    }

    private fun loadLocalMovies(): List<Movie> {
        val list = mutableListOf<Movie>()
        val jsonStr = sharedPrefs.getString("local_movies", null) ?: return emptyList()
        try {
            val jsonArray = org.json.JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    Movie(
                        id = obj.optString("id"),
                        title = obj.optString("title"),
                        description = obj.optString("description"),
                        posterUrl = obj.optString("posterUrl"),
                        genre = obj.optString("genre"),
                        googleDriveFileId = obj.optString("googleDriveFileId"),
                        mirrorIds = obj.optString("mirrorIds"),
                        views = obj.optInt("views", 0),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading local movies: ${e.message}")
        }
        return list
    }

    private fun saveLocalAds(adsList: List<Advertisement>) {
        try {
            val jsonArray = org.json.JSONArray()
            for (ad in adsList) {
                val obj = org.json.JSONObject().apply {
                    put("id", ad.id)
                    put("videoUrl", ad.videoUrl)
                    put("clickThroughUrl", ad.clickThroughUrl)
                    put("title", ad.title)
                    put("htmlCode", ad.htmlCode)
                    put("isBanner", ad.isBanner)
                }
                jsonArray.put(obj)
            }
            sharedPrefs.edit().putString("local_ads", jsonArray.toString()).apply()
            Log.d(TAG, "Saved ${adsList.size} custom ads locally.")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving local ads: ${e.message}")
        }
    }

    private fun loadLocalAds(): List<Advertisement> {
        val list = mutableListOf<Advertisement>()
        val jsonStr = sharedPrefs.getString("local_ads", null) ?: return emptyList()
        try {
            val jsonArray = org.json.JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    Advertisement(
                        id = obj.optString("id"),
                        videoUrl = obj.optString("videoUrl"),
                        clickThroughUrl = obj.optString("clickThroughUrl"),
                        title = obj.optString("title"),
                        htmlCode = obj.optString("htmlCode", ""),
                        isBanner = obj.optBoolean("isBanner", false)
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading local ads: ${e.message}")
        }
        return list
    }

    fun refreshCatalog() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d(TAG, "Refreshing OTT Catalog...")
                val fetchedMovies = FirebaseFirestoreClient.fetchMovies()
                val fetchedAds = FirebaseFirestoreClient.fetchAdvertisements()
                val fetchedSettings = FirebaseFirestoreClient.fetchAppSettings()
                
                // Merge cloud movies with our local saved movies
                val localMovies = loadLocalMovies()
                val mergedMovies = (fetchedMovies + localMovies).distinctBy { it.id }
                
                // Merge cloud ads with our local saved ads
                val localAds = loadLocalAds()
                val mergedAds = (fetchedAds + localAds).distinctBy { it.id }
                
                _movies.value = mergedMovies
                _advertisements.value = mergedAds
                
                saveLocalSettings(fetchedSettings)
                _appSettings.value = fetchedSettings
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh catalog: ${e.message}")
                // Fallback to local high-quality backups plus custom saved items
                val localMovies = loadLocalMovies()
                _movies.value = (FirebaseFirestoreClient.backupMovies + localMovies).distinctBy { it.id }
                
                val localAds = loadLocalAds()
                _advertisements.value = (FirebaseFirestoreClient.backupAds + localAds).distinctBy { it.id }
                _appSettings.value = loadLocalSettings()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectMovie(movie: Movie) {
        viewModelScope.launch {
            // Increment views locally and trigger update
            val updatedMovie = movie.copy(views = movie.views + 1)
            _selectedMovie.value = updatedMovie
            
            // Update the movies list state Flow to reflect views count change immediately
            _movies.value = _movies.value.map { 
                if (it.id == movie.id) updatedMovie else it 
            }
            
            // If it is in the local persistent list, update it there too
            val localMovies = loadLocalMovies().toMutableList()
            val localIndex = localMovies.indexOfFirst { it.id == movie.id }
            if (localIndex != -1) {
                localMovies[localIndex] = updatedMovie
                saveLocalMovies(localMovies)
            }

            // Asynchronously increment on Firestore
            launch {
                try {
                    FirebaseFirestoreClient.incrementMovieViews(movie)
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing view count to Cloud: ${e.message}")
                }
            }
            
            // Start movie streaming directly. Mid-roll ads are handled in the player overlay itself!
            _activeAd.value = null
            _isAdPlaying.value = false
            Log.d(TAG, "Selected movie: ${movie.title}, launching player directly.")
        }
    }

    fun triggerAd(ad: Advertisement) {
        _activeAd.value = ad
        _isAdPlaying.value = true
        Log.d(TAG, "Triggered ad playback for: ${ad.title}")
    }

    fun skipAd() {
        _isAdPlaying.value = false
        Log.d(TAG, "Ad skipped or finished, starting main movie stream.")
    }

    fun closePlayer() {
        _selectedMovie.value = null
        _activeAd.value = null
        _isAdPlaying.value = false
        Log.d(TAG, "Player closed.")
    }

    fun updateConfig(newProjectId: String, newWorkerUrl: String) {
        val cleanId = newProjectId.trim()
        val cleanUrl = newWorkerUrl.trim()
        val editor = sharedPrefs.edit()
        
        if (cleanId.isNotEmpty()) {
            FirebaseFirestoreClient.projectId = cleanId
            _projectId.value = cleanId
            editor.putString("project_id", cleanId)
        }
        if (cleanUrl.isNotEmpty()) {
            FirebaseFirestoreClient.cloudflareWorkerUrl = cleanUrl
            _cloudflareWorkerUrl.value = cleanUrl
            editor.putString("worker_url", cleanUrl)
        }
        editor.apply()
        Log.d(TAG, "Updated and persisted config: Firestore Project=$cleanId, Cloudflare=$cleanUrl")
        refreshCatalog()
    }

    // Admin helpers to push custom data to Firestore in-app or add locally
    fun addNewMovie(movie: Movie, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val (success, errorMsg) = FirebaseFirestoreClient.addMovieToCloud(movie)
            
            // Always save to the local persist list so the movie is immediately available and survives restarts
            val currentLocal = loadLocalMovies().toMutableList()
            // Avoid duplicates
            val existingIndex = currentLocal.indexOfFirst { it.id == movie.id }
            if (existingIndex != -1) {
                currentLocal[existingIndex] = movie
            } else {
                currentLocal.add(movie)
            }
            saveLocalMovies(currentLocal)
            
            refreshCatalog()
            onComplete(success, errorMsg)
        }
    }

    fun addNewAd(ad: Advertisement, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val (success, errorMsg) = FirebaseFirestoreClient.addAdToCloud(ad)
            
            // Always save to the local persist list so the ad is immediately available and survives restarts
            val currentLocal = loadLocalAds().toMutableList()
            val existingIndex = currentLocal.indexOfFirst { it.id == ad.id }
            if (existingIndex != -1) {
                currentLocal[existingIndex] = ad
            } else {
                currentLocal.add(ad)
            }
            saveLocalAds(currentLocal)
            
            refreshCatalog()
            onComplete(success, errorMsg)
        }
    }

    // --- DELETION OPERATIONS ---
    fun deleteMovie(id: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val (success, errorMsg) = FirebaseFirestoreClient.deleteMovieFromCloud(id)
            
            val currentLocal = loadLocalMovies().toMutableList()
            val removed = currentLocal.removeAll { it.id == id }
            if (removed) {
                saveLocalMovies(currentLocal)
            }
            
            refreshCatalog()
            onComplete(success, errorMsg)
        }
    }

    fun deleteAd(id: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val (success, errorMsg) = FirebaseFirestoreClient.deleteAdFromCloud(id)
            
            val currentLocal = loadLocalAds().toMutableList()
            val removed = currentLocal.removeAll { it.id == id }
            if (removed) {
                saveLocalAds(currentLocal)
            }
            
            refreshCatalog()
            onComplete(success, errorMsg)
        }
    }

    // --- UPDATE SETTINGS ---
    fun updateComingSoon(text: String, posterUrl: String, active: Boolean, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val current = _appSettings.value
            val settings = FirebaseFirestoreClient.AppSettings(
                comingSoonText = text,
                comingSoonPosterUrl = posterUrl,
                isComingSoonActive = active,
                sponsorText = current.sponsorText,
                sponsorPosterUrl = current.sponsorPosterUrl,
                sponsorVideoUrl = current.sponsorVideoUrl,
                isSponsorActive = current.isSponsorActive
            )
            val (success, errorMsg) = FirebaseFirestoreClient.updateAppSettings(settings)
            
            saveLocalSettings(settings)
            _appSettings.value = settings
            
            refreshCatalog()
            onComplete(success, errorMsg)
        }
    }

    fun updateSponsor(text: String, posterUrl: String, videoUrl: String, active: Boolean, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val current = _appSettings.value
            val settings = FirebaseFirestoreClient.AppSettings(
                comingSoonText = current.comingSoonText,
                comingSoonPosterUrl = current.comingSoonPosterUrl,
                isComingSoonActive = current.isComingSoonActive,
                sponsorText = text,
                sponsorPosterUrl = posterUrl,
                sponsorVideoUrl = videoUrl,
                isSponsorActive = active
            )
            val (success, errorMsg) = FirebaseFirestoreClient.updateAppSettings(settings)
            
            saveLocalSettings(settings)
            _appSettings.value = settings
            
            refreshCatalog()
            onComplete(success, errorMsg)
        }
    }
}
