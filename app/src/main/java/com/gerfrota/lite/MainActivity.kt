// ui/Nav.kt  (MainActivity + grafo de navegação)
package com.gerfrota.lite.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gerfrota.lite.ai.ChapaIAViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch(Dispatchers.IO) { MigracaoDados.importarSeNecessario(this@MainActivity) }
        setContent {
            GerFrotaTheme {
                val nav = rememberNavController()
                val chapaVm: ChapaIAViewModel = viewModel()
                NavHost(nav, "dashboard") {
                    composable("dashboard") { DashboardScreen(nav::navigate) }
                    composable("chapa") { ChapaIaScreen(chapaVm) { nav.popBackStack() } }
                    composable("frota") { FrotaListScreen(nav) }
                    composable("frota_detail/{id}") { b ->
                        FrotaDetailScreen(b.arguments?.getLong("id") ?: -1) { nav.popBackStack() } }
                    composable("frota_form/{id}") { b ->
                        FrotaFormScreen(b.arguments?.getLong("id") ?: -1) { nav.popBackStack() } }
                    composable("motoristas") { MotoristasListScreen(nav) }
                    composable("motorista_detail/{id}") { b ->
                        MotoristaDetailScreen(b.arguments?.getLong("id") ?: -1) { nav.popBackStack() } }
                    composable("manutencao") { ManutencaoScreen { nav.popBackStack() } }
                    composable("combustivel") { CombustivelScreen { nav.popBackStack() } }
                    composable("viagens") { ViagensScreen { nav.popBackStack() } }
                    composable("relatorios") { RelatoriosScreen { nav.popBackStack() } }
                }
            }
        }
    }
}
