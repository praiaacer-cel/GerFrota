package com.gerfrota.lite.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gerfrota.lite.ai.ChapaIAViewModel
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
                fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")
                fun dec(s: String): String = URLDecoder.decode(s, "UTF-8")
                fun NavBackStackEntry.str(name: String): String = arguments?.getString(name) ?: ""
                fun NavBackStackEntry.long(name: String, default: Long): Long = arguments?.getLong(name) ?: default

                NavHost(nav, if (prefs.getBoolean("logado", false)) "dashboard" else "login") {
                    composable("login") {
                        LoginScreen(prefs) { nav.navigate("dashboard") { popUpTo("login") { inclusive = true } } }
                    }
                    composable("dashboard") { DashboardScreen(nav::navigate) }
                    composable("chapa") { ChapaIaScreen(chapaVm) { nav.popBackStack() } }

                    composable("frota") { FrotaListScreen(nav) }
                    composable("frota_detail/{id}") { b ->
                        FrotaDetailScreen(b.long("id", -1L), nav) { nav.popBackStack() }
                    }
                    composable("frota_form/{id}") { b ->
                        FrotaFormScreen(b.long("id", -1L)) { nav.popBackStack() }
                    }

                    composable("motoristas") { MotoristasListScreen(nav) }
                    composable("motorista_detail/{id}") { b ->
                        MotoristaDetailScreen(b.long("id", -1L)) { nav.popBackStack() }
                    }
                    composable("motorista_form/{id}") { b ->
                        MotoristaFormScreen(b.long("id", -1L)) { nav.popBackStack() }
                    }
                    composable("adiantamentos/{id}") { b ->
                        AdiantamentosScreen(b.long("id", -1L), nav)
                    }
                    composable("acerto_periodo/{id}") { b ->
                        AcertoPeriodoScreen(b.long("id", -1L), nav)
                    }
                    composable("acerto_contas/{id}/{ini}/{fim}") { b ->
                        AcertoContasScreen(b.long("id", -1L), b.long("ini", 0L), b.long("fim", 0L), nav)
                    }

                    composable("conjuntos") {
                        ConjuntosListScreen(
                            onNovo = { nav.navigate("conjunto_form/-1") },
                            onEditar = { id: Long -> nav.navigate("conjunto_form/$id") },
                            onBack = { nav.popBackStack() }
                        )
                    }
                    composable("conjunto_form/{id}") { b ->
                        ConjuntoFormScreen(b.long("id", -1L)) { nav.popBackStack() }
                    }

                    composable("manutencao_list") { ManutencaoListScreen(nav) }
                    composable("manutencao_detail/{placa}/{tipo}") { b ->
                        ManutencaoDetailScreen(b.str("placa"), dec(b.str("tipo")), nav)
                    }
                    composable("manutencao_form/{placa}/{tipo}/{manutId}/{pneuId}") { b ->
                        val resultadoPneu: String? = b.savedStateHandle?.get<String>("resultado_pneu")
                        ManutencaoFormScreen(
                            b.str("placa"), dec(b.str("tipo")),
                            b.long("manutId", -1L), b.long("pneuId", -1L),
                            resultadoPneu,
                            { b.savedStateHandle?.remove("resultado_pneu") },
                            nav
                        )
                    }

                    composable("pneus_gestao/{placa}/{tipo}") { b ->
                        val placa: String = b.str("placa")
                        val tipo: String = dec(b.str("tipo"))
                        PneusGestaoScreen(
                            placa = placa,
                            tipo = tipo,
                            onMap = { nav.navigate("pneus_map/$placa/${enc(tipo)}/0") },
                            onServicos = { nav.navigate("pneus_servicos/$placa") },
                            onRodizio = { nav.navigate("pneus_rodizio/$placa/${enc(tipo)}") },
                            onEstoque = { nav.navigate("pneus_estoque/$placa/${enc(tipo)}") },
                            onBack = { nav.popBackStack() }
                        )
                    }
                    composable("pneus_map/{placa}/{tipo}/{sel}") { b ->
                        PneusMapScreen(
                            b.str("placa"), dec(b.str("tipo")), b.str("sel") == "1",
                            onResult = { v: String? ->
                                nav.previousBackStackEntry?.savedStateHandle?.set("resultado_pneu", v)
                                nav.popBackStack()
                            },
                            onBack = { nav.popBackStack() }
                        )
                    }
                    composable("pneus_rodizio/{placa}/{tipo}") { b ->
                        PneusRodizioScreen(b.str("placa"), dec(b.str("tipo"))) { nav.popBackStack() }
                    }
                    composable("pneus_estoque/{placa}/{tipo}") { b ->
                        PneusEstoqueScreen(b.str("placa"), dec(b.str("tipo"))) { nav.popBackStack() }
                    }
                    composable("pneus_servicos/{placa}") { b ->
                        PneusServicosScreen(b.str("placa")) { nav.popBackStack() }
                    }

                    composable("viagens_unidades") { ViagensUnidadesScreen(nav) }
                    composable("viagem_list/{unidadeId}") { b ->
                        ViagemListScreen(b.long("unidadeId", -1L), nav)
                    }
                    composable("viagem_form/{unidadeId}/{viagemId}") { b ->
                        ViagemFormScreen(b.long("unidadeId", -1L), b.long("viagemId", -1L), nav)
                    }

                    composable("combustivel_selecao") {
                        VeiculosSelecaoScreen(
                            onSelecionar = { placa: String, id: Long -> nav.navigate("combustivel_menu/$placa/$id") },
                            onBack = { nav.popBackStack() }
                        )
                    }
                    composable("combustivel_menu/{placa}/{veiculoId}") { b ->
                        val placa: String = b.str("placa")
                        val id: Long = b.long("veiculoId", 0L)
                        CombustivelArlaMenuScreen(
                            placa, "",
                            onCombustivel = { nav.navigate("abastecimento_hist/COMBUSTIVEL/$placa/$id") },
                            onArla = { nav.navigate("abastecimento_hist/ARLA/$placa/$id") },
                            onBack = { nav.popBackStack() }
                        )
                    }
                    composable("abastecimento_hist/{tipo}/{placa}/{veiculoId}") { b ->
                        val tipo = if (b.str("tipo") == "ARLA") TipoAbastecimento.ARLA else TipoAbastecimento.COMBUSTIVEL
                        AbastecimentoHistoricoScreen(
                            tipo, b.str("placa"), b.long("veiculoId", 0L),
                            onNovo = { nav.navigate("abastecimento_form/${tipo.name}/${b.str("placa")}/${b.long("veiculoId", 0L)}/-1") },
                            onEditar = { r: Long -> nav.navigate("abastecimento_form/${tipo.name}/${b.str("placa")}/${b.long("veiculoId", 0L)}/$r") },
                            onBack = { nav.popBackStack() }
                        )
                    }
                    composable("abastecimento_form/{tipo}/{placa}/{veiculoId}/{registroId}") { b ->
                        val tipo = if (b.str("tipo") == "ARLA") TipoAbastecimento.ARLA else TipoAbastecimento.COMBUSTIVEL
                        AbastecimentoFormScreen(
                            tipo, b.str("placa"), b.long("veiculoId", 0L), b.long("registroId", -1L)
                        ) { nav.popBackStack() }
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
