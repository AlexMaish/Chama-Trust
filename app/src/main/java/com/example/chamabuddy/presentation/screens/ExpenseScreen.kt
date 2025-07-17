package com.example.chamabuddy.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.chamabuddy.domain.model.ExpenseEntity
import com.example.chamabuddy.domain.model.BenefitEntity
import com.example.chamabuddy.presentation.viewmodel.ExpenseViewModel
import com.example.chamabuddy.presentation.viewmodel.BenefitViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(groupId: String) {
    val viewModel: ExpenseViewModel = hiltViewModel()
    val items by viewModel.expenses.collectAsState()
    val total by viewModel.total.collectAsState()
    val showDialog by viewModel.showDialog.collectAsState()

    LaunchedEffect(groupId) {
        viewModel.loadData(groupId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expenses") },
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
                Icon(Icons.Default.Add, contentDescription = "Add Expense")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(items) { expense ->
                ExpandableExpenseItem(expense)
            }
        }

        if (showDialog) {
            AddExpenseDialog(
                onDismiss = { viewModel.hideAddDialog() },
                onConfirm = { title, desc, amount ->
                    viewModel.addExpense(
                        ExpenseEntity(
                            groupId = groupId,
                            title = title,
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
fun ExpandableExpenseItem(expense: ExpenseEntity) {
    var expanded by remember { mutableStateOf(false) }
    val date = remember {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(expense.date))
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
                    text = expense.title,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text("Ksh${"%.2f".format(expense.amount)}")
            }

            Text(
                text = date,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )

            AnimatedVisibility(visible = expanded) {
                Text(
                    text = expense.description,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Expense") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
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
                    if (title.isNotBlank() && amt > 0) {
                        onConfirm(title, desc, amt)
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
