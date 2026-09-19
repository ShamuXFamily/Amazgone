package com.cikup.amazgone.stores

import com.cikup.amazgone.catalog.domain.model.CatalogSourceId
import com.cikup.amazgone.catalog.domain.model.ProductDetails
import com.cikup.amazgone.stores.domain.model.StoreDirectory
import com.cikup.amazgone.stores.domain.model.StoreKind
import com.cikup.amazgone.testing.product
import kotlin.test.Test
import kotlin.test.assertEquals

class StoreDirectoryTest {
    @Test
    fun realManufacturersGetAnOfficialStore() {
        val iphone = product("1").copy(brand = "Apple")
        assertEquals("official-apple", iphone.store.id)
        assertEquals("Apple Official", iphone.store.name)
        assertEquals(StoreKind.OFFICIAL, iphone.store.kind)
        assertEquals("official-dolce-gabbana", product("2").copy(brand = "Dolce & Gabbana").store.id)
    }

    @Test
    fun madeUpBrandsGetAPlainBrandStore() {
        val store = product("1").copy(brand = "Urban Chic").store
        assertEquals("brand-urban-chic", store.id)
        assertEquals("Urban Chic", store.name)
        assertEquals(StoreKind.BRAND, store.kind)
    }

    @Test
    fun gamesAreSoldByTheirDigitalStore() {
        val game = product("g", category = "video-games").copy(source = CatalogSourceId.CHEAP_SHARK, brand = null, details = ProductDetails(sellerId = "1"))
        assertEquals("digital-1", game.store.id)
        assertEquals("Steam", game.store.name)
        assertEquals(StoreKind.DIGITAL, game.store.kind)
        // unknown or missing seller falls back to first party
        assertEquals("amazgone", game.copy(details = ProductDetails(sellerId = "999")).store.id)
    }

    @Test
    fun unbrandedGoodsAreFirstParty() {
        assertEquals("amazgone", product("1", category = "kitchen-accessories").copy(brand = null).store.id)
        val fresh = product("2", category = "groceries").copy(brand = null).store
        assertEquals("amazgone-fresh", fresh.id)
        assertEquals(StoreKind.AMAZGONE, fresh.kind)
    }
}
