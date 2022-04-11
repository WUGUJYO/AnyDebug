package com.hhvvg.anydebug.hook

import android.content.res.Resources
import android.content.res.XModuleResources
import com.hhvvg.anydebug.BuildConfig
import com.hhvvg.anydebug.hook.hookers.ActivityInspectHooker
import com.hhvvg.anydebug.hook.hookers.GlobalControlReceiverHooker
import com.hhvvg.anydebug.hook.hookers.PupupWindowHooker
import com.hhvvg.anydebug.hook.hookers.TextViewHooker
import com.hhvvg.anydebug.hook.hookers.ViewBoundsHooker
import com.hhvvg.anydebug.hook.hookers.ViewClickHooker
import com.hhvvg.anydebug.hook.hookers.ViewRulesLoader
import com.hhvvg.anydebug.hook.hookers.ViewVisibilityHooker
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Class for application hook.
 *
 * @author hhvvg
 */
class AnyHookFramework : IXposedHookLoadPackage, IXposedHookZygoteInit {
    private val hookers: ArrayList<IHooker> = arrayListOf(
        ViewClickHooker(),
        PupupWindowHooker(),
        GlobalControlReceiverHooker(),
        ViewRulesLoader(),
        ViewVisibilityHooker(),
        TextViewHooker(),
        ViewBoundsHooker(),
        ActivityInspectHooker(),
    )

    override fun handleLoadPackage(p0: XC_LoadPackage.LoadPackageParam) {
        // Don't hook itself
        val packageName = p0.packageName
        if (packageName == BuildConfig.PACKAGE_NAME) {
            return
        }
        hookers.forEach {
            it.onHook(param = p0)
        }
    }

    override fun initZygote(p0: IXposedHookZygoteInit.StartupParam) {
        modulePath = p0.modulePath
        moduleRes = getModuleRes(modulePath)
    }

    companion object {
        @JvmStatic
        lateinit var moduleRes: Resources

        @JvmStatic
        lateinit var modulePath: String

        @JvmStatic
        fun getModuleRes(path: String): Resources {
            return XModuleResources.createInstance(path, null)
        }
    }
}
