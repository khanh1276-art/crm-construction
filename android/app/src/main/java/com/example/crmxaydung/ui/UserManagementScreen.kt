package com.example.crmxaydung.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crmxaydung.data.ApiClient
import com.example.crmxaydung.data.UserItem
import com.example.crmxaydung.data.UserSession
import kotlinx.coroutines.launch

@Composable
fun UserManagementScreen(currentUser: UserSession) {
    val coroutineScope = rememberCoroutineScope()
    var users by remember { mutableStateOf<List<UserItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var userToDelete by remember { mutableStateOf<UserItem?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadUsers() {
        isLoading = true
        coroutineScope.launch {
            val res = ApiClient.fetchUsers()
            users = res.getOrNull() ?: emptyList()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadUsers()
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC), // Nền sáng
        floatingActionButton = {
            if (currentUser.role == "ADMIN") {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = Color(0xFFEA580C),
                    contentColor = Color.White
                ) {
                    Text("➕", fontSize = 18.sp)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "👥 Quản Lý Tài Khoản Hệ Thống",
                        color = Color(0xFF0F172A),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Phân quyền Ban Lãnh Đạo, 5 SBU & CTV",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp
                    )
                }
                TextButton(onClick = { loadUsers() }) {
                    Text("Làm mới", color = Color(0xFFEA580C), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFEA580C))
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(users) { u ->
                        UserCard(
                            user = u,
                            canDelete = (currentUser.role == "ADMIN" && u.username != "admin" && u.id != currentUser.id),
                            onDelete = { userToDelete = u }
                        )
                    }
                }
            }
        }
    }

    // Add User Dialog
    if (showAddDialog) {
        AddUserDialog(
            onDismiss = { showAddDialog = false },
            onAdded = {
                showAddDialog = false
                loadUsers()
            }
        )
    }

    // Delete Confirmation Dialog
    userToDelete?.let { u ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("Xác nhận xóa tài khoản", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Anh có chắc chắn muốn xóa tài khoản \"${u.fullName}\" (@${u.username}) không?",
                    color = Color(0xFF334155)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val res = ApiClient.deleteUser(u.id)
                            userToDelete = null
                            if (res.isSuccess) {
                                loadUsers()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Xóa Ngay", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) { Text("Hủy", color = Color(0xFF64748B)) }
            },
            containerColor = Color.White
        )
    }
}

@Composable
fun UserCard(user: UserItem, canDelete: Boolean, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.fullName,
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "@${user.username} • ${user.title}",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp
                    )
                }

                Surface(
                    color = when (user.role) {
                        "ADMIN" -> Color(0xFFEA580C)
                        "SBU_DIRECTOR" -> Color(0xFF0284C7)
                        else -> Color(0xFF059669)
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = when (user.role) {
                            "ADMIN" -> "👑 Lãnh Đạo"
                            "SBU_DIRECTOR" -> "🏢 GĐKD ${user.sbu}"
                            else -> "🤝 CTV"
                        },
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SĐT: ${user.phone.ifEmpty { "Chưa cập nhật" }}",
                    color = Color(0xFF0284C7),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )

                if (canDelete) {
                    TextButton(onClick = onDelete) {
                        Text("🗑️ Xóa", color = Color(0xFFDC2626), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddUserDialog(
    onDismiss: () -> Unit,
    onAdded: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("123456") }
    var fullName by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("SBU_DIRECTOR") }
    var sbu by remember { mutableStateOf("SBU1") }
    var title by remember { mutableStateOf("Giám Đốc Kinh Doanh") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var errText by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val roles = listOf("SBU_DIRECTOR" to "GĐKD SBU", "COLLABORATOR" to "Cộng Tác Viên", "ADMIN" to "Ban Lãnh Đạo")
    val sbus = listOf("SBU1", "SBU2", "SBU3", "SBU4", "SBU5", "ALL")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm Tài Khoản Mới", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Tên đăng nhập (username)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF334155),
                        focusedBorderColor = Color(0xFFEA580C),
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Mật khẩu") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF334155),
                        focusedBorderColor = Color(0xFFEA580C),
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Họ và tên") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF334155),
                        focusedBorderColor = Color(0xFFEA580C),
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Số điện thoại") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF334155),
                        focusedBorderColor = Color(0xFFEA580C),
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )

                Text("Khối SBU phụ trách:", color = Color(0xFF64748B), fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    sbus.take(3).forEach { s ->
                        val isSelected = (sbu == s)
                        FilterChip(
                            selected = isSelected,
                            onClick = { sbu = s },
                            label = { Text(s, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFEA580C),
                                selectedLabelColor = Color.White,
                                containerColor = Color.White,
                                labelColor = Color(0xFF475569)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) Color.Transparent else Color(0xFFCBD5E1)
                            )
                        )
                    }
                }

                errText?.let {
                    Text(it, color = Color(0xFFDC2626), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (username.isNotBlank() && fullName.isNotBlank()) {
                        isSaving = true
                        errText = null
                        scope.launch {
                            val res = ApiClient.createUser(
                                username.trim(), password.trim(), fullName.trim(),
                                role, sbu, title.trim(), email.trim(), phone.trim()
                            )
                            isSaving = false
                            if (res.isSuccess) {
                                onAdded()
                            } else {
                                errText = res.exceptionOrNull()?.message ?: "Lỗi tạo tài khoản"
                            }
                        }
                    }
                },
                enabled = !isSaving && username.isNotBlank() && fullName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C))
            ) {
                if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                else Text("Tạo Tài Khoản", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy", color = Color(0xFF64748B)) }
        },
        containerColor = Color.White
    )
}
