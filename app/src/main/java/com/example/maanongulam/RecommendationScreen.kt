package com.example.maanongulam

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun RecommendationScreen(
    viewModel: RecommendationViewModel = viewModel()
) {
    val recommendations by viewModel.recommendations.collectAsState()
    val cookingLog by viewModel.cookingLog.collectAsState()

    if (cookingLog != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearCookingLog() },
            title = { Text("Cooking Report") },
            text = {
                Column {
                    cookingLog?.forEach { line ->
                        Text(text = line, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearCookingLog() }) {
                    Text("OK")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Recommendations",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (recommendations.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No recommendations available. Add ingredients and recipes first!")
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(recommendations) { index, item ->
                    if (index == 0) {
                        Text(
                            text = "Ulam of the Day",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        RecipeCard(
                            recommendedRecipe = item,
                            isTopPick = true,
                            onCookClick = { viewModel.cookRecipe(item.recipe) }
                        )
                        if (recommendations.size > 1) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Runners-up",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    } else {
                        RecipeCard(
                            recommendedRecipe = item,
                            isTopPick = false,
                            onCookClick = { viewModel.cookRecipe(item.recipe) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecipeCard(
    recommendedRecipe: RecommendedRecipe,
    isTopPick: Boolean,
    onCookClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (isTopPick) 
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        else 
            CardDefaults.cardColors(),
        elevation = if (isTopPick) CardDefaults.cardElevation(defaultElevation = 4.dp) else CardDefaults.cardElevation()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = recommendedRecipe.recipe.name,
                    style = if (isTopPick) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                if (recommendedRecipe.isInsufficient) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Insufficient",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Missing Ingredients",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Urgency Score: ${String.format("%.4f", recommendedRecipe.urgencyScore)}",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onCookClick,
                modifier = Modifier.align(Alignment.End),
                colors = if (recommendedRecipe.isInsufficient)
                    ButtonDefaults.buttonColors(containerColor = Color.Gray)
                else
                    ButtonDefaults.buttonColors()
            ) {
                Text("Cook This")
            }
        }
    }
}
