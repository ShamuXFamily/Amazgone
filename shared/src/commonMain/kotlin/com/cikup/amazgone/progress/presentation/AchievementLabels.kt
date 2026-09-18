package com.cikup.amazgone.progress.presentation

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.ach_BIG_SPENDER
import amazgone.shared.generated.resources.ach_BIG_SPENDER_desc
import amazgone.shared.generated.resources.ach_COLLECTOR
import amazgone.shared.generated.resources.ach_COLLECTOR_desc
import amazgone.shared.generated.resources.ach_FIRST_ORDER
import amazgone.shared.generated.resources.ach_FIRST_ORDER_desc
import amazgone.shared.generated.resources.ach_LEGEND
import amazgone.shared.generated.resources.ach_LEGEND_desc
import amazgone.shared.generated.resources.ach_LUCKY_SPIN
import amazgone.shared.generated.resources.ach_LUCKY_SPIN_desc
import amazgone.shared.generated.resources.ach_RISING_STAR
import amazgone.shared.generated.resources.ach_RISING_STAR_desc
import amazgone.shared.generated.resources.ach_SCRATCHER
import amazgone.shared.generated.resources.ach_SCRATCHER_desc
import amazgone.shared.generated.resources.ach_SHOPAHOLIC
import amazgone.shared.generated.resources.ach_SHOPAHOLIC_desc
import amazgone.shared.generated.resources.ach_WHEEL_REGULAR
import amazgone.shared.generated.resources.ach_WHEEL_REGULAR_desc
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.LocalMall
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Style
import androidx.compose.ui.graphics.vector.ImageVector
import com.cikup.amazgone.progress.domain.model.AchievementId
import org.jetbrains.compose.resources.StringResource

data class AchievementLabel(val title: StringResource, val description: StringResource, val icon: ImageVector)

fun AchievementId.label(): AchievementLabel = when (this) {
    AchievementId.FIRST_ORDER -> AchievementLabel(Res.string.ach_FIRST_ORDER, Res.string.ach_FIRST_ORDER_desc, Icons.Outlined.ShoppingBag)
    AchievementId.SHOPAHOLIC -> AchievementLabel(Res.string.ach_SHOPAHOLIC, Res.string.ach_SHOPAHOLIC_desc, Icons.Outlined.LocalMall)
    AchievementId.BIG_SPENDER -> AchievementLabel(Res.string.ach_BIG_SPENDER, Res.string.ach_BIG_SPENDER_desc, Icons.Outlined.Paid)
    AchievementId.LUCKY_SPIN -> AchievementLabel(Res.string.ach_LUCKY_SPIN, Res.string.ach_LUCKY_SPIN_desc, Icons.Outlined.Casino)
    AchievementId.WHEEL_REGULAR -> AchievementLabel(Res.string.ach_WHEEL_REGULAR, Res.string.ach_WHEEL_REGULAR_desc, Icons.Outlined.AutoAwesome)
    AchievementId.SCRATCHER -> AchievementLabel(Res.string.ach_SCRATCHER, Res.string.ach_SCRATCHER_desc, Icons.Outlined.Style)
    AchievementId.RISING_STAR -> AchievementLabel(Res.string.ach_RISING_STAR, Res.string.ach_RISING_STAR_desc, Icons.Outlined.Star)
    AchievementId.LEGEND -> AchievementLabel(Res.string.ach_LEGEND, Res.string.ach_LEGEND_desc, Icons.Outlined.MilitaryTech)
    AchievementId.COLLECTOR -> AchievementLabel(Res.string.ach_COLLECTOR, Res.string.ach_COLLECTOR_desc, Icons.Outlined.Favorite)
}
