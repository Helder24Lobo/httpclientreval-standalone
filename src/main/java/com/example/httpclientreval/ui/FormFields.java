package com.example.httpclientreval.ui;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Color;
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

    /**
     * Igual que {@link #agregarCampo}, pero para secretos (llave AES, contraseñas): el campo enmascara lo
     * escrito y un botón con un ojo lo muestra u oculta. Sigue registrado en {@code campos} como JTextField
     * (un JPasswordField lo es), así que se lee con getText() como los demás.
     */
    static void agregarCampoSecreto(JPanel panel, GridBagConstraints gbc, int[] fila,
                                    Map<String, JTextField> campos, String etiqueta, String valorPorDefecto) {
        gbc.gridx = 0;
        gbc.gridy = fila[0]++;
        gbc.weightx = 0;
        panel.add(new JLabel(etiqueta + ":"), gbc);

        JPasswordField campo = new JPasswordField(valorPorDefecto == null ? "" : valorPorDefecto, 22);
        char ecoOculto = campo.getEchoChar();

        JToggleButton ver = new JToggleButton(Icons.ojo());
        ver.setToolTipText("Mostrar");
        ver.setFocusable(false);
        ver.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        ver.addActionListener(e -> {
            boolean mostrar = ver.isSelected();
            campo.setEchoChar(mostrar ? (char) 0 : ecoOculto);
            // Un JPasswordField bloquea copiar/cortar siempre; solo se permite mientras el secreto está a la vista.
            campo.putClientProperty("JPasswordField.cutCopyAllowed", mostrar);
            ver.setIcon(mostrar ? Icons.ojoTachado() : Icons.ojo());
            ver.setToolTipText(mostrar ? "Ocultar" : "Mostrar");
        });

        // El ojo va dentro del recuadro para que todos los campos midan lo mismo.
        campo.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, ver);

        campos.put(etiqueta, campo);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(campo, gbc);
    }

    /**
     * Aplica validación en vivo a un campo numérico entero.
     * Muestra un borde rojo destacado y un tooltip si el usuario escribe caracteres no numéricos.
     */
    static void aplicarValidacionNumerica(JTextField campo) {
        if (campo == null) {
            return;
        }
        Border borderOriginal = campo.getBorder();
        Border borderError = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(211, 47, 47), 2),
                BorderFactory.createEmptyBorder(2, 4, 2, 4)
        );

        Runnable validar = () -> {
            String texto = campo.getText().trim();
            if (!texto.isEmpty() && !texto.matches("\\d+")) {
                campo.setBorder(borderError);
                campo.setToolTipText("Este campo solo permite números enteros positivos.");
            } else {
                campo.setBorder(borderOriginal);
                campo.setToolTipText(null);
            }
        };

        campo.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                validar.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                validar.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                validar.run();
            }
        });
    }

    /**
     * Valida si el campo contiene un número entero válido.
     * Si está vacío o contiene caracteres no numéricos, resalta con borde rojo y retorna false.
     */
    static boolean esEnteroValido(JTextField campo) {
        if (campo == null) {
            return false;
        }
        String texto = campo.getText().trim();
        boolean valido = !texto.isEmpty() && texto.matches("\\d+");
        if (!valido) {
            Border borderError = BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(211, 47, 47), 2),
                    BorderFactory.createEmptyBorder(2, 4, 2, 4)
            );
            campo.setBorder(borderError);
            campo.setToolTipText("Este campo debe ser un número entero (solo dígitos).");
        }
        return valido;
    }

    static JScrollPane envolver(JPanel contenido) {
        JScrollPane scroll = new JScrollPane(contenido);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setBorder(null);
        return scroll;
    }
}
