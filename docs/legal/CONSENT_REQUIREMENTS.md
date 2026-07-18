# Açık Rıza ve Bilgilendirme Karar Matrisi

Bu çalışma mevcut v1.0 kod tabanına göre hazırlanmıştır. Nihai KVKK yorumu hukuk danışmanı gerektirir.

| İşlem | Mevcut durum | Sınıflandırma taslağı | Ayrı açık rıza? | Not |
| --- | --- | --- | --- | --- |
| Hesap oluşturma ve e-posta ile giriş | Firebase Auth | hizmet için gerekli | hukuk incelemesi | KVKK aydınlatması ayrı gösterilmeli |
| Public kullanıcı adı | `users`, `usernames` | hizmet / public kimlik | genellikle ayrı rıza değil, inceleme gerekli | kayıt öncesi açık anlatılmalı |
| Alıntı paylaşma | `quotes` | kullanıcı tarafından public yayın | ayrı rıza yerine açık ürün davranışı | kullanıcı içeriği görünür olur |
| Beğeni | `likes` | sosyal etkileşim | inceleme gerekli | public sosyal sinyal olarak açıklanmalı |
| Favori/kaydetme | `favorites` | özel kişisel koleksiyon | genellikle hizmet | diğer kullanıcılara gösterilmemeli |
| Raporlama | `reports` | güvenlik/kötüye kullanım önleme | genellikle hizmet/güvenlik | kötüye kullanım önleme amacı açık yazılmalı |
| Başarım/XP/seviye | `userStats`, `userAchievements` | oyunlaştırma | ürün/hukuk incelemesi | profil görünürlüğü netleştirilmeli |
| Şifre sıfırlama | Firebase Auth | güvenlik/hizmet | hayır | e-posta varlığı ifşa edilmemeli |
| Hesap silme | `accountDeletionRequests` | hak kullanımı/hizmet | hayır | işlem ve saklama açıklanmalı |
| Analytics | bağımlılık bulunmadı | uygulanmıyor | uygulanmıyor | eklenirse Play/KVKK güncellenir |
| Reklam / kişiselleştirme | bağımlılık bulunmadı | uygulanmıyor | uygulanmıyor | eklenirse ayrıca değerlendirilir |
| Pazarlama e-postası | uygulanmıyor | uygulanmıyor | evet gerekebilir | v1.0'da eklenmedi |
| Push bildirimleri | uygulanmıyor | uygulanmıyor | platform izni/inceleme | v1.0 kapsamı dışı |
| Yurt dışı aktarım | Firebase nedeniyle inceleme gerekli | altyapı aktarımı | hukuk kararı gerekli | ayrı açık rıza/taahhütname ayrımı incelenmeli |
