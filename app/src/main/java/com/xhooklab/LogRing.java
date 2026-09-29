package com.xhooklab;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

public final class LogRing {
    private static final int CAP = 500;
    private static final Deque<String> BUF = new ArrayDeque<>();
    private static final SimpleDateFormat FMT =
            new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US);

    private LogRing() {}

    public static synchronized void add(String tag, String msg) {
        String line = FMT.format(new Date()) + " [" + tag + "] " + msg;
        if (BUF.size() >= CAP) BUF.pollFirst();
        BUF.addLast(line);
    }

    public static synchronized List<String> dump() {
        return new ArrayList<>(BUF);
    }

    public static synchronized String dumpText() {
        StringBuilder sb = new StringBuilder();
        for (String s : BUF) sb.append(s).append('\n');
        return sb.toString();
    }

    public static synchronized void clear() {
        BUF.clear();
    }
}
