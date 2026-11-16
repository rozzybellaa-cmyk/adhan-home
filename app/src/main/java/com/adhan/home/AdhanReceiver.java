package com.adhan.home;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class AdhanReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        int idx = intent.getIntExtra("idx", 0);
        Intent s = new Intent(ctx, AdhanService.class).putExtra("idx", idx);
        try {
            if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(s);
            else ctx.startService(s);
        } catch (Exception ignored) {
        }
        AdhanScheduler.scheduleNext(ctx);
    }
}
