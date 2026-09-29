package com.xhooklab;

import android.app.Application;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public final class HookEngine {
    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);
    private static volatile List<RuleManager.Rule> ACTIVE = new ArrayList<>();
    private static volatile String PKG = "";

    private HookEngine() {}

    public static synchronized void applyAll(Application app, String pkg) {
        PKG = pkg;
        List<RuleManager.Rule> rules = RuleManager.forPackage(RuleManager.load(), pkg);
        ACTIVE = rules;
        RuntimeRules.rebuild(rules);

        if (INSTALLED.compareAndSet(false, true)) {
            HookEngine.installJavaHooks(rules);
            try { NativeHook.applyAll(rules); } catch (Throwable t) { LogRing.add("NATIVE", "applyAll: " + t); }
        } else {
            LogRing.add("ENGINE", "already installed, refresh table only");
        }
    }

    private static void installJavaHooks(List<RuleManager.Rule> rules) {
        for (RuleManager.Rule r : rules) {
            if (r.hookType == RuleManager.HookType.NATIVE) continue;
            if (r.hookType == RuleManager.HookType.FIELD) {
                FieldHook.install(PKG, r.clazz, r.method, r.paramTypes, r.key());
                continue;
            }
            installOne(r);
        }
    }

    private static void installOne(final RuleManager.Rule r) {
        try {
            Class<?> c = XposedHelpers.findClass(r.clazz, null);
            Object[] params = buildParams(r);
            XC_MethodHook cb = new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam p) throws Throwable {
                    RuntimeRules.Value v = RuntimeRules.get(r.key());
                    if (v == null || !v.enabled) return;
                    LogRing.add("HOOK", r.clazz + "#" + r.method + " [" + r.hookType + "]");
                    switch (r.hookType) {
                        case REPLACE_ARGS:
                            p.args = parseArgs(v.value, p.args);
                            break;
                        case REPLACE:
                            p.setResult(v.coerce(returnType(r)));
                            break;
                        case THROW:
                            p.setThrowable(new RuntimeException(v.value));
                            break;
                        case CALL_STACK:
                            LogRing.add("STACK", new Throwable().toString());
                            break;
                        default:
                            break;
                    }
                }
                @Override protected void afterHookedMethod(MethodHookParam p) throws Throwable {
                    RuntimeRules.Value v = RuntimeRules.get(r.key());
                    if (v == null || !v.enabled) return;
                    if (r.hookType == RuleManager.HookType.AFTER || r.hookType == RuleManager.HookType.REPLACE) {
                        p.setResult(v.coerce(returnType(r)));
                    }
                }
            };
            if (r.hookType == RuleManager.HookType.CONSTRUCTOR) {
                XposedBridge.hookAllConstructors(c, cb);
            } else {
                XposedBridge.hookAllMethods(c, r.method, cb);
            }
            LogRing.add("ENGINE", "hooked " + r.clazz + "#" + r.method + " (" + r.hookType + ")");
        } catch (Throwable t) {
            LogRing.add("ENGINE", "hook fail " + r.clazz + "#" + r.method + " : " + t);
        }
    }

    private static Object[] buildParams(RuleManager.Rule r) {
        return new Object[0];
    }

    private static Class<?> returnType(RuleManager.Rule r) {
        try {
            Class<?> c = XposedHelpers.findClass(r.clazz, null);
            for (java.lang.reflect.Method m : c.getDeclaredMethods()) {
                if (m.getName().equals(r.method)) return m.getReturnType();
            }
        } catch (Throwable ignored) { }
        return String.class;
    }

    private static Object[] parseArgs(String csv, Object[] origin) {
        if (csv == null || csv.isEmpty()) return origin;
        String[] parts = csv.split(",");
        Object[] out = new Object[parts.length];
        for (int i = 0; i < parts.length; i++) out[i] = parts[i].trim();
        return out;
    }

    public static void reload() {
        List<RuleManager.Rule> rules = RuleManager.forPackage(RuleManager.load(), PKG);
        ACTIVE = rules;
        RuntimeRules.rebuild(rules);
        LogRing.add("ENGINE", "reload -> " + rules.size() + " rules");
    }

    public static void reloadNativeDiff() {
        try { NativeHook.applyAll(ACTIVE); } catch (Throwable t) { LogRing.add("NATIVE", "diff: " + t); }
    }

    public static void reapply() {
        reload();
        reloadNativeDiff();
    }
}
