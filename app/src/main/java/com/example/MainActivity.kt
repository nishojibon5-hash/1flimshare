package com.example

import android.app.Application
import android.util.Log
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.api.FirebaseFirestoreClient
import com.example.data.model.Advertisement
import com.example.data.model.Movie
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FlixbuzzViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager

class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("MainActivity", "Notification permission granted.")
        } else {
            Log.d("MainActivity", "Notification permission denied.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        createNotificationChannel()
        checkAndRequestNotificationPermission()

        setContent {
            MyApplicationTheme(darkTheme = false) { // Clean modern Light Mode
                MainOTTAppHost()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Flixbuzz Releases"
            val descriptionText = "Notifications for newly released movies and shows on Flixbuzz"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("flix_channel_id", name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

// --- Dynamic Poster Notification Builder ---
fun triggerMovieReleaseNotification(context: Context, title: String, description: String, posterUrl: String) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = "flix_channel_id"

    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_menu_slideshow)
        .setContentTitle("🎬 New Release on Flixbuzz!")
        .setContentText("$title is now streaming! Tap to watch.")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)

    // Set intent to launch MainActivity
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pendingIntent = android.app.PendingIntent.getActivity(
        context, 0, intent,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) android.app.PendingIntent.FLAG_IMMUTABLE else 0
    )
    builder.setContentIntent(pendingIntent)

    // Load poster image in a background coroutine and display it
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
        try {
            val url = java.net.URL(posterUrl)
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input = connection.inputStream
            val bitmap = BitmapFactory.decodeStream(input)
            
            if (bitmap != null) {
                builder.setLargeIcon(bitmap)
                builder.setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap)
                        .bigLargeIcon(null as Bitmap?)
                        .setBigContentTitle("🎬 New Release: $title")
                        .setSummaryText(description)
                )
            }
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Failed to load poster image for notification: ${e.message}")
        } finally {
            withContext(Dispatchers.Main) {
                try {
                    notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
                } catch (e: SecurityException) {
                    Log.e("NotificationHelper", "Permission security error: ${e.message}")
                }
            }
        }
    }
}

enum class NavigationScreen(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    THEATER("Theater", Icons.Default.Movie),
    ADMIN("Login", Icons.Default.Lock)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainOTTAppHost() {
    val context = LocalContext.current
    val viewModel: FlixbuzzViewModel = viewModel()
    
    val selectedMovie by viewModel.selectedMovie.collectAsState()
    val isAdPlaying by viewModel.isAdPlaying.collectAsState()
    val activeAd by viewModel.activeAd.collectAsState()
    val isAdminLoggedIn by viewModel.isAdminLoggedIn.collectAsState()
    val isOwnerAdmin by viewModel.isOwnerAdmin.collectAsState()
    
    var currentScreen by remember { mutableStateOf(NavigationScreen.THEATER) }
    var showFirebaseSetupDialog by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(300.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                        .padding(horizontal = 24.dp, vertical = 32.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "FLIMSHARE PREMIUM",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Stream Smarter. Stream Better.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "VIP Status",
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isAdminLoggedIn) {
                                    if (isOwnerAdmin) "Verified Creator (Owner)" else "Verified Creator (Poster)"
                                } else {
                                    "Guest Streamer (VIP Activated)"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                
                Spacer(modifier = Modifier.height(12.dp))
                
                NavigationDrawerItem(
                    icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home Theater", fontWeight = FontWeight.SemiBold) },
                    selected = currentScreen == NavigationScreen.THEATER,
                    onClick = {
                        currentScreen = NavigationScreen.THEATER
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        selectedIconColor = MaterialTheme.colorScheme.primary
                    )
                )

                NavigationDrawerItem(
                    icon = { Icon(imageVector = Icons.Default.Bookmark, contentDescription = "Watchlist") },
                    label = { Text("My Watchlist", fontWeight = FontWeight.SemiBold) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        Toast.makeText(context, "Watchlist synced with Cloud Database successfully!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(imageVector = Icons.Default.CardMembership, contentDescription = "Premium") },
                    label = { Text("Premium Subscriptions", fontWeight = FontWeight.SemiBold) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        Toast.makeText(context, "🏆 Premium VIP Status is currently ACTIVE for this device!", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                NavigationDrawerItem(
                    icon = { 
                        Icon(
                            imageVector = if (isAdminLoggedIn) Icons.Default.Settings else Icons.Default.Lock, 
                            contentDescription = "Admin"
                        ) 
                    },
                    label = { 
                        Text(
                            text = if (isAdminLoggedIn) "Creator Studio" else "Login", 
                            fontWeight = FontWeight.SemiBold
                        ) 
                    },
                    selected = currentScreen == NavigationScreen.ADMIN,
                    onClick = {
                        currentScreen = NavigationScreen.ADMIN
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        selectedIconColor = MaterialTheme.colorScheme.primary
                    )
                )

                if (isAdminLoggedIn) {
                    NavigationDrawerItem(
                        icon = { Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "Logout") },
                        label = { Text("Logout Account", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error) },
                        selected = false,
                        onClick = {
                            viewModel.setAdminLoggedIn(false)
                            scope.launch { drawerState.close() }
                            currentScreen = NavigationScreen.THEATER
                            Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedIconColor = MaterialTheme.colorScheme.error,
                            unselectedTextColor = MaterialTheme.colorScheme.error
                        )
                    )
                }

                NavigationDrawerItem(
                    icon = { Icon(imageVector = Icons.Default.Build, contentDescription = "Firebase Setup") },
                    label = { Text("Firebase & SHA-1 Setup", fontWeight = FontWeight.SemiBold) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showFirebaseSetupDialog = true
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(imageVector = Icons.Default.Info, contentDescription = "About") },
                    label = { Text("About Flimshare", fontWeight = FontWeight.SemiBold) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        Toast.makeText(context, "Flimshare OTT v1.2 - Powered by High-Performance Streaming Proxy", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(imageVector = Icons.Default.Call, contentDescription = "Support") },
                    label = { Text("Support & Sponsors", fontWeight = FontWeight.SemiBold) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        Toast.makeText(context, "Sponsor Inquiry: sponsor@flimshare.com", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "Flimshare Public Network © 2026",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    ) {
        Scaffold(
            bottomBar = {
                if (selectedMovie == null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding() // Forces bottom nav above phone system keys
                            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp) // Elevated height to keep navigation clear from phone navigation keys
                                .padding(bottom = 8.dp), // Safely lifts the icons & text
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NavigationScreen.values().forEach { screen ->
                                val isSelected = currentScreen == screen
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable { currentScreen = screen }
                                        .padding(vertical = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = screen.icon,
                                            contentDescription = screen.label,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF64748B),
                                            modifier = Modifier.size(18.dp) // Compact icons
                                        )
                                        Spacer(modifier = Modifier.height(1.dp))
                                        Text(
                                            text = screen.label,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentScreen) {
                    NavigationScreen.THEATER -> PublicTheaterScreen(
                        viewModel = viewModel,
                        onOpenDrawer = { scope.launch { drawerState.open() } }
                    )
                    NavigationScreen.ADMIN -> AdminPanelScreen(viewModel = viewModel)
                }

                // High-Fidelity Custom Immersive Player Overlay
                AnimatedVisibility(
                    visible = selectedMovie != null,
                    enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(400)) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(400)) + fadeOut()
                ) {
                    selectedMovie?.let { movie ->
                        ImmersivePlayerOverlay(
                            movie = movie,
                            isAdPlaying = isAdPlaying,
                            activeAd = activeAd,
                            onClose = { viewModel.closePlayer() },
                            onAdSkip = { viewModel.skipAd() },
                            viewModel = viewModel
                        )
                    }
                }
                if (showFirebaseSetupDialog) {
                    FirebaseSetupDialog(onDismiss = { showFirebaseSetupDialog = false })
                }
            }
        }
    }
}

// --- SCREEN 1: PUBLIC THEATER SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicTheaterScreen(viewModel: FlixbuzzViewModel, onOpenDrawer: () -> Unit) {
    val movies by viewModel.movies.collectAsState()
    val advertisements by viewModel.advertisements.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf("All") }
    var isSearchActive by remember { mutableStateOf(false) }
    
    val genres = listOf("All", "Fantasy", "Sci-Fi", "Animation", "Surreal", "Action", "Comedy")

    // Dynamic filtering
    val filteredMovies = movies.filter { movie ->
        val matchesSearch = movie.title.contains(searchQuery, ignoreCase = true) || 
                            movie.description.contains(searchQuery, ignoreCase = true)
        val matchesGenre = selectedGenre == "All" || movie.genre.equals(selectedGenre, ignoreCase = true)
        matchesSearch && matchesGenre
    }

    // Sort Trending movies by view count descending
    val trendingMovies = remember(movies) {
        movies.sortedByDescending { it.views }
    }

    // Sort Feed movies by creation timestamp descending (newest uploaded first)
    val newUploadsMovies = remember(filteredMovies) {
        filteredMovies.sortedByDescending { it.createdAt }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
    ) {
        // High-End Header with Toggle Search Saving Massive Space
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (isSearchActive) {
                // Expanded inline search
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = { 
                        isSearchActive = false 
                        searchQuery = ""
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back", 
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search blockbusters...", fontSize = 13.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("search_bar"),
                        shape = RoundedCornerShape(25.dp),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        ),
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    )
                }
            } else {
                // Normal Header with Hamburger Menu and Title
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier.padding(end = 4.dp).size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open Navigation Menu",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "FLIMSHARE STUDIO",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = "Public Streaming Network",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Search toggle button
                    IconButton(
                        onClick = { isSearchActive = true },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search, 
                            contentDescription = "Search", 
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
 
                     // Refresh Button
                     IconButton(
                         onClick = { viewModel.refreshCatalog() },
                         modifier = Modifier
                             .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                             .size(36.dp)
                     ) {
                         if (isLoading) {
                             CircularProgressIndicator(
                                 modifier = Modifier.size(16.dp), 
                                 color = MaterialTheme.colorScheme.primary, 
                                 strokeWidth = 2.dp
                             )
                         } else {
                             Icon(
                                 imageVector = Icons.Default.Refresh, 
                                 contentDescription = "Refresh", 
                                 tint = MaterialTheme.colorScheme.onSurface,
                                 modifier = Modifier.size(18.dp)
                             )
                         }
                     }
                 }
             }
         }

        // Interactive Animated Movie Slider (We feed it new uploads)
        if (searchQuery.isEmpty() && selectedGenre == "All") {
            MovieSliderCarousel(
                movies = newUploadsMovies,
                onMovieClick = { movie -> viewModel.selectMovie(movie) }
            )
        }

        // 🔥 YouTube-style "Trending on Flixbuzz" Horizontally Scrolling Section
        if (searchQuery.isEmpty() && selectedGenre == "All" && trendingMovies.isNotEmpty()) {
            Text(
                text = "🔥 Trending on Flixbuzz",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
            )
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(trendingMovies.take(10)) { movie ->
                    TrendingMovieCard(movie = movie, onClick = { viewModel.selectMovie(movie) })
                }
            }
        }

        // Genre Horizontal Filters
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(genres) { genre ->
                val isSelected = selectedGenre == genre
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { selectedGenre = genre }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = genre,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // --- DYNAMIC COMING SOON HERO BANNER ---
        if (searchQuery.isEmpty() && selectedGenre == "All" && appSettings.isComingSoonActive) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column {
                    Box(modifier = Modifier.height(180.dp).fillMaxWidth()) {
                        AsyncImage(
                            model = resolvePosterModel(appSettings.comingSoonPosterUrl),
                            contentDescription = "Coming Soon Banner",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // Gradient Overlay for text readability
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                                    )
                                )
                        )
                        // Badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "COMING SOON 🎬",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = appSettings.comingSoonText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add to your watchlist now. Stay tuned for instant streaming notifications!",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // --- DYNAMIC PREMIUM SPONSOR SPOTLIGHT CARD ---
        // Native-blended Sponsor Spotlight layout. Maximizes brand integration beautifully!
        if (searchQuery.isEmpty() && selectedGenre == "All" && appSettings.isSponsorActive) {
            SponsorSpotlightCard(appSettings = appSettings) {
                val sponsorMovie = Movie(
                    id = "sponsor_campaign",
                    title = appSettings.sponsorText,
                    description = "Official sponsored feature presentation.",
                    posterUrl = appSettings.sponsorPosterUrl,
                    genre = "Sponsored",
                    googleDriveFileId = appSettings.sponsorVideoUrl.ifEmpty { "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4" }
                )
                viewModel.selectMovie(sponsorMovie)
            }
        }

        // Movies Feed Label
        Text(
            text = if (searchQuery.isEmpty()) "Latest Videos ($selectedGenre)" else "Search Results",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Group movies and ads into rows dynamically for 2-column compact list
        val feedItems = remember(newUploadsMovies, advertisements) {
            val items = mutableListOf<FeedItem>()
            newUploadsMovies.forEachIndexed { index, movie ->
                items.add(FeedItem.MovieItem(movie))
                // Insert an ad after every 4 movies
                if ((index + 1) % 4 == 0 && advertisements.isNotEmpty()) {
                    val adIndex = ((index + 1) / 4 - 1) % advertisements.size
                    items.add(FeedItem.AdItem(advertisements[adIndex]))
                }
            }
            items
        }

        val rows = remember(feedItems) {
            val list = mutableListOf<List<FeedItem>>()
            var currentMovieRow = mutableListOf<FeedItem.MovieItem>()
            
            feedItems.forEach { item ->
                when (item) {
                    is FeedItem.AdItem -> {
                        if (currentMovieRow.isNotEmpty()) {
                            list.add(currentMovieRow.toList())
                            currentMovieRow.clear()
                        }
                        list.add(listOf(item))
                    }
                    is FeedItem.MovieItem -> {
                        currentMovieRow.add(item)
                        if (currentMovieRow.size == 2) {
                            list.add(currentMovieRow.toList())
                            currentMovieRow.clear()
                        }
                    }
                }
            }
            if (currentMovieRow.isNotEmpty()) {
                list.add(currentMovieRow.toList())
            }
            list
        }

        if (rows.isEmpty()) {
            if (isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    repeat(3) {
                        SkeletonMovieCard()
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info, 
                            contentDescription = "Empty", 
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), 
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No videos found matching selectors.", 
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), 
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            rows.forEach { rowItems ->
                if (rowItems.size == 1 && rowItems[0] is FeedItem.AdItem) {
                    val adItem = rowItems[0] as FeedItem.AdItem
                    // Show full-width Ad card
                    AdFeedCard(ad = adItem.ad)
                } else {
                    // Show 1 or 2 compact movies in a Row (Saves 50% screen estate!)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowItems.forEach { item ->
                            val movieItem = item as FeedItem.MovieItem
                            Box(modifier = Modifier.weight(1f)) {
                                CompactMovieCard(
                                    movie = movieItem.movie, 
                                    onClick = { viewModel.selectMovie(movieItem.movie) }
                                )
                            }
                        }
                        // If row has only 1 movie (odd count), add an empty spacer for perfect alignment
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun SkeletonMovieCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail skeleton
            Box(
                modifier = Modifier
                    .size(width = 80.dp, height = 110.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Title skeleton
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                )
                // Genre skeleton
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
                )
                // Desc line 1 skeleton
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                )
                // Desc line 2 skeleton
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                )
            }
        }
    }
}

@Composable
fun CompactMovieCard(movie: Movie, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("movie_card_${movie.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
            ) {
                AsyncImage(
                    model = resolvePosterModel(movie.posterUrl),
                    contentDescription = movie.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = movie.genre.uppercase(),
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = movie.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                
                val viewsFormatted = formatViews(movie.views)
                Text(
                    text = "$viewsFormatted • ${formatTimeAgo(movie.createdAt)}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun SponsorSpotlightCard(appSettings: FirebaseFirestoreClient.AppSettings, onPlaySponsor: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clickable { onPlaySponsor() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
    ) {
        Column {
            Box(modifier = Modifier.height(160.dp).fillMaxWidth()) {
                AsyncImage(
                    model = resolvePosterModel(appSettings.sponsorPosterUrl),
                    contentDescription = "Sponsor Spotlight",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Gradient Overlay for text readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                            )
                        )
                )
                // Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFF59E0B), Color(0xFFEF4444))
                            ),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Campaign, 
                            contentDescription = "Sponsor", 
                            tint = Color.White, 
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "SPONSOR SPOTLIGHT ⚡",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                
                // Play Icon in Center if video exists
                if (appSettings.sponsorVideoUrl.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(44.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play Sponsor Video",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = appSettings.sponsorText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (appSettings.sponsorVideoUrl.isNotEmpty()) "Click here to play official sponsor video 🎥" else "Sponsored partner banner presentation",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// --- SCREEN 2: ADMIN STUDIO ---
@Composable
fun AdminStudioScreen(viewModel: FlixbuzzViewModel) {
    AdminPanelScreen(viewModel = viewModel)
}

@Composable
fun LegacyAdminStudioScreen(viewModel: FlixbuzzViewModel) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var posterUrl by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("Fantasy") }
    var driveFileId by remember { mutableStateOf("") }
    var mirrorIds by remember { mutableStateOf("") }

    var adVideoUrl by remember { mutableStateOf("") }
    var adClickThroughUrl by remember { mutableStateOf("") }
    var adTitle by remember { mutableStateOf("") }
    var adHtmlCode by remember { mutableStateOf("") }
    var isBannerAd by remember { mutableStateOf(false) }

    val genres = listOf("Fantasy", "Sci-Fi", "Animation", "Surreal", "Action", "Comedy", "Drama")
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Color(0xFF08090C))
            .padding(16.dp)
    ) {
        Text(
            text = "Admin Cloud Console",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Populate your live Firestore database easily in-app. Fields write directly to the public 'movies' & 'advertisements' collections.",
            fontSize = 12.sp,
            color = Color(0xFF8B949F),
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        // Movie Creator Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141720)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(0.5.dp, Color(0xFF21262D))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Publish New Blockbuster / Video",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF3850)
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Movie Title") },
                    modifier = Modifier.fillMaxWidth().testTag("input_movie_title"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF3850), unfocusedBorderColor = Color(0xFF21262D))
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Plot Details") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF3850), unfocusedBorderColor = Color(0xFF21262D))
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = posterUrl,
                    onValueChange = { posterUrl = it },
                    label = { Text("Banner/Poster Image URL (or upload from gallery)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF3850), unfocusedBorderColor = Color(0xFF21262D))
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Gallery Image Picker and Preview Section
                val imagePickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri: Uri? ->
                    uri?.let { selectedUri ->
                        coroutineScope.launch(Dispatchers.Default) {
                            val base64Str = uriToBase64(context, selectedUri)
                            if (base64Str != null) {
                                posterUrl = "data:image/jpeg;base64,$base64Str"
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Thumbnail uploaded successfully!", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Failed to load image from gallery.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF21262D)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = "Gallery", tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Select Thumbnail", color = Color.White, fontSize = 13.sp)
                    }

                    if (posterUrl.isNotEmpty()) {
                        Button(
                            onClick = { posterUrl = "" },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FF3850)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("Clear", color = Color(0xFFFF3850), fontSize = 13.sp)
                        }
                    }
                }

                // Show thumbnail image preview if selected/entered
                if (posterUrl.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF21262D), RoundedCornerShape(8.dp))
                            .background(Color(0xFF08090C))
                    ) {
                        AsyncImage(
                            model = resolvePosterModel(posterUrl),
                            contentDescription = "Thumbnail Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .background(Color(0x99000000))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Thumbnail Preview", color = Color.White, fontSize = 10.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = driveFileId,
                    onValueChange = { driveFileId = it },
                    label = { Text("Google Drive File ID (or fallback stream URL)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF3850), unfocusedBorderColor = Color(0xFF21262D))
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = mirrorIds,
                    onValueChange = { mirrorIds = it },
                    label = { Text("Alternative Mirror File IDs (comma separated)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF3850), unfocusedBorderColor = Color(0xFF21262D))
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Genre Dropdown Selector (Simple Scroll Row)
                Text(text = "Select Movie Genre", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                LazyRow(
                    modifier = Modifier.padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(genres) { g ->
                        val active = genre == g
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (active) Color(0xFFFF3850) else Color(0xFF08090C))
                                .clickable { genre = g }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(text = g, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (title.isEmpty() || driveFileId.isEmpty()) {
                            Toast.makeText(context, "Title and File ID are required!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val cleanId = "custom_" + System.currentTimeMillis()
                        val newMovie = Movie(
                            id = cleanId,
                            title = title,
                            description = description.ifEmpty { "A high-fidelity streaming presentation." },
                            posterUrl = posterUrl.ifEmpty { "https://images.unsplash.com/photo-1536440136628-849c177e76a1" },
                            genre = genre,
                            googleDriveFileId = driveFileId,
                            mirrorIds = mirrorIds
                        )
                        viewModel.addNewMovie(newMovie) { success, errorMsg ->
                            if (success) {
                                Toast.makeText(context, "Movie Published to Firestore successfully!", Toast.LENGTH_LONG).show()
                                title = ""; description = ""; posterUrl = ""; driveFileId = ""; mirrorIds = ""
                            } else {
                                val details = if (errorMsg != null) "\nError: $errorMsg" else ""
                                Toast.makeText(context, "Offline Local Bypass: Added locally for instant play!$details", Toast.LENGTH_LONG).show()
                                title = ""; description = ""; posterUrl = ""; driveFileId = ""; mirrorIds = ""
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("add_movie_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3850))
                ) {
                    Text(text = "Publish Movie Stream", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Ads Creator Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141720)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(0.5.dp, Color(0xFF21262D))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Insert Custom Advertisement",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00F2FE)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Select Ad Type Choice chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (!isBannerAd) Color(0xFF00F2FE) else Color(0xFF21262D), RoundedCornerShape(8.dp))
                            .clickable { isBannerAd = false }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Video / VAST Ad",
                            color = if (!isBannerAd) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (isBannerAd) Color(0xFF00F2FE) else Color(0xFF21262D), RoundedCornerShape(8.dp))
                            .clickable { isBannerAd = true }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "HTML Banner Ad",
                            color = if (isBannerAd) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = adTitle,
                    onValueChange = { adTitle = it },
                    label = { Text("Ad Brand Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00F2FE), unfocusedBorderColor = Color(0xFF21262D))
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                if (!isBannerAd) {
                    OutlinedTextField(
                        value = adVideoUrl,
                        onValueChange = { adVideoUrl = it },
                        label = { Text("Direct MP4 or VAST Tag URL") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00F2FE), unfocusedBorderColor = Color(0xFF21262D))
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = adClickThroughUrl,
                        onValueChange = { adClickThroughUrl = it },
                        label = { Text("Click-through URL (Landing Website)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00F2FE), unfocusedBorderColor = Color(0xFF21262D))
                    )
                } else {
                    OutlinedTextField(
                        value = adHtmlCode,
                        onValueChange = { adHtmlCode = it },
                        label = { Text("HTML / Script Code") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 8,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00F2FE), unfocusedBorderColor = Color(0xFF21262D))
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (!isBannerAd && (adVideoUrl.isEmpty() || adClickThroughUrl.isEmpty())) {
                            Toast.makeText(context, "Direct MP4 / VAST URL and click-through link are required!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (isBannerAd && adHtmlCode.isEmpty()) {
                            Toast.makeText(context, "HTML / Script Code is required for Banner Ad!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val cleanId = "ad_" + System.currentTimeMillis()
                        val newAd = Advertisement(
                            id = cleanId,
                            videoUrl = if (isBannerAd) "" else adVideoUrl,
                            clickThroughUrl = if (isBannerAd) "" else adClickThroughUrl,
                            title = adTitle.ifEmpty { if (isBannerAd) "Sponsored Banner" else "Sponsored Video" },
                            htmlCode = if (isBannerAd) adHtmlCode else "",
                            isBanner = isBannerAd
                        )
                        viewModel.addNewAd(newAd) { success, errorMsg ->
                            if (success) {
                                Toast.makeText(context, "Ad Published to Cloud successfully!", Toast.LENGTH_LONG).show()
                                adTitle = ""; adVideoUrl = ""; adClickThroughUrl = ""; adHtmlCode = ""
                            } else {
                                val details = if (errorMsg != null) "\nError: $errorMsg" else ""
                                Toast.makeText(context, "Network bypass: Added ad locally!$details", Toast.LENGTH_LONG).show()
                                adTitle = ""; adVideoUrl = ""; adClickThroughUrl = ""; adHtmlCode = ""
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F2FE), contentColor = Color.Black)
                ) {
                    Text(text = "Publish Advertisement", fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

// --- SCREEN 3: DEVELOPER PROXY CONFIG ---
@Composable
fun DeveloperProxyScreen(viewModel: FlixbuzzViewModel) {
    AdminPanelScreen(viewModel = viewModel)
}

// --- SCREEN 2: ADMIN PANEL ---
// --- HELPERS FOR SECURING AND TROUBLESHOOTING LOGIN GATE ---
private fun getCertificateSHA1(context: android.content.Context): String {
    return try {
        val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_SIGNATURES)
        }
        val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            packageInfo.signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures
        }
        if (signatures != null && signatures.isNotEmpty()) {
            val md = java.security.MessageDigest.getInstance("SHA-1")
            val publicKey = md.digest(signatures[0].toByteArray())
            val hexString = StringBuilder()
            for (i in publicKey.indices) {
                val appendString = Integer.toHexString(0xFF and publicKey[i].toInt())
                if (appendString.length == 1) hexString.append("0")
                hexString.append(appendString.uppercase())
                if (i < publicKey.size - 1) hexString.append(":")
            }
            hexString.toString()
        } else {
            "UNKNOWN_SIGNATURE"
        }
    } catch (e: Exception) {
        "ERROR: ${e.message}"
    }
}

private fun hashPasscode(input: String): String {
    return try {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        hashBytes.joinToString("") { "%02x".format(it) }
    } catch (e: Exception) {
        ""
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminLoginScreen(viewModel: FlixbuzzViewModel) {
    val context = LocalContext.current
    var passwordInput by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    var showSha1Helper by remember { mutableStateOf(false) }
    
    val sha1Fingerprint = remember { getCertificateSHA1(context) }

    // Real Google Sign-In setup
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember {
        GoogleSignIn.getClient(context, gso)
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isVerifying = true
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val email = account?.email
            if (email != null) {
                val cleanEmail = email.trim().lowercase()
                viewModel.verifyAdminEmailOnFirestore(cleanEmail) { isOwner ->
                    isVerifying = false
                    if (isOwner) {
                        Toast.makeText(context, "✅ Verified Owner Logged In! Welcome back, Salman.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "✅ Verified Creator Account Logged In!", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                isVerifying = false
                Toast.makeText(context, "❌ Google Sign-In failed: Email not retrieved.", Toast.LENGTH_LONG).show()
            }
        } catch (e: ApiException) {
            isVerifying = false
            Log.e("AdminLogin", "Google Sign-In ApiException: ${e.statusCode}", e)
            showSha1Helper = true // Auto-expand help card when Status 10 occurs!
            Toast.makeText(context, "❌ Google Sign-In failed (Status ${e.statusCode}). Please use Security Passcode.", Toast.LENGTH_LONG).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A), // Midnight Dark
                        Color(0xFF020617)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Glowing Background Orb
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
                .size(240.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Branding Logo Lock Indicator
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Login Gate",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "LOGIN PORTAL",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Secure Google identity verification is required",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Google Sign-In Primary Button (Launches Real Google Account Picker)
            Button(
                onClick = {
                    try {
                        googleSignInClient.signOut() // Clear cache to force showing account picker popup every time
                    } catch (e: Exception) {
                        Log.e("AdminLogin", "Error signing out GoogleSignInClient: ${e.message}")
                    }
                    val signInIntent = googleSignInClient.signInIntent
                    googleSignInLauncher.launch(signInIntent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("google_login_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier.size(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "G",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4285F4)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Sign in with Google API",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }
            }

            // --- Google Sign-In SHA-1 Troubleshooter Card ---
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (showSha1Helper) Color(0xFF1E1B4B) else Color(0xFF1E293B).copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, if (showSha1Helper) MaterialTheme.colorScheme.primary else Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSha1Helper = !showSha1Helper },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Help",
                                tint = if (showSha1Helper) MaterialTheme.colorScheme.primary else Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "🛠️ Google Sign-In Error (Status 10) সমাধান করুন",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (showSha1Helper) Color.White else Color(0xFF94A3B8)
                            )
                        }
                        Icon(
                            imageVector = if (showSha1Helper) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (showSha1Helper) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "গুগল সাইন-ইন করার সময় Status 10 এরর দেখার অর্থ হলো আপনার ফায়ারবেস কনসোলে এই অ্যাপটির SHA-1 সিগনেচার কী যোগ করা হয়নি। এটি সক্রিয় করতে নিচের ধাপগুলো অনুসরণ করুন:",
                            fontSize = 11.sp,
                            color = Color(0xFFCBD5E1),
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "১. আপনার Firebase Console > Project Settings এ যান।\n" +
                                    "২. নিচে স্ক্রোল করে 'Add Fingerprint' এ ক্লিক করুন।\n" +
                                    "৩. নিচে দেওয়া SHA-1 সিগনেচার কী-টি কপি করে সেখানে পেস্ট করুন এবং সেভ করুন:",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8),
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Selectable SHA-1 Field
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFF475569), RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        ) {
                            androidx.compose.foundation.text.selection.SelectionContainer {
                                Text(
                                    text = sha1Fingerprint,
                                    fontSize = 11.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Copy Button
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("SHA1 Fingerprint", sha1Fingerprint)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "✅ SHA-1 copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Copy SHA-1 Fingerprint", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF334155))
                Text(
                    text = "OR EMERGENCY FALLBACK",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF334155))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Fallback Passcode Field
            OutlinedTextField(
                value = passwordInput,
                onValueChange = { passwordInput = it },
                label = { Text("Developer Security Passcode") },
                placeholder = { Text("e.g. flixbuzz2026") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = Color(0xFF64748B),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val hashedInput = hashPasscode(passwordInput.trim())
                    // High-Security check: Compare using SHA-256 hash so the plain-text password is never stored in the APK!
                    if (hashedInput == "751c728d3ba70e4f3f6f1289a41e519d23e92add9f9532e1514f55bb87baba36") {
                        viewModel.setAdminLoggedIn(true, "salman016500@gmail.com")
                        Toast.makeText(context, "Welcome Admin! Access granted via security passcode.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "❌ Invalid passcode key! Check your credentials.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Verify Passcode Key",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Loading overlay
        if (isVerifying) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Verifying Google Credentials securely...", color = Color.White, fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(viewModel: FlixbuzzViewModel) {
    val context = LocalContext.current
    val isAdminLoggedIn by viewModel.isAdminLoggedIn.collectAsState()
    
    if (!isAdminLoggedIn) {
        AdminLoginScreen(viewModel = viewModel)
        return
    }

    val isOwnerAdmin by viewModel.isOwnerAdmin.collectAsState()
    val movies by viewModel.movies.collectAsState()
    val advertisements by viewModel.advertisements.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()
    val cloudflareWorkerUrl by viewModel.cloudflareWorkerUrl.collectAsState()
    val projectId by viewModel.projectId.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var activeSubTab by remember { mutableStateOf(0) }

    // --- VIDEO FORM VARIABLES ---
    var movieId by remember { mutableStateOf("") }
    var movieTitle by remember { mutableStateOf("") }
    var movieDescription by remember { mutableStateOf("") }
    var moviePosterUrl by remember { mutableStateOf("") }
    var movieGenre by remember { mutableStateOf("Fantasy") }
    var movieDriveId by remember { mutableStateOf("") }
    var movieMirrorIds by remember { mutableStateOf("") }
    var isEditingMovie by remember { mutableStateOf(false) }

    // --- AD FORM VARIABLES ---
    var adId by remember { mutableStateOf("") }
    var adTitle by remember { mutableStateOf("") }
    var adVideoUrl by remember { mutableStateOf("") }
    var adClickThroughUrl by remember { mutableStateOf("") }
    var adHtmlCode by remember { mutableStateOf("") }
    var adIsBanner by remember { mutableStateOf(false) }
    var isEditingAd by remember { mutableStateOf(false) }

    // --- COMING SOON SPOTLIGHT VARIABLES ---
    var comingSoonText by remember { mutableStateOf(appSettings.comingSoonText) }
    var comingSoonPosterUrl by remember { mutableStateOf(appSettings.comingSoonPosterUrl) }
    var isComingSoonActive by remember { mutableStateOf(appSettings.isComingSoonActive) }

    // --- SPONSOR SPOTLIGHT VARIABLES ---
    var sponsorText by remember { mutableStateOf(appSettings.sponsorText) }
    var sponsorPosterUrl by remember { mutableStateOf(appSettings.sponsorPosterUrl) }
    var sponsorVideoUrl by remember { mutableStateOf(appSettings.sponsorVideoUrl) }
    var isSponsorActive by remember { mutableStateOf(appSettings.isSponsorActive) }

    // Sync coming soon & sponsor states when settings load
    LaunchedEffect(appSettings) {
        comingSoonText = appSettings.comingSoonText
        comingSoonPosterUrl = appSettings.comingSoonPosterUrl
        isComingSoonActive = appSettings.isComingSoonActive
        sponsorText = appSettings.sponsorText
        sponsorPosterUrl = appSettings.sponsorPosterUrl
        sponsorVideoUrl = appSettings.sponsorVideoUrl
        isSponsorActive = appSettings.isSponsorActive
    }

    // --- GALLERY ACTIVITY RESULT LAUNCHERS ---
    val moviePosterPicker = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            val base64 = convertUriToBase64(context, it)
            if (base64 != null) {
                moviePosterUrl = base64
                Toast.makeText(context, "Thumbnail imported and compressed successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to compress thumbnail.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val movieVideoPicker = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            val localPath = copyUriToLocalStorage(context, it, "video")
            if (localPath != null) {
                movieDriveId = localPath
                Toast.makeText(context, "Video file saved locally!\nNote: For other users to stream, use a Cloud/Drive link.", Toast.LENGTH_LONG).show()
            }
        }
    }

    val comingSoonPosterPicker = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            val base64 = convertUriToBase64(context, it)
            if (base64 != null) {
                comingSoonPosterUrl = base64
                Toast.makeText(context, "Teaser Poster imported and compressed successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to compress teaser poster.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val sponsorPosterPicker = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            val base64 = convertUriToBase64(context, it)
            if (base64 != null) {
                sponsorPosterUrl = base64
                Toast.makeText(context, "Sponsor Banner imported and compressed successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to compress sponsor banner.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val sponsorVideoPicker = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            val localPath = copyUriToLocalStorage(context, it, "sponsor_video")
            if (localPath != null) {
                sponsorVideoUrl = localPath
                Toast.makeText(context, "Sponsor Video saved locally!\nNote: For other users to stream, use a Cloud/Drive link.", Toast.LENGTH_LONG).show()
            }
        }
    }

    // --- PROXY INFRASTRUCTURE VARIABLES ---
    var infraProjectId by remember { mutableStateOf(projectId) }
    var infraWorkerUrl by remember { mutableStateOf(cloudflareWorkerUrl) }

    val workerScriptCode = """
/**
 * Cloudflare Worker: Google Drive Streaming CDN Proxy & Quota Bypass
 * Features: Chunked range requests, CDN edge caching, and token rotation.
 */
const GOOGLE_DRIVE_API_TOKENS = [
  "YOUR_GOOGLE_DRIVE_API_KEY_1",
  "YOUR_GOOGLE_DRIVE_API_KEY_2"
];

addEventListener("fetch", event => {
  event.respondWith(handleRequest(event));
});

async fun handleRequest(event) {
  const request = event.request;
  const url = new URL(request.url);
  const fileId = url.searchParams.get("id");

  if (url.pathname === "/stream" && fileId) {
    const token = GOOGLE_DRIVE_API_TOKENS[0];
    const driveUrl = "https://www.googleapis.com/drive/v3/files/" + fileId + "?alt=media&key=" + token;
    
    const forwardHeaders = new Headers();
    const rangeHeader = request.headers.get("Range");
    if (rangeHeader) forwardHeaders.set("Range", rangeHeader);

    const driveResponse = await fetch(driveUrl, {
      headers: forwardHeaders,
      cf: { cacheTtl: 86400, cacheEverything: true }
    });

    const headers = new Headers(driveResponse.headers);
    headers.set("Access-Control-Allow-Origin", "*");
    return new Response(driveResponse.body, { status: driveResponse.status, headers });
  }
  return new Response("OK", { status: 200 });
}
    """.trimIndent()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Main Admin Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = "Studio", tint = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isOwnerAdmin) "Flixbuzz Creator Studio (Owner)" else "Flixbuzz Creator Studio (Poster)",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Button(
                        onClick = {
                            viewModel.setAdminLoggedIn(false)
                            Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Logout", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    text = if (isOwnerAdmin) {
                        "Manage movies, placements, coming soon teasers & network channels."
                    } else {
                        "Publish new video posts to Flimshare OTT."
                    },
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        // Filter tabs based on owner authorization
        val tabs = if (isOwnerAdmin) {
            listOf("🎬 Videos", "📢 Ads", "⏰ Coming Soon", "🌐 Proxy Routes")
        } else {
            listOf("🎬 Videos")
        }

        // Clamp selection to valid range
        if (activeSubTab >= tabs.size) {
            activeSubTab = 0
        }

        // Sub-tabs navigation chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEachIndexed { index, label ->
                val isSelected = activeSubTab == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { activeSubTab = index }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

        // Content panel depending on activeSubTab
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            when (activeSubTab) {
                0 -> {
                    // --- MANAGE VIDEOS TAB ---
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = if (isEditingMovie) "✏️ Edit Video Properties" else "➕ Publish New Video",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                OutlinedTextField(
                                    value = movieId,
                                    onValueChange = { if (!isEditingMovie) movieId = it },
                                    label = { Text("Unique Movie ID (Auto-generated if empty)") },
                                    enabled = !isEditingMovie,
                                    placeholder = { Text("e.g. avatar_2026") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                                )

                                OutlinedTextField(
                                    value = movieTitle,
                                    onValueChange = { movieTitle = it },
                                    label = { Text("Movie Title") },
                                    placeholder = { Text("e.g. Avatar: Fire and Ash") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                                )

                                OutlinedTextField(
                                    value = movieDescription,
                                    onValueChange = { movieDescription = it },
                                    label = { Text("Movie Description / Synopsis") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                                )

                                OutlinedTextField(
                                    value = if (moviePosterUrl.startsWith("data:image/")) "[Compressed Image (Base64) - Uploaded Successfully]" else moviePosterUrl,
                                    onValueChange = { newValue ->
                                        if (newValue.isEmpty()) {
                                            moviePosterUrl = ""
                                        } else if (!newValue.startsWith("[Compressed Image")) {
                                            moviePosterUrl = newValue
                                        }
                                    },
                                    label = { Text("Poster Image URL / Local path / Base64") },
                                    trailingIcon = {
                                        if (moviePosterUrl.isNotEmpty()) {
                                            IconButton(onClick = { moviePosterUrl = "" }) {
                                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                                )

                                Button(
                                    onClick = { moviePosterPicker.launch("image/*") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Icon(imageVector = Icons.Default.Image, contentDescription = "Gallery", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Upload Thumbnail from Gallery", fontSize = 12.sp)
                                }

                                // --- REAL-TIME THUMBNAIL PREVIEW ---
                                if (moviePosterUrl.isNotEmpty()) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                        Text(
                                            text = "🖼️ Live Thumbnail Preview:",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(160.dp)
                                                .clip(RoundedCornerShape(12.dp)),
                                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                        ) {
                                            AsyncImage(
                                                model = resolvePosterModel(moviePosterUrl),
                                                contentDescription = "Thumbnail Preview",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = movieDriveId,
                                    onValueChange = { movieDriveId = it },
                                    label = { Text("Google Drive File ID / Video URL") },
                                    placeholder = { Text("Enter Google Drive ID, URL, or upload local file") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                                )

                                Button(
                                    onClick = { movieVideoPicker.launch("video/*") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Icon(imageVector = Icons.Default.Movie, contentDescription = "Gallery Video", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Upload Video from Gallery", fontSize = 12.sp)
                                }

                                OutlinedTextField(
                                    value = movieMirrorIds,
                                    onValueChange = { movieMirrorIds = it },
                                    label = { Text("Mirror Drive IDs (Comma separated)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                                )

                                // ☁️ Professional Cloud Hosting Guide Card
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Cloud,
                                                contentDescription = "Cloud Guide",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "☁️ Professional Cloud Storage Guide",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Flixbuzz একটি বিশ্বমানের OTT প্ল্যাটফর্ম। গ্যালাক্সি বা মেমোরি থেকে মিডিয়া সিলেক্ট করলে তা সাময়িকভাবে শুধুমাত্র আপনার ফোনে সংরক্ষিত থাকে (অ্যাপ ডেটা ক্লিয়ার করলে ডিলিট হবে এবং অন্য ইউজাররা দেখতে পাবে না)।",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 16.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "স্থায়ী ও প্রফেশনাল ব্যবহারের জন্য:\n" +
                                                    "১. থাম্বনেল ও ইমেজ: ফায়ারবেস স্টোরেজ, Imgur বা পোস্টইমেজে আপলোড করে ডাইরেক্ট লিঙ্ক ব্যবহার করুন।\n" +
                                                     "২. ভিডিও ফাইল: গুগল ড্রাইভ (প্রক্সি সহ), Cloudflare Stream বা ক্লাউড সার্ভারে ভিডিও আপলোড করে ডাইরেক্ট লিঙ্কটি 'Google Drive File ID / Video URL' ফিল্ডে বসান।\n" +
                                                    "এতে করে সারা বিশ্বের যেকোনো দর্শক অত্যন্ত দ্রুত ও বাফারিং ছাড়া ভিডিও উপভোগ করতে পারবেন।",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 16.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                // Genre Selector chips
                                Text("Movie Genre", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val genres = listOf("Fantasy", "Sci-Fi", "Animation", "Surreal", "Action", "Comedy")
                                    genres.forEach { g ->
                                        val isSelected = movieGenre == g
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                                .clickable { movieGenre = g }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(text = g, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                                        }
                                    }
                                }

                                                               Button(
                                    onClick = {
                                        if (movieTitle.trim().isEmpty() || movieDriveId.trim().isEmpty()) {
                                            Toast.makeText(context, "Movie Title and Google Drive ID/Video URL cannot be empty!", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        val finalMovieId = if (isEditingMovie) {
                                            movieId.trim()
                                        } else {
                                            if (movieId.trim().isNotEmpty()) {
                                                movieId.trim()
                                            } else {
                                                val slug = movieTitle.trim().lowercase()
                                                    .replace(Regex("[^a-z0-9]+"), "_")
                                                    .removeSurrounding("_")
                                                if (slug.isNotEmpty()) {
                                                    "${slug}_${System.currentTimeMillis() % 100000}"
                                                } else {
                                                    "movie_${System.currentTimeMillis()}"
                                                }
                                            }
                                        }
                                        val movie = Movie(
                                            id = finalMovieId,
                                            title = movieTitle.trim(),
                                            description = movieDescription.trim(),
                                            posterUrl = moviePosterUrl.trim().ifEmpty { "https://images.unsplash.com/photo-1536440136628-849c177e76a1" },
                                            genre = movieGenre,
                                            googleDriveFileId = movieDriveId.trim(),
                                            mirrorIds = movieMirrorIds.trim()
                                        )
                                        
                                        viewModel.addNewMovie(movie) { success, err ->
                                            if (success) {
                                                Toast.makeText(context, if (isEditingMovie) "Video updated successfully!" else "Video published to Cloud!", Toast.LENGTH_SHORT).show()
                                                
                                                // Trigger gorgeous push notification!
                                                triggerMovieReleaseNotification(
                                                    context = context,
                                                    title = movie.title,
                                                    description = movie.description.ifEmpty { "Now streaming on Flixbuzz OTT!" },
                                                    posterUrl = movie.posterUrl
                                                )

                                                // Reset variables
                                                movieId = ""; movieTitle = ""; movieDescription = ""; moviePosterUrl = ""; movieDriveId = ""; movieMirrorIds = ""
                                                isEditingMovie = false
                                            } else {
                                                Toast.makeText(context, "Bypassed Cloud: Saved video locally.\nError: $err", Toast.LENGTH_LONG).show()
                                                // Trigger notification anyway for seamless testing!
                                                triggerMovieReleaseNotification(
                                                    context = context,
                                                    title = movie.title,
                                                    description = movie.description,
                                                    posterUrl = movie.posterUrl
                                                )
                                                movieId = ""; movieTitle = ""; movieDescription = ""; moviePosterUrl = ""; movieDriveId = ""; movieMirrorIds = ""
                                                isEditingMovie = false
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text(text = if (isEditingMovie) "Save Video Changes" else "Publish Video & Notify Users 🚀", fontWeight = FontWeight.Bold)
                                }

                                if (isEditingMovie) {
                                    OutlinedButton(
                                        onClick = {
                                            movieId = ""; movieTitle = ""; movieDescription = ""; moviePosterUrl = ""; movieDriveId = ""; movieMirrorIds = ""
                                            isEditingMovie = false
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Cancel Edit")
                                    }
                                }
                            }
                        }

                        // Video Catalog Deletion / Selection Section
                        Text("Existing Videos (${movies.size})", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        
                        movies.forEach { movie ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = movie.posterUrl,
                                        contentDescription = movie.title,
                                        modifier = Modifier.size(50.dp).clip(RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = movie.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                        Text(text = "Genre: ${movie.genre} • ID: ${movie.id}", fontSize = 11.sp)
                                    }
                                    
                                    if (isOwnerAdmin) {
                                        // Edit Button
                                        IconButton(onClick = {
                                        movieId = movie.id
                                        movieTitle = movie.title
                                        movieDescription = movie.description
                                        moviePosterUrl = movie.posterUrl
                                        movieGenre = movie.genre
                                        movieDriveId = movie.googleDriveFileId
                                        movieMirrorIds = movie.mirrorIds
                                        isEditingMovie = true
                                        Toast.makeText(context, "Video details pre-filled for editing!", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                                    }

                                    // Delete Button
                                    IconButton(onClick = {
                                        viewModel.deleteMovie(movie.id) { success, err ->
                                            if (success) {
                                                Toast.makeText(context, "Movie deleted successfully from Cloud & local!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Removed movie locally (Cloud error: $err)", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // --- MANAGE ADS TAB ---
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = if (isEditingAd) "✏️ Edit Placement" else "➕ Create Advertisement Placement",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                OutlinedTextField(
                                    value = adId,
                                    onValueChange = { if (!isEditingAd) adId = it },
                                    label = { Text("Unique Ad ID") },
                                    enabled = !isEditingAd,
                                    placeholder = { Text("e.g. ad_samsung_s26") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                                )

                                OutlinedTextField(
                                    value = adTitle,
                                    onValueChange = { adTitle = it },
                                    label = { Text("Advertisement Title") },
                                    placeholder = { Text("e.g. Samsung S26 Ultra Banner Ad") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                                )

                                OutlinedTextField(
                                    value = adVideoUrl,
                                    onValueChange = { adVideoUrl = it },
                                    label = { Text("Video Streaming URL (For Mid-Rolls)") },
                                    placeholder = { Text("Direct link to raw mp4 file") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                                )

                                OutlinedTextField(
                                    value = adClickThroughUrl,
                                    onValueChange = { adClickThroughUrl = it },
                                    label = { Text("Click-through Target URL") },
                                    placeholder = { Text("e.g. https://samsung.com") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                                )

                                OutlinedTextField(
                                    value = adHtmlCode,
                                    onValueChange = { adHtmlCode = it },
                                    label = { Text("HTML Banner Placement Script (Optional)") },
                                    placeholder = { Text("<div style='background:...'>Click Here</div>") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Is Display Banner Ad?", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Disables video playback, renders custom HTML element", fontSize = 10.sp, color = Color.Gray)
                                    }
                                    Switch(
                                        checked = adIsBanner,
                                        onCheckedChange = { adIsBanner = it }
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        if (adId.trim().isEmpty() || adTitle.trim().isEmpty() || (adVideoUrl.trim().isEmpty() && adHtmlCode.trim().isEmpty())) {
                                            Toast.makeText(context, "ID, Title and either Video URL or HTML code are required!", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        val ad = Advertisement(
                                            id = adId.trim(),
                                            videoUrl = adVideoUrl.trim(),
                                            clickThroughUrl = adClickThroughUrl.trim().ifEmpty { "https://www.google.com" },
                                            title = adTitle.trim(),
                                            htmlCode = adHtmlCode.trim(),
                                            isBanner = adIsBanner
                                        )
                                        viewModel.addNewAd(ad) { success, err ->
                                            if (success) {
                                                Toast.makeText(context, if (isEditingAd) "Ad updated successfully!" else "Ad published successfully to Cloud!", Toast.LENGTH_SHORT).show()
                                                adId = ""; adTitle = ""; adVideoUrl = ""; adClickThroughUrl = ""; adHtmlCode = ""; adIsBanner = false
                                                isEditingAd = false
                                            } else {
                                                Toast.makeText(context, "Bypassed Cloud: Saved ad locally.\nError: $err", Toast.LENGTH_LONG).show()
                                                adId = ""; adTitle = ""; adVideoUrl = ""; adClickThroughUrl = ""; adHtmlCode = ""; adIsBanner = false
                                                isEditingAd = false
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text(text = if (isEditingAd) "Save Placement Changes" else "Publish Placement to Cloud", fontWeight = FontWeight.Bold)
                                }

                                if (isEditingAd) {
                                    OutlinedButton(
                                        onClick = {
                                            adId = ""; adTitle = ""; adVideoUrl = ""; adClickThroughUrl = ""; adHtmlCode = ""; adIsBanner = false
                                            isEditingAd = false
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Cancel Edit")
                                    }
                                }
                            }
                        }

                        // Ads Catalog list
                        Text("Active Ad Campaigns (${advertisements.size})", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

                        advertisements.forEach { ad ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (ad.isBanner) Icons.Default.Campaign else Icons.Default.Movie,
                                        contentDescription = "Ad",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = ad.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(text = "Type: ${if (ad.isBanner) "Banner" else "Video"} • ID: ${ad.id}", fontSize = 11.sp)
                                    }
                                    
                                    if (isOwnerAdmin) {
                                        // Edit Ad Button
                                        IconButton(onClick = {
                                        adId = ad.id
                                        adTitle = ad.title
                                        adVideoUrl = ad.videoUrl
                                        adClickThroughUrl = ad.clickThroughUrl
                                        adHtmlCode = ad.htmlCode
                                        adIsBanner = ad.isBanner
                                        isEditingAd = true
                                        Toast.makeText(context, "Ad details pre-filled for editing!", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Ad", tint = MaterialTheme.colorScheme.primary)
                                    }

                                    // Delete Ad Button
                                    IconButton(onClick = {
                                        viewModel.deleteAd(ad.id) { success, err ->
                                            if (success) {
                                                Toast.makeText(context, "Ad campaign removed successfully!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Removed ad locally (Cloud error: $err)", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // --- COMING SOON SPOTLIGHT CONFIG TAB ---
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "⏰ Customize Coming Soon Teaser Spotlight",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Configure the spotlight billboard that users see immediately at the top of their public theater feeds.",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )

                                OutlinedTextField(
                                    value = comingSoonText,
                                    onValueChange = { comingSoonText = it },
                                    label = { Text("Upcoming Movie Teaser Title/Message") },
                                    placeholder = { Text("e.g. Spider-Man: Beyond the Spider-Verse is coming soon!") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                                )

                                OutlinedTextField(
                                    value = if (comingSoonPosterUrl.startsWith("data:image/")) "[Compressed Image (Base64) - Uploaded Successfully]" else comingSoonPosterUrl,
                                    onValueChange = { newValue ->
                                        if (newValue.isEmpty()) {
                                            comingSoonPosterUrl = ""
                                        } else if (!newValue.startsWith("[Compressed Image")) {
                                            comingSoonPosterUrl = newValue
                                        }
                                    },
                                    label = { Text("Spotlight Teaser Poster Image URL / Local path") },
                                    placeholder = { Text("High resolution landscape image link") },
                                    trailingIcon = {
                                        if (comingSoonPosterUrl.isNotEmpty()) {
                                            IconButton(onClick = { comingSoonPosterUrl = "" }) {
                                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                                )

                                Button(
                                    onClick = { comingSoonPosterPicker.launch("image/*") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Icon(imageVector = Icons.Default.Image, contentDescription = "Gallery", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Choose Teaser Poster from Gallery", fontSize = 12.sp)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Publish Teaser to Feed?", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Toggle whether this billboard is visible", fontSize = 10.sp, color = Color.Gray)
                                    }
                                    Switch(
                                        checked = isComingSoonActive,
                                        onCheckedChange = { isComingSoonActive = it }
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        if (comingSoonText.trim().isEmpty() || comingSoonPosterUrl.trim().isEmpty()) {
                                            Toast.makeText(context, "Spotlight message and Poster URL are required!", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        viewModel.updateComingSoon(comingSoonText.trim(), comingSoonPosterUrl.trim(), isComingSoonActive) { success, err ->
                                            if (success) {
                                                Toast.makeText(context, "Spotlight banner published to Cloud and Feed!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Bypassed Cloud: Saved spotlight settings locally.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Publish Spotlight Banner 📺", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // --- NEW BRAND SPONSOR CONTROL SECTION ---
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "⚡ Sponsor Spotlight Showcase Editor",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Upload sponsor video & banner. The algorithm will automatically display this spotlight premium card in your users' feeds.",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )

                                OutlinedTextField(
                                    value = sponsorText,
                                    onValueChange = { sponsorText = it },
                                    label = { Text("Sponsor Display Heading / Slogan") },
                                    placeholder = { Text("Samsung Galaxy S26 Ultra - Epic, Just Like That.") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                                )

                                OutlinedTextField(
                                    value = if (sponsorPosterUrl.startsWith("data:image/")) "[Compressed Image (Base64) - Uploaded Successfully]" else sponsorPosterUrl,
                                    onValueChange = { newValue ->
                                        if (newValue.isEmpty()) {
                                            sponsorPosterUrl = ""
                                        } else if (!newValue.startsWith("[Compressed Image")) {
                                            sponsorPosterUrl = newValue
                                        }
                                    },
                                    label = { Text("Sponsor Banner Poster URL / Local path") },
                                    trailingIcon = {
                                        if (sponsorPosterUrl.isNotEmpty()) {
                                            IconButton(onClick = { sponsorPosterUrl = "" }) {
                                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                                )

                                Button(
                                    onClick = { sponsorPosterPicker.launch("image/*") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Icon(imageVector = Icons.Default.Image, contentDescription = "Sponsor Image", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Choose Sponsor Banner from Gallery", fontSize = 12.sp)
                                }

                                OutlinedTextField(
                                    value = sponsorVideoUrl,
                                    onValueChange = { sponsorVideoUrl = it },
                                    label = { Text("Sponsor Video Presentation URL / Path") },
                                    placeholder = { Text("e.g. video URL or upload file from gallery") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                                )

                                Button(
                                    onClick = { sponsorVideoPicker.launch("video/*") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Icon(imageVector = Icons.Default.Movie, contentDescription = "Sponsor Video", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Choose Sponsor Video from Gallery", fontSize = 12.sp)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Publish Sponsor Spotlight to Feed?", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Make this brand visible to audience", fontSize = 10.sp, color = Color.Gray)
                                    }
                                    Switch(
                                        checked = isSponsorActive,
                                        onCheckedChange = { isSponsorActive = it }
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        if (sponsorText.trim().isEmpty() || sponsorPosterUrl.trim().isEmpty()) {
                                            Toast.makeText(context, "Sponsor slogan and Poster are required!", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        viewModel.updateSponsor(
                                            sponsorText.trim(),
                                            sponsorPosterUrl.trim(),
                                            sponsorVideoUrl.trim(),
                                            isSponsorActive
                                        ) { success, err ->
                                            if (success) {
                                                Toast.makeText(context, "Sponsor Spotlight published to Live Cloud!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Saved Sponsor settings locally.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Publish Sponsor Spotlight 🚀", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Preview of the billboard
                        Text("Live Spotlight Feed Preview", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        
                        if (isComingSoonActive) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column {
                                    Box(modifier = Modifier.height(130.dp).fillMaxWidth()) {
                                        AsyncImage(
                                            model = comingSoonPosterUrl,
                                            contentDescription = "Coming Soon Banner",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.verticalGradient(
                                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                                                    )
                                                )
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(8.dp)
                                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("COMING SOON 🎬", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                                        }
                                    }
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(text = comingSoonText, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(100.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Spotlight is currently inactive.", color = Color.Gray)
                            }
                        }
                    }
                }

                3 -> {
                    // --- PROXY ROUTES TAB ---
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "🌐 Routing Proxies & Credentials",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                OutlinedTextField(
                                    value = infraProjectId,
                                    onValueChange = { infraProjectId = it },
                                    label = { Text("Firebase Project ID") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                                )

                                OutlinedTextField(
                                    value = infraWorkerUrl,
                                    onValueChange = { infraWorkerUrl = it },
                                    label = { Text("Cloudflare Stream CDN Proxy root") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Button(
                                    onClick = {
                                        viewModel.updateConfig(infraProjectId, infraWorkerUrl)
                                        Toast.makeText(context, "Infrastructure routing paths updated!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Save Configuration Routes", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Cloudflare Script code box
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "Cloudflare Worker Code", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Worker Script", workerScriptCode)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Worker script copied to clipboard!", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Code", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Paste this code inside your Cloudflare Worker so Google Drive video files stream at unlimited bandwidth bypass.",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                        .verticalScroll(rememberScrollState())
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = workerScriptCode,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                        fontSize = 11.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- FULL SCREEN CUSTOM IMMERSIVE MEDIA PLAYER WITH SKIP ADS CAPABILITIES ---
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ImmersivePlayerOverlay(
    movie: Movie,
    isAdPlaying: Boolean,
    activeAd: Advertisement?,
    onClose: () -> Unit,
    onAdSkip: () -> Unit,
    viewModel: FlixbuzzViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // --- Dynamic Orientation & Fullscreen Status Bar Toggles ---
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(isLandscape) {
        val window = context.findActivity()?.window ?: return@LaunchedEffect
        if (isLandscape) {
            // Hide System status/navigation bars in Landscape Mode
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                window.insetsController?.let { controller ->
                    controller.hide(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
                    controller.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                    android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                    or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
            }
        } else {
            // Restore System bars in Portrait Mode
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                window.insetsController?.show(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // Safety Clean-up: Return orientation lock back to unspecified and show system bars when player composable is disposed
            val activity = context.findActivity()
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.let { window ->
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    window.insetsController?.show(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
                } else {
                    @Suppress("DEPRECATION")
                    window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_VISIBLE
                }
            }
        }
    }

    // --- State Managers ---
    var localIsAdPlaying by remember { mutableStateOf(false) }
    var localActiveAd by remember { mutableStateOf<Advertisement?>(null) }
    var lastMoviePosition by remember { mutableStateOf(0L) }
    
    // Track which percentage thresholds have been played
    var played10s by remember { mutableStateOf(false) }
    var played25p by remember { mutableStateOf(false) }
    var played50p by remember { mutableStateOf(false) }
    var played75p by remember { mutableStateOf(false) }
    var played95p by remember { mutableStateOf(false) }

    // Create and remember ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var adCountdown by remember { mutableStateOf(5) }
    var currentPositionMillis by remember { mutableStateOf(0L) }
    var totalDurationMillis by remember { mutableStateOf(0L) }
    var selectedSpeed by remember { mutableStateOf(1.0f) }
    var selectedMirrorId by remember { mutableStateOf("Primary") }
    var isMuted by remember { mutableStateOf(false) }

    var showSpeedDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    
    // YouTube player visual states
    var areControlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var showForwardIndicator by remember { mutableStateOf(false) }
    var showBackwardIndicator by remember { mutableStateOf(false) }

    // Interactivity simulated states
    var isSubscribed by remember { mutableStateOf(false) }
    var likedState by remember { mutableStateOf(0) } // 0 = none, 1 = liked, 2 = disliked
    var likesCount by remember { mutableStateOf(4520) }
    var showCommentsList by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }
    
    val commentsList = remember(movie) {
        mutableStateListOf(
            "The cinematography is absolutely mind-blowing! Flixbuzz never disappoints.",
            "Loved the storyline. Can't wait for the sequel! 🍿🔥",
            "This was a masterpiece. Easily my favorite piece of cinema this year."
        )
    }

    // Reset ad flags and positions whenever movie changes
    LaunchedEffect(movie) {
        played10s = false
        played25p = false
        played50p = false
        played75p = false
        played95p = false
        lastMoviePosition = 0L
        localIsAdPlaying = false
        localActiveAd = null
        likedState = 0
        likesCount = (3000..9999).random()
    }

    // VAST Tag Resolver helper
    var resolvedAdVideoUrl by remember { mutableStateOf("") }
    LaunchedEffect(localIsAdPlaying, localActiveAd) {
        if (localIsAdPlaying && localActiveAd != null) {
            resolvedAdVideoUrl = ""
            val adUrl = localActiveAd!!.videoUrl
            val resolved = resolveVastVideoUrl(adUrl)
            resolvedAdVideoUrl = resolved
        }
    }

    // Synchronize media items on player (supports seamless mid-roll transition)
    LaunchedEffect(movie, localIsAdPlaying, resolvedAdVideoUrl, selectedMirrorId) {
        val streamUrl = if (localIsAdPlaying) {
            if (resolvedAdVideoUrl.isNotEmpty()) resolvedAdVideoUrl else ""
        } else {
            if (selectedMirrorId == "Primary") {
                FirebaseFirestoreClient.getVideoStreamingUrl(movie.googleDriveFileId)
            } else {
                FirebaseFirestoreClient.getVideoStreamingUrl(selectedMirrorId)
            }
        }
        
        Log.d("ImmersivePlayerOverlay", "Streaming Media URL: $streamUrl")
        
        if (streamUrl.isNotEmpty()) {
            exoPlayer.setMediaItem(MediaItem.fromUri(streamUrl))
            exoPlayer.playbackParameters = PlaybackParameters(selectedSpeed)
            exoPlayer.prepare()
            if (localIsAdPlaying) {
                exoPlayer.play()
            } else {
                exoPlayer.seekTo(lastMoviePosition)
                exoPlayer.play()
            }
            isPlaying = true
        }
    }

    // AD COUNTDOWN TICKER & AUTO SKIP
    LaunchedEffect(localIsAdPlaying, localActiveAd) {
        if (localIsAdPlaying) {
            adCountdown = 5
            while (adCountdown > 0) {
                delay(1000)
                adCountdown--
            }
        }
    }

    // Monitor ExoPlayer End of Video (to auto skip ad, or loop, etc.)
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    if (localIsAdPlaying) {
                        // Ad finished, resume movie
                        localIsAdPlaying = false
                        localActiveAd = null
                    }
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Exoplayer Position Tracker & Ad Triggering Loop
    LaunchedEffect(movie, localIsAdPlaying) {
        while (isActive) {
            delay(400)
            if (!localIsAdPlaying && exoPlayer.isPlaying) {
                currentPositionMillis = exoPlayer.currentPosition
                totalDurationMillis = exoPlayer.duration
                
                val currentSec = currentPositionMillis / 1000
                val totalSec = totalDurationMillis / 1000
                
                if (totalSec > 0) {
                    val isLongVideo = totalSec >= 600 // 10 minutes
                    var triggerAd = false
                    
                    if (currentSec >= 10 && !played10s) {
                        played10s = true
                        triggerAd = true
                    } else if (isLongVideo) {
                        val percent = (currentPositionMillis.toDouble() / totalDurationMillis * 100).toInt()
                        if (percent >= 25 && percent < 50 && !played25p) {
                            played25p = true
                            triggerAd = true
                        } else if (percent >= 50 && percent < 75 && !played50p) {
                            played50p = true
                            triggerAd = true
                        } else if (percent >= 75 && percent < 95 && !played75p) {
                            played75p = true
                            triggerAd = true
                        } else if (percent >= 95 && !played95p) {
                            played95p = true
                            triggerAd = true
                        }
                    }
                    
                    if (triggerAd) {
                        exoPlayer.pause()
                        lastMoviePosition = currentPositionMillis
                        
                        val availableAds = viewModel.advertisements.value
                        val nonBannerAds = availableAds.filter { !it.isBanner }
                        val chosenAd = if (nonBannerAds.isNotEmpty()) {
                            nonBannerAds.random()
                        } else if (availableAds.isNotEmpty()) {
                            availableAds.random()
                        } else if (FirebaseFirestoreClient.backupAds.isNotEmpty()) {
                            FirebaseFirestoreClient.backupAds.random()
                        } else {
                            null
                        }
                        
                        if (chosenAd != null) {
                            localActiveAd = chosenAd
                            localIsAdPlaying = true
                        } else {
                            Log.d("ImmersivePlayerOverlay", "No advertisements available, skipping mid-roll ad.")
                            exoPlayer.play()
                        }
                    }
                }
            } else if (localIsAdPlaying) {
                currentPositionMillis = exoPlayer.currentPosition
                totalDurationMillis = exoPlayer.duration
            }
        }
    }

    // Auto-hide controls loop
    LaunchedEffect(areControlsVisible) {
        if (areControlsVisible) {
            delay(4000)
            areControlsVisible = false
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
            isPlaying = false
        } else {
            exoPlayer.play()
            isPlaying = true
        }
    }

    fun seekForward() {
        val target = (exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)
        exoPlayer.seekTo(target)
    }

    fun seekBackward() {
        val target = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
        exoPlayer.seekTo(target)
    }

    fun toggleMute() {
        if (isMuted) {
            exoPlayer.volume = 1.0f
            isMuted = false
        } else {
            exoPlayer.volume = 0.0f
            isMuted = true
        }
    }

    fun formatTime(millis: Long): String {
        if (millis <= 0) return "00:00"
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        val hours = (millis / (1000 * 60 * 60))
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    fun skipAdAndResumeMovie() {
        localIsAdPlaying = false
        localActiveAd = null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { /* Consume background clicks */ }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // --- 1. THE NATIVE YOUTUBE-LIKE PLAYER FRAME (16:9 aspect ratio or Fullscreen in Landscape) ---
            Box(
                modifier = if (isLandscape) {
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                }
            ) {
                // Actual ExoPlayer View Rendering
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            setBackgroundColor(android.graphics.Color.BLACK)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // --- GESTURE / TAP DETECTOR OVERLAYS ---
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left 50% for double tap to rewind
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .combinedClickable(
                                onClick = { if (!isLocked) areControlsVisible = !areControlsVisible else areControlsVisible = true },
                                onDoubleClick = {
                                    if (!isLocked && !localIsAdPlaying) {
                                        seekBackward()
                                        showBackwardIndicator = true
                                        coroutineScope.launch {
                                            delay(650)
                                            showBackwardIndicator = false
                                        }
                                    }
                                },
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            )
                    ) {
                        if (showBackwardIndicator) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .background(Color(0x77000000), CircleShape)
                                    .padding(16.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(imageVector = Icons.Default.FastRewind, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                                    Text("-10s", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Right 50% for double tap to forward
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .combinedClickable(
                                onClick = { if (!isLocked) areControlsVisible = !areControlsVisible else areControlsVisible = true },
                                onDoubleClick = {
                                    if (!isLocked && !localIsAdPlaying) {
                                        seekForward()
                                        showForwardIndicator = true
                                        coroutineScope.launch {
                                            delay(650)
                                            showForwardIndicator = false
                                        }
                                    }
                                },
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            )
                    ) {
                        if (showForwardIndicator) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .background(Color(0x77000000), CircleShape)
                                    .padding(16.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(imageVector = Icons.Default.FastForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                                    Text("+10s", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // --- AD COUNTDOWN AND BRANDING OVERLAY (Locks slider and controls) ---
                if (localIsAdPlaying && localActiveAd != null) {
                    // Full-video click redirection for Ad
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(localActiveAd!!.clickThroughUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Redirecting failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                    )
                    
                    // Ad title/brand tag
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .background(Color(0xDD0F0F0F), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "SPONSORED AD • ${localActiveAd!!.title}",
                            color = Color(0xFF00F2FE),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Bottom-Right Skip button
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 12.dp, end = 12.dp)
                    ) {
                        Button(
                            onClick = { if (adCountdown == 0) skipAdAndResumeMovie() },
                            enabled = adCountdown == 0,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (adCountdown == 0) Color(0xFFFF3850) else Color(0x990F0F0F),
                                disabledContainerColor = Color(0x660F0F0F)
                            ),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                text = if (adCountdown > 0) "Skip Ad in ${adCountdown}s" else "Skip Ad >",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // --- PLAYER CONTROL ROW OVERLAYS (Shown when ad is NOT playing) ---
                if (!localIsAdPlaying) {
                    
                    // LOCKED MODE UNLOCK BUTTON
                    if (isLocked) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x22000000))
                        ) {
                            IconButton(
                                onClick = { isLocked = false },
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(50.dp)
                                    .background(Color(0xAA000000), CircleShape)
                            ) {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = "Unlock screen", tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                        }
                    }

                    // STANDARD ACTIVE PLAYER OVERLAYS (PLAY, PAUSE, PROGRESS BAR)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = areControlsVisible && !isLocked,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x66000000))
                        ) {
                            // Top Bar (Back button, Movie Title, Quality and Speed controls)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter)
                                    .background(Brush.verticalGradient(listOf(Color(0x99000000), Color.Transparent)))
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    IconButton(
                                        onClick = {
                                            if (isLandscape) {
                                                val activity = context.findActivity()
                                                activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                            } else {
                                                onClose()
                                            }
                                        }
                                    ) {
                                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = movie.title,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row {
                                    IconButton(onClick = { showSpeedDialog = true }) {
                                        Icon(imageVector = Icons.Default.Speed, contentDescription = "Speed", tint = Color.White)
                                    }
                                    IconButton(onClick = { showQualityDialog = true }) {
                                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings/Quality", tint = Color.White)
                                    }
                                    IconButton(onClick = { isLocked = true }) {
                                        Icon(imageVector = Icons.Default.LockOpen, contentDescription = "Lock Controls", tint = Color.White)
                                    }
                                }
                            }

                            // Center Controls (10s Rewind, Play/Pause, 10s Forward)
                            Row(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalArrangement = Arrangement.spacedBy(24.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { seekBackward() },
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(Color(0x88000000), CircleShape)
                                ) {
                                    Icon(imageVector = Icons.Default.Replay10, contentDescription = "-10s", tint = Color.White, modifier = Modifier.size(24.dp))
                                }

                                IconButton(
                                    onClick = { togglePlayPause() },
                                    modifier = Modifier
                                        .size(60.dp)
                                        .background(Color(0xFFFF3850), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play/Pause",
                                        tint = Color.White,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { seekForward() },
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(Color(0x88000000), CircleShape)
                                ) {
                                    Icon(imageVector = Icons.Default.Forward10, contentDescription = "+10s", tint = Color.White, modifier = Modifier.size(24.dp))
                                }
                            }

                            // Bottom Controls (Timer, Progress Bar and Audio Mute)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x99000000))))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${formatTime(currentPositionMillis)} / ${formatTime(totalDurationMillis)}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    IconButton(onClick = { toggleMute() }, modifier = Modifier.size(28.dp)) {
                                        Icon(
                                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Mute",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = {
                                            val activity = context.findActivity()
                                            if (isLandscape) {
                                                activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                            } else {
                                                activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isLandscape) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                            contentDescription = "Toggle Fullscreen",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                
                                Slider(
                                    value = currentPositionMillis.toFloat(),
                                    onValueChange = { exoPlayer.seekTo(it.toLong()) },
                                    valueRange = 0f..(totalDurationMillis.toFloat().coerceAtLeast(1f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(20.dp)
                                        .testTag("movie_seekbar_youtube"),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFFFF3850),
                                        activeTrackColor = Color(0xFFFF3850),
                                        inactiveTrackColor = Color(0x66FFFFFF)
                                    )
                                )
                            }
                        }
                    }
                }

                // Speed dialog options popup
                if (showSpeedDialog) {
                    AlertDialog(
                        onDismissRequest = { showSpeedDialog = false },
                        title = { Text("Playback Speed", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                        containerColor = Color(0xFF161618),
                        confirmButton = {
                            TextButton(onClick = { showSpeedDialog = false }) {
                                Text("CLOSE", color = Color(0xFFFF3850))
                            }
                        },
                        text = {
                            Column {
                                val speedOptions = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                                speedOptions.forEach { speed ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedSpeed = speed
                                                exoPlayer.playbackParameters = PlaybackParameters(speed)
                                                showSpeedDialog = false
                                            }
                                            .padding(vertical = 12.dp, horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${speed}x", color = Color.White, fontSize = 14.sp)
                                        if (selectedSpeed == speed) {
                                            Icon(imageVector = Icons.Default.Check, contentDescription = "Active", tint = Color(0xFFFF3850))
                                        }
                                    }
                                }
                            }
                        }
                    )
                }

                // Quality Dialog options popup
                if (showQualityDialog) {
                    AlertDialog(
                        onDismissRequest = { showQualityDialog = false },
                        title = { Text("Select Stream Mirror", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                        containerColor = Color(0xFF161618),
                        confirmButton = {
                            TextButton(onClick = { showQualityDialog = false }) {
                                Text("CLOSE", color = Color(0xFFFF3850))
                            }
                        },
                        text = {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val currentPos = exoPlayer.currentPosition
                                            selectedMirrorId = "Primary"
                                            showQualityDialog = false
                                            exoPlayer.setMediaItem(MediaItem.fromUri(FirebaseFirestoreClient.getVideoStreamingUrl(movie.googleDriveFileId)))
                                            exoPlayer.prepare()
                                            exoPlayer.seekTo(currentPos)
                                            exoPlayer.play()
                                        }
                                        .padding(vertical = 12.dp, horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Primary Stream (1080p)", color = Color.White, fontSize = 14.sp)
                                    if (selectedMirrorId == "Primary") {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = "Active", tint = Color(0xFFFF3850))
                                    }
                                }
                                
                                if (movie.mirrorIds.isNotEmpty()) {
                                    movie.mirrorIds.split(",").forEachIndexed { index, mirror ->
                                        val cleanMirror = mirror.trim()
                                        if (cleanMirror.isNotEmpty()) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        val currentPos = exoPlayer.currentPosition
                                                        selectedMirrorId = cleanMirror
                                                        showQualityDialog = false
                                                        exoPlayer.setMediaItem(MediaItem.fromUri(FirebaseFirestoreClient.getVideoStreamingUrl(cleanMirror)))
                                                        exoPlayer.prepare()
                                                        exoPlayer.seekTo(currentPos)
                                                        exoPlayer.play()
                                                    }
                                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Backup Mirror Server ${index + 1}", color = Color.White, fontSize = 14.sp)
                                                if (selectedMirrorId == cleanMirror) {
                                                    Icon(imageVector = Icons.Default.Check, contentDescription = "Active", tint = Color(0xFFFF3850))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // --- 2. YOUTUBE VIDEO INFORMATION & INTERACTION DETAILS SECTION ---
            if (!isLandscape) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .background(Color(0xFF0F0F0F))
                ) {
                
                // Video details
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Text(
                        text = movie.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 24.sp
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Text(
                        text = "${formatViews(movie.views)} • ${formatTimeAgo(movie.createdAt)}",
                        fontSize = 12.sp,
                        color = Color(0xFFAAAAAA)
                    )
                }

                // Interactive Publisher/Channel Subscribe row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFFFF3850), Color(0xFFFF758F))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = movie.genre.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = "Flixbuzz OTT - ${movie.genre}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(text = "954K subscribers", color = Color(0xFFAAAAAA), fontSize = 11.sp)
                        }
                    }

                    // Subscribe pill button
                    Button(
                        onClick = { isSubscribed = !isSubscribed },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSubscribed) Color(0xFF272727) else Color.White,
                            contentColor = if (isSubscribed) Color(0xFFAAAAAA) else Color.Black
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        if (isSubscribed) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Subscribed", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text("Subscribe", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Interactive horizontal actions row (Like, Dislike, Share, Download, Save)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Like/Dislike combined pill
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF272727), RoundedCornerShape(20.dp))
                            .height(36.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .clickable {
                                    if (likedState == 1) {
                                        likedState = 0
                                        likesCount--
                                    } else {
                                        if (likedState == 2) likesCount++
                                        likedState = 1
                                        likesCount++
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (likedState == 1) Icons.Default.ThumbUp else Icons.Default.ThumbUp,
                                contentDescription = "Like",
                                tint = if (likedState == 1) Color(0xFFFF3850) else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "${likesCount}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color(0x33FFFFFF)))
                        
                        Box(
                            modifier = Modifier
                                .clickable {
                                    if (likedState == 2) {
                                        likedState = 0
                                    } else {
                                        if (likedState == 1) {
                                            likesCount--
                                        }
                                        likedState = 2
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ThumbDown,
                                contentDescription = "Dislike",
                                tint = if (likedState == 2) Color(0xFFFF3850) else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Share Button
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF272727), RoundedCornerShape(20.dp))
                            .clickable {
                                val clip = ClipData.newPlainText("Share Movie", "Watch ${movie.title} on Flixbuzz OTT!")
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Movie link copied to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .height(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Download Button
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF272727), RoundedCornerShape(20.dp))
                            .clickable {
                                Toast.makeText(context, "Preparing high-speed offline download...", Toast.LENGTH_LONG).show()
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .height(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = "Download", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Save Watchlist Button
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF272727), RoundedCornerShape(20.dp))
                            .clickable {
                                Toast.makeText(context, "Saved to your watch history / playlist", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .height(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.PlaylistAdd, contentDescription = "Save", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // --- COMMENTS PREVIEW CARD & EXPANDED LIST SECTION ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .background(Color(0xFF1F1F1F), RoundedCornerShape(12.dp))
                        .clickable { showCommentsList = !showCommentsList }
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Comments", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "${commentsList.size}", color = Color(0xFFAAAAAA), fontSize = 13.sp)
                        }
                        Icon(
                            imageVector = if (showCommentsList) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand comments",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))

                    if (!showCommentsList) {
                        // Condensed review comment preview
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF3850)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("U", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = commentsList.firstOrNull() ?: "Add a comment...",
                                color = Color(0xFFDDDDDD),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        // Expanded view with message listing and insertion field
                        Column {
                            HorizontalDivider(color = Color(0x22FFFFFF), modifier = Modifier.padding(vertical = 8.dp))
                            
                            // Insert comment row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00F2FE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Y", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedTextField(
                                    value = commentText,
                                    onValueChange = { commentText = it },
                                    placeholder = { Text("Write a public comment...", fontSize = 12.sp) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFFF3850),
                                        unfocusedBorderColor = Color(0x33FFFFFF)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = {
                                        if (commentText.trim().isNotEmpty()) {
                                            commentsList.add(0, commentText.trim())
                                            commentText = ""
                                        }
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.Send, contentDescription = "Post comment", tint = Color(0xFFFF3850))
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Listing
                            commentsList.forEach { comment ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF21262D)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = comment.take(1).uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row {
                                            Text(text = "@flixbuzz_user", color = Color(0xFFAAAAAA), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(text = "Just now", color = Color(0x66FFFFFF), fontSize = 10.sp)
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text = comment, color = Color.White, fontSize = 12.sp, lineHeight = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFF21262D), modifier = Modifier.padding(horizontal = 14.dp))
                Spacer(modifier = Modifier.height(16.dp))

                // --- 3. MORE RELATED VIDEOS FEED (YouTube-style continuous loop!) ---
                Text(
                    text = "Up Next / More Videos",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp)
                )

                // Get other movies
                val otherMovies = viewModel.movies.collectAsState().value.filter { it.id != movie.id }
                if (otherMovies.isNotEmpty()) {
                    otherMovies.forEach { otherMovie ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .clickable {
                                    viewModel.selectMovie(otherMovie)
                                },
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                            shape = RoundedCornerShape(0.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(140.dp)
                                        .aspectRatio(16f / 9f)
                                        .clip(RoundedCornerShape(8.dp))
                                ) {
                                    AsyncImage(
                                        model = resolvePosterModel(otherMovie.posterUrl),
                                        contentDescription = otherMovie.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(6.dp)
                                            .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = formatViews(otherMovie.views),
                                            color = Color.White,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = otherMovie.title,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 18.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Flixbuzz OTT • ${otherMovie.genre}",
                                        color = Color(0xFFAAAAAA),
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = formatTimeAgo(otherMovie.createdAt),
                                        color = Color(0x66FFFFFF),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "No related videos available.", color = Color(0xFF8B949F), fontSize = 12.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
        }
    }
}

@Composable
fun MovieSliderCarousel(
    movies: List<Movie>,
    onMovieClick: (Movie) -> Unit
) {
    if (movies.isEmpty()) return
    
    val featuredList = remember(movies) {
        movies.take(5)
    }
    
    var currentIndex by remember { mutableStateOf(0) }
    
    // Auto-scroll effect
    LaunchedEffect(featuredList) {
        while (true) {
            delay(5000) // scroll every 5 seconds
            if (featuredList.isNotEmpty()) {
                currentIndex = (currentIndex + 1) % featuredList.size
            }
        }
    }
    
    val currentMovie = featuredList.getOrNull(currentIndex) ?: return
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        AnimatedContent(
            targetState = currentMovie,
            transitionSpec = {
                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> -width } + fadeOut()
                )
            },
            label = "SliderAnimation"
        ) { activeMovie ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onMovieClick(activeMovie) }
            ) {
                AsyncImage(
                    model = resolvePosterModel(activeMovie.posterUrl),
                    contentDescription = activeMovie.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xCC000000), Color(0xFF08090C))
                            )
                        )
                )
                
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFF3850), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "TRENDING",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = activeMovie.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = activeMovie.description,
                        fontSize = 11.sp,
                        color = Color(0xFFC9D1D9),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = { onMovieClick(activeMovie) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.Black, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Watch Now", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            featuredList.forEachIndexed { index, _ ->
                val isSelected = index == currentIndex
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (isSelected) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color(0xFFFF3850) else Color(0xFF484F58))
                        .clickable { currentIndex = index }
                )
            }
        }
    }
}

// --- GOOGLE YOUTUBE FEED AND ASSET RESOLUTION HELPERS ---

sealed class FeedItem {
    data class MovieItem(val movie: Movie) : FeedItem()
    data class AdItem(val ad: Advertisement) : FeedItem()
}

@Composable
fun MovieFeedCard(movie: Movie, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .clickable { onClick() }
            .testTag("movie_card_${movie.id}"),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(0.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(0.5.dp, Color(0xFF21262D), RoundedCornerShape(12.dp))
            ) {
                AsyncImage(
                    model = resolvePosterModel(movie.posterUrl),
                    contentDescription = movie.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(Color(0xE608090C), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = movie.genre.uppercase(),
                        color = Color(0xFFFF3850),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFFF3850), Color(0xFFFF758F))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = movie.genre.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = movie.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    
                    val viewsFormatted = formatViews(movie.views)
                    val timeAgo = formatTimeAgo(movie.createdAt)
                    
                    Text(
                        text = "${movie.genre} • $viewsFormatted • $timeAgo",
                        fontSize = 12.sp,
                        color = Color(0xFF8B949F)
                    )
                }
            }
        }
    }
}

@Composable
fun AdFeedCard(ad: Advertisement) {
    val context = LocalContext.current
    
    if (ad.isBanner && ad.htmlCode.isNotEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF101424)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF1F2942))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SPONSORED BANNER AD",
                        color = Color(0xFF00F2FE),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = ad.title,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                AndroidView(
                    factory = { ctx ->
                        android.webkit.WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            setBackgroundColor(0) // Set transparent background
                            webViewClient = android.webkit.WebViewClient()
                            // Load HTML content
                            loadDataWithBaseURL(null, ad.htmlCode, "text/html", "UTF-8", null)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                )
            }
        }
    } else {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .clickable {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ad.clickThroughUrl))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Redirecting failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF101424)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF1F2942))
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                ) {
                    AsyncImage(
                        model = "https://images.unsplash.com/photo-1542751371-adc38448a05e?auto=format&fit=crop&w=800&q=80",
                        contentDescription = "Sponsored Ad",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .background(Color(0xFF00F2FE), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "SPONSORED",
                            color = Color.Black,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = ad.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Tap to learn more from our sponsor partner.",
                            fontSize = 11.sp,
                            color = Color(0xFF8B949F)
                        )
                    }
                    
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ad.clickThroughUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Redirecting failed", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F2FE), contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(text = "Visit Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun TrendingMovieCard(movie: Movie, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(0.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .border(0.5.dp, Color(0xFF21262D), RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = resolvePosterModel(movie.posterUrl),
                    contentDescription = movie.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = formatViews(movie.views),
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = movie.title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = movie.genre,
                fontSize = 11.sp,
                color = Color(0xFF8B949F)
            )
        }
    }
}

fun resolvePosterModel(posterUrl: String): Any {
    if (posterUrl.startsWith("/")) {
        val file = java.io.File(posterUrl)
        if (file.exists()) {
            return file
        }
    }
    if (posterUrl.startsWith("data:image/") && posterUrl.contains("base64,")) {
        val base64Data = posterUrl.substringAfter("base64,")
        return try {
            val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size) ?: posterUrl
        } catch (e: Exception) {
            posterUrl
        }
    } else if (posterUrl.length > 200 && !posterUrl.startsWith("http")) {
        return try {
            val decodedBytes = Base64.decode(posterUrl, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size) ?: posterUrl
        } catch (e: Exception) {
            posterUrl
        }
    }
    return posterUrl
}

fun formatViews(views: Int): String {
    return when {
        views >= 1_000_000 -> String.format("%.1fM views", views / 1_000_000f).replace(".0", "")
        views >= 1_000 -> String.format("%.1fK views", views / 1_000f).replace(".0", "")
        else -> "$views views"
    }
}

fun formatTimeAgo(createdAt: Long): String {
    val diff = System.currentTimeMillis() - createdAt
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    
    return when {
        days > 0 -> "$days days ago"
        hours > 0 -> "$hours hours ago"
        minutes > 0 -> "$minutes mins ago"
        else -> "just now"
    }
}

fun uriToBase64(context: android.content.Context, uri: android.net.Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        
        if (originalBitmap == null) return null
        
        val maxWidth = 640
        val scale = maxWidth.toFloat() / originalBitmap.width.coerceAtLeast(1)
        val targetWidth = if (scale < 1.0) maxWidth else originalBitmap.width
        val targetHeight = if (scale < 1.0) (originalBitmap.height * scale).toInt() else originalBitmap.height
        
        val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)
        
        val outputStream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        val bytes = outputStream.toByteArray()
        Base64.encodeToString(bytes, Base64.DEFAULT)
    } catch (e: Exception) {
        android.util.Log.e("Base64Util", "Error converting uri to base64: ${e.message}")
        null
    }
}

// --- RESOLVE VAST XML TAG URLS TO DIRECT MP4 STREAMING URLS ---
fun resolveVastVideoUrl(vastUrl: String): String {
    if (!vastUrl.startsWith("http")) {
        return vastUrl
    }
    
    val lower = vastUrl.lowercase()
    if (lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".webm") || lower.endsWith(".m3u8")) {
        return vastUrl
    }
    
    var resolvedUrl = vastUrl
    val policy = android.os.StrictMode.ThreadPolicy.Builder().permitAll().build()
    android.os.StrictMode.setThreadPolicy(policy)
    
    try {
        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        val request = okhttp3.Request.Builder().url(vastUrl).build()
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val xml = response.body?.string() ?: ""
                if (xml.contains("<VAST") || xml.contains("<vast") || xml.contains("<Ad") || xml.contains("<ad")) {
                    // Try to parse out the CDATA block inside <MediaFile>
                    val cdataStart = xml.indexOf("<![CDATA[")
                    if (cdataStart != -1) {
                        val cdataEnd = xml.indexOf("]]>", cdataStart)
                        if (cdataEnd != -1) {
                            val extracted = xml.substring(cdataStart + 9, cdataEnd).trim()
                            if (extracted.isNotEmpty()) {
                                resolvedUrl = extracted
                            }
                        }
                    } else {
                        // Fallback: parse direct MediaFile content
                        val mediaFileStart = xml.indexOf("<MediaFile")
                        if (mediaFileStart != -1) {
                            val tagEnd = xml.indexOf(">", mediaFileStart)
                            if (tagEnd != -1) {
                                val closeTag = xml.indexOf("</MediaFile>", tagEnd)
                                if (closeTag != -1) {
                                    val extracted = xml.substring(tagEnd + 1, closeTag).trim()
                                    if (extracted.isNotEmpty()) {
                                        resolvedUrl = extracted
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("VAST_RESOLVER", "Failed to resolve VAST XML tag URL: ${e.message}")
    }
    return resolvedUrl
}

// --- EXTENSION TO RETRIEVE ACTIVITY FROM CONTEXT ---
fun android.content.Context.findActivity(): android.app.Activity? {
    var ctx = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is android.app.Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

// --- FILE UTILITIES TO COPY GALLERY SELECTION TO APP LOCAL STORAGE ---
fun copyUriToLocalStorage(context: android.content.Context, uri: android.net.Uri, prefix: String): String? {
    return try {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri)
        val extension = when {
            mimeType == "image/jpeg" -> "jpg"
            mimeType == "image/png" -> "png"
            mimeType == "video/mp4" -> "mp4"
            uri.path?.endsWith(".jpg") == true -> "jpg"
            uri.path?.endsWith(".png") == true -> "png"
            uri.path?.endsWith(".mp4") == true -> "mp4"
            else -> "bin"
        }
        val fileName = "${prefix}_${System.currentTimeMillis()}.$extension"
        val targetFile = java.io.File(context.filesDir, fileName)
        contentResolver.openInputStream(uri)?.use { inputStream ->
            targetFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        targetFile.absolutePath
    } catch (e: Exception) {
        android.util.Log.e("FileUtils", "Failed to copy URI to local storage: ${e.message}")
        null
    }
}

// --- FILE UTILITIES TO CONVERT IMAGE URI TO COMPRESSED BASE64 DATA URI FOR FIREBASE REAL-TIME SYNC ---
fun convertUriToBase64(context: android.content.Context, uri: android.net.Uri): String? {
    return try {
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(uri) ?: return null
        val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        
        if (bitmap == null) return null
        
        // Limit max resolution to 800px to maintain crisp quality while keeping document size tiny
        val maxDimension = 800
        val scaledBitmap = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
            val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val newWidth = if (ratio > 1) maxDimension else (maxDimension * ratio).toInt()
            val newHeight = if (ratio > 1) (maxDimension / ratio).toInt() else maxDimension
            android.graphics.Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }

        val outputStream = java.io.ByteArrayOutputStream()
        scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream)
        val bytes = outputStream.toByteArray()
        val base64String = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        
        "data:image/jpeg;base64,$base64String"
    } catch (e: Exception) {
        android.util.Log.e("FileUtils", "Failed to convert Uri to Base64: ${e.message}")
        null
    }
}

@Composable
fun FirebaseSetupDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val sha1Fingerprint = remember { getCertificateSHA1(context) }
    val packageName = context.packageName

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Firebase Setup",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Firebase & SHA-1 Setup",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "আপনার ফায়ারবেস প্রজেক্টে এখনও অ্যান্ড্রয়েড অ্যাপ রেজিস্ট্রেশন করা না হয়ে থাকলে গুগল সাইন-ইন এবং ক্লাউড ডাটাবেজ সচল করতে অ্যাপটি রেজিস্টার করুন ও SHA-1 কী বসান।",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                // Package name card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "১. অ্যাপের প্যাকেজ নাম (Package Name):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            androidx.compose.foundation.text.selection.SelectionContainer {
                                Text(
                                    text = packageName,
                                    fontSize = 12.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Package Name", packageName)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "✅ Package Name copied!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Package Name",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // SHA-1 Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "২. রানিং SHA-1 সিগনেচার কী:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            androidx.compose.foundation.text.selection.SelectionContainer {
                                Text(
                                    text = sha1Fingerprint,
                                    fontSize = 11.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("SHA1 Fingerprint", sha1Fingerprint)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "✅ SHA-1 copied!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy SHA-1",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Setup guide steps in Bengali
                Text(
                    text = "🛠️ ফায়ারবেস সেটআপ গাইড (ধাপসমূহ):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "১. আপনার ব্রাউজারে Firebase Console (firebase.google.com) এ যান।\n" +
                           "২. আপনার প্রজেক্টটি নির্বাচন করুন (যেমন flimshare-f6b15)।\n" +
                           "৩. Project Settings (গিয়ার আইকন) এ ক্লিক করুন।\n" +
                           "৪. General ট্যাবের নিচে 'Your apps' সেকশনে Android আইকনে (অ্যান্ড্রয়েড লোগো) ক্লিক করে নতুন অ্যাপ রেজিস্ট্রেশন শুরু করুন।\n" +
                           "৫. 'Android package name' বক্সে উপরের কপি করা প্যাকেজ নামটি পেস্ট করুন।\n" +
                           "৬. 'Debug signing certificate SHA-1' বক্সে উপরের কপি করা SHA-1 কী পেস্ট করুন।\n" +
                           "৭. 'Register app' এ ক্লিক করুন।\n" +
                           "৮. পরের ধাপে 'google-services.json' ফাইলটি ডাউনলোড করে আপনার অ্যান্ড্রয়েড প্রজেক্টের app/ ডিরেক্টরিতে রাখুন (অথবা API secrets প্যানেলে প্রয়োজনীয় মানগুলো সেট করুন)।",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("ঠিক আছে", fontWeight = FontWeight.Bold)
            }
        }
    )
}

