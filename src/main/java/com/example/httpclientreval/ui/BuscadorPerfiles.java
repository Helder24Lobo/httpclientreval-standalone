package com.example.httpclientreval.ui;

import com.example.httpclientreval.model.Profile;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Diálogo de búsqueda rápida de perfiles: se escribe para filtrar por nombre
 * (sin distinguir mayúsculas ni tildes; cada palabra debe aparecer), las
 * flechas mueven la selección, Enter elige y Esc cierra.
 */
final class BuscadorPerfiles extends JDialog {

    private final List<Profile> perfiles;
    private final Consumer<Profile> alElegir;
    private final JTextField campo = new JTextField();
    private final DefaultListModel<Profile> modelo = new DefaultListModel<>();
    private final JList<Profile> lista = new JList<>(modelo);

    BuscadorPerfiles(Frame padre, List<Profile> perfiles, Consumer<Profile> alElegir) {
        super(padre, "Buscar perfil", true);
        this.perfiles = perfiles;
        this.alElegir = alElegir;

        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.setVisibleRowCount(10);
        lista.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    elegir();
                }
            }
        });

        campo.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filtrar();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filtrar();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filtrar();
            }
        });
        // El foco se queda en el campo: Enter y las flechas se manejan desde ahí.
        campo.addActionListener(e -> elegir());
        accion(campo, KeyEvent.VK_DOWN, () -> mover(1));
        accion(campo, KeyEvent.VK_UP, () -> mover(-1));
        accion(campo, KeyEvent.VK_ESCAPE, this::dispose);

        JPanel contenido = new JPanel(new BorderLayout(6, 6));
        contenido.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        contenido.add(new JLabel("Escribe para filtrar (Enter para elegir):"), BorderLayout.NORTH);
        JPanel centro = new JPanel(new BorderLayout(0, 6));
        centro.add(campo, BorderLayout.NORTH);
        centro.add(new JScrollPane(lista), BorderLayout.CENTER);
        contenido.add(centro, BorderLayout.CENTER);
        setContentPane(contenido);

        filtrar();
        setPreferredSize(new Dimension(520, 360));
        pack();
        setLocationRelativeTo(padre);
    }

    private static void accion(JComponent componente, int tecla, Runnable accion) {
        Object clave = "buscador-" + tecla;
        componente.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(tecla, 0), clave);
        componente.getActionMap().put(clave, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                accion.run();
            }
        });
    }

    private void filtrar() {
        String[] palabras = normalizar(campo.getText()).trim().split("\\s+");
        modelo.clear();
        for (Profile p : perfiles) {
            String nombre = normalizar(p.nombre);
            boolean coincide = true;
            for (String palabra : palabras) {
                if (!nombre.contains(palabra)) {
                    coincide = false;
                    break;
                }
            }
            if (coincide) {
                modelo.addElement(p);
            }
        }
        if (!modelo.isEmpty()) {
            lista.setSelectedIndex(0);
        }
    }

    private static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    private void mover(int delta) {
        if (modelo.isEmpty()) {
            return;
        }
        int nuevo = Math.max(0, Math.min(modelo.size() - 1, lista.getSelectedIndex() + delta));
        lista.setSelectedIndex(nuevo);
        lista.ensureIndexIsVisible(nuevo);
    }

    private void elegir() {
        Profile elegido = lista.getSelectedValue();
        if (elegido == null) {
            return;
        }
        dispose();
        alElegir.accept(elegido);
    }
}
