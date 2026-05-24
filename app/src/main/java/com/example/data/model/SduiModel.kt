package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SduiComponent(
    val type: String,               // "profile_header", "status_banner", "stats_grid", "info_list", "action_button", "divider", "section_title"
    val title: String? = null,
    val subtitle: String? = null,
    val value: String? = null,
    val description: String? = null,
    val iconName: String? = null,    // "User", "Heart", "Emergency", "Verified", "Blood", "Birthday", "Calendar", "Shield", "Allergy", "Contact", "Star", "History"
    val themeColor: String? = null,  // Semantic indicator e.g. "primary", "secondary", "error", "success", "warning"
    val actionKey: String? = null,   // "toggle_theme", "sync_cloud", "export_backup", "test_sdui_alert"
    val items: List<SduiItem>? = null
)

@JsonClass(generateAdapter = true)
data class SduiItem(
    val title: String,
    val value: String,
    val iconName: String? = null,
    val labelColor: String? = null   // Semantic color indicators
)

@JsonClass(generateAdapter = true)
data class SduiLayout(
    val title: String,
    val components: List<SduiComponent>
)
