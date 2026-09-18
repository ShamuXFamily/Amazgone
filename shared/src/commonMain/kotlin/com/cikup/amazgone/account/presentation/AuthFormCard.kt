package com.cikup.amazgone.account.presentation

import com.cikup.amazgone.core.designsystem.component.appCardColors
import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.account_confirm_password
import amazgone.shared.generated.resources.account_create
import amazgone.shared.generated.resources.account_hide_password
import amazgone.shared.generated.resources.account_online_required
import amazgone.shared.generated.resources.account_password
import amazgone.shared.generated.resources.account_show_password
import amazgone.shared.generated.resources.account_sign_in
import amazgone.shared.generated.resources.account_username
import amazgone.shared.generated.resources.account_username_hint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.motion.shakeOnChange
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.presentation.error.message
import org.jetbrains.compose.resources.stringResource

@Composable
fun AuthFormCard(form: AuthForm, remoteAvailable: Boolean, onIntent: (AccountIntent) -> Unit, modifier: Modifier = Modifier) {
    ElevatedCard(modifier.fillMaxWidth().shakeOnChange(form.errorPulse), colors = appCardColors()) {
        SecondaryTabRow(selectedTabIndex = form.mode.ordinal) {
            AuthMode.entries.forEach { mode ->
                Tab(
                    selected = form.mode == mode,
                    onClick = { onIntent(AccountIntent.SwitchMode(mode)) },
                    text = { Text(stringResource(if (mode == AuthMode.SIGN_IN) Res.string.account_sign_in else Res.string.account_create)) },
                )
            }
        }
        Column(Modifier.padding(AmazgoneDimens.spaceLg), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd)) {
            OutlinedTextField(
                value = form.username,
                onValueChange = { onIntent(AccountIntent.UsernameChanged(it)) },
                label = { Text(stringResource(Res.string.account_username)) },
                singleLine = true,
                isError = "username" in form.fieldErrors,
                supportingText = {
                    Text(form.fieldErrors["username"]?.reason?.message() ?: stringResource(Res.string.account_username_hint))
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next, autoCorrectEnabled = false),
                modifier = Modifier.fillMaxWidth(),
            )
            PasswordField(form.password, Res.string.account_password, form.fieldErrors["password"]?.reason?.message()) {
                onIntent(AccountIntent.PasswordChanged(it))
            }
            AnimatedVisibility(
                visible = form.mode == AuthMode.REGISTER,
                enter = expandVertically(MotionTokens.gentle()) + fadeIn(),
                exit = shrinkVertically(MotionTokens.snappy()) + fadeOut(),
            ) {
                PasswordField(form.confirmation, Res.string.account_confirm_password, form.fieldErrors["confirmation"]?.reason?.message()) {
                    onIntent(AccountIntent.ConfirmationChanged(it))
                }
            }
            form.formError?.let { Text(it.message(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            if (!remoteAvailable) {
                Text(stringResource(Res.string.account_online_required), style = MaterialTheme.typography.bodySmall)
            }
            SubmitButton(form) { onIntent(AccountIntent.Submit) }
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    label: org.jetbrains.compose.resources.StringResource,
    error: String?,
    onChange: (String) -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label)) },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    stringResource(if (visible) Res.string.account_hide_password else Res.string.account_show_password),
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Label morphs into a spinner while submitting. */
@Composable
private fun SubmitButton(form: AuthForm, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = !form.isSubmitting, modifier = Modifier.fillMaxWidth()) {
        AnimatedContent(
            targetState = form.isSubmitting,
            transitionSpec = { (fadeIn() + scaleIn(MotionTokens.bouncy())).togetherWith(fadeOut()) },
            label = "submit",
        ) { submitting ->
            if (submitting) {
                CircularProgressIndicator(Modifier.size(AmazgoneDimens.iconMd), strokeWidth = AmazgoneDimens.spaceXs / 2)
            } else {
                Text(stringResource(if (form.mode == AuthMode.SIGN_IN) Res.string.account_sign_in else Res.string.account_create))
            }
        }
    }
}

/** Full-row click target for ListItem menus. */
fun Modifier.clickableRow(onClick: () -> Unit): Modifier = clickable(onClick = onClick)
