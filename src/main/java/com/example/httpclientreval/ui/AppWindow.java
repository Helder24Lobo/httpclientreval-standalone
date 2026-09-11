package com.example.httpclientreval.ui;

import com.example.httpclientreval.model.Profile;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Ventana principal: perfil y modo (Cifrar/Descifrar) se eligen con combos;
 * el botón "Nueva transacción" abre NewProfilePanel para registrar un
 * perfil nuevo sin editar profiles.json a mano.
 */
public class AppWindow extends JFrame {

    private final List<Profile> perfiles;
    private final Path archivoPerfiles;
    private final JComboBox<Profile> comboPerfil;
    private final JComboBox<String> comboModo;
    private final JPanel centro = new JPanel(new BorderLayout());
    private boolean mostrandoNuevaTransaccion = false;

    public AppWindow(List<Profile> perfiles, Path archivoPerfiles) {
        super("httpclientreval");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        this.perfiles = new ArrayList<>(perfiles);
        this.archivoPerfiles = archivoPerfiles;

        comboPerfil = new JComboBox<>(this.perfiles.toArray(new Profile[0]));
        comboModo = new JComboBox<>(new String[]{"Cifrar (ENCRYPT)", "Descifrar (DECRYPT)"});

        comboPerfil.addActionListener(e -> {
            mostrandoNuevaTransaccion = false;
            refrescar();
        });
        comboModo.addActionListener(e -> {
            mostrandoNuevaTransaccion = false;
            refrescar();
        });

        JButton nuevaTransaccion = new JButton("+ Nueva transacción");
        nuevaTransaccion.addActionListener(e -> {
            mostrandoNuevaTransaccion = true;
            refrescar();
        });

        JPanel norte = new JPanel(new FlowLayout(FlowLayout.LEFT));
        norte.add(new JLabel("Perfil:"));
        norte.add(comboPerfil);
        norte.add(new JLabel("Modo:"));
        norte.add(comboModo);
        norte.add(nuevaTransaccion);

        setLayout(new BorderLayout());
        add(norte, BorderLayout.NORTH);
        add(centro, BorderLayout.CENTER);

        refrescar();

        setPreferredSize(new Dimension(680, 720));
        pack();
        setLocationRelativeTo(null);
    }

    private void refrescar() {
        centro.removeAll();

        if (mostrandoNuevaTransaccion) {
            centro.add(new NewProfilePanel(archivoPerfiles, perfiles, this::alGuardarPerfil), BorderLayout.CENTER);
        } else if (comboModo.getSelectedIndex() == 0) {
            centro.add(new EncryptPanel((Profile) comboPerfil.getSelectedItem()), BorderLayout.CENTER);
        } else {
            centro.add(new DecryptPanel((Profile) comboPerfil.getSelectedItem()), BorderLayout.CENTER);
        }

        centro.revalidate();
        centro.repaint();
    }

    private void alGuardarPerfil(Profile nuevo) {
        mostrandoNuevaTransaccion = false;
        comboPerfil.addItem(nuevo);
        comboPerfil.setSelectedItem(nuevo);
        comboModo.setSelectedIndex(0);
        refrescar();
    }
}
