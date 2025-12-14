package com.skillswap.network

import com.skillswap.model.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.skillswap.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.DELETE
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface SkillSwapApi {
    @POST("/auth/login")
    suspend fun login(@Body body: Map<String, String>): SignInResponse

    @POST("/auth/register")
    suspend fun register(@Body body: Map<String, String>): SignInResponse
    @POST("/users/forgot-password")
    suspend fun forgotPassword(@Body body: ForgotPasswordRequest): ForgotPasswordResponse

    @POST("/users/referrals/validate")
    suspend fun validateReferral(@Body body: Map<String, String>): ReferralPreview

    @GET("/users/me")
    suspend fun getMe(@Header("Authorization") token: String): User
    @PATCH("/users/me")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): User

    @Multipart
    @PATCH("/users/me/image")
    suspend fun uploadProfileImage(
        @Header("Authorization") token: String,
        @Part image: MultipartBody.Part
    ): User

    @GET("/sessions/{id}")
    suspend fun getSession(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Session

    @GET("/sessions/me")
    suspend fun getSessions(@Header("Authorization") token: String): List<Session>
    
    @GET("/sessions/recommendations")
    suspend fun getSessionRecommendations(@Header("Authorization") token: String): RecommendationResponse
    
    @POST("/sessions")
    suspend fun createSession(
        @Header("Authorization") token: String,
        @Body body: CreateSessionRequest
    ): Session
    
    @GET("/sessions/availability")
    suspend fun checkAvailability(
        @Header("Authorization") token: String,
        @Query("userId") userId: String,
        @Query("date") date: String
    ): AvailabilityResponse
    
    @GET("/users/search")
    suspend fun searchUsers(
        @Header("Authorization") token: String,
        @Query("q") query: String,
        @Query("limit") limit: Int? = 10
    ): List<User>
    
    @PATCH("/sessions/{id}/status")
    suspend fun updateSessionStatus(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body body: Map<String, String>
    ): Session
    @POST("/sessions/{id}/reschedule")
    suspend fun proposeReschedule(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body body: RescheduleProposalPayload
    ): Session
    @POST("/sessions/{id}/vote")
    suspend fun respondToReschedule(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body body: Map<String, Boolean>
    ): Session
    @POST("/sessions/{id}/rate")
    suspend fun rateSession(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body body: RateSessionRequest
    ): Map<String, Any>

    @GET("/chat/threads")
    suspend fun getThreads(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int? = null,
        @Query("skip") skip: Int? = null
    ): ThreadListResponse

    @POST("/chat/threads")
    suspend fun createThread(
        @Header("Authorization") token: String,
        @Body body: Map<String, String>
    ): ChatThread

    @GET("/chat/threads/{threadId}/messages")
    suspend fun getMessages(
        @Header("Authorization") token: String,
        @Path("threadId") threadId: String
    ): MessagesResponse

    @POST("/chat/threads/{threadId}/messages")
    suspend fun sendMessage(
        @Header("Authorization") token: String,
        @Path("threadId") threadId: String,
        @Body body: Map<String, String>
    ): ThreadMessage

    @POST("/chat/threads/{threadId}/read")
    suspend fun markThreadRead(
        @Header("Authorization") token: String,
        @Path("threadId") threadId: String,
        @Body body: Map<String, List<String>?>
    ): Map<String, Any>
    
    @POST("/chat/messages/{messageId}/react")
    suspend fun reactToMessage(
        @Header("Authorization") token: String,
        @Path("messageId") messageId: String,
        @Body body: Map<String, String>
    ): ThreadMessage
    
    @DELETE("/chat/messages/{messageId}")
    suspend fun deleteMessage(
        @Header("Authorization") token: String,
        @Path("messageId") messageId: String
    ): ThreadMessage

    @Multipart
    @POST("/chat/threads/{threadId}/upload")
    suspend fun uploadChatAttachment(
        @Header("Authorization") token: String,
        @Path("threadId") threadId: String,
        @Part file: MultipartBody.Part,
        @Part("content") content: okhttp3.RequestBody? = null
    ): ThreadMessage

    // Matching / discovery
    @GET("/matching/recommendations")
    suspend fun getRecommendations(
        @Header("Authorization") token: String,
        @Query("city") city: String? = null,
        @Query("skill") skill: String? = null,
        @Query("limit") limit: Int? = null
    ): List<User>
    
    @POST("/matching/interest/{userId}")
    suspend fun expressInterest(
        @Header("Authorization") token: String,
        @Path("userId") userId: String
    ): MatchResponse

    @GET("/locations/filters")
    suspend fun getLocationFilters(
        @Header("Authorization") token: String
    ): Map<String, List<String>>

    @GET("/locations/cities")
    suspend fun getCities(): List<String>

    // Progress
    @GET("/progress/dashboard")
    suspend fun getProgressDashboard(@Header("Authorization") token: String): ProgressDashboardResponse
    @POST("/progress/goals")
    suspend fun createGoal(@Header("Authorization") token: String, @Body body: CreateGoalRequest): ProgressGoalItem
    @PATCH("/progress/goals/{id}")
    suspend fun updateGoal(@Header("Authorization") token: String, @Path("id") id: String, @Body body: UpdateGoalRequest): ProgressGoalItem
    @DELETE("/progress/goals/{id}")
    suspend fun deleteGoal(@Header("Authorization") token: String, @Path("id") id: String): Map<String, Any>

    // Promos
    @GET("/promos/me")
    suspend fun getMyPromos(@Header("Authorization") token: String): List<Promo>
    @POST("/promos")
    suspend fun createPromo(
        @Header("Authorization") token: String,
        @Body body: CreatePromoRequest
    ): Promo
    @PATCH("/promos/{id}")
    suspend fun updatePromo(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body body: UpdatePromoRequest
    ): Promo
    @Multipart
    @PATCH("/promos/{id}/image")
    suspend fun uploadPromoImage(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Part image: MultipartBody.Part
    ): Promo
    
    @DELETE("/promos/{id}")
    suspend fun deletePromo(@Header("Authorization") token: String, @Path("id") id: String)

    // AI Image Generation
    @POST("/ai/generate-image")
    suspend fun generateImage(
        @Header("Authorization") token: String,
        @Body body: Map<String, String>
    ): Map<String, String>

    // Annonces
    @GET("/annonces/me")
    suspend fun getMyAnnonces(@Header("Authorization") token: String): List<Annonce>
    @POST("/annonces")
    suspend fun createAnnonce(
        @Header("Authorization") token: String,
        @Body body: CreateAnnonceRequest
    ): Annonce

    @PATCH("/annonces/{id}")
    suspend fun updateAnnonce(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body body: UpdateAnnonceRequest
    ): Annonce
    @Multipart
    @PATCH("/annonces/{id}/image")
    suspend fun uploadAnnonceImage(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Part image: MultipartBody.Part
    ): Annonce

    @DELETE("/annonces/{id}")
    suspend fun deleteAnnonce(@Header("Authorization") token: String, @Path("id") id: String)


    // Discover
    @GET("/users")
    suspend fun getUsers(@Header("Authorization") token: String): List<User>
    
    @GET("/annonces/{id}")
    suspend fun getAnnonce(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Annonce

    @GET("/annonces")
    suspend fun getAllAnnonces(@Header("Authorization") token: String): List<Annonce>

    @GET("/promos/{id}")
    suspend fun getPromo(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Promo

    @GET("/promos")
    suspend fun getAllPromos(@Header("Authorization") token: String): List<Promo>

    // Notifications
    @GET("/notifications")
    suspend fun getNotifications(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int? = null,
        @Query("page") page: Int? = null,
        @Query("status") status: String? = null
    ): NotificationsResponse

    @GET("/notifications/unread-count")
    suspend fun getUnreadCount(@Header("Authorization") token: String): Map<String, Int>

    @POST("/notifications/mark-read")
    suspend fun markNotificationsRead(
        @Header("Authorization") token: String,
        @Body body: Map<String, List<String>>
    ): Map<String, Int>
    @POST("/notifications/mark-all-read")
    suspend fun markAllNotificationsRead(
        @Header("Authorization") token: String
    ): Map<String, Int>
    @POST("/notifications/{id}/respond")
    suspend fun respondNotification(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body body: Map<String, Any?>
    ): NotificationItem
    // Lesson plans / AI
    @GET("/lesson-plan/{sessionId}")
    suspend fun getLessonPlan(
        @Header("Authorization") token: String,
        @Path("sessionId") sessionId: String
    ): LessonPlanResponse

    @POST("/lesson-plan/generate/{sessionId}")
    suspend fun generateLessonPlan(
        @Header("Authorization") token: String,
        @Path("sessionId") sessionId: String,
        @Body body: LessonPlanGenerateRequest
    ): LessonPlanResponse

    @POST("/lesson-plan/regenerate/{sessionId}")
    suspend fun regenerateLessonPlan(
        @Header("Authorization") token: String,
        @Path("sessionId") sessionId: String,
        @Body body: LessonPlanGenerateRequest
    ): LessonPlanResponse

    @PATCH("/lesson-plan/progress/{sessionId}")
    suspend fun updateLessonPlanProgress(
        @Header("Authorization") token: String,
        @Path("sessionId") sessionId: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): LessonPlanResponse

    // Moderation
    @POST("/moderation/check-image")
    suspend fun checkImage(@Header("Authorization") token: String, @Body body: Map<String, String>): ModerationResult

    // Referrals
    @GET("/referrals/me")
    suspend fun getMyReferrals(@Header("Authorization") token: String): ReferralsMeResponse

    @POST("/referrals/codes")
    suspend fun createReferralCode(
        @Header("Authorization") token: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): ReferralCodeResponse

    @POST("/referrals/redeem")
    suspend fun redeemReferral(
        @Header("Authorization") token: String,
        @Body body: Map<String, String>
    ): RedeemResponse

    // Weekly objectives
    @GET("/weekly-objectives/current")
    suspend fun getCurrentWeeklyObjective(@Header("Authorization") token: String): WeeklyObjective

    @GET("/weekly-objectives/history")
    suspend fun getWeeklyObjectiveHistory(
        @Header("Authorization") token: String,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): WeeklyObjectiveHistoryResponse

    @POST("/weekly-objectives")
    suspend fun createWeeklyObjective(
        @Header("Authorization") token: String,
        @Body body: CreateWeeklyObjectiveRequest
    ): WeeklyObjective

    @PATCH("/weekly-objectives/{id}")
    suspend fun updateWeeklyObjective(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body body: UpdateWeeklyObjectiveRequest
    ): WeeklyObjective

    @PATCH("/weekly-objectives/{id}/complete")
    suspend fun completeWeeklyObjective(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): WeeklyObjective

    @GET("/locations/cities")
    suspend fun getCities(
        @Header("Authorization") token: String
    ): List<String>

    @DELETE("/weekly-objectives/{id}")
    suspend fun deleteWeeklyObjective(
        @Header("Authorization") token: String,
        @Path("id") id: String
    )
    
    // ============ Calendar Events ============
    
    @GET("/calendar/events")
    suspend fun getCalendarEvents(
        @Header("Authorization") token: String,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): CalendarEventsResponse
    
    @GET("/calendar/events/{id}")
    suspend fun getCalendarEvent(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): CalendarEvent
    
    @POST("/calendar/events")
    suspend fun createCalendarEvent(
        @Header("Authorization") token: String,
        @Body body: CreateEventRequest
    ): CalendarEvent
    
    @PATCH("/calendar/events/{id}")
    suspend fun updateCalendarEvent(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body body: UpdateEventRequest
    ): CalendarEvent
    
    @DELETE("/calendar/events/{id}")
    suspend fun deleteCalendarEvent(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Map<String, Any>
    
    @POST("/calendar/events/{id}/participants")
    suspend fun addEventParticipant(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body body: Map<String, String>
    ): CalendarEvent
    
    @DELETE("/calendar/events/{id}/participants/{userId}")
    suspend fun removeEventParticipant(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Path("userId") userId: String
    ): CalendarEvent
    
    @POST("/calendar/events/{id}/respond")
    suspend fun respondToEventInvite(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body body: Map<String, String>
    ): CalendarEvent
    
    // ============ Google Calendar Integration ============
    
    @GET("/calendar/google/auth-url")
    suspend fun getGoogleCalendarAuthUrl(
        @Header("Authorization") token: String
    ): GoogleCalendarAuthResponse
    
    @POST("/calendar/google/callback")
    suspend fun handleGoogleCalendarCallback(
        @Header("Authorization") token: String,
        @Body body: GoogleCalendarTokenRequest
    ): Map<String, Any>
    
    @POST("/calendar/google/sync")
    suspend fun syncWithGoogleCalendar(
        @Header("Authorization") token: String,
        @Body body: Map<String, Any>? = null
    ): GoogleCalendarSyncResult
    
    @DELETE("/calendar/google/disconnect")
    suspend fun disconnectGoogleCalendar(
        @Header("Authorization") token: String
    ): Map<String, Any>
    
    @GET("/calendar/google/status")
    suspend fun getGoogleCalendarStatus(
        @Header("Authorization") token: String
    ): Map<String, Any>
}

object NetworkService {
    private val BASE_URL = BuildConfig.API_BASE_URL.let { value ->
        val url = when {
            value.isBlank() -> {
                // Auto-detect: Use 10.0.2.2 for emulator, otherwise use network IP
                val isEmulator = isRunningOnEmulator()
                android.util.Log.d("NetworkService", "Device Detection - MODEL: ${android.os.Build.MODEL}, isEmulator: $isEmulator")
                if (isEmulator) {
                    "http://10.0.2.2:3000/" // Emulator localhost
                } else {
                    "http://192.168.1.15:3000/" // Physical device - your computer's network IP
                }
            }
            value.endsWith("/") -> value
            else -> "$value/"
        }
        android.util.Log.d("NetworkService", "Using BASE_URL: $url")
        url
    }
    
    private fun isRunningOnEmulator(): Boolean {
        return (android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")
                || android.os.Build.MODEL.contains("sdk_gphone")
                || android.os.Build.MODEL.contains("sdk_google")
                || android.os.Build.MANUFACTURER.contains("Genymotion")
                || (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic"))
                || android.os.Build.HARDWARE.contains("goldfish")
                || android.os.Build.HARDWARE.contains("ranchu")
                || "google_sdk" == android.os.Build.PRODUCT
                || android.os.Build.PRODUCT.contains("sdk_gphone")
                || android.os.Build.PRODUCT.contains("sdk_google")
                || android.os.Build.PRODUCT.contains("emulator"))
    }

    private val gson: Gson = GsonBuilder()
        .setLenient()
        .create()

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
        )
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val api: SkillSwapApi = retrofit.create(SkillSwapApi::class.java)
}
