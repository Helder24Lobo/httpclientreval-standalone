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
import java.awt.Dimension;

/**
 * Panel de DECRYPT: pega el _mensaje (o el sobre JSON completo), botón
 * Descifrar, salida con botón Copiar.
 */
public class DecryptPanel extends JPanel {

    private final Profile perfil;
    private final JTextArea entrada = new JTextArea(6, 40);
    private final StatusBanner statusBanner = new StatusBanner();
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

        JButton descifrar = new JButton("Descifrar", Icons.descifrar());
        descifrar.addActionListener(e -> descifrar());

        JButton limpiar = new JButton("Limpiar", Icons.limpiar());
        limpiar.addActionListener(e -> limpiar());

        JPanel botones = new JPanel();
        botones.add(descifrar);
        botones.add(limpiar);

        JPanel accion = new JPanel(new BorderLayout(8, 0));
        accion.add(statusBanner, BorderLayout.CENTER);
        accion.add(botones, BorderLayout.EAST);

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
            if (texto.isEmpty()) {
                statusBanner.mostrarError("Ingresa o pega un mensaje cifrado o sobre JSON.");
                return;
            }
            String base64Mensaje = texto.startsWith("{") ? Envelope.extraerMensaje(texto) : texto;
            String jsonPlano = AES256CBC.decryptWithPrependedIV(base64Mensaje, perfil.llaveAes);
            salida.setTexto(jsonPlano);
            statusBanner.mostrarExito("Mensaje descifrado correctamente.");
        } catch (Exception ex) {
            statusBanner.mostrarError("Error al descifrar: " + ex.getMessage());
        }
    }

    private void limpiar() {
        entrada.setText("");
        salida.setTexto("");
        statusBanner.ocultar();
    }
}
