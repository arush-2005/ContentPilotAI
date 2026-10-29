package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val plan: String = "free", // "free", "starter", "agency"
    val generationsUsed: Long = 0L,
    val generationLimit: Long = 10L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Business(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val category: String = "Restaurant & Cafe",
    val city: String = "Kolkata",
    val targetAudience: String = "Food lovers & families",
    val brandTone: String = "Warm & Inviting",
    val language: String = "English", // "English", "Hindi", "Bengali"
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Campaign(
    @DocumentId val id: String = "",
    val userId: String = "",
    val businessId: String = "",
    val businessName: String = "",
    val title: String = "",
    val campaignType: String = "weekly_calendar", // "weekly_calendar", "festival_special", "promo_post", "launch"
    val language: String = "English",
    val summary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MarketingPost(
    @DocumentId val id: String = "",
    val userId: String = "",
    val businessId: String = "",
    val campaignId: String = "",
    val dayOfWeek: String = "Monday",
    val theme: String = "Fresh Kickoff",
    val caption: String = "",
    val hashtags: String = "",
    val callToAction: String = "",
    val visualIdea: String = "",
    val bestTime: String = "11:00 AM",
    val language: String = "English",
    val isSaved: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class IndustryTemplate(
    val id: String,
    val title: String,
    val category: String,
    val sampleBusinessName: String,
    val city: String,
    val targetAudience: String,
    val brandTone: String,
    val language: String,
    val offerPrompt: String,
    val iconEmoji: String
)
