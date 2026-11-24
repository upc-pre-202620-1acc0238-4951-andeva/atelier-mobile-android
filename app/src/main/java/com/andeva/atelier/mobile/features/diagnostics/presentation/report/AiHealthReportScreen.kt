package com.andeva.atelier.mobile.features.diagnostics.presentation.report

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBottomSheet
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierEmptyState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierErrorState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierFichaPanel
import com.andeva.atelier.mobile.core.designsystem.component.AtelierListRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierMetricBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierNoPermissionState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierOfflineState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierPlanLockedState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierPlate
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRefreshBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRequiresConnection
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSectionHeader
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSprings
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSurface
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.component.LocalAtelierMinTouchTarget
import com.andeva.atelier.mobile.core.designsystem.component.PlateSize
import com.andeva.atelier.mobile.core.designsystem.component.SurfaceTier
import com.andeva.atelier.mobile.core.designsystem.component.bone
import com.andeva.atelier.mobile.core.designsystem.component.rememberAtelierHaptics
import com.andeva.atelier.mobile.core.designsystem.component.rememberDelayedLoading
import com.andeva.atelier.mobile.core.designsystem.component.rememberReducedMotion
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardLargeShape
import com.andeva.atelier.mobile.core.designsystem.theme.ChipShape
import com.andeva.atelier.mobile.core.designsystem.theme.HeroShape
import com.andeva.atelier.mobile.core.designsystem.theme.Spacing
import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.core.presentation.DocumentIntents
import com.andeva.atelier.mobile.core.presentation.format
import com.andeva.atelier.mobile.features.diagnostics.domain.DtcCorrelation
import com.andeva.atelier.mobile.features.diagnostics.domain.HealthDiagnosis
import com.andeva.atelier.mobile.features.diagnostics.domain.HealthReport
import com.andeva.atelier.mobile.features.diagnostics.domain.HealthReportDetail
import com.andeva.atelier.mobile.features.diagnostics.domain.PredictiveRisk
import com.andeva.atelier.mobile.features.diagnostics.domain.RecommendedServiceAction
import com.andeva.atelier.mobile.features.diagnostics.domain.SubsystemEvaluation
import com.andeva.atelier.mobile.features.diagnostics.presentation.formatLimaDateTime
import com.andeva.atelier.mobile.features.diagnostics.presentation.shareOrOpenPdf
import java.math.BigDecimal
import java.time.Instant
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val AnalysisDayOptions = listOf(7, 15, 30, 60, 90)

@Composable
fun AiHealthReportScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AiHealthReportViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val chooserTitle = stringResource(R.string.reports_pdf_chooser)

    LaunchedEffect(state.pendingPdfFile, state.pdfActionMode) {
        val file = state.pendingPdfFile ?: return@LaunchedEffect
        val handled = when (state.pdfActionMode) {
            PdfActionMode.OPEN -> DocumentIntents.openPdf(context, file, chooserTitle)
            PdfActionMode.SHARE -> DocumentIntents.sharePdf(context, file, chooserTitle)
        }
        if (handled) {
            viewModel.onPdfHandled()
        } else {
            viewModel.onPdfOpenFailed()
        }
    }

    AiHealthReportContent(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::loadLatest,
        onOpenGenerate = viewModel::openGenerateSheet,
        onDismissGenerate = viewModel::dismissGenerateSheet,
        onSelectDays = viewModel::selectDays,
        onToggleResolvedDtc = viewModel::toggleIncludeResolvedDtcHistory,
        onConfirmGenerate = viewModel::confirmGenerate,
        onDownloadPdf = { viewModel.downloadPdf(PdfActionMode.OPEN) },
        onSharePdf = viewModel::sharePdf,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun AiHealthReportContent(
    state: AiHealthReportUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpenGenerate: () -> Unit,
    onDismissGenerate: () -> Unit,
    onSelectDays: (Int) -> Unit,
    onToggleResolvedDtc: () -> Unit,
    onConfirmGenerate: () -> Unit,
    onDownloadPdf: () -> Unit,
    modifier: Modifier = Modifier,
    onSharePdf: () -> Unit = {}
) = CompositionLocalProvider(LocalAtelierMinTouchTarget provides 56.dp) {
    val haptics = rememberAtelierHaptics()
    val showSkeleton = rememberDelayedLoading(state.isLoading && state.latestSummary == null)
    val vehicleTitle = state.vehicle?.let { "${it.brand} ${it.model}".trim() }.orEmpty()
        .ifBlank { stringResource(R.string.diag_report_title) }
    val hasReport = state.latestSummary != null || state.detail != null

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AtelierFichaPanel(
                onBack = onBack,
                title = vehicleTitle,
                subtitle = state.detail?.let { d ->
                    val date = d.generatedAt?.formatLimaDateTime().orEmpty()
                    d.daysAnalyzed?.let { stringResource(R.string.diag_report_days_analyzed, it, date) } ?: date.ifBlank { null }
                } ?: state.latestSummary?.generatedAt?.formatLimaDateTime(),
                identity = {
                    if (state.displayPlate.isNotBlank()) {
                        AtelierPlate(plate = state.displayPlate, size = PlateSize.L)
                    }
                },
                status = state.detail?.diagnosis?.overallCondition?.let { cond ->
                    {
                        AtelierChip(text = cond, kind = conditionChipKind(cond))
                    }
                }
            )
        },
        bottomBar = {
            if (state.planIncludesAi && (state.canGenerate || (state.canRead && hasReport))) {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        if (!state.isOnline) {
                            AtelierRequiresConnection(
                                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs)
                            )
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            if (state.canRead && hasReport) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                                ) {
                                    AtelierButton(
                                        text = stringResource(R.string.diag_report_open_pdf),
                                        onClick = {
                                            haptics.toggle()
                                            onDownloadPdf()
                                        },
                                        modifier = Modifier.weight(1f),
                                        style = AtelierButtonStyle.Outlined,
                                        enabled = state.isOnline && !state.isDownloadingPdf && !state.isGenerating,
                                        loading = state.isDownloadingPdf && state.pdfActionMode == PdfActionMode.OPEN,
                                        icon = Icons.Outlined.PictureAsPdf
                                    )
                                    AtelierButton(
                                        text = stringResource(R.string.diag_report_share_pdf),
                                        onClick = {
                                            haptics.toggle()
                                            onSharePdf()
                                        },
                                        modifier = Modifier.weight(1f),
                                        style = AtelierButtonStyle.Outlined,
                                        enabled = state.isOnline && !state.isDownloadingPdf && !state.isGenerating,
                                        loading = state.isDownloadingPdf && state.pdfActionMode == PdfActionMode.SHARE,
                                        icon = Icons.Outlined.Share
                                    )
                                }
                            }
                            if (state.canGenerate) {
                                AtelierButton(
                                    text = stringResource(R.string.diag_report_generate),
                                    onClick = onOpenGenerate,
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = state.isOnline && !state.isGenerating,
                                    loading = state.isGenerating,
                                    icon = Icons.Outlined.AutoAwesome
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                !state.planIncludesAi -> AtelierPlanLockedState(
                    text = stringResource(R.string.diag_report_plan_locked),
                    modifier = Modifier.padding(horizontal = Spacing.md)
                )
                !state.canRead && !state.canGenerate -> AtelierNoPermissionState(
                    text = stringResource(R.string.diag_report_no_permission),
                    modifier = Modifier.padding(horizontal = Spacing.md)
                )
                else -> {
                    AtelierRefreshBar(visible = state.isLoading && hasReport)
                    PullToRefreshBox(
                        isRefreshing = state.isLoading && hasReport,
                        onRefresh = onRefresh,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(Spacing.md),
                            verticalArrangement = Arrangement.spacedBy(Spacing.md)
                        ) {
                            state.error?.let { err ->
                                if (hasReport) {
                                    item(key = "error-banner") {
                                        AtelierBanner(message = err.asString(), icon = Icons.Outlined.ErrorOutline)
                                    }
                                }
                            }
                            when {
                                showSkeleton -> item(key = "skeleton") {
                                    ScoreCard(score = 84, summary = "Motor operando en rango normal.", loading = true)
                                }
                                !hasReport && state.error != null && !state.isOnline -> item(key = "offline") {
                                    AtelierOfflineState(onRetry = onRefresh)
                                }
                                !hasReport && state.error != null -> item(key = "error") {
                                    AtelierErrorState(message = state.error.asString(), onRetry = onRefresh)
                                }
                                !hasReport -> item(key = "empty") {
                                    AtelierEmptyState(
                                        text = stringResource(R.string.diag_report_empty),
                                        actionLabel = if (state.canGenerate && state.isOnline) {
                                            stringResource(R.string.diag_report_generate)
                                        } else null,
                                        onAction = if (state.canGenerate && state.isOnline) onOpenGenerate else null
                                    )
                                }
                                else -> {
                                    item(key = "score-summary") {
                                        ScoreCard(
                                            score = state.effectiveScore,
                                            summary = state.effectiveSummary,
                                            loading = false
                                        )
                                    }
                                    val diagnosis = state.detail?.diagnosis
                                    if (diagnosis != null) {
                                        if (diagnosis.subsystemEvaluations.isNotEmpty()) {
                                            item(key = "subsystems-header") {
                                                AtelierSectionHeader(
                                                    title = stringResource(R.string.diag_report_subsystems_section),
                                                    count = diagnosis.subsystemEvaluations.size
                                                )
                                            }
                                            items(diagnosis.subsystemEvaluations) { sub ->
                                                SubsystemCard(sub)
                                            }
                                        }
                                        if (diagnosis.predictiveRisks.isNotEmpty()) {
                                            item(key = "risks-header") {
                                                AtelierSectionHeader(
                                                    title = stringResource(R.string.diag_report_risks_section),
                                                    count = diagnosis.predictiveRisks.size
                                                )
                                            }
                                            items(diagnosis.predictiveRisks) { risk ->
                                                RiskCard(risk)
                                            }
                                        }
                                        if (diagnosis.recommendedActions.isNotEmpty()) {
                                            item(key = "actions-header") {
                                                AtelierSectionHeader(
                                                    title = stringResource(R.string.diag_report_actions_section),
                                                    count = diagnosis.recommendedActions.size
                                                )
                                            }
                                            items(diagnosis.recommendedActions) { action ->
                                                RecommendedActionCard(action)
                                            }
                                        }
                                        if (diagnosis.dtcCorrelations.isNotEmpty()) {
                                            item(key = "corr-header") {
                                                AtelierSectionHeader(
                                                    title = stringResource(R.string.diag_report_correlations_section),
                                                    count = diagnosis.dtcCorrelations.size
                                                )
                                            }
                                            items(diagnosis.dtcCorrelations) { corr ->
                                                CorrelationCard(corr)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.generateSheetOpen) {
        AtelierBottomSheet(
            title = stringResource(R.string.diag_report_generate_sheet_title),
            onDismiss = onDismissGenerate,
            actions = {
                if (!state.isOnline) AtelierRequiresConnection()
                AtelierButton(
                    text = if (state.isGenerating) stringResource(R.string.diag_report_generating)
                    else stringResource(R.string.diag_report_generate),
                    onClick = {
                        haptics.confirm()
                        onConfirmGenerate()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.isOnline && !state.isGenerating,
                    loading = state.isGenerating,
                    icon = Icons.Outlined.AutoAwesome
                )
                AtelierButton(
                    text = stringResource(R.string.common_cancel),
                    onClick = onDismissGenerate,
                    modifier = Modifier.fillMaxWidth(),
                    style = AtelierButtonStyle.Text,
                    enabled = !state.isGenerating
                )
            }
        ) {
            Text(
                text = stringResource(R.string.diag_report_days_label),
                style = MaterialTheme.typography.titleSmall,
                color = AtelierTheme.colors.inkSecondary
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                AnalysisDayOptions.forEach { days ->
                    FilterChip(
                        selected = state.daysToAnalyze == days,
                        onClick = { onSelectDays(days) },
                        enabled = !state.isGenerating,
                        shape = ChipShape,
                        label = { Text(stringResource(R.string.diag_report_days_option, days)) },
                        leadingIcon = if (state.daysToAnalyze == days) {
                            { Icon(Icons.Outlined.Check, contentDescription = null) }
                        } else null
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Checkbox(
                    checked = state.includeResolvedDtcHistory,
                    onCheckedChange = { onToggleResolvedDtc() },
                    enabled = !state.isGenerating
                )
                Text(
                    text = stringResource(R.string.diag_report_include_resolved),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            state.error?.let { AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline) }
        }
    }
}

@Composable
private fun ScoreCard(score: Int?, summary: String?, loading: Boolean) {
    val colors = AtelierTheme.colors
    AtelierSurface(
        modifier = Modifier.fillMaxWidth(),
        tier = SurfaceTier.Elevated,
        shape = HeroShape,
        contentPadding = PaddingValues(Spacing.md)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text(
                text = stringResource(R.string.diag_report_score_label),
                style = MaterialTheme.typography.labelLarge,
                color = colors.inkSecondary
            )
            HealthScoreArc(
                score = score,
                loading = loading,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            if (!summary.isNullOrBlank()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = stringResource(R.string.diag_report_summary_section),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.inkSecondary
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.bone(loading, widthFraction = 0.9f)
                )
            }
        }
    }
}

/**
 * Arco radial de precisión en [Canvas] para el puntaje de salud vehicular (sin neón):
 * trazo limpio [StrokeCap.Round], marcas calibradas en el perímetro, color semántico por
 * umbral (`#3AC530` / `#F0B100` / `#FB2C36`) y cifra tabular Satoshi Black al centro.
 */
@Composable
private fun HealthScoreArc(
    score: Int?,
    loading: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = AtelierTheme.colors
    val reducedMotion = rememberReducedMotion()
    val clampedScore = score?.coerceIn(0, 100)
    val arcColor = when {
        clampedScore == null -> colors.inkSecondary
        clampedScore >= 80 -> colors.okMark
        clampedScore >= 55 -> colors.warnMark
        else -> colors.badMark
    }
    val textColor = when {
        clampedScore == null -> MaterialTheme.colorScheme.onSurface
        clampedScore >= 80 -> colors.ok
        clampedScore >= 55 -> colors.warn
        else -> colors.bad
    }
    val targetFraction = (clampedScore ?: 0) / 100f
    val animatedFraction by animateFloatAsState(
        targetValue = if (loading) 0f else targetFraction,
        animationSpec = if (reducedMotion) tween(0) else AtelierSprings.criticallyDamped(),
        label = "healthScoreArc"
    )
    val cd = clampedScore?.let { stringResource(R.string.pit_report_score_cd, it) }
        ?: stringResource(R.string.diag_report_score_label)

    Box(
        modifier = modifier
            .size(168.dp)
            .semantics(mergeDescendants = true) { contentDescription = cd },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokePx = 12.dp.toPx()
            val inset = strokePx / 2f + 10.dp.toPx()
            val arcDiameter = (size.minDimension - inset * 2f).coerceAtLeast(1f)
            val topLeft = Offset(
                x = (size.width - arcDiameter) / 2f,
                y = (size.height - arcDiameter) / 2f
            )
            val arcSize = Size(arcDiameter, arcDiameter)
            val startAngle = 150f
            val sweepTotal = 240f

            // Marcas de calibración discretas en el perímetro exterior (0, 25, 50, 75, 100 %)
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerTickR = size.minDimension / 2f - 2.dp.toPx()
            val innerTickR = outerTickR - 4.dp.toPx()
            for (step in 0..4) {
                val angleDeg = startAngle + (sweepTotal / 4f) * step
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val cosA = cos(angleRad).toFloat()
                val sinA = sin(angleRad).toFloat()
                drawLine(
                    color = colors.inkSecondary.copy(alpha = 0.30f),
                    start = Offset(center.x + innerTickR * cosA, center.y + innerTickR * sinA),
                    end = Offset(center.x + outerTickR * cosA, center.y + outerTickR * sinA),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Pista hundida del arco
            drawArc(
                color = colors.surfaceRecessed,
                startAngle = startAngle,
                sweepAngle = sweepTotal,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Arco activo con color semántico
            if (animatedFraction > 0f) {
                drawArc(
                    color = arcColor,
                    startAngle = startAngle,
                    sweepAngle = sweepTotal * animatedFraction,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = clampedScore?.toString() ?: "—",
                style = MaterialTheme.typography.displaySmall,
                color = textColor,
                modifier = Modifier.bone(loading, widthFraction = 0.4f)
            )
            if (!loading) {
                Text(
                    text = stringResource(R.string.pit_report_score_max),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.inkSecondary
                )
            }
        }
    }
}

@Composable
private fun SubsystemCard(sub: SubsystemEvaluation) {
    val colors = AtelierTheme.colors
    AtelierSurface(
        modifier = Modifier.fillMaxWidth(),
        tier = SurfaceTier.Card,
        shape = CardLargeShape,
        contentPadding = PaddingValues(Spacing.md)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = sub.subsystemName.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                sub.score?.let {
                    Text(
                        text = "$it/100",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.primaryText
                    )
                }
                sub.status?.let { st ->
                    AtelierChip(text = st, kind = conditionChipKind(st))
                }
            }
            sub.score?.let { sc ->
                val subColor = when {
                    sc >= 80 -> colors.okMark
                    sc >= 55 -> colors.warnMark
                    else -> colors.badMark
                }
                AtelierMetricBar(
                    progress = (sc / 100f).coerceIn(0f, 1f),
                    color = subColor,
                    modifier = Modifier.padding(vertical = Spacing.xs)
                )
            }
            sub.findings?.takeIf { it.isNotBlank() }?.let { findings ->
                Text(
                    text = findings,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkSecondary
                )
            }
        }
    }
}

@Composable
private fun RiskCard(risk: PredictiveRisk) {
    val probPct = risk.probability?.let { (it * 100.0).roundToInt().coerceIn(0, 100) }
    val horizon = risk.timeHorizonDays
    AtelierSurface(
        modifier = Modifier.fillMaxWidth(),
        tier = SurfaceTier.Card,
        shape = CardLargeShape,
        contentPadding = PaddingValues(Spacing.md)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                text = risk.category.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (probPct != null && horizon != null) {
                Text(
                    text = stringResource(R.string.diag_report_risk_detail, probPct, horizon),
                    style = MaterialTheme.typography.labelLarge,
                    color = AtelierTheme.colors.warn
                )
            }
            risk.consequences?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AtelierTheme.colors.inkSecondary
                )
            }
        }
    }
}

@Composable
private fun RecommendedActionCard(action: RecommendedServiceAction) {
    AtelierSurface(
        modifier = Modifier.fillMaxWidth(),
        tier = SurfaceTier.Card,
        shape = CardLargeShape,
        contentPadding = PaddingValues(Spacing.md)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = action.serviceName ?: action.serviceCode.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                action.urgency?.let { urg ->
                    AtelierChip(text = urg, kind = conditionChipKind(urg))
                }
            }
            action.estimatedCost?.let { cost ->
                Text(
                    text = Money(cost.max(BigDecimal.ZERO), Money.PEN).format(),
                    style = MaterialTheme.typography.labelLarge,
                    color = AtelierTheme.colors.primaryText
                )
            }
            action.rationale?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AtelierTheme.colors.inkSecondary
                )
            }
        }
    }
}

@Composable
private fun CorrelationCard(correlation: DtcCorrelation) {
    AtelierListRow(
        title = correlation.dtcCode.orEmpty(),
        subtitle = listOfNotNull(
            correlation.correlatedSensorReading,
            correlation.anomalyExplanation
        ).filter { it.isNotBlank() }.joinToString(" · ")
    )
}

private fun conditionChipKind(raw: String): ChipKind {
    val upper = raw.uppercase()
    return when {
        "CRITICAL" in upper || "HIGH" in upper || "BAD" in upper || "POOR" in upper || "CRITIC" in upper -> ChipKind.Bad
        "MODERATE" in upper || "WARN" in upper || "MEDIUM" in upper || "FAIR" in upper -> ChipKind.Warn
        "LOW" in upper || "GOOD" in upper || "OPTIMAL" in upper || "HEALTHY" in upper || "NORMAL" in upper -> ChipKind.Ok
        else -> ChipKind.Info
    }
}

// region Previews

private val previewReportState = AiHealthReportUiState(
    vehicleId = "v1",
    plate = "BKL482",
    canGenerate = true,
    canRead = true,
    latestSummary = HealthReport(
        reportId = "r1",
        vehicleId = "v1",
        overallHealthScore = 76,
        executiveSummary = "Sistema de refrigeración requiere purga preventiva.",
        totalRisksDetected = 1,
        generatedAt = Instant.parse("2026-10-24T14:00:00Z")
    ),
    detail = HealthReportDetail(
        reportId = "r1",
        vehicleId = "v1",
        daysAnalyzed = 30,
        generatedAt = Instant.parse("2026-10-24T14:00:00Z"),
        diagnosis = HealthDiagnosis(
            healthScore = 76,
            overallCondition = "MODERATE",
            executiveSummary = "Sistema de refrigeración con picos térmicos intermitentes sobre 102 °C.",
            subsystemEvaluations = listOf(
                SubsystemEvaluation("Refrigeración", "WARNING", 64, "Picos de temperatura en ralentí prolongado."),
                SubsystemEvaluation("Carga y batería", "GOOD", 92, "Voltaje estable entre 13.7 V y 14.1 V.")
            ),
            predictiveRisks = listOf(
                PredictiveRisk("Sobrecalentamiento", 0.72, 21, "Posible falla de termostato o bomba de agua.")
            ),
            recommendedActions = listOf(
                RecommendedServiceAction("SRV-COOL", "Mantenimiento de refrigeración", "HIGH", BigDecimal("180.00"), "Inspeccionar termostato y purgar circuito.")
            ),
            dtcCorrelations = listOf(
                DtcCorrelation("P0116", "Temp. motor 104 °C", "Lectura fuera de rango durante ciclo urbano.")
            )
        )
    )
)

@Preview(name = "Informe IA · claro", widthDp = 390, heightDp = 844)
@Composable
private fun ReportPreviewLight() = AtelierWorkshopTheme(darkTheme = false) {
    AiHealthReportContent(previewReportState, {}, {}, {}, {}, {}, {}, {}, {})
}

@Preview(name = "Informe IA · oscuro", widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ReportPreviewDark() = AtelierWorkshopTheme(darkTheme = true) {
    AiHealthReportContent(previewReportState, {}, {}, {}, {}, {}, {}, {}, {})
}

@Preview(name = "Informe IA · bloqueado por plan", widthDp = 390, heightDp = 844)
@Composable
private fun ReportPreviewPlanLocked() = AtelierWorkshopTheme(darkTheme = false) {
    AiHealthReportContent(
        AiHealthReportUiState(vehicleId = "v1", plate = "BKL482", planIncludesAi = false),
        {}, {}, {}, {}, {}, {}, {}, {}
    )
}

// endregion
