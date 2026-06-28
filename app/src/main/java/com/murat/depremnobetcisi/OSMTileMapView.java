package com.murat.depremnobetcisi;

import android.content.Context;
import android.graphics.*;
import android.view.View;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OSMTileMapView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Quake> quakes = new ArrayList<>();

    private final Map<String, Bitmap> tileCache = new HashMap<>();
    private final Set<String> loading = new HashSet<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    private boolean hasUserLocation = false;
    private double userLat = 0;
    private double userLon = 0;

    // Türkiye sınırlarını kapsayan gerçek harita alanı
    private final double minLon = 25.2;
    private final double maxLon = 45.7;
    private final double minLat = 35.4;
    private final double maxLat = 42.7;
    private final int zoom = 6;

    private RectF mapRect = new RectF();

    public OSMTileMapView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void setMapData(List<Quake> list, boolean hasLoc, double lat, double lon) {
        quakes.clear();
        if (list != null) quakes.addAll(list);
        hasUserLocation = hasLoc;
        userLat = lat;
        userLon = lon;
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        drawPanel(canvas, w, h);
        drawTiles(canvas, w, h);
        drawOverlay(canvas, w, h);
        drawUserLocation(canvas, w, h);
        drawQuakes(canvas, w, h);
        drawLegend(canvas, w, h);
    }

    private void drawPanel(Canvas c, int w, int h) {
        paint.setShader(new LinearGradient(0, 0, 0, h,
                Color.rgb(7, 20, 36),
                Color.rgb(10, 37, 60),
                Shader.TileMode.CLAMP));
        c.drawRoundRect(new RectF(0, 0, w, h), dp(22), dp(22), paint);
        paint.setShader(null);

        mapRect.set(dp(8), dp(8), w - dp(8), h - dp(34));
        paint.setColor(Color.rgb(11, 28, 45));
        c.drawRoundRect(mapRect, dp(16), dp(16), paint);
    }

    private void drawTiles(Canvas c, int w, int h) {
        double leftPx = worldPixelX(minLon, zoom);
        double rightPx = worldPixelX(maxLon, zoom);
        double topPx = worldPixelY(maxLat, zoom);
        double bottomPx = worldPixelY(minLat, zoom);

        int xStart = (int) Math.floor(leftPx / 256.0);
        int xEnd = (int) Math.floor(rightPx / 256.0);
        int yStart = (int) Math.floor(topPx / 256.0);
        int yEnd = (int) Math.floor(bottomPx / 256.0);

        int loadedCount = 0;
        int totalCount = 0;

        c.save();
        Path clip = new Path();
        clip.addRoundRect(mapRect, dp(16), dp(16), Path.Direction.CW);
        c.clipPath(clip);

        for (int tx = xStart; tx <= xEnd; tx++) {
            for (int ty = yStart; ty <= yEnd; ty++) {
                totalCount++;
                String key = zoom + "/" + tx + "/" + ty;
                Bitmap bmp = tileCache.get(key);
                if (bmp == null) {
                    loadTileAsync(tx, ty, key);
                    drawTilePlaceholder(c, tx, ty, leftPx, rightPx, topPx, bottomPx);
                    continue;
                }

                loadedCount++;
                RectF dst = tileRect(tx, ty, leftPx, rightPx, topPx, bottomPx);
                c.drawBitmap(bmp, null, dst, paint);
            }
        }

        c.restore();

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(2));
        paint.setColor(Color.argb(170, 180, 220, 245));
        c.drawRoundRect(mapRect, dp(16), dp(16), paint);
        paint.setStyle(Paint.Style.FILL);

        if (loadedCount == 0) {
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTextSize(dp(15));
            textPaint.setFakeBoldText(true);
            textPaint.setColor(Color.WHITE);
            c.drawText("Gerçek harita yükleniyor...", w / 2f, h / 2f, textPaint);
            textPaint.setFakeBoldText(false);
        }

        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTextSize(dp(10));
        textPaint.setColor(Color.argb(190, 236, 244, 255));
        c.drawText("OpenStreetMap gerçek harita", dp(14), h - dp(12), textPaint);

        textPaint.setTextAlign(Paint.Align.RIGHT);
        c.drawText(loadedCount + "/" + totalCount + " harita parçası", w - dp(14), h - dp(12), textPaint);
    }

    private void drawTilePlaceholder(Canvas c, int tx, int ty, double leftPx, double rightPx, double topPx, double bottomPx) {
        RectF dst = tileRect(tx, ty, leftPx, rightPx, topPx, bottomPx);
        paint.setColor(((tx + ty) % 2 == 0) ? Color.rgb(18, 48, 68) : Color.rgb(14, 40, 58));
        c.drawRect(dst, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        paint.setColor(Color.argb(35, 255, 255, 255));
        c.drawRect(dst, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private RectF tileRect(int tx, int ty, double leftPx, double rightPx, double topPx, double bottomPx) {
        double tileLeft = tx * 256.0;
        double tileRight = tileLeft + 256.0;
        double tileTop = ty * 256.0;
        double tileBottom = tileTop + 256.0;

        float l = (float) (mapRect.left + ((tileLeft - leftPx) / (rightPx - leftPx)) * mapRect.width());
        float r = (float) (mapRect.left + ((tileRight - leftPx) / (rightPx - leftPx)) * mapRect.width());
        float t = (float) (mapRect.top + ((tileTop - topPx) / (bottomPx - topPx)) * mapRect.height());
        float b = (float) (mapRect.top + ((tileBottom - topPx) / (bottomPx - topPx)) * mapRect.height());

        return new RectF(l, t, r, b);
    }

    private void loadTileAsync(final int tx, final int ty, final String key) {
        if (tileCache.containsKey(key) || loading.contains(key)) return;
        loading.add(key);

        executor.execute(new Runnable() {
            @Override public void run() {
                Bitmap bmp = null;
                try {
                    String urlText = "https://tile.openstreetmap.org/" + zoom + "/" + tx + "/" + ty + ".png";
                    HttpURLConnection c = (HttpURLConnection) new URL(urlText).openConnection();
                    c.setConnectTimeout(9000);
                    c.setReadTimeout(9000);
                    c.setRequestProperty("User-Agent", "DepremNobetcisi/1.5 murat");
                    c.setRequestMethod("GET");
                    if (c.getResponseCode() >= 200 && c.getResponseCode() < 300) {
                        InputStream in = c.getInputStream();
                        bmp = BitmapFactory.decodeStream(in);
                        in.close();
                    }
                } catch (Exception ignored) {}

                final Bitmap finalBmp = bmp;
                post(new Runnable() {
                    @Override public void run() {
                        loading.remove(key);
                        if (finalBmp != null) tileCache.put(key, finalBmp);
                        invalidate();
                    }
                });
            }
        });
    }

    private void drawOverlay(Canvas c, int w, int h) {
        c.save();
        Path clip = new Path();
        clip.addRoundRect(mapRect, dp(16), dp(16), Path.Direction.CW);
        c.clipPath(clip);

        paint.setColor(Color.argb(72, 7, 17, 31));
        c.drawRect(mapRect, paint);

        c.restore();
    }

    private void drawQuakes(Canvas c, int w, int h) {
        if (quakes.isEmpty()) {
            textPaint.setColor(Color.argb(210, 255, 255, 255));
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTextSize(dp(13));
            c.drawText("Filtreye uygun deprem yok", w / 2f, mapRect.centerY(), textPaint);
            return;
        }

        int drawn = 0;
        for (int i = Math.min(quakes.size(), 80) - 1; i >= 0; i--) {
            Quake q = quakes.get(i);
            if (!inside(q.lat, q.lon)) continue;

            float x = mapX(q.lon);
            float y = mapY(q.lat);
            int color = colorForMag(q.mag);
            float r = radiusForMag(q.mag);

            paint.setShader(new RadialGradient(x, y, r * 5f,
                    Color.argb(135, Color.red(color), Color.green(color), Color.blue(color)),
                    Color.argb(0, Color.red(color), Color.green(color), Color.blue(color)),
                    Shader.TileMode.CLAMP));
            c.drawCircle(x, y, r * 5f, paint);
            paint.setShader(null);

            paint.setColor(Color.argb(235, Color.red(color), Color.green(color), Color.blue(color)));
            c.drawCircle(x, y, r, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));
            paint.setColor(Color.WHITE);
            c.drawCircle(x, y, r, paint);
            paint.setStyle(Paint.Style.FILL);

            if (drawn < 6) {
                textPaint.setTextAlign(Paint.Align.CENTER);
                textPaint.setTextSize(dp(10));
                textPaint.setFakeBoldText(true);
                textPaint.setColor(Color.WHITE);
                c.drawText(String.format(Locale.US, "%.1f", q.mag), x, y - r - dp(6), textPaint);
                textPaint.setFakeBoldText(false);
            }

            drawn++;
        }
    }

    private void drawUserLocation(Canvas c, int w, int h) {
        if (!hasUserLocation || !inside(userLat, userLon)) return;

        float x = mapX(userLon);
        float y = mapY(userLat);

        paint.setColor(Color.argb(80, 77, 163, 255));
        c.drawCircle(x, y, dp(15), paint);
        paint.setColor(Color.rgb(77, 163, 255));
        c.drawCircle(x, y, dp(6), paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(2));
        paint.setColor(Color.WHITE);
        c.drawCircle(x, y, dp(6), paint);
        paint.setStyle(Paint.Style.FILL);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(dp(9));
        textPaint.setFakeBoldText(true);
        textPaint.setColor(Color.WHITE);
        c.drawText("Konumun", x, y - dp(18), textPaint);
        textPaint.setFakeBoldText(false);
    }

    private void drawLegend(Canvas c, int w, int h) {
        float boxLeft = mapRect.left + dp(8);
        float boxTop = mapRect.bottom - dp(36);
        float boxRight = mapRect.right - dp(8);
        float boxBottom = mapRect.bottom - dp(8);

        paint.setColor(Color.argb(180, 7, 17, 31));
        c.drawRoundRect(new RectF(boxLeft, boxTop, boxRight, boxBottom), dp(12), dp(12), paint);

        textPaint.setTextSize(dp(10));
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setFakeBoldText(true);

        legendItem(c, boxLeft + dp(12), boxTop + dp(17), Color.rgb(77, 163, 255), "3-4");
        legendItem(c, boxLeft + dp(78), boxTop + dp(17), Color.rgb(255, 176, 32), "4-5");
        legendItem(c, boxLeft + dp(144), boxTop + dp(17), Color.rgb(255, 77, 77), "5+");

        textPaint.setTextAlign(Paint.Align.RIGHT);
        textPaint.setColor(Color.argb(220, 255, 255, 255));
        c.drawText("Deprem noktaları", boxRight - dp(12), boxTop + dp(21), textPaint);
        textPaint.setFakeBoldText(false);
    }

    private void legendItem(Canvas c, float x, float y, int color, String label) {
        paint.setColor(color);
        c.drawCircle(x, y, dp(5), paint);
        textPaint.setColor(Color.WHITE);
        c.drawText(label, x + dp(10), y + dp(4), textPaint);
    }

    private boolean inside(double lat, double lon) {
        return lat >= minLat && lat <= maxLat && lon >= minLon && lon <= maxLon;
    }

    private float mapX(double lon) {
        double leftPx = worldPixelX(minLon, zoom);
        double rightPx = worldPixelX(maxLon, zoom);
        double px = worldPixelX(lon, zoom);
        return (float) (mapRect.left + ((px - leftPx) / (rightPx - leftPx)) * mapRect.width());
    }

    private float mapY(double lat) {
        double topPx = worldPixelY(maxLat, zoom);
        double bottomPx = worldPixelY(minLat, zoom);
        double py = worldPixelY(lat, zoom);
        return (float) (mapRect.top + ((py - topPx) / (bottomPx - topPx)) * mapRect.height());
    }

    private double worldPixelX(double lon, int z) {
        double scale = 256.0 * Math.pow(2, z);
        return (lon + 180.0) / 360.0 * scale;
    }

    private double worldPixelY(double lat, int z) {
        double sinLat = Math.sin(Math.toRadians(lat));
        double scale = 256.0 * Math.pow(2, z);
        return (0.5 - Math.log((1 + sinLat) / (1 - sinLat)) / (4 * Math.PI)) * scale;
    }

    private int colorForMag(double m) {
        if (m >= 5.0) return Color.rgb(255, 77, 77);
        if (m >= 4.0) return Color.rgb(255, 176, 32);
        return Color.rgb(77, 163, 255);
    }

    private float radiusForMag(double m) {
        if (m >= 6.0) return dp(12);
        if (m >= 5.0) return dp(10);
        if (m >= 4.0) return dp(8);
        return dp(6);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
