package com.xhooklab;

import java.lang.reflect.Field;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public final class FieldHook {
    private FieldHook() {}

    public static void install(String pkg, final String clazz, final String fieldName, final String type, final String key) {
        try {
            Class<?> c = XposedHelpers.findClass(clazz, null);
            final Field f = XposedHelpers.findField(c, fieldName);
            f.setAccessible(true);

            for (final String m : new String[]{"get", "getValue", "read"}) {
                try {
                    XposedHelpers.findAndHookMethod(c, m, new XC_MethodHook() {
                        @Override protected void afterHookedMethod(MethodHookParam param) {
                            RuntimeRules.Value v = RuntimeRules.get(key);
                            if (v != null && v.enabled) {
                                param.setResult(v.coerce(f.getType()));
                                LogRing.add("FIELD", clazz + "#" + fieldName + " get -> " + v.value);
                            }
                        }
                    });
                } catch (Throwable ignored) { }
            }
            LogRing.add("FIELD", "installed " + clazz + "#" + fieldName + " (" + type + ")");
        } catch (Throwable t) {
            LogRing.add("FIELD", "fail " + clazz + "#" + fieldName + " : " + t);
        }
    }
}
