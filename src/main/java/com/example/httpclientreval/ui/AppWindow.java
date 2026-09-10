package com.example.httpclientreval.ui;

import com.example.httpclientreval.model.Profile;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;

/**
 * Ventana principal: perfil y modo (Cifrar/Descifrar) se eligen con combos
 * en vez de menús de consola; el centro cambia entre EncryptPanel y
 * DecryptPanel según la selección.
 */
public class AppWindow extends JFrame {

    private final JComboBox<Profile> comboPerfil;
    private final JComboBox<String> comboModo;
    private final JPanel centro = new JPanel(new BorderLayout());

    public AppWindow(List<Profile> perfiles) {
        super("httpclientreval");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        comboPerfil = new JComboBox<>(perfiles.toArray(new Profile[0]));
        comboModo = new JComboBox<>(new String[]{"Cifrar (ENCRYPT)", "Descifrar (DECRYPT)"});

        comboPerfil.addActionListener(e -> refrescar());
        comboModo.addActionListener(e -> refrescar());

        JPanel norte = new JPanel(new FlowLayout(FlowLayout.LEFT));
        norte.add(new JLabel("Perfil:"));
        norte.add(comboPerfil);
        norte.add(new JLabel("Modo:"));
        norte.add(comboModo);

        setLayout(new BorderLayout());
        add(norte, BorderLayout.NORTH);
        add(centro, BorderLayout.CENTER);

        refrescar();

        setPreferredSize(new Dimension(680, 720));
        pack();
        setLocationRelativeTo(null);
    }

    private void refrescar() {
        Profile perfil = (Profile) comboPerfil.getSelectedItem();
        boolean cifrar = comboModo.getSelectedIndex() == 0;

        centro.removeAll();
        centro.add(cifrar ? new EncryptPanel(perfil) : new DecryptPanel(perfil), BorderLayout.CENTER);
        centro.revalidate();
        centro.repaint();
    }
}
