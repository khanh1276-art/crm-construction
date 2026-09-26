package com.example.crmxaydung.data

data class UserSession(
    val id: Int,
    val username: String,
    val fullName: String,
    val role: String, // ADMIN, SBU_DIRECTOR, COLLABORATOR
    val sbu: String,  // ALL, SBU1, SBU2, SBU3, SBU4, SBU5
    val title: String,
    val token: String = "",
    val email: String = "",
    val phone: String = ""
)

data class DashboardStats(
    val totalCustomers: Int = 0,
    val strategicVipCount: Int = 0,
    val totalProjects: Int = 0,
    val totalContractBillion: Double = 0.0,
    val totalCollectedBillion: Double = 0.0,
    val unpaidBalanceBillion: Double = 0.0,
    val avgProgress: Double = 0.0,
    val sbuFilter: String = "ALL"
)

data class CustomerItem(
    val id: Int,
    val code: String,
    val name: String,
    val sbu: String,
    val tier: String,
    val segment: String = "B2B",
    val taxCode: String = "",
    val phone: String = "",
    val email: String = "",
    val headquarters: String = "",
    val keyDecisionMaker: String,
    val decisionMakerRole: String,
    val decisionMakerPhone: String = "",
    val decisionMakerBirthday: String = "",
    val foundingAnniversary: String = "",
    val relationshipScore: Int = 5,
    val relationshipStatus: String = "EXCELLENT",
    val strategicNotes: String = "",
    val projectCount: Int = 0,
    val totalContractValue: Double = 0.0
)

data class MilestoneItem(
    val id: Int,
    val projectId: Int,
    val title: String,
    val dueDate: String,
    val percentage: Double,
    val amount: Double,
    val paymentStatus: String, // PAID, PENDING
    val notes: String = ""
)

data class ProjectItem(
    val id: Int,
    val code: String,
    val name: String,
    val sbu: String,
    val customerId: Int = 0,
    val customerName: String,
    val contractNumber: String = "",
    val contractValueBillion: Double,
    val progressPercent: Double,
    val collectedAmountBillion: Double,
    val unpaidBillion: Double = 0.0,
    val projectHealth: String = "GOOD", // GOOD, ATTENTION, DELAYED
    val projectDirector: String = "",
    val summaryScope: String = "",
    val startDate: String = "",
    val expectedEndDate: String = "",
    val keyDecisionMaker: String = "",
    val decisionMakerPhone: String = "",
    val headquarters: String = "",
    val milestones: List<MilestoneItem> = emptyList()
)

data class CareActivityItem(
    val id: Int,
    val customerId: Int = 0,
    val customerName: String,
    val sbu: String,
    val activityType: String,
    val title: String,
    val content: String = "",
    val occurredAt: String,
    val leaderInCharge: String,
    val outcomeStatus: String
)

data class UserItem(
    val id: Int,
    val username: String,
    val fullName: String,
    val role: String,
    val sbu: String,
    val sbuName: String = "",
    val title: String,
    val email: String = "",
    val phone: String = ""
)

data class PipelineBidItem(
    val id: Int,
    val customerId: Int = 0,
    val customerName: String = "",
    val sbu: String = "",
    val projectTitle: String = "",
    val estimatedValueBillion: Double = 0.0,
    val stage: String = "INFORMATION",
    val winRate: Int = 50,
    val tenderDeadline: String = "",
    val targetKickoff: String = "",
    val assignedDirector: String = "",
    val biddingNotes: String = "",
    val keyDecisionMaker: String = "",
    val decisionMakerPhone: String = ""
)
