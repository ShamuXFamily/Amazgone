package com.cikup.amazgone.notifications.presentation

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.notifications_earlier
import amazgone.shared.generated.resources.notifications_empty_body
import amazgone.shared.generated.resources.notifications_empty_title
import amazgone.shared.generated.resources.notifications_enable_action
import amazgone.shared.generated.resources.notifications_enable_body
import amazgone.shared.generated.resources.notifications_enable_title
import amazgone.shared.generated.resources.notifications_mark_all
import amazgone.shared.generated.resources.notifications_title
import amazgone.shared.generated.resources.notifications_today
import amazgone.shared.generated.resources.notifications_unread
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cikup.amazgone.core.designsystem.component.BackTopBar
import com.cikup.amazgone.core.designsystem.component.MessageState
import com.cikup.amazgone.core.designsystem.motion.staggeredEnter
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import com.cikup.amazgone.core.notifications.NotificationPermission
import com.cikup.amazgone.core.notifications.rememberNotificationPermission
import com.cikup.amazgone.notifications.domain.model.AppNotification
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NotificationsRoute(onBack: () -> Unit, onOpenLink: (String) -> Unit, viewModel: NotificationsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                NotificationsEffect.NavigateBack -> onBack()
                is NotificationsEffect.OpenLink -> onOpenLink(effect.link)
            }
        }
    }
    NotificationsScreen(state, viewModel::onIntent, rememberNotificationPermission())
}

@Composable
fun NotificationsScreen(state: NotificationsState, onIntent: (NotificationsIntent) -> Unit, permission: NotificationPermission) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BackTopBar(stringResource(Res.string.notifications_title), { onIntent(NotificationsIntent.Back) }) {
                AnimatedVisibility(state.unreadCount > 0, enter = fadeIn(), exit = fadeOut()) {
                    TextButton(onClick = { onIntent(NotificationsIntent.MarkAllRead) }) { Text(stringResource(Res.string.notifications_mark_all)) }
                }
            }
        },
    ) { padding ->
        val (today, earlier) = state.grouped()
        LazyColumn(
            contentPadding = PaddingValues(
                start = AmazgoneDimens.spaceLg,
                end = AmazgoneDimens.spaceLg,
                top = padding.calculateTopPadding() + AmazgoneDimens.spaceSm,
                bottom = AmazgoneDimens.iconXl + AmazgoneDimens.spaceXl,
            ),
            verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (!permission.granted) item(key = "enable") { EnableCard(permission, Modifier.animateItem()) }
            if (state.isEmpty) {
                item(key = "empty") {
                    MessageState(
                        Icons.Outlined.NotificationsNone,
                        stringResource(Res.string.notifications_empty_title),
                        stringResource(Res.string.notifications_empty_body),
                        Modifier.fillMaxWidth().padding(top = AmazgoneDimens.spaceXxl),
                    )
                }
            }
            section("today", Res.string.notifications_today, today, state, onIntent, unreadBadge = true)
            section("earlier", Res.string.notifications_earlier, earlier, state, onIntent, unreadBadge = today.isEmpty())
        }
    }
}

private fun LazyListScope.section(
    key: String,
    title: StringResource,
    items: List<AppNotification>,
    state: NotificationsState,
    onIntent: (NotificationsIntent) -> Unit,
    unreadBadge: Boolean,
) {
    if (items.isEmpty()) return
    item(key = "$key-title") { SectionTitle(title, if (unreadBadge) state.unreadCount else 0, Modifier.animateItem()) }
    itemsIndexed(items, key = { _, it -> it.id }) { index, item ->
        NotificationRow(item, state.now, onIntent, Modifier.animateItem().staggeredEnter(index))
    }
}

@Composable
private fun SectionTitle(title: StringResource, unread: Int, modifier: Modifier) {
    Row(modifier.padding(top = AmazgoneDimens.spaceMd), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (unread > 0) {
            Surface(color = AmazgoneTheme.extended.cta, contentColor = AmazgoneTheme.extended.onCta, shape = CircleShape) {
                Text(
                    stringResource(Res.string.notifications_unread, unread),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = AmazgoneDimens.spaceSm, vertical = AmazgoneDimens.spaceXs / 2),
                )
            }
        }
    }
}

/** Shown while the OS won't let us post: explains the value, then asks. */
@Composable
private fun EnableCard(permission: NotificationPermission, modifier: Modifier) {
    val ext = AmazgoneTheme.extended
    Surface(color = ext.brandNavy, contentColor = ext.onBrandNavy, shape = MaterialTheme.shapes.extraLarge, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(AmazgoneDimens.spaceLg), horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = ext.cta, contentColor = ext.onCta, shape = CircleShape) {
                Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.padding(AmazgoneDimens.spaceSm).size(AmazgoneDimens.iconMd))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs)) {
                Text(stringResource(Res.string.notifications_enable_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(stringResource(Res.string.notifications_enable_body), style = MaterialTheme.typography.bodySmall, color = ext.onBrandNavyVariant)
                Button(onClick = permission::request, colors = ButtonDefaults.buttonColors(containerColor = ext.cta, contentColor = ext.onCta)) {
                    Text(stringResource(Res.string.notifications_enable_action))
                }
            }
        }
    }
}
