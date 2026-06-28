package com.murat.depremnobetcisi;

public class Quake {
    public String id;
    public double mag;
    public String place;
    public String timeText;
    public long timeMs;
    public double depth;
    public double lat;
    public double lon;
    public String source;
    public String region;
    public double distanceKm = -1;

    public Quake(String id, double mag, String place, String timeText, long timeMs,
                 double depth, double lat, double lon, String source) {
        this.id = id;
        this.mag = mag;
        this.place = place;
        this.timeText = timeText;
        this.timeMs = timeMs;
        this.depth = depth;
        this.lat = lat;
        this.lon = lon;
        this.source = source;
        this.region = EarthquakeApi.regionOf(lat, lon, place);
    }
}
