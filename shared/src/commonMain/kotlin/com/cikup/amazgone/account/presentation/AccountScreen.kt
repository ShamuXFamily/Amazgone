package com.cikup.amazgone.account.presentation

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.account_cancel
import amazgone.shared.generated.resources.account_sign_out_anyway
import amazgone.shared.generated.resources.account_sign_out_pending_body
import amazgone.shared.generated.resources.account_sign_out_pending_title
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import com.cikup.amazgone.catalog.presentation.home.SearchPill
import com.cikup.amazgone.core.designsystem.component.BrandWordmark
import com.cikup.amazgone.core.designsystem.component.StatusBarScrim
import com.cikup.amazgone.core.designsystem.motion.staggeredEnter
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cikup.amazgone.core.designsystem.component.SyncStatusIcon
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AccountRoute(onNavigate: (AccountDestination) -> Unit, viewModel: AccountViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is AccountEffect.Navigate -> onNavigate(effect.destination)
                AccountEffect.SignedIn -> Unit
            }
        }
    }
    AccountScreen(state, viewModel::onIntent)
}

@Composable
fun AccountScreen(state: AccountState, onIntent: (AccountIntent) -> Unit) {
    Scaffold(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding()),
            contentPadding = PaddingValues(AmazgoneDimens.spaceLg),
            verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceLg),
        ) {
            item(key = "header") {
                Column(Modifier.statusBarsPadding(), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { BrandWordmark() }
                        SyncStatusIcon(state.syncStatus)
                    }
                    SearchPill(onClick = { onIntent(AccountIntent.Open(AccountDestination.SEARCH)) })
                }
            }
            if (state.session != null) signedInHeader(state)
            item(key = "tiles") { MenuTiles(state.coins, onIntent, Modifier.staggeredEnter(0)) }
            item(key = "roulette") { RouletteBanner(onIntent, Modifier.staggeredEnter(1)) }
            if (state.session == null) guestContent(state, onIntent)
            item(key = "menu") { MenuList(onIntent) }
            if (state.session != null) signedInFooter(state, onIntent)
        }
        StatusBarScrim(MaterialTheme.colorScheme.surfaceContainerLow)
    }
    state.pendingSignOutCount?.let { count ->
        AlertDialog(
            onDismissRequest = { onIntent(AccountIntent.DismissSignOut) },
            title = { Text(stringResource(Res.string.account_sign_out_pending_title)) },
            text = { Text(stringResource(Res.string.account_sign_out_pending_body, count)) },
            confirmButton = {
                TextButton(onClick = { onIntent(AccountIntent.ConfirmSignOut) }) { Text(stringResource(Res.string.account_sign_out_anyway)) }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(AccountIntent.DismissSignOut) }) { Text(stringResource(Res.string.account_cancel)) }
            },
        )
    }
}
