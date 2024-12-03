package com.example.android_cnn_cv

import android.content.Intent
import android.os.Bundle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.android_cnn_cv.ui.theme.Android_CNN_CVTheme
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import android.content.Context
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.ValueFormatter
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import java.util.Locale

class SecondActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val csvData = readCsvFile(applicationContext)
        val dailyGroupedData = filterDataForTodayAndGroup(csvData)
        val weeklyGroupedData = filterDataForLastSevenDaysAndGroup(csvData)
        val monthlyGroupedData = filterDataForCurentMonthAndGroup(csvData)
        val dailyBarEntries = convertIntGroupedDataToBarEntries(dailyGroupedData)
        val weeklyBarEntries = convertStringGroupedDataToBarEntries(weeklyGroupedData)
        val monthlyBarEntries = convertIntGroupedDataToBarEntries(monthlyGroupedData)
        val xLabels = extractXLabelsFromGroupedData(weeklyGroupedData)

        setContent {
            Android_CNN_CVTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SecondScreen(dailyBarEntries, weeklyBarEntries, xLabels, monthlyBarEntries) { navigateToMainActivity() }
                }
            }
        }

        // Aquí puedes verificar los datos agrupados
        println(dailyGroupedData)  // Solo para verificar los datos
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}

fun readCsvFile(context: Context): List<String> {
    val inputStream = context.openFileInput("log.csv")
    val reader = BufferedReader(InputStreamReader(inputStream))
    val lines = mutableListOf<String>()

    reader.forEachLine {
        lines.add(it)
    }

    // Imprimir las líneas para verificar
    println("Contenido por línea del archivo CSV:")
    lines.forEach { line ->
        println(line) // Imprime cada línea del archivo
    }

    return lines
}

fun getCurrentMonth(): String {
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("MMMM", Locale("es", "ES")) // "MMMM" da el nombre completo del mes
    return dateFormat.format(calendar.time).lowercase(Locale("es", "ES"))
}

fun filterDataForTodayAndGroup(csvData: List<String>): Map<Int, Int> {
    // Obtener la fecha de hoy en el formato "d,MM,yyyy" (día, mes, año)
    val today = Calendar.getInstance()
    println("Check 1:")
    println(today)
    val dayOfMonth = today.get(Calendar.DAY_OF_MONTH)
    println("Check 2:")
    println(dayOfMonth)
    val month = getCurrentMonth()
    println("Check 3:")
    println(month)

    val groupedCalories = mutableMapOf(0 to 0, 1 to 0, 2 to 0, 3 to 0, 4 to 0, 5 to 0)

    // Iterar sobre cada línea del CSV
    csvData.forEach { line ->
        // Parsear los datos del CSV
        val parts = line.split(",")
        if (parts.size >= 6) {
            val day = parts[1].toIntOrNull() ?: 0
            val monthName = parts[2].lowercase(Locale.getDefault())
            val hour = parts[3].toIntOrNull() ?: 0
            val calories = parts[5].toIntOrNull() ?: 0

            // Verificar si el registro es del día de hoy
            if (day == dayOfMonth && monthName == month) {
                // Agrupar por intervalo de horas
                when {
                    hour in 0..4 -> groupedCalories[0] = groupedCalories[0]?.plus(calories) ?: calories
                    hour in 5..8 -> groupedCalories[1] = groupedCalories[1]?.plus(calories) ?: calories
                    hour in 9..12 -> groupedCalories[2] = groupedCalories[2]?.plus(calories) ?: calories
                    hour in 13..16 -> groupedCalories[3] = groupedCalories[3]?.plus(calories) ?: calories
                    hour in 17..20 -> groupedCalories[4] = groupedCalories[4]?.plus(calories) ?: calories
                    hour in 21..23 -> groupedCalories[5] = groupedCalories[5]?.plus(calories) ?: calories
                }
            }
        }
    }
    return groupedCalories
}

fun filterDataForLastSevenDaysAndGroup(csvData: List<String>): Map<String, Int> {
    // Parsear todas las fechas del CSV
    val dates = mutableSetOf<String>()
    csvData.forEach { line ->
        val parts = line.split(",")
        if (parts.size >= 7) {
            val date = parts[6]
            dates.add(date)
        }
    }

    // Ordenar las fechas en orden ascendente (más antiguas primero) y tomar las últimas 7
    val sortedDates = dates.sorted().takeLast(7)

    // Agrupar las calorías por fecha
    val groupedCalories = mutableMapOf<String, Int>()
    sortedDates.forEach { date ->
        groupedCalories[date] = 0 // Inicializamos el valor con 0
    }

    // Iterar sobre cada línea del CSV y agrupar las calorías por fecha
    csvData.forEach { line ->
        val parts = line.split(",")
        if (parts.size >= 7) {
            val date = parts[6]
            val calories = parts[5].toIntOrNull() ?: 0

            // Solo agregamos las calorías si la fecha está dentro de los últimos 7 días
            if (sortedDates.contains(date)) {
                groupedCalories[date] = groupedCalories[date]?.plus(calories) ?: calories
            }
        }
    }

    return groupedCalories
}

fun filterDataForCurentMonthAndGroup(csvData: List<String>): Map<Int, Int> {
    // Obtener la fecha de hoy en el formato "d,MM,yyyy" (día, mes, año)
    val month = getCurrentMonth()
    val groupedCalories = mutableMapOf(0 to 0, 1 to 0, 2 to 0, 3 to 0)

    // Iterar sobre cada línea del CSV
    csvData.forEach { line ->
        // Parsear los datos del CSV
        val parts = line.split(",")
        if (parts.size >= 6) {
            val day = parts[1].toIntOrNull() ?: 0
            val monthName = parts[2].lowercase(Locale.getDefault())
            val calories = parts[5].toIntOrNull() ?: 0

            // Verificar si el registro es del día de hoy
            if (monthName == month) {
                when {
                    day in 0..7 -> groupedCalories[0] = groupedCalories[0]?.plus(calories) ?: calories
                    day in 8..15 -> groupedCalories[1] = groupedCalories[1]?.plus(calories) ?: calories
                    day in 16..23 -> groupedCalories[2] = groupedCalories[2]?.plus(calories) ?: calories
                    day in 24..31 -> groupedCalories[3] = groupedCalories[3]?.plus(calories) ?: calories
                }
            }
        }
    }
    return groupedCalories
}

fun convertIntGroupedDataToBarEntries(groupedData: Map<Int, Int>): List<BarEntry> {
    return groupedData.map { (range, calories) ->
        BarEntry(range.toFloat(), calories.toFloat()) // Crear BarEntry para cada intervalo
    }
}

fun convertStringGroupedDataToBarEntries(groupedData: Map<String, Int>): List<BarEntry> {
    return groupedData.entries.mapIndexed { index, (date, calories) ->
        BarEntry(index.toFloat(), calories.toFloat()) // El índice es la posición en el eje X
    }
}

fun extractXLabelsFromGroupedData(groupedData: Map<String, Int>): List<String> {
    val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) // Formato original de las fechas
    val outputFormat = SimpleDateFormat("dd-MMM", Locale.getDefault()) // Formato deseado

    return groupedData.keys.map { date ->
        try {
            // Parsear la fecha desde el formato original
            val parsedDate = inputFormat.parse(date)
            // Formatear la fecha al nuevo formato
            outputFormat.format(parsedDate ?: date)
        } catch (e: Exception) {
            // Si hay un error, devolver la fecha original
            date
        }
    }
}

@Composable
fun SecondScreen(dailyBarEntries: List<BarEntry>, weeklyBarEntries: List<BarEntry>, xLabels: List<String>, monthlyBarEntries: List<BarEntry> ,onBackClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Botón de retroceso
        Button(onClick = onBackClick) {
            Icon(
                painter = painterResource(id = R.drawable.ic_back),
                contentDescription = "Regresar"
            )
            Text(" Capturar comida")
        }

        Text("")
        Text("Calorías de hoy")

        // Gráfico de barras por dia
        BarChartView(
            entries = dailyBarEntries,
            label = "Calorías",
            barColor = Color.BLUE,
            xLabels = listOf("4h", "8h", "12h", "16h", "20h", "24h"),
            modifier = Modifier.fillMaxWidth().height(200.dp)
        )

        Text("")
        Text("Calorías de tus últimos 7 días")
        BarChartView(
            entries = weeklyBarEntries,
            label = "Calorías",
            barColor = Color.GREEN,
            xLabels = xLabels,
            modifier = Modifier.fillMaxWidth().height(200.dp)
        )

        Text("")
        Text("Calorías de este mes")
        BarChartView(
            entries = monthlyBarEntries,
            label = "Calorías",
            barColor = Color.MAGENTA,
            xLabels = listOf("Sem 1", "Sem 2", "Sem 3", "Sem 4"),
            modifier = Modifier.fillMaxWidth().height(200.dp)
        )
    }
}

@Composable
fun BarChartView(
    entries: List<BarEntry>,
    label: String,
    barColor: Int,
    xLabels: List<String>,
    modifier: Modifier = Modifier // Agregar el parámetro de modifier
) {

// Define colores según el valor y las condiciones de xLabels
    val colors = entries.map { entry ->
        when {
            // Condición para la primera gráfica (primer xLabel contiene "h")
            xLabels.firstOrNull()?.contains("h", ignoreCase = true) == true -> {
                when {
                    entry.y < 900 -> Color.parseColor("#28D809")
                    else -> Color.parseColor("#D81F09")
                }
            }

            // Condición para la segunda gráfica (sin "h" y sin "sem")
            xLabels.firstOrNull()?.let { !it.contains("h", ignoreCase = true) && !it.contains("sem", ignoreCase = true) } == true -> {
                when {
                    entry.y < 2400 -> Color.parseColor("#28D809")
                    entry.y < 2800 -> Color.parseColor("#F19D00")
                    else -> Color.parseColor("#D81F09")
                }
            }

            // Condición para la tercera gráfica (primer xLabel contiene "sem")
            xLabels.firstOrNull()?.contains("sem", ignoreCase = true) == true -> {
                when {
                    entry.y < 16800 -> Color.parseColor("#28D809")
                    entry.y < 19600 -> Color.parseColor("#F19D00")
                    else -> Color.parseColor("#D81F09")
                }
            }

            // Condición por defecto
            else -> Color.parseColor("#FF4500") // Naranja oscuro
        }
    }

// Aplica los colores al dataset
    val dataSet = BarDataSet(entries, label).apply {
        setColors(colors)
    }

    // Crear los datos
    val barData = BarData(dataSet)

    // Mostrar el gráfico en Compose
    AndroidView(
        factory = { context ->
            BarChart(context).apply {
                // Asignar los datos al gráfico
                this.data = barData

                // Configurar el eje X con los intervalos personalizados (pasados como parámetro)
                this.xAxis.apply {
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return if (value.toInt() in xLabels.indices) {
                                xLabels[value.toInt()]
                            } else {
                                ""
                            }
                        }
                    }
                    position = XAxis.XAxisPosition.BOTTOM
                    granularity = 1f
                    textColor = Color.BLACK
                    textSize = 12f
                    gridColor = Color.LTGRAY
                    axisLineColor = Color.DKGRAY
                }

                // Configuración del eje Y
                this.axisLeft.apply {
                    setAxisMinValue(0f)
                    textColor = Color.BLACK
                    textSize = 12f
                    gridColor = Color.LTGRAY
                    axisLineColor = Color.DKGRAY
                }
                this.axisRight.isEnabled = false

                // Actualizar la vista
                this.invalidate()

                this.description.isEnabled = false

                this.setBackgroundColor(Color.parseColor("#F5F5F5"))

                this.setDrawBarShadow(true)
                this.setFitBars(true)
                this.legend.isEnabled = false
            }
        },
        modifier = modifier // Aplicar el modifier para controlar el tamaño
    )
}


@Preview(showBackground = true)
@Composable
fun SecondScreenPreview() {
    val exampleBarEntries = listOf(
        BarEntry(0f, 10f),
        BarEntry(1f, 20f),
        BarEntry(2f, 30f)
    )
    val examplexLabels = listOf(
        "Día 1",
        "Día 2",
        "Día 3"
    )
    Android_CNN_CVTheme {
        SecondScreen(dailyBarEntries = exampleBarEntries, weeklyBarEntries = exampleBarEntries, xLabels = examplexLabels, monthlyBarEntries = exampleBarEntries ,onBackClick = {})
    }
}