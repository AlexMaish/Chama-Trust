package com.example.chamabuddy.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.chamabuddy.R
import com.example.chamabuddy.domain.model.Group
import com.example.chamabuddy.domain.model.User
import com.example.chamabuddy.presentation.navigation.GroupsHomeDestination
import com.example.chamabuddy.presentation.viewmodel.AuthViewModel
import com.example.chamabuddy.presentation.viewmodel.GroupHomeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsHomeScreen(
    navigateToGroupCycles: (String) -> Unit,
    viewModel: GroupHomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var groupName by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val showDialog by remember(uiState.showCreateGroupDialog) {
        derivedStateOf { uiState.showCreateGroupDialog }
    }
    val authViewModel: AuthViewModel = hiltViewModel()
    val currentUser by authViewModel.currentUser.collectAsState()
    // Drawer state
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackbar()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Box(modifier = Modifier.width(280.dp)) {
                GroupsHomeDrawerContent(
                    currentUser = currentUser, // Pass current user here
                    onClose = { scope.launch { drawerState.close() } },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                LargeTopAppBar(
                    title = {
                        Text(
                            GroupsHomeDestination.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Navigation Menu",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = PremiumNavy,
                        scrolledContainerColor = PremiumNavy,
                        titleContentColor = Color.White
                    ),
                    scrollBehavior = scrollBehavior
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { viewModel.showCreateGroupDialog() },
                    containerColor = VibrantOrange
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create new group", tint = Color.White)
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = SoftOffWhite
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                // Background curve
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(PremiumNavy, PremiumNavy.copy(alpha = 0.8f)),
                                startY = 0f,
                                endY = 500f
                            ),
                            shape = RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp)
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(1.dp))

                    // Greeting section
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow))
                    ) {
                        Column {
                            Text(
                                "Your Groups",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(top = 8.dp)
                            )

                            Text(
                                "Manage all your chama groups",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    when {
                        uiState.isLoading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = VibrantOrange)
                            }
                        }
                        uiState.groups.isEmpty() -> {
                            EmptyGroupsPlaceholder(
                                modifier = Modifier.fillMaxSize(),
                                onCreateClick = { viewModel.showCreateGroupDialog() }
                            )
                        }
                        else -> {
                            Surface(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                                    .background(Color.White),
                                color = Color.White
                            ) {
                                GroupList(
                                    groups = uiState.groups,
                                    onGroupClick = { group ->
                                        navigateToGroupCycles(group.groupId)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Group Dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideCreateGroupDialog() },
            title = { Text("Create New Group", color = PremiumNavy) },
            text = {
                Column {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Group Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = uiState.nameValidationError != null,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = SoftOffWhite,
                            unfocusedContainerColor = SoftOffWhite
                        ),
                        shape = RoundedCornerShape(12.dp)
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
                    enabled = groupName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = VibrantOrange)
                ) {
                    Text("Create", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.hideCreateGroupDialog() }
                ) {
                    Text("Cancel", color = PremiumNavy)
                }
            }
        )
    }
}

@Composable
fun GroupsHomeDrawerContent(
    currentUser: User?,  // Add currentUser parameter
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PremiumNavy)
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.ic_profile_placeholder),
                contentDescription = "Profile",
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color.White, CircleShape)
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (currentUser == null) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.CenterHorizontally),
                    color = Color.White
                )
            } else {
                // Display user info as shown above
                Text(
                    currentUser.username,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Text(
                    currentUser.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        Divider(color = Color.White.copy(alpha = 0.2f), thickness = 1.dp)

        Spacer(modifier = Modifier.height(24.dp))

        // Drawer items
        DrawerItem(
            icon = Icons.Default.Settings,
            text = "Settings",
            onClick = { /* Handle settings */ }
        )
        DrawerItem(
            icon = Icons.Default.Help,
            text = "Help & Support",
            onClick = { /* Handle help */ }
        )
        DrawerItem(
            icon = Icons.Default.Info,
            text = "About",
            onClick = { /* Handle about */ }
        )
        DrawerItem(
            icon = Icons.Default.ExitToApp,
            text = "Logout",
            onClick = { /* Handle logout */ }
        )

        Spacer(modifier = Modifier.weight(1f))

        // App version
        Text(
            "Chama Buddy v1.0.0",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun DrawerItem(icon: ImageVector, text: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = text,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                color = Color.White,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun EmptyGroupsPlaceholder(modifier: Modifier = Modifier, onCreateClick: () -> Unit) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_empty_state),
            contentDescription = "No groups",
            modifier = Modifier.size(150.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No Groups Yet",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = PremiumNavy
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Create your first group to get started with Chama Buddy",
            style = MaterialTheme.typography.bodyMedium,
            color = PremiumNavy.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onCreateClick,
            colors = ButtonDefaults.buttonColors(containerColor = VibrantOrange),
            modifier = Modifier.height(50.dp)
        ) {
            Text("Create Group", fontWeight = FontWeight.Bold)
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
            PremiumGroupCard(
                group = group,
                onClick = { onGroupClick(group) }
            )
        }
    }
}

@Composable
fun PremiumGroupCard(group: Group, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = LightAccentBlue,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = group.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = PremiumNavy,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PremiumNavy
                )
                // FIXED: Use adminName instead of memberCount
                Text(
                    text = "Admin: ${group.adminName ?: "Unknown"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PremiumNavy.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "View group",
                tint = PremiumNavy.copy(alpha = 0.7f)
            )
        }
    }
}

