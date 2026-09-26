package com.example.crmxaydung

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.crmxaydung.data.ApiClient
import com.example.crmxaydung.data.UserSession
import com.example.crmxaydung.theme.CRMXayDungTheme
import com.example.crmxaydung.ui.CRMMainScreen
import com.example.crmxaydung.ui.LoginScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CRMXayDungTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var session by remember { mutableStateOf<UserSession?>(ApiClient.currentUser) }

                    if (session == null) {
                        LoginScreen(
                            onLoginSuccess = { user ->
                                session = user
                            }
                        )
                    } else {
                        CRMMainScreen(
                            user = session!!,
                            onLogout = {
                                session = null
                            }
                        )
                    }
                }
            }
        }
    }
}
