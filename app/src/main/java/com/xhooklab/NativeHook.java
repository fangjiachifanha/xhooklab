package com.xhooklab;

import java.util.List;

import de.robv.android.xposed.XposedBridge;

public final class NativeHook {
    private NativeHook() {}

    static {
        try {
            System.loadLibrary("xhooklab");
        } catch (Throwable t) {
            LogRing.add("NATIVE", "load fail: " + t);
        }
    }

    public static native long hookSymbol(String libName, String symbol, long newAddr);
    public static native boolean unhook(long addr);
    public static native long resolve(String libName, String symbol);
    public static native String info();

    public static void applyAll(List<RuleManager.Rule> rules) {
        NativeHook.unhookAll();
        for (RuleManager.Rule r : rules) {
            if (!r.enabled || r.hookType != RuleManager.HookType.NATIVE) continue;
            try {
                long target = resolve(r.clazz, r.method);
                if (target == 0L) {
                    LogRing.add("NATIVE", "resolve miss " + r.clazz + "!" + r.method);
                    continue;
                }
                RuntimeRules.NativeSlot slot = RuntimeRules.getNative(r.key());
                if (slot == null || !slot.enabled) continue;
                long orig = hookSymbol(r.clazz, r.method, target);
                RuntimeRules.bindNative(r.key(), orig, target);
                LogRing.add("NATIVE", "hooked " + r.clazz + "!" + r.method + " @0x" + Long.toHexString(target));
            } catch (Throwable t) {
                LogRing.add("NATIVE", "hook fail " + r.clazz + "!" + r.method + " : " + t);
            }
        }
    }

    private static void unhookAll() {
        for (RuntimeRules.NativeSlot s : RuntimeRules.nativeSlots()) {
            try { if (s.addr != 0L) unhook(s.addr); } catch (Throwable ignored) { }
        }
        RuntimeRules.clearNative();
    }
}
