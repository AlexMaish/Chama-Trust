package com.example.chamabuddy.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.chamabuddy.R
import com.example.chamabuddy.domain.model.Group
import com.example.chamabuddy.presentation.navigation.GroupsHomeDestination
import com.example.chamabuddy.presentation.viewmodel.GroupHomeViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsHomeScreen(
    navigateToGroupCycles: (String) -> Unit,
    onBottomBarVisibilityChange: (Boolean) -> Unit,
    viewModel: GroupHomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var groupName by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle dialog visibility from ViewModel
    val showDialog by remember(uiState.showCreateGroupDialog) {
        derivedStateOf { uiState.showCreateGroupDialog }
    }

    // Handle snackbar messages
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackbar()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(GroupsHomeDestination.title) })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showCreateGroupDialog() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create new group")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.groups.isEmpty() -> {
                    EmptyGroupsPlaceholder(
                        modifier = Modifier.fillMaxSize(),
                        onCreateClick = { viewModel.showCreateGroupDialog() }
                    )
                }
                else -> {
                    GroupList(
                        groups = uiState.groups,
                        onGroupClick = { navigateToGroupCycles(it.groupId) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    // Create Group Dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideCreateGroupDialog() },
            title = { Text("Create New Group") },
            text = {
                Column {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Group Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = uiState.nameValidationError != null
                    )

                    uiState.nameValidationError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.validateAndCreateGroup(groupName) },
                    enabled = groupName.isNotBlank()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.hideCreateGroupDialog() }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun EmptyGroupsPlaceholder(modifier: Modifier = Modifier, onCreateClick: () -> Unit) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "You're not part of any groups yet",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Create your first group to get started",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onCreateClick) {
            Text("Create Group")
        }
    }
}

@Composable
fun GroupList(
    groups: List<Group>,
    onGroupClick: (Group) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(groups) { group ->
            GroupListItem(
                group = group,
                onClick = { onGroupClick(group) }
            )
        }
    }
}

@Composable
fun GroupListItem(group: Group, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = group.name.take(1),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = group.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}