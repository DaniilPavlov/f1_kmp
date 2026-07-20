package com.example.f1_kmp.ui.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.f1_kmp.data.model.ConstructorModel
import com.example.f1_kmp.data.model.DriverModel
import com.example.f1_kmp.ui.components.F1AppBar
import com.example.f1_kmp.ui.screens.circuits.CircuitDetailScreen
import com.example.f1_kmp.ui.screens.circuits.CircuitsScreen
import com.example.f1_kmp.ui.screens.constructor.ConstructorDetailScreen
import com.example.f1_kmp.ui.screens.driver.DriverDetailScreen
import com.example.f1_kmp.ui.screens.halloffame.HallOfFameScreen
import com.example.f1_kmp.ui.screens.home.HomeScreen
import com.example.f1_kmp.ui.screens.results.RaceInfoScreen
import com.example.f1_kmp.ui.screens.results.RaceSearchScreen
import com.example.f1_kmp.ui.screens.results.ResultsScreen
import com.example.f1_kmp.ui.screens.schedule.ScheduleScreen
import com.example.f1_kmp.ui.theme.AppStyles
import com.example.f1_kmp.ui.theme.F1Black
import com.example.f1_kmp.ui.theme.F1Red
import com.example.f1_kmp.ui.theme.F1White
import f1_kmp.composeapp.generated.resources.Res
import f1_kmp.composeapp.generated.resources.circuit_info_title
import f1_kmp.composeapp.generated.resources.constructor
import f1_kmp.composeapp.generated.resources.detailed_info
import f1_kmp.composeapp.generated.resources.driver
import f1_kmp.composeapp.generated.resources.nav_calendar
import f1_kmp.composeapp.generated.resources.nav_circuit
import f1_kmp.composeapp.generated.resources.nav_circuits
import f1_kmp.composeapp.generated.resources.nav_hall_of_fame
import f1_kmp.composeapp.generated.resources.nav_home
import f1_kmp.composeapp.generated.resources.nav_lights
import f1_kmp.composeapp.generated.resources.nav_racing_car
import f1_kmp.composeapp.generated.resources.nav_results
import f1_kmp.composeapp.generated.resources.nav_trophy
import f1_kmp.composeapp.generated.resources.race_search_title
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import com.example.f1_kmp.domain.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Описание вкладки нижней навигации: route для NavHost, подпись и иконка.
 */
sealed class BottomTab(
    val route: String,
    val labelRes: StringResource,
    val iconRes: DrawableResource,
) {
    data object Home : BottomTab("home", Res.string.nav_home, Res.drawable.nav_home)
    data object Results : BottomTab("results", Res.string.nav_results, Res.drawable.nav_racing_car)
    data object Schedule : BottomTab("schedule", Res.string.nav_calendar, Res.drawable.nav_lights)
    data object HallOfFame : BottomTab("hall_of_fame", Res.string.nav_hall_of_fame, Res.drawable.nav_trophy)
    data object Circuits : BottomTab("circuits", Res.string.nav_circuits, Res.drawable.nav_circuit)
}

private val tabs = listOf(
    BottomTab.Home,
    BottomTab.Results,
    BottomTab.Schedule,
    BottomTab.HallOfFame,
    BottomTab.Circuits,
)

/**
 * Корневой Composable приложения: Scaffold + NavHost + нижняя панель.
 *
 * [koinViewModel] создаёт ViewModel с [com.example.f1_kmp.data.repository.F1Repository] из Koin.
 * Аргументы маршрута (`season`, `round`, `circuitId`) передаются через `parametersOf`.
 *
 * На вложенных экранах (поиск, детали) нижняя панель скрывается — [showBottomBar].
 */
@Composable
fun F1App() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in tabs.map { it.route }
    val popBack: () -> Unit = { navController.popBackStack() }

    val onDriverClick: (DriverModel) -> Unit = { driver ->
        navController.navigate("driver/${driver.driverId}")
    }
    val onConstructorClick: (ConstructorModel) -> Unit = { constructor ->
        navController.navigate("constructor/${constructor.constructorId}")
    }

    Scaffold(
        topBar = {
            when {
                currentRoute in tabs.map { it.route } -> F1AppBar()
                currentRoute == "race_search" -> F1AppBar(title = stringResource(Res.string.race_search_title), onBack = popBack)
                currentRoute?.startsWith("race_info/") == true ->
                    F1AppBar(title = stringResource(Res.string.detailed_info), onBack = popBack)
                currentRoute?.startsWith("circuit/") == true ->
                    F1AppBar(title = stringResource(Res.string.circuit_info_title), onBack = popBack)
                currentRoute?.startsWith("driver/") == true ->
                    F1AppBar(title = stringResource(Res.string.driver), onBack = popBack)
                currentRoute?.startsWith("constructor/") == true ->
                    F1AppBar(title = stringResource(Res.string.constructor), onBack = popBack)
            }
        },
        bottomBar = {
            if (showBottomBar) {
                F1BottomBar(
                    currentRoute = currentRoute,
                    onTabSelected = { tab ->
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = BottomTab.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            composable(BottomTab.Home.route) {
                HomeScreen(
                    viewModel = koinViewModel(),
                    onDriverClick = onDriverClick,
                    onConstructorClick = onConstructorClick,
                )
            }
            composable(BottomTab.Results.route) {
                ResultsScreen(
                    viewModel = koinViewModel(),
                    onSearchRace = { navController.navigate("race_search") },
                    onRaceDetails = { race ->
                        navController.navigate("race_info/${race.season}/${race.round}")
                    },
                    onDriverClick = onDriverClick,
                )
            }
            composable("race_search") {
                RaceSearchScreen(
                    viewModel = koinViewModel(),
                    onRaceDetails = { race ->
                        navController.navigate("race_info/${race.season}/${race.round}")
                    },
                    onDriverClick = onDriverClick,
                )
            }
            composable(
                route = "race_info/{season}/{round}",
                arguments = listOf(
                    navArgument("season") { type = NavType.StringType },
                    navArgument("round") { type = NavType.StringType },
                ),
            ) { entry ->
                val season = entry.arguments?.getString("season").orEmpty()
                val round = entry.arguments?.getString("round").orEmpty()
                RaceInfoScreen(
                    viewModel = koinViewModel { parametersOf(season, round) },
                    onDriverClick = onDriverClick,
                )
            }
            composable(BottomTab.Schedule.route) {
                ScheduleScreen(koinViewModel())
            }
            composable(BottomTab.HallOfFame.route) {
                HallOfFameScreen(
                    viewModel = koinViewModel(),
                    onDriverClick = onDriverClick,
                    onConstructorClick = onConstructorClick,
                )
            }
            composable(BottomTab.Circuits.route) {
                CircuitsScreen(
                    viewModel = koinViewModel(),
                    onCircuitClick = { circuitId -> navController.navigate("circuit/$circuitId") },
                )
            }
            composable(
                route = "circuit/{circuitId}",
                arguments = listOf(
                    navArgument("circuitId") { type = NavType.StringType },
                ),
            ) { entry ->
                val circuitId = entry.arguments?.getString("circuitId").orEmpty()
                CircuitDetailScreen(
                    viewModel = koinViewModel { parametersOf(circuitId) },
                    onDriverClick = onDriverClick,
                )
            }
            composable(
                route = "driver/{driverId}",
                arguments = listOf(
                    navArgument("driverId") { type = NavType.StringType },
                ),
            ) { entry ->
                val driverId = entry.arguments?.getString("driverId").orEmpty()
                DriverDetailScreen(
                    viewModel = koinViewModel { parametersOf(driverId) },
                    onConstructorClick = onConstructorClick,
                )
            }
            composable(
                route = "constructor/{constructorId}",
                arguments = listOf(
                    navArgument("constructorId") { type = NavType.StringType },
                ),
            ) { entry ->
                val constructorId = entry.arguments?.getString("constructorId").orEmpty()
                ConstructorDetailScreen(
                    viewModel = koinViewModel { parametersOf(constructorId) },
                    onDriverClick = onDriverClick,
                )
            }
        }
    }
}

/** Нижняя панель с 5 вкладками; активная — красные иконка и текст, неактивная — белые. */
@Composable
private fun F1BottomBar(currentRoute: String?, onTabSelected: (BottomTab) -> Unit) {
    Column {
        Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(F1Red))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(F1Black)
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            tabs.forEach { tab ->
                val selected = currentRoute == tab.route
                val contentColor = if (selected) F1Red else F1White
                val label = stringResource(tab.labelRes)
                Column(
                    modifier = Modifier
                        .clickable { onTabSelected(tab) }
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painterResource(tab.iconRes),
                        contentDescription = label,
                        modifier = Modifier.size(28.dp),
                        colorFilter = ColorFilter.tint(contentColor),
                    )
                    Text(
                        text = label,
                        style = AppStyles.navBar.copy(color = contentColor),
                    )
                }
            }
        }
    }
}
