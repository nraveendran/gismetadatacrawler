package org.example.gismetadata.embedding;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public final class PgVector {
    private PgVector() {
    }

    public static String toLiteral(List<BigDecimal> values) {
        return values.stream()
                .map(BigDecimal::toPlainString)
                .collect(Collectors.joining(",", "[", "]"));
    }
}
