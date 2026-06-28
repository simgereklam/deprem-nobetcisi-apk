package com.murat.depremnobetcisi;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.media.AudioAttributes;
import android.net.Uri;
import android.provider.Settings;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EarthquakeService extends Service {
    public static final String PREF = "deprem_nobetcisi";
    public static final String CHANNEL_ALERTS = "deprem_alerts";
    public static final String CHANNEL_SERVICE = "deprem_service";
    private Handler handler;
    private ExecutorService executor;
    private SharedPreferences prefs;
    private final long INTERVAL = 60_000L;

    private final Runnable pollRunnable = new Runnable() {
        @Override public void run() {
            poll();
            handler.postDelayed(this, INTERVAL);
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        executor = Executors.newSingleThreadExecutor();
        prefs = getSharedPreferences(PREF, MODE_PRIVATE);
        createChannels();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1001, serviceNotification());
        handler.removeCallbacks(pollRunnable);
        handler.post(pollRunnable);
        prefs.edit().putBoolean("service_on", true).apply();
        return START_STICKY;
    }

    private void poll() {
        executor.execute(new Runnable() {
            @Override public void run() {
                try {
                    EarthquakeApi.Result r = EarthquakeApi.fetchAny();
                    checkNewQuakes(r.quakes);
                } catch (Exception ignored) {}
            }
        });
    }

    private void checkNewQuakes(List<Quake> quakes) {
        double threshold = Double.longBitsToDouble(prefs.getLong("alertMagnitude", Double.doubleToLongBits(3.5)));
        int radius = prefs.getInt("radiusKm", 0);
        boolean hasLocation = prefs.getBoolean("hasLocation", false);
        double userLat = Double.longBitsToDouble(prefs.getLong("userLat", Double.doubleToLongBits(0)));
        double userLon = Double.longBitsToDouble(prefs.getLong("userLon", Double.doubleToLongBits(0)));

        Set<String> seen = new HashSet<>(prefs.getStringSet("seenIds", new HashSet<String>()));
        long now = System.currentTimeMillis();

        for (Quake q : quakes) {
            if (seen.contains(q.id)) continue;

            boolean nearOk = true;
            if (radius > 0) {
                nearOk = hasLocation && EarthquakeApi.distanceKm(userLat, userLon, q.lat, q.lon) <= radius;
            }

            boolean fresh = Math.abs(now - q.timeMs) < 15L * 60L * 1000L;
            if (q.mag >= threshold && nearOk && fresh) {
                sendAlert(q);
            }
            seen.add(q.id);
        }

        while (seen.size() > 400) {
            String first = seen.iterator().next();
            seen.remove(first);
        }

        prefs.edit().putStringSet("seenIds", seen).apply();
    }

    private void sendAlert(Quake q) {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Intent open = new Intent(this, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 2001, open,
                PendingIntent.FLAG_UPDATE_CURRENT | pendingImmutableFlag());

        String title = "Deprem uyarısı: " + String.format("%.1f", q.mag);
        String body = q.region + " • " + q.place + " • " + q.timeText + " • Derinlik " + String.format("%.1f", q.depth) + " km";

        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ALERTS)
                : new Notification.Builder(this);

        b.setSmallIcon(R.drawable.ic_stat_quake)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_ALL)
                .setVibrate(new long[]{0, 500, 250, 500, 250, 800});

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify((int) (System.currentTimeMillis() % 100000), b.build());
    }

    private Notification serviceNotification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 1001, open,
                PendingIntent.FLAG_UPDATE_CURRENT | pendingImmutableFlag());

        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_SERVICE)
                : new Notification.Builder(this);

        b.setSmallIcon(R.drawable.ic_stat_quake)
                .setContentTitle("Deprem Nöbetçisi aktif")
                .setContentText("Yeni deprem kayıtları kontrol ediliyor.")
                .setContentIntent(pi)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_LOW);

        return b.build();
    }

    private void createChannels() {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        NotificationChannel alerts = new NotificationChannel(
                CHANNEL_ALERTS,
                "Deprem Uyarıları",
                NotificationManager.IMPORTANCE_HIGH
        );
        alerts.setDescription("Eşik üstü yeni deprem bildirimleri");
        alerts.enableVibration(true);
        alerts.setVibrationPattern(new long[]{0, 500, 250, 500, 250, 800});
        Uri alarmSound = Settings.System.DEFAULT_ALARM_ALERT_URI;
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        alerts.setSound(alarmSound, audioAttributes);

        NotificationChannel service = new NotificationChannel(
                CHANNEL_SERVICE,
                "Nöbet Servisi",
                NotificationManager.IMPORTANCE_LOW
        );
        service.setDescription("Arka planda deprem kontrol servisi");

        nm.createNotificationChannel(alerts);
        nm.createNotificationChannel(service);
    }

    private int pendingImmutableFlag() {
        return Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0;
    }

    @Override public void onDestroy() {
        prefs.edit().putBoolean("service_on", false).apply();
        handler.removeCallbacks(pollRunnable);
        executor.shutdownNow();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) {
        return null;
    }
}
