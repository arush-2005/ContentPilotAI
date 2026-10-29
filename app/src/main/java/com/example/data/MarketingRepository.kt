package com.example.data

import android.content.Context
import com.example.R
import com.example.model.Campaign
import com.example.model.MarketingPost
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class MarketingRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observePosts(businessId: String? = null): Flow<List<MarketingPost>> = flow {
        val uid = requireUserId()
        val path = "calendar_posts"
        var query = db.collection(path).whereEqualTo("userId", uid)
        if (!businessId.isNullOrBlank()) {
            query = query.whereEqualTo("businessId", businessId)
        }
        query.snapshots()
            .map { snapshot -> snapshot.toObjects(MarketingPost::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
            .collect { emit(it) }
    }

    fun observeCampaigns(businessId: String? = null): Flow<List<Campaign>> = flow {
        val uid = requireUserId()
        val path = "campaigns"
        var query = db.collection(path).whereEqualTo("userId", uid)
        if (!businessId.isNullOrBlank()) {
            query = query.whereEqualTo("businessId", businessId)
        }
        query.snapshots()
            .map { snapshot -> snapshot.toObjects(Campaign::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
            .collect { emit(it) }
    }

    suspend fun saveMarketingPost(post: MarketingPost): Result<String> {
        val uid = requireUserId()
        val docId = if (post.id.isNotBlank()) post.id else UUID.randomUUID().toString()
        val path = "calendar_posts/$docId"
        val payload = mutableMapOf<String, Any>(
            "userId" to uid,
            "businessId" to post.businessId,
            "dayOfWeek" to post.dayOfWeek,
            "theme" to post.theme,
            "caption" to post.caption,
            "hashtags" to post.hashtags,
            "callToAction" to post.callToAction,
            "visualIdea" to post.visualIdea,
            "bestTime" to post.bestTime,
            "language" to post.language,
            "isSaved" to post.isSaved,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (post.campaignId.isNotBlank()) {
            payload["campaignId"] = post.campaignId
        }
        return try {
            db.collection("calendar_posts").document(docId).set(payload).await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveCampaignWithPosts(
        campaign: Campaign,
        posts: List<MarketingPost>
    ): Result<String> {
        val uid = requireUserId()
        val campaignId = if (campaign.id.isNotBlank()) campaign.id else UUID.randomUUID().toString()
        val campaignPath = "campaigns/$campaignId"
        val campaignPayload = mapOf(
            "userId" to uid,
            "businessId" to campaign.businessId,
            "businessName" to campaign.businessName,
            "title" to campaign.title,
            "campaignType" to campaign.campaignType,
            "language" to campaign.language,
            "summary" to campaign.summary,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            db.collection("campaigns").document(campaignId).set(campaignPayload).await()
            for (post in posts) {
                val postId = if (post.id.isNotBlank()) post.id else UUID.randomUUID().toString()
                val postPayload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "businessId" to campaign.businessId,
                    "campaignId" to campaignId,
                    "dayOfWeek" to post.dayOfWeek,
                    "theme" to post.theme,
                    "caption" to post.caption,
                    "hashtags" to post.hashtags,
                    "callToAction" to post.callToAction,
                    "visualIdea" to post.visualIdea,
                    "bestTime" to post.bestTime,
                    "language" to post.language,
                    "isSaved" to post.isSaved,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.collection("calendar_posts").document(postId).set(postPayload).await()
            }
            Result.success(campaignId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, campaignPath)
            Result.failure(e)
        }
    }

    suspend fun toggleSavePost(postId: String, isSaved: Boolean): Result<Unit> {
        val uid = requireUserId()
        val path = "calendar_posts/$postId"
        return try {
            db.collection("calendar_posts").document(postId).update(
                mapOf(
                    "isSaved" to isSaved,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> {
        val uid = requireUserId()
        val path = "calendar_posts/$postId"
        return try {
            db.collection("calendar_posts").document(postId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
