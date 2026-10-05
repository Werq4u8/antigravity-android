package com.google.antigravity.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.google.antigravity.mobile.agent.AgentController
import com.google.antigravity.mobile.ui.MainScreen
import com.google.antigravity.mobile.ui.theme.AntigravityTheme

class MainActivity : ComponentActivity() {

    private lateinit var agentController: AgentController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        agentController = AgentController(applicationContext)

        setContent {
            AntigravityTheme {
                MainScreen(controller = agentController)
            }
        }
    }
}
