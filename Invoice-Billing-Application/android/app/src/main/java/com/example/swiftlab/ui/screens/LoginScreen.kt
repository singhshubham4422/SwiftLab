package com.example.swiftlab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.swiftlab.data.local.SessionManager
import com.example.swiftlab.data.remote.RetrofitClient
import com.example.swiftlab.data.remote.dto.AuthRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    sessionManager: SessionManager,
    retrofitClient: RetrofitClient,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("admin@swiftlab.local") }
    var password by remember { mutableStateOf("admin123") }
    var serverUrl by remember { mutableStateOf(sessionManager.baseUrl) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Logo / Title
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "SL",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Text(
                text = "SwiftLab Platform",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Inventory, POS & Billing Suite",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Error message
            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 14.sp
                    )
                }
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                leadingIcon = { Icon(Icons.Default.MailOutline, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    isLoading = true
                    errorMessage = null
                    coroutineScope.launch {
                        try {
                            val api = retrofitClient.getService()
                            val response = api.login(AuthRequest(email.trim(), password.trim()))
                            if (response.isSuccessful && response.body() != null) {
                                val body = response.body()!!
                                sessionManager.saveLogin(
                                    token = body.token,
                                    userId = body.user.id,
                                    email = body.user.email,
                                    name = body.user.fullName,
                                    role = body.user.role,
                                    orgId = body.organization?.id ?: 1L,
                                    orgName = body.organization?.name ?: "Default Org"
                                )
                                sessionManager.dataMode = SessionManager.MODE_CLOUD_SYNC
                                onLoginSuccess()
                            } else {
                                errorMessage = "Authentication failed: ${response.message()}"
                            }
                        } catch (e: Exception) {
                            errorMessage = "Network error: ${e.localizedMessage ?: "Unable to connect to server"}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Sign In with Cloud", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            OutlinedButton(
                onClick = {
                    // Enter strictly in Local-Only offline mode
                    sessionManager.dataMode = SessionManager.MODE_LOCAL_ONLY
                    sessionManager.userEmail = email.ifBlank { "offline@swiftlab.local" }
                    sessionManager.userName = "Offline Local User"
                    onLoginSuccess()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Continue Offline (LOCAL_ONLY)", fontSize = 15.sp)
            }

            TextButton(
                onClick = { showUrlDialog = true }
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Server: ${sessionManager.baseUrl}")
            }
        }
    }

    if (showUrlDialog) {
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            title = { Text("Configure API Server URL") },
            text = {
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text("Base URL") },
                    placeholder = { Text("http://10.0.2.2:8080/ or https://...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    sessionManager.baseUrl = serverUrl.trim()
                    showUrlDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUrlDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
