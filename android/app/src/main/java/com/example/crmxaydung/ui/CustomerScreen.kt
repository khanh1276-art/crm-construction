package com.example.crmxaydung.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crmxaydung.data.ApiClient
import com.example.crmxaydung.data.CustomerItem
import com.example.crmxaydung.data.UserSession
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerScreen(user: UserSession) {
    val coroutineScope = rememberCoroutineScope()
    var customers by remember { mutableStateOf<List<CustomerItem>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCustomer by remember { mutableStateOf<CustomerItem?>(null) }

    fun loadCustomers() {
        isLoading = true
        coroutineScope.launch {
            val res = ApiClient.fetchCustomers(if (user.role == "ADMIN") "ALL" else user.sbu)
            customers = res.getOrNull() ?: emptyList()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadCustomers()
    }

    val filteredList = customers.filter {
        searchQuery.isBlank() ||
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.code.contains(searchQuery, ignoreCase = true) ||
                it.keyDecisionMaker.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        containerColor = Color(0xFF0F172A),
        floatingActionButton = {
            // Collaborators and SBU Directors can add customers
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF2563EB),
                contentColor = Color.White
            ) {
                Text("➕", fontSize = 18.sp)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Tìm theo mã, tên DN, người đại diện...") },
                leadingIcon = { Text("🔍", fontSize = 14.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.LightGray,
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color(0xFF334155)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tổng: ${filteredList.size} Doanh nghiệp / CĐT",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                TextButton(onClick = { loadCustomers() }) {
                    Text("Làm mới", color = Color(0xFF38BDF8), fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF38BDF8))
                }
            } else if (filteredList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Không tìm thấy khách hàng phù hợp", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredList) { cust ->
                        CustomerCard(cust = cust, onClick = { selectedCustomer = cust })
                    }
                }
            }
        }
    }

    // Add Customer Dialog
    if (showAddDialog) {
        AddCustomerDialog(
            defaultSbu = if (user.role == "ADMIN") "SBU1" else user.sbu,
            onDismiss = { showAddDialog = false },
            onAdded = {
                showAddDialog = false
                loadCustomers()
            }
        )
    }

    // Customer Detail Dialog
    selectedCustomer?.let { cust ->
        AlertDialog(
            onDismissRequest = { selectedCustomer = null },
            title = { Text(cust.name, fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column {
                    Text("Mã khách hàng: ${cust.code}", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    Text("Phân hệ: ${cust.sbu} | Phân khúc: ${cust.tier}", color = Color(0xFF38BDF8), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Người quyết định: ${cust.keyDecisionMaker}", fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Chức danh: ${cust.decisionMakerRole}", color = Color.LightGray)
                    Text("Số điện thoại: ${cust.phone.ifEmpty { "Chưa cập nhật" }}", color = Color.LightGray)
                    Text("Email: ${cust.email.ifEmpty { "Chưa cập nhật" }}", color = Color.LightGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Điểm quan hệ: ${"⭐".repeat(cust.relationshipScore.coerceIn(1, 5))}", color = Color(0xFFFBBF24))
                    if (cust.strategicNotes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Ghi chú chiến lược:", fontWeight = FontWeight.Bold, color = Color(0xFFE2E8F0))
                        Text(cust.strategicNotes, color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedCustomer = null }) {
                    Text("Đóng", color = Color(0xFF38BDF8))
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}

@Composable
fun CustomerCard(cust: CustomerItem, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = cust.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = if (cust.tier == "STRATEGIC_VIP") Color(0xFF7C3AED) else Color(0xFF0284C7),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (cust.tier == "STRATEGIC_VIP") "VIP ⭐" else "Đối tác",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Đại diện: ${cust.keyDecisionMaker} (${cust.decisionMakerRole})",
                color = Color(0xFFCBD5E1),
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Mã: ${cust.code} | Khối: ${cust.sbu}", color = Color(0xFF64748B), fontSize = 11.sp)
                Text(cust.phone, color = Color(0xFF38BDF8), fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun AddCustomerDialog(
    defaultSbu: String,
    onDismiss: () -> Unit,
    onAdded: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var sbu by remember { mutableStateOf(defaultSbu) }
    var keyPerson by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm Khách Hàng / CĐT Mới", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên Doanh nghiệp / Chủ đầu tư") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                OutlinedTextField(
                    value = keyPerson,
                    onValueChange = { keyPerson = it },
                    label = { Text("Người quyết định (Chủ tịch / TGĐ)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Số điện thoại liên hệ") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        isSaving = true
                        scope.launch {
                            val res = ApiClient.createCustomer(name.trim(), sbu, "STRATEGIC_VIP", keyPerson.trim(), phone.trim())
                            isSaving = false
                            if (res.isSuccess) {
                                onAdded()
                            }
                        }
                    }
                },
                enabled = !isSaving && name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                else Text("Lưu Khách Hàng")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy", color = Color.Gray) }
        },
        containerColor = Color(0xFF1E293B)
    )
}
