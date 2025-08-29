package com.example.chamabuddy.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.chamabuddy.domain.model.Penalty
import com.example.chamabuddy.presentation.viewmodel.PenaltyViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PenaltyScreen(groupId: String) {
    val viewModel: PenaltyViewModel = hiltViewModel()
    val penalties by viewModel.penalties.collectAsState()
    val total by viewModel.total.collectAsState()
    val members by viewModel.members.collectAsState()
    val filteredMembers by viewModel.filteredMembers.collectAsState()
    val showDialog by viewModel.showDialog.collectAsState()

    LaunchedEffect(groupId) {
        viewModel.loadData(groupId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Penalties") },
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
                Icon(Icons.Default.Add, contentDescription = "Add Penalty")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(penalties) { penalty ->
                ExpandablePenaltyItem(
                    penalty = penalty,
                    onDelete = { viewModel.deletePenalty(penalty.penaltyId) }
                )
            }
        }

        if (showDialog) {
            AddPenaltyDialog(
                onDismiss = { viewModel.hideAddDialog() },
                onConfirm = { memberId, memberName, description, amount ->
                    viewModel.addPenalty(
                        Penalty(
                            groupId = groupId,
                            memberId = memberId,
                            memberName = memberName,
                            description = description,
                            amount = amount,
                            date = System.currentTimeMillis()
                        )
                    )
                },
                members = members,
                filteredMembers = filteredMembers,
                onFilterMembers = { query -> viewModel.filterMembers(query) }
            )
        }
    }
}

@Composable
fun ExpandablePenaltyItem(penalty: Penalty, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val date = remember {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(penalty.date))
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Penalty") },
            text = { Text("Are you sure you want to delete this penalty?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
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
                    text = penalty.description,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text("Ksh${"%.2f".format(penalty.amount)}")
            }

            Text(
                text = date,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )

            AnimatedVisibility(visible = expanded) {
                Column {
                    Text(
                        text = "Member ID: ${penalty.memberId}",
                        modifier = Modifier.padding(16.dp)
                    )
                    Button(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Delete Penalty")
                    }
                }
            }
        }
    }
}
@Composable
fun AddPenaltyDialog(
    onDismiss: () -> Unit,
    // now: memberId, memberName, description, amount
    onConfirm: (String, String, String, Double) -> Unit,
    members: List<com.example.chamabuddy.domain.model.Member>,
    filteredMembers: List<com.example.chamabuddy.domain.model.Member>,
    onFilterMembers: (String) -> Unit
) {
    var memberQuery by remember { mutableStateOf("") }
    var selectedMember by remember { mutableStateOf<com.example.chamabuddy.domain.model.Member?>(null) }
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var showMemberDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(memberQuery) {
        onFilterMembers(memberQuery)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Penalty") },
        text = {
            Column {
                // Member selection
                Box {
                    OutlinedTextField(
                        value = memberQuery,
                        onValueChange = {
                            memberQuery = it
                            showMemberDropdown = it.isNotBlank()
                        },
                        label = { Text("Search Member") },
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Member")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Member dropdown
                    if (showMemberDropdown && filteredMembers.isNotEmpty()) {
                        DropdownMenu(
                            expanded = showMemberDropdown,
                            onDismissRequest = { showMemberDropdown = false }
                        ) {
                            filteredMembers.forEach { member ->
                                DropdownMenuItem(
                                    text = { Text(member.name) },
                                    onClick = {
                                        selectedMember = member
                                        memberQuery = member.name
                                        showMemberDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
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
                    if (selectedMember != null && description.isNotBlank() && amt > 0) {
                        // pass memberId AND memberName now
                        onConfirm(selectedMember!!.memberId, selectedMember!!.name, description, amt)
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
