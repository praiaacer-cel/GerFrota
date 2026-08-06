package com.gerfrota.lite.ui.relatorios

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val MESES = listOf("Janeiro","Fevereiro","Março","Abril","Maio","Junho",
    "Julho","Agosto","Setembro","Outubro","Novembro","Dezembro")

@Composable
fun CardDestaque(titulo: String, valor: String, cor: Color, icone: ImageVector) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = cor.copy(alpha = .12f)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Icon(icone, null, tint = cor, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Text(titulo, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = cor)
            }
            Spacer(Modifier.height(16.dp))
            Text(valor, fontSize = 36.sp, fontWeight = FontWeight.Black, color = cor)
        }
    }
}

@Composable
fun CardSecundario(titulo: String, valor: String, cor: Color) {
    Card(Modifier.weight(1f), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(titulo, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF757575),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(valor, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = cor)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeletorMesAno(mes: Int, ano: Int, onMes: (Int) -> Unit, onAno: (Int) -> Unit) {
    var expMes by remember { mutableStateOf(false) }
    var expAno by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ExposedDropdownMenuBox(expanded = expMes, onExpandedChange = { expMes = it }, Modifier.weight(1f)) {
            OutlinedTextField(MESES[mes - 1], {}, readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expMes) })
            ExposedDropdownMenu(expMes, { expMes = false }) {
                MESES.forEachIndexed { i, m -> DropdownMenuItem({ Text(m) }, { onMes(i + 1) }) }
            }
        }
        ExposedDropdownMenuBox(expanded = expAno, onExpandedChange = { expAno = it }, Modifier.weight(1f)) {
            OutlinedTextField(ano.toString(), {}, readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expAno) })
            ExposedDropdownMenu(expAno, { expAno = false }) {
                (2023..2030).forEach { a -> DropdownMenuItem({ Text(a.toString()) }, { onAno(a) }) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSimples(label: String, valor: String?, opcoes: List<String>, onSel: (String) -> Unit) {
    var exp by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = exp, onExpandedChange = { exp = it }, Modifier.fillMaxWidth()) {
        OutlinedTextField(valor ?: "", {}, readOnly = true, label = { Text(label) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(exp) })
        ExposedDropdownMenu(exp, { exp = false }) {
            opcoes.forEach { o -> DropdownMenuItem({ Text(o) }, { onSel(o) }) }
        }
    }
}
