package com.cikup.amazgone.progress

import app.cash.turbine.test
import com.cikup.amazgone.progress.domain.model.AchievementEngine
import com.cikup.amazgone.progress.domain.model.AchievementId
import com.cikup.amazgone.progress.domain.model.PlayerStats
import com.cikup.amazgone.progress.domain.repository.AchievementRepository
import com.cikup.amazgone.progress.domain.usecase.TrackAchievementsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakeAchievements : AchievementRepository {
    val stats = MutableStateFlow(PlayerStats())
    val unlocked = MutableStateFlow<Map<AchievementId, Long>>(emptyMap())
    override fun observeStats() = stats
    override fun observeUnlocked() = unlocked
    override suspend fun unlock(ids: Set<AchievementId>) { unlocked.value = unlocked.value + ids.associateWith { 1L } }
}

class AchievementsTest {
    @Test
    fun rulesMatchThresholds() {
        assertEquals(emptySet(), AchievementEngine.earned(PlayerStats()))
        val earned = AchievementEngine.earned(PlayerStats(orders = 10, coinsSpent = 12_000, spins = 1, level = 5))
        assertEquals(setOf(AchievementId.FIRST_ORDER, AchievementId.SHOPAHOLIC, AchievementId.BIG_SPENDER, AchievementId.LUCKY_SPIN, AchievementId.RISING_STAR), earned)
    }

    @Test
    fun boardCapsProgressAndKeepsUnlockTimes() {
        val board = AchievementEngine.board(PlayerStats(orders = 50), mapOf(AchievementId.FIRST_ORDER to 99L))
        val shopaholic = board.first { it.id == AchievementId.SHOPAHOLIC }
        assertEquals(10, shopaholic.progress)
        assertEquals(1f, shopaholic.fraction)
        assertEquals(99L, board.first { it.id == AchievementId.FIRST_ORDER }.unlockedAt)
    }

    @Test
    fun trackerEmitsOnlyNewUnlocksOnce() = runTest {
        val repo = FakeAchievements()
        TrackAchievementsUseCase(repo)().test {
            repo.stats.value = PlayerStats(orders = 1)
            assertEquals(setOf(AchievementId.FIRST_ORDER), awaitItem())
            repo.stats.value = PlayerStats(orders = 2)
            expectNoEvents()
            repo.stats.value = PlayerStats(orders = 2, spins = 1)
            assertEquals(setOf(AchievementId.LUCKY_SPIN), awaitItem())
            assertTrue(AchievementId.FIRST_ORDER in repo.unlocked.value)
        }
    }
}
