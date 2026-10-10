package com.uzxsan.dev.track.money

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uzxsan.dev.track.R
import com.uzxsan.dev.track.money.data.MoneyTransaction
import com.uzxsan.dev.track.money.data.TransactionType
import com.uzxsan.dev.track.ui.components.GlassSurface
import com.uzxsan.dev.track.ui.theme.InterDisplay
import com.uzxsan.dev.track.ui.theme.accentOutline
import com.uzxsan.dev.track.ui.theme.accentTrack
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/*
 * Budget follows the spec's typography: thin (Light / ExtraLight) amounts, no bold anywhere.
 */

/** One shared formatter for the screen; amounts are minor units in the device currency. */
@Composable
fun rememberMoneyFormat(): MoneyFormat = remember { MoneyFormat() }

/** Thin amount text that shrinks to fit its width instead of wrapping or clipping. */
@Composable
fun ThinAmount(
    text: String,
    modifier: Modifier = Modifier,
    maxSize: TextUnit = 24.sp,
    minSize: TextUnit = 12.sp,
    color: Color = MaterialTheme.colorScheme.onSurface,
    weight: FontWeight = FontWeight.Light,
    textAlign: TextAlign = TextAlign.Start,
) {
    BasicText(
        text = text,
        modifier = modifier,
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = minSize, maxFontSize = maxSize),
        style = TextStyle(
            fontFamily = InterDisplay,
            fontWeight = weight,
            color = color,
            letterSpacing = (-0.3).sp,
            textAlign = textAlign,
        ),
    )
}

/** Large ExtraLight amount input with the currency symbol, like a calculator display. */
@Composable
fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    format: MoneyFormat,
    modifier: Modifier = Modifier,
    size: TextUnit = 44.sp,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White.copy(alpha = 0.7f))
            .border(1.dp, accentOutline().copy(alpha = 0.25f), shape)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            format.symbol,
            fontFamily = InterDisplay,
            fontWeight = FontWeight.ExtraLight,
            fontSize = size * 0.6f,
            color = scheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(8.dp))
        BasicTextField(
            value = value,
            onValueChange = { input -> sanitizeAmount(input, format.fractionDigits)?.let(onValueChange) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            cursorBrush = SolidColor(scheme.primary),
            textStyle = TextStyle(
                fontFamily = InterDisplay,
                fontWeight = FontWeight.ExtraLight,
                fontSize = size,
                color = scheme.onSurface,
                letterSpacing = (-0.5).sp,
            ),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            "0",
                            fontFamily = InterDisplay,
                            fontWeight = FontWeight.ExtraLight,
                            fontSize = size,
                            color = scheme.onSurfaceVariant.copy(alpha = 0.4f),
                        )
                    }
                    inner()
                }
            },
            modifier = Modifier.weight(1f),
        )
    }
}

/** Keeps digits and a single '.' with at most [fractionDigits] decimals; null rejects the edit. */
internal fun sanitizeAmount(input: String, fractionDigits: Int): String? {
    val normalized = input.replace(',', '.')
    if (normalized.any { !it.isDigit() && it != '.' }) return null
    val parts = normalized.split('.')
    if (parts.size > 2) return null
    if (parts.size == 2 && parts[1].length > fractionDigits) return null
    if (parts[0].length > 12) return null
    return normalized
}

/** Icon, tint and container color for each transaction type. */
@Composable
fun TypeBadge(type: TransactionType, size: Dp = 40.dp) {
    val scheme = MaterialTheme.colorScheme
    val (icon, container, content) = when (type) {
        TransactionType.Income -> Triple(R.drawable.ic_income, scheme.primaryContainer, scheme.onPrimaryContainer)
        TransactionType.Expense -> Triple(R.drawable.ic_expense, accentTrack(), scheme.onSurfaceVariant)
        TransactionType.Transfer -> Triple(R.drawable.ic_transfer, scheme.tertiaryContainer, scheme.onTertiaryContainer)
    }
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = content, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun typeLabel(type: TransactionType): String = stringResource(
    when (type) {
        TransactionType.Expense -> R.string.budget_expense
        TransactionType.Income -> R.string.budget_income
        TransactionType.Transfer -> R.string.budget_transfer
    }
)

/** Signed amount from the user's point of view: income +, expense −, transfer unsigned. */
fun MoneyFormat.forRow(t: MoneyTransaction): String = when (t.type) {
    TransactionType.Income -> formatSigned(t.amount)
    TransactionType.Expense -> formatSigned(-t.amount)
    TransactionType.Transfer -> format(t.amount)
}

private val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

@Composable
fun TransactionRow(
    transaction: MoneyTransaction,
    accountName: (Long?) -> String,
    format: MoneyFormat,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val title = when {
        transaction.type == TransactionType.Transfer -> stringResource(R.string.budget_transfer_title)
        transaction.category.isNotBlank() -> transaction.category
        transaction.note.isNotBlank() -> transaction.note
        else -> typeLabel(transaction.type)
    }
    val account = if (transaction.type == TransactionType.Transfer) {
        "${accountName(transaction.accountId)} → ${accountName(transaction.toAccountId)}"
    } else {
        accountName(transaction.accountId)
    }
    val note = transaction.note.takeIf { it.isNotBlank() && it != title }
    GlassSurface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), onClick = onClick) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            TypeBadge(transaction.type)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = listOfNotNull(account, transaction.localTime.format(timeFormatter), note).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.size(12.dp))
            ThinAmount(
                text = format.forRow(transaction),
                maxSize = 18.sp,
                color = when (transaction.type) {
                    TransactionType.Income -> scheme.primary
                    TransactionType.Expense -> scheme.onSurface
                    TransactionType.Transfer -> scheme.onSurfaceVariant
                },
                weight = FontWeight.Normal,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(0.6f, fill = false),
            )
        }
    }
}
