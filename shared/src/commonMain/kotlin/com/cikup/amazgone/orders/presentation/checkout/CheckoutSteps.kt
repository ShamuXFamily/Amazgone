package com.cikup.amazgone.orders.presentation.checkout

import com.cikup.amazgone.core.designsystem.component.appCardColors
import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.checkout_after_purchase
import amazgone.shared.generated.resources.checkout_balance
import amazgone.shared.generated.resources.checkout_city
import amazgone.shared.generated.resources.checkout_country
import amazgone.shared.generated.resources.checkout_coupon_flat
import amazgone.shared.generated.resources.checkout_coupon_min
import amazgone.shared.generated.resources.checkout_coupon_percent
import amazgone.shared.generated.resources.checkout_coupons
import amazgone.shared.generated.resources.checkout_discount
import amazgone.shared.generated.resources.checkout_full_name
import amazgone.shared.generated.resources.checkout_invalid_postal
import amazgone.shared.generated.resources.checkout_items
import amazgone.shared.generated.resources.checkout_line1
import amazgone.shared.generated.resources.checkout_no_coupon
import amazgone.shared.generated.resources.checkout_pay_with_coins
import amazgone.shared.generated.resources.checkout_postal
import amazgone.shared.generated.resources.checkout_ship_to
import amazgone.shared.generated.resources.checkout_subtotal
import amazgone.shared.generated.resources.checkout_total
import amazgone.shared.generated.resources.error_field_empty
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.cikup.amazgone.cart.domain.model.Coupon
import com.cikup.amazgone.cart.domain.model.CouponKind
import com.cikup.amazgone.core.designsystem.component.CoinAmount
import com.cikup.amazgone.core.designsystem.component.ProductImage
import com.cikup.amazgone.core.designsystem.motion.staggeredEnter
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.presentation.format.Formatters
import com.cikup.amazgone.orders.domain.model.AddressValidator
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun AddressStep(state: CheckoutState, onIntent: (CheckoutIntent) -> Unit) {
    val a = state.address
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(AmazgoneDimens.spaceLg),
        verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm),
    ) {
        listOf(
            Triple(AddressValidator.FIELD_NAME, a.fullName, Res.string.checkout_full_name),
            Triple(AddressValidator.FIELD_LINE1, a.line1, Res.string.checkout_line1),
            Triple(AddressValidator.FIELD_CITY, a.city, Res.string.checkout_city),
            Triple(AddressValidator.FIELD_POSTAL, a.postalCode, Res.string.checkout_postal),
            Triple(AddressValidator.FIELD_COUNTRY, a.country, Res.string.checkout_country),
        ).forEachIndexed { index, (field, value, label) ->
            AddressField(field, value, label, field in state.invalidFields, Modifier.staggeredEnter(index)) {
                onIntent(CheckoutIntent.FieldChanged(field, it))
            }
        }
    }
}

@Composable
private fun AddressField(field: String, value: String, label: StringResource, invalid: Boolean, modifier: Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label)) },
        singleLine = true,
        isError = invalid,
        supportingText = if (invalid) {
            {
                Text(stringResource(if (field == AddressValidator.FIELD_POSTAL) Res.string.checkout_invalid_postal else Res.string.error_field_empty))
            }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun PaymentStep(state: CheckoutState, onIntent: (CheckoutIntent) -> Unit) {
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(AmazgoneDimens.spaceLg),
        verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceLg),
    ) {
        ElevatedCard(Modifier.fillMaxWidth().staggeredEnter(0), colors = appCardColors()) {
            Column(Modifier.padding(AmazgoneDimens.spaceLg), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
                Text(stringResource(Res.string.checkout_pay_with_coins), style = MaterialTheme.typography.titleMedium)
                LabeledAmount(Res.string.checkout_balance, state.balance)
                LabeledAmount(Res.string.checkout_after_purchase, state.balanceAfter, animate = true)
                ErrorBanner(state)
            }
        }
        Text(stringResource(Res.string.checkout_coupons), style = MaterialTheme.typography.titleMedium, modifier = Modifier.staggeredEnter(1))
        ElevatedCard(Modifier.fillMaxWidth().staggeredEnter(2), colors = appCardColors()) {
            CouponRow(null, state.selectedCouponCode == null) { onIntent(CheckoutIntent.SelectCoupon(null)) }
            state.coupons.forEach { coupon ->
                HorizontalDivider()
                CouponRow(coupon, state.selectedCouponCode == coupon.code) { onIntent(CheckoutIntent.SelectCoupon(coupon.code)) }
            }
        }
        Totals(state, Modifier.staggeredEnter(3))
    }
}

@Composable
private fun CouponRow(coupon: Coupon?, selected: Boolean, onSelect: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(
                when {
                    coupon == null -> stringResource(Res.string.checkout_no_coupon)
                    coupon.kind == CouponKind.PERCENT -> stringResource(Res.string.checkout_coupon_percent, coupon.value.toInt())
                    else -> stringResource(Res.string.checkout_coupon_flat, Formatters.coins(coupon.value))
                },
            )
        },
        supportingContent = coupon?.takeIf { it.minSubtotalCoins > 0 }?.let {
            { Text(stringResource(Res.string.checkout_coupon_min, Formatters.coins(it.minSubtotalCoins))) }
        },
        overlineContent = coupon?.let { { Text(it.code) } },
        leadingContent = { RadioButton(selected = selected, onClick = null) },
        modifier = Modifier.selectable(selected = selected, onClick = onSelect, role = Role.RadioButton),
    )
}

@Composable
fun ReviewStep(state: CheckoutState) {
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(AmazgoneDimens.spaceLg),
        verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceLg),
    ) {
        ElevatedCard(Modifier.fillMaxWidth().staggeredEnter(0), colors = appCardColors()) {
            Column(Modifier.padding(AmazgoneDimens.spaceLg), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs)) {
                Text(stringResource(Res.string.checkout_ship_to), style = MaterialTheme.typography.titleMedium)
                with(state.address) {
                    Text(fullName)
                    Text(line1)
                    Text("$city $postalCode, $country")
                }
            }
        }
        ElevatedCard(Modifier.fillMaxWidth().staggeredEnter(1), colors = appCardColors()) {
            Text(
                stringResource(Res.string.checkout_items),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(AmazgoneDimens.spaceLg),
            )
            state.summary.lines.forEach { line ->
                ListItem(
                    leadingContent = {
                        ProductImage(line.product.thumbnailUrl, line.product.id, Modifier.size(AmazgoneDimens.iconLg).clip(MaterialTheme.shapes.small))
                    },
                    headlineContent = { Text(line.product.title, maxLines = 1) },
                    supportingContent = { Text("× ${line.quantity}") },
                    trailingContent = { CoinAmount(line.lineTotalCoins, style = MaterialTheme.typography.bodyMedium) },
                )
            }
        }
        Totals(state, Modifier.staggeredEnter(2))
        ErrorBanner(state)
    }
}

@Composable
private fun Totals(state: CheckoutState, modifier: Modifier = Modifier) {
    ElevatedCard(modifier.fillMaxWidth(), colors = appCardColors()) {
        Column(Modifier.padding(AmazgoneDimens.spaceLg), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
            LabeledAmount(Res.string.checkout_subtotal, state.summary.subtotalCoins)
            if (state.summary.couponDiscountCoins > 0) {
                LabeledAmount(Res.string.checkout_discount, -state.summary.couponDiscountCoins, animate = true)
            }
            HorizontalDivider()
            LabeledAmount(Res.string.checkout_total, state.summary.totalCoins, animate = true, emphasized = true)
        }
    }
}

@Composable
private fun LabeledAmount(label: StringResource, amount: Long, animate: Boolean = false, emphasized: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(label), style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge)
        CoinAmount(
            amount,
            style = if (emphasized) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
            animateChanges = animate,
        )
    }
}
