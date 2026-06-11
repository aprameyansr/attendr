package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

object SubjectIconHelper {
    val iconList = listOf(
        "book" to Icons.Filled.Book,
        "bolt" to Icons.Filled.Bolt,
        "science" to Icons.Filled.Science,
        "calculate" to Icons.Filled.Calculate,
        "computer" to Icons.Filled.Computer,
        "history" to Icons.Filled.History,
        "translate" to Icons.Filled.Translate,
        "palette" to Icons.Filled.Palette,
        "sports_basketball" to Icons.Filled.SportsBasketball,
        "music_note" to Icons.Filled.MusicNote,
        "engineering" to Icons.Filled.Engineering,
        "star" to Icons.Filled.Star,
        "school" to Icons.Filled.School,
        "biotech" to Icons.Filled.Biotech,
        "construction" to Icons.Filled.Construction,
        "gavel" to Icons.Filled.Gavel,
        "business" to Icons.Filled.Business,
        "psychology" to Icons.Filled.Psychology,
        "public" to Icons.Filled.Public,
        "menu_book" to Icons.Filled.MenuBook,
        "functions" to Icons.Filled.Functions,
        "terminal" to Icons.Filled.Terminal,
        "architecture" to Icons.Filled.Architecture,
        "account_balance" to Icons.Filled.AccountBalance,
        "language" to Icons.Filled.Language,
        "forest" to Icons.Filled.Forest,
        "camera_alt" to Icons.Filled.CameraAlt,
        "auto_stories" to Icons.Filled.AutoStories,
        "lightbulb" to Icons.Filled.Lightbulb,
        "group" to Icons.Filled.Group,
        "bar_chart" to Icons.Filled.BarChart,
        "layers" to Icons.Filled.Layers,
        "timer" to Icons.Filled.Timer,
        "timeline" to Icons.Filled.Timeline,
        "agriculture" to Icons.Filled.Agriculture
    )

    fun getIcon(name: String): ImageVector {
        return iconList.find { it.first == name }?.second ?: Icons.Filled.Book
    }

    fun suggestIcon(subjectName: String): String {
        val name = subjectName.lowercase()
        return when {
            name.contains("math") || name.contains("calc") || name.contains("algebra") || name.contains("function") -> "calculate"
            name.contains("phys") || name.contains("bolt") || name.contains("electr") -> "bolt"
            name.contains("chem") || name.contains("bio") || name.contains("lab") || name.contains("scie") -> "science"
            name.contains("comp") || name.contains("code") || name.contains("prog") || name.contains("soft") || name.contains("dev") -> "computer"
            name.contains("terminal") || name.contains("data struct") || name.contains("algo") -> "terminal"
            name.contains("hist") || name.contains("social") || name.contains("civic") -> "history"
            name.contains("lang") || name.contains("engl") || name.contains("span") || name.contains("write") || name.contains("read") || name.contains("lit") -> "translate"
            name.contains("art") || name.contains("draw") || name.contains("paint") || name.contains("design") -> "palette"
            name.contains("sport") || name.contains("gym") || name.contains("pe") || name.contains("physic") && name.contains("ed") -> "sports_basketball"
            name.contains("music") || name.contains("song") || name.contains("band") -> "music_note"
            name.contains("eng") || name.contains("mech") -> "engineering"
            name.contains("star") || name.contains("fav") -> "star"
            name.contains("acad") || name.contains("coll") || name.contains("univ") -> "school"
            name.contains("law") || name.contains("gavel") || name.contains("constit") -> "gavel"
            name.contains("bus") || name.contains("econ") || name.contains("fin") -> "business"
            name.contains("psych") || name.contains("mind") || name.contains("cog") -> "psychology"
            name.contains("geo") || name.contains("world") || name.contains("map") -> "public"
            name.contains("stat") || name.contains("anal") || name.contains("graph") -> "bar_chart"
            name.contains("forest") || name.contains("tree") || name.contains("eco") -> "forest"
            name.contains("photo") || name.contains("cam") -> "camera_alt"
            else -> "book"
        }
    }
}
