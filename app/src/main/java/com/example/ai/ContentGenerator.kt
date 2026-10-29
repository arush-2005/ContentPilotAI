package com.example.ai

import android.util.Log
import com.example.model.Business
import com.example.model.MarketingPost
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private const val TAG = "ContentGenerator"

sealed interface GenerationState {
    data object Idle : GenerationState
    data object Generating : GenerationState
    data class Success(val posts: List<MarketingPost>, val summary: String) : GenerationState
    data class Error(val message: String) : GenerationState
}

class ContentGenerator {

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.7f
                topP = 0.95f
            }
        )
    }

    suspend fun generateWeeklyCalendar(
        business: Business,
        offerOrTopic: String,
        selectedLanguage: String = business.language
    ): Pair<List<MarketingPost>, String> {
        val prompt = buildCalendarPrompt(business, offerOrTopic, selectedLanguage)

        try {
            val response = withTimeout(45_000L) {
                model.generateContent(prompt)
            }
            val text = response.text
            if (!text.isNullOrBlank()) {
                val parsed = parseCalendarJson(text, business, selectedLanguage)
                if (parsed.isNotEmpty()) {
                    return Pair(parsed, "Weekly localized schedule created for ${business.name} in $selectedLanguage")
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Gemini generation timed out, falling back to local generator", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Gemini error: ${e.message}, employing intelligent local generator", e)
        }

        // Reliable fallback engine
        val fallbackPosts = generateFallbackWeeklyCalendar(business, offerOrTopic, selectedLanguage)
        return Pair(fallbackPosts, "7-Day localized campaign generated for ${business.name}")
    }

    suspend fun generatePromotionalPost(
        business: Business,
        campaignType: String,
        offerTopic: String,
        selectedLanguage: String = business.language
    ): MarketingPost {
        val prompt = """
            You are ContentPilot AI, an elite marketing assistant for local small businesses in India.
            Create a high-converting promotional post for:
            Business: ${business.name} (${business.category}) in ${business.city}
            Target Audience: ${business.targetAudience}
            Tone: ${business.brandTone}
            Language: $selectedLanguage (Important: If Bengali, write in authentic Bengali script with warm Kolkata/Bengal touch. If Hindi, write in catchy Hindi with vibrant tone. If English, write in engaging modern Indian English).
            Offer/Topic: $offerTopic
            Campaign Type: $campaignType

            Return ONLY valid JSON matching this schema:
            {
              "theme": "Catchy theme or hook title",
              "caption": "Full high-converting social caption with emojis and paragraphs",
              "hashtags": "#relevant #localized #hashtags",
              "callToAction": "Clear CTA (e.g. Call/WhatsApp, Visit store, DM us)",
              "visualIdea": "Photo or 5-second Reel direction",
              "bestTime": "Recommended posting time (e.g. 12:30 PM)"
            }
        """.trimIndent()

        try {
            val response = withTimeout(45_000L) {
                model.generateContent(prompt)
            }
            val text = response.text
            if (!text.isNullOrBlank()) {
                val clean = cleanJson(text)
                val json = JSONObject(clean)
                return MarketingPost(
                    id = UUID.randomUUID().toString(),
                    businessId = business.id,
                    dayOfWeek = "Special Promo",
                    theme = json.optString("theme", "Special Offer"),
                    caption = json.optString("caption"),
                    hashtags = json.optString("hashtags"),
                    callToAction = json.optString("callToAction"),
                    visualIdea = json.optString("visualIdea"),
                    bestTime = json.optString("bestTime", "6:00 PM"),
                    language = selectedLanguage,
                    isSaved = false
                )
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Promo generation timed out", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Promo generation error", e)
        }

        return generateFallbackSinglePost(business, campaignType, offerTopic, selectedLanguage)
    }

    private fun buildCalendarPrompt(business: Business, offer: String, language: String): String {
        return """
            You are ContentPilot AI, a marketing expert for local Indian businesses.
            Generate a full 7-day marketing content calendar (Monday through Sunday) for:
            Business Name: ${business.name}
            Category: ${business.category}
            Location: ${business.city}
            Target Customers: ${business.targetAudience}
            Brand Voice/Tone: ${business.brandTone}
            Language: $language
            Featured Offer / Focus: $offer

            CRITICAL:
            - If language is 'Bengali', write authentic Bengali (বাংলা লিপি) captions with colloquial warmth and local Kolkata/Bengal touch.
            - If language is 'Hindi', write vibrant, punchy Hindi (हिंदी) with appropriate emojis and relatable hooks.
            - If language is 'English', write engaging, crisp Indian small-business copy.
            - Each day must have a distinct angle:
              Monday: Motivation / Fresh Week kickoff / Behind the scenes
              Tuesday: Product / Menu / Service highlight & pro-tip
              Wednesday: Midweek special / Combo / Flash perk
              Thursday: Customer story / Transformation / Review spotlight
              Friday: Weekend anticipation / Rush / Must-try hook
              Saturday: Best-seller showcase / Live energy / Event
              Sunday: Community gratitude / Sunday family special

            Return ONLY a raw JSON array of 7 objects (no markdown, no extra commentary):
            [
              {
                "dayOfWeek": "Monday",
                "theme": "Behind the Scenes",
                "caption": "Full caption...",
                "hashtags": "#KolkataFood #FoodiesOfBengal",
                "callToAction": "Book your table now on WhatsApp",
                "visualIdea": "Reel of fresh steaming spices being tempered in the kitchen",
                "bestTime": "11:00 AM"
              },
              ...
            ]
        """.trimIndent()
    }

    private fun cleanJson(raw: String): String {
        var str = raw.trim()
        if (str.startsWith("```json")) {
            str = str.removePrefix("```json")
        } else if (str.startsWith("```")) {
            str = str.removePrefix("```")
        }
        if (str.endsWith("```")) {
            str = str.removeSuffix("```")
        }
        return str.trim()
    }

    private fun parseCalendarJson(raw: String, business: Business, language: String): List<MarketingPost> {
        return try {
            val clean = cleanJson(raw)
            val jsonArray = if (clean.startsWith("[")) {
                JSONArray(clean)
            } else {
                JSONObject(clean).optJSONArray("posts") ?: JSONArray()
            }
            val list = mutableListOf<MarketingPost>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    MarketingPost(
                        id = UUID.randomUUID().toString(),
                        businessId = business.id,
                        dayOfWeek = obj.optString("dayOfWeek", "Day ${i + 1}"),
                        theme = obj.optString("theme", "Special Highlight"),
                        caption = obj.optString("caption", ""),
                        hashtags = obj.optString("hashtags", ""),
                        callToAction = obj.optString("callToAction", "Visit us today!"),
                        visualIdea = obj.optString("visualIdea", "Photo of featured offer"),
                        bestTime = obj.optString("bestTime", "12:00 PM"),
                        language = language,
                        isSaved = false
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing AI JSON", e)
            emptyList()
        }
    }

    // Local localized generative fallback
    private fun generateFallbackWeeklyCalendar(
        business: Business,
        offer: String,
        language: String
    ): List<MarketingPost> {
        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

        return when (language.lowercase()) {
            "bengali", "বাংলা" -> generateBengaliWeeklyCalendar(business, offer, days)
            "hindi", "हिंदी" -> generateHindiWeeklyCalendar(business, offer, days)
            else -> generateEnglishWeeklyCalendar(business, offer, days)
        }
    }

    private fun generateBengaliWeeklyCalendar(business: Business, offer: String, days: List<String>): List<MarketingPost> {
        val themes = listOf(
            "নতুন সপ্তাহের শুরু ও বিশেষ আয়োজন",
            "মেনু স্পেশাল ও গোপন রেসিপি",
            "মিড-উইক ধামাকা ও কম্বো অফার",
            "আমাদের প্রিয় গ্রাহকদের কথা",
            "উইকেন্ডের আমেজ ও উৎসবের প্রস্তুতি",
            "শনিবারের ধামাকা ভিড় ও স্পেশাল আকর্ষণ",
            "রবিবারের আড্ডা ও পরিবারের আনন্দ"
        )

        val captions = listOf(
            "নতুন সপ্তাহ শুরু হোক দারুণ স্বাদের সাথে! ✨ ${business.name}-এ নিয়ে এসেছি আপনাদের পছন্দের $offer। বন্ধুদের সাথে ভাগ করে নিন এক অদ্ভুত অভিজ্ঞতা।",
            "সরাসরি আমাদের রান্নাঘর থেকে! 👩‍🍳 প্রতিটি উপাদানে খাঁটি ভালোবাসা ও যত্নের ছোঁয়া। আজই ট্রাই করুন আমাদের স্পেশাল $offer।",
            "বুধবারের অলস দুপুরে একটু এনার্জি দরকার? 🌟 আমাদের স্পেশাল মিড-উইক অফারে পাচ্ছেন দুর্দান্ত ছাড়! মিস করবেন না কিন্তু।",
            "আপনাদের হাসিমুখই আমাদের সেরা প্রাপ্তি! ❤️ গতকাল আমাদের একজন প্রিয় অতিথি বললেন—'এমন স্বাদ কলকাতায় সত্যিই বিরল।' ধন্যবাদ আপনাদের ভালোবাসার জন্য!",
            "উইকেন্ড চলে এল! 🎉 অফিসের কাজের চাপ ভুলে শুক্রবার সন্ধ্যায় চলে আসুন ${business.city}-র সেরা ঠিকানায়। সঙ্গে থাকছে $offer!",
            "শনিবার মানেই জমজমাট আড্ডা আর অফুরন্ত খুশি! 💫 আসন সীমিত, তাই দেরি না করে এখনই বুক করে নিন আপনার টেবিল।",
            "রবিবারের ছুটির দিন পরিবারের সাথে কাটাতে ভুলবেন না। 🌿 স্পেশাল ডেজার্ট আর বিশেষ উপহার অপেক্ষা করছে আপনাদের জন্য।"
        )

        val visualIdeas = listOf(
            "ধোঁয়া ওঠা খাবারের ক্লোজ-আপ ভিডিও (5-sec Reel)",
            "শেফ বা কর্মীদের কাজ করার মিষ্টি মুহূর্তের ছবি",
            "সুন্দর টেবিলে সাজানো থালি বা প্রোডাক্টের ফ্ল্যাট-লে ফটো",
            "সন্তুষ্ট গ্রাহকের হাসিভরা রিভিউ কার্ড",
            "উইকেন্ড ভিড়ের প্রাণবন্ত পরিবেশের ভিডিও",
            "বেস্ট-সেলার আইটেমের স্লো-মোশন শট",
            "পরিবারের সাথে আড্ডার সুন্দর ফ্রেম"
        )

        val ctas = listOf(
            "হোয়াটসঅ্যাপে বুকিং করুন: আজই যোগাযোগ করুন!",
            "আমাদের স্টোরে চলে আসুন: গুগল ম্যাপে ${business.city} লোকেশন দেখুন",
            "বিশেষ কোড ব্যবহার করে ১৫% ছাড় পান!",
            "আপনার অভিজ্ঞতা কমেন্টে জানান আমাদের!",
            "দেরি করবেন না—আজই আসুন!",
            "এখনই আপনার অর্ডার কনফার্ম করুন",
            "সপরিবারে চলে আসুন রবিবার দুপুরে!"
        )

        return days.mapIndexed { idx, day ->
            MarketingPost(
                id = UUID.randomUUID().toString(),
                businessId = business.id,
                dayOfWeek = day,
                theme = themes.getOrElse(idx) { "বিশেষ আকর্ষণ" },
                caption = captions.getOrElse(idx) { "$offer নিয়ে এসেছি আপনাদের জন্য!" },
                hashtags = "#${business.city}Eats #${business.name.replace(" ", "")} #KolkataFood #BanglaFoodies #SpecialOffer",
                callToAction = ctas.getOrElse(idx) { "যোগাযোগ করুন" },
                visualIdea = visualIdeas.getOrElse(idx) { "সুন্দর ছবি পোস্ট করুন" },
                bestTime = listOf("11:30 AM", "1:00 PM", "4:30 PM", "7:00 PM", "6:30 PM", "8:00 PM", "12:30 PM")[idx % 7],
                language = "Bengali",
                isSaved = false
            )
        }
    }

    private fun generateHindiWeeklyCalendar(business: Business, offer: String, days: List<String>): List<MarketingPost> {
        val themes = listOf(
            "सोमवार मोटिवेशन और नई शुरुआत",
            "प्रोडक्ट स्पॉटलाइट और स्पेशल क्वालिटी",
            "बुधवार स्पेशल ऑफर और कॉम्बो",
            "कस्टमर रिव्यू और भरोसे की कहानी",
            "शुक्रवार वीकेंड वाइब्स और मस्ती",
            "शनिवार बेस्टसेलर रश",
            "रविवार फैमिली स्पेशल टाइम"
        )

        val captions = listOf(
            "नई शुरुआत, बेहतरीन स्वाद और शानदार माहौल! 🌟 ${business.name} में आपका स्वागत है। इस हफ्ते की खास पेशकश: $offer। आइए और लुत्फ उठाइए!",
            "क्वालिटी ही हमारी पहचान है! 🌿 शुद्धता और स्वाद का ऐसा संगम जो आपको बार-बार खींचे। आज ही ट्राई करें हमारा $offer।",
            "हफ्ते के बीच में मूड फ्रेश करने का सही मौका! ☕ आपके लिए स्पेशल मिड-वीक डिस्काउंट। दोस्तों को टैग करें और साथ आएं!",
            "जब हमारे ग्राहक खुश होते हैं, तो हमारा दिन बन जाता है! ❤️ आपका प्यार और विश्वास ही हमारी सबसे बड़ी ताकत है।",
            "वीकेंड की शाम को बनाएं और भी यादगार! 🎉 काम को कहें अलविदा और ${business.city} में हमारी आउटलेट पर पधारें।",
            "शनिवार की रौनक और आपके पसंदीदा प्रोडक्ट्स! 🔥 भारी मांग के चलते तुरंत अपनी बुकिंग सुनिश्चित करें।",
            "संडे मतलब फैमिली टाइम और भरपूर खुशियां! ✨ आज अपने अपनों के साथ आएं और खास पलों का आनंद लें।"
        )

        val ctas = listOf(
            "WhatsApp पर तुरंत ऑर्डर करें!",
            "स्टोर विजिट करें या कॉल करें: ${business.city}",
            "सीमित समय के लिए उपलब्ध, अभी बुक करें!",
            "अपना अनुभव कमेंट्स में साझा करें!",
            "आज ही आएं और वीकेंड एन्जॉय करें!",
            "जल्दी करें—ऑफर केवल आज के लिए!",
            "परिवार के साथ आएं और स्पेशल गिफ्ट पाएं!"
        )

        return days.mapIndexed { idx, day ->
            MarketingPost(
                id = UUID.randomUUID().toString(),
                businessId = business.id,
                dayOfWeek = day,
                theme = themes.getOrElse(idx) { "खास पेशकश" },
                caption = captions.getOrElse(idx) { "$offer आपके लिए!" },
                hashtags = "#${business.city}Special #${business.name.replace(" ", "")} #LocalBusinessIndia #DesiVibes #OfferAlert",
                callToAction = ctas.getOrElse(idx) { "संपर्क करें" },
                visualIdea = "Reel or carousel showing the vibrant atmosphere and product details",
                bestTime = listOf("11:00 AM", "1:30 PM", "5:00 PM", "6:30 PM", "7:30 PM", "8:00 PM", "1:00 PM")[idx % 7],
                language = "Hindi",
                isSaved = false
            )
        }
    }

    private fun generateEnglishWeeklyCalendar(business: Business, offer: String, days: List<String>): List<MarketingPost> {
        val themes = listOf(
            "Monday Kickoff & Behind the Scenes",
            "Tuesday Spotlight & Craftsmanship",
            "Wednesday Midweek Craving Saver",
            "Thursday Customer Love & Testimonial",
            "Friday Weekend Anticipation",
            "Saturday Best-Seller Rush",
            "Sunday Community & Reset"
        )

        val captions = listOf(
            "Power up your Monday! ✨ At ${business.name}, we are starting the week strong with: $offer. Crafted fresh right here in ${business.city} for you.",
            "Ever wondered what goes into making your favourites? 🔍 Premium ingredients, careful craftsmanship, and pure passion. Experience $offer today!",
            "Beat the midweek slump! ⚡ Treat yourself to our exclusive midweek special. Bring a colleague or friend and double the joy.",
            "Heartwarming moments! ❤️ 'Best experience in ${business.city} hands down' — thank you to our regulars for making this journey so special.",
            "Friday feeling is officially here! 🎉 Wrap up your workday and drop by ${business.name}. Weekend reservations filling fast for $offer!",
            "Saturday buzz is real! 🌟 Our most sought-after signature offerings are ready for you. Come through and make memories.",
            "Slow down, recharge, and savor Sunday. 🌿 Family meals, cozy corners, and warm smiles await at our ${business.city} space."
        )

        val visualIdeas = listOf(
            "B-roll video of team preparing the setup (morning light)",
            "Macro photography showcasing texture and quality",
            "Quick 7-second Reel comparing 'Before vs After'",
            "Repost of a tagged customer story with heartfelt text overlay",
            "Slow-motion pour or sizzle clip with trending audio",
            "Vibrant photo carousel of the crowd and top 3 items",
            "Warm aesthetically pleasing wide shot of the store/cafe"
        )

        val ctas = listOf(
            "Tap the link in bio to reserve via WhatsApp!",
            "Drop by our store in ${business.city} today!",
            "Comment 'OFFER' and we'll DM you a 15% discount voucher!",
            "Share your experience with us using #${business.name.replace(" ", "")}!",
            "Book your weekend spot now before slots run out!",
            "Walk-ins welcome, or call ahead to skip the queue!",
            "Treat your loved ones today — directions in bio!"
        )

        return days.mapIndexed { idx, day ->
            MarketingPost(
                id = UUID.randomUUID().toString(),
                businessId = business.id,
                dayOfWeek = day,
                theme = themes.getOrElse(idx) { "Feature Post" },
                caption = captions.getOrElse(idx) { "$offer is now live at ${business.name}!" },
                hashtags = "#${business.city} #${business.name.replace(" ", "")} #LocalBusiness #SupportLocal #WeeklySpecials",
                callToAction = ctas.getOrElse(idx) { "Contact us today" },
                visualIdea = visualIdeas.getOrElse(idx) { "Clean lifestyle photo" },
                bestTime = listOf("10:30 AM", "1:00 PM", "4:00 PM", "6:30 PM", "7:00 PM", "8:30 PM", "12:00 PM")[idx % 7],
                language = "English",
                isSaved = false
            )
        }
    }

    private fun generateFallbackSinglePost(
        business: Business,
        campaignType: String,
        offerTopic: String,
        language: String
    ): MarketingPost {
        return when (language.lowercase()) {
            "bengali", "বাংলা" -> MarketingPost(
                id = UUID.randomUUID().toString(),
                businessId = business.id,
                dayOfWeek = "উৎসব স্পেশাল",
                theme = "সীমিত সময়ের বিশেষ অফার",
                caption = "🎉 ${business.name}-এ এসে গেল দারুণ সুযোগ!\\n\\n${business.city}-র সকল প্রিয়জনদের জন্য আমাদের বিশেষ উপহার: $offerTopic।\\n\\nঐতিহ্য, স্বাদ এবং আন্তরিকতার সেরা মেলবন্ধন। আজই চলে আসুন আমাদের ঠিকানায় অথবা ঘরে বসে অর্ডার করুন!",
                hashtags = "#${business.city} #BanglaFood #${business.name.replace(" ", "")} #KolkataOffer #FestiveVibes",
                callToAction = "এখনই হোয়াটসঅ্যাপে অর্ডার করুন বা কল করুন!",
                visualIdea = "উৎসবের আলোয় সাজানো স্পেশাল আইটেমের আকর্ষণীয় রিল",
                bestTime = "সন্ধ্যা ৭:০০ টা",
                language = "Bengali",
                isSaved = false
            )
            "hindi", "हिंदी" -> MarketingPost(
                id = UUID.randomUUID().toString(),
                businessId = business.id,
                dayOfWeek = "धमाका ऑफर",
                theme = "फेस्टिवल स्पेशल डील",
                caption = "✨ खुशियों की नई शुरुआत ${business.name} के साथ!\\n\\n${business.city} वासियों के लिए खास पेशकश: $offerTopic।\\n\\nऐसा मौका बार-बार नहीं आता। अपने परिवार और दोस्तों के साथ आएं और इस शानदार ऑफर का भरपूर फायदा उठाएं!",
                hashtags = "#${business.city}Diaries #${business.name.replace(" ", "")} #OfferAlert #DesiFood #FestiveSeason",
                callToAction = "आज ही विजिट करें या WhatsApp पर संपर्क करें!",
                visualIdea = "उत्सव की जगमगाहट में सजी थाली या प्रोडक्ट की शानदार फोटो",
                bestTime = "शाम ६:३० बजे",
                language = "Hindi",
                isSaved = false
            )
            else -> MarketingPost(
                id = UUID.randomUUID().toString(),
                businessId = business.id,
                dayOfWeek = "Exclusive Promo",
                theme = "Limited Time Campaign",
                caption = "Big news from ${business.name}! 🚀\\n\\nWe are thrilled to bring you our latest highlight in ${business.city}: $offerTopic.\\n\\nHandcrafted with the finest quality and made to delight your day. Don't wait — valid this week only!",
                hashtags = "#${business.city} #LocalBusiness #${business.name.replace(" ", "")} #ExclusiveOffer #TrendingNow",
                callToAction = "DM us or tap WhatsApp to claim your special offer today!",
                visualIdea = "High-energy 6-second video showing quick cutaways of the offer",
                bestTime = "6:30 PM",
                language = "English",
                isSaved = false
            )
        }
    }
}
