package com.example.crmxaydung.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crmxaydung.data.ApiClient
import com.example.crmxaydung.data.CareActivityItem
import com.example.crmxaydung.data.CustomerItem
import com.example.crmxaydung.data.ProjectItem
import com.example.crmxaydung.data.UserSession
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CareScreen(user: UserSession, selectedSbu: String = "ALL") {
    val coroutineScope = rememberCoroutineScope()
    var activities by remember { mutableStateOf<List<CareActivityItem>>(emptyList()) }
    var customers by remember { mutableStateOf<List<CustomerItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilterType by remember { mutableStateOf("ALL") }
    var showAddCareDialog by remember { mutableStateOf(false) }
    var selectedActivity by remember { mutableStateOf<CareActivityItem?>(null) }

    fun loadData() {
        isLoading = true
        coroutineScope.launch {
            val sbuFilter = if (user.role == "ADMIN") selectedSbu else user.sbu
            val resAct = ApiClient.fetchCareActivities(sbuFilter)
            val resCust = ApiClient.fetchCustomers("ALL")
            activities = resAct.getOrNull() ?: emptyList()
            customers = resCust.getOrNull() ?: emptyList()
            isLoading = false
        }
    }

    LaunchedEffect(selectedSbu) {
        loadData()
    }

    val typeFilters = listOf(
        "ALL" to "Tất Cả",
        "DINNER_NETWORKING" to "🍷 Tiệc Giao Lưu",
        "EXECUTIVE_MEETING" to "💼 Họp Cấp Cao",
        "GIFT_DELIVERY" to "🎁 Quà Tri Ân",
        "EVENT_INVITATION" to "🏛️ Mời Sự Kiện",
        "CALL_DISCUSS" to "📞 Điện Đàm"
    )

    val filteredActivities = activities.filter {
        selectedFilterType == "ALL" || it.activityType == selectedFilterType
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC), // Nền sáng
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddCareDialog = true },
                containerColor = Color(0xFFEA580C),
                contentColor = Color.White
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("➕", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Thêm Lịch Chăm Sóc Khách Hàng", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🤝 Lịch Chăm Sóc Khách Hàng",
                        color = Color(0xFF0F172A),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tiếp khách, chiêu đãi lãnh đạo cấp cao",
                        color = Color(0xFF64748B),
                        fontSize = 11.5.sp
                    )
                }
                TextButton(onClick = { loadData() }) {
                    Text("Làm mới", color = Color(0xFFEA580C), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Filter by Care Type
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(typeFilters) { (typeKey, label) ->
                    val isSelected = (selectedFilterType == typeKey)
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilterType = typeKey },
                        label = { Text(label, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
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

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tổng: ${filteredActivities.size} sự kiện chăm sóc",
                    color = Color(0xFF475569),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFEA580C))
                }
            } else if (filteredActivities.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Không tìm thấy nhật ký chăm sóc nào", color = Color(0xFF94A3B8), fontSize = 12.sp)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredActivities) { act ->
                        CareCard(
                            act = act,
                            onClick = { selectedActivity = act }
                        )
                    }
                }
            }
        }
    }

    // Detail Dialog
    selectedActivity?.let { act ->
        CareDetailDialog(
            act = act,
            onDismiss = { selectedActivity = null }
        )
    }

    // Add Care Plan Dialog
    if (showAddCareDialog) {
        AddCarePlanDialog(
            customers = customers,
            currentUser = user,
            onDismiss = { showAddCareDialog = false },
            onAdded = {
                showAddCareDialog = false
                loadData()
            }
        )
    }
}

@Composable
fun CareCard(act: CareActivityItem, onClick: () -> Unit) {
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
                Text(
                    text = act.title,
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    color = when (act.activityType) {
                        "DINNER_NETWORKING" -> Color(0xFF7C3AED)
                        "EXECUTIVE_MEETING" -> Color(0xFF0284C7)
                        "GIFT_DELIVERY" -> Color(0xFFEA580C)
                        "EVENT_INVITATION" -> Color(0xFF059669)
                        else -> Color(0xFF64748B)
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = when (act.activityType) {
                            "DINNER_NETWORKING" -> "🍷 Tiệc tối"
                            "EXECUTIVE_MEETING" -> "💼 Họp cấp cao"
                            "GIFT_DELIVERY" -> "🎁 Tri ân"
                            "EVENT_INVITATION" -> "🏛️ Sự kiện"
                            else -> "📞 Điện đàm"
                        },
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text("Đối tác: ${act.customerName} (${act.sbu})", color = Color(0xFFEA580C), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)

            if (act.projectName.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🏗️ ${act.projectName}", color = Color(0xFF0369A1), fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                    if (act.projectLevel.isNotBlank()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            color = when (act.projectLevel) {
                                "LEVEL_SPECIAL" -> Color(0xFF7C3AED)
                                "LEVEL_1" -> Color(0xFFDC2626)
                                "LEVEL_2" -> Color(0xFFEA580C)
                                "LEVEL_3" -> Color(0xFF2563EB)
                                else -> Color(0xFF059669)
                            },
                            shape = RoundedCornerShape(3.dp)
                        ) {
                            Text(
                                text = when (act.projectLevel) {
                                    "LEVEL_SPECIAL" -> "Cấp ĐB"
                                    "LEVEL_1" -> "Cấp 1"
                                    "LEVEL_2" -> "Cấp 2"
                                    "LEVEL_3" -> "Cấp 3"
                                    else -> "Cấp 4"
                                },
                                color = Color.White,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            if (act.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = act.content,
                    color = Color(0xFF334155),
                    fontSize = 11.sp,
                    maxLines = 2
                )
            }

            if (act.cost > 0) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Chi phí: ${DecimalFormat("#,###").format(act.cost)} VNĐ",
                    color = Color(0xFFB45309),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (act.approverAuthority.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "⚖️ Duyệt: ${act.approverAuthority}",
                    color = Color(0xFF9A3412),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Lãnh đạo: ${act.leaderInCharge}", color = Color(0xFF64748B), fontSize = 10.sp)
                Text(act.occurredAt, color = Color(0xFFEA580C), fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun CareDetailDialog(act: CareActivityItem, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(act.title, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 16.sp)
                Text("Đối tác: ${act.customerName} | Khối: ${act.sbu}", color = Color(0xFFEA580C), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Hình thức:", color = Color(0xFF64748B), fontSize = 12.sp)
                    Text(
                        when (act.activityType) {
                            "DINNER_NETWORKING" -> "Bữa tối thân mật / Giao lưu"
                            "EXECUTIVE_MEETING" -> "Họp chiến lược cấp cao"
                            "GIFT_DELIVERY" -> "Gửi quà tri ân / Chúc mừng"
                            "EVENT_INVITATION" -> "Mời dự sự kiện / Khởi công"
                            else -> "Điện đàm ngoại giao"
                        },
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                if (act.projectName.isNotBlank()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Dự án liên quan:", color = Color(0xFF64748B), fontSize = 12.sp)
                        Text(act.projectName, color = Color(0xFF0369A1), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Ngày diễn ra:", color = Color(0xFF64748B), fontSize = 12.sp)
                    Text(act.occurredAt, color = Color(0xFF0F172A), fontSize = 12.sp)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Lãnh đạo tham dự:", color = Color(0xFF64748B), fontSize = 12.sp)
                    Text(act.leaderInCharge, color = Color(0xFFEA580C), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                if (act.cost > 0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Chi phí thực hiện:", color = Color(0xFF64748B), fontSize = 12.sp)
                        Text("${DecimalFormat("#,###").format(act.cost)} VNĐ", color = Color(0xFFEA580C), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                if (act.approverAuthority.isNotBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFFED7AA), RoundedCornerShape(6.dp))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("⚖️ Thẩm quyền phê duyệt chi phí CSKH:", color = Color(0xFF7C2D12), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text(act.approverAuthority, color = Color(0xFFEA580C), fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text("Nội dung & Cam kết đạt được:", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 13.sp)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = act.content.ifEmpty { "Không có ghi chú nội dung chi tiết." },
                        color = Color(0xFF334155),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Đóng", color = Color(0xFFEA580C), fontWeight = FontWeight.Bold) }
        },
        containerColor = Color.White
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCarePlanDialog(
    customers: List<CustomerItem>,
    currentUser: UserSession,
    onDismiss: () -> Unit,
    onAdded: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var customerList by remember { mutableStateOf(customers) }
    var selectedCustomer by remember { mutableStateOf<CustomerItem?>(customers.firstOrNull()) }
    var allProjects by remember { mutableStateOf<List<ProjectItem>>(emptyList()) }
    var selectedProject by remember { mutableStateOf<ProjectItem?>(null) }
    var showPartnerPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val pRes = ApiClient.fetchProjects("ALL")
        allProjects = pRes.getOrNull() ?: emptyList()

        if (customerList.isEmpty()) {
            val res = ApiClient.fetchCustomers("ALL")
            val list = res.getOrNull() ?: emptyList()
            if (list.isNotEmpty()) {
                customerList = list
                if (selectedCustomer == null) {
                    selectedCustomer = list.firstOrNull()
                }
            }
        } else if (selectedCustomer == null) {
            selectedCustomer = customerList.firstOrNull()
        }
    }

    val customerProjects = remember(selectedCustomer, allProjects) {
        if (selectedCustomer == null) emptyList()
        else allProjects.filter { it.customerId == selectedCustomer!!.id }
    }

    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var activityType by remember { mutableStateOf("DINNER_NETWORKING") }
    var occurredAt by remember { mutableStateOf(today) }
    var title by remember { mutableStateOf("") }
    var leaderInCharge by remember { mutableStateOf("${currentUser.title} - ${currentUser.fullName}") }
    var costText by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var errText by remember { mutableStateOf<String?>(null) }

    val careTypes = listOf(
        "DINNER_NETWORKING" to "🍷 Bữa tối thân mật / Giao lưu",
        "EXECUTIVE_MEETING" to "💼 Họp chiến lược cấp cao",
        "GIFT_DELIVERY" to "🎁 Gửi quà tri ân / Chúc mừng",
        "EVENT_INVITATION" to "🏛️ Mời dự sự kiện / Khởi công",
        "CALL_DISCUSS" to "📞 Trao đổi điện thoại ngoại giao"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Thêm Lịch Chăm Sóc Khách Hàng", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Lập kế hoạch & ghi nhận tiếp đón đối tác chiến lược", color = Color(0xFF64748B), fontSize = 11.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Partner Selection Card
                Text("Đối tác tiếp đón (*):", color = Color(0xFF0F172A), fontSize = 12.sp, fontWeight = FontWeight.Bold)

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFEA580C), RoundedCornerShape(10.dp))
                        .clickable { showPartnerPicker = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedCustomer?.name ?: "👉 Chạm vào đây để chọn Đối tác...",
                                color = if (selectedCustomer != null) Color(0xFF0F172A) else Color(0xFFEA580C),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            if (selectedCustomer != null) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Đại diện: ${selectedCustomer!!.keyDecisionMaker} (${selectedCustomer!!.decisionMakerRole}) • Khối: ${selectedCustomer!!.sbu}",
                                    color = Color(0xFF9A3412),
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Surface(
                            color = Color(0xFFEA580C),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "CHỌN",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Linked Project Selection (Optional)
                if (customerProjects.isNotEmpty()) {
                    Text("Gắn với Dự án cụ thể (Tùy chọn):", color = Color(0xFF0F172A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            val isNone = (selectedProject == null)
                            FilterChip(
                                selected = isNone,
                                onClick = { selectedProject = null },
                                label = { Text("Không gắn DA", fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0284C7),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        items(customerProjects) { proj ->
                            val isSel = (selectedProject?.id == proj.id)
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedProject = proj },
                                label = { Text("${proj.code} (${proj.projectLevelName})", fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFEA580C),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // Authority Preview Card
                val authorityText = selectedProject?.approverAuthority ?: selectedCustomer?.inChargeExecutive ?: "Phó Tổng Giám đốc phụ trách các mảng SBU"
                Surface(
                    color = Color(0xFFFFF7ED),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFFED7AA), RoundedCornerShape(8.dp))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = if (selectedProject != null) "⚖️ Thẩm quyền duyệt theo Dự án (${selectedProject!!.projectLevelName}):" else "⚖️ Thẩm quyền duyệt theo Hạng CĐT:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7C2D12)
                        )
                        Text(
                            text = authorityText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFEA580C)
                        )
                    }
                }

                // Care Type Selection
                Text("Hình thức ngoại giao:", color = Color(0xFF0F172A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(careTypes) { (typeKey, label) ->
                        val isSelected = (activityType == typeKey)
                        FilterChip(
                            selected = isSelected,
                            onClick = { activityType = typeKey },
                            label = { Text(label, fontSize = 10.sp) },
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

                // Date
                OutlinedTextField(
                    value = occurredAt,
                    onValueChange = { occurredAt = it },
                    label = { Text("Ngày diễn ra (YYYY-MM-DD) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF334155),
                        focusedBorderColor = Color(0xFFEA580C),
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tiêu đề buổi gặp / Tiếp đón (*)") },
                    placeholder = { Text("VD: Bữa tối thân mật với TGĐ Masterise...", color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF334155),
                        focusedBorderColor = Color(0xFFEA580C),
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )

                // Leader in charge
                OutlinedTextField(
                    value = leaderInCharge,
                    onValueChange = { leaderInCharge = it },
                    label = { Text("Lãnh đạo FECON tham dự (*)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF334155),
                        focusedBorderColor = Color(0xFFEA580C),
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )

                // Cost (VNĐ)
                OutlinedTextField(
                    value = costText,
                    onValueChange = { costText = it },
                    label = { Text("Chi phí thực hiện (VNĐ)") },
                    placeholder = { Text("VD: 5000000 (Trừ vào ngân sách năm)", color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF334155),
                        focusedBorderColor = Color(0xFFEA580C),
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )

                // Content
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Nội dung trao đổi & Kết quả cam kết") },
                    placeholder = { Text("Ghi nhận các điểm thống nhất, định hướng hợp tác...", color = Color.Gray) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF334155),
                        focusedBorderColor = Color(0xFFEA580C),
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )

                errText?.let {
                    Text(it, color = Color(0xFFDC2626), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedCustomer == null) {
                        errText = "Vui lòng chọn Đối tác tiếp đón!"
                        return@Button
                    }
                    if (title.isBlank()) {
                        errText = "Vui lòng nhập tiêu đề buổi tiếp đón!"
                        return@Button
                    }
                    isSaving = true
                    errText = null
                    val parsedCost = costText.replace(",", "").replace(".", "").trim().toDoubleOrNull() ?: 0.0
                    coroutineScope.launch {
                        val res = ApiClient.logCareActivity(
                            customerId = selectedCustomer!!.id,
                            sbu = selectedCustomer!!.sbu,
                            activityType = activityType,
                            title = title.trim(),
                            content = content.trim(),
                            occurredAt = occurredAt.trim(),
                            leaderInCharge = leaderInCharge.trim(),
                            outcomeStatus = "SUCCESS",
                            cost = parsedCost,
                            projectId = selectedProject?.id
                        )
                        isSaving = false
                        if (res.isSuccess) {
                            onAdded()
                        } else {
                            errText = res.exceptionOrNull()?.message ?: "Lỗi khi lưu lịch chăm sóc khách hàng"
                        }
                    }
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C))
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                } else {
                    Text("Lưu Kế Hoạch", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy", color = Color(0xFF64748B)) }
        },
        containerColor = Color.White
    )

    // Full Partner Picker Dialog with Search Bar
    if (showPartnerPicker) {
        PartnerPickerDialog(
            customers = customerList,
            selectedCustomerId = selectedCustomer?.id,
            onSelect = { cust ->
                selectedCustomer = cust
                showPartnerPicker = false
                errText = null
            },
            onDismiss = { showPartnerPicker = false }
        )
    }
}

@Composable
fun PartnerPickerDialog(
    customers: List<CustomerItem>,
    selectedCustomerId: Int?,
    onSelect: (CustomerItem) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = customers.filter {
        searchQuery.isBlank() ||
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.keyDecisionMaker.contains(searchQuery, ignoreCase = true) ||
                it.sbu.contains(searchQuery, ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Chọn Đối Tác Khách Hàng", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Tìm kiếm theo tên công ty, người đại diện hoặc khối SBU", color = Color(0xFF64748B), fontSize = 11.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Tìm kiếm đối tác...") },
                    leadingIcon = { Text("🔍", fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF334155),
                        focusedBorderColor = Color(0xFFEA580C),
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Không tìm thấy đối tác phù hợp", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filtered) { c ->
                            val isSelected = (c.id == selectedCustomerId)
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFFFFF7ED) else Color(0xFFF8FAFC)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                        color = if (isSelected) Color(0xFFEA580C) else Color(0xFFE2E8F0),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onSelect(c) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = c.name,
                                            color = Color(0xFF0F172A),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Đại diện: ${c.keyDecisionMaker} (${c.decisionMakerRole})",
                                            color = Color(0xFF475569),
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = "Khối: ${c.sbu} • Phân khúc: ${c.segment}",
                                            color = Color(0xFFEA580C),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    if (isSelected) {
                                        Text("✓", color = Color(0xFFEA580C), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
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
