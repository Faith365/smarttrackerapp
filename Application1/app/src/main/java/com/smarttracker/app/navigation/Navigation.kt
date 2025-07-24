package com.smarttracker.app.navigation

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.smarttracker.app.screens.AddDeviceScreen
import com.smarttracker.app.screens.BackupCodeScreen
import com.smarttracker.app.screens.CreatePasskeyScreen
import com.smarttracker.app.screens.DeviceDetailsScreen
import com.smarttracker.app.screens.HomeScreen
import com.smarttracker.app.screens.LimitedTrackingScreen
import com.smarttracker.app.screens.ManageTrustedContactsScreen
import com.smarttracker.app.screens.PredictiveTrackingScreen
import com.smarttracker.app.screens.PrivacyPolicyScreen
import com.smarttracker.app.screens.ResetPasswordScreen
import com.smarttracker.app.screens.SettingsScreen
import com.smarttracker.app.screens.SignInScreen
import com.smarttracker.app.screens.SignInUpScreen
import com.smarttracker.app.screens.SyncDevicesToUsersScreen
import com.smarttracker.app.screens.TodayTrailScreen
import com.smarttracker.app.screens.TrackingScreen
import com.smarttracker.app.screens.TrailListScreen
import com.smarttracker.app.screens.WebAuthnDemoScreen
import com.smarttracker.app.screens.WebAuthnFallbackScreen

/**
 * AppNavigation sets up all screen routes and how they pass data (arguments).
 * It also wires up sign-in and sign-up logic via callbacks from MainActivity.
 */
@SuppressLint("ContextCastToActivity")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation(
    signUpUser: (
        email: String,
        password: String,
        fullName: String,
        phoneNumber: String,
        role: String,
        onSuccess: (String, String) -> Unit
    ) -> Unit,

    signInUser: (String, String, (String, String) -> Unit) -> Unit,
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = "authScreen"
    ) {

        // 🚪 Entry screen (sign-up or go to login)
        composable("authScreen") {
            SignInUpScreen(
                signUpUser = signUpUser,
                navController = navController
            )
        }

        // 🔐 Sign-in screen
        composable("signInScreen") {
            SignInScreen(
                signInUser = { email, password, onResults ->
                    signInUser(email, password) { message, _ ->
                        onResults(message, email) // 👈 use email as navigation ID
                    }
                },
                navController = navController
            )
        }

        // 🏠 Home screen — now uses userEmail instead of UID
        composable(
            route = "homeScreen/{userEmail}/{firebaseUid}",
            arguments = listOf(
                navArgument("userEmail") { type = NavType.StringType },
                navArgument("firebaseUid") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userEmail = backStackEntry.arguments?.getString("userEmail") ?: ""
            val firebaseUid = backStackEntry.arguments?.getString("firebaseUid") ?: ""

            HomeScreen(
                userEmail = userEmail,
                firebaseUid = firebaseUid,
                navController = navController
            )
        }


        composable("webauthn_demo") {
            WebAuthnDemoScreen()
        }



        composable("createPasskeyScreen/{email}/{userId}") { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            CreatePasskeyScreen(email = email, userId = userId, navController = navController)
        }









        // 🔐 Backup recovery screen
        composable("backupCodeScreen") {
            BackupCodeScreen(navController = navController)
        }

        // ⚙️ Settings screen
        composable(
            route = "settingsScreen/{firebaseUid}",
            arguments = listOf(navArgument("firebaseUid") { type = NavType.StringType })
        ) { backStackEntry ->
            val firebaseUid = backStackEntry.arguments?.getString("firebaseUid") ?: ""
            SettingsScreen(navController, firebaseUid)
        }
        // 🔮 Predictive tracking screen
        composable(
            route = "predictiveTrackingScreen/{firebaseUid}",
            arguments = listOf(navArgument("firebaseUid") { type = NavType.StringType })
        ) { backStackEntry ->
            val firebaseUid = backStackEntry.arguments?.getString("firebaseUid") ?: ""
            PredictiveTrackingScreen(navController = navController, firebaseUid = firebaseUid)
        }
        // 📜 Privacy policy screen
        composable("privacyPolicyScreen") {
            PrivacyPolicyScreen(navController = navController)
        }




        // 🌐 WebAuthn fallback screen
        composable("webAuthnFallbackScreen") {
            WebAuthnFallbackScreen(navController)
        }

        // 🛰️ Limited tracking screen with Firebase UID (used for trails or emergency)
        composable(
            route = "limitedTrackingScreen/{firebaseUid}",
            arguments = listOf(navArgument("firebaseUid") { type = NavType.StringType })
        ) { backStackEntry ->
            val firebaseUid = backStackEntry.arguments?.getString("firebaseUid") ?: ""
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            LimitedTrackingScreen(userId = userId, firebaseUid = firebaseUid, navController = navController)
        }

        // ➕ Add new device — pass email, not UID
        composable(
            route = "addDeviceScreen/{userEmail}",
            arguments = listOf(navArgument("userEmail") { type = NavType.StringType })
        ) { backStackEntry ->
            val userEmail = backStackEntry.arguments?.getString("userEmail") ?: ""
            AddDeviceScreen(navController = navController, userId = userEmail)
        }

        // 📱 Device details screen
        composable(
            route = "deviceDetails/{deviceId}?isDelegate={isDelegate}&userEmail={userEmail}",
            arguments = listOf(
                navArgument("deviceId") { type = NavType.StringType },
                navArgument("isDelegate") { type = NavType.BoolType; defaultValue = false },
                navArgument("userEmail") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: ""
            val isDelegate = backStackEntry.arguments?.getBoolean("isDelegate") == true
            val userEmail = backStackEntry.arguments?.getString("userEmail") ?: ""

            DeviceDetailsScreen(
                deviceId = deviceId,
                isDelegate = isDelegate,
                userEmail = userEmail,
                navController = navController
            )
        }


        composable("manageTrustedContacts/{deviceId}") { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: ""
            ManageTrustedContactsScreen(deviceId, navController)
        }


        // 🔁 Developer sync screen
        composable("syncDevicesScreen") {
            SyncDevicesToUsersScreen()
        }

        composable(
            "createPasskeyScreen/{email}/{userId}"
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            CreatePasskeyScreen(
                email = email,
                userId = userId,
                navController = navController
            )
        }

        composable("resetPasswordScreen/{email}") { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            ResetPasswordScreen(email = email, navController = navController)
        }








        // 📊 All trail records for a Firebase UID
        composable("trailList/{firebaseUid}") { backStackEntry ->
            val firebaseUid = backStackEntry.arguments?.getString("firebaseUid") ?: ""
            TrailListScreen(navController, firebaseUid)
        }

        // 📍 Specific day trail screen
        composable("trail/{firebaseUid}/{date}") { backStackEntry ->
            val firebaseUid = backStackEntry.arguments?.getString("firebaseUid") ?: ""
            val date = backStackEntry.arguments?.getString("date") ?: ""
            TodayTrailScreen(navController, firebaseUid, date)
        }

        // 🛰️ Tracking screen for real-time movement
        composable(
            route = "trackingScreen/{firebaseUid}?emergency={emergency}",
            arguments = listOf(
                navArgument("firebaseUid") { type = NavType.StringType },
                navArgument("emergency") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val firebaseUid = backStackEntry.arguments?.getString("firebaseUid") ?: ""
            TrackingScreen(firebaseUid = firebaseUid, navController = navController)
        }
    }
}


// 🚧 Placeholder for future use if needed
@Composable
fun TrailDetailScreen(uid: String, date: String) {
    TODO("Not yet implemented")
}
