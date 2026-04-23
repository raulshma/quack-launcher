package com.raulshma.minkoa

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import android.app.role.RoleManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.raulshma.minkoa.data.SlotContent
import com.raulshma.minkoa.launcher.QuackLauncherRoot
import com.raulshma.minkoa.ui.theme.QuackLauncherTheme
import com.raulshma.minkoa.widget.LauncherWidgetHostController

class MainActivity : ComponentActivity() {
    lateinit var widgetHostController: LauncherWidgetHostController

    var pendingWidgetBind by mutableStateOf<PendingWidgetBind?>(null)
        private set

    private val requestHomeRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        widgetHostController = LauncherWidgetHostController(this)

        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        setContent {
            val bindLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                val pending = pendingWidgetBind
                if (pending != null && result.resultCode == RESULT_OK) {
                    pending.onBound(
                        SlotContent.Widget(
                            appWidgetId = pending.appWidgetId,
                            providerPkg = pending.provider.packageName,
                            providerCls = pending.provider.className
                        )
                    )
                } else {
                    val pending2 = pendingWidgetBind
                    if (pending2 != null) {
                        widgetHostController.deleteAppWidgetId(pending2.appWidgetId)
                    }
                    pending2?.onBound(null)
                }
                pendingWidgetBind = null
            }

            QuackLauncherTheme {
                QuackLauncherRoot(
                    widgetHostController = widgetHostController,
                    requestWidgetBind = { appWidgetId, provider, onResult ->
                        val bound =
                            widgetHostController.bindAppWidgetIdIfAllowed(
                                appWidgetId,
                                provider
                            )
                        if (bound) {
                            onResult(
                                SlotContent.Widget(
                                    appWidgetId = appWidgetId,
                                    providerPkg = provider.packageName,
                                    providerCls = provider.className
                                )
                            )
                        } else {
                            pendingWidgetBind = PendingWidgetBind(
                                appWidgetId = appWidgetId,
                                provider = provider,
                                onBound = onResult
                            )
                            bindLauncher.launch(
                                widgetHostController.createBindIntent(
                                    appWidgetId,
                                    provider
                                )
                            )
                        }
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val roleManager = getSystemService(RoleManager::class.java)
        if (roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
            !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
        ) {
            requestHomeRoleLauncher.launch(
                roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
            )
        }
    }

    override fun onStart() {
        super.onStart()
        widgetHostController.startListening()
    }

    override fun onStop() {
        widgetHostController.stopListening()
        super.onStop()
    }
}

data class PendingWidgetBind(
    val appWidgetId: Int,
    val provider: ComponentName,
    val onBound: (SlotContent.Widget?) -> Unit
)
