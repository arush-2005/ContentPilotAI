package com.example.data

import com.example.model.IndustryTemplate

object PreloadedTemplates {
    val templates = listOf(
        IndustryTemplate(
            id = "tpl_rest_1",
            title = "Authentic Lunch Thali Launch",
            category = "Restaurant & Cafe",
            sampleBusinessName = "Sonar Bangla Diner",
            city = "Kolkata",
            targetAudience = "Office workers, students, food lovers",
            brandTone = "Warm & Inviting",
            language = "Bengali",
            offerPrompt = "New traditional Bengali lunch thali with rice, shukto, dal, bhetki paturi, and rosogolla at ₹249",
            iconEmoji = "🍛"
        ),
        IndustryTemplate(
            id = "tpl_rest_2",
            title = "Weekend Brunch & Live Music",
            category = "Restaurant & Cafe",
            sampleBusinessName = "Urban Bean Cafe",
            city = "Bengaluru",
            targetAudience = "Young professionals and coffee aficionados",
            brandTone = "Trendy & Casual",
            language = "English",
            offerPrompt = "Sunday rooftop acoustic brunch with artisanal drip coffee and avocado sourdough toast",
            iconEmoji = "☕"
        ),
        IndustryTemplate(
            id = "tpl_fest_1",
            title = "Durga Puja Festive Collection",
            category = "Clothing & Fashion",
            sampleBusinessName = "Tantushree Handlooms",
            city = "Kolkata",
            targetAudience = "Saree lovers, festive shoppers, families",
            brandTone = "Celebratory & Elegant",
            language = "Bengali",
            offerPrompt = "Puja special pure Dhakai Jamdani and Tussar silk sarees with flat 15% festival discount",
            iconEmoji = "🥻"
        ),
        IndustryTemplate(
            id = "tpl_fest_2",
            title = "Diwali Glow & Traditional Attire",
            category = "Clothing & Fashion",
            sampleBusinessName = "Rangoli Ethnic Wear",
            city = "Delhi",
            targetAudience = "Wedding and festival shoppers",
            brandTone = "Festive & Vibrant",
            language = "Hindi",
            offerPrompt = "दिवाली स्पेशल कुर्ता पायजामा और अनारकली सूट्स पर फ्लैट 25% की छूट, नए कलेक्शंस अब स्टोर में उपलब्ध",
            iconEmoji = "✨"
        ),
        IndustryTemplate(
            id = "tpl_salon_1",
            title = "Bridal & Festive Glow Makeover",
            category = "Salon & Spa",
            sampleBusinessName = "Aura Studio & Salon",
            city = "Mumbai",
            targetAudience = "Brides, bridesmaids, partygoers",
            brandTone = "Luxurious & Pampering",
            language = "English",
            offerPrompt = "Pre-bridal glow ritual with gold facial, hair botox, and gel nail art package at ₹3,999",
            iconEmoji = "💇‍♀️"
        ),
        IndustryTemplate(
            id = "tpl_fit_1",
            title = "Post-Festive 30-Day Body Reset",
            category = "Fitness & Wellness",
            sampleBusinessName = "IronCore Fitness",
            city = "Pune",
            targetAudience = "Working professionals wanting weight loss",
            brandTone = "Energetic & Motivating",
            language = "Hindi",
            offerPrompt = "30-दिन का बॉडी रीसेट चैलेंज: पर्सनल ट्रेनर + न्यूट्रिशन प्लान, पहले 20 मेंबर्स को ₹500 का स्पेशल डिस्काउंट",
            iconEmoji = "💪"
        ),
        IndustryTemplate(
            id = "tpl_tutor_1",
            title = "Board Exam Crash Course Enrollment",
            category = "Tutor & Coaching",
            sampleBusinessName = "Apex Scholars Institute",
            city = "Kolkata",
            targetAudience = "Class 10 and 12 students & parents",
            brandTone = "Trustworthy & Encouraging",
            language = "Bengali",
            offerPrompt = "মাধ্যমিক ও উচ্চ মাধ্যমিক পরীক্ষার জন্য স্পেশাল ক্র্যাশ কোর্স ও টেস্ট সিরিজ, প্রথম ফ্রি ডেমো ক্লাস এই শনিবারে",
            iconEmoji = "📚"
        ),
        IndustryTemplate(
            id = "tpl_agency_1",
            title = "Local Business Social Starter Pack",
            category = "Freelancer / Agency",
            sampleBusinessName = "PixelKraft Studio",
            city = "Bengaluru",
            targetAudience = "Local shop owners, doctors, cafes",
            brandTone = "Professional & Result-Oriented",
            language = "English",
            offerPrompt = "Full monthly social media package: 12 designed posts, reels scripting, and Google Business profile optimization",
            iconEmoji = "🚀"
        )
    )

    val festivalSuggestions = listOf(
        FestivalPreset("Durga Puja (শারদীয়া)", "Bengali", "Mahalaya to Dashami special offers, festive menus, new clothes celebration", "🪔"),
        FestivalPreset("Diwali & Dhanteras (दीपावली)", "Hindi", "Prosperity gifts, sweet boxes, festive apparel, glowing offers", "🪔"),
        FestivalPreset("Poila Baisakh (পয়লা বৈশাখ)", "Bengali", "Bengali New Year special discounts, traditional food, Haalkhata celebration", "🎋"),
        FestivalPreset("Eid-ul-Fitr (ঈদ মুবারক)", "Hindi", "Festive feast, bespoke attire, gift hampers, celebratory wishes", "🌙"),
        FestivalPreset("Holi / Dolyatra (হোলি উৎসব)", "Hindi", "Color festival combos, skin-friendly organic events, party menus", "🎨"),
        FestivalPreset("Christmas & New Year", "English", "Year-end party bookings, seasonal winter specials, gift hampers", "🎄")
    )
}

data class FestivalPreset(
    val name: String,
    val defaultLanguage: String,
    val description: String,
    val emoji: String
)
