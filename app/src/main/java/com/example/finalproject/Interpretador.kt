package com.example.finalproject

import androidx.compose.ui.graphics.Color
import java.util.Locale

class Interpretador {

    private var linesCodigo: List<String> = emptyList()
    private val rotulos = mutableMapOf<String, Int>()
    private val variaveis = mutableMapOf<String, Int>()
    private var contadorPassos = 0
    var estado = EstadoInterpretador()
        private set

    private fun mapearCor(corTexto: String): Color {
        return when (corTexto.lowercase(Locale.ROOT).replace("\"", "").trim()) {
            "red" -> Color.Red
            "blue" -> Color.Blue
            "green" -> Color.Green
            "yellow" -> Color.Yellow
            "orange" -> Color(0xFFFFA500)
            "purple" -> Color.Magenta
            "black" -> Color.Black
            "white" -> Color.White
            else -> Color.Transparent
        }

    }
    private fun eNumero(valor: String): Boolean {
        return valor.trim().toIntOrNull() != null
    }

    fun compilar(codigo: String, tamanhoTelaInicial: Int): EstadoInterpretador {
        linesCodigo = codigo.lines().map { it.trim() }
        rotulos.clear()
        variaveis.clear()
        val erros = mutableListOf<String>()
        val variaveisDeclaradas = mutableSetOf<String>()
        // antes das outras validações, percorre as atribuições
        linesCodigo.forEach { linha ->
            if (linha.contains("<-")) {
                val nome = linha
                    .substringBefore("<-")
                    .trim()
                variaveisDeclaradas.add(nome)
            }
        }

        if (linesCodigo.isEmpty() ||
            !linesCodigo[0].startsWith("Spawn", true)
        ) {
            erros.add("Erro: código deve começar com Spawn.")
        }

        // LABELS
        linesCodigo.forEachIndexed { i, l ->
            if (l.endsWith(":")) {
                val nomeLabel = l
                    .removeSuffix(":")
                    .trim()
                    .lowercase()
                    .replace("_", "")

                rotulos[nomeLabel] = i
            }
        }
        // Validar labels do Goto
        linesCodigo.forEachIndexed { indice, linha ->

            if (linha.startsWith("Goto", true)) {

                val regex = """(?:Goto|GoTo)\s*\[?\s*(\w+)\s*\]?"""
                    .toRegex()

                val m = regex.find(linha)

                if (m != null) {

                    val label = m.groupValues[1]
                        .lowercase()
                        .replace("_", "")

                    if (!rotulos.containsKey(label)) {

                        erros.add(
                            "Erro linha ${indice + 1}: label '$label' não existe."
                        )
                    }
                }
            }
        }
        // valida comandos
        val comandosValidos = listOf(
            "Spawn",
            "Cor",
            "DrawLine",
            "DrawCircle",
            "DrawRectangle",
            "Goto"
        )
        linesCodigo.forEachIndexed { indice, linha ->
            if (linha.isBlank()) return@forEachIndexed
            if (linha.endsWith(":")) return@forEachIndexed
            if (linha.contains("<-")) return@forEachIndexed
            var comando = linha.substringBefore("(").trim()
            if (comando.startsWith("Goto", true)) {
                comando = "Goto"
            }
            if (!comandosValidos.any { it.equals(comando, true) }) {
                erros.add(
                    "Erro linha ${indice + 1}: comando '$comando' não existe."
                )
            }
        }
        // VALIDAÇÃO DE Parametro
        linesCodigo.forEachIndexed { indice, linha ->

            if (linha.isBlank()) return@forEachIndexed

            if (linha.endsWith(":")) return@forEachIndexed

            if (linha.contains("<-")) return@forEachIndexed

            if (!linha.contains("(") || !linha.contains(")"))
                return@forEachIndexed

            var comando = linha.substringBefore("(").trim()

            if (comando.startsWith("Goto", true)) {
                comando = "Goto"
            }

            val parametros = linha
                .substringAfter("(")
                .substringBeforeLast(")")
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            parametros.forEach { p ->

                val valor = p.replace("\"", "").trim()

                if (
                    !eNumero(valor)
                    && !variaveisDeclaradas.contains(valor)
                    && !valor.startsWith("GetActualX")
                    && !valor.startsWith("GetActualY")
                    && !valor.startsWith("GetScreenSize")
                    && !valor.startsWith("IsBrushColor")
                    && !valor.equals("Red", true)
                    && !valor.equals("Blue", true)
                    && !valor.equals("Green", true)
                    && !valor.equals("Yellow", true)
                    && !valor.equals("Black", true)
                    && !valor.equals("White", true)
                ) {
                    erros.add(
                        "Erro linha ${indice + 1}: parâmetro '$valor' não definido."
                    )
                }
            }

            when {

                comando.equals("Spawn", true) && parametros.size != 2 -> {
                    erros.add(
                        "Erro linha ${indice + 1}: Spawn exige 2 parâmetros."
                    )
                }

                comando.equals("DrawLine", true) && parametros.size != 3 -> {
                    erros.add(
                        "Erro linha ${indice + 1}: DrawLine exige 3 parâmetros."
                    )
                }

                comando.equals("DrawRectangle", true) && parametros.size != 5 -> {
                    erros.add(
                        "Erro linha ${indice + 1}: DrawRectangle exige 5 parâmetros."
                    )
                }

                comando.equals("DrawCircle", true) && parametros.size != 2 -> {
                    erros.add(
                        "Erro linha ${indice + 1}: DrawCircle exige 2 parâmetro."
                    )
                }

                comando.equals("Cor", true) && parametros.size != 1 -> {
                    erros.add(
                        "Erro linha ${indice + 1}: Cor exige 1 parâmetro."
                    )
                }
            }
        }



// VALIDAÇÃO DE SINTAXE
        linesCodigo.forEachIndexed { indice, linha ->
            if (linha.isBlank()) return@forEachIndexed
            if (linha.endsWith(":")) return@forEachIndexed
            if (linha.contains("<-")) return@forEachIndexed
            val temAbre = linha.contains("(")
            val temFecha = linha.contains(")")
            if (temAbre != temFecha) {
                erros.add(
                    "Erro linha ${indice + 1}: parênteses inválidos."
                )
            }
            if (temAbre && temFecha) {
                val posAbre = linha.indexOf("(")
                val posFecha = linha.lastIndexOf(")")
                if (posAbre > posFecha) {
                    erros.add(
                        "Erro linha ${indice + 1}: sintaxe inválida."
                    )
                }
            }
        }

        estado = if (erros.isEmpty()) {
            EstadoInterpretador(
                tamanhoTela = tamanhoTelaInicial,
                compiladoComSucesso = true
            )
        } else {
            EstadoInterpretador(
                tamanhoTela = tamanhoTelaInicial,
                erros = erros,
                compiladoComSucesso = false
            )
        }

        return estado
    }

    fun executarProximaLinha(): EstadoInterpretador {

        if (estado.execucaoTerminada || estado.linhaAtual >= linesCodigo.size) {
            estado = estado.copy(execucaoTerminada = true)
            return estado
        }
        contadorPassos++
        if (contadorPassos > 10000) {
            estado = estado.copy(
                execucaoTerminada = true
            )
            return estado
        }
        val linha = linesCodigo[estado.linhaAtual]
        var next = estado.linhaAtual + 1

        fun eval(expr: String): Int {
            val e = expr.trim()
            if (e == "GetActualX()") return estado.posX
            if (e == "GetActualY()") return estado.posY
            if (e == "GetScreenSize()") return estado.tamanhoTela
            if (e.startsWith("IsBrushColor")) {

                val cor = e
                    .substringAfter("(")
                    .substringBefore(")")
                    .replace("\"", "")

                return if (
                    estado.corNomeAtual.equals(cor, true)
                ) 1 else 0
            }

            if (variaveis.containsKey(e))
                return variaveis[e] ?: 0
            if (e.contains("*")) {
                val p = e.split("*", limit = 2)
                return eval(p[0]) * eval(p[1])
            }

            if (e.contains("/")) {
                val p = e.split("/", limit = 2)
                val div = eval(p[1])
                return if (div != 0) eval(p[0]) / div else 0
            }

            if (e.contains("+")) {
                val p = e.split("+", limit = 2)
                return eval(p[0]) + eval(p[1])
            }

            if (e.contains("-")) {
                val p = e.split("-", limit = 2)
                return eval(p[0]) - eval(p[1])
            }

            return e.toIntOrNull() ?: 0
        }

        when {

            linha.endsWith(":") -> {
                // é apenas um label
            }

            linha.startsWith("Spawn", true) -> {
                val v = linha.substringAfter("(").substringBefore(")").split(",")

                estado = estado.copy(
                    posX = eval(v.getOrNull(0) ?: "0"),
                    posY = eval(v.getOrNull(1) ?: "0")
                )
            }

            linha.startsWith("Cor", true) -> {
                val c = linha.substringAfter("(").substringBefore(")")
                    .replace("\"", "")

                estado = estado.copy(
                    corAtual = mapearCor(c),
                    corNomeAtual = c
                )
            }

            "<-" in linha -> {
                val parts = linha.split("<-")
                val name = parts[0].trim()
                val value = eval(parts[1])

                variaveis[name] = value



                estado = estado.copy(
                    variaveis = variaveis.toMap()
                )
            }
            linha.startsWith("DrawLine", true) -> {
                val v = linha.substringAfter("(").substringBefore(")").split(",")
                val dx = eval(v.getOrNull(0) ?: "0")
                val dy = eval(v.getOrNull(1) ?: "0")
                val dist = eval(v.getOrNull(2) ?: "0")

                val pixels = mutableListOf<PixelPintado>()
                var x = estado.posX
                var y = estado.posY

                // 1. Pegamos a espessura atual do estado do pincel
                val espessura = estado.larguraPincel

                // Adiciona o ponto inicial respeitando a espessura
                for (w in 0 until espessura) {
                    pixels.add(
                        PixelPintado(
                            x,
                            y + w,
                            estado.corAtual
                        )
                    )
                }

                // 2. Loop principal que move o cursor e desenha a linha grossa
                repeat(dist) {
                    x += dx
                    y += dy

                    // Aplica a espessura adicionando pixels na vertical (y + w)
                    for (w in 0 until espessura) {
                        pixels.add(
                            PixelPintado(
                                x,
                                y + w,
                                estado.corAtual
                            )
                        )
                    }
                }

                estado = estado.copy(
                    posX = x,
                    posY = y,
                    pixelsGrelha = estado.pixelsGrelha + pixels
                )
            }
            linha.startsWith("DrawCircle", true) -> {
                val v = linha.substringAfter("(").substringBefore(")").split(",")
                val r = eval(v.getOrNull(0) ?: "0")
                val cheio = eval(v.getOrNull(1) ?: "1")

                val pixels = mutableListOf<PixelPintado>()

                // 1. Pegamos a espessura atual do estado do pincel
                val espessura = estado.larguraPincel

                for (y in -r..r) {
                    for (x in -r..r) {
                        val dist2 = x * x + y * y

                        if (cheio == 1) {
                            if (dist2 <= r * r) {
                                // 2. Aplica o pincel multiplicando o pixel verticalmente (y + t)
                                for (t in 0 until espessura) {
                                    pixels.add(
                                        PixelPintado(
                                            estado.posX + x,
                                            estado.posY + y + t,
                                            estado.corAtual
                                        )
                                    )
                                }
                            }
                        } else {
                            if (dist2 >= r * r - r && dist2 <= r * r + r) {
                                // 3. Aplica o pincel também na borda do círculo vazado
                                for (t in 0 until espessura) {
                                    pixels.add(
                                        PixelPintado(
                                            estado.posX + x,
                                            estado.posY + y + t,
                                            estado.corAtual
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                estado = estado.copy(
                    pixelsGrelha = estado.pixelsGrelha + pixels
                )
            }

            linha.startsWith("DrawRectangle", true) -> {
                val v = linha.substringAfter("(").substringBefore(")").split(",")

                val dirX = eval(v.getOrNull(0) ?: "0")
                val dirY = eval(v.getOrNull(1) ?: "0")
                val largura = eval(v.getOrNull(2) ?: "1")
                val altura = eval(v.getOrNull(3) ?: "1")
                val cheio = eval(v.getOrNull(4) ?: "1")

                val centroX = estado.posX + dirX
                val centroY = estado.posY + dirY

                val pixels = mutableListOf<PixelPintado>()

                val inicioX = centroX - (largura / 2)
                val inicioY = centroY - (altura / 2)

                // 1. Captura a espessura atual do pincel vinda do estado
                val espessura = estado.larguraPincel

                for (y in 0 until altura) {
                    for (x in 0 until largura) {

                        val px = inicioX + x
                        val py = inicioY + y

                        if (
                            cheio == 1 ||
                            x == 0 ||
                            x == largura - 1 ||
                            y == 0 ||
                            y == altura - 1
                        ) {
                            // 2. Aplica o loop do pincel adicionando pixels na vertical (py + t)
                            for (t in 0 until espessura) {
                                pixels.add(
                                    PixelPintado(
                                        px,
                                        py + t,
                                        estado.corAtual
                                    )
                                )
                            }
                        }
                    }
                }

                // 3. Atualiza os pixels da grelha E move o cursor para o centro do retângulo
                estado = estado.copy(
                    posX = centroX,
                    posY = centroY,
                    pixelsGrelha = estado.pixelsGrelha + pixels
                )
            }

            linha.startsWith("Goto", true) -> {

                val regex = """(?:Goto|GoTo)\s*\[?\s*(\w+)\s*\]?\s*\((.*)\)"""
                    .toRegex()

                val m = regex.find(linha)

                if (m != null) {

                    val label = m.groupValues[1]
                        .lowercase()
                        .replace("_", "")

                    val cond = m.groupValues[2]

                    fun evalBool(c: String): Boolean {
                        val s = c.replace(" ", "")
                        if (s == "1==1") return true
                        if (s == "true") return true

                        val ops = listOf(">=", "<=", "==", ">", "<")

                        for (op in ops) {
                            if (s.contains(op)) {
                                val p = s.split(op)
                                val a = eval(p[0])
                                val b = eval(p[1])

                                return when (op) {
                                    "==" -> a == b
                                    "<" -> a < b
                                    ">" -> a > b
                                    "<=" -> a <= b
                                    ">=" -> a >= b
                                    else -> false
                                }
                            }
                        }

                        return eval(c) == 1
                    }

                    if (evalBool(cond) && rotulos.containsKey(label)) {
                        next = rotulos[label]!!
                    }
                }
            }
        }

        estado = estado.copy(linhaAtual = next)

        if (estado.linhaAtual >= linesCodigo.size) {
            estado = estado.copy(execucaoTerminada = true)
        }

        return estado
    }

    fun alterarPropriedadesPincel(largura: Int, tipo: String) {
        estado = estado.copy(larguraPincel = largura, tipoPincel = tipo)
    }

    fun atualizarCorEmTempoReal(novaCor: Color, nomeCor: String) {
        estado = estado.copy(corAtual = novaCor, corNomeAtual = nomeCor)
    }
}