package com.cikup.amazgone.wallet.presentation

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.ledger_BASELINE
import amazgone.shared.generated.resources.ledger_DAILY_XP
import amazgone.shared.generated.resources.ledger_GAME_XP
import amazgone.shared.generated.resources.ledger_ORDER_XP
import amazgone.shared.generated.resources.ledger_PURCHASE
import amazgone.shared.generated.resources.ledger_SCRATCH_REWARD
import amazgone.shared.generated.resources.ledger_SPIN_REWARD
import amazgone.shared.generated.resources.ledger_STARTER
import androidx.compose.runtime.Composable
import com.cikup.amazgone.wallet.domain.model.LedgerReason
import org.jetbrains.compose.resources.stringResource

@Composable
fun ledgerLabel(reason: LedgerReason): String = stringResource(
    when (reason) {
        LedgerReason.STARTER -> Res.string.ledger_STARTER
        LedgerReason.BASELINE -> Res.string.ledger_BASELINE
        LedgerReason.PURCHASE -> Res.string.ledger_PURCHASE
        LedgerReason.SPIN_REWARD -> Res.string.ledger_SPIN_REWARD
        LedgerReason.SCRATCH_REWARD -> Res.string.ledger_SCRATCH_REWARD
        LedgerReason.ORDER_XP -> Res.string.ledger_ORDER_XP
        LedgerReason.GAME_XP -> Res.string.ledger_GAME_XP
        LedgerReason.DAILY_XP -> Res.string.ledger_DAILY_XP
    },
)
