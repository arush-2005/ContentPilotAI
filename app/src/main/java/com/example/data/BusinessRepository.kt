package com.example.data

import android.content.Context
import com.example.R
import com.example.model.Business
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

class BusinessRepository(private val db: FirebaseFirestore) {

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

    fun observeBusinesses(): Flow<List<Business>> = flow {
        val uid = requireUserId()
        val path = "businesses"
        db.collection(path)
            .whereEqualTo("userId", uid)
            .snapshots()
            .map { snapshot -> snapshot.toObjects(Business::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
            .collect { emit(it) }
    }

    suspend fun createBusiness(business: Business): Result<String> {
        val uid = requireUserId()
        val docId = if (business.id.isNotBlank()) business.id else UUID.randomUUID().toString()
        val path = "businesses/$docId"
        val payload = mapOf(
            "userId" to uid,
            "name" to business.name.trim(),
            "category" to business.category.trim(),
            "city" to business.city.trim(),
            "targetAudience" to business.targetAudience.trim(),
            "brandTone" to business.brandTone.trim(),
            "language" to business.language.trim(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            db.collection("businesses").document(docId).set(payload).await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateBusiness(business: Business): Result<Unit> {
        val uid = requireUserId()
        require(business.id.isNotBlank()) { "Business ID cannot be blank for update" }
        val path = "businesses/${business.id}"
        val updates = mapOf<String, Any>(
            "name" to business.name.trim(),
            "category" to business.category.trim(),
            "city" to business.city.trim(),
            "targetAudience" to business.targetAudience.trim(),
            "brandTone" to business.brandTone.trim(),
            "language" to business.language.trim(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            db.collection("businesses").document(business.id).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteBusiness(businessId: String): Result<Unit> {
        val uid = requireUserId()
        val path = "businesses/$businessId"
        return try {
            db.collection("businesses").document(businessId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
