package com.xhooklab;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RuntimeRules {
    private static final Map<String, Value> TABLE = new ConcurrentHashMap<>();
    private static final Map<String, NativeSlot> NATIVE = new ConcurrentHashMap<>();

    private RuntimeRules() {}

    public static final class Value {
        public boolean enabled;
        public String value;
        public String extra;

        public Value(boolean enabled, String value, String extra) {
            this.enabled = enabled;
            this.value = value;
            this.extra = extra;
        }

        public Object coerce(Class<?> type) {
            if (type == null) return value;
            try {
                if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(value);
                if (type == int.class || type == Integer.class) return Integer.parseInt(value.trim());
                if (type == long.class || type == Long.class) return Long.parseLong(value.trim());
                if (type == double.class || type == Double.class) return Double.parseDouble(value.trim());
                if (type == float.class || type == Float.class) return Float.parseFloat(value.trim());
                if (type == short.class || type == Short.class) return Short.parseShort(value.trim());
                if (type == byte.class || type == Byte.class) return Byte.parseByte(value.trim());
                if (type == char.class || type == Character.class) return value.isEmpty() ? '\0' : value.charAt(0);
                if (type == String.class || type == CharSequence.class) return value;
            } catch (Throwable ignored) { }
            return value;
        }
    }

    public static final class NativeSlot {
        public long addr;   // 目标函数地址
        public long orig;   // 原始 trampoline 地址
        public boolean enabled;

        public NativeSlot(long addr, long orig, boolean enabled) {
            this.addr = addr;
            this.orig = orig;
            this.enabled = enabled;
        }
    }

    /* ---------- 通用值表 ---------- */

    public static Value get(String key) {
        return TABLE.get(key);
    }

    public static void put(String key, boolean enabled, String value, String extra) {
        TABLE.put(key, new Value(enabled, value, extra));
    }

    public static void clear() {
        TABLE.clear();
    }

    public static int size() {
        return TABLE.size();
    }

    public static List<String> keys() {
        return new ArrayList<>(TABLE.keySet());
    }

    /** 从规则列表刷新实时表（reload / 热改后调用） */
    public static void rebuild(List<RuleManager.Rule> rules) {
        TABLE.clear();
        for (RuleManager.Rule r : rules) {
            if (r.hookType == RuleManager.HookType.NATIVE) continue;
            TABLE.put(r.key(), new Value(r.enabled, r.value, r.script));
        }
    }

    /* ---------- Native 槽位表 ---------- */

    public static NativeSlot getNative(String key) {
        return NATIVE.get(key);
    }

    public static void bindNative(String key, long orig, long addr) {
        NativeSlot old = NATIVE.get(key);
        boolean enabled = old != null ? old.enabled : true;
        NATIVE.put(key, new NativeSlot(addr, orig, enabled));
    }

    public static void putNative(String key, NativeSlot slot) {
        NATIVE.put(key, slot);
    }

    public static Collection<NativeSlot> nativeSlots() {
        return NATIVE.values();
    }

    public static void clearNative() {
        NATIVE.clear();
    }
}
