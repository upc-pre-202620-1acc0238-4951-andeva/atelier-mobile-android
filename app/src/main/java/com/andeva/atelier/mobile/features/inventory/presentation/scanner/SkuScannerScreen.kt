package com.andeva.atelier.mobile.features.inventory.presentation.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierNoPermissionState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRefreshBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSurface
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.EaseOutQuint
import com.andeva.atelier.mobile.core.designsystem.component.SurfaceTier
import com.andeva.atelier.mobile.core.designsystem.component.rememberAtelierHaptics
import com.andeva.atelier.mobile.core.designsystem.component.rememberReducedMotion
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierBlueSoft
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardLargeShape
import com.andeva.atelier.mobile.core.designsystem.theme.HeroShape
import com.andeva.atelier.mobile.core.designsystem.theme.PanelShape
import com.andeva.atelier.mobile.core.designsystem.theme.Spacing
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

/** Escáner real de SKU con CameraX + ML Kit y entrada manual siempre disponible. */
@Composable
fun SkuScannerScreen(
    onBack: () -> Unit,
    onOpenPart: (partId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SkuScannerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = rememberAtelierHaptics()

    LaunchedEffect(state.foundPartId) {
        val partId = state.foundPartId ?: return@LaunchedEffect
        haptics.confirm()
        viewModel.onPartNavigated()
        onOpenPart(partId)
    }

    LaunchedEffect(state.notFoundSku, state.invalidSku, state.error) {
        if (state.notFoundSku != null || state.invalidSku != null || state.error != null) {
            haptics.reject()
        }
    }

    SkuScannerContent(
        state = state,
        onBack = onBack,
        onBarcodeDetected = viewModel::onBarcodeDetected,
        onManualSkuChange = viewModel::onManualSkuChange,
        onSubmitManualSku = viewModel::onSubmitManualSku,
        onResetScan = viewModel::onResetScan,
        modifier = modifier
    )
}

@Composable
private fun SkuScannerContent(
    state: SkuScannerUiState,
    onBack: () -> Unit,
    onBarcodeDetected: (String) -> Unit,
    onManualSkuChange: (String) -> Unit,
    onSubmitManualSku: () -> Unit,
    onResetScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val inPreview = LocalInspectionMode.current
    var hasCameraPermission by remember {
        mutableStateOf(
            if (inPreview) false
            else ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }
    LaunchedEffect(Unit) {
        if (!inPreview && !hasCameraPermission && state.canRead) {
            askPermission.launch(Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .imePadding()
    ) {
        AtelierTopBar(title = stringResource(R.string.inv_seg2_scanner_title), onBack = onBack)
        if (!state.canRead) {
            AtelierNoPermissionState(
                text = stringResource(R.string.inv_seg2_scanner_no_permission),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            return@Column
        }
        AtelierRefreshBar(visible = state.isSearching)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            if (hasCameraPermission && !inPreview) {
                BarcodeCameraViewport(
                    onBarcodeDetected = onBarcodeDetected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                )
                Text(
                    text = stringResource(R.string.inv_seg2_scanner_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AtelierTheme.colors.inkSecondary
                )
            } else {
                CameraPermissionCard(
                    onRequestPermission = { askPermission.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            state.notFoundSku?.let { sku ->
                AtelierBanner(
                    message = stringResource(R.string.inv_seg2_scanner_not_found, sku),
                    icon = Icons.Outlined.ErrorOutline,
                    kind = BannerKind.Wait
                )
            }
            state.invalidSku?.let { sku ->
                AtelierBanner(
                    message = stringResource(R.string.inv_seg2_scanner_invalid_sku, sku),
                    icon = Icons.Outlined.ErrorOutline,
                    kind = BannerKind.Bad
                )
            }
            state.error?.let { err ->
                AtelierBanner(
                    message = err.asString(),
                    icon = Icons.Outlined.ErrorOutline,
                    kind = BannerKind.Bad
                )
            }

            if (state.notFoundSku != null || state.invalidSku != null || state.error != null) {
                AtelierButton(
                    text = stringResource(R.string.inv_seg2_scanner_retry),
                    onClick = onResetScan,
                    style = AtelierButtonStyle.Outlined,
                    icon = Icons.Outlined.QrCodeScanner,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        AtelierSurface(
            tier = SurfaceTier.Floating,
            shape = HeroShape,
            contentPadding = PaddingValues(Spacing.md),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                AtelierTextField(
                    label = stringResource(R.string.inv_seg2_scanner_manual_label),
                    value = state.manualSku,
                    onValueChange = onManualSkuChange,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = Icons.Outlined.Search,
                    placeholder = stringResource(R.string.inv_seg2_scanner_manual_hint),
                    enabled = !state.isSearching,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(onSearch = { onSubmitManualSku() })
                )
                AtelierButton(
                    text = stringResource(R.string.inv_seg2_scanner_search_btn),
                    onClick = onSubmitManualSku,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.manualSku.isNotBlank(),
                    loading = state.isSearching
                )
            }
        }
    }
}

@Composable
private fun CameraPermissionCard(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    AtelierSurface(
        modifier = modifier.fillMaxWidth(),
        tier = SurfaceTier.Card,
        shape = CardLargeShape,
        contentPadding = PaddingValues(Spacing.md)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                imageVector = Icons.Outlined.CameraAlt,
                contentDescription = null,
                tint = AtelierTheme.colors.primaryText,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = stringResource(R.string.inv_seg2_scanner_permission_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.inv_seg2_scanner_permission_body),
                style = MaterialTheme.typography.bodyMedium,
                color = AtelierTheme.colors.inkSecondary
            )
            AtelierButton(
                text = stringResource(R.string.inv_seg2_scanner_permission_grant),
                onClick = onRequestPermission,
                style = AtelierButtonStyle.Tonal,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun BarcodeCameraViewport(
    onBarcodeDetected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var surfaceRequest by remember { mutableStateOf<SurfaceRequest?>(null) }

    DisposableEffect(lifecycleOwner) {
        val executor = Executors.newSingleThreadExecutor()
        val scanner = BarcodeScanning.getClient()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        var boundProvider: ProcessCameraProvider? = null

        val listener = Runnable {
            val provider = runCatching { cameraProviderFuture.get() }.getOrNull() ?: return@Runnable
            boundProvider = provider
            val preview = CameraPreview.Builder().build().apply {
                setSurfaceProvider { request -> surfaceRequest = request }
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .apply {
                    setAnalyzer(executor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage == null) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                val raw = barcodes.firstNotNullOfOrNull { barcode ->
                                    barcode.rawValue?.trim()?.takeIf { it.isNotEmpty() }
                                }
                                if (raw != null) onBarcodeDetected(raw)
                            }
                            .addOnCompleteListener { imageProxy.close() }
                    }
                }
            runCatching {
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            }
        }
        cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))

        onDispose {
            boundProvider?.unbindAll()
            scanner.close()
            executor.shutdown()
        }
    }

    Box(
        modifier = modifier
            .clip(PanelShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        surfaceRequest?.let { req ->
            CameraXViewfinder(
                surfaceRequest = req,
                modifier = Modifier.fillMaxSize()
            )
        }
        PrecisionReticleOverlay(modifier = Modifier.matchParentSize())
    }
}

/**
 * Retícula de precisión en [Canvas] para lectura de códigos de barras/QR: velo oscuro fuera de la zona
 * de lectura, esquinas redondeadas en `#69B1FF`/`#FFFFFF` y haz de escaneo animado (desactivado si
 * `rememberReducedMotion()` está activo).
 */
@Composable
private fun PrecisionReticleOverlay(modifier: Modifier = Modifier) {
    val reducedMotion = rememberReducedMotion()
    val scanProgress = if (reducedMotion) {
        0.5f
    } else {
        val transition = rememberInfiniteTransition(label = "skuScanReticle")
        val animated by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1800, easing = EaseOutQuint),
                repeatMode = RepeatMode.Reverse
            ),
            label = "skuScanLine"
        )
        animated
    }

    Canvas(modifier = modifier) {
        val reticleW = size.width * 0.78f
        val reticleH = minOf(size.height * 0.52f, 136.dp.toPx())
        val left = (size.width - reticleW) / 2f
        val top = (size.height - reticleH) / 2f
        val right = left + reticleW
        val bottom = top + reticleH
        val cornerRadius = 14.dp.toPx()
        val bracketArm = 22.dp.toPx()

        val cutoutPath = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(left, top, right, bottom),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                )
            )
        }

        // Velo oscuro sutil fuera del área de lectura
        clipPath(path = cutoutPath, clipOp = ClipOp.Difference) {
            drawRect(color = Color.Black.copy(alpha = 0.52f))
        }

        // Borde guía sutil dentro del visor
        drawRoundRect(
            color = Color.White.copy(alpha = 0.22f),
            topLeft = Offset(left, top),
            size = Size(reticleW, reticleH),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            style = Stroke(width = 1.dp.toPx())
        )

        // Esquinas redondeadas de precisión en #69B1FF + acento blanco
        val bracketStroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        val innerStroke = Stroke(width = 1.25.dp.toPx(), cap = StrokeCap.Round)
        val cornersPath = Path().apply {
            // Superior izquierda
            moveTo(left, top + bracketArm)
            lineTo(left, top + cornerRadius)
            quadraticTo(left, top, left + cornerRadius, top)
            lineTo(left + bracketArm, top)
            // Superior derecha
            moveTo(right - bracketArm, top)
            lineTo(right - cornerRadius, top)
            quadraticTo(right, top, right, top + cornerRadius)
            lineTo(right, top + bracketArm)
            // Inferior derecha
            moveTo(right, bottom - bracketArm)
            lineTo(right, bottom - cornerRadius)
            quadraticTo(right, bottom, right - cornerRadius, bottom)
            lineTo(right - bracketArm, bottom)
            // Inferior izquierda
            moveTo(left + bracketArm, bottom)
            lineTo(left + cornerRadius, bottom)
            quadraticTo(left, bottom, left, bottom - cornerRadius)
            lineTo(left, bottom - bracketArm)
        }
        drawPath(path = cornersPath, color = AtelierBlueSoft, style = bracketStroke)
        drawPath(path = cornersPath, color = Color.White.copy(alpha = 0.65f), style = innerStroke)

        // Línea de escaneo suave animada
        val lineInset = 10.dp.toPx()
        val scanY = (top + lineInset) + (reticleH - lineInset * 2f) * scanProgress
        val glowHalfHeight = 10.dp.toPx()
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, AtelierBlueSoft.copy(alpha = 0.24f), Color.Transparent),
                startY = scanY - glowHalfHeight,
                endY = scanY + glowHalfHeight
            ),
            topLeft = Offset(left + lineInset, scanY - glowHalfHeight),
            size = Size(reticleW - lineInset * 2f, glowHalfHeight * 2f)
        )
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    AtelierBlueSoft,
                    Color.White,
                    AtelierBlueSoft,
                    Color.Transparent
                ),
                startX = left + lineInset,
                endX = right - lineInset
            ),
            start = Offset(left + lineInset, scanY),
            end = Offset(right - lineInset, scanY),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Preview(name = "Retícula escáner · claro", widthDp = 390, heightDp = 300)
@Composable
private fun PrecisionReticlePreviewLight() = AtelierWorkshopTheme(darkTheme = false) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.onBackground)
    ) {
        PrecisionReticleOverlay(modifier = Modifier.fillMaxSize())
    }
}

@Preview(name = "Retícula escáner · oscuro", widthDp = 390, heightDp = 300, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PrecisionReticlePreviewDark() = AtelierWorkshopTheme(darkTheme = true) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PrecisionReticleOverlay(modifier = Modifier.fillMaxSize())
    }
}

@Preview(name = "Escáner SKU · claro", widthDp = 390, heightDp = 844)
@Composable
private fun SkuScannerPreviewLight() = AtelierWorkshopTheme(darkTheme = false) {
    SkuScannerContent(
        state = SkuScannerUiState(manualSku = "BRK-001"),
        onBack = {},
        onBarcodeDetected = {},
        onManualSkuChange = {},
        onSubmitManualSku = {},
        onResetScan = {}
    )
}

@Preview(name = "Escáner SKU · oscuro", widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SkuScannerPreviewDark() = AtelierWorkshopTheme(darkTheme = true) {
    SkuScannerContent(
        state = SkuScannerUiState(notFoundSku = "XXX-999"),
        onBack = {},
        onBarcodeDetected = {},
        onManualSkuChange = {},
        onSubmitManualSku = {},
        onResetScan = {}
    )
}
