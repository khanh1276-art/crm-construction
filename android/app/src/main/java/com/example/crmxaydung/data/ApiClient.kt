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
    // Official Render live deployment URL
    var baseUrl: String = "https://crm-construction-6lrg.onrender.com"
        set(value) {
            field = value.trim().removeSuffix("/")
        }

    var currentUser: UserSession? = null

    private fun openConnection(endpoint: String, method: String = "GET"): HttpURLConnection {
        val cleanUrl = if (endpoint.startsWith("http")) endpoint else "$baseUrl$endpoint"
        val conn = (URL(cleanUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            setRequestProperty("Accept", "application/json")
            // Crucial: Pass Role and SBU headers for proper data isolation and sync
            setRequestProperty("X-User-Role", currentUser?.role ?: "ADMIN")
            setRequestProperty("X-User-SBU", currentUser?.sbu ?: "ALL")
            connectTimeout = 45000
            readTimeout = 45000
        }
        return conn
    }

    suspend fun checkHealth(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val conn = openConnection("/api/health")
            val code = conn.responseCode
            if (code == 200) {
                Result.success("Kết nối Render thành công (200 OK)")
            } else if (code == 503) {
                Result.failure(Exception("Máy chủ Render đang thức dậy (Cold start). Vui lòng đợi 30 giây rồi thử lại."))
            } else {
                Result.failure(Exception("Mã phản hồi từ máy chủ: $code"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(username: String, password: String): Result<UserSession> = withContext(Dispatchers.IO) {
        try {
            val conn = openConnection("/api/users/login", "POST").apply {
                setRequestProperty("Content-Type", "application/json; utf-8")
                doOutput = true
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
                    email = userObj.optString("email", ""),
                    phone = userObj.optString("phone", ""),
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

    // --- Dashboard Metrics ---
    suspend fun fetchDashboardStats(sbu: String = "ALL"): Result<DashboardStats> = withContext(Dispatchers.IO) {
        try {
            val endpoint = if (sbu == "ALL") "/api/dashboard/metrics" else "/api/dashboard/metrics?sbu=$sbu"
            val conn = openConnection(endpoint)
            if (conn.responseCode == 200) {
                val responseText = BufferedReader(InputStreamReader(conn.inputStream)).readText()
                val json = JSONObject(responseText)
                val overview = json.optJSONObject("overview") ?: JSONObject()

                val contractVal = overview.optDouble("total_contract_value", 0.0)
                val paidVal = overview.optDouble("total_paid_amount", 0.0)
                val unpaidVal = overview.optDouble("unpaid_balance", 0.0)

                val stats = DashboardStats(
                    totalCustomers = overview.optInt("total_customers", 0),
                    strategicVipCount = overview.optInt("active_bids_count", 0),
                    totalProjects = overview.optInt("total_projects", 0),
                    totalContractBillion = contractVal / 1_000_000_000.0,
                    totalCollectedBillion = paidVal / 1_000_000_000.0,
                    unpaidBalanceBillion = unpaidVal / 1_000_000_000.0,
                    avgProgress = if (contractVal > 0) (paidVal / contractVal * 100.0) else 0.0,
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

    // --- Customers ---
    suspend fun fetchCustomers(sbu: String = "ALL"): Result<List<CustomerItem>> = withContext(Dispatchers.IO) {
        try {
            val endpoint = if (sbu == "ALL") "/api/customers" else "/api/customers?sbu=$sbu"
            val conn = openConnection(endpoint)
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
                            tier = obj.optString("tier", "GOLD"),
                            segment = obj.optString("segment", "B2B"),
                            taxCode = obj.optString("tax_code", ""),
                            phone = obj.optString("phone", ""),
                            email = obj.optString("email", ""),
                            headquarters = obj.optString("headquarters", ""),
                            keyDecisionMaker = obj.optString("key_decision_maker", ""),
                            decisionMakerRole = obj.optString("decision_maker_role", ""),
                            decisionMakerPhone = obj.optString("decision_maker_phone", ""),
                            decisionMakerBirthday = obj.optString("decision_maker_birthday", ""),
                            foundingAnniversary = obj.optString("founding_anniversary", ""),
                            relationshipScore = obj.optInt("relationship_score", 5),
                            relationshipStatus = obj.optString("relationship_status", "EXCELLENT"),
                            strategicNotes = obj.optString("strategic_notes", ""),
                            projectCount = obj.optInt("project_count", 0),
                            totalContractValue = obj.optDouble("total_contract_value", 0.0),
                            scoreScaleProject = obj.optDouble("score_scale_project", 15.0),
                            scoreFeconFit = obj.optDouble("score_fecon_fit", 25.0),
                            scoreFinancialCapacity = obj.optDouble("score_financial_capacity", 25.0),
                            scoreCooperationHistory = obj.optDouble("score_cooperation_history", 20.0),
                            scoreManagementCapacity = obj.optDouble("score_management_capacity", 15.0),
                            totalScore = obj.optDouble("total_score", 100.0),
                            isSpecialElevated = obj.optInt("is_special_elevated", 0) == 1,
                            vetoApplied = obj.optInt("veto_applied", 0) == 1,
                            annualCareBudget = obj.optDouble("annual_care_budget", 80000000.0),
                            spentCareBudget = obj.optDouble("spent_care_budget", 0.0),
                            inChargeExecutive = obj.optString("in_charge_executive", "Chủ tịch / TGĐ trực tiếp phụ trách"),
                            careFrequency = obj.optString("care_frequency", "1 tháng / lần")
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

    suspend fun createCustomer(
        name: String,
        sbu: String,
        tier: String,
        keyDecisionMaker: String,
        role: String,
        phone: String,
        email: String,
        taxCode: String,
        headquarters: String,
        birthday: String,
        anniversary: String,
        notes: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("name", name)
                put("sbu", sbu)
                put("tier", tier)
                put("key_decision_maker", keyDecisionMaker)
                put("decision_maker_role", role.ifEmpty { "Chủ tịch / Tổng Giám Đốc" })
                put("decision_maker_phone", phone)
                put("phone", phone)
                put("email", email)
                put("tax_code", taxCode)
                put("headquarters", headquarters)
                put("decision_maker_birthday", birthday)
                put("founding_anniversary", anniversary)
                put("strategic_notes", notes)
                put("relationship_score", 5)
            }
            val conn = openConnection("/api/customers", "POST").apply {
                setRequestProperty("Content-Type", "application/json; utf-8")
                doOutput = true
            }
            OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
            if (conn.responseCode in 200..201) {
                Result.success(true)
            } else {
                val err = BufferedReader(InputStreamReader(conn.errorStream ?: conn.inputStream)).readText()
                Result.failure(Exception("Lỗi tạo khách hàng: $err"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Projects ---
    suspend fun fetchProjects(sbu: String = "ALL"): Result<List<ProjectItem>> = withContext(Dispatchers.IO) {
        try {
            val endpoint = if (sbu == "ALL") "/api/projects" else "/api/projects?sbu=$sbu"
            val conn = openConnection(endpoint)
            if (conn.responseCode == 200) {
                val responseText = BufferedReader(InputStreamReader(conn.inputStream)).readText()
                val arr = JSONArray(responseText)
                val list = mutableListOf<ProjectItem>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val cVal = obj.optDouble("contract_value", 0.0)
                    val pVal = obj.optDouble("paid_amount", 0.0)
                    list.add(
                        ProjectItem(
                            id = obj.getInt("id"),
                            code = obj.optString("code", ""),
                            name = obj.getString("name"),
                            sbu = obj.optString("sbu", ""),
                            customerId = obj.optInt("customer_id", 0),
                            customerName = obj.optString("customer_name", "Chủ đầu tư"),
                            contractNumber = obj.optString("contract_number", ""),
                            contractValueBillion = cVal / 1_000_000_000.0,
                            progressPercent = obj.optDouble("progress_percent", 0.0),
                            collectedAmountBillion = pVal / 1_000_000_000.0,
                            unpaidBillion = (cVal - pVal) / 1_000_000_000.0,
                            projectHealth = obj.optString("project_health", "GOOD"),
                            projectDirector = obj.optString("project_director", ""),
                            summaryScope = obj.optString("summary_scope", ""),
                            startDate = obj.optString("start_date", ""),
                            expectedEndDate = obj.optString("expected_end_date", ""),
                            keyDecisionMaker = obj.optString("key_decision_maker", ""),
                            decisionMakerPhone = obj.optString("decision_maker_phone", "")
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

    suspend fun fetchProjectDetail(projectId: Int): Result<ProjectItem> = withContext(Dispatchers.IO) {
        try {
            val conn = openConnection("/api/projects/$projectId")
            if (conn.responseCode == 200) {
                val responseText = BufferedReader(InputStreamReader(conn.inputStream)).readText()
                val obj = JSONObject(responseText)
                val cVal = obj.optDouble("contract_value", 0.0)
                val pVal = obj.optDouble("paid_amount", 0.0)

                val milestonesArr = obj.optJSONArray("milestones") ?: JSONArray()
                val mList = mutableListOf<MilestoneItem>()
                for (i in 0 until milestonesArr.length()) {
                    val mObj = milestonesArr.getJSONObject(i)
                    mList.add(
                        MilestoneItem(
                            id = mObj.getInt("id"),
                            projectId = mObj.optInt("project_id", projectId),
                            title = mObj.getString("title"),
                            dueDate = mObj.optString("due_date", ""),
                            percentage = mObj.optDouble("percentage", 0.0),
                            amount = mObj.optDouble("amount", 0.0),
                            paymentStatus = mObj.optString("payment_status", "PENDING"),
                            notes = mObj.optString("notes", "")
                        )
                    )
                }

                val item = ProjectItem(
                    id = obj.getInt("id"),
                    code = obj.optString("code", ""),
                    name = obj.getString("name"),
                    sbu = obj.optString("sbu", ""),
                    customerId = obj.optInt("customer_id", 0),
                    customerName = obj.optString("customer_name", "Chủ đầu tư"),
                    contractNumber = obj.optString("contract_number", ""),
                    contractValueBillion = cVal / 1_000_000_000.0,
                    progressPercent = obj.optDouble("progress_percent", 0.0),
                    collectedAmountBillion = pVal / 1_000_000_000.0,
                    unpaidBillion = (cVal - pVal) / 1_000_000_000.0,
                    projectHealth = obj.optString("project_health", "GOOD"),
                    projectDirector = obj.optString("project_director", ""),
                    summaryScope = obj.optString("summary_scope", ""),
                    startDate = obj.optString("start_date", ""),
                    expectedEndDate = obj.optString("expected_end_date", ""),
                    keyDecisionMaker = obj.optString("key_decision_maker", ""),
                    decisionMakerPhone = obj.optString("decision_maker_phone", ""),
                    headquarters = obj.optString("headquarters", ""),
                    milestones = mList
                )
                Result.success(item)
            } else {
                Result.failure(Exception("Lỗi xem chi tiết dự án: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createProject(
        name: String,
        sbu: String,
        customerId: Int,
        contractValueVnd: Double,
        progressPercent: Double,
        contractNumber: String,
        projectDirector: String,
        summaryScope: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("name", name)
                put("sbu", sbu)
                put("customer_id", customerId)
                put("contract_value", contractValueVnd)
                put("paid_amount", 0.0)
                put("progress_percent", progressPercent)
                put("contract_number", contractNumber)
                put("project_director", projectDirector)
                put("summary_scope", summaryScope)
                put("project_health", "GOOD")
            }
            val conn = openConnection("/api/projects", "POST").apply {
                setRequestProperty("Content-Type", "application/json; utf-8")
                doOutput = true
            }
            OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
            if (conn.responseCode in 200..201) {
                Result.success(true)
            } else {
                val err = BufferedReader(InputStreamReader(conn.errorStream ?: conn.inputStream)).readText()
                Result.failure(Exception("Lỗi tạo dự án: $err"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Care Activities ---
    suspend fun fetchCareActivities(sbu: String = "ALL"): Result<List<CareActivityItem>> = withContext(Dispatchers.IO) {
        try {
            val endpoint = if (sbu == "ALL") "/api/care-activities" else "/api/care-activities?sbu=$sbu"
            val conn = openConnection(endpoint)
            if (conn.responseCode == 200) {
                val responseText = BufferedReader(InputStreamReader(conn.inputStream)).readText()
                val arr = JSONArray(responseText)
                val list = mutableListOf<CareActivityItem>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        CareActivityItem(
                            id = obj.getInt("id"),
                            customerId = obj.optInt("customer_id", 0),
                            customerName = obj.optString("customer_name", "Khách hàng"),
                            sbu = obj.optString("sbu", ""),
                            activityType = obj.optString("activity_type", "EXECUTIVE_MEETING"),
                            title = obj.getString("title"),
                            content = obj.optString("content", ""),
                            occurredAt = obj.optString("occurred_at", ""),
                            leaderInCharge = obj.optString("leader_in_charge", ""),
                            outcomeStatus = obj.optString("outcome_status", "SUCCESS"),
                            cost = obj.optDouble("cost", 0.0)
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

    suspend fun logCareActivity(
        customerId: Int,
        sbu: String,
        activityType: String,
        title: String,
        content: String,
        occurredAt: String,
        leaderInCharge: String,
        outcomeStatus: String = "SUCCESS",
        cost: Double = 0.0
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("customer_id", customerId)
                put("sbu", sbu)
                put("activity_type", activityType)
                put("title", title)
                put("content", content)
                put("occurred_at", occurredAt)
                put("leaderInCharge", leaderInCharge)
                put("leader_in_charge", leaderInCharge)
                put("outcome_status", outcomeStatus)
                put("cost", cost)
            }
            val conn = openConnection("/api/care-activities", "POST").apply {
                setRequestProperty("Content-Type", "application/json; utf-8")
                doOutput = true
            }
            OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
            if (conn.responseCode in 200..201) {
                Result.success(true)
            } else {
                val err = BufferedReader(InputStreamReader(conn.errorStream ?: conn.inputStream)).readText()
                Result.failure(Exception("Lỗi ghi nhật ký: $err"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun assessCustomer(
        customerId: Int,
        scoreScale: Double,
        scoreFit: Double,
        scoreFinance: Double,
        scoreHistory: Double,
        scoreMgmt: Double,
        isSpecialElevated: Boolean = false,
        notes: String = ""
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("score_scale_project", scoreScale)
                put("score_fecon_fit", scoreFit)
                put("score_financial_capacity", scoreFinance)
                put("score_cooperation_history", scoreHistory)
                put("score_management_capacity", scoreMgmt)
                put("is_special_elevated", isSpecialElevated)
                put("strategic_notes", notes)
            }
            val conn = openConnection("/api/customers/$customerId/assess", "POST").apply {
                setRequestProperty("Content-Type", "application/json; utf-8")
                doOutput = true
            }
            OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
            if (conn.responseCode in 200..299) {
                val res = BufferedReader(InputStreamReader(conn.inputStream)).readText()
                Result.success(JSONObject(res))
            } else {
                val err = BufferedReader(InputStreamReader(conn.errorStream ?: conn.inputStream)).readText()
                Result.failure(Exception("Lỗi đánh giá khách hàng: $err"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Users & Account Management ---
    suspend fun fetchUsers(): Result<List<UserItem>> = withContext(Dispatchers.IO) {
        try {
            val conn = openConnection("/api/users")
            if (conn.responseCode == 200) {
                val responseText = BufferedReader(InputStreamReader(conn.inputStream)).readText()
                val arr = JSONArray(responseText)
                val list = mutableListOf<UserItem>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        UserItem(
                            id = obj.getInt("id"),
                            username = obj.getString("username"),
                            fullName = obj.getString("full_name"),
                            role = obj.getString("role"),
                            sbu = obj.getString("sbu"),
                            sbuName = obj.optString("sbu_name", ""),
                            title = obj.optString("title", ""),
                            email = obj.optString("email", ""),
                            phone = obj.optString("phone", "")
                        )
                    )
                }
                Result.success(list)
            } else {
                Result.failure(Exception("Lỗi tải danh sách tài khoản: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createUser(
        username: String,
        password: String,
        fullName: String,
        role: String,
        sbu: String,
        title: String,
        email: String,
        phone: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("username", username)
                put("password", password.ifEmpty { "123456" })
                put("full_name", fullName)
                put("role", role)
                put("sbu", sbu)
                put("title", title)
                put("email", email)
                put("phone", phone)
            }
            val conn = openConnection("/api/users", "POST").apply {
                setRequestProperty("Content-Type", "application/json; utf-8")
                doOutput = true
            }
            OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
            if (conn.responseCode in 200..201) {
                Result.success(true)
            } else {
                val err = BufferedReader(InputStreamReader(conn.errorStream ?: conn.inputStream)).readText()
                val errMsg = try { JSONObject(err).optString("detail", err) } catch (_: Exception) { err }
                Result.failure(Exception(errMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUser(userId: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val conn = openConnection("/api/users/$userId", "DELETE")
            if (conn.responseCode in 200..204) {
                Result.success(true)
            } else {
                val err = BufferedReader(InputStreamReader(conn.errorStream ?: conn.inputStream)).readText()
                val errMsg = try { JSONObject(err).optString("detail", err) } catch (_: Exception) { err }
                Result.failure(Exception(errMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Bidding Pipeline ---
    suspend fun fetchPipelineBids(sbu: String = "ALL"): Result<List<PipelineBidItem>> = withContext(Dispatchers.IO) {
        try {
            val endpoint = if (sbu == "ALL") "/api/customers/bids/pipeline" else "/api/customers/bids/pipeline?sbu=$sbu"
            val conn = openConnection(endpoint)
            if (conn.responseCode == 200) {
                val responseText = BufferedReader(InputStreamReader(conn.inputStream)).readText()
                val json = JSONObject(responseText)
                val itemsObj = json.getJSONObject("items")
                val list = mutableListOf<PipelineBidItem>()
                val keys = itemsObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val arr = itemsObj.getJSONArray(k)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(
                            PipelineBidItem(
                                id = obj.getInt("id"),
                                customerId = obj.optInt("customer_id", 0),
                                customerName = obj.optString("customer_name", ""),
                                sbu = obj.optString("sbu", ""),
                                projectTitle = obj.optString("project_title", ""),
                                estimatedValueBillion = obj.optDouble("estimated_value", 0.0) / 1_000_000_000.0,
                                stage = obj.optString("stage", "INFORMATION"),
                                winRate = obj.optInt("win_rate", 50),
                                tenderDeadline = obj.optString("tender_deadline", ""),
                                targetKickoff = obj.optString("target_kickoff", ""),
                                assignedDirector = obj.optString("assigned_director", ""),
                                biddingNotes = obj.optString("bidding_notes", ""),
                                keyDecisionMaker = obj.optString("key_decision_maker", ""),
                                decisionMakerPhone = obj.optString("decision_maker_phone", "")
                            )
                        )
                    }
                }
                Result.success(list)
            } else {
                Result.failure(Exception("Lỗi tải phễu thầu: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateBidStage(bidId: Int, stage: String, winRate: Int, notes: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("stage", stage)
                put("win_rate", winRate)
                put("bidding_notes", notes)
            }
            val conn = openConnection("/api/customers/bids/$bidId/stage", "PUT").apply {
                setRequestProperty("Content-Type", "application/json; utf-8")
                doOutput = true
            }
            OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
            if (conn.responseCode in 200..204) {
                Result.success(true)
            } else {
                val err = BufferedReader(InputStreamReader(conn.errorStream ?: conn.inputStream)).readText()
                val errMsg = try { JSONObject(err).optString("detail", err) } catch (_: Exception) { err }
                Result.failure(Exception(errMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
