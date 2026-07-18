# Google Play Data Safety Taslak Envanteri

Bu belge Play Console'a gönderilmemiştir. Final cevaplar üretim SDK seti ve hukuk incelemesiyle birebir eşleşmelidir.

| Kategori | Toplanıyor mu? | Paylaşılıyor mu? | Amaç | Zorunlu/opsiyonel | Aktarım güvenliği | Silme | Kanıt |
| --- | --- | --- | --- | --- | --- | --- | --- |
| E-posta adresi | Evet | İnceleme: Firebase hizmet sağlayıcı | hesap/giriş/şifre sıfırlama | zorunlu | Firebase/TLS varsayımları review | hesap silme akışı | Firebase Auth, `AuthRepository` |
| Kullanıcı ID | Evet | İnceleme: Firebase | hesap ve kayıt ilişkisi | zorunlu | review | hesap silme | Auth UID, Firestore docs |
| Kullanıcı adı | Evet | Normal kullanıcılara görünür | public profil | zorunlu | review | silme/rezervasyon release | `users`, `usernames` |
| Kullanıcı içerikleri | Evet | Giriş yapmış kullanıcılara görünür | alıntı paylaşımı | opsiyonel içerik | review | silme/gizleme | `quotes` |
| App interactions | Evet | İnceleme | beğeni/favori/başarım | özellik kullanımına bağlı | review | silme | `likes`, `favorites`, `userStats` |
| Raporlar | Evet | admin sınırlı | güvenlik/moderasyon | opsiyonel rapor | review | anonimleştirme | `reports` |
| Diagnostik | Repo içinde Crashlytics/analytics SDK yok | hayır | uygulanmıyor | uygulanmıyor | uygulanmıyor | uygulanmıyor | Gradle bağımlılıkları |
| Cihaz ID | App Check teknik verisi olabilir | Firebase review | güvenlik | güvenlik | review | Firebase policy | `QuoteApp.java`, App Check |
| Reklam verisi | Hayır | Hayır | uygulanmıyor | uygulanmıyor | uygulanmıyor | uygulanmıyor | Ads SDK yok |
| Satın alma/billing | Hayır | Hayır | uygulanmıyor | uygulanmıyor | uygulanmıyor | uygulanmıyor | Billing SDK yok |

Play formu gönderilmeden önce Firebase dokümantasyonu, App Check sağlayıcısı, hedef ülke ve veri silme URL'leri doğrulanmalıdır.
