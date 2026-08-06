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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.f1_kmp.data.analytics.AnalyticsEvent
import com.example.f1_kmp.data.analytics.AnalyticsGateway
import com.example.f1_kmp.data.deeplink.DeepLinkBus
import com.example.f1_kmp.data.deeplink.DeepLinkTarget
import com.example.f1_kmp.domain.live.LiveWeekendController
import com.example.f1_kmp.domain.model.Circuit
import com.example.f1_kmp.domain.model.Constructor
import com.example.f1_kmp.domain.model.Driver
import com.example.f1_kmp.domain.stringResource
import com.example.f1_kmp.ui.components.F1AppBar
import com.example.f1_kmp.ui.components.LiveSessionBanner
import com.example.f1_kmp.ui.screens.circuits.CircuitDetailScreen
import com.example.f1_kmp.ui.screens.circuits.CircuitsScreen
import com.example.f1_kmp.ui.screens.constructor.ConstructorDetailScreen
import com.example.f1_kmp.ui.screens.driver.DriverDetailScreen
import com.example.f1_kmp.ui.screens.finishstatus.FinishStatusScreen
import com.example.f1_kmp.ui.screens.h2h.H2hMode
import com.example.f1_kmp.ui.screens.h2h.H2hScreen
import com.example.f1_kmp.ui.screens.halloffame.HallOfFameScreen
import com.example.f1_kmp.ui.screens.home.HomeScreen
import com.example.f1_kmp.ui.screens.predictor.PredictorLeaderboardScreen
import com.example.f1_kmp.ui.screens.predictor.PredictorScreen
import com.example.f1_kmp.ui.screens.predictor.PredictorSeasonHistoryScreen
import com.example.f1_kmp.ui.screens.predictor.PredictorWeekendDetailScreen
import com.example.f1_kmp.ui.screens.profile.AuthRegisterScreen
import com.example.f1_kmp.ui.screens.profile.AuthSignInScreen
import com.example.f1_kmp.ui.screens.profile.ProfileScreen
import com.example.f1_kmp.ui.screens.results.RaceInfoScreen
import com.example.f1_kmp.ui.screens.results.RaceSearchScreen
import com.example.f1_kmp.ui.screens.results.ResultsScreen
import com.example.f1_kmp.ui.screens.rewind.SeasonRewindScreen
import com.example.f1_kmp.ui.screens.schedule.ScheduleScreen
import com.example.f1_kmp.ui.theme.AppStyles
import com.example.f1_kmp.ui.theme.F1Chrome
import com.example.f1_kmp.ui.theme.F1OnChrome
import com.example.f1_kmp.ui.theme.F1Red
import com.example.f1_kmp.util.LocalShareActionSetter
import f1_kmp.composeapp.generated.resources.Res
import f1_kmp.composeapp.generated.resources.circuit_info_title
import f1_kmp.composeapp.generated.resources.constructor
import f1_kmp.composeapp.generated.resources.detailed_info
import f1_kmp.composeapp.generated.resources.driver
import f1_kmp.composeapp.generated.resources.finish_status_title
import f1_kmp.composeapp.generated.resources.h2h_title
import f1_kmp.composeapp.generated.resources.hall_of_fame_title
import f1_kmp.composeapp.generated.resources.auth_register_title
import f1_kmp.composeapp.generated.resources.auth_sign_in_title
import f1_kmp.composeapp.generated.resources.nav_calendar
import f1_kmp.composeapp.generated.resources.nav_circuits
import f1_kmp.composeapp.generated.resources.nav_helmet
import f1_kmp.composeapp.generated.resources.nav_home
import f1_kmp.composeapp.generated.resources.nav_lights
import f1_kmp.composeapp.generated.resources.nav_predictor
import f1_kmp.composeapp.generated.resources.nav_profile
import f1_kmp.composeapp.generated.resources.nav_racing_car
import f1_kmp.composeapp.generated.resources.nav_results
import f1_kmp.composeapp.generated.resources.nav_trophy
import f1_kmp.composeapp.generated.resources.predictor_history_title
import f1_kmp.composeapp.generated.resources.predictor_leaderboard_title
import f1_kmp.composeapp.generated.resources.predictor_title
import f1_kmp.composeapp.generated.resources.profile_title
import f1_kmp.composeapp.generated.resources.race_search_title
import f1_kmp.composeapp.generated.resources.season_rewind_title
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.reflect.KClass

/** Описание вкладки нижней навигации (type-safe route). */
sealed class BottomTab(
    val route: Any,
    val routeClass: KClass<out Any>,
    val labelRes: StringResource,
    val iconRes: DrawableResource,
) {
    data object HomeTab : BottomTab(Home, Home::class, Res.string.nav_home, Res.drawable.nav_home)
    data object ResultsTab : BottomTab(Results, Results::class, Res.string.nav_results, Res.drawable.nav_racing_car)
    data object ScheduleTab : BottomTab(Schedule, Schedule::class, Res.string.nav_calendar, Res.drawable.nav_lights)
    data object PredictorTab : BottomTab(Predictor, Predictor::class, Res.string.nav_predictor, Res.drawable.nav_trophy)
    data object ProfileTab : BottomTab(Profile, Profile::class, Res.string.nav_profile, Res.drawable.nav_helmet)
}

private val tabs = listOf(
    BottomTab.HomeTab,
    BottomTab.ResultsTab,
    BottomTab.ScheduleTab,
    BottomTab.PredictorTab,
    BottomTab.ProfileTab,
)

/** Корневой Composable: Scaffold + NavHost + нижняя панель. */
@Composable
fun F1App() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val showBottomBar = tabs.any { destination?.hasRoute(it.routeClass) == true }
    val popBack: () -> Unit = { navController.popBackStack() }
    var shareAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val liveController = koinInject<LiveWeekendController>()
    val deepLinkBus = koinInject<DeepLinkBus>()
    val analytics = koinInject<AnalyticsGateway>()

    LaunchedEffect(Unit) {
        liveController.loadScoreboard()
        deepLinkBus.targets.collectLatest { target ->
            navigateDeepLink(navController, target, liveController.isLive)
        }
    }

    LaunchedEffect(destination?.route) {
        val route = destination?.route ?: return@LaunchedEffect
        val screenName = route.substringBefore('?').substringBefore('/')
            .substringAfterLast('.')
            .ifBlank { route }
        analytics.log(
            AnalyticsEvent.ScreenView(
                screenName = screenName,
                screenClass = destination.route,
            ),
        )
    }

    val onDriverClick: (Driver) -> Unit = { driver ->
        analytics.log(AnalyticsEvent.DriverOpened(driver.driverId, driver.fullName))
        navController.navigate(DriverDetail(driver.driverId))
    }
    val onConstructorClick: (Constructor) -> Unit = { constructor ->
        analytics.log(AnalyticsEvent.ConstructorOpened(constructor.constructorId, constructor.name))
        navController.navigate(ConstructorDetail(constructor.constructorId))
    }
    val onCircuitClick: (Circuit) -> Unit = { circuit ->
        analytics.log(AnalyticsEvent.CircuitOpened(circuit.circuitId, circuit.circuitName))
        navController.navigate(CircuitDetail(circuit.circuitId))
    }

    CompositionLocalProvider(LocalShareActionSetter provides { shareAction = it }) {
        Scaffold(
            topBar = {
                F1TopBar(
                    destination = destination,
                    backStackEntry = backStackEntry,
                    showBottomBar = showBottomBar,
                    popBack = popBack,
                    shareAction = shareAction,
                )
            },
            bottomBar = {
                if (showBottomBar) {
                    Column {
                        LiveSessionBanner(
                            controller = liveController,
                            onTap = {
                                navController.navigate(Results) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                        F1BottomBar(
                            currentDestination = destination,
                            onTabSelected = { tab ->
                                analytics.log(AnalyticsEvent.TabSwitched(tab.analyticsTab))
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                }
            },
        ) { padding ->
            F1NavHost(
                navController = navController,
                onDriverClick = onDriverClick,
                onConstructorClick = onConstructorClick,
                onCircuitClick = onCircuitClick,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        }
    }
}

private val BottomTab.analyticsTab: String
    get() = when (this) {
        BottomTab.HomeTab -> "home"
        BottomTab.ResultsTab -> "results"
        BottomTab.ScheduleTab -> "schedule"
        BottomTab.PredictorTab -> "predictor"
        BottomTab.ProfileTab -> "profile"
    }

private fun navigateDeepLink(
    navController: NavHostController,
    target: DeepLinkTarget,
    isLive: Boolean,
) {
    when (target) {
        is DeepLinkTarget.Driver -> navController.navigate(DriverDetail(target.driverId))
        is DeepLinkTarget.Constructor -> navController.navigate(ConstructorDetail(target.constructorId))
        is DeepLinkTarget.Circuit -> navController.navigate(CircuitDetail(target.circuitId))
        DeepLinkTarget.RaceLive -> navController.navigate(Results) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        is DeepLinkTarget.Race -> {
            val route = if (isLive) Results else Schedule
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }
}

@Composable
@Suppress("CyclomaticComplexMethod")
private fun F1TopBar(
    destination: NavDestination?,
    backStackEntry: androidx.navigation.NavBackStackEntry?,
    showBottomBar: Boolean,
    popBack: () -> Unit,
    shareAction: (() -> Unit)?,
) {
    when {
        showBottomBar && destination?.hasRoute<Profile>() == true -> F1AppBar(
            title = stringResource(Res.string.profile_title),
        )
        showBottomBar && destination?.hasRoute<Predictor>() == true -> F1AppBar(
            title = stringResource(Res.string.predictor_title),
        )
        showBottomBar -> F1AppBar()
        destination?.hasRoute<AuthSignIn>() == true -> F1AppBar(
            title = stringResource(Res.string.auth_sign_in_title),
            onBack = popBack,
        )
        destination?.hasRoute<AuthRegister>() == true -> F1AppBar(
            title = stringResource(Res.string.auth_register_title),
            onBack = popBack,
        )
        destination?.hasRoute<PredictorWeekendDetail>() == true -> {
            val raceName = backStackEntry?.toRoute<PredictorWeekendDetail>()?.raceName
            F1AppBar(
                title = raceName?.takeIf { it.isNotBlank() }
                    ?: stringResource(Res.string.predictor_title),
                onBack = popBack,
            )
        }
        destination?.hasRoute<PredictorSeasonHistory>() == true -> F1AppBar(
            title = backStackEntry?.toRoute<PredictorSeasonHistory>()?.year
                ?: stringResource(Res.string.predictor_history_title),
            onBack = popBack,
        )
        destination?.hasRoute<PredictorLeaderboard>() == true -> F1AppBar(
            title = stringResource(
                Res.string.predictor_leaderboard_title,
                backStackEntry?.toRoute<PredictorLeaderboard>()?.year.orEmpty(),
            ),
            onBack = popBack,
        )
        destination?.hasRoute<Circuits>() == true -> F1AppBar(
            title = stringResource(Res.string.nav_circuits),
            onBack = popBack,
        )
        destination?.hasRoute<RaceSearch>() == true -> F1AppBar(
            title = stringResource(Res.string.race_search_title),
            onBack = popBack,
        )
        destination?.hasRoute<HallOfFame>() == true -> F1AppBar(
            title = stringResource(Res.string.hall_of_fame_title),
            onBack = popBack,
        )
        destination?.hasRoute<SeasonRewind>() == true -> F1AppBar(
            title = stringResource(Res.string.season_rewind_title),
            onBack = popBack,
        )
        destination?.hasRoute<H2hDrivers>() == true -> F1AppBar(
            title = stringResource(Res.string.h2h_title),
            onBack = popBack,
        )
        destination?.hasRoute<H2hConstructors>() == true -> F1AppBar(
            title = stringResource(Res.string.h2h_title),
            onBack = popBack,
        )
        destination?.hasRoute<FinishStatus>() == true -> F1AppBar(
            title = stringResource(Res.string.finish_status_title),
            onBack = popBack,
        )
        destination?.hasRoute<RaceInfo>() == true -> F1AppBar(
            title = stringResource(Res.string.detailed_info),
            onBack = popBack,
            onShare = shareAction,
        )
        destination?.hasRoute<CircuitDetail>() == true -> F1AppBar(
            title = stringResource(Res.string.circuit_info_title),
            onBack = popBack,
            onShare = shareAction,
        )
        destination?.hasRoute<DriverDetail>() == true -> F1AppBar(
            title = stringResource(Res.string.driver),
            onBack = popBack,
            onShare = shareAction,
        )
        destination?.hasRoute<ConstructorDetail>() == true -> F1AppBar(
            title = stringResource(Res.string.constructor),
            onBack = popBack,
            onShare = shareAction,
        )
    }
}

@Composable
@Suppress("LongMethod")
private fun F1NavHost(
    navController: NavHostController,
    onDriverClick: (Driver) -> Unit,
    onConstructorClick: (Constructor) -> Unit,
    onCircuitClick: (Circuit) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Home,
        modifier = modifier,
    ) {
        composable<Home> {
            HomeScreen(
                viewModel = koinViewModel(),
                newsViewModel = koinViewModel(),
                onDriverClick = onDriverClick,
                onConstructorClick = onConstructorClick,
            )
        }
        composable<Results> {
            ResultsScreen(
                viewModel = koinViewModel(),
                onSearchRace = { navController.navigate(RaceSearch) },
                onHallOfFame = { navController.navigate(HallOfFame) },
                onSeasonRewind = { navController.navigate(SeasonRewind) },
                onH2h = { navController.navigate(H2hDrivers) },
                onFinishStatus = { navController.navigate(FinishStatus) },
                onRaceDetails = { race ->
                    navController.navigate(RaceInfo(race.season, race.round))
                },
                onDriverClick = onDriverClick,
            )
        }
        composable<RaceSearch> {
            RaceSearchScreen(
                viewModel = koinViewModel(),
                onRaceDetails = { race ->
                    navController.navigate(RaceInfo(race.season, race.round))
                },
                onDriverClick = onDriverClick,
            )
        }
        composable<HallOfFame> {
            HallOfFameScreen(
                viewModel = koinViewModel(),
                onDriverClick = onDriverClick,
                onConstructorClick = onConstructorClick,
            )
        }
        composable<SeasonRewind> {
            SeasonRewindScreen(viewModel = koinViewModel())
        }
        composable<H2hDrivers> {
            H2hScreen(
                driversViewModel = koinViewModel(),
                constructorsViewModel = koinViewModel(),
                initialMode = H2hMode.Drivers,
            )
        }
        composable<H2hConstructors> {
            H2hScreen(
                driversViewModel = koinViewModel(),
                constructorsViewModel = koinViewModel(),
                initialMode = H2hMode.Constructors,
            )
        }
        composable<FinishStatus> {
            FinishStatusScreen(viewModel = koinViewModel())
        }
        composable<RaceInfo> { entry ->
            val args = entry.toRoute<RaceInfo>()
            RaceInfoScreen(
                viewModel = koinViewModel { parametersOf(args.season, args.round) },
                onDriverClick = onDriverClick,
            )
        }
        composable<Schedule> {
            ScheduleScreen(
                viewModel = koinViewModel(),
                onCircuits = { navController.navigate(Circuits) },
            )
        }
        composable<Predictor> {
            PredictorScreen(
                viewModel = koinViewModel(),
                onGoSignIn = { navController.navigate(AuthSignIn) },
                onGoProfile = {
                    navController.navigate(Profile) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onOpenLeaderboard = { year, myPoints ->
                    navController.navigate(PredictorLeaderboard(year = year, myPoints = myPoints))
                },
                onOpenWeekend = { season, weekend ->
                    navController.navigate(
                        PredictorWeekendDetail(
                            season = season,
                            round = weekend.round,
                            raceName = weekend.raceName,
                        ),
                    )
                },
                onOpenSeason = { year ->
                    navController.navigate(PredictorSeasonHistory(year = year))
                },
            )
        }
        composable<PredictorWeekendDetail> { entry ->
            val args = entry.toRoute<PredictorWeekendDetail>()
            PredictorWeekendDetailScreen(
                viewModel = koinViewModel { parametersOf(args.season, args.round, args.raceName) },
                onGoSignIn = {
                    navController.navigate(AuthSignIn) {
                        popUpTo(Predictor) { inclusive = false }
                    }
                },
                onBlocked = { navController.popBackStack() },
            )
        }
        composable<PredictorSeasonHistory> { entry ->
            val args = entry.toRoute<PredictorSeasonHistory>()
            PredictorSeasonHistoryScreen(
                viewModel = koinViewModel { parametersOf(args.year) },
                onGoSignIn = {
                    navController.navigate(AuthSignIn) {
                        popUpTo(Predictor) { inclusive = false }
                    }
                },
                onBlocked = { navController.popBackStack() },
                onOpenWeekend = { season, weekend ->
                    navController.navigate(
                        PredictorWeekendDetail(
                            season = season,
                            round = weekend.round,
                            raceName = weekend.raceName,
                        ),
                    )
                },
            )
        }
        composable<PredictorLeaderboard> { entry ->
            val args = entry.toRoute<PredictorLeaderboard>()
            PredictorLeaderboardScreen(
                viewModel = koinViewModel { parametersOf(args.year, args.myPoints) },
                onGoSignIn = {
                    navController.navigate(AuthSignIn) {
                        popUpTo(Predictor) { inclusive = false }
                    }
                },
                onBlocked = { navController.popBackStack() },
            )
        }
        composable<Profile> {
            ProfileScreen(
                viewModel = koinViewModel(),
                onSignIn = { navController.navigate(AuthSignIn) },
            )
        }
        composable<AuthSignIn> {
            AuthSignInScreen(
                viewModel = koinViewModel(),
                onSuccess = { navController.popBackStack() },
                onGoRegister = {
                    navController.navigate(AuthRegister) {
                        popUpTo(AuthSignIn) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable<AuthRegister> {
            AuthRegisterScreen(
                viewModel = koinViewModel(),
                onSuccess = { navController.popBackStack() },
                onGoSignIn = {
                    navController.navigate(AuthSignIn) {
                        popUpTo(AuthRegister) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable<Circuits> {
            CircuitsScreen(
                viewModel = koinViewModel(),
                onCircuitClick = { circuitId ->
                    navController.navigate(CircuitDetail(circuitId))
                },
            )
        }
        composable<CircuitDetail> { entry ->
            val args = entry.toRoute<CircuitDetail>()
            CircuitDetailScreen(
                viewModel = koinViewModel { parametersOf(args.circuitId) },
                onDriverClick = onDriverClick,
            )
        }
        composable<DriverDetail> { entry ->
            val args = entry.toRoute<DriverDetail>()
            DriverDetailScreen(
                viewModel = koinViewModel { parametersOf(args.driverId) },
                onConstructorClick = onConstructorClick,
                onCircuitClick = onCircuitClick,
            )
        }
        composable<ConstructorDetail> { entry ->
            val args = entry.toRoute<ConstructorDetail>()
            ConstructorDetailScreen(
                viewModel = koinViewModel { parametersOf(args.constructorId) },
                onDriverClick = onDriverClick,
                onCircuitClick = onCircuitClick,
            )
        }
    }
}

@Composable
private fun F1BottomBar(
    currentDestination: NavDestination?,
    onTabSelected: (BottomTab) -> Unit,
) {
    Column {
        Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(F1Red))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(F1Chrome)
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            tabs.forEach { tab ->
                val label = stringResource(tab.labelRes)
                val selected = currentDestination?.hasRoute(tab.routeClass) == true
                val contentColor = if (selected) F1Red else F1OnChrome
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
