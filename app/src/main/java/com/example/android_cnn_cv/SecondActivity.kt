package com.example.android_cnn_cv

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.android_cnn_cv.ui.theme.Android_CNN_CVTheme

class SecondActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Android_CNN_CVTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SecondScreen { navigateToMainActivity() }
                }
            }
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}

@Composable
fun SecondScreen(onBackClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = onBackClick) {
            Icon( // Los iconos estan en la carpeta res/drawable por si quieres usarlos para los botones :)
                painter = painterResource(id = R.drawable.ic_back),
                contentDescription = "Back to MainActivity"
            )
            Text(" Back")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SecondScreenPreview() {
    Android_CNN_CVTheme {
        SecondScreen {}
    }
}
