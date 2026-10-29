package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FestivalPreset
import com.example.data.PreloadedTemplates
import com.example.model.IndustryTemplate
import com.example.ui.MainViewModel
import com.example.ui.components.MarketingPostCard
import com.example.ui.components.PlanUpgradeDialog
import com.example.ui.components.QuotaBanner
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AmberSecondary
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.FestiveRuby

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToGenerator: (offer: String?, lang: String?, type: String?) -> Unit,
    onNavigateToBusinesses: () -> Unit,
    onNavigateToCalendar: () -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val businesses by viewModel.businesses.collectAsState()
    val selectedBusiness by viewModel.selectedBusiness.collectAsState()
    val calendarPosts by viewModel.calendarPosts.collectAsState()
    val savedPosts by viewModel.savedPosts.collectAsState()

    var showBusinessMenu by remember { mutableStateOf(false) }
    var showPlanDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Bar: Active Business Selector & Avatar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Business Dropdown Trigger
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { showBusinessMenu = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = "Business",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = selectedBusiness?.name ?: "Select Business",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${selectedBusiness?.city ?: "India"} • ${selectedBusiness?.category ?: "Local"}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Dropdown",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showBusinessMenu,
                        onDismissRequest = { showBusinessMenu = false }
                    ) {
                        businesses.forEach { biz ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(biz.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${biz.city} • ${biz.language}", fontSize = 11.sp)
                                    }
                                },
                                leadingIcon = {
                                    if (biz.id == selectedBusiness?.id) {
                                        Icon(Icons.Default.Check, contentDescription = "Active", tint = AccentGreen)
                                    }
                                },
                                onClick = {
                                    viewModel.selectBusiness(biz)
                                    showBusinessMenu = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("+ Manage / Add Business", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) },
                            onClick = {
                                showBusinessMenu = false
                                onNavigateToBusinesses()
                            }
                        )
                    }
                }

                // Plan Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { showPlanDialog = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = (userProfile?.plan ?: "FREE").uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Quota Bar
        item {
            QuotaBanner(
                userProfile = userProfile,
                onUpgradeClick = { showPlanDialog = true }
            )
        }

        // Hero Weekly Pilot Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hero_calendar_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF0F2942),
                                    Color(0xFF133E59),
                                    Color(0xFF1B4E6B)
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(CyanPrimary.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = CyanPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "WEEKLY CONTENT PILOT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CyanPrimary,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Generate a full week's localized schedule",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "7 distinct angles for ${selectedBusiness?.name ?: "your business"}: behind the scenes, menu highlights, customer love, and weekend rush.",
                            fontSize = 12.sp,
                            color = Color(0xFFCFE4F0),
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                onNavigateToGenerator(
                                    null,
                                    selectedBusiness?.language ?: "English",
                                    "weekly_calendar"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("start_weekly_pilot_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF00363F)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Create 7-Day Plan",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00363F),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // Section: Festival & Seasonal Campaigns (USP for Indian small businesses!)
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Festivals & Regional Campaigns",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(PreloadedTemplates.festivalSuggestions) { fest ->
                        FestivalCard(
                            preset = fest,
                            onClick = {
                                onNavigateToGenerator(
                                    "${fest.name} festive special offer and customer appreciation",
                                    fest.defaultLanguage,
                                    "festival_special"
                                )
                            }
                        )
                    }
                }
            }
        }

        // Section: Industry Starter Templates
        item {
            Column {
                Text(
                    text = "Industry Ready Templates",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(PreloadedTemplates.templates) { tpl ->
                        TemplateCard(
                            template = tpl,
                            onClick = {
                                viewModel.applyTemplate(tpl)
                            }
                        )
                    }
                }
            }
        }

        // Section: Recent Content or Calendar Preview
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (calendarPosts.isNotEmpty()) "This Week's Plan Preview" else "Quick Start Guides",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (calendarPosts.isNotEmpty()) {
                    Text(
                        text = "View All (${calendarPosts.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onNavigateToCalendar() }
                    )
                }
            }
        }

        if (calendarPosts.isNotEmpty()) {
            items(calendarPosts.take(2)) { post ->
                MarketingPostCard(
                    post = post,
                    onToggleBookmark = { viewModel.toggleBookmarkPost(it) },
                    onUpdatePost = { viewModel.updatePost(it) }
                )
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "💡 Local Business Marketing Tip",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = AmberSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Kolkata, Delhi & Mumbai small businesses get 3x higher WhatsApp engagement by combining a local language hook (বাংলা বা हिंदी) with a clear Sunday or Weekend Call to Action.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }

    if (showPlanDialog) {
        PlanUpgradeDialog(
            currentPlan = userProfile?.plan ?: "free",
            onDismiss = { showPlanDialog = false },
            onSelectPlan = { newPlan ->
                viewModel.upgradePlan(newPlan)
                showPlanDialog = false
            }
        )
    }
}

@Composable
fun FestivalCard(
    preset: FestivalPreset,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(170.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(preset.emoji, fontSize = 22.sp)
                Box(
                    modifier = Modifier
                        .background(FestiveRuby.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(preset.defaultLanguage, fontSize = 10.sp, color = FestiveRuby, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = preset.name,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = preset.description,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 13.sp
            )
        }
    }
}

@Composable
fun TemplateCard(
    template: IndustryTemplate,
    onClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .width(180.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(template.iconEmoji, fontSize = 20.sp)
                Text(
                    text = template.language,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = template.title,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${template.sampleBusinessName} • ${template.city}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
