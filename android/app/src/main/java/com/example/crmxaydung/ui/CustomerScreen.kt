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
import com.example.crmxaydung.data.UserSession
import kotlinx.coroutines.launch
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerScreen(user: UserSession) {
    val coroutineScope = rememberCoroutineScope()
    var customers by remember { mutableStateOf<List<CustomerItem>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTierFilter by remember { mutableStateOf("ALL") }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCustomer by remember { mutableStateOf<CustomerItem?>(null) }
    var careCustomer by remember { mutableStateOf<CustomerItem?>(null) }
    var assessCustomer by remember { mutableStateOf<CustomerItem?>(null) }

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

    val tierFilters = listOf(
        "ALL" to "Tất Cả",
        "DIAMOND" to "💎 Kim Cương",
        "GOLD" to "🥇 Vàng",
        "SILVER" to "🥈 Bạc"
    )

    val filteredList = customers.filter {
        (selectedTierFilter == "ALL" || it.tier == selectedTierFilter) &&
        (searchQuery.isBlank() ||
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.code.contains(searchQuery, ignoreCase = true) ||
                it.keyDecisionMaker.contains(searchQuery, ignoreCase = true) ||
                it.phone.contains(searchQuery) ||
                it.decisionMakerPhone.contains(searchQuery))
    }

    val df = DecimalFormat("#,##0.#")
    val currencyDf = DecimalFormat("#,###")

    Scaffold(
        containerColor = Color(0xFFF8FAFC), // Nền sáng
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFFEA580C),
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
            // Search Input (Light Theme)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Tìm theo mã, tên DN, người đại diện...") },
                leadingIcon = { Text("🔍", fontSize = 14.sp) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp)),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF334155),
                    focusedBorderColor = Color(0xFFEA580C),
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Tier Filter Chips (CSCSKH/ĐT-01)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(tierFilters) { (tierKey, label) ->
                    val isSelected = (selectedTierFilter == tierKey)
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTierFilter = tierKey },
                        label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (tierKey) {
                                "DIAMOND" -> Color(0xFF0891B2)
                                "GOLD" -> Color(0xFFD97706)
                                "SILVER" -> Color(0xFF64748B)
                                else -> Color(0xFFEA580C)
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tổng: ${filteredList.size} Khách hàng / CĐT",
                    color = Color(0xFF475569),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = { loadCustomers() }) {
                    Text("Làm mới", color = Color(0xFFEA580C), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFEA580C))
                }
            } else if (filteredList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Không tìm thấy khách hàng phù hợp", color = Color(0xFF94A3B8))
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredList) { cust ->
                        CustomerCard(
                            cust = cust,
                            df = df,
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
            currencyDf = currencyDf,
            onDismiss = { selectedCustomer = null },
            onLogCare = {
                val target = selectedCustomer
                selectedCustomer = null
                careCustomer = target
            },
            onAssess = {
                val target = selectedCustomer
                selectedCustomer = null
                assessCustomer = target
            }
        )
    }

    // Customer Assessment Dialog (FECON CSCSKH/ĐT-01)
    assessCustomer?.let { cust ->
        CustomerAssessDialog(
            cust = cust,
            onDismiss = { assessCustomer = null },
            onAssessed = {
                assessCustomer = null
                loadCustomers()
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
fun CustomerCard(cust: CustomerItem, df: DecimalFormat, onClick: () -> Unit) {
    val tierColor = when (cust.tier) {
        "DIAMOND" -> Color(0xFF0891B2)
        "GOLD" -> Color(0xFFD97706)
        "SILVER" -> Color(0xFF64748B)
        else -> Color(0xFF0284C7)
    }
    val tierLabel = when (cust.tier) {
        "DIAMOND" -> "💎 KIM CƯƠNG"
        "GOLD" -> "🥇 VÀNG"
        "SILVER" -> "🥈 BẠC"
        else -> cust.tier
    }

    val spentMil = cust.spentCareBudget / 1_000_000.0
    val budgetMil = cust.annualCareBudget / 1_000_000.0
    val progress = if (cust.annualCareBudget > 0) (cust.spentCareBudget / cust.annualCareBudget).coerceIn(0.0, 1.0).toFloat() else 0f

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
            // Header Row: Customer Name & Tier Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = cust.name,
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    color = tierColor,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = tierLabel,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Score & Rule Tags
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(4.dp))
                ) {
                    Text(
                        text = "Điểm: ${df.format(cust.totalScore)} / 100đ",
                        color = Color(0xFF0284C7),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (cust.vetoApplied) {
                    Surface(
                        color = Color(0xFFFEE2E2),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(4.dp))
                    ) {
                        Text(
                            text = "⚠️ Phủ quyết TC3",
                            color = Color(0xFFB91C1C),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (cust.isSpecialElevated) {
                    Surface(
                        color = Color(0xFFF3E8FF),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.border(1.dp, Color(0xFFD8B4FE), RoundedCornerShape(4.dp))
                    ) {
                        Text(
                            text = "⭐ Đặc cách TGĐ",
                            color = Color(0xFF7E22CE),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Đại diện: ${cust.keyDecisionMaker} (${cust.decisionMakerRole})",
                color = Color(0xFF334155),
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Annual Care Budget Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Ngân sách CSKH: ${df.format(spentMil)}M / ${df.format(budgetMil)}M VNĐ/năm",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    color = if (progress >= 1f) Color(0xFFDC2626) else Color(0xFFEA580C),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = if (progress >= 1f) Color(0xFFDC2626) else Color(0xFFEA580C),
                trackColor = Color(0xFFE2E8F0)
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Mã: ${cust.code} | Khối: ${cust.sbu}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                Text(cust.phone.ifEmpty { cust.decisionMakerPhone }, color = Color(0xFF0284C7), fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun CustomerDetailDialog(
    cust: CustomerItem,
    df: DecimalFormat,
    currencyDf: DecimalFormat,
    onDismiss: () -> Unit,
    onLogCare: () -> Unit,
    onAssess: () -> Unit
) {
    val context = LocalContext.current
    val contactPhone = cust.decisionMakerPhone.ifEmpty { cust.phone }

    val tierColor = when (cust.tier) {
        "DIAMOND" -> Color(0xFF0891B2)
        "GOLD" -> Color(0xFFD97706)
        "SILVER" -> Color(0xFF64748B)
        else -> Color(0xFF0284C7)
    }
    val tierLabel = when (cust.tier) {
        "DIAMOND" -> "💎 KIM CƯƠNG"
        "GOLD" -> "🥇 VÀNG"
        "SILVER" -> "🥈 BẠC"
        else -> cust.tier
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = cust.name,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(color = tierColor, shape = RoundedCornerShape(4.dp)) {
                        Text(
                            text = tierLabel,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text("Mã KH: ${cust.code} | Khối: ${cust.sbu}", color = Color(0xFF64748B), fontSize = 12.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Section: CSKH FECON Policy Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.border(1.dp, Color(0xFFFED7AA), RoundedCornerShape(10.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🏆 Chính Sách CSKH FECON", fontWeight = FontWeight.Bold, color = Color(0xFF9A3412), fontSize = 13.sp)
                            Text("Tổng: ${df.format(cust.totalScore)} / 100đ", fontWeight = FontWeight.Bold, color = Color(0xFFEA580C), fontSize = 12.sp)
                        }

                        if (cust.vetoApplied) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(color = Color(0xFFFEE2E2), shape = RoundedCornerShape(6.dp), modifier = Modifier.border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(6.dp))) {
                                Text(
                                    text = "⚠️ Áp dụng quy tắc phủ quyết: Năng lực tài chính & dòng tiền = 0 điểm. Giới hạn tối đa Hạng Vàng.",
                                    color = Color(0xFFB91C1C),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        }

                        if (cust.isSpecialElevated) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(color = Color(0xFFF3E8FF), shape = RoundedCornerShape(6.dp), modifier = Modifier.border(1.dp, Color(0xFFD8B4FE), RoundedCornerShape(6.dp))) {
                                Text(
                                    text = "⭐ Phê duyệt đặc cách: Chủ tịch HĐQT / TGĐ phê duyệt nâng Hạng Kim Cương.",
                                    color = Color(0xFF7E22CE),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("• Ngân sách năm: ${currencyDf.format(cust.annualCareBudget)} VNĐ", color = Color(0xFFEA580C), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("• Đã sử dụng: ${currencyDf.format(cust.spentCareBudget)} VNĐ", color = Color(0xFF334155), fontSize = 12.sp)
                        Text("• Lãnh đạo phụ trách: ${cust.inChargeExecutive}", color = Color(0xFF334155), fontSize = 12.sp)
                        Text("• Tần suất chăm sóc: ${cust.careFrequency}", color = Color(0xFF334155), fontSize = 12.sp)

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Bảng điểm 5 tiêu chí:", fontWeight = FontWeight.Bold, color = Color(0xFF475569), fontSize = 11.sp)
                        Text("1. Quy mô Đối tác/DA: ${df.format(cust.scoreScaleProject)} / 15đ", color = Color(0xFF334155), fontSize = 11.sp)
                        Text("2. Loại hình & Phù hợp: ${df.format(cust.scoreFeconFit)} / 25đ", color = Color(0xFF334155), fontSize = 11.sp)
                        Text("3. Năng lực TC & Dòng tiền: ${df.format(cust.scoreFinancialCapacity)} / 25đ (Tiêu chí phủ quyết)", color = if (cust.scoreFinancialCapacity == 0.0) Color(0xFFDC2626) else Color(0xFF334155), fontSize = 11.sp)
                        Text("4. Lịch sử HT & Thanh toán: ${df.format(cust.scoreCooperationHistory)} / 20đ", color = Color(0xFF334155), fontSize = 11.sp)
                        Text("5. Năng lực quản lý: ${df.format(cust.scoreManagementCapacity)} / 15đ", color = Color(0xFF334155), fontSize = 11.sp)
                    }
                }

                // Section: Personal Decision Maker Card
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)), shape = RoundedCornerShape(10.dp), modifier = Modifier.border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("👤 Hồ Sơ Cá Nhân Người Quyết Định", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Họ và tên: ${cust.keyDecisionMaker}", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 14.sp)
                        Text("Chức danh: ${cust.decisionMakerRole}", color = Color(0xFF475569), fontSize = 12.sp)
                        if (contactPhone.isNotBlank()) {
                            Text("SĐT trực tiếp: $contactPhone", color = Color(0xFF0284C7), fontSize = 12.sp, fontWeight = FontWeight.Medium)
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
                            Text("🎂 Sinh nhật: ${cust.decisionMakerBirthday}", color = Color(0xFFD97706), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Section: Corporate Details
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)), shape = RoundedCornerShape(10.dp), modifier = Modifier.border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("🏢 Thông Tin Doanh Nghiệp & Ngoại Giao", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        if (cust.headquarters.isNotBlank()) {
                            Text("Trụ sở: ${cust.headquarters}", color = Color(0xFF475569), fontSize = 12.sp)
                        }
                        if (cust.taxCode.isNotBlank()) {
                            Text("Mã số thuế: ${cust.taxCode}", color = Color(0xFF475569), fontSize = 12.sp)
                        }
                        if (cust.foundingAnniversary.isNotBlank()) {
                            Text("🏛️ Ngày thành lập: ${cust.foundingAnniversary}", color = Color(0xFF0284C7), fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Điểm quan hệ: ${"⭐".repeat(cust.relationshipScore.coerceIn(1, 5))} (${cust.relationshipStatus})", color = Color(0xFFD97706), fontSize = 12.sp)
                    }
                }

                // Section: Strategic Notes
                if (cust.strategicNotes.isNotBlank()) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)), shape = RoundedCornerShape(10.dp), modifier = Modifier.border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("📝 Ghi Chú Chiến Lược Cá Nhân", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(cust.strategicNotes, color = Color(0xFF475569), fontSize = 12.sp)
                        }
                    }
                }

                // Actions
                Button(
                    onClick = onAssess,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0891B2)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🏆 Chấm Điểm & Phân Hạng FECON", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onLogCare,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🤝 Ghi Nhận Lịch Chăm Sóc Khách Hàng", fontWeight = FontWeight.Bold)
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
fun CustomerAssessDialog(
    cust: CustomerItem,
    onDismiss: () -> Unit,
    onAssessed: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var scoreScale by remember { mutableStateOf(cust.scoreScaleProject) }
    var scoreFit by remember { mutableStateOf(cust.scoreFeconFit) }
    var scoreFinance by remember { mutableStateOf(cust.scoreFinancialCapacity) }
    var scoreHistory by remember { mutableStateOf(cust.scoreCooperationHistory) }
    var scoreMgmt by remember { mutableStateOf(cust.scoreManagementCapacity) }
    var isSpecialElevated by remember { mutableStateOf(cust.isSpecialElevated) }
    var notes by remember { mutableStateOf(cust.strategicNotes) }
    var isSaving by remember { mutableStateOf(false) }
    var errText by remember { mutableStateOf<String?>(null) }

    val totalScore = scoreScale + scoreFit + scoreFinance + scoreHistory + scoreMgmt
    val isVeto = (scoreFinance == 0.0)
    val calculatedTier = if (isSpecialElevated) {
        "DIAMOND"
    } else if (totalScore >= 80.0) {
        if (isVeto) "GOLD" else "DIAMOND"
    } else if (totalScore >= 50.0) {
        "GOLD"
    } else {
        "SILVER"
    }

    val df = DecimalFormat("#,##0.#")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Chấm Điểm & Phân Hạng FECON", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 16.sp)
                Text("Đối tác: ${cust.name} (${cust.code})", color = Color(0xFFEA580C), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Live Assessment Summary Box
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.border(
                        1.dp,
                        when (calculatedTier) {
                            "DIAMOND" -> Color(0xFF0891B2)
                            "GOLD" -> Color(0xFFD97706)
                            else -> Color(0xFF64748B)
                        },
                        RoundedCornerShape(10.dp)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Tổng điểm: ${df.format(totalScore)} / 100đ", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 14.sp)
                            Surface(
                                color = when (calculatedTier) {
                                    "DIAMOND" -> Color(0xFF0891B2)
                                    "GOLD" -> Color(0xFFD97706)
                                    else -> Color(0xFF64748B)
                                },
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = when (calculatedTier) {
                                        "DIAMOND" -> "💎 KIM CƯƠNG"
                                        "GOLD" -> "🥇 VÀNG"
                                        else -> "🥈 BẠC"
                                    },
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when (calculatedTier) {
                                "DIAMOND" -> "• Ngân sách: 80 Triệu VNĐ/năm | CT HĐQT / TGĐ phụ trách"
                                "GOLD" -> "• Ngân sách: 20 Triệu VNĐ/năm | TGĐ / SBU Leader phụ trách"
                                else -> "• Ngân sách: 5 Triệu VNĐ/năm | SBU Leader / GĐKD phụ trách"
                            },
                            color = Color(0xFF475569),
                            fontSize = 11.sp
                        )

                        if (isVeto && !isSpecialElevated) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(color = Color(0xFFFEE2E2), shape = RoundedCornerShape(4.dp), modifier = Modifier.border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(4.dp))) {
                                Text(
                                    text = "⚠️ QUY TẮC PHỦ QUYẾT: Tiêu chí 3 (Tài chính) = 0đ. Khách hàng bị hạ tối đa Hạng Vàng dù tổng điểm đạt ${df.format(totalScore)}đ!",
                                    color = Color(0xFFB91C1C),
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        }
                    }
                }

                // TC 1: Quy mô Đối tác / Dự án (Max 15)
                CriteriaSection(
                    title = "1. Quy mô Đối tác / Dự án (Tối đa 15đ)",
                    options = listOf(
                        15.0 to "Tốt (15đ) - DA cấp đặc biệt / CĐT hàng đầu",
                        7.5 to "TB (7.5đ) - DA cấp I/II, quy mô khá",
                        0.0 to "Tiềm năng (0đ) - Quy mô vừa & nhỏ"
                    ),
                    selectedScore = scoreScale,
                    onSelect = { scoreScale = it }
                )

                // TC 2: Loại hình & Phù hợp FECON (Max 25)
                CriteriaSection(
                    title = "2. Loại hình ĐT & Phù hợp FECON (Tối đa 25đ)",
                    options = listOf(
                        25.0 to "Tốt (25đ) - Hạ tầng/Móng/Ngầm/NL trọng tâm",
                        12.5 to "TB (12.5đ) - Xây dựng hỗn hợp/Dân dụng",
                        0.0 to "Chưa phù hợp (0đ) - Ngoài thế mạnh FECON"
                    ),
                    selectedScore = scoreFit,
                    onSelect = { scoreFit = it }
                )

                // TC 3: Năng lực TC & Dòng tiền (Max 25) - VETO RULE!
                CriteriaSection(
                    title = "3. Năng lực TC & Dòng tiền (Tối đa 25đ) ⚠️ PHỦ QUYẾT",
                    options = listOf(
                        25.0 to "Tốt (25đ) - Vốn rõ ràng, bảo lãnh mạnh, thanh toán chuẩn",
                        12.5 to "TB (12.5đ) - Phụ thuộc tín dụng, giải ngân định kỳ",
                        0.0 to "Rủi ro (0đ) - Nợ đọng, dòng tiền yếu [PHỦ QUYẾT TỐI ĐA VÀNG]"
                    ),
                    selectedScore = scoreFinance,
                    onSelect = { scoreFinance = it },
                    isVetoNotice = true
                )

                // TC 4: Lịch sử hợp tác & Thanh toán (Max 20)
                CriteriaSection(
                    title = "4. Lịch sử hợp tác & Thanh toán (Tối đa 20đ)",
                    options = listOf(
                        20.0 to "Tốt (20đ) - Đã hợp tác nhiều gói thầu, uy tín cao",
                        10.0 to "TB (10đ) - Từng chậm tiến độ TT nhưng đã khắc phục",
                        0.0 to "Mới (0đ) - Chưa từng hợp tác với FECON"
                    ),
                    selectedScore = scoreHistory,
                    onSelect = { scoreHistory = it }
                )

                // TC 5: Năng lực quản lý & Chuyên nghiệp (Max 15)
                CriteriaSection(
                    title = "5. Năng lực QLDA & Tính chuyên nghiệp (Tối đa 15đ)",
                    options = listOf(
                        15.0 to "Tốt (15đ) - Ban QLDA chuyên nghiệp, nghiệm thu nhanh",
                        7.5 to "TB (7.5đ) - Thủ tục hành chính chậm, hay đổi NS",
                        0.0 to "Kém (0đ) - Phức tạp, khó phối hợp công trường"
                    ),
                    selectedScore = scoreMgmt,
                    onSelect = { scoreMgmt = it }
                )

                // Special Elevation Checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isSpecialElevated = !isSpecialElevated }
                ) {
                    Checkbox(
                        checked = isSpecialElevated,
                        onCheckedChange = { isSpecialElevated = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF7C3AED))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "⭐ Phê duyệt đặc cách của CT HĐQT / TGĐ (Nâng Hạng Kim Cương)",
                        color = if (isSpecialElevated) Color(0xFF7E22CE) else Color(0xFF475569),
                        fontSize = 12.sp,
                        fontWeight = if (isSpecialElevated) FontWeight.Bold else FontWeight.Normal
                    )
                }

                // Strategic Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Ghi chú đánh giá / Lý do phân hạng") },
                    placeholder = { Text("VD: Đối tác chiến lược tiềm năng cao cho dự án điện gió...", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF334155),
                        focusedBorderColor = Color(0xFF0891B2),
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
                    isSaving = true
                    errText = null
                    coroutineScope.launch {
                        val res = ApiClient.assessCustomer(
                            customerId = cust.id,
                            scoreScale = scoreScale,
                            scoreFit = scoreFit,
                            scoreFinance = scoreFinance,
                            scoreHistory = scoreHistory,
                            scoreMgmt = scoreMgmt,
                            isSpecialElevated = isSpecialElevated,
                            notes = notes.trim()
                        )
                        isSaving = false
                        if (res.isSuccess) {
                            onAssessed()
                        } else {
                            errText = res.exceptionOrNull()?.message ?: "Lỗi khi lưu đánh giá"
                        }
                    }
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0891B2))
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                } else {
                    Text("Lưu Đánh Giá", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy", color = Color(0xFF64748B)) }
        },
        containerColor = Color.White
    )
}

@Composable
fun CriteriaSection(
    title: String,
    options: List<Pair<Double, String>>,
    selectedScore: Double,
    onSelect: (Double) -> Unit,
    isVetoNotice: Boolean = false
) {
    Column {
        Text(
            text = title,
            color = if (isVetoNotice) Color(0xFFD97706) else Color(0xFF0F172A),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            options.forEach { (score, label) ->
                val isSelected = (selectedScore == score)
                Surface(
                    color = if (isSelected) Color(0xFFE0F2FE) else Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (isSelected) Color(0xFF0284C7) else Color(0xFFE2E8F0),
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onSelect(score) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelect(score) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFF0284C7),
                                unselectedColor = Color(0xFF94A3B8)
                            ),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            color = if (isSelected) Color(0xFF0369A1) else Color(0xFF334155),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
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
    var costText by remember { mutableStateOf("") }
    var activityType by remember { mutableStateOf("EXECUTIVE_MEETING") }
    var isSaving by remember { mutableStateOf(false) }
    var errText by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val types = listOf(
        "EXECUTIVE_MEETING" to "💼 Họp Cấp Cao",
        "DINNER_NETWORKING" to "🍷 Tiệc Giao Lưu",
        "GIFT_DELIVERY" to "🎁 Quà Tri Ân",
        "CALL_DISCUSS" to "📞 Điện Đàm",
        "EVENT_INVITATION" to "🏛️ Mời Sự Kiện"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ghi Lịch Chăm Sóc Khách Hàng", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text("Khách hàng: ${customer.name}", color = Color(0xFFEA580C), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("Đại diện: ${customer.keyDecisionMaker} (${customer.decisionMakerRole})", color = Color(0xFF334155), fontSize = 12.sp)

                Text("Hình thức chăm sóc:", color = Color(0xFF64748B), fontSize = 11.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(types) { (key, label) ->
                        val isSelected = (activityType == key)
                        FilterChip(
                            selected = isSelected,
                            onClick = { activityType = key },
                            label = { Text(label, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFEA580C),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFFF8FAFC),
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

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tiêu đề sự kiện chăm sóc (*)") },
                    placeholder = { Text("VD: Bữa tối thân mật cùng Chủ tịch...", color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF334155),
                        focusedBorderColor = Color(0xFFEA580C),
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )

                OutlinedTextField(
                    value = costText,
                    onValueChange = { costText = it },
                    label = { Text("Chi phí thực hiện (VNĐ)") },
                    placeholder = { Text("VD: 5000000 (Khấu trừ vào ngân sách năm)", color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF334155),
                        focusedBorderColor = Color(0xFFEA580C),
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Nội dung chi tiết trao đổi & Cam kết") },
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
                    if (title.isNotBlank()) {
                        isSaving = true
                        errText = null
                        val parsedCost = costText.replace(",", "").replace(".", "").trim().toDoubleOrNull() ?: 0.0
                        scope.launch {
                            val res = ApiClient.logCareActivity(
                                customerId = customer.id,
                                sbu = customer.sbu,
                                activityType = activityType,
                                title = title.trim(),
                                content = content.trim(),
                                occurredAt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
                                leaderInCharge = currentUser.fullName,
                                outcomeStatus = "SUCCESS",
                                cost = parsedCost
                            )
                            isSaving = false
                            if (res.isSuccess) {
                                onLogged()
                            } else {
                                errText = res.exceptionOrNull()?.message ?: "Lỗi ghi nhật ký"
                            }
                        }
                    }
                },
                enabled = !isSaving && title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C))
            ) {
                if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                else Text("Lưu Lịch Chăm Sóc", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy", color = Color(0xFF64748B)) }
        },
        containerColor = Color.White
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
        title = { Text("Thêm Khách Hàng / CĐT Mới", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold) },
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
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color(0xFF0F172A), unfocusedTextColor = Color(0xFF334155))
                )
                OutlinedTextField(
                    value = keyPerson,
                    onValueChange = { keyPerson = it },
                    label = { Text("Người quyết định cá nhân (*)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color(0xFF0F172A), unfocusedTextColor = Color(0xFF334155))
                )
                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("Chức vụ") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color(0xFF0F172A), unfocusedTextColor = Color(0xFF334155))
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Số điện thoại cá nhân (*)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color(0xFF0F172A), unfocusedTextColor = Color(0xFF334155))
                )
                OutlinedTextField(
                    value = headquarters,
                    onValueChange = { headquarters = it },
                    label = { Text("Trụ sở chính") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color(0xFF0F172A), unfocusedTextColor = Color(0xFF334155))
                )
                OutlinedTextField(
                    value = birthday,
                    onValueChange = { birthday = it },
                    label = { Text("Sinh nhật (YYYY-MM-DD)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color(0xFF0F172A), unfocusedTextColor = Color(0xFF334155))
                )
                OutlinedTextField(
                    value = anniversary,
                    onValueChange = { anniversary = it },
                    label = { Text("Ngày thành lập (YYYY-MM-DD)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color(0xFF0F172A), unfocusedTextColor = Color(0xFF334155))
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Ghi chú chiến lược cá nhân") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color(0xFF0F172A), unfocusedTextColor = Color(0xFF334155))
                )

                errText?.let {
                    Text(it, color = Color(0xFFDC2626), fontSize = 12.sp)
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
                                tier = "GOLD",
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C))
            ) {
                if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                else Text("Lưu Khách Hàng", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy", color = Color(0xFF64748B)) }
        },
        containerColor = Color.White
    )
}
