package com.cikup.amazgone.core.di

import com.cikup.amazgone.account.presentation.AccountViewModel
import com.cikup.amazgone.cart.presentation.CartViewModel
import com.cikup.amazgone.orders.presentation.checkout.CheckoutViewModel
import com.cikup.amazgone.orders.presentation.detail.OrderDetailViewModel
import com.cikup.amazgone.orders.presentation.list.OrdersViewModel
import com.cikup.amazgone.wallet.presentation.WalletViewModel
import com.cikup.amazgone.wishlist.presentation.WishlistViewModel
import com.cikup.amazgone.catalog.presentation.detail.ProductDetailViewModel
import com.cikup.amazgone.catalog.presentation.home.HomeViewModel
import com.cikup.amazgone.catalog.presentation.search.SearchViewModel
import com.cikup.amazgone.games.presentation.deal.DealViewModel
import com.cikup.amazgone.games.presentation.hub.GamesHubViewModel
import com.cikup.amazgone.games.presentation.scratch.ScratchViewModel
import com.cikup.amazgone.games.presentation.spin.SpinViewModel
import com.cikup.amazgone.navigation.presentation.ShellViewModel
import com.cikup.amazgone.progress.presentation.AchievementsViewModel
import com.cikup.amazgone.progress.presentation.LeaderboardViewModel
import com.cikup.amazgone.catalog.presentation.categories.CategoriesViewModel
import com.cikup.amazgone.catalog.presentation.flash.FlashSaleViewModel
import com.cikup.amazgone.settings.presentation.ThemeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val presentationModule = module {
    viewModelOf(::ShellViewModel)
    viewModelOf(::ThemeViewModel)
    viewModelOf(::HomeViewModel)
    viewModel { params -> SearchViewModel(params.getOrNull(), get(), get(), get()) }
    viewModelOf(::FlashSaleViewModel)
    viewModelOf(::CategoriesViewModel)
    viewModelOf(::CartViewModel)
    viewModelOf(::ProductDetailViewModel)
    viewModelOf(::AccountViewModel)
    viewModelOf(::CheckoutViewModel)
    viewModelOf(::OrdersViewModel)
    viewModelOf(::OrderDetailViewModel)
    viewModelOf(::WishlistViewModel)
    viewModelOf(::WalletViewModel)
    viewModelOf(::GamesHubViewModel)
    viewModelOf(::SpinViewModel)
    viewModelOf(::ScratchViewModel)
    viewModelOf(::DealViewModel)
    viewModelOf(::LeaderboardViewModel)
    viewModelOf(::AchievementsViewModel)
}
