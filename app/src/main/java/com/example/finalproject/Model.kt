package com.example.finalproject

import androidx.compose.ui.graphics.Color

data class PixelPintado(
    val x: Int,
    val y: Int,
    val cor: Color
)

data class EstadoInterpretador(
    val tamanhoTela: Int = 12,
    val posX: Int = 0,
    val posY: Int = 0,
    val corAtual: Color = Color.Black,
    val corNomeAtual: String = "Black",
    val larguraPincel: Int = 1,
    val tipoPincel: String = "Circular",
    val pixelsGrelha: List<PixelPintado> = emptyList(),
    val variaveis: Map<String, Int> = emptyMap(),
    val erros: List<String> = emptyList(),
    val linhaAtual: Int = 0,
    val compiladoComSucesso: Boolean = false,
    val execucaoTerminada: Boolean = false
)