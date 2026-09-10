package com.example.httpclientreval.ui;

import com.example.httpclientreval.crypto.AES256CBC;
import com.example.httpclientreval.model.Envelope;
import com.example.httpclientreval.model.Profile;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

/**
 * Panel de DECRYPT: pega el _mensaje (o el sobre JSON completo), botón
 * Descifrar, salida con botón Copiar.
 */
public class DecryptPanel extends JPanel {

    private final Profile perfil;
    private final JTextArea entrada = new JTextArea(6, 40);
    private final JLabel error = new JLabel(" ");
    private final OutputBlock salida = new OutputBlock("JSON de negocio en claro");

    public DecryptPanel(Profile perfil) {
        super(new BorderLayout(8, 8));
        this.perfil = perfil;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        entrada.setLineWrap(true);
        entrada.setWrapStyleWord(true);

        JPanel norte = new JPanel(new BorderLayout(4, 4));
        norte.add(new JLabel("Pega el _mensaje en base64, o el sobre JSON completo:"), BorderLayout.NORTH);
        norte.add(new JScrollPane(entrada), BorderLayout.CENTER);

        JButton descifrar = new JButton("Descifrar");
        descifrar.addActionListener(e -> descifrar());

        error.setForeground(Color.RED);
        JPanel accion = new JPanel(new BorderLayout());
        accion.add(error, BorderLayout.CENTER);
        accion.add(descifrar, BorderLayout.EAST);

        JPanel centro = new JPanel(new BorderLayout(4, 4));
        centro.add(norte, BorderLayout.CENTER);
        centro.add(accion, BorderLayout.SOUTH);

        salida.setPreferredSize(new Dimension(100, 260));

        add(centro, BorderLayout.CENTER);
        add(salida, BorderLayout.SOUTH);
    }

    private void descifrar() {
        try {
            String texto = entrada.getText().trim();
            String base64Mensaje = texto.startsWith("{") ? Envelope.extraerMensaje(texto) : texto;
            String jsonPlano = AES256CBC.decryptWithPrependedIV(base64Mensaje, perfil.llaveAes);
            salida.setTexto(jsonPlano);
            error.setText(" ");
        } catch (Exception ex) {
            error.setText("Error: " + ex.getMessage());
        }
    }
}
