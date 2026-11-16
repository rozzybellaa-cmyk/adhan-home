package com.adhan.home;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

public class AdhanService extends Service {
    public static volatile boolean playing = false;
    public static volatile String name = "";

    private MediaPlayer mp;
    private AudioManager am;
    private AudioFocusRequest focusReq;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int idx = intent != null ? intent.getIntExtra("idx", 0) : 0;
        if (idx < 0 || idx >= PrayerTimes.NAMES.length) idx = 0;
        startFg();
        if (playing) return START_NOT_STICKY;
        playing = true;
        name = PrayerTimes.NAMES[idx];

        am = (AudioManager) getSystemService(AUDIO_SERVICE);
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                focusReq = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(attrs).build();
                am.requestAudioFocus(focusReq);
            } else {
                am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            }
            mp = new MediaPlayer();
            mp.setAudioAttributes(attrs);
            mp.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
            AssetFileDescriptor afd = getResources().openRawResourceFd(R.raw.adhan);
            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            mp.setOnCompletionListener(m -> finishAdhan());
            mp.setOnErrorListener((m, w, e) -> { finishAdhan(); return true; });
            mp.prepare();
            mp.start();
        } catch (Exception e) {
            finishAdhan();
            return START_NOT_STICKY;
        }

        try {
            Intent a = new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(a);
        } catch (Exception ignored) {
        }
        handler.postDelayed(this::finishAdhan, 10 * 60 * 1000L);
        return START_NOT_STICKY;
    }

    private void startFg() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel("adhan", "الأذان", NotificationManager.IMPORTANCE_LOW));
            b = new Notification.Builder(this, "adhan");
        } else {
            b = new Notification.Builder(this);
        }
        Notification n = b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("حان وقت الأذان").build();
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(1, n);
        }
    }

    private void finishAdhan() {
        handler.removeCallbacksAndMessages(null);
        playing = false;
        try { if (mp != null) { mp.release(); mp = null; } } catch (Exception ignored) { }
        try {
            if (am != null) {
                if (Build.VERSION.SDK_INT >= 26 && focusReq != null) am.abandonAudioFocusRequest(focusReq);
                else if (Build.VERSION.SDK_INT < 26) am.abandonAudioFocus(null);
            }
        } catch (Exception ignored) { }
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        playing = false;
        super.onDestroy();
    }
}
