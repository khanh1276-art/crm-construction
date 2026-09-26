package com.example.crmxaydung.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {
    // Default URL for Android Emulator pointing to host machine port 8088.
    // Can be changed dynamically to LAN IP or Render live URL.
    var baseUrl: String = "http://10.0.2.2:8088"
    var currentUser: UserSession? = null

    suspend fun login(username: String, password: String): Result<UserSession> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/api/users/login")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; utf-8")
                setRequestProperty("Accept", "application/json")
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
            }

            val body = JSONObject().apply {
                put("username", username)
                put("password", password)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val responseText = reader.readText()
                val json = JSONObject(responseText)
                val userObj = json.getJSONObject("user")
                val session = UserSession(
                    id = userObj.getInt("id"),
                    username = userObj.getString("username"),
                    fullName = userObj.getString("full_name"),
                    role = userObj.getString("role"),
                    sbu = userObj.getString("sbu"),
                    title = userObj.optString("title", ""),
                    token = json.optString("token", "")
                )
                currentUser = session
                Result.success(session)
            } else {
                val errReader = BufferedReader(InputStreamReader(conn.errorStream ?: conn.inputStream))
                val errText = errReader.readText()
                val errMsg = try {
                    JSONObject(errText).optString("detail", "Đăng nhập thất bại (Mã lỗi: $responseCode)")
                } catch (_: Exception) {
                    "Đăng nhập thất bại (Mã lỗi: $responseCode)"
                }
                Result.failure(Exception(errMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchDashboardStats(sbu: String = "ALL"): Result<DashboardStats> = withContext(Dispatchers.IO) {
        try {
            val urlStr = if (sbu == "ALL") "$baseUrl/api/dashboard/stats" else "$baseUrl/api/dashboard/stats?sbu=$sbu"
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                connectTimeout = 8000
                readTimeout = 8000
            }
            if (conn.responseCode == 200) {
                val responseText = BufferedReader(InputStreamReader(conn.inputStream)).readText()
                val json = JSONObject(responseText)
                val summary = json.optJSONObject("summary") ?: JSONObject()
                val stats = DashboardStats(
                    totalCustomers = summary.optInt("total_customers", 0),
                    strategicVipCount = summary.optInt("strategic_vip_count", 0),
                    totalProjects = summary.optInt("total_projects", 0),
                    totalContractBillion = summary.optDouble("total_contract_value", 0.0) / 1_000_000_000.0,
                    totalCollectedBillion = summary.optDouble("total_collected", 0.0) / 1_000_000_000.0,
                    avgProgress = summary.optDouble("overall_progress_pct", 0.0),
                    sbuFilter = sbu
                )
                Result.success(stats)
            } else {
                Result.failure(Exception("Lỗi tải báo cáo: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchCustomers(sbu: String = "ALL"): Result<List<CustomerItem>> = withContext(Dispatchers.IO) {
        try {
            val urlStr = if (sbu == "ALL") "$baseUrl/api/customers" else "$baseUrl/api/customers?sbu=$sbu"
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                connectTimeout = 8000
                readTimeout = 8000
            }
            if (conn.responseCode == 200) {
                val responseText = BufferedReader(InputStreamReader(conn.inputStream)).readText()
                val arr = JSONArray(responseText)
                val list = mutableListOf<CustomerItem>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        CustomerItem(
                            id = obj.getInt("id"),
                            code = obj.optString("code", ""),
                            name = obj.getString("name"),
                            sbu = obj.optString("sbu", ""),
                            tier = obj.optString("tier", "STRATEGIC_VIP"),
                            phone = obj.optString("phone", ""),
                            email = obj.optString("email", ""),
                            keyDecisionMaker = obj.optString("key_decision_maker", ""),
                            decisionMakerRole = obj.optString("decision_maker_role", ""),
                            relationshipScore = obj.optInt("relationship_score", 5),
                            strategicNotes = obj.optString("strategic_notes", "")
                        )
                    )
                }
                Result.success(list)
            } else {
                Result.failure(Exception("Lỗi tải khách hàng: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createCustomer(name: String, sbu: String, tier: String, keyDecisionMaker: String, phone: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val code = "KH-" + sbu + "-" + (System.currentTimeMillis() % 10000)
            val json = JSONObject().apply {
                put("name", name)
                put("code", code)
                put("sbu", sbu)
                put("tier", tier)
                put("key_decision_maker", keyDecisionMaker)
                put("phone", phone)
                put("decision_maker_role", "Chủ tịch / Tổng Giám Đốc")
                put("relationship_score", 5)
            }
            val conn = (URL("$baseUrl/api/customers").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; utf-8")
                setRequestProperty("Accept", "application/json")
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
            }
            OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
            if (conn.responseCode in 200..201) {
                Result.success(true)
            } else {
                Result.failure(Exception("Lỗi tạo khách hàng: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchProjects(sbu: String = "ALL"): Result<List<ProjectItem>> = withContext(Dispatchers.IO) {
        try {
            val urlStr = if (sbu == "ALL") "$baseUrl/api/projects" else "$baseUrl/api/projects?sbu=$sbu"
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                connectTimeout = 8000
                readTimeout = 8000
            }
            if (conn.responseCode == 200) {
                val responseText = BufferedReader(InputStreamReader(conn.inputStream)).readText()
                val arr = JSONArray(responseText)
                val list = mutableListOf<ProjectItem>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        ProjectItem(
                            id = obj.getInt("id"),
                            code = obj.optString("code", ""),
                            name = obj.getString("name"),
                            sbu = obj.optString("sbu", ""),
                            customerName = obj.optString("customer_name", "Chủ đầu tư"),
                            contractValueBillion = obj.optDouble("contract_value_vnd", 0.0) / 1_000_000_000.0,
                            progressPercent = obj.optDouble("progress_pct", 0.0),
                            collectedAmountBillion = obj.optDouble("collected_amount_vnd", 0.0) / 1_000_000_000.0,
                            status = obj.optString("status", "IN_PROGRESS")
                        )
                    )
                }
                Result.success(list)
            } else {
                Result.failure(Exception("Lỗi tải dự án: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchCareActivities(sbu: String = "ALL"): Result<List<CareActivityItem>> = withContext(Dispatchers.IO) {
        try {
            val urlStr = if (sbu == "ALL") "$baseUrl/api/care-activities" else "$baseUrl/api/care-activities?sbu=$sbu"
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                connectTimeout = 8000
                readTimeout = 8000
            }
            if (conn.responseCode == 200) {
                val responseText = BufferedReader(InputStreamReader(conn.inputStream)).readText()
                val arr = JSONArray(responseText)
                val list = mutableListOf<CareActivityItem>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        CareActivityItem(
                            id = obj.getInt("id"),
                            customerName = obj.optString("customer_name", "Khách hàng"),
                            sbu = obj.optString("sbu", ""),
                            activityType = obj.optString("activity_type", "EXECUTIVE_MEETING"),
                            title = obj.getString("title"),
                            occurredAt = obj.optString("occurred_at", ""),
                            leaderInCharge = obj.optString("leader_in_charge", ""),
                            outcomeStatus = obj.optString("outcome_status", "SUCCESS")
                        )
                    )
                }
                Result.success(list)
            } else {
                Result.failure(Exception("Lỗi tải chăm sóc VIP: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
