package com.example.httpclientreval.ui;

import com.example.httpclientreval.model.Profile;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import java.awt.Component;
import java.util.List;
import java.util.function.ToIntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Contador de transacciones junto al nombre de cada grupo en los combos de grupos (ej. "Recaudos (5)").
 * El contador es solo visual: los ítems del combo siguen siendo el nombre del grupo tal cual, así que
 * {@code getSelectedItem()} y el editor de los combos editables devuelven el nombre sin el "(N)".
 */
final class ConteoGrupos {

    /** Un " (N)" al final del texto, como lo pinta {@link #conConteo(String, int)}. */
    private static final Pattern SUFIJO_CONTEO = Pattern.compile("^(.*\\S)\\s*\\((\\d+)\\)$");

    private ConteoGrupos() {
    }

    /** Cuántos perfiles pertenecen al grupo (según {@link Profile#grupo()}). */
    static int contar(List<Profile> perfiles, String grupo) {
        int total = 0;
        for (Profile p : perfiles) {
            if (p.grupo().equals(grupo)) {
                total++;
            }
        }
        return total;
    }

    /** "Recaudos (5)"; sin contador si el grupo no tiene perfiles (p. ej. uno recién escrito). */
    static String conConteo(String grupo, int total) {
        return total > 0 ? grupo + " (" + total + ")" : grupo;
    }

    /**
     * Nombre del grupo sin el contador, por si el texto llega con él (p. ej. copiado de la lista y
     * pegado en un combo editable). Solo lo quita si lo que queda es un grupo existente, para no
     * recortar un grupo nuevo cuyo nombre termine legítimamente en "(2024)".
     */
    static String nombreBase(String texto, List<Profile> perfiles) {
        if (texto == null) {
            return "";
        }
        String limpio = texto.trim();
        Matcher m = SUFIJO_CONTEO.matcher(limpio);
        if (m.matches() && contar(perfiles, m.group(1).trim()) > 0) {
            return m.group(1).trim();
        }
        return limpio;
    }

    /** Pinta cada grupo del combo con su contador; {@code conteo} se consulta al pintar, así nunca queda desfasado. */
    static void mostrarConteo(JComboBox<String> combo, ToIntFunction<String> conteo) {
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                Object texto = value instanceof String grupo ? conConteo(grupo, conteo.applyAsInt(grupo)) : value;
                return super.getListCellRendererComponent(list, texto, index, isSelected, cellHasFocus);
            }
        });
    }
}
