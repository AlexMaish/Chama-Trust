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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.chamabuddy.domain.model.Member
import com.example.chamabuddy.presentation.viewmodel.MemberEvent
import com.example.chamabuddy.presentation.viewmodel.MemberState
import com.example.chamabuddy.presentation.viewmodel.MemberViewModel

//// Premium color scheme
//val PremiumNavy = Color(0xFF0A1D3A)
//val SoftOffWhite = Color(0xFFF8F9FA)
//val VibrantOrange = Color(0xFFFF6B35)
//val CardSurface = Color(0xFFFFFFFF)
//val LightAccentBlue = Color(0xFFE3F2FD)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersScreen(
    groupId: String,
    navigateBack: () -> Unit,
    navigateToProfile: (String) -> Unit,
    viewModel: MemberViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackbar()
        }
    }

    LaunchedEffect(groupId) {
        viewModel.setGroupId(groupId)
        viewModel.handleEvent(MemberEvent.LoadMembersForGroup(groupId))
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "Group Members",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = PremiumNavy,
                    scrolledContainerColor = PremiumNavy,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = VibrantOrange,
                modifier = Modifier.zIndex(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Member", tint = Color.White)
            }
        },
        containerColor = SoftOffWhite
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
                else -> Unit
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = VibrantOrange)
        ) {
            Text("Retry", color = Color.White)
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
        Text(message, color = PremiumNavy)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Add a new member to get started",
            style = MaterialTheme.typography.bodySmall,
            color = PremiumNavy.copy(alpha = 0.7f))
    }
}

@Composable
fun MemberListItem(
    member: Member,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
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
                    fontWeight = FontWeight.Bold,
                    color = PremiumNavy
                )
                Text(
                    text = member.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PremiumNavy.copy(alpha = 0.7f)
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
                color = LightAccentBlue,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.take(1).uppercase(),
            style = MaterialTheme.typography.titleLarge,
            color = PremiumNavy,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RoleBadge(isAdmin: Boolean) {
    Badge(
        containerColor = if (isAdmin) VibrantOrange else LightAccentBlue
    ) {
        Text(
            text = if (isAdmin) "Admin" else "Member",
            style = MaterialTheme.typography.labelSmall,
            color = if (isAdmin) Color.White else PremiumNavy
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
        title = { Text("Add New Member", color = PremiumNavy) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SoftOffWhite,
                        unfocusedContainerColor = SoftOffWhite
                    )
                )
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("Nickname (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SoftOffWhite,
                        unfocusedContainerColor = SoftOffWhite
                    )
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = phone.isNotBlank() && phone.length < 10,
                    supportingText = {
                        if (phone.isNotBlank() && phone.length < 10) {
                            Text("Valid phone number required")
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SoftOffWhite,
                        unfocusedContainerColor = SoftOffWhite
                    )
                )
                Text(
                    "* Member must have registered account with this phone number",
                    style = MaterialTheme.typography.labelSmall,
                    color = PremiumNavy.copy(alpha = 0.7f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Administrator",
                        modifier = Modifier.weight(1f),
                        color = PremiumNavy
                    )
                    Switch(
                        checked = isAdmin,
                        onCheckedChange = { isAdmin = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = VibrantOrange,
                            checkedTrackColor = VibrantOrange.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newMember = Member(
                        memberId = "",
                        name = name.trim(),
                        nickname = nickname.takeIf { it.isNotBlank() },
                        phoneNumber = phone.trim(),
                        isAdmin = isAdmin,
                        groupId = groupId
                    )
                    onAddMember(newMember)
                },
                enabled = name.isNotBlank() && phone.length >= 10,
                colors = ButtonDefaults.buttonColors(containerColor = VibrantOrange)
            ) {
                Text("Add Member", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = PremiumNavy)
            }
        }
    )
}