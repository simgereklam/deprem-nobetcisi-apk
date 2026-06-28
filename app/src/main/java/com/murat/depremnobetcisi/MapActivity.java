package com.murat.depremnobetcisi;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.graphics.Color;

public class MapActivity extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);

        double lat = getIntent().getDoubleExtra("lat", 39.0);
        double lon = getIntent().getDoubleExtra("lon", 35.0);
        double mag = getIntent().getDoubleExtra("mag", 0);
        String place = getIntent().getStringExtra("place");
        String region = getIntent().getStringExtra("region");
        String time = getIntent().getStringExtra("time");
        if (place == null) place = "Deprem";
        if (region == null) region = "";

        WebView web = new WebView(this);
        web.setBackgroundColor(Color.rgb(7,17,31));
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);

        String html = buildHtml(lat, lon, mag, place, region, time);
        web.loadDataWithBaseURL("https://www.openstreetmap.org/", html, "text/html", "UTF-8", null);
        setContentView(web);
    }

    private String esc(String x) {
        if (x == null) return "";
        return x.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String buildHtml(double lat, double lon, double mag, String place, String region, String time) {
        int radius = mag >= 5 ? 26000 : mag >= 4 ? 17000 : 10000;
        String color = mag >= 5 ? "#ff4d4d" : mag >= 4 ? "#ffb020" : "#4da3ff";

        return "<!doctype html><html><head>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'>"
                + "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>"
                + "<style>html,body,#map{height:100%;margin:0;background:#07111f} .info{position:absolute;z-index:999;left:10px;right:10px;top:10px;background:#0f1f33;color:#ecf4ff;border:1px solid rgba(255,255,255,.18);border-radius:16px;padding:12px;font-family:Arial;box-shadow:0 12px 30px rgba(0,0,0,.25)} .info b{color:#ffb020;font-size:18px}</style>"
                + "</head><body>"
                + "<div id='map'></div>"
                + "<div class='info'><b>" + esc(String.format(java.util.Locale.US, "%.1f", mag)) + " büyüklüğünde deprem</b><br>"
                + esc(place) + "<br>" + esc(region) + " • " + esc(time) + "</div>"
                + "<script>"
                + "var map=L.map('map',{zoomControl:true}).setView([" + lat + "," + lon + "],8);"
                + "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:18,attribution:'© OpenStreetMap'}).addTo(map);"
                + "L.circle([" + lat + "," + lon + "],{radius:" + radius + ",color:'" + color + "',fillColor:'" + color + "',fillOpacity:.25,weight:3}).addTo(map);"
                + "L.marker([" + lat + "," + lon + "]).addTo(map).bindPopup('" + esc(place) + "<br>" + esc(region) + "<br>M: " + String.format(java.util.Locale.US, "%.1f", mag) + "').openPopup();"
                + "</script></body></html>";
    }
}
