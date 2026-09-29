package com.xhooklab;

import android.app.Application;
import android.content.Context;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XHookEntry implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    @Override
    public void initZygote(StartupParam sp) {
        LogRing.add("Zygote", "initZygote pid=" + android.os.Process.myPid());
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) {
        if (lp == null || lp.packageName == null) return;
        if ("com.xhooklab".equals(lp.packageName)) {
            ControlServer.setContext(android.app.AndroidAppHelper.currentApplication());
            return;
        }
        try {
            XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    Application app = (Application) param.thisObject;
                    onAppReady(app, lp.packageName, lp.processName);
                }
            });
        } catch (Throwable t) {
            LogRing.add("ENTRY", "attach hook fail: " + t);
        }
    }

    private void onAppReady(Application app, String pkg, String proc) {
        LogRing.add("ENTRY", "app ready pkg=" + pkg + " proc=" + proc);
        ControlServer.setContext(app);
        ControlServer.start();
        try { HookEngine.applyAll(app, pkg); } catch (Throwable t) { LogRing.add("ENTRY", "applyAll: " + t); }
    }
}
