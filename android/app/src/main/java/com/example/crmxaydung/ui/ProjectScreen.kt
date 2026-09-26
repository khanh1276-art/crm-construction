package com.example.crmxaydung.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.crmxaydung.data.PipelineBidItem
import com.example.crmxaydung.data.ProjectItem
import com.example.crmxaydung.data.UserSession
import kotlinx.coroutines.launch
import java.text.DecimalFormat

enum class ProjectViewTab {
    PIPELINE, ACTIVE_PROJECTS
}

@Composable
fun ProjectScreen(user: UserSession) {
    val coroutineScope = rememberCoroutineScope()
    var currentSubTab by remember { mutableStateOf(ProjectViewTab.PIPELINE) }

    var projects by remember { mutableStateOf<List<ProjectItem>>(emptyList()) }
    var pipelineBids by remember { mutableStateOf<List<PipelineBidItem>>(emptyList()) }
    var customers by remember { mutableStateOf<List<CustomerItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var selectedProject by remember { mutableStateOf<ProjectItem?>(null) }
    var selectedBid by remember { mutableStateOf<PipelineBidItem?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedPipelineStage by remember { mutableStateOf("ALL") }

    val feconOrange = Color(0xFFEA580C)
    val df = DecimalFormat("#,##0.0")

    fun loadData() {
        isLoading = true
        coroutineScope.launch {
            val sbuFilter = if (user.role == "ADMIN") "ALL" else user.sbu
            val resP = ApiClient.fetchProjects(sbuFilter)
            val resBids = ApiClient.fetchPipelineBids(sbuFilter)
            val resC = ApiClient.fetchCustomers("ALL")
            projects = resP.getOrNull() ?: emptyList()
            pipelineBids = resBids.getOrNull() ?: emptyList()
            customers = resC.getOrNull() ?: emptyList()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    val pipelineStages = listOf(
        "ALL" to "Tất Cả",
        "INFORMATION" to "1. Tiếp Cận",
        "EVALUATION" to "2. Khảo Sát",
        "TENDER_PREP" to "3. Lập Hồ Sơ",
        "NEGOTIATION" to "4. Thương Thảo",
        "WON" to "🏆 Trúng Thầu",
        "LOST" to "❌ Trượt Thầu"
    )

    val filteredBids = pipelineBids.filter {
        selectedPipelineStage == "ALL" || it.stage == selectedPipelineStage
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC), // Nền sáng
        floatingActionButton = {
            if (user.role != "COLLABORATOR" && currentSubTab == ProjectViewTab.ACTIVE_PROJECTS) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = feconOrange,
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🏗️ Quản Lý Dự Án & Hồ Sơ Thầu",
                        color = Color(0xFF0F172A),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Theo dõi phễu cơ hội, tiến trình đấu thầu & dự án",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp
                    )
                }
                TextButton(onClick = { loadData() }) {
                    Text("Làm mới", color = feconOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sub Tab Selector: PIPELINE vs ACTIVE PROJECTS (Light Theme Card)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                    .padding(4.dp)
            ) {
                Surface(
                    color = if (currentSubTab == ProjectViewTab.PIPELINE) feconOrange else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { currentSubTab = ProjectViewTab.PIPELINE }
                ) {
                    Text(
                        text = "🎯 Phễu Dự Án (${pipelineBids.size})",
                        color = if (currentSubTab == ProjectViewTab.PIPELINE) Color.White else Color(0xFF475569),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                Surface(
                    color = if (currentSubTab == ProjectViewTab.ACTIVE_PROJECTS) feconOrange else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { currentSubTab = ProjectViewTab.ACTIVE_PROJECTS }
                ) {
                    Text(
                        text = "🏗️ Đang Thi Công (${projects.size})",
                        color = if (currentSubTab == ProjectViewTab.ACTIVE_PROJECTS) Color.White else Color(0xFF475569),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = feconOrange)
                }
            } else if (currentSubTab == ProjectViewTab.PIPELINE) {
                // Stage Filter Chips for Pipeline
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(pipelineStages) { (key, label) ->
                        val isSelected = (selectedPipelineStage == key)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedPipelineStage = key },
                            label = { Text(label, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when (key) {
                                    "WON" -> Color(0xFF059669)
                                    "LOST" -> Color(0xFFDC2626)
                                    else -> feconOrange
                                },
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

                Spacer(modifier = Modifier.height(10.dp))

                if (filteredBids.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Không có hồ sơ thầu nào trong giai đoạn này", color = Color(0xFF94A3B8))
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredBids) { bid ->
                            PipelineBidCard(
                                bid = bid,
                                df = df,
                                onClick = { selectedBid = bid }
                            )
                        }
                    }
                }
            } else {
                // Active Projects List
                if (projects.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Chưa có dự án nào được ghi nhận", color = Color(0xFF94A3B8))
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
                                    coroutineScope.launch {
                                        val detailRes = ApiClient.fetchProjectDetail(proj.id)
                                        selectedProject = detailRes.getOrNull() ?: proj
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Pipeline Bid Detail Dialog
    selectedBid?.let { bid ->
        PipelineDetailDialog(
            bid = bid,
            df = df,
            onDismiss = { selectedBid = null },
            onUpdated = {
                selectedBid = null
                loadData()
            }
        )
    }

    // Active Project Detail Dialog
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
fun PipelineBidCard(bid: PipelineBidItem, df: DecimalFormat, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = when (bid.sbu) {
                        "SBU1" -> Color(0xFF1E3A8A)
                        "SBU2" -> Color(0xFF78350F)
                        "SBU3" -> Color(0xFF581C87)
                        "SBU4" -> Color(0xFF064E3B)
                        else -> Color(0xFF164E63)
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = bid.sbu,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Surface(
                    color = when (bid.stage) {
                        "WON" -> Color(0xFF059669)
                        "LOST" -> Color(0xFFDC2626)
                        "NEGOTIATION" -> Color(0xFFD97706)
                        "TENDER_PREP" -> Color(0xFF2563EB)
                        else -> Color(0xFF64748B)
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = when (bid.stage) {
                            "WON" -> "🏆 Trúng Thầu"
                            "LOST" -> "❌ Trượt Thầu"
                            "NEGOTIATION" -> "🤝 Thương Thảo"
                            "TENDER_PREP" -> "📑 Lập Hồ Sơ"
                            "EVALUATION" -> "🔍 Khảo Sát"
                            else -> "📝 Tiếp Cận"
                        },
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = bid.projectTitle,
                color = Color(0xFF0F172A),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "Đối tác: ${bid.customerName}",
                color = Color(0xFF64748B),
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${df.format(bid.estimatedValueBillion)} Tỷ VNĐ",
                    color = Color(0xFFEA580C),
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
                Text(
                    text = "${bid.winRate}% Xác Suất",
                    color = Color(0xFF7C3AED),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun PipelineDetailDialog(
    bid: PipelineBidItem,
    df: DecimalFormat,
    onDismiss: () -> Unit,
    onUpdated: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentStage by remember { mutableStateOf(bid.stage) }
    var winRate by remember { mutableStateOf(bid.winRate) }
    var notes by remember { mutableStateOf(bid.biddingNotes) }
    var isSaving by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf<String?>(null) }

    val feconOrange = Color(0xFFEA580C)

    fun performUpdate(targetStage: String, targetWinRate: Int, targetNotes: String) {
        isSaving = true
        statusMsg = null
        scope.launch {
            val res = ApiClient.updateBidStage(bid.id, targetStage, targetWinRate, targetNotes.trim())
            isSaving = false
            if (res.isSuccess) {
                onUpdated()
            } else {
                statusMsg = res.exceptionOrNull()?.message ?: "Lỗi khi cập nhật trạng thái"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFF2563EB),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(bid.sbu, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                    }
                    Text("Chi Tiết Gói Thầu", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(bid.projectTitle, color = feconOrange, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Partner & Representative Info (Light Card)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Đối tác / CĐT: ${bid.customerName}", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 12.sp)
                        if (bid.keyDecisionMaker.isNotBlank()) {
                            Text("Đại diện: ${bid.keyDecisionMaker}", color = Color(0xFF475569), fontSize = 11.sp)
                        }
                        if (bid.decisionMakerPhone.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("SĐT: ${bid.decisionMakerPhone}", color = Color(0xFF0284C7), fontSize = 11.sp)
                                TextButton(onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${bid.decisionMakerPhone}"))
                                    context.startActivity(intent)
                                }) {
                                    Text("📞 Gọi CĐT", color = Color(0xFF059669), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Overview Values
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Giá trị dự toán:", color = Color(0xFF64748B), fontSize = 12.sp)
                    Text("${df.format(bid.estimatedValueBillion)} Tỷ VNĐ", color = feconOrange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                if (bid.tenderDeadline.isNotBlank()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Hạn nộp thầu:", color = Color(0xFF64748B), fontSize = 12.sp)
                        Text(bid.tenderDeadline, color = Color(0xFF0F172A), fontSize = 12.sp)
                    }
                }
                if (bid.assignedDirector.isNotBlank()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Lãnh đạo phụ trách:", color = Color(0xFF64748B), fontSize = 12.sp)
                        Text(bid.assignedDirector, color = Color(0xFF334155), fontSize = 12.sp)
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                // DIRECT STAGE CONVERSION BUTTONS
                Text("⚡ Chuyển Đổi Nhanh Kết Quả Thầu:", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 12.sp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            currentStage = "WON"
                            winRate = 100
                            performUpdate("WON", 100, notes)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving
                    ) {
                        Text("🏆 TRÚNG THẦU", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            currentStage = "LOST"
                            winRate = 0
                            performUpdate("LOST", 0, notes)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving
                    ) {
                        Text("❌ TRƯỢT THẦU", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Stage Select Chips
                Text("Hoặc chuyển giai đoạn đấu thầu:", color = Color(0xFF64748B), fontSize = 11.sp)
                val allStages = listOf(
                    "INFORMATION" to "1. Tiếp cận",
                    "EVALUATION" to "2. Khảo sát",
                    "TENDER_PREP" to "3. Lập hồ sơ",
                    "NEGOTIATION" to "4. Thương thảo"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    allStages.forEach { (stgKey, label) ->
                        val isSelected = (currentStage == stgKey)
                        Surface(
                            color = if (isSelected) feconOrange else Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, if (isSelected) feconOrange else Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                                .clickable { currentStage = stgKey }
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else Color(0xFF475569),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(vertical = 6.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                // Win rate
                OutlinedTextField(
                    value = winRate.toString(),
                    onValueChange = { winRate = it.toIntOrNull() ?: winRate },
                    label = { Text("Xác suất thắng thầu (%)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF334155),
                        focusedBorderColor = feconOrange,
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )

                // Editable Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Ghi chú chiến lược & Cập nhật tiến độ") },
                    placeholder = { Text("Ghi nhận nội dung làm việc với CĐT, cam kết...", color = Color.Gray) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF334155),
                        focusedBorderColor = feconOrange,
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )

                statusMsg?.let {
                    Text(it, color = Color(0xFFDC2626), fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { performUpdate(currentStage, winRate, notes) },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = feconOrange)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                } else {
                    Text("Lưu Thay Đổi", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Đóng", color = Color(0xFF64748B)) }
        },
        containerColor = Color.White
    )
}

@Composable
fun ProjectCard(proj: ProjectItem, df: DecimalFormat, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
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
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = when (proj.projectHealth) {
                        "GOOD" -> Color(0xFF059669)
                        "WARNING" -> Color(0xFFD97706)
                        else -> Color(0xFFDC2626)
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = when (proj.projectHealth) {
                            "GOOD" -> "An toàn"
                            "WARNING" -> "Cảnh báo"
                            else -> "Rủi ro"
                        },
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text("Chủ đầu tư: ${proj.customerName} (${proj.sbu})", color = Color(0xFFEA580C), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Giá trị HĐ", color = Color(0xFF64748B), fontSize = 11.sp)
                    Text("${df.format(proj.contractValueBillion)} Tỷ", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Đã thu", color = Color(0xFF64748B), fontSize = 11.sp)
                    Text("${df.format(proj.collectedAmountBillion)} Tỷ", color = Color(0xFF059669), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Công nợ còn lại:", color = Color(0xFF64748B), fontSize = 11.sp)
                Text("${df.format(proj.unpaidBillion)} Tỷ", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
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
                Text(proj.name, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 16.sp)
                Text("Mã DA: ${proj.code} • Khối: ${proj.sbu}", color = Color(0xFFEA580C), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Customer & Decision Maker
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Chủ đầu tư: ${proj.customerName}", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 13.sp)
                        if (proj.keyDecisionMaker.isNotBlank()) {
                            Text("Đại diện: ${proj.keyDecisionMaker}", color = Color(0xFF475569), fontSize = 12.sp)
                        }
                        if (proj.decisionMakerPhone.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("SĐT: ${proj.decisionMakerPhone}", color = Color(0xFF0284C7), fontSize = 12.sp)
                                TextButton(onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${proj.decisionMakerPhone}"))
                                    context.startActivity(intent)
                                }) {
                                    Text("📞 Gọi CĐT", color = Color(0xFF059669), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Financial Overview
                Text("💰 Tình Hình Dòng Tiền:", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Tổng giá trị HĐ:", color = Color(0xFF64748B), fontSize = 12.sp)
                    Text("${df.format(proj.contractValueBillion)} Tỷ", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Đã thu hồi:", color = Color(0xFF64748B), fontSize = 12.sp)
                    Text("${df.format(proj.collectedAmountBillion)} Tỷ", color = Color(0xFF059669), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Công nợ còn lại:", color = Color(0xFF64748B), fontSize = 12.sp)
                    Text("${df.format(proj.unpaidBillion)} Tỷ", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (proj.projectDirector.isNotBlank()) {
                    Text("Giám đốc điều hành DA: ${proj.projectDirector}", color = Color(0xFF334155), fontSize = 12.sp)
                }
                if (proj.contractNumber.isNotBlank()) {
                    Text("Số hợp đồng: ${proj.contractNumber}", color = Color(0xFF64748B), fontSize = 11.sp)
                }
                if (proj.summaryScope.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Quy mô: ${proj.summaryScope}", color = Color(0xFF64748B), fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Milestones & Cashflow
                Text("📋 Các Đợt Nghiệm Thu & Giải Ngân:", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))

                if (proj.milestones.isEmpty()) {
                    Text("Chưa có mốc giải ngân nào được thiết lập.", color = Color(0xFF94A3B8), fontSize = 11.sp)
                } else {
                    proj.milestones.forEach { m ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(m.title, color = Color(0xFF0F172A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Đáo hạn: ${m.dueDate} • ${m.percentage}% HĐ", color = Color(0xFF64748B), fontSize = 10.sp)
                                }
                                Surface(
                                    color = if (m.paymentStatus == "PAID") Color(0xFF059669) else Color(0xFFD97706),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (m.paymentStatus == "PAID") "ĐÃ THU" else "CHỜ THU",
                                        color = Color.White,
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
            TextButton(onClick = onDismiss) { Text("Đóng", color = Color(0xFFEA580C), fontWeight = FontWeight.Bold) }
        },
        containerColor = Color.White
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
    val feconOrange = Color(0xFFEA580C)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tạo Hồ Sơ Dự Án Mới", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold) },
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
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF334155),
                        focusedBorderColor = feconOrange,
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )

                OutlinedTextField(
                    value = contractValueBillion,
                    onValueChange = { contractValueBillion = it },
                    label = { Text("Giá trị Hợp đồng (Tỷ VNĐ)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF334155),
                        focusedBorderColor = feconOrange,
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )

                OutlinedTextField(
                    value = director,
                    onValueChange = { director = it },
                    label = { Text("Giám đốc phụ trách") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF334155),
                        focusedBorderColor = feconOrange,
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )

                OutlinedTextField(
                    value = summaryScope,
                    onValueChange = { summaryScope = it },
                    label = { Text("Quy mô tóm tắt") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF334155),
                        focusedBorderColor = feconOrange,
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )

                errText?.let {
                    Text(it, color = Color(0xFFDC2626), fontSize = 12.sp)
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
                colors = ButtonDefaults.buttonColors(containerColor = feconOrange)
            ) {
                if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                else Text("Tạo Dự Án", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy", color = Color(0xFF64748B)) }
        },
        containerColor = Color.White
    )
}
