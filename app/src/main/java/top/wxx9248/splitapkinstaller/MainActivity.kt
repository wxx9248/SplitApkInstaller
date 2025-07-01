package top.wxx9248.splitapkinstaller

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.json.Json
import top.wxx9248.splitapkinstaller.ui.screens.ApkListRoute
import top.wxx9248.splitapkinstaller.ui.screens.ApkListScreen
import top.wxx9248.splitapkinstaller.ui.screens.HomeRoute
import top.wxx9248.splitapkinstaller.ui.screens.HomeScreen
import top.wxx9248.splitapkinstaller.ui.screens.InstallationRoute
import top.wxx9248.splitapkinstaller.ui.screens.InstallationScreen
import top.wxx9248.splitapkinstaller.ui.theme.SplitApkInstallerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SplitApkInstallerTheme {
                SplitApkInstallerApp(intent)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
fun SplitApkInstallerApp(intent: Intent? = null) {
    val controller = rememberNavController()
    var initialRoute by remember { mutableStateOf<Any>(HomeRoute) }

    // Handle file intent when the app is opened with a file
    LaunchedEffect(intent) {
        intent?.let {
            if (it.action == Intent.ACTION_VIEW && it.data != null) {
                val uri = it.data!!
                val apkListRoute = ApkListRoute(
                    packageUriString = uri.toString(),
                    isFile = true
                )
                initialRoute = apkListRoute
            }
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        NavHost(
            navController = controller,
            startDestination = initialRoute,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<HomeRoute> { backStackEntry ->
                HomeScreen(
                    onNavigateToApkList = { apkListRoute ->
                        controller.navigate(apkListRoute)
                    })
            }

            composable<ApkListRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<ApkListRoute>()
                ApkListScreen(
                    route.packageUriString.toUri(),
                    route.isFile,
                    onNavigateBack = { controller.popBackStack() },
                    onNavigateToInstallation = { installationRoute ->
                        controller.navigate(installationRoute)
                    })
            }

            composable<InstallationRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<InstallationRoute>()
                val selectedApkNames =
                    Json.decodeFromString<List<String>>(route.selectedApkNamesString)
                InstallationScreen(
                    route.packageUriString.toUri(),
                    route.isFile,
                    selectedApkNames,
                    onNavigateBack = {
                        // Normal back navigation - just pop the current screen
                        controller.popBackStack()
                    },
                    onNavigateToHome = {
                        // Clear the entire back stack and navigate to home
                        controller.navigate(HomeRoute) {
                            popUpTo(controller.graph.startDestinationId) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    })
            }
        }
    }
}
