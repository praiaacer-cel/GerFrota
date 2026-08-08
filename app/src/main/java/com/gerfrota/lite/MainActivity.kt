// ui/Nav.kt  (MainActivity + grafo de navegação)
package com.gerfrota.lite.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gerfrota.lite.ai.ChapaIAViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// ---------------------------------------------------------
// IMPORTS PRESUMIDOS (Ajuste conforme a estrutura do seu projeto)
// ---------------------------------------------------------
import com.gerfrota.lite.data.MigracaoDados
import com.gerfrota.lite.ui.theme.GerFrotaTheme
// import com.gerfrota.lite.ui.screens.* (Importe suas telas aqui)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Executa a migração de dados em background (Thread IO)
        lifecycleScope.launch(Dispatchers.IO) { 
            MigracaoDados.importarSeNecessario(this@MainActivity) 
        }
        
        setContent {
            GerFrotaTheme {
                val nav = rememberNavController()
                val chapaVm: ChapaIAViewModel = viewModel()
                
                NavHost(
                    navController = nav, 
                    startDestination = "dashboard"
                ) {
                    composable("dashboard") { DashboardScreen(nav::navigate) }
                    
                    composable("chapa") { 
                        ChapaIaScreen(chapaVm) { nav.popBackStack() } 
                    }
                    
                    composable("frota") { FrotaListScreen(nav) }
                    
                    composable("frota_detail/{id}") { b ->
                        FrotaDetailScreen(b.arguments?.getLong("id") ?: -1L) { nav.popBackStack() } 
                    }
                    
                    composable("frota_form/{id}") { b ->
                        FrotaFormScreen(b.arguments?.getLong("id") ?: -1L) { nav.popBackStack() } 
                    }
                    
                    composable("motoristas") { MotoristasListScreen(nav) }
                    
                    composable("motorista_detail/{id}") { b ->
                        MotoristaDetailScreen(b.arguments?.getLong("id") ?: -1L) { nav.popBackStack() } 
                    }
                    
                    composable("manutencao") { ManutencaoScreen { nav.popBackStack() } }
                    composable("combustivel") { CombustivelScreen { nav.popBackStack() } }
                    composable("viagens") { ViagensScreen { nav.popBackStack() } }
                    composable("relatorios") { RelatoriosScreen { nav.popBackStack() } }
                }
            }
        }
    }
}
