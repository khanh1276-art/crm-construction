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
import com.example.crmxaydung.data.ProjectItem
import com.example.crmxaydung.data.UserSession
import kotlinx.coroutines.launch
import java.text.DecimalFormat

@Composable
fun ProjectScreen(user: UserSession) {
    val coroutineScope = rememberCoroutineScope()
    var projects by remember { mutableStateOf<List<ProjectItem>>(emptyList()) }
    var customers by remember { mutableStateOf<List<CustomerItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedProject by remember { mutableStateOf<ProjectItem?>(null) }
    var isLoadingDetail by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    fun loadData() {
        isLoading = true
        coroutineScope.launch {
            val sbuFilter = if (user.role == "ADMIN") "ALL" else user.sbu
            val resP = ApiClient.fetchProjects(sbuFilter)
            val resC = ApiClient.fetchCustomers(sbuFilter)
            projects = resP.getOrNull() ?: emptyList()
            customers = resC.getOrNull() ?: emptyList()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    val df = DecimalFormat("#,##0.0")

    Scaffold(
        containerColor = Color(0xFF0F172A),
        floatingActionButton = {
            if (user.role != "COLLABORATOR") {
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
                        text = "🏗️ Quản Lý & Tiến Độ Dự Án",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Bấm vào từng dự án để xem chi tiết tiến độ & dòng tiền",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
                TextButton(onClick = { loadData() }) {
                    Text("Làm mới", color = Color(0xFF38BDF8), fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF38BDF8))
                }
            } else if (projects.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Chưa có dự án nào được ghi nhận", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(projects) { proj ->
                        ProjectCard(
                            proj = proj,
                            df = df,
                            onClick = {
                                isLoadingDetail = true
                                coroutineScope.launch {
                                    val detailRes = ApiClient.fetchProjectDetail(proj.id)
                                    selectedProject = detailRes.getOrNull() ?: proj
                                    isLoadingDetail = false
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Project Detail Dialog (Full Details & Milestones)
    selectedProject?.let { proj ->
        ProjectDetailDialog(
            proj = proj,
            df = df,
            onDismiss = { selectedProject = null }
        )
    }

    // Add Project Dialog
    if (showAddDialog) {
        AddProjectDialog(
            customers = customers,
            defaultSbu = if (user.role == "ADMIN") "SBU1" else user.sbu,
            onDismiss = { showAddDialog = false },
            onAdded = {
                showAddDialog = false
                loadData()
            }
        )
    }
}

@Composable
fun ProjectCard(proj: ProjectItem, df: DecimalFormat, onClick: () -> Unit) {
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
                    text = proj.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = when (proj.projectHealth) {
                        "GOOD" -> Color(0xFF047857)
                        "ATTENTION" -> Color(0xFFB45309)
                        else -> Color(0xFFB91C1C)
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "${proj.sbu} • ${proj.projectHealth}",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text("Chủ đầu tư: ${proj.customerName}", color = Color(0xFF94A3B8), fontSize = 12.sp)

            Spacer(modifier = Modifier.height(8.dp))

            // Financial Summary
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Giá trị HĐ: ${df.format(proj.contractValueBillion)} tỷ", color = Color(0xFFE2E8F0), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("Đã thu: ${df.format(proj.collectedAmountBillion)} tỷ", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress Bar
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { (proj.progressPercent / 100.0).toFloat().coerceIn(0f, 1f) },
                    color = Color(0xFF38BDF8),
                    trackColor = Color(0xFF334155),
                    modifier = Modifier.weight(1f).height(7.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${proj.progressPercent.toInt()}%",
                    color = Color(0xFF38BDF8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text("👉 Chạm để xem các đợt nghiệm thu & thanh toán", color = Color(0xFF64748B), fontSize = 10.sp)
        }
    }
}

@Composable
fun ProjectDetailDialog(proj: ProjectItem, df: DecimalFormat, onDismiss: () -> Unit) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(proj.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                Text("Mã dự án: ${proj.code} | Khối: ${proj.sbu}", color = Color(0xFF38BDF8), fontSize = 12.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Customer & Decision Maker
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)), shape = RoundedCornerShape(8.dp)) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Chủ đầu tư: ${proj.customerName}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        if (proj.keyDecisionMaker.isNotBlank()) {
                            Text("Đại diện: ${proj.keyDecisionMaker}", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                        }
                        if (proj.decisionMakerPhone.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("SĐT: ${proj.decisionMakerPhone}", color = Color(0xFF38BDF8), fontSize = 12.sp)
                                TextButton(onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${proj.decisionMakerPhone}"))
                                    context.startActivity(intent)
                                }) {
                                    Text("📞 Gọi CĐT", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Financial Overview
                Text("💰 Tình Hình Dòng Tiền:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Tổng giá trị HĐ:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Text("${df.format(proj.contractValueBillion)} Tỷ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Đã thu hồi:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Text("${df.format(proj.collectedAmountBillion)} Tỷ", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Công nợ còn lại:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Text("${df.format(proj.unpaidBillion)} Tỷ", color = Color(0xFFF87171), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scope & Director
                if (proj.projectDirector.isNotBlank()) {
                    Text("Giám đốc điều hành DA: ${proj.projectDirector}", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                }
                if (proj.contractNumber.isNotBlank()) {
                    Text("Số hợp đồng: ${proj.contractNumber}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
                if (proj.summaryScope.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Quy mô: ${proj.summaryScope}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Milestones & Cashflow
                Text("📋 Các Đợt Nghiệm Thu & Giải Ngân:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))

                if (proj.milestones.isEmpty()) {
                    Text("Chưa có mốc giải ngân nào được thiết lập.", color = Color.Gray, fontSize = 11.sp)
                } else {
                    proj.milestones.forEach { m ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(m.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Đáo hạn: ${m.dueDate} • ${m.percentage}% HĐ", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                }
                                Surface(
                                    color = if (m.paymentStatus == "PAID") Color(0xFF065F46) else Color(0xFF78350F),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (m.paymentStatus == "PAID") "ĐÃ THU" else "CHỜ THU",
                                        color = if (m.paymentStatus == "PAID") Color(0xFF6EE7B7) else Color(0xFFFDE68A),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
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
fun AddProjectDialog(
    customers: List<CustomerItem>,
    defaultSbu: String,
    onDismiss: () -> Unit,
    onAdded: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var sbu by remember { mutableStateOf(defaultSbu) }
    var selectedCustomerId by remember { mutableStateOf(customers.firstOrNull()?.id ?: 0) }
    var contractValueBillion by remember { mutableStateOf("") }
    var progressPercent by remember { mutableStateOf("0") }
    var contractNumber by remember { mutableStateOf("") }
    var director by remember { mutableStateOf("") }
    var summaryScope by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var errText by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tạo Hồ Sơ Dự Án Mới", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên Dự Án / Gói Thầu") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = contractValueBillion,
                    onValueChange = { contractValueBillion = it },
                    label = { Text("Giá trị Hợp đồng (Tỷ VNĐ)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = director,
                    onValueChange = { director = it },
                    label = { Text("Giám đốc phụ trách") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = summaryScope,
                    onValueChange = { summaryScope = it },
                    label = { Text("Quy mô tóm tắt") },
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
                    if (name.isNotBlank() && selectedCustomerId > 0) {
                        isSaving = true
                        errText = null
                        val cVal = (contractValueBillion.toDoubleOrNull() ?: 0.0) * 1_000_000_000.0
                        val pPct = progressPercent.toDoubleOrNull() ?: 0.0
                        scope.launch {
                            val res = ApiClient.createProject(
                                name.trim(), sbu, selectedCustomerId,
                                cVal, pPct, contractNumber.trim(), director.trim(), summaryScope.trim()
                            )
                            isSaving = false
                            if (res.isSuccess) {
                                onAdded()
                            } else {
                                errText = res.exceptionOrNull()?.message ?: "Lỗi tạo dự án"
                            }
                        }
                    }
                },
                enabled = !isSaving && name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                else Text("Tạo Dự Án")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy", color = Color.Gray) }
        },
        containerColor = Color(0xFF1E293B)
    )
}
