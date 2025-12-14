package com.skillswap.model

import com.google.gson.annotations.SerializedName

data class ReferralPreview(
    val username: String,
    val badges: List<String>?,
    val remainingSlots: Int
)

data class SignInResponse(
    val message: String? = null,
    @SerializedName("access_token") val accessToken: String?,
    val user: User?
)

data class User(
    @SerializedName("_id") val id: String,
    val username: String,
    val email: String,
    val role: String,
    val credits: Int?,
    val ratingAvg: Double?,
    val isVerified: Boolean?,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val image: String? = null, // Added to match backend
    @SerializedName("skillsTeach") val skillsTeach: List<String>? = null,
    @SerializedName("skillsLearn") val skillsLearn: List<String>? = null,
    val xp: Int? = null,
    val badges: List<String>? = null,
    val location: UserLocation? = null
)

data class UserLocation(
    val lat: Double?,
    val lon: Double?,
    val city: String?
)

data class Session(
    @SerializedName("_id") val id: String,
    val teacher: SessionUserSummary,
    val student: SessionUserSummary?,
    val students: List<SessionUserSummary>?,
    val skill: String,
    val title: String,
    val date: String, // Keep as String to avoid parsing issues, parse in UI
    val duration: Int,
    val status: String,
    val location: String?,
    val rescheduleRequest: RescheduleStatus?,
    val meetingLink: String? = null
)

data class SessionUserSummary(
    @SerializedName("_id") val id: String,
    val username: String,
    val email: String,
    val image: String?,
    val avatarUrl: String?
)

data class RescheduleStatus(
    val proposedDate: String?,
    val proposedTime: String?,
    val note: String?,
    val responses: List<RescheduleVote>?,
    val isActive: Boolean?
)

data class RescheduleVote(
    val userId: String,
    val answer: String,
    val respondedAt: String?
)

data class RescheduleProposalPayload(
    val proposedDate: String,
    val proposedTime: String,
    val note: String?
)

data class RateSessionRequest(
    val ratedUserId: String,
    val rating: Int,
    val comment: String? = null
)

data class CreateSessionRequest(
    val title: String,
    val skill: String,
    val studentEmail: String,
    val date: String,
    val duration: Int,
    val meetingLink: String? = null,
    val notes: String? = null
)

data class CreateGoalRequest(
    val title: String,
    val targetHours: Double,
    val period: String = "week",
    val dueDate: String? = null
)

data class UpdateGoalRequest(
    val title: String? = null,
    val targetHours: Double? = null,
    val period: String? = null,
    val dueDate: String? = null,
    val status: String? = null
)

// API Wrapper
data class SessionsResponse(
    val message: String?,
    val data: List<Session>
)

data class Conversation(
    @SerializedName("_id") val id: String,
    val partnerId: String,
    val partnerName: String,
    val lastMessage: String,
    val timestamp: String,
    val unreadCount: Int
)

data class Message(
    val id: String,
    val text: String,
    val isMe: Boolean,
    val time: String,
    val read: Boolean = false,
    val reactions: Map<String, List<String>>? = null,
    val isDeleted: Boolean = false
)

data class ChatPayload(
    val partnerId: String
)

data class ThreadListResponse(
    val items: List<ChatThread>,
    val total: Int,
    val limit: Int,
    val skip: Int,
    val hasNextPage: Boolean
)

data class ChatThread(
    val id: String,
    val participants: List<User>,
    val sessionId: String?,
    val topic: String?,
    val metadata: Map<String, Any>?,
    val lastMessageAt: String?,
    val lastMessage: ThreadMessage?,
    val unreadCount: Int
)

data class ThreadMessage(
    @SerializedName("_id") val id: String,
    @SerializedName("thread") val threadId: String,
    @SerializedName("sender") val senderId: String,
    @SerializedName("recipient") val recipientId: String?,
    val type: String,
    val content: String,
    val read: Boolean,
    val createdAt: String,
    val reactions: Map<String, List<String>>? = null, // emoji -> list of userIds
    val isDeleted: Boolean? = null,
    val replyTo: ReferencedMessage? = null
)

data class ReferencedMessage(
    @SerializedName("_id") val id: String,
    val content: String,
    @SerializedName("sender") val senderId: String,
    val type: String = "text"
)

data class MessagesResponse(
    val threadId: String,
    val items: List<ThreadMessage>,
    val hasMore: Boolean
)

data class SocketMessagePayload(
    val id: String,
    val threadId: String,
    val senderId: String,
    val content: String,
    val createdAt: String
)

data class SocketTypingPayload(
    val threadId: String,
    val userId: String,
    val isTyping: Boolean
)

// --- Call signaling (lightweight stubs for parity) ---
data class CallSdp(
    val type: String,
    val sdp: String
)

data class CallIceCandidate(
    val sdp: String,
    val sdpMid: String?,
    val sdpMLineIndex: Int
)
data class CallOfferPayload(
    val callId: String,
    val callerId: String,
    val sdp: String,
    val isVideo: Boolean,
    val threadId: String? = null
)
data class CallAnswerPayload(
    val callId: String,
    val fromUserId: String? = null,
    val sdp: String,
    val threadId: String? = null
)
data class CallIcePayload(
    val callId: String,
    val fromUserId: String? = null,
    val candidate: String,
    val sdpMid: String?,
    val sdpMLineIndex: Int
)

data class CallEndPayload(
    val callId: String,
    val fromUserId: String? = null
)

data class CallRejectPayload(
    val callId: String,
    val fromUserId: String? = null
)

data class CallBusyPayload(
    val callId: String,
    val fromUserId: String? = null
)

// --- Quizzes ---
data class QuizQuestion(
    val id: String = java.util.UUID.randomUUID().toString(),
    val question: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val explanation: String
)

data class QuizResult(
    val id: String = java.util.UUID.randomUUID().toString(),
    val subject: String,
    val level: Int,
    val score: Int,
    val totalQuestions: Int,
    val date: Long = System.currentTimeMillis()
)
// --- Lesson Plan ---
data class LessonPlan(
    @SerializedName("_id") val id: String,
    val sessionId: String,
    val skill: String,
    val level: String,
    val duration: Int,
    val goal: String,
    val plan: String,
    val checklist: List<String>,
    val resources: List<String>,
    val homework: String,
    val progress: Map<String, Boolean>,
    val createdAt: String?,
    val updatedAt: String?
)

data class LessonPlanResponse(
    val message: String?,
    val data: LessonPlan?,
    val error: String?
)

data class LessonPlanGenerateRequest(
    val level: String? = null,
    val goal: String? = null
)

// --- Auth extras ---
data class ForgotPasswordRequest(val email: String)
data class ForgotPasswordResponse(val message: String?)

// --- Referrals ---
data class ReferralCodeResponse(
    val code: String,
    val codeId: String,
    val expiresAt: String?
)

data class RedeemResponse(
    val referralId: String,
    val status: String
)

data class ReferralItem(
    @SerializedName("_id") val id: String,
    val codeId: String?,
    val inviterId: String?,
    val inviteeId: String?,
    val inviteeEmail: String?,
    val status: String?,
    val rewardApplied: Boolean?,
    val createdAt: String?,
    val updatedAt: String?
)

data class RewardItem(
    @SerializedName("_id") val id: String,
    val referralId: String?,
    val userId: String,
    val rewardType: String,
    val amount: Int?,
    val status: String,
    val createdAt: String?
)

data class ReferralsMeResponse(
    val inviterReferrals: List<ReferralItem>,
    val inviteeReferral: ReferralItem?,
    val rewards: List<RewardItem>
)

// --- Weekly Objectives ---
data class WeeklyObjective(
    @SerializedName("_id") val id: String,
    val user: String,
    val title: String,
    val targetHours: Int,
    val completedHours: Double,
    val startDate: String,
    val endDate: String,
    val status: String,
    val dailyTasks: List<DailyTask>,
    val createdAt: String?,
    val updatedAt: String?
) {
    val progressPercent: Int
        get() = if (targetHours > 0) ((completedHours / targetHours.toDouble()) * 100).toInt().coerceIn(0, 100) else 0
    val completedTasksCount: Int
        get() = dailyTasks.count { it.isCompleted }
}

data class DailyTask(
    val day: String,
    val task: String,
    val isCompleted: Boolean
)

data class CreateWeeklyObjectiveRequest(
    val title: String,
    val targetHours: Int,
    val startDate: String,
    val endDate: String,
    val dailyTasks: List<DailyTaskRequest>
)

data class DailyTaskRequest(
    val day: String,
    val task: String
)

data class UpdateWeeklyObjectiveRequest(
    val taskUpdates: List<TaskUpdateRequest>
)

data class TaskUpdateRequest(
    val index: Int,
    val isCompleted: Boolean
)

data class WeeklyObjectiveHistoryResponse(
    val objectives: List<WeeklyObjective>,
    val total: Int,
    val page: Int,
    val pages: Int
)
// --- Progress Models ---
data class ProgressDashboardResponse(
    val stats: ProgressStats,
    val goals: List<ProgressGoalItem>,
    val weeklyActivity: List<WeeklyActivityPoint>,
    val skillProgress: List<SkillProgressItem>,
    val badges: List<BadgeItem>,
    val xpSummary: XPSummary
)

data class ProgressStats(
    val weeklyHours: Double,
    val skillsCount: Int
)

data class ProgressGoalItem(
    @SerializedName("_id") val id: String,
    val title: String,
    val targetHours: Double,
    val currentHours: Double,
    val period: String, // "week" or "month"
    val status: String,
    val dueDate: String?,
    val progressPercent: Int?
)

data class WeeklyActivityPoint(
    val day: String,
    val hours: Double
)

data class SkillProgressItem(
    val skill: String,
    val hours: Double,
    val level: String,
    val progress: Int
)

data class BadgeItem(
    val tier: String,
    val title: String,
    val description: String,
    val iconKey: String,
    val icon: String,
    val color: String,
    val threshold: Int,
    val unlocked: Boolean
) {
    val displayIcon: String
        get() = if (icon.isNotEmpty() && icon != iconKey) icon else when (iconKey) {
            "badge-iron" -> "🛡️"
            "badge-bronze" -> "🥉"
            "badge-silver" -> "🥈"
            "badge-gold" -> "🥇"
            else -> "🎖️"
        }
}

data class XPSummary(
    val xp: Int,
    val referralCount: Int,
    val nextBadge: NextBadgeInfo?
)

data class NextBadgeInfo(
    val tier: String,
    val title: String,
    val threshold: Int
)

// --- Promos Models ---
data class Promo(
    @SerializedName("_id") val id: String,
    val title: String,
    val description: String,
    @SerializedName("discountPercent") val discount: Int,
    val imageUrl: String?,
    val promoCode: String?,
    val validFrom: String?,
    @SerializedName("validTo") val validUntil: String,
    val createdAt: String,
    val updatedAt: String
)

data class CreatePromoRequest(
    val title: String,
    val description: String,
    @SerializedName("discountPercent") val discount: Int,
    val validFrom: String?,
    @SerializedName("validTo") val validUntil: String,
    val promoCode: String? = null,
    val imageUrl: String? = null
)

data class UpdatePromoRequest(
    val title: String? = null,
    val description: String? = null,
    @SerializedName("discountPercent") val discount: Int? = null,
    val validFrom: String? = null,
    @SerializedName("validTo") val validUntil: String? = null,
    val promoCode: String? = null,
    val imageUrl: String? = null
)

data class CreateAnnonceRequest(
    val title: String,
    val description: String,
    val city: String?,
    val category: String? = null,
    val price: Double? = null,
    val imageUrl: String? = null
)

data class UpdateAnnonceRequest(
    val title: String? = null,
    val description: String? = null,
    val city: String? = null,
    val category: String? = null,
    val price: Double? = null,
    val imageUrl: String? = null
)

// --- Annonces Models ---
data class Annonce(
    @SerializedName("_id") val id: String,
    val title: String,
    val description: String,
    val imageUrl: String?,
    val isNew: Bool, // Use Boolean? if backend might send null, matching Swift `Bool` is likely non-null
    val city: String?,
    val category: String?,
    val user: AnnonceUser?,
    val createdAt: String,
    val updatedAt: String
)

typealias Bool = Boolean

data class AnnonceUser(
    val _id: String,
    val username: String,
    val image: String?
)

// --- Notifications ---
data class NotificationItem(
    @SerializedName("_id") val id: String,
    val title: String,
    val message: String,
    val type: String,
    val payload: Map<String, Any>?,
    @SerializedName("read") val isRead: Boolean,
    val createdAt: String,
    val actionable: Boolean? = null,
    val responded: Boolean? = null,
    val meetingUrl: String? = null,
    // enriched for reschedule/presence
    val sessionId: String? = null,
    val senderId: String? = null,
    val recipientId: String? = null,
    val proposedDate: String? = null,
    val reason: String? = null
)

data class NotificationsResponse(
    val items: List<NotificationItem>,
    val total: Int = 0,
    val limit: Int = 0,
    val skip: Int = 0,
    val hasNextPage: Boolean = false,
    val page: Int? = null
)

data class ModerationResult(
    val safe: Boolean,
    val reasons: List<String>? = null,
    val categories: List<String>? = null,
    val message: String? = null
)
