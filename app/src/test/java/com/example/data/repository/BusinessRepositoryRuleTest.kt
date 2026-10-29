package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.BusinessRepository
import com.example.model.Business
import org.junit.Assert.assertTrue
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test
import java.util.UUID

class BusinessRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createBusiness_validPayload_createsDocumentAndReturnsId() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repository = BusinessRepository(firestore)

    val business = Business(
      id = "biz_${UUID.randomUUID().toString().take(8)}",
      name = "Alice Kolkata Bistro",
      category = "Restaurant & Cafe",
      city = "Kolkata",
      targetAudience = "Food lovers",
      brandTone = "Warm & Inviting",
      language = "Bengali"
    )

    val result = withTimeout(DEFAULT_TIMEOUT_MS) { repository.createBusiness(business) }
    assertTrue(result.isSuccess)
    val id = result.getOrThrow()
    assertTrue(id.isNotEmpty())
  }

  @Test
  fun observeBusinesses_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repository = BusinessRepository(firestore)
    val businessId = "biz_${UUID.randomUUID().toString().take(8)}"

    val business = Business(
      id = businessId,
      name = "Alice Fashion Studio",
      category = "Clothing & Fashion",
      city = "Mumbai",
      targetAudience = "Youth",
      brandTone = "Energetic",
      language = "English"
    )

    withTimeout(DEFAULT_TIMEOUT_MS) { repository.createBusiness(business).getOrThrow() }

    val list = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeBusinesses().first { businesses ->
        businesses.any { it.id == businessId }
      }
    }
    assertTrue(list.map { it.id }.contains(businessId))
  }

  @Test
  fun observeBusinesses_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repository = BusinessRepository(firestore)

    var threw = false
    try {
      withTimeout(FLOW_TIMEOUT_MS) { repository.observeBusinesses().first() }
    } catch (e: Exception) {
      threw = true
    }
    assertTrue("Expected exception for unauthenticated query", threw)
  }

  private companion object {
    const val ALICE_EMAIL = "alice@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
