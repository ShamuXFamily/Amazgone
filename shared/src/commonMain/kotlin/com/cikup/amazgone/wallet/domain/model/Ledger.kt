package com.cikup.amazgone.wallet.domain.model

/** Coins are the only money; XP is tracked with the same append-only ledger mechanics. */
enum class Currency { COINS, XP }

enum class LedgerReason {
    STARTER, BASELINE, PURCHASE, SPIN_REWARD, SCRATCH_REWARD, ORDER_XP, GAME_XP, DAILY_XP,
}

/** PENDING = applied locally, awaiting the server; REJECTED entries no longer count. */
enum class LedgerStatus { PENDING, CONFIRMED, REJECTED }

data class LedgerEntry(
    val id: String,
    val currency: Currency,
    val amount: Long,
    val reason: LedgerReason,
    val refId: String?,
    val status: LedgerStatus,
    val createdAt: Long,
)

data class WalletSummary(
    val coins: Long,
    val pendingCoins: Long,
    val recent: List<LedgerEntry>,
)

object WalletRules {
    /** Coins every new player (guest or account) starts with. */
    const val STARTER_COINS = 5_000L
}
