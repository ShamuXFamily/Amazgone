package com.cikup.amazgone.orders.presentation

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.order_status_CONFIRMED
import amazgone.shared.generated.resources.order_status_PENDING_SYNC
import amazgone.shared.generated.resources.order_status_REJECTED
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.orders.domain.model.OrderStatus
import org.jetbrains.compose.resources.stringResource

/** Status pill whose color animates as the order moves from pending → confirmed/rejected. */
@Composable
fun OrderStatusChip(status: OrderStatus, modifier: Modifier = Modifier) {
    val container by animateColorAsState(
        when (status) {
            OrderStatus.PENDING_SYNC -> MaterialTheme.colorScheme.tertiaryContainer
            OrderStatus.CONFIRMED -> MaterialTheme.colorScheme.primaryContainer
            OrderStatus.REJECTED -> MaterialTheme.colorScheme.errorContainer
        },
        label = "statusColor",
    )
    Surface(color = container, shape = MaterialTheme.shapes.small, modifier = modifier) {
        Row(
            Modifier.padding(horizontal = AmazgoneDimens.spaceSm, vertical = AmazgoneDimens.spaceXs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs),
        ) {
            Icon(
                when (status) {
                    OrderStatus.PENDING_SYNC -> Icons.Outlined.CloudSync
                    OrderStatus.CONFIRMED -> Icons.Outlined.CheckCircle
                    OrderStatus.REJECTED -> Icons.Outlined.Undo
                },
                contentDescription = null,
                modifier = Modifier.size(AmazgoneDimens.iconSm),
            )
            Text(
                stringResource(
                    when (status) {
                        OrderStatus.PENDING_SYNC -> Res.string.order_status_PENDING_SYNC
                        OrderStatus.CONFIRMED -> Res.string.order_status_CONFIRMED
                        OrderStatus.REJECTED -> Res.string.order_status_REJECTED
                    },
                ),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}
