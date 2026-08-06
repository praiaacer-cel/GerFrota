package com.gerfrota.lite.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gerfrota.lite.ai.ChapaIAViewModel
import com.gerfrota.lite.data.DatabaseHelper
import com.gerfrota.lite.ui.conjuntos.ConjuntoFormScreen
import com.gerfrota.lite.ui.conjuntos.ConjuntosListScreen
import com.gerfrota.lite.ui.pneus.*
import com.gerfrota.lite.ui.viagens.*
import com.gerfrota.lite.ui.acertos.*
import com.gerfrota.lite.ui.combustivel.*
import com.gerfrota.lite.ui.login.LoginScreen
import com.gerfrota.lite.ui.relatorios.*
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.net.URLDecoder
import java.net.URLEncoder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GerFrotaTheme {
                val nav = rememberNavController()
                val chapaVm: ChapaIAViewModel = viewModel()
                val prefs = LocalContext.current.getSharedPreferences("gerfrota", android.content.Context.MODE_PRIVATE)
                val inicio = if (prefs.getBoolean("logado", false)) "dashboard" else "login"
                
                fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
                fun dec(s: String) = URLDecoder.decode(s, "UTF-8")

                NavHost(nav, inicio) {
                    // ---- LOGIN ----
                    composable("login") { 
                        LoginScreen(prefs) { 
                            nav.navigate("dashboard") { 
                                popUpTo("login") { inclusive = true } 
                            } 
                        } 
                    }
                    
                    // ---- DASHBOARD ----
                    composable("dashboard") { DashboardScreen(nav::navigate) }
                    composable("chapa") { ChapaIaScreen(chapaVm) { nav.popBackStack() } }

                    composable("frota") { FrotaListScreen(nav) }
                    composable("frota_detail/{id}") { b ->
                        FrotaDetailScreen(b.arguments?.getLong("id") ?: -1, nav) { nav.popBackStack() } }
                    composable("frota_form/{id}") { b ->
                        FrotaFormScreen(b.arguments?.getLong("id") ?: -1) { nav.popBackStack() } }

                    composable("motoristas") { MotoristasListScreen(nav) }
                    composable("motorista_detail/{id}") { b ->
                        MotoristaDetailScreen(b.arguments?.getLong("id") ?: -1) { nav.popBackStack() } }

                    composable("manutencao") { ManutencaoScreen { nav.popBackStack() } }

                    // ---- MANUTENÇÃO ----
                    composable("manutencao_list") { 
                        ManutencaoListScreen(nav) 
                    }

                    composable("manutencao_detail/{placa}/{tipo}") { b ->
                        ManutencaoDetailScreen(
                            b.arguments?.getString("placa") ?: "",
                            URLDecoder.decode(b.arguments?.getString("tipo") ?: "", "UTF-8"), 
                            nav
                        )
                    }

                    composable("manutencao_form/{placa}/{tipo}/{manutencaoId}/{pneuId}") { b ->
                        val resultado = b.savedStateHandle.get<String>("resultado_pneu")
                        ManutencaoFormScreen(
                            placa = b.arguments?.getString("placa") ?: "",
                            tipo = URLDecoder.decode(b.arguments?.getString("tipo") ?: "", "UTF-8"),
                            manutencaoId = b.arguments?.getLong("manutencaoId") ?: -1,
                            pneuId = b.arguments?.getLong("pneuId") ?: -1,
                            resultadoPneu = resultado,
                            onConsumirResultado = { b.savedStateHandle.remove<String>("resultado_pneu") },
                            nav = nav
                        )
                    }

                    // ---- PNEUS ----
                    composable("pneus_gestao/{placa}/{tipo}") { b ->
                        val placa = b.arguments?.getString("placa") ?: ""
                        val tipo = dec(b.arguments?.getString("tipo") ?: "")
                        PneusGestaoScreen(placa, tipo, "", "",
                            onMap = { nav.navigate("pneus_map/$placa/${enc(tipo)}/0") },
                            onServicos = { nav.navigate("pneus_servicos/$placa") },
                            onRodizio = { nav.navigate("pneus_rodizio/$placa/${enc(tipo)}") },
                            onEstoque = { nav.navigate("pneus_estoque/$placa/${enc(tipo)}") },
                            onBack = { nav.popBackStack() })
                    }
                    
                    composable("pneus_map/{placa}/{tipo}/{sel}") { b ->
                        val placa = b.arguments?.getString("placa") ?: ""
                        val tipo = dec(b.arguments?.getString("tipo") ?: "")
                        val sel = b.arguments?.getString("sel") == "1"
                        PneusMapScreen(placa, tipo, sel,
                            onResult = { valor ->
                                nav.previousBackStackEntry?.savedStateHandle?.set("resultado_pneu", valor)
                                nav.popBackStack()
                            },
                            onBack = { nav.popBackStack() })
                    }
                    
                    composable("pneus_rodizio/{placa}/{tipo}") { b ->
                        PneusRodizioScreen(b.arguments?.getString("placa") ?: "",
                            dec(b.arguments?.getString("tipo") ?: ""), { nav.popBackStack() })
                    }
                    composable("pneus_estoque/{placa}/{tipo}") { b ->
                        PneusEstoqueScreen(b.arguments?.getString("placa") ?: "",
                            dec(b.arguments?.getString("tipo") ?: ""), { nav.popBackStack() })
                    }
                    composable("pneus_servicos/{placa}") { b ->
                        PneusServicosScreen(b.arguments?.getString("placa") ?: "", { nav.popBackStack() })
                    }

                    // ---- CONJUNTOS ----
                    composable("conjuntos") {
                        ConjuntosListScreen(
                            onNovo = { nav.navigate("conjuntos_form/-1") },
                            onEditar = { id -> nav.navigate("conjuntos_form/$id") },
                            onBack = { nav.popBackStack() })
                    }
                    composable("conjuntos_form/{id}") { b ->
                        ConjuntoFormScreen(b.arguments?.getLong("id") ?: -1) { nav.popBackStack() }
                    }

                    // ---- VIAGENS E FRETES ----
                    composable("viagens_unidades") { ViagensUnidadesScreen(nav) }
                    composable("viagem_list/{unidadeId}") { b ->
                        ViagemListScreen(b.arguments?.getLong("unidadeId") ?: -1, nav) }
                    composable("viagem_form/{unidadeId}/{viagemId}") { b ->
                        ViagemFormScreen(b.arguments?.getLong("unidadeId") ?: -1, b.arguments?.getLong("viagemId") ?: -1, nav) }

                    // ---- ADIANTAMENTOS E ACERTOS ----
                    composable("adiantamentos/{motoristaId}") { b ->
                        AdiantamentosScreen(b.arguments?.getLong("motoristaId") ?: -1, nav) }
                    composable("acerto_periodo/{motoristaId}") { b ->
                        AcertoPeriodoScreen(b.arguments?.getLong("motoristaId") ?: -1, nav) }
                    composable("acerto_contas/{motoristaId}/{inicio}/{fim}") { b ->
                        AcertoContasScreen(b.arguments?.getLong("motoristaId") ?: -1,
                            b.arguments?.getLong("inicio") ?: 0, b.arguments?.getLong("fim") ?: 0, nav) }

                    // ---- COMBUSTÍVEL E ARLA ----
                    composable("combustivel_selecao") {
                        VeiculosSelecaoScreen(
                            onSelecionar = { placa, id -> nav.navigate("combustivel_menu/$placa/$id") },
                            onBack = { nav.popBackStack() })
                    }
                    composable("combustivel_menu/{placa}/{veiculoId}") { b ->
                        val placa = b.arguments?.getString("placa") ?: ""
                        val id = b.arguments?.getLong("veiculoId") ?: 0L
                        val mm = remember(placa) {
                            val db = DatabaseHelper.get(LocalContext.current)
                            db.queryAll("frota").firstOrNull { db.str(it["placa"]) == placa }
                                ?.let { "${db.str(it["marca"])} ${db.str(it["modelo"])}".trim() } ?: ""
                        }
                        CombustivelArlaMenuScreen(placa, mm,
                            onCombustivel = { nav.navigate("combustivel_hist/$placa/$id") },
                            onArla = { nav.navigate("arla_hist/$placa/$id") },
                            onBack = { nav.popBackStack() })
                    }
                    composable("combustivel_hist/{placa}/{veiculoId}") { b ->
                        val placa = b.arguments?.getString("placa") ?: ""
                        val id = b.arguments?.getLong("veiculoId") ?: 0L
                        val mm = remember(placa) {
                            val db = DatabaseHelper.get(LocalContext.current)
                            db.queryAll("frota").firstOrNull { db.str(it["placa"]) == placa }
                                ?.let { "${db.str(it["marca"])} ${db.str(it["modelo"])}".trim() } ?: ""
                        }
                        CombustivelHistoricoScreen(placa, id, mm,
                            onNovo = { nav.navigate("combustivel_form/$placa/$id/-1") },
                            onEditar = { r -> nav.navigate("combustivel_form/$placa/$id/$r") },
                            onBack = { nav.popBackStack() })
                    }
                    composable("combustivel_form/{placa}/{veiculoId}/{registroId}") { b ->
                        CombustivelFormScreen(
                            b.arguments?.getString("placa") ?: "",
                            b.arguments?.getLong("veiculoId") ?: 0L,
                            b.arguments?.getLong("registroId") ?: -1L) { nav.popBackStack() }
                    }
                    composable("arla_hist/{placa}/{veiculoId}") { b ->
                        val placa = b.arguments?.getString("placa") ?: ""
                        val id = b.arguments?.getLong("veiculoId") ?: 0L
                        val mm = remember(placa) {
                            val db = DatabaseHelper.get(LocalContext.current)
                            db.queryAll("frota").firstOrNull { db.str(it["placa"]) == placa }
                                ?.let { "${db.str(it["marca"])} ${db.str(it["modelo"])}".trim() } ?: ""
                        }
                        ArlaHistoricoScreen(placa, id, mm,
                            onNovo = { nav.navigate("arla_form/$placa/$id/-1") },
                            onEditar = { r -> nav.navigate("arla_form/$placa/$id/$r") },
                            onBack = { nav.popBackStack() })
                    }
                    composable("arla_form/{placa}/{veiculoId}/{registroId}") { b ->
                        ArlaFormScreen(
                            b.arguments?.getString("placa") ?: "",
                            b.arguments?.getLong("veiculoId") ?: 0L,
                            b.arguments?.getLong("registroId") ?: -1L) { nav.popBackStack() }
                    }

                    // ---- RELATÓRIOS ----
                    composable("relatorios") { RelatoriosScreen(nav) }
                    composable("rel_rent") { RelatorioRentabilidadeScreen() }
                    composable("rel_acerto") { RelatorioAcertoMotoristaScreen() }
                    composable("rel_pneus") { RelatorioDesempenhoPneusScreen() }
                    composable("rel_comb") { RelatorioCombustivelScreen() }
                    composable("rel_manut") { RelatorioManutencaoScreen() }
                    composable("rel_fluxo") { RelatorioFluxoCaixaScreen() }
                    composable("rel_receber") { RelatorioContasReceberScreen() }

                    // ---- LEGADO / OUTROS ----
                    composable("combustivel") { CombustivelScreen { nav.popBackStack() } }
                    composable("viagens") { ViagensScreen { nav.popBackStack() } }
                }
            }
        }
    }
}
