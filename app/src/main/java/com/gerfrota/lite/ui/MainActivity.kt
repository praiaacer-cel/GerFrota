package com.gerfrota.lite.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gerfrota.lite.ai.ChapaIAViewModel
import com.gerfrota.lite.data.DatabaseHelper
import com.gerfrota.lite.services.MigracaoDados
import com.gerfrota.lite.ui.chapa.ChapaIaScreen
import com.gerfrota.lite.ui.combustivel.*
import com.gerfrota.lite.ui.conjuntos.*
import com.gerfrota.lite.ui.frota.*
import com.gerfrota.lite.ui.login.LoginScreen
import com.gerfrota.lite.ui.manutencao.*
import com.gerfrota.lite.ui.motoristas.*
import com.gerfrota.lite.ui.pneus.*
import com.gerfrota.lite.ui.relatorios.*
import com.gerfrota.lite.ui.viagens.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch(Dispatchers.IO) { MigracaoDados.importarSeNecessario(this@MainActivity) }
        val prefs = getSharedPreferences("gerfrota", MODE_PRIVATE)
        setContent {
            GerFrotaTheme {
                val nav = rememberNavController()
                val chapaVm: ChapaIAViewModel = viewModel()
                fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
                fun dec(s: String) = URLDecoder.decode(s, "UTF-8")
                NavHost(nav, if (prefs.getBoolean("logado", false)) "dashboard" else "login") {
                    composable("login") { LoginScreen(prefs) { nav.navigate("dashboard") { popUpTo("login") { inclusive = true } } } }
                    composable("dashboard") { DashboardScreen(nav::navigate) }
                    composable("chapa") { ChapaIaScreen(chapaVm) { nav.popBackStack() } }
                    composable("frota") { FrotaListScreen(nav) }
                    composable("frota_detail/{id}") { b -> FrotaDetailScreen(b.arguments?.getLong("id") ?: -1, nav) { nav.popBackStack() } }
                    composable("frota_form/{id}") { b -> FrotaFormScreen(b.arguments?.getLong("id") ?: -1) { nav.popBackStack() } }
                    composable("motoristas") { MotoristasListScreen(nav) }
                    composable("motorista_detail/{id}") { b -> MotoristaDetailScreen(b.arguments?.getLong("id") ?: -1) { nav.popBackStack() } }
                    composable("adiantamentos/{id}") { b -> AdiantamentosScreen(b.arguments?.getLong("id") ?: -1, nav) }
                    composable("acerto_periodo/{id}") { b -> AcertoPeriodoScreen(b.arguments?.getLong("id") ?: -1, nav) }
                    composable("acerto_contas/{id}/{ini}/{fim}") { b -> AcertoContasScreen(b.arguments?.getLong("id") ?: -1, b.arguments?.getLong("ini") ?: 0, b.arguments?.getLong("fim") ?: 0, nav) }
                    composable("conjuntos") { ConjuntosListScreen(onNovo = { nav.navigate("conjunto_form/-1") }, onEditar = { id -> nav.navigate("conjunto_form/$id") }, onBack = { nav.popBackStack() }) }
                    composable("conjunto_form/{id}") { b -> ConjuntoFormScreen(b.arguments?.getLong("id") ?: -1) { nav.popBackStack() } }
                    composable("manutencao_list") { ManutencaoListScreen(nav) }
                    composable("manutencao_detail/{placa}/{tipo}") { b -> ManutencaoDetailScreen(b.arguments?.getString("placa") ?: "", dec(b.arguments?.getString("tipo") ?: ""), nav) }
                    composable("manutencao_form/{placa}/{tipo}/{manutId}/{pneuId}") { b ->
                        val resultadoPneu = b.savedStateHandle?.get<String>("resultado_pneu")
                        ManutencaoFormScreen(b.arguments?.getString("placa") ?: "", dec(b.arguments?.getString("tipo") ?: ""), b.arguments?.getLong("manutId") ?: -1, b.arguments?.getLong("pneuId") ?: -1, resultadoPneu, { b.savedStateHandle?.remove("resultado_pneu") }, nav)
                    }
                    composable("pneus_gestao/{placa}/{tipo}") { b ->
                        val placa = b.arguments?.getString("placa") ?: ""
                        val tipo = dec(b.arguments?.getString("tipo") ?: "")
                        val ctx = LocalContext.current
                        val db = remember { DatabaseHelper.get(ctx) }
                        val v = remember { db.queryAll("frota").firstOrNull { db.str(it["placa"]) == placa } }
                        PneusGestaoScreen(placa, tipo, db.str(v?.get("marca")), db.str(v?.get("modelo")),
                            onMap = { nav.navigate("pneus_map/$placa/${enc(tipo)}/0") },
                            onServicos = { nav.navigate("pneus_servicos/$placa") },
                            onRodizio = { nav.navigate("pneus_rodizio/$placa/${enc(tipo)}") },
                            onEstoque = { nav.navigate("pneus_estoque/$placa/${enc(tipo)}") },
                            onBack = { nav.popBackStack() })
                    }
                    composable("pneus_map/{placa}/{tipo}/{sel}") { b -> PneusMapScreen(b.arguments?.getString("placa") ?: "", dec(b.arguments?.getString("tipo") ?: ""), b.arguments?.getString("sel") == "1", onResult = { v -> nav.previousBackStackEntry?.savedStateHandle?.set("resultado_pneu", v); nav.popBackStack() }, onBack = { nav.popBackStack() }) }
                    composable("pneus_rodizio/{placa}/{tipo}") { b -> PneusRodizioScreen(b.arguments?.getString("placa") ?: "", dec(b.arguments?.getString("tipo") ?: "")) { nav.popBackStack() } }
                    composable("pneus_estoque/{placa}/{tipo}") { b -> PneusEstoqueScreen(b.arguments?.getString("placa") ?: "", dec(b.arguments?.getString("tipo") ?: "")) { nav.popBackStack() } }
                    composable("pneus_servicos/{placa}") { b -> PneusServicosScreen(b.arguments?.getString("placa") ?: "") { nav.popBackStack() } }
                    composable("viagens_unidades") { ViagensUnidadesScreen(nav) }
                    composable("viagem_list/{unidadeId}") { b -> ViagemListScreen(b.arguments?.getLong("unidadeId") ?: -1, nav) }
                    composable("viagem_form/{unidadeId}/{viagemId}") { b -> ViagemFormScreen(b.arguments?.getLong("unidadeId") ?: -1, b.arguments?.getLong("viagemId") ?: -1, nav) }
                    composable("combustivel_selecao") { VeiculosSelecaoScreen(onSelecionar = { placa, id -> nav.navigate("combustivel_menu/$placa/$id") }, onBack = { nav.popBackStack() }) }
                    composable("combustivel_menu/{placa}/{veiculoId}") { b ->
                        val placa = b.arguments?.getString("placa") ?: ""
                        val id = b.arguments?.getLong("veiculoId") ?: 0L
                        val ctx = LocalContext.current
                        val db = remember { DatabaseHelper.get(ctx) }
                        val mm = remember { db.queryAll("frota").firstOrNull { db.str(it["placa"]) == placa }?.let { "${db.str(it["marca"])} ${db.str(it["modelo"])}".trim() } ?: "" }
                        CombustivelArlaMenuScreen(placa, mm, onCombustivel = { nav.navigate("abastecimento_hist/COMBUSTIVEL/$placa/$id") }, onArla = { nav.navigate("abastecimento_hist/ARLA/$placa/$id") }, onBack = { nav.popBackStack() })
                    }
                    composable("abastecimento_hist/{tipo}/{placa}/{veiculoId}") { b ->
                        val tipo = if (b.arguments?.getString("tipo") == "ARLA") TipoAbastecimento.ARLA else TipoAbastecimento.COMBUSTIVEL
                        val placa = b.arguments?.getString("placa") ?: ""
                        val id = b.arguments?.getLong("veiculoId") ?: 0L
                        AbastecimentoHistoricoScreen(tipo, placa, id, onNovo = { nav.navigate("abastecimento_form/${tipo.name}/$placa/$id/-1") }, onEditar = { r -> nav.navigate("abastecimento_form/${tipo.name}/$placa/$id/$r") }, onBack = { nav.popBackStack() })
                    }
                    composable("abastecimento_form/{tipo}/{placa}/{veiculoId}/{registroId}") { b ->
                        val tipo = if (b.arguments?.getString("tipo") == "ARLA") TipoAbastecimento.ARLA else TipoAbastecimento.COMBUSTIVEL
                        AbastecimentoFormScreen(tipo, b.arguments?.getString("placa") ?: "", b.arguments?.getLong("veiculoId") ?: 0L, b.arguments?.getLong("registroId") ?: -1) { nav.popBackStack() }
                    }
                    composable("relatorios") { RelatoriosScreen(nav) }
                    composable("rel_rent") { RelatorioRentabilidadeScreen() }
                    composable("rel_acerto") { RelatorioAcertoMotoristaScreen() }
                    composable("rel_pneus") { RelatorioDesempenhoPneusScreen() }
                    composable("rel_comb") { RelatorioCombustivelScreen() }
                    composable("rel_manut") { RelatorioManutencaoScreen() }
                    composable("rel_fluxo") { RelatorioFluxoCaixaScreen() }
                    composable("rel_receber") { RelatorioContasReceberScreen() }
                }
            }
        }
    }
}
