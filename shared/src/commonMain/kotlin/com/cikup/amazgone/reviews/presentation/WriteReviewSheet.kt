package com.cikup.amazgone.reviews.presentation

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.review_comment
import amazgone.shared.generated.resources.review_comment_short
import amazgone.shared.generated.resources.review_rating_1
import amazgone.shared.generated.resources.review_rating_2
import amazgone.shared.generated.resources.review_rating_3
import amazgone.shared.generated.resources.review_rating_4
import amazgone.shared.generated.resources.review_rating_5
import amazgone.shared.generated.resources.review_star
import amazgone.shared.generated.resources.review_submit
import amazgone.shared.generated.resources.review_tap_to_rate
import amazgone.shared.generated.resources.review_title
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.cikup.amazgone.core.designsystem.component.ProductImage
import com.cikup.amazgone.core.designsystem.component.rememberFieldText
import com.cikup.amazgone.core.designsystem.motion.LocalReduceMotion
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.motion.shakeOnChange
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import com.cikup.amazgone.reviews.domain.model.ReviewProblem
import com.cikup.amazgone.reviews.domain.model.ReviewRules
import org.jetbrains.compose.resources.stringResource

/** What the sheet shows; kept free of order types so any screen can host it. */
data class ReviewSheetModel(
    val productId: String,
    val title: String,
    val thumbnailUrl: String,
    val rating: Int,
    val comment: String,
    val problems: Set<ReviewProblem>,
    val isSubmitting: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteReviewSheet(
    model: ReviewSheetModel,
    onRating: (Int) -> Unit,
    onComment: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            Modifier.fillMaxWidth().imePadding().navigationBarsPadding().padding(horizontal = AmazgoneDimens.spaceLg).padding(bottom = AmazgoneDimens.spaceLg),
            verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceLg),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd)) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium) {
                    ProductImage(model.thumbnailUrl, model.productId, Modifier.size(AmazgoneDimens.iconLg + AmazgoneDimens.spaceSm).padding(AmazgoneDimens.spaceXs))
                }
                Column(Modifier.weight(1f)) {
                    Text(stringResource(Res.string.review_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(model.title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            StarPicker(model.rating, onRating, Modifier.shakeOnChange(if (ReviewProblem.NO_RATING in model.problems) model.problems.hashCode() else 0))
            CommentField(model, onComment)
            Button(
                onClick = onSubmit,
                enabled = !model.isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = AmazgoneTheme.extended.cta, contentColor = AmazgoneTheme.extended.onCta),
                modifier = Modifier.fillMaxWidth().heightIn(min = AmazgoneDimens.minTouchTarget + AmazgoneDimens.spaceSm),
            ) {
                if (model.isSubmitting) {
                    CircularProgressIndicator(Modifier.size(AmazgoneDimens.iconSm + AmazgoneDimens.spaceXs), color = AmazgoneTheme.extended.onCta, strokeWidth = AmazgoneDimens.spaceXs / 2)
                } else {
                    Text(stringResource(Res.string.review_submit), style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}

/** Five big stars; the tapped one bounces and everything up to it lights up, with a label that slides in. */
@Composable
private fun StarPicker(rating: Int, onRating: (Int) -> Unit, modifier: Modifier) {
    val haptics = LocalHapticFeedback.current
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
            (1..5).forEach { star ->
                Star(filled = star <= rating, bounceKey = if (star == rating) rating else 0, label = stringResource(Res.string.review_star, star)) {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    onRating(star)
                }
            }
        }
        AnimatedContent(
            rating,
            transitionSpec = { (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut()) },
            label = "ratingLabel",
        ) { value ->
            Text(
                stringResource(RATING_LABELS.getOrNull(value - 1) ?: Res.string.review_tap_to_rate),
                style = MaterialTheme.typography.titleMedium,
                color = if (value == 0) MaterialTheme.colorScheme.onSurfaceVariant else AmazgoneTheme.extended.cta,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun Star(filled: Boolean, bounceKey: Int, label: String, onClick: () -> Unit) {
    val reduceMotion = LocalReduceMotion.current
    val scale = remember { Animatable(1f) }
    LaunchedEffect(bounceKey) {
        if (bounceKey > 0 && !reduceMotion) {
            scale.snapTo(STAR_POP)
            scale.animateTo(1f, MotionTokens.bouncy())
        }
    }
    val tint by animateColorAsState(if (filled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant, label = "star")
    Icon(
        if (filled) Icons.Rounded.Star else Icons.Rounded.StarOutline,
        contentDescription = label,
        tint = tint,
        modifier = Modifier.size(STAR_SIZE)
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .selectable(selected = filled, role = Role.RadioButton, onClick = onClick),
    )
}

@Composable
private fun CommentField(model: ReviewSheetModel, onComment: (String) -> Unit) {
    val short = ReviewProblem.COMMENT_TOO_SHORT in model.problems
    var text by rememberFieldText(model.comment, model.productId)
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it.take(ReviewRules.MAX_COMMENT)
            onComment(text)
        },
        label = { Text(stringResource(Res.string.review_comment)) },
        minLines = COMMENT_LINES,
        isError = short,
        supportingText = {
            Row(Modifier.fillMaxWidth()) {
                if (short) Text(stringResource(Res.string.review_comment_short), modifier = Modifier.weight(1f)) else Row(Modifier.weight(1f)) {}
                Text("${text.length} / ${ReviewRules.MAX_COMMENT}")
            }
        },
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    )
}

private val RATING_LABELS = listOf(Res.string.review_rating_1, Res.string.review_rating_2, Res.string.review_rating_3, Res.string.review_rating_4, Res.string.review_rating_5)
private val STAR_SIZE = AmazgoneDimens.iconLg
private const val STAR_POP = 1.35f
private const val COMMENT_LINES = 4
