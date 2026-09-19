package com.cikup.amazgone.orders.presentation.checkout

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.checkout_address_hint
import amazgone.shared.generated.resources.checkout_city
import amazgone.shared.generated.resources.checkout_country
import amazgone.shared.generated.resources.checkout_full_name
import amazgone.shared.generated.resources.checkout_invalid_postal
import amazgone.shared.generated.resources.checkout_line1
import amazgone.shared.generated.resources.checkout_postal
import amazgone.shared.generated.resources.error_field_empty
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.cikup.amazgone.core.designsystem.component.rememberFieldText
import com.cikup.amazgone.core.designsystem.motion.staggeredEnter
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
            Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = AmazgoneTheme.extended.cta)
            Text(stringResource(Res.string.checkout_address_hint), style = MaterialTheme.typography.titleMedium)
        }
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
    var text by rememberFieldText(value, field)
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onChange(it)
        },
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
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    )
}
