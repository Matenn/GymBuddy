package com.kaczmarzykmarcin.GymBuddy.features.auth.presentation

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kaczmarzykmarcin.GymBuddy.data.repository.UserRepository
import com.kaczmarzykmarcin.GymBuddy.features.auth.presentation.login.LoginScreen
import com.kaczmarzykmarcin.GymBuddy.features.auth.presentation.register.RegisterScreen
import com.kaczmarzykmarcin.GymBuddy.features.auth.presentation.welcome.WelcomeScreen
import com.kaczmarzykmarcin.GymBuddy.ui.theme.GymBuddyTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.viewModels
import com.kaczmarzykmarcin.GymBuddy.navigation.AppNavigation
import com.facebook.CallbackManager
import javax.inject.Inject

@AndroidEntryPoint
class AuthActivity : ComponentActivity() {

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GymBuddyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AppNavigation(navController) // Używamy AppNavigation zamiast własnego NavHost
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Add logging for debugging
        Log.d("AuthActivity", "onActivityResult: requestCode=$requestCode, resultCode=$resultCode")

        if (requestCode == AuthViewModel.GOOGLE_SIGN_IN_REQUEST_CODE) {
            Log.d("AuthActivity", "Handling Google Sign-In result")
            viewModel.handleGoogleSignInResult(data)
        } else {
            // Only pass to Facebook handler if it's a Facebook related request
            if (requestCode == CallbackManager.Factory.create().hashCode()) {
                Log.d("AuthActivity", "Handling Facebook Sign-In result")
                viewModel.handleFacebookSignInResult(requestCode, resultCode, data)
            }
        }
    }

}


