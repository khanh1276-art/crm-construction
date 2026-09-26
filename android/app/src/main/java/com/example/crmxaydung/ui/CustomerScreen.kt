package com.example.crmxaydung.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crmxaydung.data.ApiClient
import com.example.crmxaydung.data.CustomerItem
import com.example.crmxaydung.data.UserSession
import kotlinx.coroutines.launch
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerScreen(user: UserSession) {
    val coroutineScope = rememberCoroutineScope()
    var customers by remember { mutableStateOf<List<CustomerItem>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCustomer by remember { mutableStateOf<CustomerItem?>(null) }
    var careCustomer by remember { mutableStateOf<CustomerItem?>(null) }

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
                it.keyDecisionMaker.contains(searchQuery, ignoreCase = true) ||
                it.phone.contains(searchQuery) ||
                it.decisionMakerPhone.contains(searchQuery)
    }

    val df = DecimalFormat("#,##0.0")

    Scaffold(
        containerColor = Color(0xFF0F172A),
        floatingActionButton = {
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
                    text = "Tổng: ${filteredList.size} Khách hàng / CĐT",
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
                        CustomerCard(
                            cust = cust,
                            onClick = { selectedCustomer = cust }
                        )
                    }
                }
            }
        }
    }

    // Customer Personal Profile Detail Dialog
    selectedCustomer?.let { cust ->
        CustomerDetailDialog(
            cust = cust,
            df = df,
            onDismiss = { selectedCustomer = null },
            onLogCare = {
                careCustomer = cust
            }
        )
    }

    // Log Care Activity Dialog
    careCustomer?.let { cust ->
        LogCareDialog(
            customer = cust,
            currentUser = user,
            onDismiss = { careCustomer = null },
            onLogged = {
                careCustomer = null
                loadCustomers()
            }
        )
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
                Text(cust.phone.ifEmpty { cust.decisionMakerPhone }, color = Color(0xFF38BDF8), fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("👉 Chạm để theo dõi chi tiết hồ sơ cá nhân", color = Color(0xFF64748B), fontSize = 10.sp)
        }
    }
}

@Composable
fun CustomerDetailDialog(
    cust: CustomerItem,
    df: DecimalFormat,
    onDismiss: () -> Unit,
    onLogCare: () -> Unit
) {
    val context = LocalContext.current
    val contactPhone = cust.decisionMakerPhone.ifEmpty { cust.phone }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(cust.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                Text("Mã KH: ${cust.code} | Khối: ${cust.sbu} | Cấp: ${cust.tier}", color = Color(0xFF38BDF8), fontSize = 12.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Section: Personal Decision Maker Card
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)), shape = RoundedCornerShape(10.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("👤 Hồ Sơ Cá Nhân Người Quyết Định", fontWeight = FontWeight.Bold, color = Color(0xFFE2E8F0), fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Họ và tên: ${cust.keyDecisionMaker}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        Text("Chức danh: ${cust.decisionMakerRole}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        if (contactPhone.isNotBlank()) {
                            Text("SĐT trực tiếp: $contactPhone", color = Color(0xFF38BDF8), fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$contactPhone"))
                                        context.startActivity(intent)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("📞 Gọi Điện", fontSize = 12.sp)
                                }
                                Button(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:$contactPhone"))
                                        context.startActivity(intent)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("💬 Nhắn SMS", fontSize = 12.sp)
                                }
                            }
                        }
                        if (cust.decisionMakerBirthday.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("🎂 Sinh nhật: ${cust.decisionMakerBirthday}", color = Color(0xFFFBBF24), fontSize = 12.sp)
                        }
                    }
                }

                // Section: Corporate & Diplomatic Details
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)), shape = RoundedCornerShape(10.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("🏢 Thông Tin Doanh Nghiệp & Ngoại Giao", fontWeight = FontWeight.Bold, color = Color(0xFFE2E8F0), fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        if (cust.headquarters.isNotBlank()) {
                            Text("Trụ sở: ${cust.headquarters}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        }
                        if (cust.taxCode.isNotBlank()) {
                            Text("Mã số thuế: ${cust.taxCode}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        }
                        if (cust.foundingAnniversary.isNotBlank()) {
                            Text("🏛️ Ngày thành lập: ${cust.foundingAnniversary}", color = Color(0xFF38BDF8), fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Điểm quan hệ: ${"⭐".repeat(cust.relationshipScore.coerceIn(1, 5))} (${cust.relationshipStatus})", color = Color(0xFFFBBF24), fontSize = 12.sp)
                    }
                }

                // Section: Projects & Strategic Notes
                if (cust.strategicNotes.isNotBlank()) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)), shape = RoundedCornerShape(10.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("📝 Ghi Chú Chiến Lược Cá Nhân", fontWeight = FontWeight.Bold, color = Color(0xFFE2E8F0), fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(cust.strategicNotes, color = Color(0xFF94A3B8), fontSize = 12.sp)
                        }
                    }
                }

                // Action to log VIP care
                Button(
                    onClick = onLogCare,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🤝 Ghi Nhận Chăm Sóc VIP")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Đóng", color = Color(0xFF38BDF8)) }
        },
        containerColor = Color(0xFF1E293B)
    )
}

@Composable
fun LogCareDialog(
    customer: CustomerItem,
    currentUser: UserSession,
    onDismiss: () -> Unit,
    onLogged: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var activityType by remember { mutableStateOf("EXECUTIVE_MEETING") }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val types = listOf(
        "EXECUTIVE_MEETING" to "Họp Cấp Cao",
        "DINNER_NETWORKING" to "Tiệc Ngoại Giao",
        "GIFT_DELIVERY" to "Tặng Quà",
        "CALL_DISCUSS" to "Điện Đàm"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ghi Nhật Ký Chăm Sóc VIP", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Khách hàng: ${customer.name}", color = Color(0xFF38BDF8), fontSize = 13.sp)
                Text("Đại diện: ${customer.keyDecisionMaker}", color = Color.White, fontSize = 12.sp)

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tiêu đề sự kiện chăm sóc") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Nội dung chi tiết trao đổi") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        isSaving = true
                        scope.launch {
                            val res = ApiClient.logCareActivity(
                                customerId = customer.id,
                                sbu = customer.sbu,
                                activityType = activityType,
                                title = title.trim(),
                                content = content.trim(),
                                leaderInCharge = currentUser.fullName
                            )
                            isSaving = false
                            if (res.isSuccess) {
                                onLogged()
                            }
                        }
                    }
                },
                enabled = !isSaving && title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                else Text("Lưu Nhật Ký")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy", color = Color.Gray) }
        },
        containerColor = Color(0xFF1E293B)
    )
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
    var role by remember { mutableStateOf("Chủ tịch / Tổng Giám Đốc") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var taxCode by remember { mutableStateOf("") }
    var headquarters by remember { mutableStateOf("") }
    var birthday by remember { mutableStateOf("") }
    var anniversary by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var errText by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm Khách Hàng / CĐT Mới", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên Doanh nghiệp / CĐT (*)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                OutlinedTextField(
                    value = keyPerson,
                    onValueChange = { keyPerson = it },
                    label = { Text("Người quyết định cá nhân (*)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("Chức vụ") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Số điện thoại cá nhân (*)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                OutlinedTextField(
                    value = headquarters,
                    onValueChange = { headquarters = it },
                    label = { Text("Trụ sở chính") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                OutlinedTextField(
                    value = birthday,
                    onValueChange = { birthday = it },
                    label = { Text("Sinh nhật (YYYY-MM-DD)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                OutlinedTextField(
                    value = anniversary,
                    onValueChange = { anniversary = it },
                    label = { Text("Ngày thành lập (YYYY-MM-DD)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Ghi chú chiến lược cá nhân") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                errText?.let {
                    Text(it, color = Color(0xFFF87171), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && keyPerson.isNotBlank()) {
                        isSaving = true
                        errText = null
                        scope.launch {
                            val res = ApiClient.createCustomer(
                                name = name.trim(),
                                sbu = sbu,
                                tier = "STRATEGIC_VIP",
                                keyDecisionMaker = keyPerson.trim(),
                                role = role.trim(),
                                phone = phone.trim(),
                                email = email.trim(),
                                taxCode = taxCode.trim(),
                                headquarters = headquarters.trim(),
                                birthday = birthday.trim(),
                                anniversary = anniversary.trim(),
                                notes = notes.trim()
                            )
                            isSaving = false
                            if (res.isSuccess) {
                                onAdded()
                            } else {
                                errText = res.exceptionOrNull()?.message ?: "Lỗi tạo khách hàng"
                            }
                        }
                    }
                },
                enabled = !isSaving && name.isNotBlank() && keyPerson.isNotBlank(),
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
