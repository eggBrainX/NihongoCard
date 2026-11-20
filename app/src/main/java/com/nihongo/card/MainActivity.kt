package com.nihongo.card

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Prefs.init(applicationContext)
        setContent {
            val dark = when (Prefs.theme) { 0 -> isSystemInDarkTheme(); 1 -> false; else -> true }
            NeoTheme(dark) { App() }
        }
    }
}

@Composable
fun App() {
    val nav = rememberNavController()
    val back: () -> Unit = { nav.popBackStack() }
    NavHost(nav, "home", Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        composable("home") { HomeScreen { nav.navigate(it) } }
        composable("study") { StudyScreen(back) }
        composable("quiz") { QuizScreen(false, back) }
        composable("sim") { QuizScreen(true, back) }
        composable("write") { WriteScreen(back) }
        composable("match") { MatchScreen(back) }
        composable("ai") { AiScreen(back) }
        composable("settings") { SettingsScreen(back) }
    }
}
