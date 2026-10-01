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
import com.example.crmxaydung.data.UserSession
import kotlinx.coroutines.launch
import java.text.DecimalFormat

@Composable
fun ProjectScreen(user: UserSession) {
    val coroutineScope = rememberCoroutineScope()

    var pipelineBids by remember { mutableStateOf<List<PipelineBidItem>>(emptyList()) }
    var customers by remember { mutableStateOf<List<CustomerItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var selectedBid by remember { mutableStateOf<PipelineBidItem?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedPipelineStage by remember { mutableStateOf("ALL") }
    var selectedProjectLevel by remember { mutableStateOf("ALL") }

    val feconOrange = Color(0xFFEA580C)
    val df = DecimalFormat("#,##0.0")

    fun loadData() {
        isLoading = true
        coroutineScope.launch {
            val sbuFilter = if (user.role == "ADMIN") "ALL" else user.sbu
            val resBids = ApiClient.fetchPipelineBids(sbuFilter)
            val resC = ApiClient.fetchCustomers("ALL")
            pipelineBids = resBids.getOrNull() ?: emptyList()
            customers = resC.getOrNull() ?: emptyList()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    val pipelineStages = listOf(
        "ALL" to "Tất Cả Giai Đoạn",
        "INFORMATION" to "1. Tiếp Cận",
        "EVALUATION" to "2. Khảo Sát",
        "TENDER_PREP" to "3. Lập Hồ Sơ",
        "NEGOTIATION" to "4. Thương Thảo",
        "WON" to "🏆 Trúng Thầu",
        "LOST" to "❌ Trượt Thầu"
    )

    val projectLevels = listOf(
        "ALL" to "Tất Cả Cấp",
        "LEVEL_SPECIAL" to "🟣 Cấp Đặc Biệt",
        "LEVEL_1" to "🔴 Cấp 1",
        "LEVEL_2" to "🟠 Cấp 2",
        "LEVEL_3" to "🔵 Cấp 3",
        "LEVEL_4" to "🟢 Cấp 4"
    )

    val filteredBids = pipelineBids.filter {
        (selectedPipelineStage == "ALL" || it.stage == selectedPipelineStage) &&
        (selectedProjectLevel == "ALL" || it.projectLevel == selectedProjectLevel)
    }

    val totalEstimatedBillion = filteredBids.sumOf { it.estimatedValueBillion }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        floatingActionButton = {
            if (user.role != "COLLABORATOR") {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = feconOrange,
                    contentColor = Color.White,
                    icon = { Text("➕", fontSize = 16.sp) },
                    text = { Text("Thêm Gói Thầu", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            // Compact Header with Inline Funnel Metrics & Refresh
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🎯 Phễu Thầu",
                        color = Color(0xFF0F172A),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(6.dp))
                    ) {
                        Text(
                            text = "${filteredBids.size} gói • ${df.format(totalEstimatedBillion)} Tỷ",
                            color = Color(0xFF1E40AF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                TextButton(
                    onClick = { loadData() },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Text("🔄 Làm mới", color = feconOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Stage Filter Chips (Compact 28dp)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(pipelineStages) { (key, label) ->
                    val isSelected = (selectedPipelineStage == key)
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedPipelineStage = key },
                        label = { Text(label, fontSize = 9.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                        modifier = Modifier.height(28.dp),
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

            Spacer(modifier = Modifier.height(3.dp))

            // Level Filter Chips (Clean names without monetary values, compact 28dp)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(projectLevels) { (lvlKey, label) ->
                    val isSelected = (selectedProjectLevel == lvlKey)
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedProjectLevel = lvlKey },
                        label = { Text(label, fontSize = 9.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                        modifier = Modifier.height(28.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (lvlKey) {
                                "LEVEL_SPECIAL" -> Color(0xFF7C3AED)
                                "LEVEL_1" -> Color(0xFFDC2626)
                                "LEVEL_2" -> Color(0xFFEA580C)
                                "LEVEL_3" -> Color(0xFF2563EB)
                                "LEVEL_4" -> Color(0xFF059669)
                                else -> Color(0xFF475569)
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

            Spacer(modifier = Modifier.height(5.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = feconOrange)
                }
            } else if (filteredBids.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎯", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Không có hồ sơ thầu nào phù hợp bộ lọc", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
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

    // Add Bid Dialog with Quick Customer Creation
    if (showAddDialog) {
        AddBidDialog(
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
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
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
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                    )
                }

                Surface(
                    color = when (bid.stage) {
                        "WON" -> Color(0xFF059669)
                        "LOST" -> Color(0xFFDC2626)
                        "NEGOTIATION" -> Color(0xFFD97706)
                        "TENDER_PREP" -> Color(0xFF2563EB)
                        "EVALUATION" -> Color(0xFF0284C7)
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
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = bid.projectTitle,
                color = Color(0xFF0F172A),
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp
            )

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Đối tác: ${bid.customerName}",
                color = Color(0xFF64748B),
                fontSize = 11.5.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${df.format(bid.estimatedValueBillion)} Tỷ VNĐ",
                    color = Color(0xFFEA580C),
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
                Text(
                    text = "${bid.winRate}% Xác Suất",
                    color = Color(0xFF7C3AED),
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Clean FECON Level (no threshold value) and Approver Short
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = when (bid.projectLevel) {
                        "LEVEL_SPECIAL" -> Color(0xFF7C3AED)
                        "LEVEL_1" -> Color(0xFFDC2626)
                        "LEVEL_2" -> Color(0xFFEA580C)
                        "LEVEL_3" -> Color(0xFF2563EB)
                        else -> Color(0xFF059669)
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = bid.projectLevelName,
                        color = Color.White,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                    )
                }
                Text(
                    text = "Duyệt: ${bid.approverShort}",
                    color = Color(0xFF475569),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Medium
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

                // FECON Project Level & Approval Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.border(1.dp, Color(0xFFFED7AA), RoundedCornerShape(8.dp))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = when (bid.projectLevel) {
                                    "LEVEL_SPECIAL" -> Color(0xFF7C3AED)
                                    "LEVEL_1" -> Color(0xFFDC2626)
                                    "LEVEL_2" -> Color(0xFFEA580C)
                                    "LEVEL_3" -> Color(0xFF2563EB)
                                    else -> Color(0xFF059669)
                                },
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = bid.projectLevelName,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Phân cấp dự án FECON", color = Color(0xFF9A3412), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("⚖️ Thẩm quyền duyệt chi phí tiếp khách/CSKH:", color = Color(0xFF7C2D12), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text(bid.approverAuthority, color = Color(0xFFEA580C), fontWeight = FontWeight.Black, fontSize = 12.sp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBidDialog(
    customers: List<CustomerItem>,
    defaultSbu: String,
    onDismiss: () -> Unit,
    onAdded: () -> Unit
) {
    var localCustomers by remember { mutableStateOf(customers) }
    var selectedCustomerId by remember { mutableStateOf(customers.firstOrNull()?.id ?: 0) }
    var showQuickAddCustomer by remember { mutableStateOf(false) }

    // Quick add customer fields
    var quickCustName by remember { mutableStateOf("") }
    var quickCustContact by remember { mutableStateOf("") }
    var quickCustPhone by remember { mutableStateOf("") }
    var quickCustSegment by remember { mutableStateOf("B2B") }
    var quickCustTier by remember { mutableStateOf("GOLD") }
    var isQuickSaving by remember { mutableStateOf(false) }
    var quickError by remember { mutableStateOf<String?>(null) }

    // Bid fields
    var sbu by remember { mutableStateOf(defaultSbu) }
    var projectTitle by remember { mutableStateOf("") }
    var estimatedValueBillionText by remember { mutableStateOf("") }
    var stage by remember { mutableStateOf("INFORMATION") }
    var winRate by remember { mutableStateOf("50") }
    var tenderDeadline by remember { mutableStateOf("") }
    var assignedDirector by remember { mutableStateOf("") }
    var biddingNotes by remember { mutableStateOf("") }

    var isSaving by remember { mutableStateOf(false) }
    var errText by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val feconOrange = Color(0xFFEA580C)

    // Calculate dynamic FECON level preview based on estimated value in Billion VNĐ
    val estimatedBillion = estimatedValueBillionText.toDoubleOrNull() ?: 0.0
    val levelInfo = when {
        estimatedBillion >= 500.0 -> Triple("LEVEL_SPECIAL", "Cấp Đặc Biệt", "Chủ tịch HĐQT quyết định")
        estimatedBillion >= 300.0 -> Triple("LEVEL_1", "Cấp 1", "Tổng Giám đốc (hoặc PTGĐ ủy quyền)")
        estimatedBillion >= 150.0 -> Triple("LEVEL_2", "Cấp 2", "Phó Tổng Giám đốc phụ trách SBU")
        estimatedBillion >= 50.0 -> Triple("LEVEL_3", "Cấp 3", "Phó Tổng Giám đốc phụ trách SBU")
        else -> Triple("LEVEL_4", "Cấp 4", "Phó Tổng Giám đốc phụ trách SBU")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Thêm Cơ Hội & Hồ Sơ Dự Thầu", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Quản lý từ khảo sát, lập thầu đến trúng/trượt", color = Color(0xFF64748B), fontSize = 11.sp)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Customer selector header with "+ Thêm CĐT mới" button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Chủ đầu tư / Khách hàng *", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF334155))
                    TextButton(
                        onClick = { showQuickAddCustomer = !showQuickAddCustomer },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = if (showQuickAddCustomer) "✕ Đóng thêm CĐT" else "➕ Thêm CĐT mới",
                            color = Color(0xFF2563EB),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                // Inline Quick Add Customer Form
                if (showQuickAddCustomer) {
                    Surface(
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(8.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("🏢 Nhập Thông Tin Chủ Đầu Tư Mới", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF1E40AF))
                            OutlinedTextField(
                                value = quickCustName,
                                onValueChange = { quickCustName = it },
                                label = { Text("Tên CĐT / Doanh nghiệp *") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = quickCustContact,
                                onValueChange = { quickCustContact = it },
                                label = { Text("Lãnh đạo / Người liên hệ *") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = quickCustPhone,
                                onValueChange = { quickCustPhone = it },
                                label = { Text("Số điện thoại liên hệ *") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            quickError?.let {
                                Text(it, color = Color(0xFFDC2626), fontSize = 11.sp)
                            }
                            Button(
                                onClick = {
                                    if (quickCustName.isBlank() || quickCustContact.isBlank() || quickCustPhone.isBlank()) {
                                        quickError = "Vui lòng điền đủ Tên, Lãnh đạo và SĐT CĐT"
                                        return@Button
                                    }
                                    isQuickSaving = true
                                    quickError = null
                                    scope.launch {
                                        val res = ApiClient.createCustomerReturnId(
                                            name = quickCustName.trim(),
                                            sbu = sbu,
                                            tier = quickCustTier,
                                            keyDecisionMaker = quickCustContact.trim(),
                                            role = "Chủ tịch / Tổng Giám Đốc",
                                            phone = quickCustPhone.trim(),
                                            email = "",
                                            taxCode = "",
                                            headquarters = "",
                                            birthday = "",
                                            anniversary = "",
                                            notes = "Thêm nhanh từ màn hình tạo hồ sơ thầu"
                                        )
                                        isQuickSaving = false
                                        if (res.isSuccess) {
                                            val newId = res.getOrNull() ?: 0
                                            val newCust = CustomerItem(
                                                id = newId,
                                                code = "KH-$sbu-$newId",
                                                name = quickCustName.trim(),
                                                sbu = sbu,
                                                tier = quickCustTier,
                                                keyDecisionMaker = quickCustContact.trim(),
                                                decisionMakerRole = "Chủ tịch / TGĐ",
                                                decisionMakerPhone = quickCustPhone.trim()
                                            )
                                            localCustomers = listOf(newCust) + localCustomers
                                            selectedCustomerId = newId
                                            showQuickAddCustomer = false
                                            quickCustName = ""
                                            quickCustContact = ""
                                            quickCustPhone = ""
                                        } else {
                                            quickError = res.exceptionOrNull()?.message ?: "Lỗi thêm CĐT"
                                        }
                                    }
                                },
                                enabled = !isQuickSaving && quickCustName.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isQuickSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                                else Text("✓ Lưu & Chọn CĐT này", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Customer selection dropdown / list
                var expandedCust by remember { mutableStateOf(false) }
                val selectedCustName = localCustomers.find { it.id == selectedCustomerId }?.name ?: "Chọn chủ đầu tư"

                ExposedDropdownMenuBox(
                    expanded = expandedCust,
                    onExpandedChange = { expandedCust = !expandedCust }
                ) {
                    OutlinedTextField(
                        value = selectedCustName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCust) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF334155),
                            focusedBorderColor = feconOrange,
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCust,
                        onDismissRequest = { expandedCust = false }
                    ) {
                        localCustomers.forEach { cust ->
                            DropdownMenuItem(
                                text = { Text("${cust.name} (${cust.sbu})", fontSize = 12.sp) },
                                onClick = {
                                    selectedCustomerId = cust.id
                                    if (cust.sbu.isNotBlank() && cust.sbu != "ALL") {
                                        sbu = cust.sbu
                                    }
                                    expandedCust = false
                                }
                            )
                        }
                    }
                }

                // SBU Selector
                val sbus = listOf("SBU1", "SBU2", "SBU3", "SBU4", "SBU5")
                Text("Thuộc Khối SBU:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF475569))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    sbus.forEach { itemSbu ->
                        val isSel = (sbu == itemSbu)
                        Surface(
                            color = if (isSel) feconOrange else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { sbu = itemSbu }
                        ) {
                            Text(
                                text = itemSbu,
                                color = if (isSel) Color.White else Color(0xFF334155),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 6.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                // Project Title
                OutlinedTextField(
                    value = projectTitle,
                    onValueChange = { projectTitle = it },
                    label = { Text("Tên gói thầu / Dự án *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF334155),
                        focusedBorderColor = feconOrange,
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )

                // Estimated Value in Billion VNĐ
                OutlinedTextField(
                    value = estimatedValueBillionText,
                    onValueChange = { estimatedValueBillionText = it },
                    label = { Text("Ước tính giá trị gói thầu (Tỷ VNĐ) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF334155),
                        focusedBorderColor = feconOrange,
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )

                // Live FECON Classification & Authority Preview
                Surface(
                    color = Color(0xFFFAF5FF),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE9D5FF), RoundedCornerShape(8.dp))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Phân cấp FECON: ", fontSize = 11.sp, color = Color(0xFF6B21A8), fontWeight = FontWeight.Bold)
                            Surface(
                                color = when (levelInfo.first) {
                                    "LEVEL_SPECIAL" -> Color(0xFF7C3AED)
                                    "LEVEL_1" -> Color(0xFFDC2626)
                                    "LEVEL_2" -> Color(0xFFEA580C)
                                    "LEVEL_3" -> Color(0xFF2563EB)
                                    else -> Color(0xFF059669)
                                },
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = levelInfo.second,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Thẩm quyền duyệt tiếp khách/CSKH: ${levelInfo.third}",
                            fontSize = 10.sp,
                            color = Color(0xFF581C87),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Initial Stage
                val initialStages = listOf(
                    "INFORMATION" to "1. Tiếp Cận",
                    "EVALUATION" to "2. Khảo Sát",
                    "TENDER_PREP" to "3. Lập Hồ Sơ",
                    "NEGOTIATION" to "4. Thương Thảo"
                )
                Text("Giai đoạn ban đầu:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF475569))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    initialStages.forEach { (stgKey, label) ->
                        val isSel = (stage == stgKey)
                        Surface(
                            color = if (isSel) feconOrange else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { stage = stgKey }
                        ) {
                            Text(
                                text = label,
                                color = if (isSel) Color.White else Color(0xFF334155),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 6.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                // Win rate & Deadline
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = winRate,
                        onValueChange = { winRate = it },
                        label = { Text("Xác suất (%)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = tenderDeadline,
                        onValueChange = { tenderDeadline = it },
                        label = { Text("Hạn nộp (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.weight(1.5f)
                    )
                }

                // Director
                OutlinedTextField(
                    value = assignedDirector,
                    onValueChange = { assignedDirector = it },
                    label = { Text("Giám đốc / Cán bộ phụ trách") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Notes
                OutlinedTextField(
                    value = biddingNotes,
                    onValueChange = { biddingNotes = it },
                    label = { Text("Ghi chú chiến lược đấu thầu") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                errText?.let {
                    Text(it, color = Color(0xFFDC2626), fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (projectTitle.isBlank()) {
                        errText = "Vui lòng nhập tên gói thầu / dự án"
                        return@Button
                    }
                    if (selectedCustomerId <= 0) {
                        errText = "Vui lòng chọn hoặc thêm chủ đầu tư"
                        return@Button
                    }
                    val estValVnd = (estimatedValueBillionText.toDoubleOrNull() ?: 0.0) * 1_000_000_000.0
                    val winRateInt = winRate.toIntOrNull() ?: 50
                    isSaving = true
                    errText = null
                    scope.launch {
                        val res = ApiClient.createBid(
                            customerId = selectedCustomerId,
                            sbu = sbu,
                            projectTitle = projectTitle.trim(),
                            estimatedValueVnd = estValVnd,
                            stage = stage,
                            winRate = winRateInt,
                            tenderDeadline = tenderDeadline.trim(),
                            targetKickoff = "",
                            assignedDirector = assignedDirector.trim(),
                            biddingNotes = biddingNotes.trim(),
                            projectLevel = levelInfo.first,
                            projectLevelName = levelInfo.second,
                            approverAuthority = levelInfo.third
                        )
                        isSaving = false
                        if (res.isSuccess) {
                            onAdded()
                        } else {
                            errText = res.exceptionOrNull()?.message ?: "Lỗi tạo gói thầu"
                        }
                    }
                },
                enabled = !isSaving && projectTitle.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = feconOrange)
            ) {
                if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                else Text("Lưu Gói Thầu", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy", color = Color(0xFF64748B)) }
        },
        containerColor = Color.White
    )
}
