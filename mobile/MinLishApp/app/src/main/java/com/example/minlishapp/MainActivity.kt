package com.example.minlishapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.minlishapp.data.*
import com.example.minlishapp.ui.navigation.AppNavGraph
import com.example.minlishapp.ui.screens.*
import com.example.minlishapp.ui.theme.MinLishAppTheme
import com.example.minlishapp.ui.viewmodel.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.minlishapp.core.network.TokenManager

class MainActivity : ComponentActivity() {
    private var onTokenReceivedCallback: ((String, String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current

            // Request Notification Permission on Android 13+
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    Log.d("MainActivity", "Notification permission granted.")
                } else {
                    Log.d("MainActivity", "Notification permission denied.")
                }
            }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            // ============================================================
            // CORE UI STATE
            // ============================================================
            val sharedPrefs = remember { context.getSharedPreferences("minlish_prefs", android.content.Context.MODE_PRIVATE) }
            val savedLanguage = remember { sharedPrefs.getString("app_language", "Vietnamese") ?: "Vietnamese" }
            var isDarkTheme by remember { mutableStateOf(false) }
            var userProgress by remember { mutableStateOf(UserProgress(appLanguage = savedLanguage)) }
            var activeDeck by remember { mutableStateOf<Deck?>(null) }

            LaunchedEffect(userProgress.appLanguage) {
                sharedPrefs.edit().putString("app_language", userProgress.appLanguage).apply()
            }

            // ============================================================
            // NAVIGATION CONTROLLER
            // ============================================================
            val navController = rememberNavController()

            // Setup deep link token receiver
            val tokenManager = remember { TokenManager.getInstance(context) }
            LaunchedEffect(Unit) {
                onTokenReceivedCallback = { token, refreshToken ->
                    tokenManager.saveToken(token)
                    tokenManager.saveRefreshToken(refreshToken)
                    navController.navigate(AppRoute.ResetPassword.route)
                }
                handleIntent(intent)
            }

            // ============================================================
            // VIEWMODELS (MVVM Architecture)
            // ============================================================
            val vocabViewModel: VocabViewModel = viewModel()
            val authViewModel: AuthViewModel = viewModel()
            val statsViewModel: StatsViewModel = viewModel()
            val learningViewModel: LearningViewModel = viewModel()
            val profileViewModel: ProfileViewModel = viewModel()
            val tutorViewModel: TutorViewModel = viewModel()

            // ============================================================
            // SCREEN ROUTING WITH NAVIGATION COMPONENT
            // ============================================================
            MinLishAppTheme(darkTheme = isDarkTheme) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (currentRoute in AppRoute.bottomBarRoutes) {
                            AppBottomBar(
                                navController = navController,
                                currentRoute = currentRoute,
                                appLanguage = userProgress.appLanguage
                            )
                        }
                    }
                ) { innerPadding ->
                    AppNavGraph(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding),
                        userProgress = userProgress,
                        onProgressUpdate = { userProgress = it },
                        isDarkTheme = isDarkTheme,
                        onThemeToggle = { isDarkTheme = !isDarkTheme },
                        activeDeck = activeDeck,
                        onActiveDeckSelect = { activeDeck = it },
                        vocabViewModel = vocabViewModel,
                        authViewModel = authViewModel,
                        statsViewModel = statsViewModel,
                        learningViewModel = learningViewModel,
                        profileViewModel = profileViewModel,
                        tutorViewModel = tutorViewModel
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        val data: android.net.Uri? = intent?.data
        if (data != null && data.scheme == "minlish" && data.host == "reset-password") {
            var token: String? = null
            var refreshToken: String? = null
            val fragment = data.fragment
            if (!fragment.isNullOrEmpty()) {
                val params = fragment.split("&")
                for (param in params) {
                    val pair = param.split("=")
                    if (pair.size == 2) {
                        if (pair[0] == "access_token") {
                            token = pair[1]
                        } else if (pair[0] == "refresh_token") {
                            refreshToken = pair[1]
                        }
                    }
                }
            }
            if (token == null) {
                token = data.getQueryParameter("access_token")
            }
            if (refreshToken == null) {
                refreshToken = data.getQueryParameter("refresh_token")
            }

            if (!token.isNullOrEmpty() && !refreshToken.isNullOrEmpty()) {
                Log.d("MainActivity", "Successfully extracted deep link access token and refresh token.")
                onTokenReceivedCallback?.invoke(token, refreshToken)
            } else {
                Log.e("MainActivity", "Deep link matching reset-password but access_token or refresh_token is missing.")
            }
        }
    }
}
