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


V1.1 DÜZELTME:
- Seçim kutularındaki siyah yazı problemi düzeltildi.
- Yazılar beyaz ve kalın yapıldı.


V1.2 GELİŞTİRME:
- Alarm testi butonu eklendi.
- Deprem bildirimlerine alarm sesi ve titreşim eklendi.
- Depremin yaklaşık Türkiye bölgesi yazdırıldı.
- Haritada aç butonu uygulama içi harita ekranına çevrildi.
- Haritada OpenStreetMap tabanı ve deprem merkez işareti gösterilir.
- Haritada büyüklüğe göre etki dairesi görsel olarak büyür.

NOT:
Harita ekranı için internet gerekir.
Android bazı telefonlarda bildirim sesini sistem ayarlarındaki kanal sesine göre kısabilir.
Uygulama ayarlarından "Deprem Uyarıları" bildirim kanalının sesi açık olmalı.


V1.3 HARİTA DÜZELTMESİ:
- Uygulama içi WebView haritası bazı telefonlarda açılmadığı için değiştirildi.
- "Haritada aç" butonu artık Google Haritalar / tarayıcı haritasını açar.
- Depremin enlem ve boylam noktası doğrudan haritada gösterilir.
- Bu yöntem daha garantili çalışır.


V1.4 PROFESYONEL ANA EKRAN HARİTASI:
- Ana ekrana Türkiye haritası eklendi.
- Depremler harita üzerinde nokta olarak gösterilir.
- Büyüklüğe göre renk:
  Mavi: 3.0-3.9
  Turuncu: 4.0-4.9
  Kırmızı: 5.0+
- Marmara, Ege, Akdeniz, İç Anadolu, Karadeniz, Doğu Anadolu, Güneydoğu Anadolu bölgeleri yazdırılır.
- Kullanıcı konumu haritada mavi nokta olarak görünür.
- Harita liste filtresine göre güncellenir.
- Haritada internet gerekmez; ana ekran haritası native çizimdir.
- Deprem satırındaki "Haritada aç" butonu ayrıca Google Haritalar'da nokta açmaya devam eder.

NOT:
Ana ekrandaki Türkiye haritası profesyonel görünüm için yaklaşık çizimdir.
Deprem noktaları gerçek enlem/boylam koordinatına göre yerleştirilir.


V1.5 GERÇEK HARİTA:
- Basit çizim harita kaldırıldı.
- Ana ekrana gerçek OpenStreetMap haritası eklendi.
- Harita parçaları internetten yüklenir ve uygulama içinde görünür.
- Deprem noktaları gerçek enlem/boylam koordinatına göre harita üstüne yerleşir.
- Haritanın altında kaç harita parçası yüklendiği yazar. Böylece çalışıp çalışmadığı anlaşılır.
- Kullanıcı konumu gerçek harita üzerinde görünür.
- Büyüklüğe göre renkli deprem işareti devam eder:
  Mavi: 3.0-3.9
  Turuncu: 4.0-4.9
  Kırmızı: 5.0+
- Deprem satırındaki Haritada aç butonu Google Haritalar'ı açmaya devam eder.

NOT:
Gerçek haritanın yüklenmesi için internet gerekir.
İlk açılışta harita parçalarının gelmesi birkaç saniye sürebilir.
