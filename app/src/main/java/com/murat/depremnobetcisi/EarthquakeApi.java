package com.murat.depremnobetcisi;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class EarthquakeApi {
    public static class Result {
        public String sourceName;
        public List<Quake> quakes;
        public Result(String sourceName, List<Quake> quakes) {
            this.sourceName = sourceName;
            this.quakes = quakes;
        }
    }

    public static Result fetchAny() throws Exception {
        List<String> errors = new ArrayList<>();

        try {
            List<Quake> q = parseAfad(httpGet("https://deprem.afad.gov.tr/apiv2/event/latest"));
            if (!q.isEmpty()) return new Result("AFAD", sort(q));
        } catch (Exception e) {
            errors.add("AFAD: " + e.getMessage());
        }

        try {
            List<Quake> q = parseSonDepremler(httpGet("https://sondepremler.live/api/earthquakes?limit=100&hours=72"));
            if (!q.isEmpty()) return new Result("SonDepremler.live", sort(q));
        } catch (Exception e) {
            errors.add("SonDepremler: " + e.getMessage());
        }

        try {
            List<Quake> q = parseUsgs(httpGet(usgsUrl()));
            if (!q.isEmpty()) return new Result("USGS", sort(q));
        } catch (Exception e) {
            errors.add("USGS: " + e.getMessage());
        }

        throw new Exception("Veri alınamadı: " + errors.toString());
    }

    private static String httpGet(String urlText) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(urlText).openConnection();
        c.setConnectTimeout(10000);
        c.setReadTimeout(10000);
        c.setRequestMethod("GET");
        c.setRequestProperty("User-Agent", "DepremNobetcisi/1.0 Android");
        int code = c.getResponseCode();
        if (code < 200 || code >= 300) throw new Exception("HTTP " + code);

        BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        return sb.toString();
    }

    private static String usgsUrl() {
        long weekAgo = System.currentTimeMillis() - 7L * 24L * 60L * 60L * 1000L;
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        String start = f.format(new Date(weekAgo));
        return "https://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson"
                + "&starttime=" + start
                + "&minlatitude=35&maxlatitude=43&minlongitude=25&maxlongitude=46"
                + "&minmagnitude=1&orderby=time";
    }

    private static List<Quake> parseAfad(String json) throws Exception {
        JSONArray arr = new JSONArray(json);
        List<Quake> out = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            double mag = optDoubleAny(o, "magnitude", "mag", "ml", "mw");
            double lat = optDoubleAny(o, "latitude", "lat");
            double lon = optDoubleAny(o, "longitude", "lon", "lng");
            if (mag <= 0 || !validCoord(lat, lon)) continue;

            String t = optStringAny(o, "date", "time", "eventDate", "datetime");
            long tm = parseTime(t);
            String id = "afad-" + optStringAny(o, "eventID", "eventId", "id", "eventid");
            if (id.equals("afad-")) id = "afad-" + tm + "-" + mag + "-" + lat + "-" + lon;

            out.add(new Quake(
                    id,
                    mag,
                    optStringAny(o, "location", "region", "province", "district", "address", "Konum bilgisi yok"),
                    formatTime(tm),
                    tm,
                    optDoubleAny(o, "depth", "depthKm", "depth_km"),
                    lat,
                    lon,
                    "AFAD"
            ));
        }
        return out;
    }

    private static List<Quake> parseSonDepremler(String json) throws Exception {
        Object root = json.trim().startsWith("[") ? new JSONArray(json) : new JSONObject(json);
        JSONArray arr;
        if (root instanceof JSONArray) {
            arr = (JSONArray) root;
        } else {
            JSONObject obj = (JSONObject) root;
            arr = obj.optJSONArray("data");
            if (arr == null) arr = obj.optJSONArray("earthquakes");
            if (arr == null) arr = obj.optJSONArray("result");
            if (arr == null) throw new Exception("liste yok");
        }

        List<Quake> out = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            double mag = optDoubleAny(o, "magnitude", "mag", "ml", "mw");
            double lat = optDoubleAny(o, "latitude", "lat");
            double lon = optDoubleAny(o, "longitude", "lon", "lng");
            if (mag <= 0 || !validCoord(lat, lon)) continue;

            String city = optStringAny(o, "city", "province");
            String district = optStringAny(o, "district", "town");
            String region = optStringAny(o, "region", "location", "place");
            String place = joinPlace(region, city, district);
            String t = optStringAny(o, "date", "time", "datetime");
            long tm = parseTime(t);
            String id = "sd-" + optStringAny(o, "id", "earthquake_id");
            if (id.equals("sd-")) id = "sd-" + tm + "-" + mag + "-" + lat + "-" + lon;

            out.add(new Quake(id, mag, place, formatTime(tm), tm,
                    optDoubleAny(o, "depth", "depthKm", "depth_km"), lat, lon, "SonDepremler.live"));
        }
        return out;
    }

    private static List<Quake> parseUsgs(String json) throws Exception {
        JSONObject obj = new JSONObject(json);
        JSONArray arr = obj.getJSONArray("features");
        List<Quake> out = new ArrayList<>();

        for (int i = 0; i < arr.length(); i++) {
            JSONObject f = arr.getJSONObject(i);
            JSONObject p = f.getJSONObject("properties");
            JSONArray c = f.getJSONObject("geometry").getJSONArray("coordinates");

            double mag = p.optDouble("mag", 0);
            double lon = c.optDouble(0);
            double lat = c.optDouble(1);
            double depth = c.optDouble(2);
            long tm = p.optLong("time", System.currentTimeMillis());
            if (mag <= 0 || !validCoord(lat, lon)) continue;

            out.add(new Quake("usgs-" + f.optString("id"),
                    mag,
                    p.optString("place", "Konum bilgisi yok"),
                    formatTime(tm),
                    tm,
                    depth,
                    lat,
                    lon,
                    "USGS"));
        }
        return out;
    }

    private static List<Quake> sort(List<Quake> q) {
        Collections.sort(q, new Comparator<Quake>() {
            @Override public int compare(Quake a, Quake b) {
                return Long.compare(b.timeMs, a.timeMs);
            }
        });
        return q;
    }

    private static boolean validCoord(double lat, double lon) {
        return lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180 && !(lat == 0 && lon == 0);
    }

    private static double optDoubleAny(JSONObject o, String... keys) {
        for (String k : keys) {
            if (o.has(k) && !o.isNull(k)) {
                try { return Double.parseDouble(String.valueOf(o.get(k)).replace(",", ".")); }
                catch (Exception ignored) {}
            }
        }
        return 0;
    }

    private static String optStringAny(JSONObject o, String... keys) {
        String fallback = "";
        if (keys.length > 0 && "Konum bilgisi yok".equals(keys[keys.length-1])) fallback = "Konum bilgisi yok";
        for (String k : keys) {
            if ("Konum bilgisi yok".equals(k)) continue;
            if (o.has(k) && !o.isNull(k)) {
                String v = String.valueOf(o.opt(k)).trim();
                if (!v.isEmpty() && !"null".equalsIgnoreCase(v)) return v;
            }
        }
        return fallback;
    }

    private static String joinPlace(String a, String b, String c) {
        StringBuilder sb = new StringBuilder();
        if (a != null && !a.trim().isEmpty()) sb.append(a.trim());
        if (b != null && !b.trim().isEmpty() && !sb.toString().contains(b.trim())) {
            if (sb.length() > 0) sb.append(" / ");
            sb.append(b.trim());
        }
        if (c != null && !c.trim().isEmpty() && !sb.toString().contains(c.trim())) {
            if (sb.length() > 0) sb.append(" / ");
            sb.append(c.trim());
        }
        return sb.length() == 0 ? "Konum bilgisi yok" : sb.toString();
    }

    public static long parseTime(String text) {
        if (text == null || text.trim().isEmpty()) return System.currentTimeMillis();
        String t = text.trim();
        String[] patterns = new String[]{
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss.SSS",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        };
        for (String p : patterns) {
            try {
                SimpleDateFormat f = new SimpleDateFormat(p, Locale.US);
                if (p.endsWith("'Z'")) f.setTimeZone(TimeZone.getTimeZone("UTC"));
                return f.parse(t).getTime();
            } catch (Exception ignored) {}
        }
        try { return Long.parseLong(t); } catch (Exception ignored) {}
        return System.currentTimeMillis();
    }

    public static String formatTime(long tm) {
        SimpleDateFormat f = new SimpleDateFormat("dd.MM.yyyy HH:mm", new Locale("tr", "TR"));
        return f.format(new Date(tm));
    }


    public static String regionOf(double lat, double lon, String place) {
        String p = place == null ? "" : place.toLowerCase(new Locale("tr", "TR"));

        if (p.contains("marmara") || p.contains("istanbul") || p.contains("tekirdağ") || p.contains("tekirdag")
                || p.contains("kocaeli") || p.contains("sakarya") || p.contains("bursa") || p.contains("yalova")
                || p.contains("balıkesir") || p.contains("balikesir") || p.contains("çanakkale") || p.contains("canakkale")
                || p.contains("edirne") || p.contains("kırklareli") || p.contains("kirklareli")) return "Marmara Bölgesi";

        if (p.contains("ege") || p.contains("izmir") || p.contains("manisa") || p.contains("aydın") || p.contains("aydin")
                || p.contains("muğla") || p.contains("mugla") || p.contains("denizli") || p.contains("uşak") || p.contains("usak")
                || p.contains("kütahya") || p.contains("kutahya") || p.contains("afyon")) return "Ege Bölgesi";

        if (p.contains("akdeniz") || p.contains("antalya") || p.contains("mersin") || p.contains("adana")
                || p.contains("hatay") || p.contains("osmaniye") || p.contains("ısparta") || p.contains("isparta")
                || p.contains("burdur") || p.contains("kahramanmaraş") || p.contains("kahramanmaras")) return "Akdeniz Bölgesi";

        if (p.contains("karadeniz") || p.contains("samsun") || p.contains("trabzon") || p.contains("ordu")
                || p.contains("rize") || p.contains("giresun") || p.contains("sinop") || p.contains("kastamonu")
                || p.contains("zonguldak") || p.contains("bolu") || p.contains("düzce") || p.contains("duzce")
                || p.contains("bartın") || p.contains("bartin") || p.contains("amasya") || p.contains("tokat")
                || p.contains("çorum") || p.contains("corum")) return "Karadeniz Bölgesi";

        if (p.contains("iç anadolu") || p.contains("ic anadolu") || p.contains("ankara") || p.contains("konya")
                || p.contains("kayseri") || p.contains("eskişehir") || p.contains("eskisehir") || p.contains("sivas")
                || p.contains("kırıkkale") || p.contains("kirikkale") || p.contains("kırşehir") || p.contains("kirsehir")
                || p.contains("aksaray") || p.contains("niğde") || p.contains("nigde") || p.contains("nevşehir")
                || p.contains("nevsehir") || p.contains("karaman") || p.contains("yozgat")) return "İç Anadolu Bölgesi";

        if (p.contains("güneydoğu") || p.contains("guneydogu") || p.contains("gaziantep") || p.contains("şanlıurfa")
                || p.contains("sanliurfa") || p.contains("diyarbakır") || p.contains("diyarbakir") || p.contains("mardin")
                || p.contains("batman") || p.contains("siirt") || p.contains("şırnak") || p.contains("sirnak")
                || p.contains("adıyaman") || p.contains("adiyaman") || p.contains("kilis")) return "Güneydoğu Anadolu Bölgesi";

        if (p.contains("doğu anadolu") || p.contains("dogu anadolu") || p.contains("erzurum") || p.contains("erzincan")
                || p.contains("malatya") || p.contains("elazığ") || p.contains("elazig") || p.contains("bingöl")
                || p.contains("bingol") || p.contains("van") || p.contains("ağrı") || p.contains("agri")
                || p.contains("kars") || p.contains("muş") || p.contains("mus") || p.contains("bitlis")
                || p.contains("hakkari") || p.contains("tunceli") || p.contains("ığdır") || p.contains("igdir")
                || p.contains("ardahan")) return "Doğu Anadolu Bölgesi";

        // Koordinata göre yaklaşık Türkiye bölge tahmini
        if (lat >= 40.0 && lon <= 31.5) return "Marmara Bölgesi";
        if (lat < 40.2 && lon <= 30.8) return "Ege Bölgesi";
        if (lat < 38.2 && lon > 30.8 && lon < 37.5) return "Akdeniz Bölgesi";
        if (lat >= 40.0 && lon > 31.5 && lon < 42.5) return "Karadeniz Bölgesi";
        if (lat >= 38.0 && lat < 40.5 && lon >= 30.8 && lon < 38.5) return "İç Anadolu Bölgesi";
        if (lat < 38.8 && lon >= 37.0 && lon < 42.5) return "Güneydoğu Anadolu Bölgesi";
        if (lon >= 38.5) return "Doğu Anadolu Bölgesi";

        return "Bölge tahmini yok";
    }

    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon/2) * Math.sin(dLon/2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }
}
