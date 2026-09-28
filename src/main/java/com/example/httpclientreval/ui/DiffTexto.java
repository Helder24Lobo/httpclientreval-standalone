package com.example.httpclientreval.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Diferencia dos textos línea por línea (LCS, como {@code diff}/{@code git diff}): cada línea de
 * salida queda marcada como igual, agregada (solo en B) o eliminada (solo en A). No detecta líneas
 * "modificadas": un cambio dentro de una línea se ve como su eliminación en A y su agregado en B.
 */
final class DiffTexto {

    enum Tipo { IGUAL, AGREGADA, ELIMINADA }

    static final class Linea {
        final Tipo tipo;
        final String texto;

        Linea(Tipo tipo, String texto) {
            this.tipo = tipo;
            this.texto = texto;
        }
    }

    /** Resultado del cálculo; {@code demasiadoGrande} evita colgar la UI con textos enormes (el algoritmo es O(n·m)). */
    static final class Resultado {
        final List<Linea> lineas;
        final int iguales;
        final int agregadas;
        final int eliminadas;
        final boolean demasiadoGrande;

        private Resultado(List<Linea> lineas, boolean demasiadoGrande) {
            this.lineas = lineas;
            this.demasiadoGrande = demasiadoGrande;
            int ig = 0, ag = 0, el = 0;
            for (Linea l : lineas) {
                switch (l.tipo) {
                    case IGUAL: ig++; break;
                    case AGREGADA: ag++; break;
                    default: el++;
                }
            }
            this.iguales = ig;
            this.agregadas = ag;
            this.eliminadas = el;
        }

        boolean hayDiferencias() {
            return agregadas > 0 || eliminadas > 0;
        }
    }

    /** Por encima de esto (n·m celdas) se corta: para textos de este tamaño el diff tardaría demasiado. */
    private static final int MAX_CELDAS = 4_000_000;

    private DiffTexto() {
    }

    static Resultado calcular(String a, String b) {
        String[] la = dividir(a);
        String[] lb = dividir(b);
        if ((long) la.length * lb.length > MAX_CELDAS) {
            return new Resultado(List.of(), true);
        }

        int n = la.length;
        int m = lb.length;
        // lcs[i][j] = longitud de la subsecuencia común más larga entre la[i..] y lb[j..].
        int[][] lcs = new int[n + 1][m + 1];
        for (int i = n - 1; i >= 0; i--) {
            for (int j = m - 1; j >= 0; j--) {
                lcs[i][j] = la[i].equals(lb[j]) ? lcs[i + 1][j + 1] + 1 : Math.max(lcs[i + 1][j], lcs[i][j + 1]);
            }
        }

        List<Linea> resultado = new ArrayList<>(n + m);
        int i = 0, j = 0;
        while (i < n && j < m) {
            if (la[i].equals(lb[j])) {
                resultado.add(new Linea(Tipo.IGUAL, la[i]));
                i++;
                j++;
            } else if (lcs[i + 1][j] >= lcs[i][j + 1]) {
                resultado.add(new Linea(Tipo.ELIMINADA, la[i]));
                i++;
            } else {
                resultado.add(new Linea(Tipo.AGREGADA, lb[j]));
                j++;
            }
        }
        while (i < n) {
            resultado.add(new Linea(Tipo.ELIMINADA, la[i++]));
        }
        while (j < m) {
            resultado.add(new Linea(Tipo.AGREGADA, lb[j++]));
        }
        return new Resultado(resultado, false);
    }

    private static String[] dividir(String texto) {
        return texto == null || texto.isEmpty() ? new String[0] : texto.split("\n", -1);
    }
}
