package com.example.httpclientreval.ui;

import com.example.httpclientreval.model.Profile;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Diálogo de búsqueda rápida de perfiles: se escribe para filtrar por nombre
 * (sin distinguir mayúsculas ni tildes; cada palabra debe aparecer), las
 * flechas mueven la selección, Enter elige y Esc cierra. Los favoritos (★)
 * salen primero, luego los recientes (◷); Ctrl+D marca o desmarca el favorito
 * y "Solo favoritos" limita la lista a ellos.
 */
final class BuscadorPerfiles extends JDialog {

    private final List<Profile> perfiles;
    private final PerfilesPreferencias preferencias;
    private final Consumer<Profile> alElegir;
    private final JTextField campo = new JTextField();
    private final JCheckBox soloFavoritos = new JCheckBox("Solo favoritos");
    private final DefaultListModel<Profile> modelo = new DefaultListModel<>();
    private final JList<Profile> lista = new JList<>(modelo);

    BuscadorPerfiles(Frame padre, List<Profile> perfiles, PerfilesPreferencias preferencias,
                     Consumer<Profile> alElegir) {
        super(padre, "Buscar perfil", true);
        this.perfiles = perfiles;
        this.preferencias = preferencias;
        this.alElegir = alElegir;

        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.setVisibleRowCount(10);
        lista.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object valor, int indice,
                                                          boolean seleccionado, boolean conFoco) {
                String nombre = String.valueOf(valor);
                String marca = "    ";
                if (valor instanceof Profile) {
                    if (preferencias.esFavorito(nombre)) {
                        marca = "★  ";
                    } else if (preferencias.recientes().contains(nombre)) {
                        marca = "◷  ";
                    }
                }
                return super.getListCellRendererComponent(l, marca + nombre, indice, seleccionado, conFoco);
            }
        });
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
        soloFavoritos.addActionListener(e -> {
            filtrar();
            campo.requestFocusInWindow();
        });
        // El foco se queda en el campo: Enter y las flechas se manejan desde ahí.
        campo.addActionListener(e -> elegir());
        accion(campo, KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), () -> mover(1));
        accion(campo, KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), () -> mover(-1));
        accion(campo, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), this::dispose);
        accion(campo, Atajos.FAVORITO, this::alternarFavorito);
        campo.setToolTipText(Atajos.texto(Atajos.FAVORITO) + " marca o desmarca el favorito");

        JPanel arriba = new JPanel(new BorderLayout(6, 0));
        arriba.add(campo, BorderLayout.CENTER);
        arriba.add(soloFavoritos, BorderLayout.EAST);

        JPanel contenido = new JPanel(new BorderLayout(6, 6));
        contenido.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        contenido.add(new JLabel("Escribe para filtrar (Enter elige · "
                + Atajos.texto(Atajos.FAVORITO) + " favorito):"), BorderLayout.NORTH);
        JPanel centro = new JPanel(new BorderLayout(0, 6));
        centro.add(arriba, BorderLayout.NORTH);
        centro.add(new JScrollPane(lista), BorderLayout.CENTER);
        contenido.add(centro, BorderLayout.CENTER);
        setContentPane(contenido);

        filtrar();
        setPreferredSize(new Dimension(560, 380));
        pack();
        setLocationRelativeTo(padre);
    }

    private static void accion(JComponent componente, KeyStroke atajo, Runnable accion) {
        Object clave = "buscador-" + atajo;
        componente.getInputMap(JComponent.WHEN_FOCUSED).put(atajo, clave);
        componente.getActionMap().put(clave, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                accion.run();
            }
        });
    }

    /** Recalcula la lista conservando, si sigue apareciendo, el perfil seleccionado. */
    private void filtrar() {
        Profile anterior = lista.getSelectedValue();
        String[] palabras = normalizar(campo.getText()).trim().split("\\s+");
        List<String> favoritos = preferencias.favoritos();
        List<String> recientes = preferencias.recientes();

        List<Profile> coincidencias = new ArrayList<>();
        for (Profile p : perfiles) {
            if (soloFavoritos.isSelected() && !favoritos.contains(p.nombre)) {
                continue;
            }
            String nombre = normalizar(p.nombre);
            boolean coincide = true;
            for (String palabra : palabras) {
                if (!nombre.contains(palabra)) {
                    coincide = false;
                    break;
                }
            }
            if (coincide) {
                coincidencias.add(p);
            }
        }
        // Favoritos primero, luego recientes (el más nuevo arriba); el resto conserva el orden de profiles.json.
        coincidencias.sort((a, b) -> Integer.compare(rango(a, favoritos, recientes), rango(b, favoritos, recientes)));

        modelo.clear();
        modelo.addAll(coincidencias);
        if (!modelo.isEmpty()) {
            lista.setSelectedIndex(anterior != null && modelo.contains(anterior) ? modelo.indexOf(anterior) : 0);
            lista.ensureIndexIsVisible(lista.getSelectedIndex());
        }
    }

    private static int rango(Profile p, List<String> favoritos, List<String> recientes) {
        if (favoritos.contains(p.nombre)) {
            return 0;
        }
        int posicion = recientes.indexOf(p.nombre);
        return posicion >= 0 ? 1 + posicion : 1 + PerfilesPreferencias.MAX_RECIENTES;
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

    private void alternarFavorito() {
        Profile seleccionado = lista.getSelectedValue();
        if (seleccionado == null) {
            return;
        }
        preferencias.alternarFavorito(seleccionado.nombre);
        filtrar();
        lista.repaint();
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
