package com.murat.depremnobetcisi;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TurkeyMapView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Quake> quakes = new ArrayList<>();
    private boolean hasUserLocation = false;
    private double userLat = 0, userLon = 0;
    private final double minLon = 25.3, maxLon = 45.5, minLat = 35.5, maxLat = 42.6;

    public TurkeyMapView(Context context) {
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
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        drawBackground(canvas, w, h);
        drawTurkey(canvas, w, h);
        drawRegionLines(canvas, w, h);
        drawRegionLabels(canvas, w, h);
        drawUser(canvas, w, h);
        drawQuakes(canvas, w, h);
        drawLegend(canvas, w, h);
    }

    private void drawBackground(Canvas c, int w, int h) {
        paint.setShader(new LinearGradient(0, 0, 0, h, Color.rgb(7,25,44), Color.rgb(11,39,62), Shader.TileMode.CLAMP));
        c.drawRoundRect(new RectF(0, 0, w, h), dp(22), dp(22), paint);
        paint.setShader(null);
        paint.setColor(Color.argb(28,255,255,255));
        paint.setStrokeWidth(dp(1));
        for (int i=1;i<4;i++) c.drawLine(dp(12), h*i/4f, w-dp(12), h*i/4f, paint);
        for (int i=1;i<5;i++) c.drawLine(w*i/5f, dp(12), w*i/5f, h-dp(32), paint);
    }

    private void drawTurkey(Canvas c, int w, int h) {
        double[][] pts = {
            {26.0,40.1},{26.8,40.7},{27.8,40.8},{28.8,41.1},{29.7,41.0},{30.6,41.3},
            {31.8,41.2},{32.8,41.5},{34.0,41.7},{35.4,41.6},{36.8,41.2},{38.0,41.3},
            {39.4,41.0},{40.7,41.1},{41.8,41.5},{43.1,41.3},{44.5,40.7},{44.6,39.8},
            {43.8,39.1},{43.4,38.4},{42.4,37.9},{41.3,37.4},{40.0,37.1},{38.5,36.8},
            {37.1,36.6},{35.8,36.2},{34.6,36.1},{33.2,36.0},{31.8,36.0},{30.5,36.2},
            {29.4,36.4},{28.4,36.6},{27.7,37.0},{27.0,37.6},{26.4,38.3},{26.1,39.1}
        };
        Path p = new Path();
        for (int i=0;i<pts.length;i++) {
            float x = x(pts[i][0], w), y = y(pts[i][1], h);
            if (i==0) p.moveTo(x,y); else p.lineTo(x,y);
        }
        p.close();
        paint.setShader(new LinearGradient(0, dp(18), 0, h-dp(35), Color.rgb(24,86,96), Color.rgb(18,68,78), Shader.TileMode.CLAMP));
        c.drawPath(p, paint);
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(2));
        paint.setColor(Color.argb(170,160,220,230));
        c.drawPath(p, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawRegionLines(Canvas c, int w, int h) {
        paint.setColor(Color.argb(65,255,255,255));
        paint.setStrokeWidth(dp(1));
        line(c,w,h,30.2,36.3,30.6,41.2);
        line(c,w,h,33.5,36.1,33.8,41.6);
        line(c,w,h,37.2,36.4,37.1,41.4);
        line(c,w,h,41.0,37.2,40.7,41.2);
        line(c,w,h,30.8,40.2,40.8,40.0);
        line(c,w,h,30.5,37.2,37.8,37.4);
    }

    private void drawRegionLabels(Canvas c, int w, int h) {
        textPaint.setColor(Color.argb(185,230,244,255));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(dp(10));
        textPaint.setFakeBoldText(true);
        label(c,"Marmara",28.8,40.3,w,h); label(c,"Ege",28.5,38.2,w,h);
        label(c,"Akdeniz",33.8,36.8,w,h); label(c,"İç Anadolu",34.7,39.0,w,h);
        label(c,"Karadeniz",36.3,40.7,w,h); label(c,"Doğu",41.0,39.2,w,h);
        label(c,"G.Doğu",40.1,37.5,w,h);
        textPaint.setFakeBoldText(false);
    }

    private void drawQuakes(Canvas c, int w, int h) {
        if (quakes.isEmpty()) {
            textPaint.setColor(Color.argb(190,230,244,255));
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTextSize(dp(13));
            c.drawText("Filtreye uygun deprem yok", w/2f, h-dp(48), textPaint);
            return;
        }
        int drawn = 0;
        for (int i=Math.min(quakes.size(),60)-1;i>=0;i--) {
            Quake q = quakes.get(i);
            if (q.lat<minLat || q.lat>maxLat || q.lon<minLon || q.lon>maxLon) continue;
            float px=x(q.lon,w), py=y(q.lat,h), r=rad(q.mag);
            int col=col(q.mag);
            paint.setShader(new RadialGradient(px, py, r*4f, Color.argb(120,Color.red(col),Color.green(col),Color.blue(col)), Color.argb(0,Color.red(col),Color.green(col),Color.blue(col)), Shader.TileMode.CLAMP));
            c.drawCircle(px, py, r*4f, paint); paint.setShader(null);
            paint.setColor(Color.argb(230,Color.red(col),Color.green(col),Color.blue(col))); c.drawCircle(px, py, r, paint);
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(1)); paint.setColor(Color.WHITE); c.drawCircle(px, py, r, paint); paint.setStyle(Paint.Style.FILL);
            if (drawn < 4) {
                textPaint.setTextAlign(Paint.Align.CENTER); textPaint.setTextSize(dp(9)); textPaint.setFakeBoldText(true); textPaint.setColor(Color.WHITE);
                c.drawText(String.format(Locale.US,"%.1f",q.mag), px, py-r-dp(5), textPaint); textPaint.setFakeBoldText(false);
            }
            drawn++;
        }
    }

    private void drawUser(Canvas c, int w, int h) {
        if (!hasUserLocation || userLat<minLat || userLat>maxLat || userLon<minLon || userLon>maxLon) return;
        float px=x(userLon,w), py=y(userLat,h);
        paint.setColor(Color.argb(70,77,163,255)); c.drawCircle(px,py,dp(12),paint);
        paint.setColor(Color.rgb(77,163,255)); c.drawCircle(px,py,dp(5),paint);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(2)); paint.setColor(Color.WHITE); c.drawCircle(px,py,dp(5),paint); paint.setStyle(Paint.Style.FILL);
    }

    private void drawLegend(Canvas c, int w, int h) {
        float yy=h-dp(18), xx=dp(16);
        textPaint.setTextSize(dp(10)); textPaint.setTextAlign(Paint.Align.LEFT); textPaint.setFakeBoldText(true);
        legend(c,xx,yy,Color.rgb(77,163,255),"3-4"); legend(c,xx+dp(70),yy,Color.rgb(255,176,32),"4-5"); legend(c,xx+dp(140),yy,Color.rgb(255,77,77),"5+");
        textPaint.setFakeBoldText(false); textPaint.setColor(Color.argb(160,236,244,255)); textPaint.setTextAlign(Paint.Align.RIGHT);
        c.drawText("Son depremler", w-dp(14), yy+dp(4), textPaint);
    }

    private void legend(Canvas c,float x,float y,int color,String label){ paint.setColor(color); c.drawCircle(x,y,dp(5),paint); textPaint.setColor(Color.argb(210,236,244,255)); c.drawText(label,x+dp(10),y+dp(4),textPaint); }
    private void label(Canvas c,String s,double lon,double lat,int w,int h){ c.drawText(s,x(lon,w),y(lat,h),textPaint); }
    private void line(Canvas c,int w,int h,double lon1,double lat1,double lon2,double lat2){ c.drawLine(x(lon1,w),y(lat1,h),x(lon2,w),y(lat2,h),paint); }
    private float x(double lon,int w){ float l=dp(14), r=w-dp(14); return (float)(l+((lon-minLon)/(maxLon-minLon))*(r-l)); }
    private float y(double lat,int h){ float t=dp(24), b=h-dp(42); return (float)(t+((maxLat-lat)/(maxLat-minLat))*(b-t)); }
    private int col(double m){ if(m>=5)return Color.rgb(255,77,77); if(m>=4)return Color.rgb(255,176,32); return Color.rgb(77,163,255); }
    private float rad(double m){ if(m>=6)return dp(10); if(m>=5)return dp(8); if(m>=4)return dp(6); return dp(4); }
    private int dp(int v){ return Math.round(v*getResources().getDisplayMetrics().density); }
}
