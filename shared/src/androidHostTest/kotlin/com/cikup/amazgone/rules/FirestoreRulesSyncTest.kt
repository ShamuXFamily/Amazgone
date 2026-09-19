package com.cikup.amazgone.rules

import com.cikup.amazgone.delivery.domain.model.Courier
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/** firestore.rules hard-codes courier prices; this fails if the app and the rules ever disagree. */
class FirestoreRulesSyncTest {
    private val rules = generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, "firestore.rules") }
        .first { it.exists() }
        .readText()

    @Test
    fun courierPricesMatchTheApp() {
        val map = Regex("""function courierPrices\(\) \{ return \{([^}]*)\}; \}""").find(rules)!!.groupValues[1]
        val inRules = Regex("""'([A-Z_]+)': (\d+)""").findAll(map).associate { it.groupValues[1] to it.groupValues[2].toLong() }
        val inApp = Courier.entries.filterNot { it.isStarter }.associate { it.name to it.priceCoins }
        assertEquals(inApp, inRules)
    }
}
