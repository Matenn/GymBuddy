package com.kaczmarzykmarcin.GymBuddy.features.auth.presentation.welcome

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kaczmarzykmarcin.GymBuddy.R
import com.kaczmarzykmarcin.GymBuddy.features.auth.presentation.AuthState
import com.kaczmarzykmarcin.GymBuddy.features.auth.presentation.AuthViewModel
import com.kaczmarzykmarcin.GymBuddy.features.auth.presentation.components.AuthButton
import com.kaczmarzykmarcin.GymBuddy.features.auth.presentation.components.LoadingIndicator
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
private const val TAG = "WelcomeScreen"
@Composable
fun WelcomeScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }


    // Check Google Play Services availability
    LaunchedEffect(Unit) {
        checkGooglePlayServices(context as Activity)
    }

    // Obserwuj stan uwierzytelniania
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                // Przejdź do głównego ekranu aplikacji
                navController.navigate("main") {
                    // Wyczyść stos nawigacji, aby użytkownik nie mógł wrócić do ekranu logowania
                    popUpTo("welcome") { inclusive = true }
                }
            }
            is AuthState.Error -> {
                // Wyświetl komunikat o błędzie
                val message = (authState as AuthState.Error).message
                snackbarHostState.showSnackbar(message)
            }
            else -> {} // Inne stany obsługujemy w interfejsie
        }
    }

    // Inicjalizacja Google Sign In przy pierwszym renderowaniu
    LaunchedEffect(Unit) {
        try {
            // Get web_client_id from strings.xml
            val webClientId = context.getString(R.string.web_client_id)
            Log.d("WelcomeScreen", "Initializing Google Sign In with webClientId: $webClientId")

            // Initialize Google Sign In
            viewModel.initGoogleSignIn(
                activity = context as Activity,
                webClientId = webClientId
            )
        } catch (e: Exception) {
            Log.e("WelcomeScreen", "Failed to initialize Google Sign In", e)
            snackbarHostState.showSnackbar("Failed to initialize Google Sign In: ${e.message}")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 32.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(100.dp))

        // Logo aplikacji
        Image(
            painter = painterResource(id = R.drawable.ic_logo_barbell),
            contentDescription = stringResource(R.string.app_logo),
            modifier = Modifier.size(240.dp)
        )

        Spacer(modifier = Modifier.weight(3f))

        // Wskaźnik ładowania podczas procesu uwierzytelniania
        if (authState is AuthState.Loading) {
            LoadingIndicator()
        } else {
            // Przyciski logowania
            AuthButton(
                text = stringResource(R.string.login_with_google),
                onClick = { viewModel.signInWithGoogle(context as Activity) },
                iconResId = R.drawable.ic_google_logo
            )

            Spacer(modifier = Modifier.height(16.dp))

            AuthButton(
                text = stringResource(R.string.login_with_facebook),
                onClick = { viewModel.signInWithFacebook(context as Activity) },
                iconResId = R.drawable.ic_facebook_logo
            )

            // Zwiększony odstęp między przyciskiem Facebooka a rejestracją email
            Spacer(modifier = Modifier.height(16.dp))

            AuthButton(
                text = stringResource(R.string.register_with_email),
                onClick = { navController.navigate("register") },
                iconResId = R.drawable.ic_mail
            )

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

            // Przycisk logowania
            AuthButton(
                text = (stringResource(R.string.login_with_email)),
                onClick = { navController.navigate("login") }
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}
private fun checkGooglePlayServices(activity: Activity) {
    val googleApiAvailability = GoogleApiAvailability.getInstance()
    val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(activity)

    if (resultCode != ConnectionResult.SUCCESS) {
        if (googleApiAvailability.isUserResolvableError(resultCode)) {
            Log.w(TAG, "Google Play Services issue detected: $resultCode")
            val dialog = googleApiAvailability.getErrorDialog(activity, resultCode, 1000)
            dialog?.show()
        } else {
            Log.e(TAG, "This device is not supported for Google Play Services")
        }
    } else {
        Log.d(TAG, "Google Play Services is available and up to date")
    }
}