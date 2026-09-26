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
import com.example.crmxaydung.data.ProjectItem
import com.example.crmxaydung.data.UserSession
import kotlinx.coroutines.launch
import java.text.DecimalFormat

@Composable
fun ProjectScreen(user: UserSession) {
    val coroutineScope = rememberCoroutineScope()
    var projects by remember { mutableStateOf<List<ProjectItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun loadProjects() {
        isLoading = true
        coroutineScope.launch {
            val res = ApiClient.fetchProjects(if (user.role == "ADMIN") "ALL" else user.sbu)
            projects = res.getOrNull() ?: emptyList()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadProjects()
    }

    val df = DecimalFormat("#,##0.0")

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
                text = "🏗️ Theo Dõi Tiến Độ & Dòng Tiền",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = { loadProjects() }) {
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
                    ProjectCard(proj = proj, df = df)
                }
            }
        }
    }
}

@Composable
fun ProjectCard(proj: ProjectItem, df: DecimalFormat) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
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
                    color = Color(0xFF0284C7),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = proj.sbu,
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
        }
    }
}
