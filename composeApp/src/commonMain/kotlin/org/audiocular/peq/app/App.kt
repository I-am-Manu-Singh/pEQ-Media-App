@file:OptIn(ExperimentalMaterial3Api::class)

package org.audiocular.peq.app
import kotlin.IllegalStateException
import kotlin.collections.listOf
import kotlin.collections.forEach
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.alpha
import org.audiocular.peq.app.ui.components.*
import peq_app.composeapp.generated.resources.*
import org.audiocular.peq.app.ui.screens.*
import org.audiocular.peq.app.ui.theme.*
import org.audiocular.peq.app.ui.viewmodel.AppViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import kotlinx.coroutines.delay
import org.audiocular.peq.app.data.MediaType
import androidx.compose.ui.zIndex
import org.audiocular.peq.app.data.Device
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.Image
import org.jetbrains.compose.resources.painterResource
import peq_app.composeapp.generated.resources.Res
import peq_app.composeapp.generated.resources.app_icon
import androidx.compose.ui.text.TextStyle
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
val LocalAppViewModel = staticCompositionLocalOf<AppViewModel> { throw IllegalStateException("No AppViewModel provided") }
@Composable
fun App() {

    val viewModel =
        LocalAppViewModel.current

    val theme by viewModel.selectedTheme

    val showSplash =
        !viewModel.splashCompleted

LaunchedEffect(Unit) {

    if (!viewModel.splashCompleted) {

        delay(2400)

        viewModel.completeSplash()
    }
}

    pEQTheme(theme = theme) {

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
        ) {

            AnimatedContent(
    targetState = showSplash,

    transitionSpec = {

        fadeIn(
            animationSpec =
                tween(700)
        ).togetherWith(

            fadeOut(
                animationSpec =
                    tween(500)
            )
        )
    },

    label = "app_root_transition"
) { splash ->

    if (splash) {

        pEQSplashScreen()

    } else {

        MainScreen(viewModel)
    }
}

// 🧠 NEURAL FOCUS OVERLAY (Lock Screen / Floating Lyrics)
AnimatedVisibility(
    visible = viewModel.isLockScreenMode.value && !viewModel.isInPipMode.value,
    enter = fadeIn(tween(400)) + slideInVertically { it },
    exit = fadeOut(tween(400)) + slideOutVertically { it }
) {
    NeuralLockScreenOverlay(viewModel)
}
        }
    }
}
@Composable
fun pEQSplashScreen() {
    val infinite = rememberInfiniteTransition(label = "pEQ_splash")

    val progress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    val glow by infinite.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Use DynamicBackground instead of hardcoded gradient
        DynamicBackground(  baseColor = MaterialTheme.colorScheme.primary,
            transientImpact = 0.2f)

        // =========================
        // SOFT AMBIENT LIGHT (Yellow Glow)
        // =========================
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-120).dp)
                .size(420.dp)
                .graphicsLayer {
                    scaleX = glow
                    scaleY = glow
                }
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFD600).copy(alpha = 0.15f), // Yellow accent
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
                .blur(80.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // =========================
            // LOGO CONTAINER
            // =========================
            Surface(
                modifier = Modifier.size(124.dp),
                shape = RoundedCornerShape(34.dp),
                color = Color.White.copy(alpha = 0.06f),
                border = BorderStroke(
                    1.dp,
                    Color(0xFFFFD600).copy(alpha = 0.2f) // Yellow border tint
                ),
                tonalElevation = 10.dp,
                shadowElevation = 20.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.08f),
                                    Color.Transparent,
                                    Color(0xFFFFD600).copy(alpha = 0.05f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(Res.drawable.app_icon),
                        contentDescription = null,
                        modifier = Modifier.clip(RoundedCornerShape(22.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(Modifier.height(34.dp))

            // =========================
            // TITLE
            // =========================
            Text(
                text = "pEQ",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Media Player",
                color = Color.White.copy(alpha = 0.58f),
                fontSize = 13.sp,
                letterSpacing = 1.sp
            )

            Spacer(Modifier.height(42.dp))

            // =========================
            // PROGRESS BAR (Yellow Gradient)
            // =========================
            Box(
                modifier = Modifier
                    .width(220.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFFD600), // Vibrant Yellow
                                    Color(0xFFFFA000)  // Amber/Orange
                                )
                            )
                        )
                )
            }

            Spacer(Modifier.height(18.dp))

           var loadingText by remember {
    mutableStateOf("Loading audio engine")
}

LaunchedEffect(Unit) {
    delay(1000)
    loadingText = "Loading video engine"
}

Text(
    text = loadingText,
    color = Color.White.copy(alpha = 0.42f),
    fontSize = 11.sp,
    letterSpacing = 0.5.sp
)
        }

        // =========================
        // VERSION
        // =========================
        Text(
            text = "v1.0",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 26.dp),
            color = Color.White.copy(alpha = 0.18f),
            fontSize = 10.sp
        )
    }
}

@Composable
private fun NeuralLockScreenOverlay(viewModel: AppViewModel) {
    val item = viewModel.currentMediaItem.value ?: return
    val currentLyric = viewModel.syncedLyrics.getOrNull(viewModel.currentLyricsLine.value)
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.92f)).clickable { /* Consume clicks */ }) {
        DynamicBackground(baseColor = MaterialTheme.colorScheme.primary, transientImpact = 0.15f)
        
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp).systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "NEURAL FOCUS MODE",
                style = TextStyle(color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            )
            
            Spacer(Modifier.height(48.dp))
            
            // Prominent Lyrics
            AnimatedContent(
                targetState = currentLyric,
                transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) }
            ) { lyric ->
                Text(
                    text = lyric?.text ?: "...",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        lineHeight = 42.sp
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
            }
            
            Spacer(Modifier.height(24.dp))
            
            Text(
                text = item.title,
                style = TextStyle(color = Color.White.copy(alpha = 0.6f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            )
            
            Spacer(Modifier.weight(1f))
            
            // Minimal Controls
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                IconButton(onClick = { viewModel.previousTrack() }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
                FilledIconButton(onClick = { viewModel.togglePlayback() }, modifier = Modifier.size(82.dp)) {
                    Icon(if (viewModel.isPlaying.value) Icons.Default.Pause else Icons.Default.PlayArrow, null, modifier = Modifier.size(42.dp))
                }
                IconButton(onClick = { viewModel.nextTrack() }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }
            
            Spacer(Modifier.height(48.dp))
            
            Button(
                onClick = { viewModel.isLockScreenMode.value = false },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f))
            ) {
                Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("EXIT FOCUS")
            }
        }
    }
}

@Composable
fun MainScreen(
    viewModel: AppViewModel
) {

    val snackbarHostState =
        remember { SnackbarHostState() }

    val currentTab by viewModel.currentTab

    val isVideoFullScreen by
    viewModel.isVideoFullScreen

    val isPlayerActive =
        currentTab == Tab.Player

    // =========================
    // SYSTEM BARS
    // =========================

    // LaunchedEffect(isVideoFullScreen) {

    //     setSystemBarsVisibility(
    //         visible = !isVideoFullScreen
    //     )
    // }

    // =========================
    // TOASTS
    // =========================

    LaunchedEffect(
        viewModel.toastMessage.value
    ) {

        viewModel.toastMessage.value?.let { msg ->

            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )

            viewModel.toastMessage.value = null
        }
    }

    // =========================
    // INITIALIZATION
    // =========================

    LaunchedEffect(Unit) {
        viewModel.start()
        viewModel.setMasterVolume(getSystemVolume())
        
        // 🔥 FIRST INSTALL ONBOARDING
        val folders = viewModel.persistenceManager.loadScanFolders()
        if (folders.isEmpty()) {
            delay(400) // Let the app breathe
            viewModel.showFolderManager.value = true
        }
    }

    // =========================
    // GLOBAL BACK
    // =========================

    pEQBackHandler(

        enabled =
            currentTab != Tab.Dashboard &&
                    currentTab != Tab.Player &&
                    !isVideoFullScreen

    ) {

        viewModel.currentTab.value =
            Tab.Dashboard
    }

    // =========================
    // ROOT
    // =========================

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {

        val isWideScreen =
            maxWidth > 840.dp || maxWidth > maxHeight

        val isLandscape = maxWidth > maxHeight

        Scaffold(

            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),

            containerColor = Color.Black,

            contentWindowInsets =

                if (isVideoFullScreen)
                    WindowInsets(0.dp)
                else
                    WindowInsets.safeDrawing,

            topBar = {

                if (
                    !isPlayerActive &&
                    !isVideoFullScreen &&
                    !isWideScreen
                ) {

                    Column(
                        modifier = Modifier
                            .statusBarsPadding()
                    ) {

                        HeaderSection(viewModel) {

                            viewModel.showSettings.value =
                                true
                        }
                    }
                }
            },

            snackbarHost = {

                SnackbarHost(
                    snackbarHostState
                )
            },

            bottomBar = {

                if (
                    !isWideScreen &&
                    !viewModel.showSettings.value &&
                    !isPlayerActive &&
                    !isVideoFullScreen
                ) {

                    BottomNavigationBar(
                        current = currentTab,

                        onSelect = { tab: Tab ->

                            viewModel.currentTab.value = tab
                        }
                    )
                }
            }

        ) { paddingValues ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {

                // ALWAYS RENDER BACKGROUND FIRST
                DynamicBackground(
                    baseColor = MaterialTheme.colorScheme.primary,
                    transientImpact = 0.2f
                )


                if (isVideoFullScreen) {

                    val currentItem =
                        viewModel.currentMediaItem.value

                    if (currentItem != null) {

                        key(currentItem.path) {

                            PlayerScreen(
                                viewModel = viewModel,
                                item = currentItem
                            )
                        }
                    }

                } else {

                    // =========================
                    // NORMAL APP LAYOUT
                    // =========================

                    if (!viewModel.showSettings.value) {

                        Row(
                            modifier = Modifier.fillMaxSize()
                        ) {

                            // =========================
                            // TABLET / LANDSCAPE NAV
                            // =========================

                            if (
                                isWideScreen &&
                                !isPlayerActive
                            ) {

                                Column(

                                    modifier = Modifier
                                        .width(240.dp)
                                        .fillMaxHeight()
                                        .statusBarsPadding()
                                        .background(
                                            MaterialTheme.colorScheme.surface
                                        )
                                        .padding(16.dp),

                                    verticalArrangement =
                                        Arrangement.spacedBy(12.dp)
                                ) {

                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                         Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                            modifier = Modifier.size(42.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Image(
                                                    painter = painterResource(Res.drawable.app_icon),
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(), 
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                        }
                                        
                                        Spacer(Modifier.width(12.dp))

                                        Column {
                                            Text(
                                                text = "pEQ",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 20.sp,
                                                letterSpacing = 1.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "Workstation",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 8.sp,
                                                letterSpacing = 0.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        }
                                    }

                                    Spacer(
                                        Modifier.height(24.dp)
                                    )

                                    NavigationRailSection(
                                        viewModel
                                    )
                                }
                            }

                            // =========================
                            // MAIN CONTENT
                            // =========================

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .then(

                                        if (
                                            !isWideScreen &&
                                            !isPlayerActive
                                        ) {

                                            Modifier.padding(
                                                paddingValues
                                            )

                                        } else {

                                            Modifier
                                        }
                                    )
                            ) {

                                AnimatedContent(
                                    targetState = currentTab,

                                    transitionSpec = {

                                        fadeIn(
                                            tween(220)
                                        ).togetherWith(

                                            fadeOut(
                                                tween(160)
                                            )
                                        )
                                    },

                                    label = "main_content"
                                ) { tab ->

                                    when (tab) {

                                        Tab.Dashboard -> {

                                            pEQWorkstationDashboard(
                                                viewModel
                                            )
                                        }

                                        Tab.Tuning -> {
                                            TuningScreen(viewModel)
                                        }

                                        Tab.Player -> {
                                            val currentItem =
                                                viewModel.currentMediaItem.value

                                            if (currentItem != null) {

                                                key(currentItem.path) {

                                                    PlayerScreen(
                                                        viewModel =
                                                            viewModel,

                                                        item =
                                                            currentItem
                                                    )
                                                }

                                            } else {

                                                EmptyPlayerState(onBack = { viewModel.currentTab.value = Tab.Dashboard })
                                            }
                                        }

                                        Tab.Hardware -> {
                                            HardwareScreen(viewModel)
                                        }

                                        Tab.Mixer -> {
                                            pEQMixerScreen(viewModel)
                                        }

                                        Tab.Learn -> {
                                            pEQLearnScreen(viewModel)
                                        }


                                    }
                                }
                            }
                        }
                    }
                }
                // =========================
                // FOLDER MANAGER
                // =========================

                if (viewModel.showFolderManager.value) {

                    PEQFolderManagerDialog {

                        viewModel.showFolderManager.value =
                            false
                    }
                }

                // =========================
                // DEVICE SWITCHER
                // =========================

                if (viewModel.showDeviceSwitcher.value) {

                    DeviceSwitcherDialog(
                        viewModel
                    )
                }
            }
        }
    }

}

@Composable
fun BottomNavigationBar(
    current: Tab,
    onSelect: (Tab) -> Unit
) {
    val tabs = listOf(
        Tab.Dashboard,
        Tab.Tuning,
        Tab.Mixer,
        Tab.Hardware,
        Tab.Learn
    )

    NavigationBar(

        containerColor =
            MaterialTheme.colorScheme.surface,

        tonalElevation = 2.dp
    ) {

        for (tab in tabs) {

            val isSelected =
                current == tab

            NavigationBarItem(

                selected = isSelected,

                onClick = {

                    onSelect(tab)
                },

                icon = {

                    Icon(
                        imageVector = tab.icon,

                        contentDescription =
                            tab.label,

                        modifier =
                            Modifier.size(20.dp)
                    )
                },

                label = {

                    Text(
                        text = tab.label,

                        fontSize = 9.sp,

                        fontWeight =
                            if (isSelected)
                                FontWeight.SemiBold
                            else
                                FontWeight.Normal,

                        maxLines = 1
                    )
                },

                alwaysShowLabel = false,

                colors =
                    NavigationBarItemDefaults.colors(

                        selectedIconColor =
                            MaterialTheme.colorScheme.primary,

                        selectedTextColor =
                            MaterialTheme.colorScheme.primary,

                        indicatorColor =
                            MaterialTheme.colorScheme.primary
                                .copy(alpha = 0.12f)
                    )
            )
        }
    }
}
@Composable
fun Modifier.auroraBorder(): Modifier {

    val infinite = rememberInfiniteTransition()

    val alpha by infinite.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000),
            repeatMode = RepeatMode.Reverse
        )
    )

    val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f

    return this.border(
        BorderStroke(
            if (isLight) 0.5.dp else 1.dp,
            Brush.linearGradient(
                listOf(
                    MaterialTheme.colorScheme.primary.copy(if (isLight) alpha / 2 else alpha),
                    Color.Transparent,
                    MaterialTheme.colorScheme.secondary.copy(if (isLight) alpha / 2 else alpha)
                )
            )
        ),
        RoundedCornerShape(22.dp)
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderSection(
    viewModel: AppViewModel,
    onSettingsClick: () -> Unit
) {

    var showAudioPanel by remember {
        mutableStateOf(false)
    }

    var showDeviceSheet by remember {
        mutableStateOf(false)
    }

    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )

    val nowPlaying =
        viewModel.currentMediaItem.value

    BoxWithConstraints {

        val isLandscape =
            maxWidth > maxHeight

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal =
                        if (isLandscape) 10.dp
                        else 8.dp,

                    vertical =
                        if (isLandscape) 4.dp
                        else 8.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically,

            horizontalArrangement =
                Arrangement.spacedBy(
                    if (isLandscape) 6.dp
                    else 10.dp
                )
        ) {
            IconButton(
                onClick = { viewModel.showMiniPlayer.value = !viewModel.showMiniPlayer.value },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AppShortcut,
                    contentDescription = "Widget Command",
                    tint = if (viewModel.showMiniPlayer.value) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }

            // =========================
            // PLAYER CARD
            // =========================

            Surface(
                onClick = {

                    if (nowPlaying != null) {

                        viewModel.currentTab.value =
                            Tab.Player

                        if (
                            nowPlaying.type ==
                            MediaType.VIDEO
                        ) {

                            viewModel.isVideoFullScreen.value =
                                true
                        }
                    }
                },

                modifier = Modifier
                    .weight(1f)
                    .heightIn(
                        min =
                            if (isLandscape)
                                54.dp
                            else
                                64.dp,

                        max =
                            if (isLandscape)
                                62.dp
                            else
                                74.dp
                    )
                    .auroraBorder(),

                shape = RoundedCornerShape(
                    if (isLandscape)
                        22.dp
                    else
                        28.dp
                ),

                color =
                    MaterialTheme
                        .colorScheme
                        .surfaceContainerHighest
                        .copy(alpha = 0.45f)
            ) {

                if (nowPlaying != null) {

                    Column {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal =
                                        if (isLandscape)
                                            10.dp
                                        else
                                            12.dp,

                                    vertical =
                                        if (isLandscape)
                                            6.dp
                                        else
                                            10.dp
                                ),

                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            // =========================
                            // THUMBNAIL
                            // =========================

                            Box(
                                modifier = Modifier
                                    .size(
                                        if (isLandscape)
                                            38.dp
                                        else
                                            46.dp
                                    )
                                    .clip(
                                        RoundedCornerShape(
                                            if (isLandscape)
                                                10.dp
                                            else
                                                14.dp
                                        )
                                    )
                                    .background(Color.Black)
                            ) {

                                pEQImage(
                                    data =
                                        nowPlaying.thumbnail,

                                    modifier = Modifier
                                        .fillMaxSize()
                                        .alpha(0.75f),

                                    contentScale =
                                        ContentScale.Crop,
                                    
                                    mediaType = nowPlaying.type
                                )
                            }

                            Spacer(
                                Modifier.width(
                                    if (isLandscape)
                                        8.dp
                                    else
                                        10.dp
                                )
                            )

                            // =========================
                            // INFO
                            // =========================

                            Column(
                                modifier =
                                    Modifier.weight(1f),

                                verticalArrangement =
                                    Arrangement.spacedBy(1.dp)
                            ) {

                                Text(
                                    text =
                                        nowPlaying.title,

                                    fontSize =
                                        if (isLandscape)
                                            10.sp
                                        else
                                            11.sp,

                                    lineHeight =
                                        if (isLandscape)
                                            12.sp
                                        else
                                            14.sp,

                                    fontWeight =
                                        FontWeight.Bold,

                                    maxLines = 1,

                                    overflow =
                                        TextOverflow.Ellipsis
                                )

                                Text(
                                    text =
                                        nowPlaying.subtitle,

                                    fontSize =
                                        if (isLandscape)
                                            8.sp
                                        else
                                            9.sp,

                                    lineHeight =
                                        if (isLandscape)
                                            9.sp
                                        else
                                            10.sp,

                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .primary,

                                    maxLines = 1,

                                    overflow =
                                        TextOverflow.Ellipsis
                                )
                            }

                            Spacer(
                                Modifier.width(
                                    if (isLandscape)
                                        2.dp
                                    else
                                        6.dp
                                )
                            )

                            // =========================
                            // CONTROLS
                            // =========================

                            Row(
                                verticalAlignment =
                                    Alignment.CenterVertically,

                                horizontalArrangement =
                                    Arrangement.spacedBy(
                                        if (isLandscape)
                                            1.dp
                                        else
                                            3.dp
                                    )
                            ) {

                                FilledTonalIconButton(
                                    onClick = {
                                        viewModel.previousTrack()
                                    },

                                    modifier = Modifier.size(
                                        if (isLandscape)
                                            20.dp
                                        else
                                            24.dp
                                    )
                                ) {

                                    Icon(
                                        imageVector =
                                            Icons.Default.SkipPrevious,

                                        contentDescription = null,

                                        modifier = Modifier.size(
                                            if (isLandscape)
                                                13.dp
                                            else
                                                16.dp
                                        )
                                    )
                                }

                                FilledIconButton(
                                    onClick = {
                                        viewModel.togglePlayback()
                                    },

                                    modifier = Modifier.size(
                                        if (isLandscape)
                                            28.dp
                                        else
                                            34.dp
                                    )
                                ) {

                                    Icon(
                                        imageVector =
                                            if (viewModel.isPlaying.value)
                                                Icons.Default.Pause
                                            else
                                                Icons.Default.PlayArrow,

                                        contentDescription = null,

                                        modifier = Modifier.size(
                                            if (isLandscape)
                                                15.dp
                                            else
                                                18.dp
                                        )
                                    )
                                }

                                FilledTonalIconButton(
                                    onClick = {
                                        viewModel.nextTrack()
                                    },

                                    modifier = Modifier.size(
                                        if (isLandscape)
                                            20.dp
                                        else
                                            24.dp
                                    )
                                ) {

                                    Icon(
                                        imageVector =
                                            Icons.Default.SkipNext,

                                        contentDescription = null,

                                        modifier = Modifier.size(
                                            if (isLandscape)
                                                13.dp
                                            else
                                                16.dp
                                        )
                                    )
                                }
                            }
                        }

                        val progress =
                            (
                                    viewModel.currentPosition.value
                                        .toFloat() /

                                            viewModel.currentDuration.value
                                                .coerceAtLeast(1L)
                                                .toFloat()

                                    ).coerceIn(0f, 1f)

                        LinearProgressIndicator(
                            progress = { progress },

                            modifier = Modifier
                                .fillMaxWidth()
                                .height(
                                    if (isLandscape)
                                        2.dp
                                    else
                                        3.dp
                                ),

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .primary,

                            trackColor =
                                MaterialTheme
                                    .colorScheme
                                    .primary
                                    .copy(alpha = 0.10f)
                        )
                    }

                } else {

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                horizontal =
                                    if (isLandscape)
                                        10.dp
                                    else
                                        14.dp
                            ),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Surface(
    shape = RoundedCornerShape(if (isLandscape) 10.dp else 14.dp),
    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
    modifier = Modifier.size(if (isLandscape) 38.dp else 46.dp)
) {
    Box(contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(Res.drawable.app_icon),
            contentDescription = null,
            // Change size to fillMaxSize to stretch to the Surface boundaries
            modifier = Modifier.fillMaxSize(), 
            contentScale = ContentScale.Crop
        )
    }
}


                        Spacer(
                            Modifier.width(
                                if (isLandscape)
                                    8.dp
                                else
                                    12.dp
                            )
                        )

                        Column {

                            Text(
                                text = "pEQ Media Player",

                                fontSize =
                                    if (isLandscape)
                                        11.sp
                                    else
                                        13.sp,

                                fontWeight =
                                    FontWeight.Black,

                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .primary
                            )

                            Text(
                                text =
                                    "No active playback",

                                fontSize =
                                    if (isLandscape)
                                        8.sp
                                    else
                                        9.sp,

                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // =========================
            // RIGHT CONTROLS
            // =========================

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(
                        if (isLandscape)
                            6.dp
                        else
                            10.dp
                    ),

                verticalAlignment =
                    Alignment.Top,

                modifier = Modifier.padding(end = 2.dp)
            ) {

                // =========================
                // DEVICE
                // =========================

                Column(
                    horizontalAlignment =
                        Alignment.CenterHorizontally,

                    verticalArrangement =
                        Arrangement.spacedBy(3.dp),

                    modifier = Modifier.widthIn(
                        min =
                            if (isLandscape)
                                36.dp
                            else
                                44.dp,

                        max =
                            if (isLandscape)
                                48.dp
                            else
                                58.dp
                    )
                ) {

                    FilledTonalIconButton(
                        onClick = {
                            showDeviceSheet = true
                        },

                        modifier = Modifier.size(
                            if (isLandscape)
                                30.dp
                            else
                                36.dp
                        )
                    ) {

                        Icon(
                            imageVector =
                                viewModel.currentDevice.value.icon,

                            contentDescription = null,

                            tint =
                                viewModel.currentDevice.value.color,

                            modifier = Modifier.size(
                                if (isLandscape)
                                    15.dp
                                else
                                    18.dp
                            )
                        )
                    }

                    Text(
                        text =
                            viewModel.currentDevice.value.deviceName,

                        fontSize =
                            if (isLandscape)
                                6.sp
                            else
                                7.sp,

                        lineHeight =
                            if (isLandscape)
                                7.sp
                            else
                                8.sp,

                        fontWeight =
                            FontWeight.Bold,

                        textAlign =
                            TextAlign.Center,

                        maxLines = 2,

                        overflow =
                            TextOverflow.Ellipsis,

                        modifier =
                            Modifier.fillMaxWidth()
                    )
                }
// =========================
// VOLUME
// =========================

                Box {

    Column(
        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.spacedBy(2.dp)
    ) {

        val isMuted =
            viewModel.masterVolume.value <= 0f

        FilledTonalIconButton(
            onClick = {
                showAudioPanel = !showAudioPanel
            },

            modifier = Modifier.size(
                if (isLandscape)
                    30.dp
                else
                    36.dp
            ),

            colors =
    IconButtonDefaults.filledTonalIconButtonColors(

        containerColor =
            if (isMuted)
                MaterialTheme.colorScheme.errorContainer
            else
                MaterialTheme.colorScheme.secondaryContainer,

        contentColor =
            if (isMuted)
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.onSecondaryContainer
    )
        ) {

            Icon(
                imageVector =
                    when {

                        viewModel.masterVolume.value > 0.5f ->
                            Icons.AutoMirrored.Filled.VolumeUp

                        viewModel.masterVolume.value > 0f ->
                            Icons.AutoMirrored.Filled.VolumeDown

                        else ->
                            Icons.AutoMirrored.Filled.VolumeOff
                    },

                contentDescription = null,

                modifier = Modifier.size(
                    if (isLandscape)
                        15.dp
                    else
                        18.dp
                )
            )
        }

        Text(
            text =
                "${(viewModel.masterVolume.value * 100).toInt()}%",

            modifier = Modifier.padding(4.dp),

            fontSize =
                if (isLandscape)
                    9.sp
                else
                    9.sp,

            lineHeight =
                if (isLandscape)
                    5.sp
                else
                    6.sp,

            fontWeight = FontWeight.Bold,

            color =
                if (isMuted)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary
        )
    }

    // =========================
    // FLOATING PANEL
    // =========================

    Popup(
        alignment = Alignment.TopEnd,

        offset = IntOffset(
            x =
                if (isLandscape)
                    -120
                else
                    -160,

            y =
                if (isLandscape)
                    55
                else
                    70
        ),

        onDismissRequest = {
            showAudioPanel = false
        }
    ) {

        androidx.compose.animation.AnimatedVisibility(
            visible = showAudioPanel,

            enter =
                fadeIn() + scaleIn(
                    initialScale = 0.92f
                ),

            exit =
                fadeOut() + scaleOut(
                    targetScale = 0.92f
                )
        ) {

            Surface(
                shape = RoundedCornerShape(26.dp),

                tonalElevation = 20.dp,

                shadowElevation = 30.dp,

                color =
                    MaterialTheme
                        .colorScheme
                        .surfaceContainerHighest
                        .copy(alpha = 0.96f),

                border = BorderStroke(
                    1.dp,
                    MaterialTheme
                        .colorScheme
                        .outline
                        .copy(alpha = 0.08f)
                )
            ) {

                VolumeControlSlider(
                    viewModel = viewModel,

                    modifier = Modifier
                        .width(
                            if (isLandscape)
                                200.dp
                            else
                                250.dp
                        )
                        .padding(10.dp)
                )
                // Global Browser Overlay
                if (viewModel.showBrowser.value) {
                    org.audiocular.peq.app.ui.components.pEQBrowser(
                        url = viewModel.browserUrl.value,
                        onClose = { viewModel.showBrowser.value = false },
                        onStreamCaptured = { viewModel.handleCapturedStream(it) }
                    )
                }
            }
        }
    }
}
            }
        }

        // =========================
        // DEVICE SHEET
        // =========================

        if (showDeviceSheet) {

            ModalBottomSheet(
                onDismissRequest = {

                    showDeviceSheet = false

                },

                sheetState = sheetState
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp,0.dp,20.dp,8.dp),

                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    Text(
                        text = pEQStrings.SELECT_DEVICE,

                        fontSize = 14.sp,

                        fontWeight = FontWeight.Bold
                    )

                    Device.values().forEach { dev ->

                        val selected =
                            dev == viewModel.currentDevice.value

                        val isConnected =
                            viewModel.connectedDevices.contains(dev)

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(
                                    if (isConnected)
                                        1f
                                    else
                                        0.45f
                                )
                                .clickable(
                                    enabled = isConnected
                                ) {

                                    viewModel.updateDevice(dev)

                                    showDeviceSheet = false
                                },

                            shape = RoundedCornerShape(18.dp),

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .surfaceContainerHighest
                                    .copy(alpha = 0.45f),

                            border = BorderStroke(
                                1.dp,

                                if (selected)
                                    MaterialTheme
                                        .colorScheme
                                        .primary
                                        .copy(alpha = 0.25f)

                                else
                                    MaterialTheme
                                        .colorScheme
                                        .outline
                                        .copy(alpha = 0.08f)
                            )
                        ) {

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),

                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Surface(
                                    shape =
                                        RoundedCornerShape(14.dp),

                                    color =
                                        if (selected)
                                            MaterialTheme
                                                .colorScheme
                                                .primary
                                                .copy(alpha = 0.15f)

                                        else
                                            MaterialTheme
                                                .colorScheme
                                                .surfaceVariant,

                                    modifier = Modifier.size(42.dp)
                                ) {

                                    Box(
                                        contentAlignment =
                                            Alignment.Center
                                    ) {

                                        Icon(
                                            imageVector = dev.icon,

                                            contentDescription = null,

                                            tint =

                                                if (selected)
                                                    MaterialTheme
                                                        .colorScheme
                                                        .primary

                                                else if (isConnected)
                                                    dev.color

                                                else
                                                    Color.Gray,

                                            modifier =
                                                Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(
                                    Modifier.width(10.dp)
                                )

                                Column(
                                    modifier =
                                        Modifier.weight(1f)
                                ) {

                                    Text(
                                        text = dev.deviceName,

                                        fontSize = 13.sp,

                                        fontWeight = FontWeight.Bold,

                                        color =

                                            if (isConnected)
                                                MaterialTheme
                                                    .colorScheme
                                                    .onSurface

                                            else
                                                Color.Gray
                                    )

                                    Spacer(
                                        Modifier.height(2.dp)
                                    )

                                    Text(
                                        text =

                                            if (isConnected)
                                                "${dev.chip} • ${dev.mode}"

                                            else
                                                "Device not detected",

                                        fontSize = 10.sp,

                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .onSurfaceVariant
                                    )
                                }

                                if (selected) {

                                    Icon(
                                        imageVector =
                                            Icons.Default.CheckCircle,

                                        contentDescription = null,

                                        tint =
                                            MaterialTheme
                                                .colorScheme
                                                .primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================
        // ARCHIVE SHEET
        // =========================

        if (viewModel.showFolderManager.value) {

            PEQArchiveRegistryDialog(
                onDismiss = {

                    viewModel.showFolderManager.value =
                        false
                }
            )
        }
    }
}

@Composable
fun DynamicBackground(
    baseColor: Color,
    transientImpact: Float
) {

    val scheme = MaterialTheme.colorScheme

    val isLight =
        scheme.background.luminance() > 0.5f

    val infinite =
        rememberInfiniteTransition()

    val drift by infinite.animateFloat(
        initialValue = -120f,
        targetValue = 120f,

        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 18000,
                easing = LinearEasing
            ),

            repeatMode = RepeatMode.Reverse
        )
    )

    val pulse by infinite.animateFloat(
        initialValue =
            if (isLight) 0.08f else 0.16f,

        targetValue =
            if (isLight) 0.14f else 0.28f,

        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 5000,
                easing = EaseInOutSine
            ),

            repeatMode = RepeatMode.Reverse
        )
    )

    val reactiveAlpha by animateFloatAsState(
        targetValue =
            (0.03f + transientImpact * 0.08f)
                .coerceAtMost(0.18f),

        animationSpec = tween(400),

        label = "reactive"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors =

                        if (isLight) {

                            listOf(
                                Color(0xFFF7F8FC),
                                Color(0xFFEFF2F9),
                                Color(0xFFF9FAFD)
                            )

                        } else {

                            listOf(
                                Color(0xFF05070B),
                                Color(0xFF0A0F18),
                                Color(0xFF05070B)
                            )
                        }
                )
            )
    ) {

        // =========================
        // MAIN LIGHT ORB
        // =========================

        Box(
            modifier = Modifier
                .size(700.dp)
                .offset(
                    x = drift.dp,
                    y = (-120).dp
                )
                .align(Alignment.TopCenter)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            baseColor.copy(alpha = pulse),
                            baseColor.copy(alpha = reactiveAlpha),
                            Color.Transparent
                        )
                    ),

                    shape = CircleShape
                )
        )

        // =========================
        // SECONDARY ORB
        // =========================

        Box(
            modifier = Modifier
                .size(520.dp)
                .offset(
                    x = (-drift * 0.6f).dp,
                    y = 220.dp
                )
                .align(Alignment.Center)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            scheme.secondary.copy(
                                alpha =
                                    if (isLight) 0.05f else 0.09f
                            ),
                            Color.Transparent
                        )
                    ),

                    shape = CircleShape
                )
        )

        // =========================
        // TOP SHINE
        // =========================

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            if (isLight)
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                            else
                                Color.White.copy(alpha = 0.03f),

                            Color.Transparent
                        )
                    )
                )
        )

        // =========================
        // EDGE VIGNETTE
        // =========================

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,

                            if (isLight)
                                Color.Black.copy(alpha = 0.05f)
                            else
                                Color.Black.copy(alpha = 0.42f)
                        )
                    )
                )
        )
    }
}
    @Composable
    fun NavigationRailSection(viewModel: AppViewModel) {
        val tabs = listOf(Tab.Player, Tab.Dashboard, Tab.Tuning, Tab.Hardware, Tab.Learn)
        tabs.forEach { tab ->
            val isSelected = viewModel.currentTab.value == tab
            Surface(
                onClick = { viewModel.currentTab.value = tab },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                modifier = Modifier.fillMaxWidth()
                    .height(54.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        tab.icon,
                        null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        tab.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }


@Composable
private fun VolumeControlSlider(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {

    val volume =
        viewModel.masterVolume.value

    val volumeIcon =
        when {

            volume > 0.5f ->
                Icons.AutoMirrored.Filled.VolumeUp

            volume > 0f ->
                Icons.AutoMirrored.Filled.VolumeDown

            else ->
                Icons.AutoMirrored.Filled.VolumeMute
        }

    Surface(
        modifier = modifier,

        shape = RoundedCornerShape(22.dp),

        color =
            MaterialTheme.colorScheme.surfaceContainerHigh
                .copy(alpha = 0.72f),

        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline
                .copy(alpha = 0.06f)
        )
    ) {

        Row(
            modifier = Modifier
                .wrapContentWidth()
                .padding(
                    horizontal = 14.dp,
                    vertical = 12.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Surface(
                modifier = Modifier.size(44.dp),

                shape = CircleShape,

                color =
                    MaterialTheme
                        .colorScheme
                        .primary
                        .copy(alpha = 0.12f)
            ) {

                Box(
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector = volumeIcon,

                        contentDescription = null,

                        tint =
                            MaterialTheme
                                .colorScheme
                                .primary,

                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Slider(
                value = volume,

                onValueChange = {
                    viewModel.setMasterVolume(it)
                },

                modifier = Modifier.width(140.dp),

                colors = SliderDefaults.colors(

                    thumbColor =
                        MaterialTheme.colorScheme.primary,

                    activeTrackColor =
                        MaterialTheme.colorScheme.primary,

                    inactiveTrackColor =
                        MaterialTheme
                            .colorScheme
                            .onSurface
                            .copy(alpha = 0.10f)
                )
            )

            Spacer(Modifier.width(10.dp))

            Text(
                text =
                    "${(volume * 100).toInt()}%",

                fontSize = 11.sp,

                fontWeight = FontWeight.Bold,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,

                modifier = Modifier.width(42.dp),

                textAlign = TextAlign.End
            )
        }
    }
}
