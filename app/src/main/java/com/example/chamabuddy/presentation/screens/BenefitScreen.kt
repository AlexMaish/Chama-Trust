package com.example.chamabuddy.presentation.screens


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.chamabuddy.domain.model.BenefitEntity
import com.example.chamabuddy.presentation.navigation.AuthDestination.title
import com.example.chamabuddy.presentation.viewmodel.BenefitViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BenefitScreen(groupId: String) {
    val viewModel: BenefitViewModel = hiltViewModel()
    val items by viewModel.benefits.collectAsState()
    val total by viewModel.total.collectAsState()
    val showDialog by viewModel.showDialog.collectAsState()

    LaunchedEffect(groupId) {
        viewModel.loadData(groupId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Benefits") },
                actions = {
                    Text(
                        "Total: Ksh${"%.2f".format(total)}",
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showAddDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "Add Benefit")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(items) { benefit ->
                ExpandableBenefitItem(benefit)
            }
        }

        if (showDialog) {
            AddBenefitDialog(
                onDismiss = { viewModel.hideAddDialog() },
                onConfirm = { name, desc, amount ->
                    viewModel.addBenefit(
                        BenefitEntity(
                            groupId = groupId,
                            name = name, // ✅ FIXED
                            description = desc,
                            amount = amount,
                            date = System.currentTimeMillis()
                        )
                    )
                }
            )
        }
    }
}

@Composable
fun ExpandableBenefitItem(benefit: BenefitEntity) {
    var expanded by remember { mutableStateOf(false) }
    val date = remember {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(benefit.date))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = benefit.name,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text("Ksh${"%.2f".format(benefit.amount)}")
            }

            Text(
                text = date,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )

            AnimatedVisibility(visible = expanded) {
                Text(
                    text = benefit.description,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun AddBenefitDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double) -> Unit
) {
    var name by remember { mutableStateOf("") } // Corrected: use 'name' for benefit name
    var desc by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Benefit") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    // FIX: Use 'name' instead of 'title'
                    if (name.isNotBlank() && amt > 0) {
                        onConfirm(name, desc, amt)
                        onDismiss()
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}