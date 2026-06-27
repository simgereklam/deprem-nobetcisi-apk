package com.murat.depremnobetcisi;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int REQ_LOCATION = 22;
    private static final int REQ_NOTIF = 33;
    private SharedPreferences prefs;
    private ExecutorService executor;

    private Spinner minMagSpinner;
    private Spinner alertSpinner;
    private Spinner radiusSpinner;
    private TextView sourceText, lastCheckText, countText, subInfoText, errorText;
    private Button serviceButton, locationButton;
    private LinearLayout quakeList;

    private final String[] minLabels = {"Tüm depremler", "2.0 ve üzeri", "3.0 ve üzeri", "4.0 ve üzeri", "5.0 ve üzeri"};
    private final double[] minValues = {0, 2, 3, 4, 5};

    private final String[] alertLabels = {"3.0 ve üzeri", "3.5 ve üzeri", "4.0 ve üzeri", "4.5 ve üzeri", "5.0 ve üzeri"};
    private final double[] alertValues = {3, 3.5, 4, 4.5, 5};

    private final String[] radiusLabels = {"Türkiye geneli", "50 km yakınım", "100 km yakınım", "200 km yakınım", "300 km yakınım"};
    private final int[] radiusValues = {0, 50, 100, 200, 300};

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences(EarthquakeService.PREF, MODE_PRIVATE);
        executor = Executors.newSingleThreadExecutor();
        createChannels();
        buildUi();
        loadPrefsToUi();
        refreshData();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(7,17,31));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(26), dp(18), dp(30));
        scroll.addView(root);

        TextView eyebrow = text("TÜRKİYE DEPREM TAKİP", 14, Color.rgb(255,176,32), true);
        root.addView(eyebrow);

        TextView title = text("Deprem Nöbetçisi", 34, Color.rgb(236,244,255), true);
        title.setPadding(0, dp(8), 0, dp(8));
        root.addView(title);

        TextView lead = text("Son depremleri takip eder. Eşik üstü yeni deprem görünce bildirim verir.", 18, Color.rgb(159,178,200), false);
        lead.setLineSpacing(4, 1.0f);
        root.addView(lead);

        TextView warn = text("Önemli: Bu uygulama depremi önceden tahmin etmez. Resmi erken uyarı sistemi değildir. AFAD, Kandilli ve resmi duyuruları takip et.", 16, Color.rgb(255,230,173), true);
        warn.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams warnLp = lp(-1, -2);
        warnLp.setMargins(0, dp(24), 0, dp(16));
        warn.setLayoutParams(warnLp);
        warn.setBackground(cardBg(Color.rgb(32,31,25), Color.rgb(115,84,30)));
        root.addView(warn);

        LinearLayout controls = card();
        root.addView(controls);

        minMagSpinner = spinner(minLabels);
        alertSpinner = spinner(alertLabels);
        radiusSpinner = spinner(radiusLabels);

        controls.addView(label("LİSTEDE GÖSTER"));
        controls.addView(minMagSpinner);
        controls.addView(space(12));
        controls.addView(label("BİLDİRİM EŞİĞİ"));
        controls.addView(alertSpinner);
        controls.addView(space(12));
        controls.addView(label("YAKINLIK FİLTRESİ"));
        controls.addView(radiusSpinner);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.LEFT);
        actions.setPadding(0, dp(16), 0, 0);
        controls.addView(actions);

        Button refresh = button("Yenile", true);
        serviceButton = button("Nöbeti başlat", false);
        locationButton = button("Konumumu al", false);

        actions.addView(refresh);
        actions.addView(serviceButton);
        actions.addView(locationButton);

        refresh.setOnClickListener(v -> refreshData());
        serviceButton.setOnClickListener(v -> toggleService());
        locationButton.setOnClickListener(v -> getLocationNow());

        root.addView(statCard("KAYNAK"));
        sourceText = (TextView) ((LinearLayout) root.getChildAt(root.getChildCount()-1)).getChildAt(1);
        sourceText.setText("Hazırlanıyor");

        root.addView(statCard("SON KONTROL"));
        lastCheckText = (TextView) ((LinearLayout) root.getChildAt(root.getChildCount()-1)).getChildAt(1);
        lastCheckText.setText("-");

        root.addView(statCard("GÖRÜNEN DEPREM"));
        countText = (TextView) ((LinearLayout) root.getChildAt(root.getChildCount()-1)).getChildAt(1);
        countText.setText("0");

        LinearLayout listCard = card();
        LinearLayout.LayoutParams listLp = lp(-1, -2);
        listLp.setMargins(0, dp(16), 0, 0);
        listCard.setLayoutParams(listLp);
        root.addView(listCard);

        TextView listTitle = text("Son Depremler", 24, Color.WHITE, true);
        listCard.addView(listTitle);

        subInfoText = text("Veri alınıyor...", 14, Color.rgb(159,178,200), false);
        listCard.addView(subInfoText);

        errorText = text("", 14, Color.rgb(255,180,180), true);
        errorText.setVisibility(View.GONE);
        errorText.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams errLp = lp(-1, -2);
        errLp.setMargins(0, dp(12), 0, dp(8));
        errorText.setLayoutParams(errLp);
        errorText.setBackground(cardBg(Color.rgb(60,20,24), Color.rgb(130,40,50)));
        listCard.addView(errorText);

        quakeList = new LinearLayout(this);
        quakeList.setOrientation(LinearLayout.VERTICAL);
        quakeList.setPadding(0, dp(12), 0, 0);
        listCard.addView(quakeList);

        TextView footer = text("Veriler halka açık servislerden alınır. İnternet ve kaynak durumuna göre gecikme olabilir.", 12, Color.rgb(159,178,200), false);
        LinearLayout.LayoutParams footLp = lp(-1, -2);
        footLp.setMargins(0, dp(16), 0, 0);
        footer.setLayoutParams(footLp);
        root.addView(footer);

        AdapterView.OnItemSelectedListener saveListener = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                saveSettings();
                refreshData();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        };
        minMagSpinner.setOnItemSelectedListener(saveListener);
        alertSpinner.setOnItemSelectedListener(saveListener);
        radiusSpinner.setOnItemSelectedListener(saveListener);

        setContentView(scroll);
    }

    private void refreshData() {
        showError("");
        subInfoText.setText("Veri alınıyor...");
        quakeList.removeAllViews();

        executor.execute(new Runnable() {
            @Override public void run() {
                try {
                    EarthquakeApi.Result result = EarthquakeApi.fetchAny();
                    runOnUiThread(() -> render(result));
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        sourceText.setText("Hata");
                        lastCheckText.setText(EarthquakeApi.formatTime(System.currentTimeMillis()));
                        countText.setText("0");
                        subInfoText.setText("Veri alınamadı.");
                        showError(e.getMessage());
                    });
                }
            }
        });
    }

    private void render(EarthquakeApi.Result result) {
        double minMag = minValues[minMagSpinner.getSelectedItemPosition()];
        int radius = radiusValues[radiusSpinner.getSelectedItemPosition()];
        boolean hasLocation = prefs.getBoolean("hasLocation", false);
        double userLat = Double.longBitsToDouble(prefs.getLong("userLat", Double.doubleToLongBits(0)));
        double userLon = Double.longBitsToDouble(prefs.getLong("userLon", Double.doubleToLongBits(0)));

        quakeList.removeAllViews();
        int count = 0;

        for (Quake q : result.quakes) {
            if (q.mag < minMag) continue;

            if (radius > 0) {
                if (!hasLocation) continue;
                q.distanceKm = EarthquakeApi.distanceKm(userLat, userLon, q.lat, q.lon);
                if (q.distanceKm > radius) continue;
            }

            quakeList.addView(quakeRow(q));
            count++;
            if (count >= 80) break;
        }

        sourceText.setText(result.sourceName);
        lastCheckText.setText(EarthquakeApi.formatTime(System.currentTimeMillis()));
        countText.setText(String.valueOf(count));
        subInfoText.setText("Bildirim eşiği " + alertLabels[alertSpinner.getSelectedItemPosition()] + " • Nöbet servisi 60 sn kontrol eder.");

        if (count == 0) {
            TextView empty = text("Bu filtreye uygun deprem görünmüyor.", 15, Color.rgb(159,178,200), false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(20), 0, dp(20));
            quakeList.addView(empty);
        }
    }

    private View quakeRow(Quake q) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams rowLp = lp(-1, -2);
        rowLp.setMargins(0, 0, 0, dp(10));
        row.setLayoutParams(rowLp);
        row.setBackground(cardBg(Color.rgb(18,40,64), Color.rgb(45,70,105)));

        TextView mag = text(String.format(Locale.US, "%.1f", q.mag), 22, Color.WHITE, true);
        mag.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams magLp = lp(dp(62), dp(62));
        magLp.setMargins(0, 0, dp(12), 0);
        mag.setLayoutParams(magLp);
        int magColor = q.mag >= 5 ? Color.rgb(90,30,35) : q.mag >= 4 ? Color.rgb(85,60,25) : Color.rgb(25,65,105);
        mag.setBackground(cardBg(magColor, Color.rgb(80,100,130)));
        row.addView(mag);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(lp(0, -2, 1));

        TextView place = text(q.place, 16, Color.WHITE, true);
        info.addView(place);

        String dist = q.distanceKm >= 0 ? " • Yaklaşık " + Math.round(q.distanceKm) + " km" : "";
        TextView meta = text(q.timeText + " • Derinlik " + String.format(Locale.US, "%.1f", q.depth) + " km" + dist
                + "\nKaynak: " + q.source, 13, Color.rgb(159,178,200), false);
        meta.setPadding(0, dp(5), 0, dp(8));
        info.addView(meta);

        Button map = button("Haritada aç", false);
        map.setTextSize(12);
        map.setOnClickListener(v -> {
            String uri = "geo:" + q.lat + "," + q.lon + "?q=" + q.lat + "," + q.lon + "(" + Uri.encode(q.place) + ")";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            startActivity(intent);
        });
        info.addView(map);

        row.addView(info);
        return row;
    }

    private void toggleService() {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIF);
            return;
        }

        boolean on = prefs.getBoolean("service_on", false);
        if (on) {
            stopService(new Intent(this, EarthquakeService.class));
            prefs.edit().putBoolean("service_on", false).apply();
            serviceButton.setText("Nöbeti başlat");
            Toast.makeText(this, "Nöbet durduruldu", Toast.LENGTH_SHORT).show();
        } else {
            saveSettings();
            Intent i = new Intent(this, EarthquakeService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
            else startService(i);
            prefs.edit().putBoolean("service_on", true).apply();
            serviceButton.setText("Nöbeti durdur");
            Toast.makeText(this, "Nöbet başladı. Bildirim çubuğunda aktif görünür.", Toast.LENGTH_LONG).show();
        }
    }

    private void getLocationNow() {
        if (Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOCATION);
            return;
        }

        try {
            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            Location best = null;
            for (String p : lm.getProviders(true)) {
                Location l = lm.getLastKnownLocation(p);
                if (l != null && (best == null || l.getAccuracy() < best.getAccuracy())) best = l;
            }

            if (best == null) {
                Toast.makeText(this, "Konum bulunamadı. GPS'i açıp tekrar dene.", Toast.LENGTH_LONG).show();
                return;
            }

            prefs.edit()
                    .putBoolean("hasLocation", true)
                    .putLong("userLat", Double.doubleToLongBits(best.getLatitude()))
                    .putLong("userLon", Double.doubleToLongBits(best.getLongitude()))
                    .apply();

            locationButton.setText("Konum kayıtlı");
            Toast.makeText(this, "Konum kaydedildi", Toast.LENGTH_SHORT).show();
            refreshData();
        } catch (Exception e) {
            Toast.makeText(this, "Konum alınamadı: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void saveSettings() {
        if (prefs == null || alertSpinner == null || radiusSpinner == null) return;
        prefs.edit()
                .putLong("alertMagnitude", Double.doubleToLongBits(alertValues[alertSpinner.getSelectedItemPosition()]))
                .putInt("radiusKm", radiusValues[radiusSpinner.getSelectedItemPosition()])
                .apply();
    }

    private void loadPrefsToUi() {
        double alert = Double.longBitsToDouble(prefs.getLong("alertMagnitude", Double.doubleToLongBits(3.5)));
        int radius = prefs.getInt("radiusKm", 0);
        setSpinnerByDouble(alertSpinner, alertValues, alert);
        setSpinnerByInt(radiusSpinner, radiusValues, radius);
        minMagSpinner.setSelection(2);

        if (prefs.getBoolean("hasLocation", false)) locationButton.setText("Konum kayıtlı");
        serviceButton.setText(prefs.getBoolean("service_on", false) ? "Nöbeti durdur" : "Nöbeti başlat");
    }

    private void setSpinnerByDouble(Spinner s, double[] values, double v) {
        for (int i=0;i<values.length;i++) if (values[i] == v) { s.setSelection(i); return; }
    }

    private void setSpinnerByInt(Spinner s, int[] values, int v) {
        for (int i=0;i<values.length;i++) if (values[i] == v) { s.setSelection(i); return; }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION) getLocationNow();
        if (requestCode == REQ_NOTIF) toggleService();
    }

    private void createChannels() {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.createNotificationChannel(new NotificationChannel(EarthquakeService.CHANNEL_ALERTS, "Deprem Uyarıları", NotificationManager.IMPORTANCE_HIGH));
        nm.createNotificationChannel(new NotificationChannel(EarthquakeService.CHANNEL_SERVICE, "Nöbet Servisi", NotificationManager.IMPORTANCE_LOW));
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private TextView label(String s) {
        TextView t = text(s, 13, Color.rgb(159,178,200), true);
        t.setPadding(0, dp(10), 0, dp(8));
        return t;
    }

    private Spinner spinner(String[] labels) {
        Spinner s = new Spinner(this);
        ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels);
        s.setAdapter(ad);
        s.setPadding(dp(10), dp(5), dp(10), dp(5));
        s.setBackground(cardBg(Color.rgb(19,43,70), Color.rgb(55,80,118)));
        return s;
    }

    private Button button(String s, boolean primary) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(primary ? Color.rgb(17,24,39) : Color.WHITE);
        b.setBackground(cardBg(primary ? Color.rgb(255,140,42) : Color.rgb(19,43,70), primary ? Color.rgb(255,170,70) : Color.rgb(55,80,118)));
        LinearLayout.LayoutParams lp = lp(-2, dp(58));
        lp.setMargins(0, 0, dp(8), 0);
        b.setLayoutParams(lp);
        return b;
    }

    private LinearLayout card() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams lp = lp(-1, -2);
        lp.setMargins(0, dp(14), 0, 0);
        l.setLayoutParams(lp);
        l.setBackground(cardBg(Color.rgb(10,31,51), Color.rgb(35,62,94)));
        return l;
    }

    private LinearLayout statCard(String label) {
        LinearLayout l = card();
        TextView lab = text(label, 13, Color.rgb(159,178,200), true);
        TextView val = text("-", 22, Color.WHITE, true);
        val.setPadding(0, dp(8), 0, 0);
        l.addView(lab);
        l.addView(val);
        return l;
    }

    private View space(int h) {
        View v = new View(this);
        v.setLayoutParams(lp(1, dp(h)));
        return v;
    }

    private LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    private LinearLayout.LayoutParams lp(int w, int h, float weight) {
        return new LinearLayout.LayoutParams(w, h, weight);
    }

    private GradientDrawable cardBg(int fill, int stroke) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(22));
        g.setStroke(dp(1), stroke);
        return g;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void showError(String s) {
        if (s == null || s.isEmpty()) {
            errorText.setVisibility(View.GONE);
            errorText.setText("");
        } else {
            errorText.setText(s);
            errorText.setVisibility(View.VISIBLE);
        }
    }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
