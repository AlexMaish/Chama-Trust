package com.example.chamabuddy.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.chamabuddy.domain.model.Member
import com.example.chamabuddy.presentation.viewmodel.MemberEvent
import com.example.chamabuddy.presentation.viewmodel.MemberState
import com.example.chamabuddy.presentation.viewmodel.MemberViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersScreen(
    groupId: String,
    navigateBack: () -> Unit,
    navigateToProfile: (String) -> Unit,
    viewModel: MemberViewModel = hiltViewModel()
) {
    // Track if we should show the Add Member Dialog
    var showAddDialog by remember { mutableStateOf(false) }

    // Load members when groupId changes
    LaunchedEffect(groupId) {
        viewModel.setGroupId(groupId)
        viewModel.handleEvent(MemberEvent.LoadMembersForGroup(groupId))
    }

    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Group Members") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.zIndex(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Member")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (state) {
                MemberState.Loading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                is MemberState.Error -> {
                    ErrorState(
                        message = (state as MemberState.Error).message,
                        onRetry = { viewModel.handleEvent(MemberEvent.LoadMembersForGroup(groupId)) }
                    )
                }
                is MemberState.MembersLoaded -> {
                    MemberListContent(
                        members = (state as MemberState.MembersLoaded).members,
                        onItemClick = navigateToProfile
                    )
                }
                is MemberState.Empty -> {
                    EmptyState(message = (state as MemberState.Empty).message)
                }
                else -> Unit // Handle other states if needed
            }
        }

        if (showAddDialog) {
            AddMemberDialog(
                groupId = groupId,
                onDismiss = { showAddDialog = false },
                onAddMember = { newMember ->
                    viewModel.handleEvent(MemberEvent.AddMember(newMember))
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
private fun MemberListContent(
    members: List<Member>,
    onItemClick: (String) -> Unit
) {
    if (members.isEmpty()) {
        EmptyState(message = "No members found")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(members) { member ->
                MemberListItem(
                    member = member,
                    onClick = { onItemClick(member.memberId) }
                )
            }
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Add a new member to get started", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun MemberListItem(
    member: Member,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InitialsAvatar(name = member.name)

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = member.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            RoleBadge(isAdmin = member.isAdmin)
        }
    }
}

@Composable
private fun InitialsAvatar(name: String) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.take(1).uppercase(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun RoleBadge(isAdmin: Boolean) {
    Badge(
        containerColor = if (isAdmin) MaterialTheme.colorScheme.tertiaryContainer
        else MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = if (isAdmin) "Admin" else "Member",
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun AddMemberDialog(
    groupId: String,
    onDismiss: () -> Unit,
    onAddMember: (Member) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var isAdmin by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Member") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("Nickname (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Administrator",
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = isAdmin,
                        onCheckedChange = { isAdmin = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newMember = Member(
                        memberId = "", // Will be generated by repository
                        name = name.trim(),
                        nickname = nickname.takeIf { it.isNotBlank() },
                        phoneNumber = phone.trim(),
                        isAdmin = isAdmin,
                        groupId = groupId
                    )
                    onAddMember(newMember)
                },
                enabled = name.isNotBlank() && phone.isNotBlank()
            ) {
                Text("Add Member")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}