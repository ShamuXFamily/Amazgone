package com.cikup.amazgone.catalog

import com.cikup.amazgone.catalog.data.sync.SeedCatalogImporter
import com.cikup.amazgone.catalog.domain.model.CatalogSourceId
import com.cikup.amazgone.delivery.domain.model.Origins
import com.cikup.amazgone.stores.domain.model.StoreDirectory
import com.cikup.amazgone.stores.domain.model.StoreKind
import com.cikup.amazgone.stores.presentation.BRAND_LOGOS
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Guards the hand-edited new_arrivals.json: every entry must survive validation and land in a complete brand store. */
class BundledCatalogTest {
    private val json = generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, "shared/src/commonMain/composeResources/files/seed/new_arrivals.json") }
        .first { it.exists() }
        .readText()

    private val products = SeedCatalogImporter.parseNewArrivals(json).products
    private val entries = Regex("\"id\": \"").findAll(json).count()

    @Test
    fun everyBundledProductIsValid() {
        assertEquals(entries, products.size, "a product in new_arrivals.json failed validation")
        assertEquals(products.size, products.map { it.id }.toSet().size, "duplicate ids")
        assertTrue(products.all { it.imageUrls.isNotEmpty() && it.details.imageCredit != null }, "every photo needs a credit")
    }

    @Test
    fun everyBrandHasAnOfficialStoreWithHeadquartersAndLogo() {
        products.mapNotNull { it.brand }.distinct().forEach { brand ->
            val store = StoreDirectory.resolve(CatalogSourceId.AMAZGONE, brand, "any", null)
            assertEquals(StoreKind.OFFICIAL, store.kind, "$brand should be an official store")
            assertTrue(Origins.headquarters(store.id) != null, "$brand needs its HQ in Origins")
            assertTrue(store.id in BRAND_LOGOS, "$brand needs a logo or colour in build_logos.py")
        }
    }
}
