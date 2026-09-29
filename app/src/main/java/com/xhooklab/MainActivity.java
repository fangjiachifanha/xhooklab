package com.xhooklab;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private TextView out;
    private final Handler ui = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (getResources().getDisplayMetrics().density * 12);
        root.setPadding(pad, pad, pad, pad);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);

        Button reload = new Button(this);
        reload.setText("Reload");
        Button apply = new Button(this);
        apply.setText("Reapply");
        Button logs = new Button(this);
        logs.setText("Logs");
        Button clr = new Button(this);
        clr.setText("Clear");

        bar.addView(reload);
        bar.addView(apply);
        bar.addView(logs);
        bar.addView(clr);
        root.addView(bar);

        out = new TextView(this);
        out.setTextSize(11f);
        out.setText("XHookLab ready.\nport=" + ControlServer.pickPort()
                + "\nrules=" + RuntimeRules.size()
                + "\n\n模块已激活，控制接口:\n  http://127.0.0.1:" + ControlServer.pickPort() + "/ping\n");
        ScrollView sc = new ScrollView(this);
        sc.addView(out);
        root.addView(sc, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        reload.setOnClickListener(v -> {
            HookEngine.reload();
            toast("reload done");
            refresh();
        });
        apply.setOnClickListener(v -> {
            HookEngine.reapply();
            toast("reapply done");
            refresh();
        });
        logs.setOnClickListener(v -> refresh());
        clr.setOnClickListener(v -> { LogRing.clear(); refresh(); });

        setContentView(root);
        refresh();
    }

    private void refresh() {
        ui.post(() -> out.setText("XHookLab  port=" + ControlServer.pickPort()
                + "  rules=" + RuntimeRules.size()
                + "\n\n" + LogRing.dumpText()));
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
