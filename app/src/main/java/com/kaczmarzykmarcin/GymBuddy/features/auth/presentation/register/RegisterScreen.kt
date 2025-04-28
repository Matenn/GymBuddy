package com.kaczmarzykmarcin.GymBuddy.features.auth.presentation.register

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
fun RegisterScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Observe authentication state
    val authState by viewModel.authState.collectAsState()

    // Observe validation errors
    val emailError by viewModel.emailError.collectAsState()
    val passwordError by viewModel.passwordError.collectAsState()

    // Track registration error message
    var registrationErrorMessage by remember { mutableStateOf<String?>(null) }

    // Clear registration error when user changes either email or password
    LaunchedEffect(email, password) {
        if (registrationErrorMessage != null) {
            registrationErrorMessage = null
            viewModel.resetValidationErrors()
        }
    }

    // Update registration error message when auth state changes to Error
    LaunchedEffect(authState) {
        Log.d("RegisterScreen", "Auth state changed: $authState")
        registrationErrorMessage = if (authState is AuthState.Error) {
            (authState as AuthState.Error).message
        } else {
            null
        }
    }
    // Clear errors on screen leave (handles both button navigation and gesture navigation)
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

        Spacer(modifier = Modifier.height(100.dp))
        // Logo aplikacji
        Image(
            painter = painterResource(id = R.drawable.ic_logo_barbell),
            contentDescription = stringResource(R.string.app_logo),
            modifier = Modifier.size(240.dp)
        )

        Spacer(modifier = Modifier.weight(2f))

        // Pole Email
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
            isError = emailError != null || (registrationErrorMessage != null && registrationErrorMessage!!.contains("email")),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedIndicatorColor = if (emailError != null || (registrationErrorMessage != null && registrationErrorMessage!!.contains("email"))) Color.Red else Color.LightGray,
                unfocusedIndicatorColor = if (emailError != null || (registrationErrorMessage != null && registrationErrorMessage!!.contains("email"))) Color.Red else Color.LightGray,
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

        Spacer(modifier = Modifier.height(24.dp))

        // Pole Hasło
        Text(
            text = stringResource(R.string.password),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            color = Color.Black
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text(stringResource(R.string.password_hint)) },
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = PasswordVisualTransformation(),
            isError = passwordError != null || (registrationErrorMessage != null && registrationErrorMessage!!.contains("hasło")),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedIndicatorColor = if (passwordError != null || (registrationErrorMessage != null && registrationErrorMessage!!.contains("hasło"))) Color.Red else Color.LightGray,
                unfocusedIndicatorColor = if (passwordError != null || (registrationErrorMessage != null && registrationErrorMessage!!.contains("hasło"))) Color.Red else Color.LightGray,
                errorContainerColor = Color.White,
                errorIndicatorColor = Color.Red
            )
        )

        // Password error message
        if (passwordError != null) {
            Text(
                text = passwordError!!,
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, top = 4.dp)
            )
        }

        // General registration error message
        if (registrationErrorMessage != null && emailError == null && passwordError == null) {
            Text(
                text = registrationErrorMessage!!,
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Przycisk Zarejestruj się - dodaj indykator ładowania
        if (authState is AuthState.Loading) {
            LoadingIndicator()
        } else {
            AuthButton(
                text = stringResource(R.string.register),
                onClick = { viewModel.signUp(email, password) },
                filled = true
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sekcja "lub"
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                thickness = 1.dp,
                color = Color.LightGray
            )
            Text(
                text = stringResource(R.string.or),
                modifier = Modifier.padding(horizontal = 16.dp),
                color = Color.Gray
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                thickness = 1.dp,
                color = Color.LightGray
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tekst "Posiadasz już konto?"
        Text(
            text = stringResource(R.string.have_account),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Przycisk Zaloguj się
        AuthButton(
            text = stringResource(R.string.login),
            onClick = { navController.navigate("welcome") }
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}