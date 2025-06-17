// CategoryManagementScreen.kt (ciąg dalszy)
package com.kaczmarzykmarcin.GymBuddy.features.workout.presentation.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.kaczmarzykmarcin.GymBuddy.R
import com.kaczmarzykmarcin.GymBuddy.core.presentation.components.BottomNavigationBar
import com.kaczmarzykmarcin.GymBuddy.features.workouts.domain.model.WorkoutCategory
import com.kaczmarzykmarcin.GymBuddy.features.workout.presentation.viewmodel.WorkoutViewModel
import com.kaczmarzykmarcin.GymBuddy.core.presentation.theme.AppBackgroundLight
import com.kaczmarzykmarcin.GymBuddy.core.presentation.theme.Black
import com.kaczmarzykmarcin.GymBuddy.core.presentation.theme.DarkGray
import com.kaczmarzykmarcin.GymBuddy.core.presentation.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(
    navController: NavController,
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsState(initial = emptyList())
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<WorkoutCategory?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<WorkoutCategory?>(null) }

    LaunchedEffect(key1 = Unit) {
        viewModel.loadCategories()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manage_categories)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppBackgroundLight,
                    titleContentColor = Black
                )
            )
        },
        bottomBar = { BottomNavigationBar(navController) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddCategoryDialog = true },
                containerColor = Color.Black,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_category))
            }
        },
        containerColor = AppBackgroundLight
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            val sortedCategories = categories.sortedBy { it.isDefault }

            items(sortedCategories) { category ->
                CategoryItem(
                    category = category,
                    onEdit = { editingCategory = category },
                    onDelete = {
                        if (!category.isDefault) {
                            showDeleteConfirmDialog = category
                        }
                    }
                )
            }
        }
    }

    // Dialog dla potwierdzenia usunięcia
    showDeleteConfirmDialog?.let { category ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text(stringResource(R.string.delete_category)) },
            text = {
                Text(
                    stringResource(
                        R.string.delete_category_confirmation,
                        category.name
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCategory(category.id)
                        showDeleteConfirmDialog = null
                    }
                ) {
                    Text(
                        stringResource(R.string.delete),
                        color = Color.Red
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Dialog dla dodawania/edycji kategorii
    if (showAddCategoryDialog || editingCategory != null) {
        CategoryDialog(
            category = editingCategory,
            onDismiss = {
                showAddCategoryDialog = false
                editingCategory = null
            },
            onSave = { name, color ->
                if (editingCategory != null) {
                    viewModel.updateCategory(editingCategory!!.copy(name = name, color = color))
                } else {
                    viewModel.addCategory(name, color)
                }
                showAddCategoryDialog = false
                editingCategory = null
            }
        )
    }
}

@Composable
fun CategoryItem(
    category: WorkoutCategory,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color circle
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.parseColor(category.color)))
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Category name
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleMedium,
                color = Black,
                modifier = Modifier.weight(1f)
            )

            // Default indicator if needed
            if (category.isDefault) {
                Text(
                    text = stringResource(R.string.default_label),
                    color = DarkGray,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(end = 16.dp)
                )
            }

            // Action buttons (tylko dla niestandardowych kategorii)
            if (!category.isDefault) {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit),
                        tint = Black
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = Color.Red
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryDialog(
    category: WorkoutCategory?,
    onDismiss: () -> Unit,
    onSave: (name: String, color: String) -> Unit
) {
    val isEditing = category != null
    var categoryName by remember { mutableStateOf(category?.name ?: "") }
    var categoryColor by remember { mutableStateOf(category?.color ?: "#4285F4") }

    val availableColors = listOf(
        "#4285F4", // Blue
        "#EA4335", // Red
        "#FBBC05", // Yellow
        "#34A853", // Green
        "#9C27B0", // Purple
        "#FF9800", // Orange
        "#795548", // Brown
        "#607D8B", // Gray
        "#00BCD4", // Cyan
        "#009688", // Teal
        "#FF5722", // Deep Orange
        "#3F51B5"  // Indigo
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (isEditing) R.string.edit_category else R.string.add_category
                )
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { categoryName = it },
                    label = { Text(stringResource(R.string.category_name)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                Text(
                    text = stringResource(R.string.category_color),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Siatka dostępnych kolorów
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableColors) { color ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(color)))
                                .border(
                                    width = if (color == categoryColor) 3.dp else 0.dp,
                                    color = Color.Black,
                                    shape = CircleShape
                                )
                                .clickable { categoryColor = color }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (categoryName.isNotBlank()) {
                        onSave(categoryName, categoryColor)
                    }
                },
                enabled = categoryName.isNotBlank()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}