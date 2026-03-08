package com.solanki.myapplication.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.random.Random

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

val IncomeGreen = Color(0xFF4CAF50)
val ExpenseRed = Color(0xFFF44336)

val BankColors = listOf(
    Color(0xFF1E88E5), // Blue
    Color(0xFFD81B60), // Pink
    Color(0xFF8E24AA), // Purple
    Color(0xFF00897B), // Teal
    Color(0xFFF4511E), // Deep Orange
    Color(0xFF43A047), // Green
    Color(0xFFFFB300)  // Amber
)


fun generateRandomColorHex(): String {
    val red = Random.nextInt(100, 256)
    val green = Random.nextInt(100, 256)
    val blue = Random.nextInt(100, 256)

    return String.format("#%02X%02X%02X", red, green, blue)
}
