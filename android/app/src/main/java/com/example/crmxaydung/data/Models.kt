package com.example.crmxaydung.data

data class UserSession(
    val id: Int,
    val username: String,
    val fullName: String,
    val role: String, // ADMIN, SBU_DIRECTOR, COLLABORATOR
    val sbu: String,  // ALL, SBU1, SBU2, SBU3, SBU4, SBU5
    val title: String,
    val token: String = ""
)

data class DashboardStats(
    val totalCustomers: Int = 0,
    val strategicVipCount: Int = 0,
    val totalProjects: Int = 0,
    val totalContractBillion: Double = 0.0,
    val totalCollectedBillion: Double = 0.0,
    val avgProgress: Double = 0.0,
    val sbuFilter: String = "ALL"
)

data class CustomerItem(
    val id: Int,
    val code: String,
    val name: String,
    val sbu: String,
    val tier: String,
    val phone: String,
    val email: String,
    val keyDecisionMaker: String,
    val decisionMakerRole: String,
    val relationshipScore: Int,
    val strategicNotes: String
)

data class ProjectItem(
    val id: Int,
    val code: String,
    val name: String,
    val sbu: String,
    val customerName: String,
    val contractValueBillion: Double,
    val progressPercent: Double,
    val collectedAmountBillion: Double,
    val status: String
)

data class CareActivityItem(
    val id: Int,
    val customerName: String,
    val sbu: String,
    val activityType: String,
    val title: String,
    val occurredAt: String,
    val leaderInCharge: String,
    val outcomeStatus: String
)
