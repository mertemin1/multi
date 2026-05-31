import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * RESPONSE mesajlarını Fonksiyonel Programlama (FP) tarzında işleyen
 * yardımcı fonksiyonlar.
 *
 * Amaç:
 *  - map / filter / reduce zincirini gerçek bir kullanımda göstermek
 *  - Optional ile hata/boş durumları yan etkisiz yönetmek
 */
public final class ResponseProcessing {

    private ResponseProcessing() {
        // statik yardımcı sınıf
    }

    /**
     * Tek bir ham seri mesajını (ör: "RESPONSE", "response", "  RESPONSE  ")
     * anlamlı bir domain değeri olan Optional<Boolean>'a dönüştürür.
     *
     * true  -> Geçerli "RESPONSE" mesajı (hasta butona bastı)
     * empty -> Geçersiz / ilgisiz mesaj
     */
    public static Optional<Boolean> parseResponse(String rawMessage) {
        if (rawMessage == null) {
            return Optional.empty();
        }
        String normalized = rawMessage.trim().toUpperCase(Locale.ROOT);
        if ("RESPONSE".equals(normalized)) {
            return Optional.of(Boolean.TRUE);
        }
        return Optional.empty();
    }

    /**
     * map / filter / reduce zinciriyle, ham mesaj listesinden
     * toplam geçerli RESPONSE sayısını hesaplar.
     */
    public static long countValidResponses(List<String> rawMessages) {
        return rawMessages.stream()
                // map: her ham mesajı Optional<Boolean>'a çevir
                .map(ResponseProcessing::parseResponse)
                // filter: sadece gerçekten RESPONSE olanlar (present) kalsın
                .filter(Optional::isPresent)
                // map: Optional içindeki Boolean değeri al
                .map(Optional::get)
                // reduce: toplam sayıyı hesapla
                .reduce(0L, (acc, value) -> acc + 1L, Long::sum);
    }

    /**
     * Ham mesaj listesinden, ilk geçerli RESPONSE'u bulup döndürür.
     * Hiç yoksa Optional.empty() döner.
     */
    public static Optional<String> firstValidResponse(List<String> rawMessages) {
        return rawMessages.stream()
                .filter(m -> parseResponse(m).isPresent())
                .findFirst();
    }
}

