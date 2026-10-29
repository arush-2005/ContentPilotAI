package com.example.data

import android.content.Context
import com.example.R
import com.example.model.UserProfile
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

class UserRepository(private val db: FirebaseFirestore) {

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

    fun observeUserProfile(): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        val path = "user_profiles/$uid"
        db.collection("user_profiles").document(uid)
            .snapshots()
            .map { it.toObject(UserProfile::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                throw error
            }
            .collect { emit(it) }
    }

    suspend fun getOrCreateUserProfile(email: String, displayName: String): Result<UserProfile> {
        val uid = requireUserId()
        val path = "user_profiles/$uid"
        val docRef = db.collection("user_profiles").document(uid)
        return try {
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val profile = snapshot.toObject(UserProfile::class.java)
                Result.success(profile ?: UserProfile(userId = uid, email = email, displayName = displayName))
            } else {
                val newProfileMap = mapOf(
                    "userId" to uid,
                    "email" to email,
                    "displayName" to displayName,
                    "plan" to "free",
                    "generationsUsed" to 0L,
                    "generationLimit" to 10L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(newProfileMap).await()
                Result.success(
                    UserProfile(
                        userId = uid,
                        email = email,
                        displayName = displayName,
                        plan = "free",
                        generationsUsed = 0L,
                        generationLimit = 10L
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun canGenerateContent(): Boolean {
        return try {
            val uid = requireUserId()
            val snapshot = db.collection("user_profiles").document(uid).get().await()
            val used = snapshot.getLong("generationsUsed") ?: 0L
            val limit = snapshot.getLong("generationLimit") ?: 10L
            used < limit
        } catch (e: Exception) {
            true // Allow generation in case of transient read error
        }
    }

    suspend fun recordGeneration(): Result<Unit> {
        val uid = requireUserId()
        val path = "user_profiles/$uid"
        return try {
            db.collection("user_profiles").document(uid).update(
                mapOf(
                    "generationsUsed" to FieldValue.increment(1L),
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

    suspend fun upgradePlan(newPlan: String): Result<Unit> {
        val uid = requireUserId()
        val limit = when (newPlan.lowercase()) {
            "agency" -> 999L
            "starter" -> 100L
            else -> 10L
        }
        val path = "user_profiles/$uid"
        return try {
            db.collection("user_profiles").document(uid).update(
                mapOf(
                    "plan" to newPlan.lowercase(),
                    "generationLimit" to limit,
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
}
