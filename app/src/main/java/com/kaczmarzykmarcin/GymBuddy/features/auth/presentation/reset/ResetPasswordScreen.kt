package com.kaczmarzykmarcin.GymBuddy.features.auth.presentation.reset

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.kaczmarzykmarcin.GymBuddy.R
import com.kaczmarzykmarcin.GymBuddy.features.auth.presentation.AuthState
import com.kaczmarzykmarcin.GymBuddy.features.auth.presentation.AuthViewModel
import com.kaczmarzykmarcin.GymBuddy.features.auth.presentation.components.AuthButton
import com.kaczmarzykmarcin.GymBuddy.features.auth.presentation.components.LoadingIndicator

@Composable
fun PasswordResetScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }

    // Observe validation errors
    val emailError by viewModel.emailError.collectAsState()

    // Observe authentication state
    val authState by viewModel.authState.collectAsState()

    // Track reset message
    var resetMessage by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    // Clear validation errors when user types
    LaunchedEffect(email) {
        if (emailError != null) {
            viewModel.resetValidationErrors()
        }
    }

    // Update message when auth state changes
    LaunchedEffect(authState) {
        Log.d("PasswordResetScreen", "Auth state changed: $authState")
        resetMessage = when (authState) {
            is AuthState.PasswordResetSent -> {
                Pair("Link do resetowania hasła został wysłany na adres $email", true)
            }
            is AuthState.Error -> {
                Pair((authState as AuthState.Error).message, false)
            }
            else -> null
        }
    }

    // Clear errors on screen leave
    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetValidationErrors()
            viewModel.resetAuthStateToNotAuthenticated()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 32.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Back button and title
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.Start)
                .padding(top = 8.dp, bottom = 16.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Powrót",
                tint = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Logo aplikacji
        Image(
            painter = painterResource(id = R.drawable.ic_logo_barbell),
            contentDescription = stringResource(R.string.app_logo),
            modifier = Modifier.size(180.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Title
        Text(
            text = "Resetowanie hasła",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Description
        Text(
            text = "Wprowadź swój adres email, a wyślemy Ci link do zresetowania hasła.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Email field
        Text(
            text = stringResource(R.string.email),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            color = Color.Black
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text(stringResource(R.string.email_hint)) },
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = emailError != null,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedIndicatorColor = if (emailError != null) Color.Red else Color.LightGray,
                unfocusedIndicatorColor = if (emailError != null) Color.Red else Color.LightGray,
                errorContainerColor = Color.White,
                errorIndicatorColor = Color.Red
            )
        )

        // Email error message
        if (emailError != null) {
            Text(
                text = emailError!!,
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, top = 4.dp)
            )
        }

        // Reset response message
        resetMessage?.let { (message, isSuccess) ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                color = if (isSuccess) Color(0xFF4CAF50) else Color.Red,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Reset password button
        if (authState is AuthState.Loading) {
            LoadingIndicator()
        } else {
            AuthButton(
                text = "Wyślij link resetujący",
                onClick = { viewModel.resetPassword(email) },
                filled = true,
                backgroundColor = Color.Black,
                contentColor = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Back to login button
        if (authState !is AuthState.Loading) {
            AuthButton(
                text = "Powrót do logowania",
                onClick = {
                    viewModel.resetValidationErrors()
                    viewModel.resetAuthStateToNotAuthenticated()
                    navController.popBackStack()
                }
            )
        }
    }
}