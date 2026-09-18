package com.cikup.amazgone.catalog.domain.model

data class HomeFeed(
    val deals: List<Product>,
    val topRated: List<Product>,
    val categories: List<Category>,
    val products: List<Product>,
) {
    val isEmpty: Boolean get() = products.isEmpty() && deals.isEmpty()
}
