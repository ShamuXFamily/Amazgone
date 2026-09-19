package com.cikup.amazgone.orders.presentation

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.order_stage_confirmed
import amazgone.shared.generated.resources.order_stage_placed
import amazgone.shared.generated.resources.order_stage_shipped
import amazgone.shared.generated.resources.order_step_delivered
import amazgone.shared.generated.resources.order_status_REJECTED
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import com.cikup.amazgone.orders.domain.model.OrderStage
import org.jetbrains.compose.resources.stringResource

/** Pill showing where an order is; its colour animates as the order moves along. */
@Composable
fun OrderStageChip(stage: OrderStage, modifier: Modifier = Modifier) {
    val ext = AmazgoneTheme.extended
    val tint by animateColorAsState(
        when (stage) {
            OrderStage.PLACED -> MaterialTheme.colorScheme.onSurfaceVariant
            OrderStage.CONFIRMED, OrderStage.SHIPPED -> ext.cta
            OrderStage.DELIVERED -> ext.success
            OrderStage.REJECTED -> MaterialTheme.colorScheme.error
        },
        label = "stageColor",
    )
    Surface(color = tint.copy(alpha = CHIP_ALPHA), contentColor = tint, shape = MaterialTheme.shapes.small, modifier = modifier) {
        Row(
            Modifier.padding(horizontal = AmazgoneDimens.spaceSm, vertical = AmazgoneDimens.spaceXs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs),
        ) {
            Icon(
                when (stage) {
                    OrderStage.PLACED -> Icons.Outlined.CloudSync
                    OrderStage.CONFIRMED -> Icons.Outlined.Inventory2
                    OrderStage.SHIPPED -> Icons.Outlined.LocalShipping
                    OrderStage.DELIVERED -> Icons.Outlined.CheckCircle
                    OrderStage.REJECTED -> Icons.Outlined.Undo
                },
                contentDescription = null,
                modifier = Modifier.size(AmazgoneDimens.iconSm),
            )
            Text(
                stringResource(
                    when (stage) {
                        OrderStage.PLACED -> Res.string.order_stage_placed
                        OrderStage.CONFIRMED -> Res.string.order_stage_confirmed
                        OrderStage.SHIPPED -> Res.string.order_stage_shipped
                        OrderStage.DELIVERED -> Res.string.order_step_delivered
                        OrderStage.REJECTED -> Res.string.order_status_REJECTED
                    },
                ),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private const val CHIP_ALPHA = 0.14f
