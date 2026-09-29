package com.xhooklab;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public final class ControlServer {
    public static final int BASE_PORT = 19321;
    private static volatile Context CTX;
    private static volatile Thread SERVER;

    private ControlServer() {}

    public static void setContext(Context c) {
        CTX = c;
    }

    public static int pickPort() {
        String proc = procName();
        if (proc == null || proc.isEmpty() || proc.endsWith(":main") || !proc.contains(":")) {
            return BASE_PORT;
        }
        int h = 0;
        for (int i = 0; i < proc.length(); i++) h = h * 31 + proc.charAt(i);
        return BASE_PORT + 1 + (Math.abs(h) & 0xFF);
    }

    private static String procName() {
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(
                    new java.io.FileInputStream("/proc/self/cmdline"), StandardCharsets.UTF_8));
            String s = r.readLine();
            r.close();
            return s == null ? "" : s.replace("\u0000", "").trim();
        } catch (Throwable t) {
            return "";
        }
    }

    public static synchronized void start() {
        if (SERVER != null && SERVER.isAlive()) return;
        SERVER = new Thread(ControlServer::loop, "xhooklab-ctl");
        SERVER.setDaemon(true);
        SERVER.start();
    }

    private static void loop() {
        try (ServerSocket ss = new ServerSocket(pickPort())) {
            LogRing.add("CTL", "listening on " + ss.getLocalPort());
            while (true) {
                Socket s = ss.accept();
                handle(s);
            }
        } catch (Throwable t) {
            LogRing.add("CTL", "server dead: " + t);
        }
    }

    private static void handle(Socket s) {
        try {
            s.setSoTimeout(5000);
            BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            String req = r.readLine();
            String body = null;
            int len = 0;
            String line;
            while ((line = r.readLine()) != null && !line.isEmpty()) {
                if (line.toLowerCase().startsWith("content-length:")) {
                    len = Integer.parseInt(line.substring(15).trim());
                }
            }
            if (len > 0) {
                char[] buf = new char[len];
                int n = r.read(buf);
                body = new String(buf, 0, Math.max(n, 0));
            }

            String resp = route(req, body);
            byte[] out = resp.getBytes(StandardCharsets.UTF_8);
            OutputStream os = s.getOutputStream();
            os.write(("HTTP/1.1 200 OK\r\nContent-Type: application/json; charset=utf-8\r\nContent-Length: "
                    + out.length + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            os.write(out);
            os.flush();
        } catch (Throwable t) {
            LogRing.add("CTL", "handle: " + t);
        } finally {
            try { s.close(); } catch (Throwable ignored) { }
        }
    }

    private static String route(String req, String body) {
        if (req == null) return resp(false, "null request", null);
        String[] p = req.split(" ");
        String path = p.length > 1 ? p[1] : "/";

        if (path.startsWith("/ping")) return resp(true, "pong", "\"port\":" + pickPort());
        if (path.startsWith("/reload")) { HookEngine.reload(); return resp(true, "reloaded", "\"rules\":" + RuntimeRules.size()); }
        if (path.startsWith("/reapply")) { HookEngine.reapply(); return resp(true, "reapplied", "\"rules\":" + RuntimeRules.size()); }
        if (path.startsWith("/natives")) { HookEngine.reloadNativeDiff(); return resp(true, "native diff done", null); }
        if (path.startsWith("/logs")) return resp(true, "ok", "\"logs\":\"" + JsonUtil.esc(LogRing.dumpText()) + "\"");
        if (path.startsWith("/clear")) { LogRing.clear(); return resp(true, "cleared", null); }
        if (path.startsWith("/set") && body != null) return setRule(body);
        if (path.startsWith("/get")) return resp(true, "ok", "\"keys\":\"" + JsonUtil.esc(RuntimeRules.keys().toString()) + "\"");
        return resp(false, "unknown path: " + path, null);
    }

    private static String setRule(String body) {
        try {
            org.json.JSONObject o = new org.json.JSONObject(body);
            String key = o.optString("key", "");
            boolean enabled = o.optBoolean("enabled", true);
            String value = o.optString("value", "");
            if (key.isEmpty()) return resp(false, "empty key", null);
            RuntimeRules.put(key, enabled, value, "");
            return resp(true, "set ok", "\"key\":\"" + JsonUtil.esc(key) + "\",\"value\":\"" + JsonUtil.esc(value) + "\"");
        } catch (Throwable t) {
            return resp(false, "set fail: " + t, null);
        }
    }

    private static String resp(boolean ok, String msg, String extra) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"ok\":").append(ok).append(",\"msg\":\"").append(JsonUtil.esc(msg)).append("\"");
        if (extra != null && !extra.isEmpty()) sb.append(",").append(extra);
        sb.append("}");
        return sb.toString();
    }
}
