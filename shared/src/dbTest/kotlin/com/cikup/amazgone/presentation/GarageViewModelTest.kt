package com.cikup.amazgone.presentation

import com.cikup.amazgone.delivery.domain.model.Courier
import com.cikup.amazgone.delivery.presentation.GarageEffect
import com.cikup.amazgone.delivery.presentation.GarageIntent
import com.cikup.amazgone.delivery.presentation.GarageViewModel
import com.cikup.amazgone.testing.TestGraph
import com.cikup.amazgone.testing.eventually
import com.cikup.amazgone.testing.mainForViewModels
import com.cikup.amazgone.wallet.domain.usecase.ObserveWalletUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GarageViewModelTest {
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

    @Test
    fun buyingAskesFirstThenUnlocksAndDebitsCoins() = runTest {
        graph.grantGuestWallet()
        val vm = graph.get<GarageViewModel>()
        val start = vm.state.eventually { it.balance == 5_000L }
        assertEquals(Courier.STARTERS, start.owned)

        vm.onIntent(GarageIntent.RequestBuy(Courier.PONY_EXPRESS))
        assertEquals(Courier.PONY_EXPRESS, vm.state.eventually { it.confirming != null }.confirming)
        vm.onIntent(GarageIntent.ConfirmBuy)
        assertEquals(GarageEffect.Unlocked(Courier.PONY_EXPRESS), vm.effects.first())
        assertTrue(Courier.PONY_EXPRESS in vm.state.eventually { Courier.PONY_EXPRESS in it.owned }.owned)
        assertEquals(5_000L - Courier.PONY_EXPRESS.priceCoins, graph.get<ObserveWalletUseCase>()().first { it.coins < 5_000 }.coins)
    }

    @Test
    fun cannotBuyWhatYouCannotAfford() = runTest {
        graph.grantGuestWallet()
        val vm = graph.get<GarageViewModel>()
        vm.state.eventually { it.balance == 5_000L }
        vm.onIntent(GarageIntent.RequestBuy(Courier.TELEPORTER)) // 20,000 coins
        assertEquals(GarageEffect.NotEnoughCoins(Courier.TELEPORTER.priceCoins - 5_000), vm.effects.first())
    }
}
