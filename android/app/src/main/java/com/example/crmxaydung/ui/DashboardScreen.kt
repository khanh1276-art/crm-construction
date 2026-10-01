package com.example.crmxaydung.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.crmxaydung.data.DashboardStats
import com.example.crmxaydung.data.PipelineBidItem
import com.example.crmxaydung.data.UserSession
import kotlinx.coroutines.launch
import java.text.DecimalFormat

@Composable
fun DashboardScreen(
    user: UserSession,
    selectedSbu: String = "ALL",
    onNavigateToCustomers: () -> Unit = {},
    onNavigateToActiveBids: () -> Unit = {},
    onNavigateToWonBids: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var stats by remember { mutableStateOf<DashboardStats?>(null) }
    var pipelineBids by remember { mutableStateOf<List<PipelineBidItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedBid by remember { mutableStateOf<PipelineBidItem?>(null) }

    fun loadData(sbu: String) {
        isLoading = true
        coroutineScope.launch {
            val statsRes = ApiClient.fetchDashboardStats(sbu)
            val bidsRes = ApiClient.fetchPipelineBids(sbu)
            stats = statsRes.getOrNull() ?: DashboardStats()
            pipelineBids = bidsRes.getOrNull() ?: emptyList()
            isLoading = false
        }
    }

    LaunchedEffect(selectedSbu) {
        loadData(selectedSbu)
    }

    val df = DecimalFormat("#,##0.0")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFEA580C))
            }
        } else {
            val s = stats ?: DashboardStats()
            val totalFunnelBillion = s.activeBidsBillion + s.wonBidsBillion
            val totalBidsCount = s.activeBidsCount + s.wonBidsCount
            val winRatio = if (totalBidsCount > 0) (s.wonBidsCount * 100 / totalBidsCount) else 0

            // 4 KPI Cards Grid (Focusing on Bidding Funnel & Strategic Customers)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                KpiCard(
                    title = "Khách Hàng",
                    value = "${s.totalCustomers}",
                    sub = "Đối tác chiến lược",
                    bgColor = Color(0xFFEFF6FF),
                    borderColor = Color(0xFFBFDBFE),
                    titleColor = Color(0xFF1E40AF),
                    valColor = Color(0xFF1E3A8A),
                    onClick = onNavigateToCustomers,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Gói Thầu Đang Đấu",
                    value = "${s.activeBidsCount}",
                    sub = "${df.format(s.activeBidsBillion)} tỷ trong phễu",
                    bgColor = Color(0xFFFFF7ED),
                    borderColor = Color(0xFFFED7AA),
                    titleColor = Color(0xFF9A3412),
                    valColor = Color(0xFFEA580C),
                    onClick = onNavigateToActiveBids,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                KpiCard(
                    title = "Gói Thầu Đã Trúng",
                    value = "${s.wonBidsCount}",
                    sub = "${df.format(s.wonBidsBillion)} tỷ thành công",
                    bgColor = Color(0xFFECFDF5),
                    borderColor = Color(0xFFA7F3D0),
                    titleColor = Color(0xFF065F46),
                    valColor = Color(0xFF047857),
                    onClick = onNavigateToWonBids,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Tổng Quy Mô Phễu",
                    value = "${df.format(totalFunnelBillion)} tỷ",
                    sub = "Tỷ lệ trúng: $winRatio%",
                    bgColor = Color(0xFFFAF5FF),
                    borderColor = Color(0xFFE9D5FF),
                    titleColor = Color(0xFF6B21A8),
                    valColor = Color(0xFF581C87),
                    onClick = onNavigateToActiveBids,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bidding Opportunities Highlight Section (Replaces construction projects)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToActiveBids() }
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎯 Các Gói Thầu Trọng Điểm Đang Bám Sát",
                    color = Color(0xFF0F172A),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${pipelineBids.count { it.stage != "WON" && it.stage != "LOST" }} gói",
                        color = Color(0xFFEA580C),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("➔", color = Color(0xFFEA580C), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            val activeBids = pipelineBids
                .filter { it.stage != "WON" && it.stage != "LOST" }
                .sortedByDescending { it.estimatedValueBillion }

            if (activeBids.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                ) {
                    Box(modifier = Modifier.padding(14.dp), contentAlignment = Alignment.Center) {
                        Text("Chưa có gói thầu nào trong phân hệ này.", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                }
            } else {
                activeBids.take(6).forEach { bid ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                            .clickable { selectedBid = bid }
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
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                    )
                                }

                                Surface(
                                    color = when (bid.stage) {
                                        "NEGOTIATION" -> Color(0xFFD97706)
                                        "TENDER_PREP" -> Color(0xFF2563EB)
                                        "EVALUATION" -> Color(0xFF0284C7)
                                        else -> Color(0xFF64748B)
                                    },
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = when (bid.stage) {
                                            "NEGOTIATION" -> "🤝 Thương Thảo"
                                            "TENDER_PREP" -> "📑 Lập Hồ Sơ"
                                            "EVALUATION" -> "🔍 Khảo Sát"
                                            else -> "📝 Tiếp Cận"
                                        },
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = bid.projectTitle,
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )

                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Đối tác / CĐT: ${bid.customerName}",
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
                                        fontSize = 9.sp,
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
            }
        }
    }

    selectedBid?.let { bid ->
        PipelineDetailDialog(
            bid = bid,
            df = df,
            onDismiss = { selectedBid = null },
            onUpdated = {
                selectedBid = null
                loadData(selectedSbu)
            }
        )
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    sub: String,
    bgColor: Color,
    borderColor: Color,
    titleColor: Color,
    valColor: Color,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.border(1.dp, borderColor, RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = titleColor, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                if (onClick != null) {
                    Text("➔", color = titleColor.copy(alpha = 0.65f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, color = valColor, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(1.dp))
            Text(sub, color = titleColor.copy(alpha = 0.85f), fontSize = 9.5.sp, fontWeight = FontWeight.Medium)
        }
    }
}
