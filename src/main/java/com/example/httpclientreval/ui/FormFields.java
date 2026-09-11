package com.example.httpclientreval.ui;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.util.Map;

/**
 * Helpers para armar formularios de campos etiqueta+texto con
 * GridBagLayout, compartidos entre EncryptPanel y NewProfilePanel.
 */
final class FormFields {

    private FormFields() {
    }

    static GridBagConstraints gbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 6, 3, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        return gbc;
    }

    static void agregarSeccion(JPanel panel, GridBagConstraints gbc, int[] fila, String titulo) {
        gbc.gridx = 0;
        gbc.gridy = fila[0]++;
        gbc.gridwidth = 2;
        JLabel etiqueta = new JLabel(titulo);
        etiqueta.setFont(etiqueta.getFont().deriveFont(Font.BOLD));
        panel.add(etiqueta, gbc);
        gbc.gridwidth = 1;
    }

    static void agregarCampo(JPanel panel, GridBagConstraints gbc, int[] fila,
                              Map<String, JTextField> campos, String etiqueta, String valorPorDefecto) {
        gbc.gridx = 0;
        gbc.gridy = fila[0]++;
        gbc.weightx = 0;
        panel.add(new JLabel(etiqueta + ":"), gbc);

        JTextField campo = new JTextField(valorPorDefecto == null ? "" : valorPorDefecto, 22);
        campos.put(etiqueta, campo);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(campo, gbc);
    }

    static JScrollPane envolver(JPanel contenido) {
        JScrollPane scroll = new JScrollPane(contenido);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setBorder(null);
        return scroll;
    }
}
