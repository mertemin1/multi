package com.odyometre.core.processor;

import java.util.List;
import java.util.Optional;

public class ResponseProcessor {

    /**
     * Map / Filter / Reduce zinciri kullanılarak "RESPONSE" içeren stringleri sayar.
     */
    public static long countValidResponses(List<String> rawMessages) {
        return rawMessages.stream()
                .filter(msg -> msg != null)         // null olanları at (Filter)
                .map(String::trim)                  // boşlukları temizle (Map)
                .map(String::toUpperCase)           // büyük harfe çevir (Map)
                .filter(msg -> msg.equals("RESPONSE")) // Sadece RESPONSE olanları al (Filter)
                .count();                           // Say (Reduce)
    }

    /**
     * Optional / Maybe deseni kullanılarak güvenli parse işlemi. Exception atılmaz.
     * Örnek Girdi: "FREQ:1000" veya "INT:30"
     */
    public static Optional<Integer> parseValue(String message, String prefix) {
        return Optional.ofNullable(message)
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(msg -> msg.startsWith(prefix.toUpperCase() + ":"))
                .map(msg -> msg.substring(prefix.length() + 1))
                .flatMap(valStr -> {
                    try {
                        return Optional.of(Integer.parseInt(valStr.trim()));
                    } catch (NumberFormatException e) {
                        return Optional.empty(); // Yan etki (Exception) yaratmadan empty dön
                    }
                });
    }
}
