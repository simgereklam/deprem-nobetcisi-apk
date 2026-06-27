DEPREM NÖBETÇİSİ - APK OLUŞTURMA

Bu paket hazır Android APK proje paketidir.
Android Studio kurmana gerek yok.
GitHub Actions APK'yı otomatik oluşturur.

NE VAR?
- Native Android uygulama
- Son depremler listesi
- AFAD / SonDepremler.live / USGS yedek veri sırası
- Büyüklük filtresi
- Yakınlık filtresi
- Konum kaydetme
- Haritada açma
- Arka plan için "Nöbeti başlat" servisi
- 60 saniyede bir yeni deprem kontrolü
- Eşik üstü yeni depremde Android bildirimi

ÖNEMLİ:
Bu uygulama depremi önceden tahmin etmez.
Resmi erken uyarı sistemi değildir.
Deprem olduktan sonra veri kaynaklarına düşen kayıtları takip eder.

GITHUB'DA APK ALMA:
1) Bu ZIP dosyasını bilgisayarda ayıkla.
2) GitHub'da yeni repo aç: deprem-nobetcisi-apk
3) Ayıklanan klasörün içindeki TÜM dosya ve klasörleri repo içine yükle.
   Özellikle şu klasör de yüklü olmalı:
   .github/workflows/build-apk.yml
4) Commit changes de.
5) GitHub üst menüden Actions sekmesine gir.
6) "Android APK Oluştur" çalışacak.
   Çalışmazsa "Run workflow" butonuna bas.
7) İşlem bitince en altta "Artifacts" bölümünde:
   Deprem-Nobetcisi-APK
   dosyasını indir.
8) İçinden app-debug.apk çıkar.
9) Telefona atıp kur.
10) Telefon izin isterse "Bilinmeyen uygulamalara izin ver" de.

TELEFONDA KULLANIM:
1) Uygulamayı aç.
2) Bildirim izni ver.
3) Konum istersen "Konumumu al" de.
4) Bildirim eşiğini seç.
5) Arka plan uyarısı için "Nöbeti başlat" de.
6) Bildirim çubuğunda "Deprem Nöbetçisi aktif" görünürse servis çalışıyor.

NOT:
Android arka plan kısıtları telefondan telefona değişir.
En sağlam kullanım için pil tasarrufundan bu uygulamayı hariç tut.
