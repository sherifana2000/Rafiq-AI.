package com.example.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.character.CharacterState
import com.example.ui.character.RafiqCharacter
import com.example.ui.components.RafiqResponseBottomSheet
import com.example.ui.home.components.AddTaskDialog
import com.example.ui.home.components.FocusTimerDialog

/**
 * الشاشة الرئيسية لشخصية رفيق الذكية (Character-First HomeScreen)
 * تركز بالكامل على: الشخصية + الصوت + التفاعل الطبيعي الهادئ.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // الرد الذكي لرفيق عبر Bottom Sheet
    if (uiState.showResponseSheet && uiState.pendingParsedCommand != null) {
        RafiqResponseBottomSheet(
            command = uiState.pendingParsedCommand!!,
            sheetState = sheetState,
            onConfirm = viewModel::confirmPendingCommand,
            onDismiss = viewModel::dismissResponseSheet
        )
    }

    // مؤقت التركيز
    if (uiState.showFocusDialog && uiState.activeFocusTimer != null) {
        FocusTimerDialog(
            focusSession = uiState.activeFocusTimer!!,
            onToggleRunning = viewModel::toggleFocusTimerRunning,
            onReset = viewModel::resetFocusTimer,
            onDismiss = viewModel::dismissFocusDialog
        )
    }

    // إضافة مهمة سريعة
    if (uiState.showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { viewModel.showAddTaskDialog(false) },
            onAdd = viewModel::addNewManualTask
        )
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "رفيق",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 21.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    navigationIcon = {
                        // زر ☰ لفتح القائمة الجانبية (Drawer)
                        IconButton(
                            onClick = onOpenDrawer,
                            modifier = Modifier.testTag("drawer_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "فتح القائمة",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    },
                    actions = {
                        // حالة المساعد الصغيرة
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00C853))
                                )
                                Text(
                                    text = "متصل",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 600.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. قسم التحية الهادئة (Greeting)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.greetingText,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 23.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = uiState.greetingSubText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 2. الشخصية التفاعلية في المنتصف (Character as Centerpiece)
                    RafiqCharacter(
                        state = uiState.characterState,
                        size = 190.dp
                    )

                    // فقاعة أو نص رفيق تحت الشخصية مباشرة
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = uiState.characterSpeechText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3. التفاعل الصوتي (Voice-First Interaction)
                    VoiceInteractionCenter(
                        isListening = uiState.characterState == CharacterState.LISTENING,
                        onMicClick = viewModel::onMicPressed
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // 4. حقل الكتابة البسيط (Text Composer)
                    TextComposerBar(
                        inputText = uiState.commandInput,
                        onInputChange = viewModel::onCommandInputChanged,
                        onSubmit = viewModel::submitCommand,
                        isProcessing = uiState.isProcessing
                    )

                    // 5. الاقتراحات السريعة (Quick Suggestions)
                    QuickSuggestionsBar(
                        onSelectSuggestion = viewModel::onSuggestionSelected
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // 6. الإجراء الحالي أو القادم (Current Action)
                    CurrentActionBadge(
                        currentTask = uiState.currentTask,
                        onStartTask = viewModel::startCurrentTaskFocus,
                        onPostponeTask = viewModel::postponeCurrentTask
                    )

                    Spacer(modifier = Modifier.height(18.dp))
                }
            }
        }
    }
}

/**
 * زر الميكروفون المركزي التفاعلي
 */
@Composable
private fun VoiceInteractionCenter(
    isListening: Boolean,
    onMicClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_scale"
    )

    val buttonBgColor by animateColorAsState(
        targetValue = if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
        label = "mic_btn_color"
    )

    val iconTint by animateColorAsState(
        targetValue = if (isListening) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
        label = "mic_icon_color"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .then(if (isListening) Modifier.scale(pulseScale) else Modifier)
                .clip(CircleShape)
                .background(buttonBgColor)
                .border(
                    width = if (isListening) 3.dp else 1.5.dp,
                    color = if (isListening) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onMicClick,
                modifier = Modifier
                    .size(76.dp)
                    .testTag("central_voice_mic_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "تحدث مع رفيق",
                    tint = iconTint,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Text(
            text = if (isListening) "أنا أسمعك الآن..." else "اضغط وتحدث مع رفيق",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * حقل كتابة بسيط لرفيق مع زر إرسال
 */
@Composable
private fun TextComposerBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    isProcessing: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                RoundedCornerShape(22.dp)
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = inputText,
            onValueChange = onInputChange,
            placeholder = {
                Text(
                    text = "اكتب لرفيق ما تريد...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                )
            },
            modifier = Modifier
                .weight(1f)
                .testTag("companion_text_input"),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            )
        )

        IconButton(
            onClick = onSubmit,
            enabled = inputText.isNotBlank() && !isProcessing,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .testTag("companion_send_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "إرسال",
                tint = if (inputText.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * شرائح الاقتراحات السريعة
 */
@Composable
private fun QuickSuggestionsBar(
    onSelectSuggestion: (String) -> Unit
) {
    val suggestions = listOf(
        "رتب يومي",
        "ذكّرني بمراجعة درس النحو",
        "جلسة تركيز 50 دقيقة",
        "افتح القرآن على آل عمران"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "جرّب أن تقول:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestions.forEach { suggestion ->
                FilterChip(
                    selected = false,
                    onClick = { onSelectSuggestion(suggestion) },
                    label = {
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        enabled = true,
                        selected = false
                    )
                )
            }
        }
    }
}

/**
 * شارة الإجراء الحالي أو القادم (Current Action)
 */
@Composable
private fun CurrentActionBadge(
    currentTask: com.example.model.ScheduledTask?,
    onStartTask: () -> Unit,
    onPostponeTask: () -> Unit
) {
    if (currentTask != null) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("current_action_badge"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                ),
                width = 1.dp
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "الآن",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Column {
                        Text(
                            text = currentTask.title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "09:00",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onStartTask,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "بدء",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onPostponeTask,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = "تأجيل",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
