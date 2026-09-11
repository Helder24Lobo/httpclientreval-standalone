package com.example.httpclientreval.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;

/**
 * Bloque de salida de solo lectura con botón "Copiar" y, opcionalmente, un
 * badge de estado (ej. "200 - 250ms") para respuestas HTTP.
 */
public class OutputBlock extends JPanel {

    private final JTextArea area = new JTextArea();
    private final JLabel badge = new JLabel(" ");

    public OutputBlock(String titulo) {
        super(new BorderLayout(4, 4));
        setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        badge.setOpaque(true);
        badge.setBorder(BorderFactory.createEmptyBorder(1, 8, 1, 8));
        badge.setFont(badge.getFont().deriveFont(Font.BOLD, 11f));
        badge.setVisible(false);

        JButton copiar = new JButton("Copiar", Icons.copiar());
        copiar.addActionListener(e -> ClipboardUtil.copiar(area.getText()));

        JPanel izquierda = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        izquierda.add(new JLabel(titulo));
        izquierda.add(badge);

        JPanel norte = new JPanel(new BorderLayout());
        norte.add(izquierda, BorderLayout.WEST);
        norte.add(copiar, BorderLayout.EAST);

        add(norte, BorderLayout.NORTH);
        add(new JScrollPane(area), BorderLayout.CENTER);
    }

    public void setTexto(String texto) {
        area.setText(texto);
        area.setCaretPosition(0);
    }

    /** Muestra un badge de estado (ej. "200 - 250ms"), verde si éxito o rojo si no. */
    public void setBadge(String texto, boolean exito) {
        badge.setText(texto);
        badge.setForeground(Color.WHITE);
        badge.setBackground(exito ? new Color(46, 125, 50) : new Color(198, 40, 40));
        badge.setVisible(true);
    }

    public void ocultarBadge() {
        badge.setVisible(false);
    }
}
