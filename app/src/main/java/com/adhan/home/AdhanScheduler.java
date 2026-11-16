package com.adhan.home;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class AdhanScheduler {
    public static void scheduleNext(Context c) {
        try {
            PrayerTimes.Next n = PrayerTimes.next(System.currentTimeMillis());
            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            Intent i = new Intent(c, AdhanReceiver.class).putExtra("idx", n.idx);
            PendingIntent pi = PendingIntent.getBroadcast(c, 1, i,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            PendingIntent show = PendingIntent.getActivity(c, 2, new Intent(c, MainActivity.class),
                    PendingIntent.FLAG_IMMUTABLE);
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(n.time, show), pi);
        } catch (Exception ignored) {
        }
    }
}
