package com.example.httpclientreval.ui;

import com.example.httpclientreval.model.Profile;
import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Ventana principal: perfil y modo (Cifrar/Descifrar) se eligen con combos;
 * el botón "Nueva transacción" abre NewProfilePanel para registrar un
 * perfil nuevo sin editar profiles.json a mano; el botón "Configuración"
 * cambia el tema (claro/oscuro) en caliente y lo recuerda para la próxima
 * vez que se abra la app.
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

        JButton nuevaTransaccion = new JButton("Nueva transacción", Icons.nuevo());
        nuevaTransaccion.addActionListener(e -> {
            mostrandoNuevaTransaccion = true;
            refrescar();
        });

        JButton configuracion = new JButton("Configuración", Icons.configuracion());
        configuracion.addActionListener(e -> abrirConfiguracion());

        JPanel norte = new JPanel(new FlowLayout(FlowLayout.LEFT));
        norte.add(new JLabel("Perfil:"));
        norte.add(comboPerfil);
        norte.add(new JLabel("Modo:"));
        norte.add(comboModo);
        norte.add(nuevaTransaccion);
        norte.add(configuracion);

        setLayout(new BorderLayout());
        add(norte, BorderLayout.NORTH);
        add(centro, BorderLayout.CENTER);

        refrescar();

        // El tamaño se fija en "centro" (no en el JFrame) para que pack() calcule
        // la altura real de "norte" incluyendo el ancho que ocupan sus botones,
        // en vez de forzar un tamaño total que los solape con las pestañas.
        centro.setPreferredSize(new Dimension(900, 650));
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

    private void abrirConfiguracion() {
        JComboBox<String> comboTema = new JComboBox<>(new String[]{TemaPreferencias.OSCURO, TemaPreferencias.CLARO});
        comboTema.setSelectedItem(TemaPreferencias.obtenerTema());

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.add(new JLabel("Tema:"), BorderLayout.WEST);
        panel.add(comboTema, BorderLayout.CENTER);

        int resultado = JOptionPane.showConfirmDialog(this, panel, "Configuración",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (resultado != JOptionPane.OK_OPTION) {
            return;
        }

        String nuevoTema = (String) comboTema.getSelectedItem();
        TemaPreferencias.guardarTema(nuevoTema);

        try {
            if (TemaPreferencias.CLARO.equals(nuevoTema)) {
                UIManager.setLookAndFeel(new FlatLightLaf());
            } else {
                UIManager.setLookAndFeel(new FlatDarculaLaf());
            }
            SwingUtilities.updateComponentTreeUI(this);
            pack();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo aplicar el tema: " + ex.getMessage());
        }
    }
}
