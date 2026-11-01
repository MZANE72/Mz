package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.PlaylistConfig
import com.example.ui.screens.ExpiredScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MainScreen
import com.example.ui.theme.ZanaIptvTheme
import com.example.viewmodel.IptvViewModel
import com.example.viewmodel.IptvViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as ZanaIptvApplication
        val repository = app.repository

        setContent {
            val viewModel: IptvViewModel = viewModel(
                factory = IptvViewModelFactory(repository)
            )

            val config by viewModel.playlistConfig.collectAsState()
            val isLoading by viewModel.isLoading.collectAsState()
            val loadingMessage by viewModel.loadingMessage.collectAsState()
            val errorMessage by viewModel.errorMessage.collectAsState()

            ZanaIptvTheme(themeMode = config.themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        !config.isConfigured() -> {
                            LoginScreen(
                                isLoading = isLoading,
                                loadingMessage = loadingMessage,
                                errorMessage = errorMessage,
                                onClearError = { viewModel.clearError() },
                                onLoadUrl = { url, exp, name ->
                                    viewModel.loadFromUrl(url, exp, name)
                                },
                                onLoadFile = { uri, exp ->
                                    viewModel.loadFromFile(uri, this, exp)
                                },
                                onLoginXtream = { server, user, pass, exp ->
                                    viewModel.loginXtream(server, user, pass, exp)
                                },
                                onLoadDemoStreams = {
                                    viewModel.loadDemoStreams()
                                },
                                onLoadStalker = { portalUrl, mac, name, exp ->
                                    viewModel.loadStalkerPortal(portalUrl, mac, name, exp)
                                },
                                themeMode = config.themeMode,
                                onToggleTheme = {
                                    viewModel.setThemeMode(
                                        if (config.themeMode == PlaylistConfig.THEME_DARK) {
                                            PlaylistConfig.THEME_SYSTEM
                                        } else {
                                            PlaylistConfig.THEME_DARK
                                        }
                                    )
                                }
                            )
                        }

                        config.isExpired() -> {
                            ExpiredScreen(
                                config = config,
                                onAddNewPlaylist = { viewModel.logout() }
                            )
                        }

                        else -> {
                            MainScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
