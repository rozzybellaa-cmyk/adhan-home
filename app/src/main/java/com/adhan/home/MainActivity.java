package com.adhan.home;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class MainActivity extends Activity {
    static final int BG = Color.parseColor("#0b1d26");
    static final int CARD = Color.parseColor("#12303c");
    static final int ON = Color.parseColor("#16505a");
    static final int ACC = Color.parseColor("#2dd4bf");
    static final int MUTE = Color.parseColor("#8fb0ba");
    static final TimeZone RIY = TimeZone.getTimeZone("Asia/Riyadh");

    private final Handler h = new Handler(Looper.getMainLooper());
    private TextView clock, ap, greg, hijri, temp, nextTv, cd, adhanName;
    private final TextView[] pTime = new TextView[6];
    private final LinearLayout[] cards = new LinearLayout[6];
    private View overlay;
    private int lastIdx = -1;
    private android.icu.text.SimpleDateFormat hijriF, gregF;

    static String ar(String s) {
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) b.append(c >= '0' && c <= '9' ? (char) ('\u0660' + (c - '0')) : c);
        return b.toString();
    }

    static String pad(long n) { return n < 10 ? "0" + n : String.valueOf(n); }

    static String fmt(long ms) {
        Calendar c = Calendar.getInstance(RIY);
        c.setTimeInMillis(ms);
        int hh = c.get(Calendar.HOUR);
        if (hh == 0) hh = 12;
        int mm = c.get(Calendar.MINUTE);
        return ar(hh + ":" + pad(mm)) + (c.get(Calendar.AM_PM) == Calendar.AM ? " ص" : " م");
    }

    int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    TextView tv(String text, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    GradientDrawable box(int color, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        return g;
    }

    LinearLayout.LayoutParams lp(int w, int hh, float wt, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, hh, wt);
        p.setMargins(dp(l), dp(t), dp(r), dp(b));
        return p;
    }

    TextView appButton(String label, final String key, final String[] exclude) {
        final TextView b = tv(label, 30, Color.WHITE, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(40), dp(16), dp(40), dp(16));
        b.setBackground(box(CARD, 14));
        b.setFocusable(true);
        b.setClickable(true);
        b.setOnFocusChangeListener((v, f) -> {
            b.setBackground(box(f ? ACC : CARD, 14));
            b.setTextColor(f ? Color.parseColor("#04201c") : Color.WHITE);
        });
        b.setOnClickListener(v -> {
            Intent i = findApp(key, exclude);
            if (i == null) {
                Toast.makeText(this, "التطبيق غير موجود", Toast.LENGTH_SHORT).show();
            } else {
                try { startActivity(i); } catch (Exception e) {
                    Toast.makeText(this, "تعذر فتح التطبيق", Toast.LENGTH_SHORT).show();
                }
            }
        });
        return b;
    }

    Intent findApp(String key, String[] exclude) {
        PackageManager pm = getPackageManager();
        String[] cats = {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER};
        for (String cat : cats) {
            List<ResolveInfo> l = pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(cat), 0);
            for (ResolveInfo ri : l) {
                if (getPackageName().equals(ri.activityInfo.packageName)) continue;
                String label = String.valueOf(ri.loadLabel(pm)).toLowerCase();
                boolean bad = false;
                for (String x : exclude) if (label.contains(x)) bad = true;
                if (!bad && label.contains(key)) {
                    return new Intent(Intent.ACTION_MAIN)
                            .setClassName(ri.activityInfo.packageName, ri.activityInfo.name)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
            }
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

        try {
            hijriF = new android.icu.text.SimpleDateFormat("EEEE d MMMM y",
                    new android.icu.util.ULocale("ar_SA@calendar=islamic-umalqura;numbers=arab"));
            hijriF.setTimeZone(android.icu.util.TimeZone.getTimeZone("Asia/Riyadh"));
            gregF = new android.icu.text.SimpleDateFormat("d MMMM y",
                    new android.icu.util.ULocale("ar_SA@calendar=gregorian;numbers=arab"));
            gregF.setTimeZone(android.icu.util.TimeZone.getTimeZone("Asia/Riyadh"));
        } catch (Throwable ignored) { }

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(BG);

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(dp(40), dp(24), dp(40), dp(24));

        // top: clock (left) + dates & temperature (right)
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.HORIZONTAL);
        left.setGravity(Gravity.BOTTOM);
        clock = tv("", 96, Color.WHITE, true);
        ap = tv("", 28, MUTE, false);
        left.addView(clock);
        left.addView(ap);
        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setGravity(Gravity.END);
        greg = tv("", 22, MUTE, false);
        hijri = tv("", 22, MUTE, false);
        temp = tv("--°", 48, Color.WHITE, true);
        right.addView(greg);
        right.addView(hijri);
        right.addView(temp);
        top.addView(left, lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, 0, 0, 0, 0));
        top.addView(right, lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 0, 0, 0, 0));
        main.addView(top, lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 0, 0, 0, 0));

        // next prayer bar
        LinearLayout next = new LinearLayout(this);
        next.setOrientation(LinearLayout.HORIZONTAL);
        next.setGravity(Gravity.CENTER_VERTICAL);
        next.setBackground(box(CARD, 14));
        next.setPadding(dp(24), dp(14), dp(24), dp(14));
        nextTv = tv("", 28, Color.WHITE, false);
        cd = tv("", 34, ACC, true);
        next.addView(nextTv, lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, 0, 0, 0, 0));
        next.addView(cd);
        main.addView(next, lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 0, 16, 0, 0));

        // prayer cards (RTL so Fajr is on the right)
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        for (int i = 0; i < 6; i++) {
            LinearLayout c = new LinearLayout(this);
            c.setOrientation(LinearLayout.VERTICAL);
            c.setGravity(Gravity.CENTER);
            c.setBackground(box(CARD, 14));
            TextView n = tv(PrayerTimes.NAMES[i], 24, MUTE, false);
            pTime[i] = tv("", 30, Color.WHITE, true);
            c.addView(n);
            c.addView(pTime[i]);
            cards[i] = c;
            row.addView(c, lp(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f, 6, 0, 6, 0));
        }
        main.addView(row, lp(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f, 0, 16, 0, 16));

        // the two apps
        LinearLayout apps = new LinearLayout(this);
        apps.setOrientation(LinearLayout.HORIZONTAL);
        apps.setGravity(Gravity.CENTER);
        TextView yt = appButton("YouTube", "youtube", new String[]{"music", "kids", "studio"});
        TextView stc = appButton("stc tv", "stc", new String[]{});
        apps.addView(yt, lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 12, 0, 12, 0));
        apps.addView(stc, lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 12, 0, 12, 0));
        main.addView(apps, lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 0, 0, 0, 0));

        root.addView(main, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // adhan overlay: covers everything while the adhan plays
        LinearLayout ov = new LinearLayout(this);
        ov.setOrientation(LinearLayout.VERTICAL);
        ov.setGravity(Gravity.CENTER);
        ov.setBackgroundColor(Color.parseColor("#F2050F14"));
        ov.setClickable(true);
        ov.setFocusable(true);
        adhanName = tv("", 90, ACC, true);
        TextView sub = tv("حان الآن موعد الأذان", 40, Color.WHITE, false);
        ov.addView(adhanName);
        ov.addView(sub);
        ov.setVisibility(View.GONE);
        overlay = ov;
        root.addView(ov, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        setContentView(root);
        yt.requestFocus();
    }

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            try { update(); } catch (Exception ignored) { }
            h.postDelayed(this, 1000);
        }
    };

    void update() {
        long now = System.currentTimeMillis();
        Calendar c = Calendar.getInstance(RIY);
        c.setTimeInMillis(now);
        int hh = c.get(Calendar.HOUR);
        if (hh == 0) hh = 12;
        clock.setText(ar(hh + ":" + pad(c.get(Calendar.MINUTE))));
        ap.setText(c.get(Calendar.AM_PM) == Calendar.AM ? " ص" : " م");
        try {
            if (gregF != null) greg.setText(gregF.format(new Date(now)));
            if (hijriF != null) hijri.setText(hijriF.format(new Date(now)));
        } catch (Throwable ignored) { }

        long[] t = PrayerTimes.forDay(now, 0);
        for (int i = 0; i < 6; i++) pTime[i].setText(fmt(t[i]));
        PrayerTimes.Next n = PrayerTimes.next(now);
        if (n.idx != lastIdx) {
            lastIdx = n.idx;
            for (int i = 0; i < 6; i++) cards[i].setBackground(box(i == n.idx ? ON : CARD, 14));
        }
        nextTv.setText("الصلاة القادمة: " + PrayerTimes.NAMES[n.idx] + " · " + fmt(n.time));
        long s = Math.max(0, (n.time - now) / 1000);
        cd.setText(ar(pad(s / 3600) + ":" + pad(s % 3600 / 60) + ":" + pad(s % 60)));

        if (AdhanService.playing) {
            adhanName.setText(AdhanService.name);
            overlay.setVisibility(View.VISIBLE);
        } else {
            overlay.setVisibility(View.GONE);
        }
    }

    void fetchTemp() {
        new Thread(() -> {
            try {
                URL u = new URL("https://api.open-meteo.com/v1/forecast?latitude=24.7136&longitude=46.6753&current=temperature_2m");
                HttpURLConnection con = (HttpURLConnection) u.openConnection();
                con.setConnectTimeout(8000);
                con.setReadTimeout(8000);
                BufferedReader r = new BufferedReader(new InputStreamReader(con.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
                r.close();
                double v = new JSONObject(sb.toString()).getJSONObject("current").getDouble("temperature_2m");
                final String txt = ar(String.valueOf(Math.round(v))) + "°";
                runOnUiThread(() -> temp.setText(txt));
            } catch (Exception ignored) { }
        }).start();
        h.postDelayed(this::fetchTemp, 30 * 60 * 1000L);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AdhanScheduler.scheduleNext(this);
        h.removeCallbacksAndMessages(null);
        h.post(tick);
        fetchTemp();
    }

    @Override
    protected void onPause() {
        h.removeCallbacksAndMessages(null);
        super.onPause();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (AdhanService.playing) return true;
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        // home screen: back does nothing
    }
}
