package com.caiana.talks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import com.caiana.talks.ui.theme.LcarsColors
import com.caiana.talks.ui.theme.LcarsTheme
import androidx.lifecycle.lifecycleScope
import com.caiana.talks.data.repository.ConversationRepository
import com.caiana.talks.ui.navigation.AppNavGraph
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var conversationRepository: ConversationRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch { conversationRepository.recoverDanglingSessions() }
        setContent {
            LcarsTheme {
                Surface(color = LcarsColors.Black) {
                    AppNavGraph()
                }
            }
        }
    }
}
