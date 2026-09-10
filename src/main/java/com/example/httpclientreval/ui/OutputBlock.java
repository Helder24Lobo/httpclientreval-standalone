package com.example.httpclientreval.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Font;

/**
 * Bloque de salida de solo lectura con botón "Copiar", para no tener que
 * seleccionar el texto a mano (mensaje cifrado, sobre JSON, XML SOAP, etc.).
 */
public class OutputBlock extends JPanel {

    private final JTextArea area = new JTextArea();

    public OutputBlock(String titulo) {
        super(new BorderLayout(4, 4));
        setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JButton copiar = new JButton("Copiar");
        copiar.addActionListener(e -> ClipboardUtil.copiar(area.getText()));

        JPanel norte = new JPanel(new BorderLayout());
        norte.add(new JLabel(titulo), BorderLayout.WEST);
        norte.add(copiar, BorderLayout.EAST);

        add(norte, BorderLayout.NORTH);
        add(new JScrollPane(area), BorderLayout.CENTER);
    }

    public void setTexto(String texto) {
        area.setText(texto);
        area.setCaretPosition(0);
    }
}
