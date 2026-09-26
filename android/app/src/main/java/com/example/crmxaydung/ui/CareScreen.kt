package com.example.crmxaydung.ui

import androidx.compose.foundation.background
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
import com.example.crmxaydung.data.UserSession
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareScreen(user: UserSession) {
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
            val sbuFilter = if (user.role == "ADMIN") "ALL" else user.sbu
            val resAct = ApiClient.fetchCareActivities(sbuFilter)
            val resCust = ApiClient.fetchCustomers(sbuFilter)
            activities = resAct.getOrNull() ?: emptyList()
            customers = resCust.getOrNull() ?: emptyList()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
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
        containerColor = Color(0xFF0F172A),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddCareDialog = true },
                containerColor = Color(0xFFE11D48), // Rose Red highlight
                contentColor = Color.White
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("➕", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ghi Nhật Ký Tiếp Khách", fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🤝 Kế Hoạch Chăm Sóc & Tiếp Khách",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ghi nhận tiệc tối, họp cấp cao & ngoại giao CĐT",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
                TextButton(onClick = { loadData() }) {
                    Text("Làm mới", color = Color(0xFF38BDF8), fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Type Filter Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(typeFilters) { (typeKey, label) ->
                    FilterChip(
                        selected = (selectedFilterType == typeKey),
                        onClick = { selectedFilterType = typeKey },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFE11D48),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFF94A3B8)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF38BDF8))
                }
            } else if (filteredActivities.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Chưa có nhật ký tiếp khách nào trong mục này", color = Color.Gray, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showAddCareDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                        ) {
                            Text("➕ Thêm Kế Hoạch Tiếp Khách Mới")
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredActivities) { act ->
                        CareActivityCard(
                            act = act,
                            onClick = { selectedActivity = act }
                        )
                    }
                }
            }
        }
    }

    // Detail Dialog of selected care activity
    selectedActivity?.let { act ->
        CareDetailDialog(act = act, onDismiss = { selectedActivity = null })
    }

    // Add Care Plan / Reception Dialog (matching webapp)
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
fun CareActivityCard(act: CareActivityItem, onClick: () -> Unit) {
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
                    text = act.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    color = when (act.activityType) {
                        "DINNER_NETWORKING" -> Color(0xFF9F1239)
                        "EXECUTIVE_MEETING" -> Color(0xFF1E40AF)
                        "GIFT_DELIVERY" -> Color(0xFF854D0E)
                        "EVENT_INVITATION" -> Color(0xFF065F46)
                        else -> Color(0xFF374151)
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = when (act.activityType) {
                            "DINNER_NETWORKING" -> "🍷 Bữa tối"
                            "EXECUTIVE_MEETING" -> "💼 Họp cấp cao"
                            "GIFT_DELIVERY" -> "🎁 Tri ân"
                            "EVENT_INVITATION" -> "🏛️ Sự kiện"
                            else -> "📞 Điện đàm"
                        },
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text("Đối tác: ${act.customerName} (${act.sbu})", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

            if (act.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = act.content,
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Lãnh đạo: ${act.leaderInCharge}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                Text(act.occurredAt, color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Medium)
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
                Text(act.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                Text("Đối tác: ${act.customerName} | Khối: ${act.sbu}", color = Color(0xFF38BDF8), fontSize = 12.sp)
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
                    Text("Hình thức:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Text(
                        when (act.activityType) {
                            "DINNER_NETWORKING" -> "Bữa tối thân mật / Giao lưu"
                            "EXECUTIVE_MEETING" -> "Họp chiến lược cấp cao"
                            "GIFT_DELIVERY" -> "Gửi quà tri ân / Chúc mừng"
                            "EVENT_INVITATION" -> "Mời dự sự kiện / Khởi công"
                            else -> "Điện đàm ngoại giao"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Ngày diễn ra:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Text(act.occurredAt, color = Color.White, fontSize = 12.sp)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Lãnh đạo tham dự:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Text(act.leaderInCharge, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text("Nội dung & Kết quả đạt được:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)), shape = RoundedCornerShape(8.dp)) {
                    Text(
                        text = act.content.ifEmpty { "Không có ghi chú nội dung chi tiết." },
                        color = Color(0xFFE2E8F0),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Đóng", color = Color(0xFF38BDF8)) }
        },
        containerColor = Color(0xFF1E293B)
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
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    var selectedCustomerId by remember { mutableStateOf(customers.firstOrNull()?.id ?: 0) }
    var activityType by remember { mutableStateOf("DINNER_NETWORKING") }
    var occurredAt by remember { mutableStateOf(today) }
    var title by remember { mutableStateOf("") }
    var leaderInCharge by remember { mutableStateOf("${currentUser.title} - ${currentUser.fullName}") }
    var content by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var errText by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val careTypes = listOf(
        "DINNER_NETWORKING" to "🍷 Bữa tối thân mật / Giao lưu",
        "EXECUTIVE_MEETING" to "💼 Họp chiến lược cấp cao",
        "GIFT_DELIVERY" to "🎁 Gửi quà tri ân / Chúc mừng",
        "EVENT_INVITATION" to "🏛️ Mời dự sự kiện / Khởi công",
        "CALL_DISCUSS" to "📞 Trao đổi điện thoại ngoại giao"
    )

    val selectedCustomer = customers.firstOrNull { it.id == selectedCustomerId } ?: customers.firstOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Ghi Nhận Hoạt Động & Kế Hoạch Tiếp Khách", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Giao lưu ngoại giao & tiếp đón đối tác chiến lược", color = Color(0xFF94A3B8), fontSize = 11.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Partner Selection
                Text("Đối tác tiếp đón (*):", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                if (customers.isEmpty()) {
                    Text("Đang tải danh sách khách hàng...", color = Color.Gray, fontSize = 11.sp)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(customers) { c ->
                            FilterChip(
                                selected = (selectedCustomerId == c.id),
                                onClick = { selectedCustomerId = c.id },
                                label = { Text(c.name, fontSize = 11.sp, maxLines = 1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFE11D48),
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFF0F172A),
                                    labelColor = Color(0xFF94A3B8)
                                )
                            )
                        }
                    }
                    selectedCustomer?.let {
                        Text("Đại diện: ${it.keyDecisionMaker} (${it.decisionMakerRole}) • ${it.sbu}", color = Color(0xFF38BDF8), fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Care Type Selection
                Text("Hình thức ngoại giao:", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(careTypes) { (typeKey, label) ->
                        FilterChip(
                            selected = (activityType == typeKey),
                            onClick = { activityType = typeKey },
                            label = { Text(label, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF2563EB),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF0F172A),
                                labelColor = Color(0xFF94A3B8)
                            )
                        )
                    }
                }

                // Date
                OutlinedTextField(
                    value = occurredAt,
                    onValueChange = { occurredAt = it },
                    label = { Text("Ngày diễn ra (YYYY-MM-DD)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tiêu đề buổi gặp (*)") },
                    placeholder = { Text("VD: Bữa tối thân mật với TGĐ Masterise...", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                // Leader in charge
                OutlinedTextField(
                    value = leaderInCharge,
                    onValueChange = { leaderInCharge = it },
                    label = { Text("Lãnh đạo FECON tham dự (*)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                // Content
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Nội dung trao đổi & Kết quả cam kết") },
                    placeholder = { Text("Ghi nhận các điểm thống nhất, nguyên tắc hợp tác các dự án tới...", color = Color.Gray) },
                    minLines = 3,
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
                    if (title.isNotBlank() && selectedCustomer != null) {
                        isSaving = true
                        errText = null
                        scope.launch {
                            val res = ApiClient.logCareActivity(
                                customerId = selectedCustomer.id,
                                sbu = selectedCustomer.sbu,
                                activityType = activityType,
                                title = title.trim(),
                                content = content.trim(),
                                occurredAt = occurredAt.trim(),
                                leaderInCharge = leaderInCharge.trim(),
                                outcomeStatus = "SUCCESS"
                            )
                            isSaving = false
                            if (res.isSuccess) {
                                onAdded()
                            } else {
                                errText = res.exceptionOrNull()?.message ?: "Lỗi lưu hoạt động"
                            }
                        }
                    }
                },
                enabled = !isSaving && title.isNotBlank() && selectedCustomer != null,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
            ) {
                if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                else Text("Lưu Hoạt Động")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy", color = Color.Gray) }
        },
        containerColor = Color(0xFF1E293B)
    )
}
