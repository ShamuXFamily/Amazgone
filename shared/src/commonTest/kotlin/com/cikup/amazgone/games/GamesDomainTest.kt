package com.cikup.amazgone.games

import com.cikup.amazgone.core.domain.DomainError
import com.cikup.amazgone.core.domain.DomainResult
import com.cikup.amazgone.games.domain.model.Cooldowns
import com.cikup.amazgone.games.domain.model.GameKind
import com.cikup.amazgone.games.domain.model.GamePlay
import com.cikup.amazgone.games.domain.model.LightningDeal
import com.cikup.amazgone.games.domain.model.Reward
import com.cikup.amazgone.games.domain.model.RewardTables
import com.cikup.amazgone.games.domain.repository.GamesRepository
import com.cikup.amazgone.games.domain.usecase.PlayGameUseCase
import com.cikup.amazgone.progress.domain.model.XpRules
import com.cikup.amazgone.testing.FakeClock
import com.cikup.amazgone.testing.SequentialIds
import com.cikup.amazgone.testing.product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private class FakeGames : GamesRepository {
    val plays = MutableStateFlow<List<GamePlay>>(emptyList())
    override fun observeLastPlayedAt(kind: GameKind) = plays.map { all -> all.filter { it.kind == kind }.maxOfOrNull { it.playedAt } }
    override suspend fun lastPlayedAt(kind: GameKind) = plays.value.filter { it.kind == kind }.maxOfOrNull { it.playedAt }
    override suspend fun record(play: GamePlay) { plays.value = plays.value + play }
    override fun observeLatestPlay(kind: GameKind) = plays.map { all -> all.filter { it.kind == kind }.maxByOrNull { it.playedAt } }
}

class GamesDomainTest {
    @Test
    fun weightedPickMatchesTheOddsOverManyRolls() {
        val random = Random(42)
        val counts = IntArray(RewardTables.SPIN.size)
        repeat(20_000) { counts[RewardTables.pickIndex(RewardTables.SPIN, random)]++ }
        val total = RewardTables.SPIN.sumOf { it.weight }.toDouble()
        RewardTables.SPIN.forEachIndexed { i, reward ->
            val expected = reward.weight / total
            val actual = counts[i] / 20_000.0
            assertTrue(kotlin.math.abs(expected - actual) < 0.02, "segment $i: expected $expected got $actual")
        }
    }

    @Test
    fun spinEconomyStaysModest() {
        val total = RewardTables.SPIN.sumOf { it.weight }.toDouble()
        val expectedCoins = RewardTables.SPIN.filterIsInstance<Reward.Coins>().sumOf { it.amount * it.weight } / total
        assertTrue(expectedCoins in 50.0..200.0, "EV $expectedCoins")
    }

    @Test
    fun cooldownCountsDownToZero() {
        assertEquals(0, Cooldowns.remaining(GameKind.SPIN, null, 1_000))
        assertEquals(GameKind.SPIN.cooldownMillis - 10, Cooldowns.remaining(GameKind.SPIN, 0, 10))
        assertEquals(0, Cooldowns.remaining(GameKind.SCRATCH, 0, GameKind.SCRATCH.cooldownMillis + 1))
    }

    @Test
    fun playingRecordsRewardWithXpAndEnforcesCooldown() = runTest {
        val games = FakeGames()
        val clock = FakeClock()
        val play = PlayGameUseCase(games, clock, SequentialIds(), Random(1))

        val first = (play(GameKind.SPIN) as DomainResult.Success).value
        assertTrue(first.play.xp >= XpRules.SPIN_XP)
        assertEquals(RewardTables.SPIN[first.rewardIndex], first.play.reward)

        val second = play(GameKind.SPIN) as DomainResult.Failure
        assertIs<DomainError.Cooldown>(second.error)

        clock.advance(GameKind.SPIN.cooldownMillis)
        assertIs<DomainResult.Success<*>>(play(GameKind.SPIN))
    }

    @Test
    fun cooldownsAreIndependentPerGame() = runTest {
        val play = PlayGameUseCase(FakeGames(), FakeClock(), SequentialIds(), Random(1))
        play(GameKind.SPIN)
        assertIs<DomainResult.Success<*>>(play(GameKind.SCRATCH))
    }

    @Test
    fun lightningDealIsStablePerWindowAndRotates() {
        val deals = listOf(product("a"), product("b"), product("c"))
        val w = LightningDeal.WINDOW_MILLIS
        val first = LightningDeal.forTime(deals, 5 * w + 10)!!
        assertEquals(first.product.id, LightningDeal.forTime(deals.reversed(), 5 * w + w - 1)!!.product.id)
        assertEquals(w - 10, first.remainingMillis(5 * w + 10))
        val next = LightningDeal.forTime(deals, 6 * w)!!
        assertTrue(next.product.id != first.product.id)
        assertTrue(first.claimedFraction(5 * w) < first.claimedFraction(6 * w - 1))
    }
}

class WheelGeometryTest {
    @Test
    fun targetLandsOnTheChosenSegmentAfterFullTurns() {
        val segments = 8
        (0 until segments).forEach { index ->
            val target = com.cikup.amazgone.games.domain.model.WheelGeometry.targetRotation(123f, index, segments, extraTurns = 6)
            assertEquals(index, com.cikup.amazgone.games.domain.model.WheelGeometry.segmentAt(target, segments))
            assertTrue(target - 123f >= 5 * 360f)
        }
    }
}

class ScratchCoverageTest {
    @Test
    fun coverageGrowsWithStrokesAndReachesTheThreshold() {
        val coverage = com.cikup.amazgone.games.domain.model.ScratchCoverage()
        assertEquals(0f, coverage.fraction)
        coverage.scratch(0.5f, 0.5f)
        val single = coverage.fraction
        assertTrue(single > 0f && single < 0.1f)
        coverage.scratch(0.5f, 0.5f)
        assertEquals(single, coverage.fraction, "scratching the same spot adds nothing")
        var y = 0f
        while (y <= 1f) { var x = 0f; while (x <= 1f) { coverage.scratch(x, y); x += 0.05f }; y += 0.05f }
        assertTrue(coverage.fraction >= com.cikup.amazgone.games.domain.model.ScratchCoverage.REVEAL_THRESHOLD)
    }
}
