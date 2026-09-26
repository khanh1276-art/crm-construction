package com.example.crmxaydung.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crmxaydung.data.ApiClient
import com.example.crmxaydung.data.UserSession
import kotlinx.coroutines.launch

data class DemoUser(val username: String, val name: String, val sbu: String, val role: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (UserSession) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var serverUrl by remember { mutableStateOf(ApiClient.baseUrl) }
    var username by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("123456") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val demoUsers = listOf(
        DemoUser("admin", "Chủ Tịch & TGĐ", "ALL", "Ban Lãnh Đạo"),
        DemoUser("gdkd_sbu1", "KS. Đỗ Hoàng Long", "SBU1", "Nền móng & Hầm"),
        DemoUser("gdkd_sbu2", "ThS. Nguyễn Quốc Thái", "SBU2", "Năng lượng"),
        DemoUser("gdkd_sbu3", "KS. Vũ Trọng Khôi", "SBU3", "Metro đô thị"),
        DemoUser("gdkd_sbu4", "ThS. Lê Thành Trung", "SBU4", "ĐS cao tốc"),
        DemoUser("gdkd_sbu5", "KS. Trần Đình Bách", "SBU5", "Cảng biển"),
        DemoUser("ctv_hanoi", "CTV Nguyễn Văn Hùng", "SBU1", "Cộng tác viên")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Header Card / Logo
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🏗️ CRM TẬP ĐOÀN XÂY DỰNG",
                    color = Color(0xFFF8FAFC),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Quản Trị Khách Hàng Ban Lãnh Đạo & 5 Khối SBU",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Server URL Configuration
        OutlinedTextField(
            value = serverUrl,
            onValueChange = {
                serverUrl = it
                ApiClient.baseUrl = it
            },
            label = { Text("Địa chỉ Máy chủ CRM (API Base URL)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.LightGray,
                focusedBorderColor = Color(0xFF38BDF8),
                unfocusedBorderColor = Color(0xFF475569)
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Username
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Tên đăng nhập") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.LightGray,
                focusedBorderColor = Color(0xFF38BDF8),
                unfocusedBorderColor = Color(0xFF475569)
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Password
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Mật khẩu") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                TextButton(onClick = { passwordVisible = !passwordVisible }) {
                    Text(if (passwordVisible) "Ẩn" else "Hiện", color = Color(0xFF38BDF8))
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.LightGray,
                focusedBorderColor = Color(0xFF38BDF8),
                unfocusedBorderColor = Color(0xFF475569)
            )
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = errorMessage ?: "",
                    color = Color(0xFFFECACA),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Login Button
        Button(
            onClick = {
                errorMessage = null
                isLoading = true
                coroutineScope.launch {
                    val result = ApiClient.login(username.trim(), password)
                    isLoading = false
                    result.onSuccess { session ->
                        onLoginSuccess(session)
                    }.onFailure { err ->
                        errorMessage = err.message ?: "Đăng nhập thất bại"
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
            enabled = !isLoading && username.isNotBlank() && password.isNotBlank()
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text(
                    text = "ĐĂNG NHẬP HỆ THỐNG",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Demo User Quick Selector
        Text(
            text = "⚡ Chọn nhanh tài khoản kiểm thử:",
            color = Color(0xFF94A3B8),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(demoUsers) { user ->
                OutlinedButton(
                    onClick = {
                        username = user.username
                        password = "123456"
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (username == user.username) Color(0xFF1E3A8A) else Color(0xFF1E293B)
                    )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(user.username, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(user.sbu, color = Color(0xFF93C5FD), fontSize = 10.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
