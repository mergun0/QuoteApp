# Veri Saklama ve Silme Matrisi

Saklama süreleri taslaktır; final süreler hukuki ve operasyonel inceleme gerektirir.

| Veri / koleksiyon | Veri sahibi | Amaç | Görünürlük | Mevcut silme davranışı | Anonimleştirme | Saklama süresi | Teknik yöntem |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Firebase Auth user | kullanıcı | giriş/kimlik | özel | admin silme fazında Auth delete | yok | `[SÜRE]` | Admin SDK |
| `users/{uid}` | kullanıcı | profil | public username, özel state | silinir | pending flag önce set edilir | `[SÜRE]` | Admin SDK / Rules |
| `usernames/{username}` | kullanıcı | benzersizlik | auth kullanıcı get | silinir | yok | hesap süresi | Admin SDK |
| `quotes` | quote sahibi | içerik paylaşımı | görünür quote public | sahibin hesap silmede silinir | rapor bağlamı ayrı | `[SÜRE]` | query delete |
| `likes` | beğenen kullanıcı | sosyal etkileşim | auth kullanıcılarca okunabilir | kullanıcı veya quote silmede silinir | yok | `[SÜRE]` | query delete |
| `favorites` | kaydeden kullanıcı | özel koleksiyon | sadece sahibi | kullanıcı veya quote silmede silinir | yok | `[SÜRE]` | query delete |
| `userAchievements` | kullanıcı | başarımlar | auth kullanıcılarca okunabilir | hesap silmede silinir | yok | `[SÜRE]` | query delete |
| `userStats` | kullanıcı | XP/seviye/istatistik | auth kullanıcılarca okunabilir | hesap silmede silinir | yok | `[SÜRE]` | doc delete |
| `reports` | reporter/reported | güvenlik | kendi raporu get, admin | kullanıcı eşleşmelerinde anonimleştirilir | `deleted_<hash>` | `[SÜRE]` | query update |
| `moderationActions` | admin/moderasyon | denetim | admin-only | hedef kullanıcı anonimleştirilebilir | `deleted_<hash>` | `[SÜRE]` | query update |
| `reporterStats` | kullanıcı | kötüye kullanım önleme | admin-only | hesap silmede doc silinir | gerekirse anonim | `[SÜRE]` | doc delete |
| `moderationStats` | sistem/kullanıcı | moderasyon metrikleri | admin-only | hesap silmede doc silinir | gerekirse anonim | `[SÜRE]` | doc delete |
| `moderatorStats` | moderatör | admin metrikleri | admin-only | hesap silmede doc silinir | gerekirse anonim | `[SÜRE]` | doc delete |
| `userRestrictions` | kullanıcı | güvenlik kısıtı | admin-only | hesap silmede doc silinir | gerekirse anonim | `[SÜRE]` | doc delete |
| `accountDeletionRequests` | kullanıcı | silme talebi | kullanıcı kendi get, admin | sanitized retained | kullanıcı adı temizlenir | `[SÜRE]` | Admin SDK |
| `accountDeletionActions` | sistem/admin | denetim | admin-only | retained sanitized | kişisel alan yok | `[SÜRE]` | Admin SDK |
| Local admin logs | admin makinesi | hata/denetim | yerel admin | manuel log yönetimi | teknik detaylar sınırlandırılmalı | `[SÜRE]` | yerel operasyon |
