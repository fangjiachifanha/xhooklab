package com.xhooklab;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class RuleManager {
    public static final String RULES_PATH = "/data/adb/xhooklab/rules.json";

    public enum HookType {
        BEFORE, AFTER, REPLACE, REPLACE_ARGS, THROW, CALL_STACK, CONSTRUCTOR, FIELD, NATIVE;

        public static HookType of(String s) {
            if (s == null) return BEFORE;
            try { return valueOf(s.trim().toUpperCase()); }
            catch (Throwable t) { return BEFORE; }
        }
    }

    public static final class Rule {
        public String pkg = "*";
        public String clazz = "";
        public String method = "";
        public String paramTypes = "";
        public HookType hookType = HookType.BEFORE;
        public boolean enabled = true;
        public String value = "";
        public String script = "";

        public String key() {
            return pkg + "#" + clazz + "#" + method + "#" + hookType.name();
        }

        public static Rule from(JSONObject o) {
            Rule r = new Rule();
            r.pkg = o.optString("pkg", "*");
            r.clazz = o.optString("clazz", "");
            r.method = o.optString("method", "");
            r.paramTypes = o.optString("paramTypes", "");
            r.hookType = HookType.of(o.optString("hookType", "BEFORE"));
            r.enabled = o.optBoolean("enabled", true);
            r.value = o.optString("value", "");
            r.script = o.optString("script", "");
            return r;
        }
    }

    private RuleManager() {}

    public static List<Rule> load() {
        List<Rule> out = new ArrayList<>();
        File f = new File(RULES_PATH);
        if (!f.exists()) {
            LogRing.add("RULE", "no rules file, empty");
            return out;
        }
        try (FileInputStream in = new FileInputStream(f)) {
            byte[] buf = new byte[(int) f.length()];
            int n = in.read(buf);
            String json = new String(buf, 0, Math.max(n, 0), StandardCharsets.UTF_8);
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) out.add(Rule.from(arr.getJSONObject(i)));
            LogRing.add("RULE", "loaded " + out.size() + " rules");
        } catch (Throwable t) {
            LogRing.add("RULE", "parse fail: " + t);
        }
        return out;
    }

    public static List<Rule> forPackage(List<Rule> all, String pkg) {
        List<Rule> out = new ArrayList<>();
        for (Rule r : all) {
            if ("*".equals(r.pkg) || pkg.equals(r.pkg)) out.add(r);
        }
        return out;
    }
}
