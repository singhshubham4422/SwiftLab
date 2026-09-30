package com.example.swiftlab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.swiftlab.data.local.SessionManager
import com.example.swiftlab.data.remote.RetrofitClient
import com.example.swiftlab.data.remote.dto.ModeSwitchRequest
import com.example.swiftlab.data.repository.AppRepository
import com.example.swiftlab.sync.SyncManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: AppRepository,
    sessionManager: SessionManager,
    syncManager: SyncManager,
    retrofitClient: RetrofitClient,
    onLogout: () -> Unit
) {
    var currentMode by remember { mutableStateOf(sessionManager.dataMode) }
    var serverUrl by remember { mutableStateOf(sessionManager.baseUrl) }
    var showConfirmCloudDialog by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }
    var syncFeedback by remember { mutableStateOf<String?>(null) }

    val pendingCount by repository.pendingSyncCount.collectAsState(initial = 0)
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Synchronization", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (syncFeedback != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = syncFeedback!!,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 13.sp
                    )
                }
            }

            // 1. Data Mode Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Data Mode", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Surface(
                            color = if (currentMode == SessionManager.MODE_LOCAL_ONLY) Color(0xFF10B981) else Color(0xFF2563EB),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = currentMode,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = if (currentMode == SessionManager.MODE_LOCAL_ONLY)
                            "LOCAL_ONLY: All data stays on this device. Cloud uploads and sync are completely disabled. Internet connectivity will NEVER upload data."
                        else
                            "CLOUD_SYNC: Operations are queued locally and automatically synchronized with the Spring Boot & Supabase backend.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (currentMode != SessionManager.MODE_LOCAL_ONLY) {
                                    sessionManager.dataMode = SessionManager.MODE_LOCAL_ONLY
                                    currentMode = SessionManager.MODE_LOCAL_ONLY
                                    syncManager.cancelAllSync()
                                    syncFeedback = "Switched to LOCAL_ONLY mode. All background syncing stopped."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentMode == SessionManager.MODE_LOCAL_ONLY) Color(0xFF10B981) else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (currentMode == SessionManager.MODE_LOCAL_ONLY) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("LOCAL_ONLY")
                        }

                        Button(
                            onClick = {
                                if (currentMode != SessionManager.MODE_CLOUD_SYNC) {
                                    showConfirmCloudDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentMode == SessionManager.MODE_CLOUD_SYNC) Color(0xFF2563EB) else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (currentMode == SessionManager.MODE_CLOUD_SYNC) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("CLOUD_SYNC")
                        }
                    }
                }
            }

            // 2. Cloud Server & Sync Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Cloud Sync Engine", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        label = { Text("Server Base URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            sessionManager.baseUrl = serverUrl.trim()
                            syncFeedback = "Server URL saved: ${sessionManager.baseUrl}"
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Server URL")
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pending Items in Queue:", fontSize = 14.sp)
                        Text(
                            text = if (sessionManager.isLocalOnly) "0 (Disabled)" else "$pendingCount",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (pendingCount > 0) Color(0xFFF59E0B) else Color(0xFF10B981)
                        )
                    }

                    Button(
                        onClick = {
                            if (sessionManager.isLocalOnly) {
                                syncFeedback = "Cannot sync while in LOCAL_ONLY mode. Enable CLOUD_SYNC first."
                            } else {
                                isSyncing = true
                                coroutineScope.launch {
                                    val pushOk = repository.syncPendingQueue()
                                    val pullOk = repository.pullRemoteData()
                                    isSyncing = false
                                    syncFeedback = if (pushOk && pullOk) "Sync complete! Local queue pushed and cloud changes pulled." else "Sync completed with warnings. Check server connectivity."
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSyncing && !sessionManager.isLocalOnly
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Syncing...")
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Trigger Manual Sync Now")
                        }
                    }
                }
            }

            // 3. Device & Identity Information
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Device & Organization", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Organization: ${sessionManager.orgName}", fontSize = 14.sp)
                    Text("Logged in: ${sessionManager.userName} (${sessionManager.userEmail})", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Role: ${sessionManager.role}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Device ID: ${sessionManager.deviceId}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                }
            }

            // Logout Button
            OutlinedButton(
                onClick = {
                    sessionManager.logout()
                    onLogout()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out")
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showConfirmCloudDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmCloudDialog = false },
            title = { Text("Confirm Switch to CLOUD_SYNC") },
            text = {
                Text(
                    "Are you sure you want to enable Cloud Sync?\n\n" +
                    "When Cloud Sync is enabled, pending transactions will be synchronized with the remote Spring Boot backend and Supabase database."
                )
            },
            confirmButton = {
                Button(onClick = {
                    sessionManager.dataMode = SessionManager.MODE_CLOUD_SYNC
                    currentMode = SessionManager.MODE_CLOUD_SYNC
                    syncManager.schedulePeriodicSync()
                    showConfirmCloudDialog = false
                    syncFeedback = "Cloud Sync enabled. Background synchronization scheduled."
                }) {
                    Text("Enable Cloud Sync")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmCloudDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
