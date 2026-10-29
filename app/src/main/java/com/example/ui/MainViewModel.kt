package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.ContentGenerator
import com.example.ai.GenerationState
import com.example.data.BusinessRepository
import com.example.data.MarketingRepository
import com.example.data.UserRepository
import com.example.model.Business
import com.example.model.Campaign
import com.example.model.IndustryTemplate
import com.example.model.MarketingPost
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

private const val TAG = "MainViewModel"

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = Firebase.auth
    private val businessRepo = BusinessRepository(application)
    private val marketingRepo = MarketingRepository(application)
    private val userRepo = UserRepository(application)
    private val contentGenerator = ContentGenerator()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _businesses = MutableStateFlow<List<Business>>(emptyList())
    val businesses: StateFlow<List<Business>> = _businesses.asStateFlow()

    private val _selectedBusiness = MutableStateFlow<Business?>(null)
    val selectedBusiness: StateFlow<Business?> = _selectedBusiness.asStateFlow()

    private val _calendarPosts = MutableStateFlow<List<MarketingPost>>(emptyList())
    val calendarPosts: StateFlow<List<MarketingPost>> = _calendarPosts.asStateFlow()

    private val _savedPosts = MutableStateFlow<List<MarketingPost>>(emptyList())
    val savedPosts: StateFlow<List<MarketingPost>> = _savedPosts.asStateFlow()

    private val _campaigns = MutableStateFlow<List<Campaign>>(emptyList())
    val campaigns: StateFlow<List<Campaign>> = _campaigns.asStateFlow()

    private val _generationState = MutableStateFlow<GenerationState>(GenerationState.Idle)
    val generationState: StateFlow<GenerationState> = _generationState.asStateFlow()

    private val _activeTab = MutableStateFlow(0) // 0: Home, 1: Generator, 2: Calendar, 3: Saved, 4: Businesses
    val activeTab: StateFlow<Int> = _activeTab.asStateFlow()

    // Transient prefill states
    var prefillOfferTopic: String = ""
    var prefillLanguage: String = "English"
    var prefillCampaignType: String = "weekly_calendar"

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _currentUser.value = user
            if (user != null) {
                initializeUserData(user)
            } else {
                _userProfile.value = null
                _businesses.value = emptyList()
                _selectedBusiness.value = null
                _calendarPosts.value = emptyList()
                _savedPosts.value = emptyList()
            }
        }
    }

    fun setActiveTab(tab: Int) {
        _activeTab.value = tab
    }

    private fun initializeUserData(user: FirebaseUser) {
        viewModelScope.launch {
            userRepo.getOrCreateUserProfile(
                email = user.email.orEmpty(),
                displayName = user.displayName ?: "Business Owner"
            ).onSuccess { profile ->
                _userProfile.value = profile
            }

            // Observe user profile
            launch {
                userRepo.observeUserProfile()
                    .catch { Log.e(TAG, "Error observing profile", it) }
                    .collect { _userProfile.value = it }
            }

            // Observe businesses
            launch {
                businessRepo.observeBusinesses()
                    .catch { Log.e(TAG, "Error observing businesses", it) }
                    .collect { list ->
                        _businesses.value = list
                        if (list.isNotEmpty() && _selectedBusiness.value == null) {
                            _selectedBusiness.value = list.first()
                        } else if (list.isNotEmpty() && _selectedBusiness.value != null) {
                            // Keep matching or fallback
                            _selectedBusiness.value = list.find { it.id == _selectedBusiness.value?.id } ?: list.first()
                        } else if (list.isEmpty()) {
                            // Seed default demo business if none exists
                            seedDefaultBusiness(user.uid)
                        }
                    }
            }

            // Observe calendar and saved posts
            launch {
                marketingRepo.observePosts()
                    .catch { Log.e(TAG, "Error observing posts", it) }
                    .collect { allPosts ->
                        _calendarPosts.value = allPosts.filter { it.dayOfWeek != "Special Promo" }
                        _savedPosts.value = allPosts.filter { it.isSaved }
                    }
            }

            // Observe campaigns
            launch {
                marketingRepo.observeCampaigns()
                    .catch { Log.e(TAG, "Error observing campaigns", it) }
                    .collect { _campaigns.value = it }
            }
        }
    }

    private suspend fun seedDefaultBusiness(uid: String) {
        val defaultBiz = Business(
            userId = uid,
            name = "Sonar Bangla Diner",
            category = "Restaurant & Cafe",
            city = "Kolkata",
            targetAudience = "Families, college students, and traditional food lovers",
            brandTone = "Warm & Inviting",
            language = "Bengali"
        )
        businessRepo.createBusiness(defaultBiz).onSuccess { id ->
            _selectedBusiness.value = defaultBiz.copy(id = id)
        }
    }

    fun selectBusiness(business: Business) {
        _selectedBusiness.value = business
    }

    fun saveBusiness(
        id: String = "",
        name: String,
        category: String,
        city: String,
        targetAudience: String,
        brandTone: String,
        language: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val biz = Business(
                id = id,
                name = name,
                category = category,
                city = city,
                targetAudience = targetAudience,
                brandTone = brandTone,
                language = language
            )
            val result = if (id.isBlank()) {
                businessRepo.createBusiness(biz)
            } else {
                businessRepo.updateBusiness(biz)
            }
            if (result.isSuccess) {
                _selectedBusiness.value = biz
                onComplete(true)
            } else {
                onComplete(false)
            }
        }
    }

    fun deleteBusiness(businessId: String) {
        viewModelScope.launch {
            businessRepo.deleteBusiness(businessId)
            if (_selectedBusiness.value?.id == businessId) {
                _selectedBusiness.value = _businesses.value.firstOrNull { it.id != businessId }
            }
        }
    }

    fun generateContent(
        campaignType: String,
        offerTopic: String,
        selectedLanguage: String
    ) {
        val currentBiz = _selectedBusiness.value ?: return
        viewModelScope.launch {
            // Check quota
            if (!userRepo.canGenerateContent()) {
                _generationState.value = GenerationState.Error("Monthly quota reached! Please upgrade to Starter or Agency plan.")
                return@launch
            }

            _generationState.value = GenerationState.Generating

            if (campaignType == "weekly_calendar") {
                val (posts, summary) = contentGenerator.generateWeeklyCalendar(
                    business = currentBiz,
                    offerOrTopic = offerTopic,
                    selectedLanguage = selectedLanguage
                )
                if (posts.isNotEmpty()) {
                    userRepo.recordGeneration()
                    _generationState.value = GenerationState.Success(posts, summary)
                } else {
                    _generationState.value = GenerationState.Error("Could not generate content. Please retry.")
                }
            } else {
                val post = contentGenerator.generatePromotionalPost(
                    business = currentBiz,
                    campaignType = campaignType,
                    offerTopic = offerTopic,
                    selectedLanguage = selectedLanguage
                )
                userRepo.recordGeneration()
                _generationState.value = GenerationState.Success(
                    listOf(post),
                    "Promotional content generated for ${currentBiz.name}"
                )
            }
        }
    }

    fun saveGeneratedCampaign(title: String, posts: List<MarketingPost>, onComplete: () -> Unit = {}) {
        val currentBiz = _selectedBusiness.value ?: return
        viewModelScope.launch {
            val campaign = Campaign(
                businessId = currentBiz.id,
                businessName = currentBiz.name,
                title = title,
                campaignType = "weekly_calendar",
                language = posts.firstOrNull()?.language ?: currentBiz.language,
                summary = "7-Day plan for ${currentBiz.name}"
            )
            marketingRepo.saveCampaignWithPosts(campaign, posts)
            onComplete()
        }
    }

    fun saveSinglePost(post: MarketingPost, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            marketingRepo.saveMarketingPost(post)
            onComplete()
        }
    }

    fun toggleBookmarkPost(post: MarketingPost) {
        viewModelScope.launch {
            marketingRepo.toggleSavePost(post.id, !post.isSaved)
        }
    }

    fun updatePost(post: MarketingPost) {
        viewModelScope.launch {
            marketingRepo.saveMarketingPost(post)
        }
    }

    fun deletePost(post: MarketingPost) {
        viewModelScope.launch {
            marketingRepo.deletePost(post.id)
        }
    }

    fun upgradePlan(newPlan: String) {
        viewModelScope.launch {
            userRepo.upgradePlan(newPlan)
        }
    }

    fun applyTemplate(template: IndustryTemplate) {
        prefillOfferTopic = template.offerPrompt
        prefillLanguage = template.language
        prefillCampaignType = "weekly_calendar"
        _activeTab.value = 1 // Switch to Generator screen
    }

    fun resetGenerationState() {
        _generationState.value = GenerationState.Idle
    }
}
