# Satır Arası Gizlilik Politikası Taslağı

Yürürlük tarihi: `[YÜRÜRLÜK TARİHİ EKLENECEK]`  
Versiyon: `[POLİTİKA VERSİYONU EKLENECEK]`  
Veri sorumlusu / geliştirici: `[VERİ SORUMLUSU ADI EKLENECEK]`  
Adres: `[ADRES EKLENECEK]`  
İletişim: `[GİZLİLİK İLETİŞİM E-POSTASI EKLENECEK]`

> Bu belge, depodaki mevcut Android, Firebase, Firestore, admin panel ve dokümantasyon kanıtlarına göre hazırlanmış v1.0 taslağıdır. Hukuki tavsiye değildir; yayın öncesinde geliştirici kimliği, iletişim bilgileri, saklama süreleri, yurt dışı aktarım ve hukuki dayanaklar hukuk danışmanı tarafından doğrulanmalıdır.

## Kapsam

Bu politika Satır Arası mobil uygulamasında hesap oluşturma, giriş, alıntı paylaşma, keşfetme, beğenme, favorilere kaydetme, raporlama, başarım/XP/seviye, profil görüntüleme ve hesap silme talebi süreçlerini kapsar.

## İşlenen veri kategorileri

| Veri | Kaynak / kanıt | Amaç | Görünürlük |
| --- | --- | --- | --- |
| E-posta ve Firebase kullanıcı kimliği | Firebase Authentication, `AuthRepository` | hesap oluşturma, giriş, şifre sıfırlama | özel / Firebase |
| Kullanıcı adı ve `usernameLowercase` | `users`, `usernames` | public kimlik ve benzersizlik | kullanıcı adı diğer kullanıcılara görünür |
| Alıntı içeriği | `quotes` | alıntı paylaşımı, arama, keşif | görünür alıntılar giriş yapmış kullanıcılara açık |
| Beğeniler | `likes` | sosyal etkileşim ve sayaçlar | sosyal etkileşim verisi |
| Favoriler / kaydedilenler | `favorites` | özel koleksiyon | kullanıcının kendisine özel |
| Başarım, XP, seviye ve istatistikler | `achievements`, `userAchievements`, `userStats`, `levels` | oyunlaştırma ve profil göstergeleri | profil bağlamında kısmen görünür |
| Rapor ve moderasyon | `reports`, `moderationActions`, `reporterStats`, `moderationStats`, `moderatorStats`, `userRestrictions` | güvenlik, kötüye kullanım önleme, içerik inceleme | admin/moderatör sınırlı |
| Hesap silme talepleri | `accountDeletionRequests`, `accountDeletionActions` | silme talebi ve denetim | kullanıcı kendi talebini görebilir, admin sınırlı |
| Güvenlik / App Check | Firebase App Check | kötüye kullanım azaltma | Firebase hizmet verisi |

## Kullanım amaçları

Veriler uygulamayı çalıştırmak, hesap güvenliğini sağlamak, kullanıcı içeriklerini göstermek, kişisel favorileri yönetmek, raporları incelemek, kötüye kullanımı önlemek, başarımları/istatistikleri hesaplamak ve hesap silme taleplerini yürütmek için işlenir.

## Firebase ve hizmet sağlayıcılar

Uygulama Firebase Authentication, Cloud Firestore ve Firebase App Check kullanır. Bu hizmetler uygulamanın kimlik doğrulama, veri saklama ve güvenlik altyapısıdır. Firebase altyapısının veri aktarımı ve teknik log işleme koşulları için Google/Firebase sözleşmeleri ve bölgesel ayarlar hukuk incelemesi gerektirir.

## Kamusal içerik

Kullanıcı adı, herkese açık profil istatistikleri ve görünür alıntı içerikleri diğer giriş yapmış kullanıcılara gösterilebilir. E-posta, favori listesi ve hesap silme talebi normal kullanıcılara açık değildir.

## Saklama, silme ve anonimleştirme

Hesap silme talebi uygulama içinden başlatılabilir. Admin SDK tabanlı yerel tamamlanma akışı Firebase Auth hesabını, kullanıcı profilini, alıntıları, beğenileri, favorileri, başarımları ve istatistikleri kaldırmayı hedefler. Raporlar ve moderasyon kayıtları kötüye kullanım önleme ve denetim gerekçesiyle anonimleştirilmiş şekilde sınırlı süre tutulabilir.

Saklama süreleri: `[SAKLAMA SÜRELERİ HUKUK İNCELEMESİ GEREKTİRİR]`

## Haklar ve iletişim

Kullanıcılar uygulanabilir veri koruma mevzuatı kapsamındaki haklarını kullanmak için şu adrese başvurabilir: `[BAŞVURU / GİZLİLİK E-POSTASI EKLENECEK]`.

## Çocuklar ve yaş

Satır Arası’nın hedef yaşı ve çocuklara yönelik olup olmadığı ürün kararı gerektirir. Yaş toplama, ebeveyn izni ve içerik derecelendirmesi yayın öncesi değerlendirilecektir.

## Güvenlik

Firestore Rules, App Check ve Firebase Authentication kullanılır. Bu belge, platform davranışının ötesinde özel şifreleme garantisi vermez.

## Değişiklikler

Bu politika değişebilir. Güncel sürüm uygulama içinde ve ileride herkese açık web sayfasında gösterilmelidir.
