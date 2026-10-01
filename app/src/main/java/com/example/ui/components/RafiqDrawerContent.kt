package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.character.CharacterState
import com.example.ui.character.RafiqCharacter
import com.example.ui.navigation.DrawerDestination

@Composable
fun RafiqDrawerContent(
    currentDestination: DrawerDestination,
    onSelectDestination: (DrawerDestination) -> Unit,
    onShowThemesNotice: () -> Unit,
    onShowAboutDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(
        modifier = modifier
            .width(300.dp)
            .fillMaxHeight()
            .testTag("rafiq_drawer_sheet"),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp, horizontal = 16.dp)
        ) {
            // هيدر القائمة الجانبية: شخصية رفيق الصغيرة + الاسم والصفة
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    RafiqCharacter(
                        state = CharacterState.IDLE,
                        size = 52.dp
                    )
                }

                Column {
                    Text(
                        text = "رفيق AI",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "مساعدك الشخصي",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                thickness = 1.dp
            )
            Spacer(modifier = Modifier.height(14.dp))

            // عناصر التنقل الأساسية
            DrawerItemRow(
                title = "الرئيسية",
                icon = Icons.Default.Home,
                isSelected = currentDestination == DrawerDestination.HOME,
                onClick = { onSelectDestination(DrawerDestination.HOME) }
            )

            DrawerItemRow(
                title = "يومي",
                icon = Icons.Default.CalendarMonth,
                isSelected = currentDestination == DrawerDestination.TODAY,
                onClick = { onSelectDestination(DrawerDestination.TODAY) }
            )

            DrawerItemRow(
                title = "مهامي",
                icon = Icons.Default.CheckCircle,
                isSelected = currentDestination == DrawerDestination.TASKS,
                onClick = { onSelectDestination(DrawerDestination.TASKS) }
            )

            DrawerItemRow(
                title = "تقدمي",
                icon = Icons.Default.QueryStats,
                isSelected = currentDestination == DrawerDestination.PROGRESS,
                onClick = { onSelectDestination(DrawerDestination.PROGRESS) }
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                thickness = 1.dp
            )
            Spacer(modifier = Modifier.height(14.dp))

            // الثيمات (قريبًا)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onShowThemesNotice() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "الثيمات",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "قريباً",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // الإعدادات
            DrawerItemRow(
                title = "الإعدادات",
                icon = Icons.Default.Settings,
                isSelected = currentDestination == DrawerDestination.SETTINGS,
                onClick = { onSelectDestination(DrawerDestination.SETTINGS) }
            )

            // حول رفيق
            DrawerItemRow(
                title = "حول رفيق",
                icon = Icons.Default.Info,
                isSelected = false,
                onClick = onShowAboutDialog
            )

            Spacer(modifier = Modifier.weight(1f))

            // نبذة تذييل لطيفة
            Text(
                text = "رفيق الإصدار التجريبي 1.0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun DrawerItemRow(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            )
        },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(22.dp)
            )
        },
        selected = isSelected,
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier.padding(vertical = 3.dp)
    )
}
