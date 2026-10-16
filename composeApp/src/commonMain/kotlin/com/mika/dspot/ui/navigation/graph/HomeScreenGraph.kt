package com.mika.dspot.ui.navigation.graph

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.mika.dspot.ui.navigation.destination.home.AppLogDestination
import com.mika.dspot.ui.navigation.destination.home.ChangelogDestination
import com.mika.dspot.ui.navigation.destination.home.CreditDestination
import com.mika.dspot.ui.navigation.destination.home.ListenTogetherDestination
import com.mika.dspot.ui.navigation.destination.home.ListenTogetherSettingsDestination
import com.mika.dspot.ui.navigation.destination.home.MoodDestination
import com.mika.dspot.ui.navigation.destination.home.NotificationDestination
import com.mika.dspot.ui.navigation.destination.home.RecentlySongsDestination
import com.mika.dspot.ui.navigation.destination.home.SettingsDestination
import com.mika.dspot.ui.screen.home.ListenTogetherScreen
import com.mika.dspot.ui.screen.home.ListenTogetherSettingsScreen
import com.mika.dspot.ui.screen.home.MoodScreen
import com.mika.dspot.ui.screen.home.NotificationScreen
import com.mika.dspot.ui.screen.home.RecentlySongsScreen
import com.mika.dspot.ui.screen.home.SettingScreen
import com.mika.dspot.ui.screen.other.AppLogScreen
import com.mika.dspot.ui.screen.other.ChangelogScreen
import com.mika.dspot.ui.screen.other.CreditScreen

import com.mika.dspot.ui.navigation.destination.home.AudioCutterDestination
import com.mika.dspot.ui.screen.other.AudioCutterScreen
import com.mika.dspot.ui.navigation.destination.home.DuplicateDetectorDestination
import com.mika.dspot.ui.screen.other.DuplicateDetectorScreen

fun NavGraphBuilder.homeScreenGraph(
    innerPadding: PaddingValues,
    navController: NavController,
) {
    composable<AudioCutterDestination> {
        AudioCutterScreen(
            navController = navController,
        )
    }
    composable<DuplicateDetectorDestination> {
        DuplicateDetectorScreen(
            navController = navController,
        )
    }
    composable<ChangelogDestination> {
        ChangelogScreen(
            paddingValues = innerPadding,
            navController = navController,
        )
    }
    composable<CreditDestination> {
        CreditScreen(
            paddingValues = innerPadding,
            navController = navController,
        )
    }
    composable<AppLogDestination> {
        AppLogScreen(
            paddingValues = innerPadding,
            navController = navController,
        )
    }
    composable<MoodDestination> { entry ->
        val params = entry.toRoute<MoodDestination>().params
        MoodScreen(
            navController = navController,
            params = params,
        )
    }
    composable<ListenTogetherDestination> {
        ListenTogetherScreen(
            navController = navController,
            innerPadding = innerPadding,
        )
    }
    composable<ListenTogetherSettingsDestination> {
        ListenTogetherSettingsScreen(
            navController = navController,
            innerPadding = innerPadding,
        )
    }
    composable<NotificationDestination> {
        NotificationScreen(
            navController = navController,
        )
    }
    composable<RecentlySongsDestination> {
        RecentlySongsScreen(
            navController = navController,
            innerPadding = innerPadding,
        )
    }
    composable<SettingsDestination> {
        SettingScreen(
            navController = navController,
            innerPadding = innerPadding,
        )
    }
}