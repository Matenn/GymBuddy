package com.kaczmarzykmarcin.GymBuddy.features.workout.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.kaczmarzykmarcin.GymBuddy.R
import com.kaczmarzykmarcin.GymBuddy.core.presentation.components.AppScaffold
import com.kaczmarzykmarcin.GymBuddy.data.model.WorkoutTemplate
import com.kaczmarzykmarcin.GymBuddy.features.workout.presentation.components.WorkoutTemplateBottomSheet
import com.kaczmarzykmarcin.GymBuddy.features.workout.presentation.viewmodel.WorkoutViewModel
import com.kaczmarzykmarcin.GymBuddy.navigation.NavigationRoutes
import com.kaczmarzykmarcin.GymBuddy.ui.theme.LightGrayBackground

@Composable
fun WorkoutScreen(
    navController: NavController,
    workoutViewModel: WorkoutViewModel = hiltViewModel()
) {
    val workoutTemplates by workoutViewModel.workoutTemplates.collectAsState()
    val filteredWorkoutTemplates by workoutViewModel.filteredWorkoutTemplates.collectAsState()
    val templateSearchQuery by workoutViewModel.templateSearchQuery.collectAsState()
    val activeWorkout by workoutViewModel.activeWorkout.collectAsState()
    val currentUserId by workoutViewModel.currentUserId.collectAsState()
    val focusManager = LocalFocusManager.current

    // Stan dla wyświetlania bottom sheet z szczegółami szablonu
    var selectedTemplate by remember { mutableStateOf<WorkoutTemplate?>(null) }
    var showTemplateSheet by remember { mutableStateOf(false) }
    var isCreatingNewTemplate by remember { mutableStateOf(false) }

    // Fetch user's workout templates when screen is first displayed
    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            workoutViewModel.loadWorkoutTemplates(currentUserId)
            workoutViewModel.checkActiveWorkout(currentUserId)
            workoutViewModel.loadCategories()
            workoutViewModel.loadAllExercises()
        }
    }

    // Obsługa szablonu treningu
    if (showTemplateSheet) {
        WorkoutTemplateBottomSheet(
            isEditing = isCreatingNewTemplate,
            template = selectedTemplate,
            onDismiss = {
                showTemplateSheet = false
                selectedTemplate = null
                isCreatingNewTemplate = false
            },
            onTemplateSave = { template ->
                if (template.id.isEmpty()) {
                    workoutViewModel.createWorkoutTemplate(template)
                } else {
                    workoutViewModel.updateWorkoutTemplate(template)
                }
            },
            onTemplateDelete = { templateId ->
                workoutViewModel.deleteWorkoutTemplate(templateId)
            },
            navController = navController
        )
    }

    AppScaffold(
        navController = navController,
        workoutViewModel = workoutViewModel,
        contentPadding = PaddingValues(bottom = 80.dp)
    ) { paddingValues ->
        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp,0.dp,16.dp,16.dp)
                .padding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top).asPaddingValues())
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Title row with settings icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.training),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )

                // Settings icon that navigates to the category management screen
                IconButton(
                    onClick = {
                        navController.navigate(NavigationRoutes.CATEGORY_MANAGEMENT)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.manage_categories)
                    )
                }
            }

            // Template search bar (only show if there are templates)
            if (workoutTemplates.isNotEmpty()) {
                OutlinedTextField(
                    value = templateSearchQuery,
                    onValueChange = { workoutViewModel.updateTemplateSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    placeholder = { Text(stringResource(R.string.search_templates)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(32.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = LightGrayBackground,
                        unfocusedContainerColor = LightGrayBackground,
                        cursorColor = Color.Black,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                )
            }

            // Quick start section
            Text(
                text = stringResource(R.string.quick_start),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(8.dp)
            )

            Button(
                onClick = {
                    // Only start a new workout if there isn't already an active one
                    if (activeWorkout == null) {
                        workoutViewModel.startNewWorkout(currentUserId)
                    }
                    // Always show the recorder (whether new or existing workout)
                    workoutViewModel.showWorkoutRecorder(true)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black
                )
            ) {
                Text(
                    text = if (activeWorkout == null)
                        stringResource(R.string.start_new_workout)
                    else
                        stringResource(R.string.resume_workout)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Workout templates section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.workout_templates),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )

                // Add template button
                IconButton(
                    onClick = {
                        selectedTemplate = null
                        isCreatingNewTemplate = true
                        showTemplateSheet = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.create_new_template)
                    )
                }
            }

            // List of workout templates (use filtered list)
            if (workoutTemplates.isEmpty()) {
                // Empty state
                EmptyTemplatesState(
                    onClick = {
                        selectedTemplate = null
                        isCreatingNewTemplate = true
                        showTemplateSheet = true
                    }
                )
            } else if (filteredWorkoutTemplates.isEmpty() && templateSearchQuery.isNotEmpty()) {
                // No search results state
                NoTemplatesFoundState()
            } else {
                filteredWorkoutTemplates.forEach { template ->
                    WorkoutTemplateItem(
                        template = template,
                        onEditClick = {
                            selectedTemplate = template
                            isCreatingNewTemplate = false
                            showTemplateSheet = true
                        },
                        onTemplateClick = {
                            selectedTemplate = template
                            isCreatingNewTemplate = false
                            showTemplateSheet = true
                        }
                    )
                }
            }

            // Add space for the bottom elements
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun EmptyTemplatesState(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFEFF1F5)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = stringResource(R.string.no_templates_yet),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )

            Text(
                text = stringResource(R.string.create_first_template),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun NoTemplatesFoundState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.LightGray
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.no_templates_found),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.try_different_search),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun WorkoutTemplateItem(
    template: WorkoutTemplate,
    onEditClick: () -> Unit,
    onTemplateClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onTemplateClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (template.description.isNotEmpty()) {
                    Text(
                        text = template.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Display number of exercises
                Text(
                    text = stringResource(
                        id = R.string.exercises_count,
                        template.exercises.size
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.edit_template)
                )
            }
        }
    }
}