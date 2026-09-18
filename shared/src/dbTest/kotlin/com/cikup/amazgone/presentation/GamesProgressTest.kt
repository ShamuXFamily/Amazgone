package com.cikup.amazgone.presentation

import com.cikup.amazgone.account.domain.repository.AuthRepository
import com.cikup.amazgone.catalog.data.mapper.CatalogBatch
import com.cikup.amazgone.catalog.data.repository.CatalogRepositoryImpl
import com.cikup.amazgone.core.remote.FirebaseServices
import com.cikup.amazgone.core.sync.domain.OutboxStore
import com.cikup.amazgone.core.sync.domain.PushResult
import com.cikup.amazgone.core.sync.domain.SyncEngine
import com.cikup.amazgone.games.data.GameRewardHandler
import com.cikup.amazgone.games.data.REJECT_COOLDOWN
import com.cikup.amazgone.games.domain.model.GameKind
import com.cikup.amazgone.games.presentation.deal.DealEffect
import com.cikup.amazgone.games.presentation.deal.DealIntent
import com.cikup.amazgone.games.presentation.deal.DealViewModel
import com.cikup.amazgone.games.presentation.hub.GamesHubViewModel
import com.cikup.amazgone.games.presentation.scratch.ScratchIntent
import com.cikup.amazgone.games.presentation.scratch.ScratchViewModel
import com.cikup.amazgone.games.presentation.spin.SpinIntent
import com.cikup.amazgone.games.presentation.spin.SpinViewModel
import com.cikup.amazgone.progress.data.DailyVisitTask
import com.cikup.amazgone.progress.data.DailyXpHandler
import com.cikup.amazgone.progress.data.REJECT_ALREADY_CLAIMED
import com.cikup.amazgone.progress.presentation.AchievementsViewModel
import com.cikup.amazgone.progress.presentation.LeaderboardViewModel
import com.cikup.amazgone.remote.DOCS
import com.cikup.amazgone.remote.ok
import com.cikup.amazgone.remote.userDoc
import com.cikup.amazgone.testing.TestGraph
import com.cikup.amazgone.testing.eventually
import com.cikup.amazgone.testing.product
import com.cikup.amazgone.wallet.domain.model.Currency
import com.cikup.amazgone.wallet.domain.repository.WalletRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import com.cikup.amazgone.testing.mainForViewModels
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private const val TOKENS = """{"localId":"uid-1","idToken":"id-1","refreshToken":"r-1","expiresIn":"3600"}"""

class GamesProgressTest {
    private lateinit var graph: TestGraph

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(mainForViewModels)
        graph = TestGraph()
    }

    @AfterTest
    fun tearDown() {
        graph.close()
        Dispatchers.resetMain()
    }

    private suspend fun signIn() {
        graph.backend.onPathEnds("POST", "accounts:signInWithPassword", ok(TOKENS))
        graph.backend.onPathEnds("POST", ":beginTransaction", ok("""{"transaction":"tx"}"""))
        graph.backend.onPathEnds("POST", ":commit", ok("{}"))
        graph.get<AuthRepository>().login("bob", "password1")
    }

    @Test
    fun spinLandsOnTheChosenSegmentAndStartsTheCooldown() = runTest {
        val vm = graph.get<SpinViewModel>()
        vm.state.eventually { it.canSpin }
        vm.onIntent(SpinIntent.Spin)
        val spinning = vm.state.eventually { it.targetIndex != null }
        assertTrue(spinning.isSpinning)

        vm.onIntent(SpinIntent.Landed)
        val landed = vm.state.eventually { it.wonReward != null }
        assertEquals(landed.segments[spinning.targetIndex!!], landed.wonReward)
        vm.state.eventually { it.cooldown > 0 && !it.canSpin }
        assertTrue(graph.get<WalletRepository>().balance(Currency.XP) >= 25)
    }

    @Test
    fun scratchCardRevealsAndRestoresDuringCooldown() = runTest {
        val vm = graph.get<ScratchViewModel>()
        vm.state.eventually { it.cooldown == 0L }
        vm.onIntent(ScratchIntent.NewCard)
        val card = vm.state.eventually { it.card != null }.card!!
        vm.onIntent(ScratchIntent.Revealed)
        vm.state.eventually { it.revealed }

        val returning = graph.get<ScratchViewModel>()
        assertEquals(card.play.id, returning.state.eventually { it.card != null }.card!!.play.id)
    }

    @Test
    fun hubShowsCooldownsAndTheHourlyDeal() = runTest {
        graph.get<CatalogRepositoryImpl>().save(CatalogBatch(listOf(product("1", price = 10.0, original = 20.0)), emptyList()))
        val hub = graph.get<GamesHubViewModel>()
        val state = hub.state.eventually { it.deal != null }
        assertEquals(0L, state.spinCooldown)
        assertTrue(state.deal!!.remainingMillis(graph.clock.now) > 0)
    }

    @Test
    fun claimingTheDealAddsCouponAndCartItem() = runTest {
        graph.get<CatalogRepositoryImpl>().save(CatalogBatch(listOf(product("1", price = 10.0, original = 20.0)), emptyList()))
        val vm = graph.get<DealViewModel>()
        val deal = vm.state.eventually { it.deal != null }
        assertEquals(80L, deal.dealPriceCoins)
        vm.onIntent(DealIntent.Claim)
        assertIs<DealEffect.Claimed>(vm.effects.first())
    }

    @Test
    fun rewardHandlerCreditsServerOrRejectsDuringServerCooldown() = runTest {
        signIn()
        graph.get<SpinViewModel>().apply {
            state.eventually { it.canSpin }
            onIntent(SpinIntent.Spin)
            state.eventually { it.targetIndex != null }
        }
        val handler = graph.get<GameRewardHandler>()
        val entry = graph.get<OutboxStore>().head(50).first { it.type == handler.type }

        graph.backend.onPathEnds("GET", "users/uid-1", ok(userDoc("uid-1", coins = 100)))
        assertEquals(PushResult.Success, handler.push(entry))
        val commit = graph.backend.bodies.last { it?.get("writes") != null }!!.toString()
        assertTrue("updateTransforms" in commit && "lastSpinAt" in commit)

        val recent = userDoc("uid-1", coins = 100).replace(
            "\"level\":{\"integerValue\":\"1\"}",
            "\"level\":{\"integerValue\":\"1\"},\"lastSpinAt\":{\"timestampValue\":\"2100-01-01T00:00:00Z\"}",
        )
        graph.backend.onPathEnds("GET", "users/uid-1", ok(recent))
        assertEquals(PushResult.Rejected(REJECT_COOLDOWN), handler.push(entry))
    }

    @Test
    fun dailyVisitGrantsXpOncePerDay() = runTest {
        val task = graph.get<DailyVisitTask>()
        task.run()
        task.run()
        assertEquals(20L, graph.get<WalletRepository>().balance(Currency.XP))
        graph.clock.advance(24 * 60 * 60 * 1_000L)
        task.run()
        assertEquals(40L, graph.get<WalletRepository>().balance(Currency.XP))

        signIn()
        val handler = graph.get<DailyXpHandler>()
        val entries = graph.get<OutboxStore>().head(50).filter { it.type == handler.type }
        graph.backend.onPathEnds("GET", "users/uid-1", ok(userDoc("uid-1", coins = 0)))
        assertEquals(PushResult.Success, handler.push(entries.first()))
        val claimedToday = userDoc("uid-1", coins = 0).replace(
            "\"level\":{\"integerValue\":\"1\"}",
            "\"level\":{\"integerValue\":\"1\"},\"lastDailyAt\":{\"timestampValue\":\"2100-01-01T00:00:00Z\"}",
        )
        graph.backend.onPathEnds("GET", "users/uid-1", ok(claimedToday))
        assertEquals(PushResult.Rejected(REJECT_ALREADY_CLAIMED), handler.push(entries.last()))
        handler.onRejected(entries.last(), REJECT_ALREADY_CLAIMED)
        assertEquals(20L, graph.get<WalletRepository>().balance(Currency.XP))
    }

    @Test
    fun achievementsUnlockFromActivity() = runTest {
        val vm = graph.get<AchievementsViewModel>()
        vm.state.eventually { it.achievements.isNotEmpty() && it.unlockedCount == 0 }
        graph.get<com.cikup.amazgone.progress.domain.usecase.TrackAchievementsUseCase>()().let { tracker ->
            graph.get<SpinViewModel>().apply {
                state.eventually { it.canSpin }
                onIntent(SpinIntent.Spin)
                state.eventually { it.targetIndex != null }
            }
            assertTrue(com.cikup.amazgone.progress.domain.model.AchievementId.LUCKY_SPIN in tracker.first())
        }
        vm.state.eventually { it.unlockedCount == 1 }
        assertTrue(graph.get<OutboxStore>().head(50).any { it.type == "achievement.unlock" })
    }

    @Test
    fun leaderboardSyncsTopPlayersForSignedInUsers() = runTest {
        signIn()
        graph.backend.onPathEnds(
            "POST", ":runQuery",
            ok("""[{"document":{"name":"$DOCS/leaderboard/uid-1","fields":{"username":{"stringValue":"bob"},"xp":{"integerValue":"900"},"level":{"integerValue":"4"}}}},
                  {"document":{"name":"$DOCS/leaderboard/uid-2","fields":{"username":{"stringValue":"ann"},"xp":{"integerValue":"50"},"level":{"integerValue":"1"}}}}]"""),
        )
        graph.backend.on({ it.method.value == "GET" }, ok("{}"))
        val vm = graph.get<LeaderboardViewModel>()
        graph.get<SyncEngine>().sync(forcePull = true)
        val board = vm.state.eventually { it.leaderboard.entries.size == 2 }.leaderboard
        assertEquals(listOf(1, 2), board.entries.map { it.rank })
        assertEquals("bob", board.myEntry?.username)
    }

    @Test
    fun guestsCannotPushSoRewardsStayPending() = runTest {
        graph.get<SpinViewModel>().apply {
            state.eventually { it.canSpin }
            onIntent(SpinIntent.Spin)
            state.eventually { it.targetIndex != null }
        }
        graph.get<SyncEngine>().sync()
        assertTrue(graph.get<OutboxStore>().head(50).any { it.type == GameRewardHandler::class.simpleName.let { "game.reward" } })
        assertEquals(false, graph.get<FirebaseServices>().isAvailable.not())
        assertTrue(GameKind.SPIN.cooldownMillis > 0)
    }
}
