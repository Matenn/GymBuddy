package com.kaczmarzykmarcin.GymBuddy.features.workout.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.kaczmarzykmarcin.GymBuddy.R
import com.kaczmarzykmarcin.GymBuddy.core.data.model.PreviousSetInfo
import com.kaczmarzykmarcin.GymBuddy.data.model.CompletedExercise
import com.kaczmarzykmarcin.GymBuddy.data.model.ExerciseSet
import com.kaczmarzykmarcin.GymBuddy.data.model.WorkoutTemplate
import com.kaczmarzykmarcin.GymBuddy.features.exercises.presentation.components.ExerciseSelectionBottomSheet
import com.kaczmarzykmarcin.GymBuddy.features.workout.presentation.viewmodel.WorkoutViewModel





@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutTemplateBottomSheet(
    isEditing: Boolean,
    template: WorkoutTemplate?,
    onDismiss: () -> Unit,
    onTemplateSave: (WorkoutTemplate) -> Unit,
    onTemplateDelete: (String) -> Unit,
    navController: NavController? = null,
    workoutViewModel: WorkoutViewModel = hiltViewModel()
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val scope = rememberCoroutineScope()

    // Stany dla edycji szablonu
    var isEditMode by remember { mutableStateOf(isEditing || template == null) }
    var showExerciseSelection by remember { mutableStateOf(false) }
    var showDiscardChangesDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showNameEditDialog by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }

    // Podstawowe dane szablonu
    val currentUserId by workoutViewModel.currentUserId.collectAsState()
    val initialName = template?.name ?: stringResource(id = R.string.new_workout_template)
    var templateName by remember { mutableStateOf(initialName) }
    var selectedCategoryId by remember(template?.categoryId) { mutableStateOf(template?.categoryId) }

    // Kategorie
    val categories by workoutViewModel.categories.collectAsState(initial = emptyList())
    val selectedCategory = remember(selectedCategoryId, categories) {
        categories.find { it.id == selectedCategoryId }
    }

    // Lista dostępnych ćwiczeń
    val availableExercises by workoutViewModel.exercisesList.collectAsState()

    // Ćwiczenia w szablonie z zachowaniem wszystkich informacji o seriach
    val exercises = remember(template?.id) { mutableStateListOf<CompletedExercise>() }

    // Załaduj ćwiczenia dla szablonu
    LaunchedEffect(template, availableExercises) {
        exercises.clear()

        // Jeśli szablon istnieje, załaduj jego ćwiczenia
        if (template != null) {
            exercises.addAll(template.exercises)
        }
    }

    // Załaduj wszystkie ćwiczenia na początku
    LaunchedEffect(Unit) {
        workoutViewModel.loadAllExercises()
        // Załaduj dane o poprzednich seriach (potrzebne do podpowiedzi)
        workoutViewModel.loadPreviousSetsData(currentUserId)
        // Ładuj dane statystyk ćwiczeń
        workoutViewModel.loadExerciseStats(currentUserId)
    }

    // Lokalny copy szablonu do edycji
    val currentTemplate = remember(template, templateName, exercises, selectedCategoryId) {
        WorkoutTemplate(
            id = template?.id ?: "",
            userId = template?.userId ?: currentUserId,
            name = templateName,
            description = "", // Pusty opis
            categoryId = selectedCategoryId,
            exercises = exercises.toList() // Przechowujemy pełne obiekty CompletedExercise
        )
    }

    // Dialog edycji nazwy
    if (showNameEditDialog) {
        AlertDialog(
            onDismissRequest = { showNameEditDialog = false },
            title = { Text(stringResource(R.string.edit_template_name)) },
            text = {
                OutlinedTextField(
                    value = templateName,
                    onValueChange = { templateName = it },
                    label = { Text(stringResource(R.string.template_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { showNameEditDialog = false }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    templateName = template?.name ?: ""
                    showNameEditDialog = false
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Dialog potwierdzenia odrzucenia zmian
    if (showDiscardChangesDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardChangesDialog = false },
            title = { Text(stringResource(R.string.discard_changes)) },
            text = { Text(stringResource(R.string.discard_changes_confirmation)) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardChangesDialog = false
                    isEditMode = false

                    // Reset to original values
                    templateName = template?.name ?: ""
                    selectedCategoryId = template?.categoryId

                    // Przywróć oryginalne ćwiczenia
                    exercises.clear()
                    if (template != null) {
                        exercises.addAll(template.exercises)
                    }
                }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardChangesDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Dialog potwierdzenia usunięcia
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.delete_template)) },
            text = { Text(stringResource(R.string.delete_template_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        template?.id?.let { onTemplateDelete(it) }
                        onDismiss()
                    }
                ) {
                    Text(stringResource(R.string.confirm), color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (isEditMode &&
                (templateName != template?.name ||
                        selectedCategoryId != template.categoryId ||
                        exercises != template.exercises)) {
                showDiscardChangesDialog = true
            } else {
                onDismiss()
            }
        },
        sheetState = sheetState,
        containerColor = Color.White,
        modifier = Modifier.padding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top).asPaddingValues())
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top controls - przyciski
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Close/Cancel button
                Box(
                    modifier = Modifier.width(48.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (isEditMode) {
                        IconButton(onClick = {
                            if (templateName != template?.name ||
                                selectedCategoryId != template.categoryId ||
                                exercises != template?.exercises) {
                                showDiscardChangesDialog = true
                            } else {
                                isEditMode = false
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.cancel)
                            )
                        }
                    } else {
                        IconButton(onClick = { onDismiss() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.close)
                            )
                        }
                    }
                }

                // Tytuł w środku
                Text(
                    text = if (template == null)
                        stringResource(R.string.new_template)
                    else
                        stringResource(R.string.template_details),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )

                // Edit/Save button
                Box(
                    modifier = Modifier.width(80.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    if (isEditMode) {
                        Button(
                            onClick = {
                                // Zapisz zmiany w szablonie ze wszystkimi informacjami o ćwiczeniach
                                val updatedTemplate = WorkoutTemplate(
                                    id = template?.id ?: "",
                                    userId = template?.userId ?: currentUserId,
                                    name = templateName,
                                    description = "",
                                    categoryId = selectedCategoryId,
                                    exercises = exercises.toList()  // Zapisz pełne obiekty CompletedExercise
                                )
                                // Save changes
                                onTemplateSave(updatedTemplate)
                                isEditMode = false
                            },
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Black
                            )
                        ) {
                            Text(stringResource(R.string.save))
                        }

                    } else {
                        Button(
                            onClick = { isEditMode = true },
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Black
                            )
                        ) {
                            Text(stringResource(R.string.edit))
                        }
                    }
                }
            }

            // W trybie edycji, dodaj wiersz do wyboru kategorii
            if (isEditMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.workout_category),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    // Przycisk wyboru kategorii
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { showCategoryPicker = true }
                            .border(
                                width = 1.dp,
                                color = selectedCategory?.let {
                                    Color(android.graphics.Color.parseColor(it.color))
                                } ?: Color.Gray,
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            selectedCategory?.let {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(it.color)))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(it.name)
                            } ?: Text(
                                text = stringResource(R.string.select_category),
                                color = Color.Gray
                            )
                        }
                    }
                }

                // Dialog wyboru kategorii w trybie edycji
                if (showCategoryPicker) {
                    CategoryPickerDialog(
                        categories = categories,
                        selectedCategoryId = selectedCategoryId,
                        onCategorySelected = { categoryId ->
                            selectedCategoryId = categoryId
                            showCategoryPicker = false
                        },
                        onDismissRequest = { showCategoryPicker = false },
                        navController = navController
                    )
                }
            }
            // W trybie podglądu, tylko wyświetl kategorię (jeśli istnieje)
            else {
                template?.categoryId?.let { catId ->
                    val category = categories.find { it.id == catId }

                    category?.let {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.workout_category) + ":",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // Etykieta kategorii
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = Color(android.graphics.Color.parseColor(category.color)),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = category.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Nazwa szablonu z przyciskiem edycji (tylko w trybie edycji)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = templateName,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (isEditMode) {
                    IconButton(onClick = { showNameEditDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit_template_name)
                        )
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))

            // Main content (exercise list)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (exercises.isEmpty()) {
                    // Empty state
                    EmptyTemplateState()
                } else {
                    // List of exercises
                    exercises.forEachIndexed { index, exercise ->
                        if (isEditMode) {
                            // W trybie edycji używamy ExerciseItemWorkout z TrainingRecorderBottomSheet
                            ExerciseItemWorkout(
                                exercise = exercise,
                                userId = currentUserId,
                                onExerciseUpdated = { updatedExercise ->
                                    exercises[index] = updatedExercise
                                },
                                onExerciseDeleted = {
                                    exercises.removeAt(index)
                                }
                            )
                        } else {
                            // W trybie podglądu używamy ReadOnlyTemplateExerciseItem
                            ReadOnlyTemplateExerciseItem(exercise = exercise)
                        }

                        if (index < exercises.size - 1) {
                            Divider(modifier = Modifier.padding(vertical = 16.dp))
                        }
                    }
                }

                // Extra space at bottom
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Bottom buttons (zależne od trybu)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                if (isEditMode) {
                    // Add exercise button
                    Button(
                        onClick = { showExerciseSelection = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black
                        )
                    ) {
                        Text(text = stringResource(R.string.add_exercise))
                    }

                    // Delete template button (tylko jeśli szablon już istnieje)
                    if (template != null) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { showDeleteConfirmation = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEFF1F5),
                                contentColor = Color.Red
                            )
                        ) {
                            Text(text = stringResource(R.string.delete_template))
                        }
                    }
                } else {
                    // Start workout with template button
                    Button(
                        onClick = {
                            template?.let {
                                workoutViewModel.startWorkoutFromTemplate(it)
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black
                        )
                    ) {
                        Text(text = stringResource(R.string.start_workout_with_template))
                    }
                }
            }
        }
    }

    // Exercise selection sheet
    if (showExerciseSelection) {
        ExerciseSelectionBottomSheet(
            onDismiss = { showExerciseSelection = false },
            onExercisesSelected = { selectedExercises ->
                // Konwertuj do CompletedExercise i dodawaj do listy
                val newExercises = selectedExercises.map { exercise ->
                    CompletedExercise(
                        exerciseId = exercise.id,
                        name = exercise.name,
                        category = exercise.category,
                        sets = listOf(
                            ExerciseSet(
                                setType = "normal",
                                weight = 0.0,
                                reps = 0
                            )
                        )
                    )
                }
                exercises.addAll(newExercises)
                showExerciseSelection = false
            }
        )
    }
}

@Composable
fun EmptyTemplateState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_logo_barbell),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color.LightGray
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.add_exercises_to_template),
            textAlign = TextAlign.Center,
            color = Color.Gray
        )
    }
}

@Composable
fun ReadOnlyTemplateExerciseItem(
    exercise: CompletedExercise
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Nazwa ćwiczenia
        Text(
            text = exercise.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Kategoria ćwiczenia
        Text(
            text = exercise.category,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Nagłówki kolumn
        if (exercise.sets.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Kolumna typu serii
                Box(modifier = Modifier.width(60.dp)) {
                    Text(
                        text = stringResource(R.string.seria),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }

                // Kolumna wagi
                Text(
                    text = stringResource(R.string.ciezar),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                // Kolumna powtórzeń
                Text(
                    text = stringResource(R.string.powtorzenia),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Serie ćwiczenia - użyj istniejącej funkcji ReadOnlySetItem z WorkoutDetailsBottomSheet.kt
        exercise.sets.forEachIndexed { index, set ->
            // Oblicz numer serii normalnej do wyświetlenia
            val normalSetNumber = exercise.sets.take(index + 1).count { it.setType == "normal" }

            // Wykorzystujemy istniejącą funkcję ReadOnlySetItem z WorkoutDetailsBottomSheet.kt
            ReadOnlySetItem(
                set = set,
                normalSetNumber = if (set.setType == "normal") normalSetNumber else 0
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}