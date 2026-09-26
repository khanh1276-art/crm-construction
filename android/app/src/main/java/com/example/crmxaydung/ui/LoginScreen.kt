package com.example.crmxaydung.ui

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crmxaydung.R
import com.example.crmxaydung.data.ApiClient
import com.example.crmxaydung.data.UserSession
import kotlinx.coroutines.launch

data class DemoUser(val username: String, val name: String, val sbu: String, val role: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (UserSession) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("crm_prefs", Context.MODE_PRIVATE) }
    val coroutineScope = rememberCoroutineScope()

    val defaultRenderUrl = "https://crm-construction-6lrg.onrender.com"
    var serverUrl by remember {
        mutableStateOf(prefs.getString("server_url", defaultRenderUrl) ?: defaultRenderUrl)
    }

    LaunchedEffect(serverUrl) {
        ApiClient.baseUrl = serverUrl
    }

    var username by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("123456") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDemoHelper by remember { mutableStateOf(false) }
    var showServerSettings by remember { mutableStateOf(false) }
    var isCheckingHealth by remember { mutableStateOf(false) }
    var healthMessage by remember { mutableStateOf<String?>(null) }
    var healthSuccess by remember { mutableStateOf(false) }

    val deepNavyBlue = Color(0xFF0A3583)
    val feconOrange = Color(0xFFEA5713)

    val demoUsers = listOf(
        DemoUser("admin", "Chủ Tịch & TGĐ", "ALL", "Ban Lãnh Đạo"),
        DemoUser("gdkd_sbu1", "KS. Đỗ Hoàng Long", "SBU1", "Nền móng & Hầm"),
        DemoUser("gdkd_sbu2", "ThS. Nguyễn Quốc Thái", "SBU2", "Năng lượng"),
        DemoUser("gdkd_sbu3", "KS. Vũ Trọng Khôi", "SBU3", "Metro đô thị"),
        DemoUser("gdkd_sbu4", "ThS. Lê Thành Trung", "SBU4", "ĐS cao tốc"),
        DemoUser("gdkd_sbu5", "KS. Trần Đình Bách", "SBU5", "Cảng biển"),
        DemoUser("ctv_hanoi", "CTV Nguyễn Văn Hùng", "SBU1", "Cộng tác viên")
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(deepNavyBlue)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Top Hero Banner (Construction site matching web mockup)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.login_hero_building),
                    contentDescription = "FECON Project Hero",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Gradient overlay blending into deep navy
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, deepNavyBlue.copy(alpha = 0.5f), deepNavyBlue)
                            )
                        )
                )
            }

            // White Login Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // FECON CRM Logo at Top
                        Image(
                            painter = painterResource(id = R.drawable.fecon_crm_logo),
                            contentDescription = "FECON CRM",
                            modifier = Modifier
                                .height(56.dp)
                                .padding(vertical = 4.dp),
                            contentScale = ContentScale.Fit
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Username/Email Field
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Username/Email",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                placeholder = { Text("Username/Email", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF0F172A),
                                    unfocusedTextColor = Color(0xFF0F172A),
                                    focusedBorderColor = Color(0xFF2563EB),
                                    unfocusedBorderColor = Color(0xFFCBD5E1),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Password Field with Eye Toggle
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Password",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                placeholder = { Text("Password", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Text(if (passwordVisible) "👁️" else "🔒", fontSize = 14.sp)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF0F172A),
                                    unfocusedTextColor = Color(0xFF0F172A),
                                    focusedBorderColor = Color(0xFF2563EB),
                                    unfocusedBorderColor = Color(0xFFCBD5E1),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Remember Me & Forgot Password Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { rememberMe = !rememberMe }
                            ) {
                                Checkbox(
                                    checked = rememberMe,
                                    onCheckedChange = { rememberMe = it },
                                    colors = CheckboxDefaults.colors(checkedColor = feconOrange)
                                )
                                Text("Remember Me", fontSize = 12.sp, color = Color(0xFF475569))
                            }

                            Text(
                                text = "Forgot Password",
                                fontSize = 12.sp,
                                color = Color(0xFF1D4ED8),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable {
                                    errorMessage = "Mật khẩu mặc định hệ thống cấp: 123456"
                                }
                            )
                        }

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = Color(0xFFFEF2F2),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = errorMessage!!,
                                    color = Color(0xFFDC2626),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Submit Button (Matching Webapp #EA5713 Orange)
                        Button(
                            onClick = {
                                errorMessage = null
                                isLoading = true
                                coroutineScope.launch {
                                    ApiClient.baseUrl = serverUrl
                                    prefs.edit().putString("server_url", serverUrl).apply()
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
                                .height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = feconOrange),
                            enabled = !isLoading && username.isNotBlank() && password.isNotBlank()
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            } else {
                                Text(
                                    text = "Đăng nhập",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick Demo Accounts Toggle
                        TextButton(
                            onClick = { showDemoHelper = !showDemoHelper },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (showDemoHelper) "Ẩn danh mục tài khoản mẫu ▲" else "ℹ️ Danh mục tài khoản mẫu (Demo) ▼",
                                color = Color(0xFF2563EB),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (showDemoHelper) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("Mật khẩu: 123456 (Chạm để tự điền):", fontSize = 10.sp, color = Color.Gray)
                                demoUsers.forEach { u ->
                                    Surface(
                                        color = Color.White,
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                username = u.username
                                                password = "123456"
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(u.username, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = feconOrange)
                                            Text("${u.name} • ${u.role}", fontSize = 10.sp, color = Color.DarkGray)
                                        }
                                    }
                                }
                            }
                        }

                        // Server Settings Toggle (Compact)
                        TextButton(
                            onClick = { showServerSettings = !showServerSettings }
                        ) {
                            Text(
                                text = "⚙️ Cài đặt kết nối máy chủ",
                                color = Color(0xFF64748B),
                                fontSize = 10.sp
                            )
                        }

                        if (showServerSettings) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = serverUrl,
                                    onValueChange = {
                                        serverUrl = it
                                        ApiClient.baseUrl = it
                                        prefs.edit().putString("server_url", it).apply()
                                    },
                                    label = { Text("Server URL", fontSize = 10.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    TextButton(onClick = {
                                        serverUrl = defaultRenderUrl
                                        ApiClient.baseUrl = defaultRenderUrl
                                        prefs.edit().putString("server_url", defaultRenderUrl).apply()
                                    }) {
                                        Text("Đặt lại Render", fontSize = 10.sp, color = feconOrange)
                                    }

                                    TextButton(onClick = {
                                        isCheckingHealth = true
                                        healthMessage = null
                                        coroutineScope.launch {
                                            ApiClient.baseUrl = serverUrl
                                            val res = ApiClient.checkHealth()
                                            isCheckingHealth = false
                                            if (res.isSuccess) {
                                                healthSuccess = true
                                                healthMessage = "Máy chủ hoạt động tốt!"
                                            } else {
                                                healthSuccess = false
                                                healthMessage = "Không kết nối được: ${res.exceptionOrNull()?.message}"
                                            }
                                        }
                                    }) {
                                        Text(if (isCheckingHealth) "Đang kiểm tra..." else "Kiểm tra kết nối", fontSize = 10.sp, color = Color(0xFF2563EB))
                                    }
                                }

                                healthMessage?.let {
                                    Text(
                                        text = it,
                                        fontSize = 10.sp,
                                        color = if (healthSuccess) Color(0xFF16A34A) else Color(0xFFDC2626)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Footer Watermark (Matching mock-up)
                Text(
                    text = "FECON CRM • Executive Portal",
                    color = Color(0x99FFFFFF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
