package com.example.chamabuddy.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.chamabuddy.R
import com.example.chamabuddy.domain.model.Member
import com.example.chamabuddy.presentation.viewmodel.MemberEvent
import com.example.chamabuddy.presentation.viewmodel.MemberState
import com.example.chamabuddy.presentation.viewmodel.MemberViewModel

val PremiumTeal = Color(0xFF00C9B1)
val PremiumCoral = Color(0xFFFF6B6B)
val DarkNavy = Color(0xFF0A1D3A)

val AppBarGradient = Brush.verticalGradient(
    colors = listOf(PremiumNavy, DarkNavy),
    startY = 0f,
    endY = Float.POSITIVE_INFINITY
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersScreen(
    groupId: String,
    currentUserId: String,
    currentUserIsAdmin: Boolean,
    currentUserIsOwner: Boolean,
    navigateBack: () -> Unit,
    navigateToProfile: (String) -> Unit,
    viewModel: MemberViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var showActionsDialog by remember { mutableStateOf(false) }
    var selectedMember by remember { mutableStateOf<Member?>(null) }

    LaunchedEffect(groupId, currentUserId) {
        if (currentUserId.isNotEmpty()) {
            viewModel.loadCurrentUserRole(groupId, currentUserId)
        }
    }

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
                        text = "Group Members",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = navigateBack,
                        modifier = Modifier
                            .background(Color.Transparent)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                scrollBehavior = scrollBehavior,
                modifier = Modifier.background(AppBarGradient)
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = currentUserIsAdmin,
                enter = scaleIn(animationSpec = tween(300)),
                exit = scaleOut(animationSpec = tween(200))
            ) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = PremiumTeal,
                    modifier = Modifier
                        .zIndex(1f)
                        .shadow(8.dp, shape = CircleShape)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Member",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        containerColor = SoftOffWhite
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SoftOffWhite)
        ) {
            when (state) {
                MemberState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = PremiumTeal,
                        strokeWidth = 3.dp
                    )
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
                        onItemClick = navigateToProfile,
                        onLongPress = { member ->
                            selectedMember = member
                            showActionsDialog = true
                        }
                    )

                    if (showActionsDialog && selectedMember != null) {
                        MemberActionsDialog(
                            member = selectedMember!!,
                            currentUserId = currentUserId,
                            currentUserIsAdmin = currentUserIsAdmin,
                            currentUserIsOwner = currentUserIsOwner,
                            onDismiss = { showActionsDialog = false },
                            onDelete = {
                                viewModel.handleEvent(MemberEvent.DeleteMember(selectedMember!!))
                                showActionsDialog = false
                            },
                            onPromote = {
                                viewModel.handleEvent(MemberEvent.PromoteToAdmin(selectedMember!!.memberId))
                                showActionsDialog = false
                            },
                            onDemote = {
                                viewModel.handleEvent(MemberEvent.DemoteToMember(selectedMember!!.memberId))
                                showActionsDialog = false
                            },
                            onDeactivate = {
                                viewModel.handleEvent(MemberEvent.DeactivateMember(selectedMember!!.memberId))
                                showActionsDialog = false
                            },
                            onReactivate = {
                                viewModel.handleEvent(MemberEvent.ReactivateMember(selectedMember!!.memberId))
                                showActionsDialog = false
                            }
                        )
                    }
                }
                is MemberState.Empty -> {
                    EmptyState(message = (state as MemberState.Empty).message)
                }
                else -> Unit
            }
        }

        AnimatedVisibility(
            visible = showAddDialog,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
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
    onItemClick: (String) -> Unit,
    onLongPress: (Member) -> Unit
) {
    LazyColumn(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val visibleMembers = members.filter { !it.isDeleted }
        items(visibleMembers) { member ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                MemberListItem(
                    member = member,
                    onClick = { onItemClick(member.memberId) },
                    onLongPress = { onLongPress(member) }
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
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.PersonOff,
            contentDescription = "Error",
            tint = PremiumCoral,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            color = PremiumNavy,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = PremiumTeal,
                contentColor = Color.White
            ),
            modifier = Modifier
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            Text("Retry", fontSize = 16.sp)
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Person,
            contentDescription = "Empty",
            tint = PremiumTeal,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = message,
            color = PremiumNavy,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Add a new member to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = PremiumNavy.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MemberListItem(
    member: Member,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = PremiumNavy.copy(alpha = 0.1f),
                spotColor = PremiumNavy.copy(alpha = 0.2f)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(16.dp),
        color = CardSurface,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InitialsAvatar(name = member.name, isActive = member.isActive)

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = PremiumNavy,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = member.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PremiumNavy.copy(alpha = 0.7f)
                )
            }

            RoleBadge(isAdmin = member.isAdmin, isActive = member.isActive)
        }
    }
}

@Composable
private fun InitialsAvatar(name: String, isActive: Boolean) {
    val bgColor = if (isActive) PremiumTeal else Color.LightGray
    val contentColor = if (isActive) Color.White else Color.DarkGray

    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.take(1).uppercase(),
            style = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            ),
            color = contentColor
        )
    }
}

@Composable
private fun RoleBadge(isAdmin: Boolean, isActive: Boolean) {
    val bgColor = when {
        !isActive -> Color.Gray
        isAdmin -> PremiumGold
        else -> LightAccentBlue
    }

    val text = when {
        !isActive -> "Inactive"
        isAdmin -> "Admin"
        else -> "Member"
    }

    val textColor = when {
        !isActive -> Color.White
        isAdmin -> PremiumNavy
        else -> PremiumNavy
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontWeight = FontWeight.SemiBold
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
    var expanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = CardSurface,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Add New Member",
                    style = MaterialTheme.typography.headlineSmall,
                    color = PremiumNavy,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SoftOffWhite,
                        unfocusedContainerColor = SoftOffWhite,
                        focusedIndicatorColor = PremiumTeal,
                        focusedLabelColor = PremiumTeal
                    ),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = "Name")
                    }
                )

                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("Popular Name (Optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SoftOffWhite,
                        unfocusedContainerColor = SoftOffWhite,
                        focusedIndicatorColor = PremiumTeal,
                        focusedLabelColor = PremiumTeal
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
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
                        unfocusedContainerColor = SoftOffWhite,
                        focusedIndicatorColor = PremiumTeal,
                        focusedLabelColor = PremiumTeal,
                        errorIndicatorColor = PremiumCoral
                    ),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(Icons.Default.Phone, contentDescription = "Phone")
                    }
                )

                Text(
                    "* Member must have registered account with this phone number",
                    style = MaterialTheme.typography.labelSmall,
                    color = PremiumNavy.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = SoftOffWhite
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isAdmin) Icons.Default.Star else Icons.Default.StarOutline,
                            contentDescription = "Admin",
                            tint = if (isAdmin) PremiumGold else PremiumNavy.copy(0.6f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Administrator Role",
                            color = PremiumNavy,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = isAdmin,
                            onCheckedChange = { isAdmin = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PremiumTeal,
                                checkedTrackColor = PremiumTeal.copy(alpha = 0.5f),
                                uncheckedThumbColor = PremiumNavy.copy(0.4f),
                                uncheckedTrackColor = PremiumNavy.copy(0.2f)
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Cancel", color = PremiumNavy)
                    }
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PremiumTeal,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Add Member", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun MemberActionsDialog(
    member: Member,
    currentUserId: String,
    currentUserIsAdmin: Boolean,
    currentUserIsOwner: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onPromote: () -> Unit,
    onDemote: () -> Unit,
    onDeactivate: () -> Unit,
    onReactivate: () -> Unit
) {
    val isCurrentUser = member.userId == currentUserId
    var expanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = CardSurface,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Member Actions",
                    style = MaterialTheme.typography.headlineSmall,
                    color = PremiumNavy,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = member.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PremiumNavy,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = member.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PremiumNavy.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (isCurrentUser) {
                    Text(
                        "⚠️ You can't modify your own status",
                        color = PremiumCoral,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                if (currentUserIsAdmin && !isCurrentUser) {
                    ActionButton(
                        text = if (member.isAdmin) "Demote to Member" else "Promote to Admin",
                        icon = if (member.isAdmin) Icons.Default.StarOutline else Icons.Default.Star,
                        onClick = {
                            if (member.isAdmin) onDemote() else onPromote()
                            onDismiss()
                        },
                        tint = PremiumGold
                    )
                }

                if (currentUserIsAdmin && !isCurrentUser) {
                    ActionButton(
                        text = if (member.isActive) "Deactivate Member" else "Reactivate Member",
                        icon = if (member.isActive) Icons.Default.PersonOff else Icons.Default.Person,
                        onClick = {
                            if (member.isActive) onDeactivate() else onReactivate()
                            onDismiss()
                        },
                        tint = if (member.isActive) PremiumCoral else PremiumTeal
                    )
                }

                if (currentUserIsOwner || (currentUserIsAdmin && !member.isOwner)) {
                    ActionButton(
                        text = "Delete Member",
                        icon = Icons.Default.Delete,
                        onClick = {
                            onDelete()
                            onDismiss()
                        },
                        tint = PremiumCoral,
                        enabled = !isCurrentUser
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PremiumNavy.copy(alpha = 0.1f),
                        contentColor = PremiumNavy
                    )
                ) {
                    Text("Cancel", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    tint: Color,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(bottom = 8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = tint.copy(alpha = 0.1f),
            contentColor = tint
        ),
        enabled = enabled
    ) {
        Icon(
            icon,
            contentDescription = text,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, fontWeight = FontWeight.Medium)
    }
}