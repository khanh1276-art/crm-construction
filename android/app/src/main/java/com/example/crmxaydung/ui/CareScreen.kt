package com.example.crmxaydung.ui

import androidx.compose.foundation.background
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
import com.example.crmxaydung.data.CareActivityItem
import com.example.crmxaydung.data.UserSession
import kotlinx.coroutines.launch

@Composable
fun CareScreen(user: UserSession) {
    val coroutineScope = rememberCoroutineScope()
    var activities by remember { mutableStateOf<List<CareActivityItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun loadActivities() {
        isLoading = true
        coroutineScope.launch {
            val res = ApiClient.fetchCareActivities(if (user.role == "ADMIN") "ALL" else user.sbu)
            activities = res.getOrNull() ?: emptyList()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadActivities()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🤝 Chăm Sóc Đối Tác Chiến Lược",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = { loadActivities() }) {
                Text("Làm mới", color = Color(0xFF38BDF8), fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF38BDF8))
            }
        } else if (activities.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Chưa có sự kiện chăm sóc nào", color = Color.Gray)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(activities) { act ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = act.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    color = Color(0xFF166534),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = act.sbu,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Khách hàng: ${act.customerName}", color = Color(0xFF38BDF8), fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Lãnh đạo phụ trách: ${act.leaderInCharge}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                Text(act.occurredAt, color = Color(0xFF64748B), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
