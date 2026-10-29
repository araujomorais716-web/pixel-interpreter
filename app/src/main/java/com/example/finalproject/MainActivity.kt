package com.example.finalproject

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class MainActivity : ComponentActivity() {
    private val interpretador = Interpretador()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var telaAtual by remember { mutableStateOf("editor") }
            var codigoTexto by remember { mutableStateOf("") }
            var tamanhoTelaInput by remember { mutableStateOf("12") }
            var estadoApp by remember { mutableStateOf(EstadoInterpretador()) }

            val context = LocalContext.current

            val filePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument(),
                onResult = { uri: Uri? ->
                    uri?.let {
                        try {
                            context.contentResolver.openInputStream(it)?.use { inputStream ->
                                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                                    codigoTexto = reader.readText()
                                }
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }
            )

            val fileSaverLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("text/plain"),
                onResult = { uri: Uri? ->
                    uri?.let {
                        try {
                            context.contentResolver.openOutputStream(it)?.use { outputStream ->
                                BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                                    writer.write(codigoTexto)
                                }
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }
            )

            if (telaAtual == "editor") {
                ScreenEditor(
                    codigo = codigoTexto,
                    onCodigoChange = { codigoTexto = it },
                    tamanhoTela = tamanhoTelaInput,
                    onTamanhoTelaChange = { tamanhoTelaInput = it },
                    estado = estadoApp,
                    onAbrirFicheiro = { filePickerLauncher.launch(arrayOf("*/*")) },
                    onGravarFicheiro = { fileSaverLauncher.launch("meu_codigo.pw") },
                    onCompilar = {
                        val tam = tamanhoTelaInput.toIntOrNull() ?: 12
                        val novoEstado = interpretador.compilar(codigoTexto, tam)
                        estadoApp = novoEstado
                        if (novoEstado.compiladoComSucesso) {
                            telaAtual = "execucao"
                        }
                    }
                )
            } else {
                ScreenExecucao(
                    estado = estadoApp,
                    onPassoAPasso = {
                        estadoApp = interpretador.executarProximaLinha()
                    },
                    onModificarPincel = { larg, tipo ->
                        interpretador.alterarPropriedadesPincel(larg, tipo)
                        estadoApp = interpretador.estado
                    },
                    onVoltar = { telaAtual = "editor" }
                )
            }
            }
        }
    }


@Composable
fun ScreenEditor(
    codigo: String,
    onCodigoChange: (String) -> Unit,
    tamanhoTela: String,
    onTamanhoTelaChange: (String) -> Unit,
    estado: EstadoInterpretador,
    onAbrirFicheiro: () -> Unit,
    onGravarFicheiro: () -> Unit,
    onCompilar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("💻 IDE Pixel Compiler Real-Grid", fontSize = 22.sp, style = MaterialTheme.typography.titleLarge)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onAbrirFicheiro, modifier = Modifier.weight(1f)) { Text("📁 Abrir .pw") }
            Button(onClick = onGravarFicheiro, modifier = Modifier.weight(1f)) { Text("💾 Gravar") }
        }

        OutlinedTextField(
            value = tamanhoTela,
            onValueChange = onTamanhoTelaChange,
            label = { Text("Dimensão do Quadro Grelha (Ex: 12)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = codigo,
            onValueChange = onCodigoChange,
            label = { Text("Código .pw") },
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
        )

        if (estado.erros.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFCDD2)), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("❌ Erros:", color = Color.Red, fontSize = 16.sp)
                    estado.erros.forEach { Text("• $it", color = Color.Black, fontSize = 14.sp) }
                }
            }
        }

        Button(onClick = onCompilar, modifier = Modifier.fillMaxWidth()) {
            Text("⚙️ Compilar e Iniciar AT")
        }
    }
}

@Composable
fun ScreenExecucao(
    estado: EstadoInterpretador,
    onPassoAPasso: () -> Unit,
    onModificarPincel: (Int, String) -> Unit,

    onVoltar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = onVoltar) { Text("Voltar") }
            Button(onClick = onPassoAPasso, enabled = !estado.execucaoTerminada) {
                Text(if (estado.execucaoTerminada) "Execução Terminada" else "Próximo Passo ➡️")
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Cor Atual: ", fontSize = 14.sp)
                    Box(modifier = Modifier.size(24.dp).background(estado.corAtual, CircleShape).border(1.dp, Color.Black, CircleShape))
                    Text(estado.corNomeAtual, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                }



                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text("Definir Grossura do Pincel:", fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.size(45.dp).background(if(estado.larguraPincel == 1) Color.LightGray else Color.White, CircleShape).border(1.dp, Color.Black, CircleShape).clickable { onModificarPincel(1, "Circular") }, contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.size(4.dp).background(Color.Black, CircleShape))
                    }
                    Box(modifier = Modifier.size(45.dp).background(if(estado.larguraPincel == 2) Color.LightGray else Color.White, CircleShape).border(1.dp, Color.Black, CircleShape).clickable { onModificarPincel(2, "Circular") }, contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.size(10.dp).background(Color.Black, CircleShape))
                    }
                    Box(modifier = Modifier.size(45.dp).background(if(estado.larguraPincel == 3) Color.LightGray else Color.White).border(1.dp, Color.Black).clickable { onModificarPincel(3, "Quadrado") }, contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.size(18.dp).background(Color.Black))
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text("Linha Atual: ${estado.linhaAtual} | Cursor: [${estado.posX}, ${estado.posY}]", fontSize = 14.sp)
                val varTexto = if (estado.variaveis.isEmpty()) "Nenhuma" else estado.variaveis.entries.joinToString(", ") { "${it.key}: ${it.value}" }
                Text("Variáveis: $varTexto", fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF004D40))
            }
        }

        Box(modifier = Modifier.size(312.dp).background(Color.White).border(1.dp, Color.LightGray)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val colunas = estado.tamanhoTela
                val pixelDimon = size.width / colunas

                for (i in 0..colunas) {
                    val pos = i * pixelDimon
                    drawLine(Color(0xFFE0E0E0), Offset(pos, 0f), Offset(pos, size.height), strokeWidth = 1f)
                    drawLine(Color(0xFFE0E0E0), Offset(0f, pos), Offset(size.width, pos), strokeWidth = 1f)
                }

                estado.pixelsGrelha.forEach { pixel ->
                    if (pixel.x in 0 until colunas && pixel.y in 0 until colunas) {
                        drawRect(
                            color = pixel.cor,
                            topLeft = Offset(pixel.x * pixelDimon, pixel.y * pixelDimon),
                            size = Size(pixelDimon, pixelDimon)
                        )
                    }
                }

                if (estado.posX in 0 until colunas && estado.posY in 0 until colunas) {
                    val cursorX = estado.posX * pixelDimon + (pixelDimon / 2)
                    val cursorY = estado.posY * pixelDimon + (pixelDimon / 2)
                    drawCircle(
                        color = Color.Blue,
                        radius = pixelDimon * 0.3f,
                        center = Offset(cursorX, cursorY)
                    )
                }
            }
        }
    }
}