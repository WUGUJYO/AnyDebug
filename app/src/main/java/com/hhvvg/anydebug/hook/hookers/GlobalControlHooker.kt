package com.hhvvg.anydebug.hook.hookers

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import com.hhvvg.anydebug.hook.IHooker
import com.hhvvg.anydebug.ui.fragment.SettingsFragment.Companion.ACTION_ENABLE
import com.hhvvg.anydebug.ui.fragment.SettingsFragment.Companion.ACTION_GLOBAL_ENABLE
import com.hhvvg.anydebug.ui.fragment.SettingsFragment.Companion.ACTION_PERSISTENT_ENABLE
import com.hhvvg.anydebug.ui.fragment.SettingsFragment.Companion.EXTRA_CONTROL_ACTION
import com.hhvvg.anydebug.util.ACTIVITY_FIELD_GLOBAL_ENABLE
import com.hhvvg.anydebug.util.APP_FIELD_FORCE_CLICKABLE
import com.hhvvg.anydebug.util.APP_FIELD_GLOBAL_CONTROL_ENABLED
import com.hhvvg.anydebug.util.APP_FIELD_PERSISTENT_ENABLE
import com.hhvvg.anydebug.util.getInjectedField
import com.hhvvg.anydebug.util.injectField
import com.hhvvg.anydebug.util.setAllViewsHookClick
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * @author hhvvg
 *
 * Hook for listening config change broadcast.
 */
class GlobalControlHooker : IHooker {
    override fun onHook(param: XC_LoadPackage.LoadPackageParam) {
        val methodHook = OnAppCreateMethodHook()
        val method = XposedHelpers.findMethodBestMatch(
            Application::class.java,
            "onCreate",
            arrayOf(),
            arrayOf()
        )
        XposedBridge.hookMethod(method, methodHook)
    }

    private class OnAppCreateMethodHook : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            val app = param.thisObject as Application
            // Disable by default
            app.injectField(APP_FIELD_GLOBAL_CONTROL_ENABLED, false)
            app.injectField(APP_FIELD_PERSISTENT_ENABLE, false)

            registerReceiverForActivity(app)
            registerReceiverForApp(app)
        }

        private fun registerReceiverForApp(app: Application) {
            val persistentEnableReceiver = PersistentEnableReceiver()
            val filter = IntentFilter(ACTION_PERSISTENT_ENABLE)
            app.registerReceiver(persistentEnableReceiver, filter)
        }

        private fun registerReceiverForActivity(app: Application) {
            val callback =
                XposedHelpers.findField(Application::class.java, "mActivityLifecycleCallbacks")
            val callbackArray =
                callback.get(app) as ArrayList<Application.ActivityLifecycleCallbacks>
            callbackArray.add(ActivityCallback())
        }
    }

    private class ActivityCallback : Application.ActivityLifecycleCallbacks {
        override fun onActivityPostCreated(activity: Activity, savedInstanceState: Bundle?) {
            // Register this receiver for every activity
            val receiver = GlobalEnableReceiver(activity)
            activity.injectField(ACTIVITY_FIELD_GLOBAL_ENABLE, receiver)
            val filter = IntentFilter(ACTION_GLOBAL_ENABLE)
            activity.registerReceiver(receiver, filter)
        }

        override fun onActivityCreated(p0: Activity, p1: Bundle?) {
        }

        override fun onActivityStarted(p0: Activity) {
        }

        override fun onActivityResumed(activity: Activity) {
        }

        override fun onActivityPaused(p0: Activity) {
        }

        override fun onActivityStopped(p0: Activity) {
        }

        override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {
        }

        override fun onActivityDestroyed(activity: Activity) {
            // Don't forget to release it.
            val receiver = activity.getInjectedField<BroadcastReceiver>(ACTIVITY_FIELD_GLOBAL_ENABLE)
            receiver?.let {
                activity.unregisterReceiver(receiver)
            }
        }

    }

    private class GlobalEnableReceiver(private val activity: Activity) : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val decorView = activity.window.decorView
            val enabled = when (intent.getIntExtra(EXTRA_CONTROL_ACTION, ACTION_ENABLE)) {
                ACTION_ENABLE -> {
                    true
                }
                else -> {
                    false
                }
            }
            setGlobalEnable(decorView, enabled)
        }

        private fun setGlobalEnable(decorView: View, enabled: Boolean) {
            val app = AndroidAppHelper.currentApplication()
            app.injectField(APP_FIELD_GLOBAL_CONTROL_ENABLED, enabled)
            val forceClick = app.getInjectedField(APP_FIELD_FORCE_CLICKABLE, false) ?: false
            decorView.setAllViewsHookClick(
                enabled = enabled,
                traversalChildren = true,
                forceClickable = forceClick
            )
        }
    }

    private class PersistentEnableReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val enabled = when (intent.getIntExtra(EXTRA_CONTROL_ACTION, ACTION_ENABLE)) {
                ACTION_ENABLE -> {
                    true
                }
                else -> false
            }
            val app = AndroidAppHelper.currentApplication()
            app.injectField(APP_FIELD_PERSISTENT_ENABLE, enabled)
        }
    }
}
