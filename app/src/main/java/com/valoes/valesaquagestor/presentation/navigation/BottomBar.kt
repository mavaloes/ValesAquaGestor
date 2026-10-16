package com.valoes.valesaquagestor.presentation.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import com.valoes.valesaquagestor.R
import kotlin.reflect.KClass

/** Um item do menu inferior. O menu tem no máximo 4 itens (`specs/04-requisitos-ux.md`). */
private data class BottomBarItem(
    val route: Any,
    val routeClass: KClass<*>,
    @StringRes val label: Int,
    val icon: ImageVector,
)

private val items = listOf(
    BottomBarItem(Home, Home::class, R.string.nav_home, Icons.Default.Home),
    BottomBarItem(Tanques, Tanques::class, R.string.nav_tanks, Icons.AutoMirrored.Filled.List),
    BottomBarItem(Registrar, Registrar::class, R.string.nav_register, Icons.Default.Add),
    BottomBarItem(Mais, Mais::class, R.string.nav_more, Icons.Default.Menu),
)

/** `true` se a tela atual é uma das abas do menu (só nelas o menu aparece). */
fun NavDestination?.isTopLevel(): Boolean = this != null && items.any { hasRoute(it.routeClass) }

/** Menu inferior com Início, Tanques, Registrar e Mais. */
@Composable
fun AppBottomBar(currentDestination: NavDestination?, onNavigate: (Any) -> Unit) {
    NavigationBar {
        items.forEach { item ->
            val label = stringResource(item.label)
            NavigationBarItem(
                selected = currentDestination?.hierarchy?.any { it.hasRoute(item.routeClass) } == true,
                onClick = { onNavigate(item.route) },
                // O rótulo ao lado já diz o destino; o leitor de tela não repete a descrição do ícone.
                icon = { Icon(imageVector = item.icon, contentDescription = label, modifier = Modifier.clearAndSetSemantics { }) },
                label = { Text(label) },
            )
        }
    }
}
