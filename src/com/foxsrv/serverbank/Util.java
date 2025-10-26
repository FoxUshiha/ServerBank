package com.foxsrv.serverbank;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class Util {

    private Util() {}

    /**
     * Faz parse de valor aceitando vírgula ou ponto e trunca para 4 casas depois.
     */
    public static BigDecimal parseAmount(String raw) {
        if (raw == null) return null;
        raw = raw.trim();

        // Aceitar vírgula como decimal
        char decSep = DecimalFormatSymbols.getInstance(Locale.getDefault()).getDecimalSeparator();
        if (raw.indexOf(',') >= 0 && raw.indexOf('.') < 0) {
            raw = raw.replace(',', '.'); // normaliza para ponto
        }

        try {
            BigDecimal bd = new BigDecimal(raw);
            if (bd.scale() > 8) {
                // Limita entrada a 8 casas para evitar números tolos antes de truncar para 4
                bd = bd.setScale(8, RoundingMode.DOWN);
            }
            return truncate4(bd);
        } catch (Exception e) {
            return null;
        }
    }

    /** Trunca (NÃO arredonda) para 4 casas decimais. */
    public static BigDecimal truncate4(BigDecimal bd) {
        if (bd == null) return null;
        if (bd.scale() <= 4) {
            return bd.setScale(4, RoundingMode.DOWN);
        }
        return bd.setScale(4, RoundingMode.DOWN);
    }

    /** Percentual como número “em %”: ex: "100" => 100%, "0.1" => 0.1% */
    public static Double parsePercent(String raw) {
        if (raw == null) return null;
        raw = raw.trim();
        if (raw.indexOf(',') >= 0 && raw.indexOf('.') < 0) {
            raw = raw.replace(',', '.');
        }
        try {
            return Double.parseDouble(raw);
        } catch (Exception e) {
            return null;
        }
    }
}
