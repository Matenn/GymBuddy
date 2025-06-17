package com.kaczmarzykmarcin.GymBuddy.features.auth.presentation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.kaczmarzykmarcin.GymBuddy.features.user.data.repository.UserRepository
import com.kaczmarzykmarcin.GymBuddy.features.achievements.domain.service.AchievementService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth,
    private val achievementService: AchievementService, // DODANY DEPENDENCY
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "auth_prefs", Context.MODE_PRIVATE
    )

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState

    // Dodane: flaga wskazująca, czy inicjalizacja jest zakończona
    private val _initialCheckDone = MutableStateFlow(false)
    val initialCheckDone: StateFlow<Boolean> = _initialCheckDone

    // Do walidacji formularzy
    private val _emailError = MutableStateFlow<String?>(null)
    val emailError: StateFlow<String?> = _emailError

    private val _passwordError = MutableStateFlow<String?>(null)
    val passwordError: StateFlow<String?> = _passwordError

    // Google Sign In
    private lateinit var googleSignInClient: GoogleSignInClient

    // Facebook Sign In
    private val facebookCallbackManager = CallbackManager.Factory.create()

    companion object {
        const val GOOGLE_SIGN_IN_REQUEST_CODE = 1001
        private const val TAG = "AuthViewModel"
        private const val KEY_AUTH_STATE = "auth_state"
    }

    init {
        // Uruchom sprawdzanie w coroutine
        viewModelScope.launch {
            checkCurrentUser()
            // Po zakończeniu checkCurrentUser, ustaw flagę na true
            _initialCheckDone.value = true
            Log.d(TAG, "Initial auth check completed, initialCheckDone = true")
        }
    }

    /**
     * Pobiera zapamiętany stan uwierzytelnienia z SharedPreferences
     * Zwraca true, jeśli ostatni zapisany stan to Authenticated
     */
    fun getRememberedAuthState(): Boolean {
        return sharedPreferences.getBoolean(KEY_AUTH_STATE, false)
    }

    /**
     * Zapisuje stan uwierzytelnienia do SharedPreferences
     */
    private fun saveAuthState(isAuthenticated: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_AUTH_STATE, isAuthenticated).apply()
    }

    /**
     * Inicjalizuje klienta Google Sign In.
     * Należy wywołać na początku, np. w onCreate Activity
     */
    fun initGoogleSignIn(activity: Activity, webClientId: String) {
        try {
            Log.d(TAG, "Initializing Google Sign In with webClientId: $webClientId")

            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build()

            googleSignInClient = GoogleSignIn.getClient(activity, gso)
            Log.d(TAG, "Google Sign In initialized successfully")

            // Check if we have a previously signed in account
            val account = GoogleSignIn.getLastSignedInAccount(activity)
            if (account != null) {
                Log.d(TAG, "Found previously signed in Google account: ${account.email}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Google Sign In", e)
            _authState.value = AuthState.Error("Failed to initialize Google Sign In: ${e.message}")
        }
    }

    private suspend fun checkCurrentUser() {
        try {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                Log.d(TAG, "Current user found: ${currentUser.uid}")

                // Przygotuj aplikację dla tego użytkownika
                onSignInSuccess(currentUser)
            } else {
                Log.d(TAG, "No current user found, setting state to NotAuthenticated")
                // Zapisz stan uwierzytelnienia
                saveAuthState(false)
                _authState.value = AuthState.NotAuthenticated
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during initial auth check", e)
            _authState.value = AuthState.Error("Error checking authentication status: ${e.message}")
        }
    }

    /**
     * Inicjalizuje domyślne osiągnięcia w systemie
     */
    private suspend fun initializeAchievements() {
        try {
            Log.d(TAG, "Initializing default achievements...")
            val result = achievementService.initializeDefaultAchievements()
            if (result.isSuccess) {
                Log.d(TAG, "Default achievements initialized successfully")
            } else {
                Log.e(TAG, "Failed to initialize default achievements: ${result.exceptionOrNull()?.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception initializing achievements", e)
        }
    }

    /**
     * Obsługuje udane logowanie i przygotowuje aplikację dla użytkownika
     */
    private suspend fun onSignInSuccess(firebaseUser: FirebaseUser) {
        try {
            Log.d(TAG, "Processing successful sign in for user: ${firebaseUser.uid}")

            // 1. Przygotuj aplikację dla nowego użytkownika (wyczyść stare dane)
            val prepareResult = userRepository.prepareForNewUser(firebaseUser.uid)
            if (prepareResult.isFailure) {
                Log.w(TAG, "Failed to prepare for new user, but continuing...")
            }

            // 2. Inicjalizuj domyślne osiągnięcia przy każdym logowaniu
            initializeAchievements()

            // 3. Utwórz/pobierz dane użytkownika
            val userResult = userRepository.createOrGetUser(firebaseUser)
            if (userResult.isSuccess) {
                Log.d(TAG, "User data created/retrieved successfully")
            } else {
                Log.e(TAG, "Failed to create/get user data")
            }

            // 4. Aktualizacja czasu ostatniego logowania
            try {
                val result = userRepository.updateLastLogin(firebaseUser.uid)
                if (result.isSuccess) {
                    Log.d(TAG, "Last login updated successfully")

                    // Dodaj wywołanie synchronizacji treningów
                    try {
                        userRepository.getFullUserData(firebaseUser.uid) // To wywołanie powinno już synchronizować treningi
                    } catch (e: Exception) {
                        Log.e(TAG, "Error synchronizing workouts", e)
                    }
                } else {
                    Log.w(TAG, "Failed to update last login")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception updating last login", e)
            }

            // 5. Zaktualizuj stan uwierzytelniania
            saveAuthState(true)
            _authState.value = AuthState.Authenticated(firebaseUser)

            Log.d(TAG, "Sign in process completed successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Error during sign in process", e)
            // W przypadku błędu, nadal ustaw stan jako zalogowany
            // Synchronizacja danych może nastąpić później
            saveAuthState(true)
            _authState.value = AuthState.Authenticated(firebaseUser)
        }
    }

    /**
     * Waliduje format adresu email
     */
    private fun validateEmail(email: String): Boolean {
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"

        return when {
            email.isEmpty() -> {
                _emailError.value = "Email jest wymagany"
                false
            }
            !email.matches(emailPattern.toRegex()) -> {
                _emailError.value = "Niepoprawny format adresu email"
                false
            }
            else -> {
                _emailError.value = null
                true
            }
        }
    }

    /**
     * Waliduje format hasła
     */
    private fun validatePassword(password: String, isSignUp: Boolean = false): Boolean {
        return when {
            password.isEmpty() -> {
                _passwordError.value = "Hasło jest wymagane"
                false
            }
            isSignUp && password.length < 6 -> {
                _passwordError.value = "Hasło musi mieć co najmniej 6 znaków"
                false
            }
            else -> {
                _passwordError.value = null
                true
            }
        }
    }

    /**
     * Resetuje błędy walidacji formularza
     */
    fun resetValidationErrors() {
        _emailError.value = null
        _passwordError.value = null
    }

    /**
     * Logowanie przez email i hasło
     */
    fun signInWithEmail(email: String, password: String) {
        // Najpierw sprawdź poprawność danych
        val isEmailValid = validateEmail(email)
        val isPasswordValid = validatePassword(password)

        if (!isEmailValid || !isPasswordValid) {
            return
        }

        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                Log.d(TAG, "Attempting to sign in with email: $email")
                val result = auth.signInWithEmailAndPassword(email, password).await()

                result.user?.let { user ->
                    Log.d(TAG, "Email sign in successful for user: ${user.uid}")
                    onSignInSuccess(user)
                } ?: run {
                    Log.e(TAG, "Sign in succeeded but user is null")
                    saveAuthState(false)
                    _authState.value = AuthState.Error("Logowanie nie powiodło się. Sprawdź dane i spróbuj ponownie.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Email sign in failed", e)
                saveAuthState(false)
                // Tłumaczenie typowych błędów Firebase na przyjazne komunikaty
                val errorMessage = when (e.message) {
                    "There is no user record corresponding to this identifier. The user may have been deleted." ->
                        "Nie znaleziono użytkownika o podanym adresie email"
                    "The password is invalid or the user does not have a password." ->
                        "Niepoprawne hasło"
                    "The user account has been disabled by an administrator." ->
                        "Konto zostało zablokowane przez administratora"
                    "A network error (such as timeout, interrupted connection or unreachable host) has occurred." ->
                        "Problem z połączeniem internetowym. Sprawdź swoje połączenie i spróbuj ponownie."
                    else -> e.message ?: "Wystąpił nieznany błąd podczas logowania"
                }
                _authState.value = AuthState.Error(errorMessage)
            }
        }
    }

    /**
     * Resetowanie hasła (wysyłanie emaila resetującego)
     */
    fun resetPassword(email: String) {
        if (!validateEmail(email)) {
            return
        }

        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                Log.d(TAG, "Sending password reset email to: $email")
                auth.sendPasswordResetEmail(email).await()
                Log.d(TAG, "Password reset email sent successfully")
                _authState.value = AuthState.PasswordResetSent
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send password reset email", e)
                val errorMessage = when (e.message) {
                    "There is no user record corresponding to this identifier. The user may have been deleted." ->
                        "Nie znaleziono użytkownika o podanym adresie email"
                    else -> e.message ?: "Wystąpił nieznany błąd podczas resetowania hasła"
                }
                _authState.value = AuthState.Error(errorMessage)
            }
        }
    }

    /**
     * Rozpoczyna proces logowania przez Google.
     * Zwraca Intent, który należy uruchomić z Activity
     */
    fun signInWithGoogle(activity: Activity) {
        if (!::googleSignInClient.isInitialized) {
            Log.e(TAG, "Google Sign In not initialized. Call initGoogleSignIn first.")
            _authState.value = AuthState.Error("Google Sign In not initialized. Call initGoogleSignIn first.")
            return
        }

        Log.d(TAG, "Starting Google Sign In process")
        _authState.value = AuthState.Loading

        // Sign out first to ensure we get the account selection dialog
        googleSignInClient.signOut().addOnCompleteListener {
            Log.d(TAG, "Signed out of previous Google account")
            val signInIntent = googleSignInClient.signInIntent
            activity.startActivityForResult(signInIntent, GOOGLE_SIGN_IN_REQUEST_CODE)
        }
    }

    /**
     * Obsługuje wynik logowania przez Google.
     * Należy wywołać w onActivityResult Activity
     */
    fun handleGoogleSignInResult(data: Intent?) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                Log.d(TAG, "Handling Google Sign In result")

                if (data == null) {
                    Log.e(TAG, "Google sign-in failed: Intent data is null")
                    saveAuthState(false)
                    _authState.value = AuthState.Error("Google Sign In failed: No data received")
                    return@launch
                }

                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    Log.d(TAG, "Google sign-in successful: ${account.email}")
                    firebaseAuthWithGoogle(account)
                } catch (e: ApiException) {
                    // Handle specific API exceptions
                    Log.e(TAG, "Google sign-in failed with ApiException: ${e.statusCode}", e)
                    saveAuthState(false)
                    val errorMessage = when (e.statusCode) {
                        7 -> "This app needs to be verified by Google. Please check your configuration."
                        10 -> "A network error occurred. Please check your connection."
                        12501 -> "Sign in was cancelled by user"
                        12502 -> "The connection to Google Play Services has timed out"
                        else -> "Google Sign In failed: ${e.statusCode}"
                    }
                    _authState.value = AuthState.Error(errorMessage)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Google sign-in failed with unexpected exception", e)
                saveAuthState(false)
                _authState.value = AuthState.Error(e.message ?: "Google Sign In failed")
            }
        }
    }

    private suspend fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        try {
            Log.d(TAG, "Starting Firebase auth with Google token")
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            val result = auth.signInWithCredential(credential).await()
            Log.d(TAG, "Firebase auth successful, user: ${result.user?.uid}")

            val user = result.user
            if (user != null) {
                // Sprawdź czy to nowy użytkownik
                val isNewUser = result.additionalUserInfo?.isNewUser == true
                Log.d(TAG, "Is new user: $isNewUser")

                // Obsłuż logowanie niezależnie od tego czy to nowy czy istniejący użytkownik
                onSignInSuccess(user)
            } else {
                Log.e(TAG, "Firebase auth succeeded but user is null")
                saveAuthState(false)
                _authState.value = AuthState.Error("Authentication failed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase auth with Google failed", e)
            saveAuthState(false)
            _authState.value = AuthState.Error(e.message ?: "Google authentication failed")
        }
    }

    /**
     * Rozpoczyna proces logowania przez Facebook.
     */
    fun signInWithFacebook(activity: Activity) {
        Log.d(TAG, "Starting Facebook Sign In process")
        _authState.value = AuthState.Loading

        // Clear any previous session
        LoginManager.getInstance().logOut()

        LoginManager.getInstance().registerCallback(facebookCallbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    Log.d(TAG, "Facebook login success, token: ${result.accessToken.token}")
                    handleFacebookAccessToken(result.accessToken)
                }

                override fun onCancel() {
                    Log.d(TAG, "Facebook login cancelled by user")
                    saveAuthState(false)
                    _authState.value = AuthState.NotAuthenticated
                }

                override fun onError(error: FacebookException) {
                    Log.e(TAG, "Facebook login error", error)
                    saveAuthState(false)
                    _authState.value = AuthState.Error(error.message ?: "Facebook Sign In failed")
                }
            })

        // Rozpocznij logowanie przez Facebook, prosząc o dostęp do email
        LoginManager.getInstance().logInWithReadPermissions(activity, listOf("email", "public_profile"))
    }

    /**
     * Obsługuje wynik logowania przez Facebook.
     * Należy wywołać w onActivityResult Activity
     */
    fun handleFacebookSignInResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG, "Handling Facebook Sign In result: requestCode=$requestCode, resultCode=$resultCode")
        facebookCallbackManager.onActivityResult(requestCode, resultCode, data)
    }

    private fun handleFacebookAccessToken(token: AccessToken) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting Firebase auth with Facebook token")
                val credential = FacebookAuthProvider.getCredential(token.token)
                val result = auth.signInWithCredential(credential).await()

                val user = result.user
                if (user != null) {
                    Log.d(TAG, "Firebase auth with Facebook successful for user: ${user.uid}")

                    // Sprawdź czy to nowy użytkownik
                    val isNewUser = result.additionalUserInfo?.isNewUser == true
                    Log.d(TAG, "Is new user: $isNewUser")

                    // Obsłuż logowanie niezależnie od tego czy to nowy czy istniejący użytkownik
                    onSignInSuccess(user)
                } else {
                    Log.e(TAG, "Authentication succeeded but user is null")
                    saveAuthState(false)
                    _authState.value = AuthState.Error("Authentication failed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Facebook authentication failed", e)
                saveAuthState(false)
                _authState.value = AuthState.Error(e.message ?: "Facebook authentication failed")
            }
        }
    }

    /**
     * Rejestracja użytkownika przez email i hasło
     */
    fun signUp(email: String, password: String) {
        // Najpierw sprawdź poprawność danych
        val isEmailValid = validateEmail(email)
        val isPasswordValid = validatePassword(password, isSignUp = true)

        if (!isEmailValid || !isPasswordValid) {
            return
        }

        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val result = auth.createUserWithEmailAndPassword(email, password).await()

                val user = result.user
                if (user != null) {
                    try {
                        Log.d(TAG, "User created successfully: ${user.uid}")
                        onSignInSuccess(user)
                    } catch (e: Exception) {
                        // Jeśli nie udało się utworzyć profilu, usuń konto aby uniknąć zawieszonych kont
                        try {
                            user.delete().await()
                        } catch (deleteError: Exception) {
                            // Ignoruj błędy podczas usuwania
                        }
                        saveAuthState(false)
                        _authState.value = AuthState.Error("Failed to create user profile: ${e.message}")
                    }
                } else {
                    saveAuthState(false)
                    _authState.value = AuthState.Error("User creation failed")
                }
            } catch (e: Exception) {
                // Tłumaczenie typowych błędów Firebase na przyjazne komunikaty
                val errorMessage = when (e.message) {
                    "The email address is already in use by another account." ->
                        "Ten adres email jest już zajęty przez inne konto"
                    "The email address is badly formatted." ->
                        "Niepoprawny format adresu email"
                    "The password is invalid or the user does not have a password." ->
                        "Hasło musi mieć co najmniej 6 znaków"
                    "A network error (such as timeout, interrupted connection or unreachable host) has occurred." ->
                        "Problem z połączeniem internetowym. Sprawdź swoje połączenie i spróbuj ponownie."
                    else -> e.message ?: "Wystąpił nieznany błąd podczas rejestracji"
                }
                saveAuthState(false)
                _authState.value = AuthState.Error(errorMessage)
            }
        }
    }

    /**
     * Wylogowuje użytkownika i czyści lokalną bazę danych
     */
    fun signOut() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting sign out process")

                // 1. Wyczyść lokalną bazę danych
                val clearResult = userRepository.clearAllUserData()
                if (clearResult.isSuccess) {
                    Log.d(TAG, "Local database cleared successfully")
                } else {
                    Log.w(TAG, "Failed to clear local database")
                }

                // 2. Wyloguj z Google
                if (::googleSignInClient.isInitialized) {
                    googleSignInClient.signOut()
                }

                // 3. Wyloguj z Facebook
                LoginManager.getInstance().logOut()

                // 4. Wyloguj z Firebase
                auth.signOut()
                Log.d(TAG, "Signed out from Firebase")

                // 5. Zaktualizuj stan uwierzytelniania
                saveAuthState(false)
                _authState.value = AuthState.NotAuthenticated

                Log.d(TAG, "Sign out completed successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Error during sign out", e)
                // Nawet jeśli wystąpi błąd, spróbuj wylogować z Firebase
                try {
                    if (::googleSignInClient.isInitialized) {
                        googleSignInClient.signOut()
                    }
                    LoginManager.getInstance().logOut()
                    auth.signOut()
                    saveAuthState(false)
                    _authState.value = AuthState.NotAuthenticated
                } catch (firebaseError: Exception) {
                    Log.e(TAG, "Failed to sign out from Firebase", firebaseError)
                }
            }
        }
    }

    /**
     * Wymusza pełne odświeżenie danych użytkownika
     */
    fun refreshCurrentUser() {
        viewModelScope.launch {
            val currentUser = (_authState.value as? AuthState.Authenticated)?.user
            if (currentUser != null) {
                try {
                    Log.d(TAG, "Refreshing current user data")

                    // Wymuś synchronizację danych z Firebase
                    userRepository.getFullUserData(currentUser.uid)

                    Log.d(TAG, "User data refreshed successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error refreshing user data", e)
                }
            }
        }
    }

    fun resetAuthStateToNotAuthenticated() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.NotAuthenticated
        }
    }
}

sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: FirebaseUser?) : AuthState()
    object NotAuthenticated : AuthState()
    object PasswordResetSent : AuthState()
    data class Error(val message: String) : AuthState()
}