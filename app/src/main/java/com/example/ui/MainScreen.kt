package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.character.CharacterState
import com.example.ui.character.RafiqCharacter
import com.example.ui.components.RafiqDrawerContent
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.navigation.DrawerDestination
import com.example.ui.screens.ProgressScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.screens.TodayScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAboutDialog by remember { mutableStateOf(false) }

    // حوار "حول رفيق"
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        RafiqCharacter(state = CharacterState.HAPPY, size = 40.dp)
                    }
                    Text(
                        text = "حول رفيق AI",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Text(
                    text = "رفيق هو رفيقك الشخصي التفاعلي داخل الهاتف. يتحدث معك، ويستمع لأوامرك، وينظم يومك دون الحاجة للدخول في تعقيدات الجداول التقليدية.\n\nالإصدار: 1.0 (النموذج الأولي للشخصية التفاعلية)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = { showAboutDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("إغلاق")
                }
            }
        )
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                RafiqDrawerContent(
                    currentDestination = uiState.currentDestination,
                    onSelectDestination = { dest ->
                        viewModel.selectDestination(dest)
                        coroutineScope.launch { drawerState.close() }
                    },
                    onShowThemesNotice = {
                        coroutineScope.launch {
                            drawerState.close()
                            snackbarHostState.showSnackbar("متجر الثيمات وتخصيص الشخصية قادم في مرحلة لاحقة 🎨")
                        }
                    },
                    onShowAboutDialog = {
                        coroutineScope.launch { drawerState.close() }
                        showAboutDialog = true
                    }
                )
            },
            gesturesEnabled = true,
            modifier = modifier.fillMaxSize()
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = {
                    if (uiState.currentDestination != DrawerDestination.HOME) {
                        // ترويسة بسيطة للشاشات الثانوية مع زر العودة للرئيسية
                        TopAppBar(
                            title = {
                                Text(
                                    text = uiState.currentDestination.titleArabic,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    )
                                )
                            },
                            navigationIcon = {
                                IconButton(
                                    onClick = { viewModel.selectDestination(DrawerDestination.HOME) }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "العودة للرئيسية"
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    AnimatedContent(
                        targetState = uiState.currentDestination,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "screen_transition"
                    ) { destination ->
                        when (destination) {
                            DrawerDestination.HOME -> HomeScreen(
                                viewModel = viewModel,
                                onOpenDrawer = {
                                    coroutineScope.launch { drawerState.open() }
                                }
                            )
                            DrawerDestination.TODAY -> TodayScreen(viewModel = viewModel)
                            DrawerDestination.TASKS -> TasksScreen(viewModel = viewModel)
                            DrawerDestination.PROGRESS -> ProgressScreen(viewModel = viewModel)
                            DrawerDestination.SETTINGS -> SettingsScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
