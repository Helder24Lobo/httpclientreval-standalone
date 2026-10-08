package com.example.httpclientreval.ui;

import com.example.httpclientreval.model.EnvioRegistrado;
import com.example.httpclientreval.model.SoapHttpClient;
import com.example.httpclientreval.util.Registro;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Ventana (no modal) con los envíos al WS de la sesión: lista arriba y, al
 * seleccionar uno, la petición, la respuesta y la respuesta en claro. Permite
 * reenviar la misma petición tal cual, lo que agrega un envío nuevo a la lista.
 */
final class HistorialDialog extends JDialog {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String[] COLUMNAS = {"Hora", "Transacción", "HTTP", "Duración", "Resultado"};

    private final HistorialEnvios historial = HistorialEnvios.instancia();
    private final ModeloTabla modelo = new ModeloTabla();
    private final JTable tabla = new JTable(modelo);
    private final JLabel titulo = new JLabel(" ");
    private final OutputBlock salidaPeticion = new OutputBlock("XML SOAP enviado");
    {
        salidaPeticion.habilitarCopiarCurl(() -> SoapHttpClient.curlPara(salidaPeticion.getTexto()));
    }
    private final OutputBlock salidaRespuesta = new OutputBlock("Respuesta HTTP");
    private final OutputBlock salidaPlano = new OutputBlock("Respuesta en claro");
    private final JButton reenviar = new JButton("Reenviar", Icons.enviar());
    private final JButton vaciar = new JButton("Vaciar historial", Icons.limpiar());

    private final TableRowSorter<ModeloTabla> sorter = new TableRowSorter<>(modelo);
    private final JTextField campoBusqueda = new JTextField(22);
    private final JComboBox<FiltroEnvios.Estado> comboResultado = new JComboBox<>(FiltroEnvios.Estado.values());
    private final JLabel contador = new JLabel(" ");

    private final DefaultComboBoxModel<EnvioRegistrado> modeloComparar = new DefaultComboBoxModel<>();
    private final JComboBox<EnvioRegistrado> comboComparar = new JComboBox<>(modeloComparar);
    private final DiffPanel diffPeticion = new DiffPanel();
    private final DiffPanel diffRespuestaHttp = new DiffPanel();
    private final DiffPanel diffRespuestaClaro = new DiffPanel();

    HistorialDialog(JFrame padre) {
        super(padre, "Historial de envíos", false);
        setDefaultCloseOperation(HIDE_ON_CLOSE);

        // El orden es siempre "más nuevo primero": el sorter se usa solo para filtrar, no para ordenar por columna.
        for (int c = 0; c < COLUMNAS.length; c++) {
            sorter.setSortable(c, false);
        }
        tabla.setRowSorter(sorter);

        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Accesibilidad.nombrar(tabla, "Historial de envíos", "Una fila por envío; Reenviar vuelve a mandar la petición seleccionada");
        tabla.setRowHeight(tabla.getRowHeight() + 4);
        tabla.setFillsViewportHeight(true);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(70);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(320);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(60);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(80);
        tabla.getColumnModel().getColumn(4).setPreferredWidth(160);
        tabla.getColumnModel().getColumn(4).setCellRenderer(new RenderizadorResultado());
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                mostrarSeleccionado();
            }
        });

        reenviar.setToolTipText("Vuelve a enviar exactamente la misma petición SOAP");
        reenviar.addActionListener(e -> reenviarSeleccionado());
        vaciar.addActionListener(e -> historial.vaciar());

        // En el combo se ve "hora · perfil · resultado", no toString(): el nombre del perfil ya identifica la
        // transacción y así se distinguen envíos de la misma hora sin alargar cada opción con el XML completo.
        comboComparar.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice,
                                                          boolean seleccionado, boolean conFoco) {
                Object texto = valor instanceof EnvioRegistrado ? etiqueta((EnvioRegistrado) valor) : valor;
                return super.getListCellRendererComponent(lista, texto, indice, seleccionado, conFoco);
            }
        });
        comboComparar.addActionListener(e -> actualizarDiferencias());

        JLabel etiquetaComparar = new JLabel("Comparar el envío seleccionado con:");
        Accesibilidad.etiquetar(etiquetaComparar, comboComparar, "Comparar con");

        JPanel compararCon = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        compararCon.add(etiquetaComparar);
        compararCon.add(comboComparar);

        JTabbedPane subTabsDiferencias = new JTabbedPane();
        Accesibilidad.nombrar(subTabsDiferencias, "Diferencias por sección");
        subTabsDiferencias.addTab("Petición", diffPeticion);
        subTabsDiferencias.addTab("Respuesta HTTP", diffRespuestaHttp);
        subTabsDiferencias.addTab("Respuesta en claro", diffRespuestaClaro);

        JPanel panelDiferencias = new JPanel(new BorderLayout(0, 6));
        panelDiferencias.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        panelDiferencias.add(compararCon, BorderLayout.NORTH);
        panelDiferencias.add(subTabsDiferencias, BorderLayout.CENTER);

        JTabbedPane detalle = new JTabbedPane();
        Accesibilidad.nombrar(detalle, "Detalle del envío seleccionado");
        detalle.addTab("Petición", salidaPeticion);
        detalle.addTab("Respuesta HTTP", salidaRespuesta);
        detalle.addTab("Respuesta en claro", salidaPlano);
        detalle.addTab("Diferencias", panelDiferencias);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        botones.add(reenviar);
        botones.add(vaciar);

        JPanel barraDetalle = new JPanel(new BorderLayout(8, 0));
        barraDetalle.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
        barraDetalle.add(titulo, BorderLayout.CENTER);
        barraDetalle.add(botones, BorderLayout.EAST);

        JPanel abajo = new JPanel(new BorderLayout());
        abajo.add(barraDetalle, BorderLayout.NORTH);
        abajo.add(detalle, BorderLayout.CENTER);

        campoBusqueda.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                "Buscar por transacción, hora, código, mensaje...");
        campoBusqueda.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
        campoBusqueda.setToolTipText("Ctrl+F para buscar, Esc para limpiar. Varias palabras: todas deben aparecer.");
        campoBusqueda.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                aplicarFiltro();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                aplicarFiltro();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                aplicarFiltro();
            }
        });
        campoBusqueda.registerKeyboardAction(e -> campoBusqueda.setText(""),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_FOCUSED);
        comboResultado.addActionListener(e -> aplicarFiltro());
        Atajos.registrar(getRootPane(), Atajos.FILTRAR, () -> {
            campoBusqueda.requestFocusInWindow();
            campoBusqueda.selectAll();
        });

        JLabel etiquetaBusqueda = new JLabel("Buscar:");
        Accesibilidad.etiquetar(etiquetaBusqueda, campoBusqueda, "Buscar en el historial");
        Accesibilidad.nombrar(comboResultado, "Filtrar por resultado");

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        filtros.add(etiquetaBusqueda);
        filtros.add(campoBusqueda);
        filtros.add(comboResultado);
        filtros.add(contador);

        JPanel arriba = new JPanel(new BorderLayout(0, 6));
        arriba.add(filtros, BorderLayout.NORTH);
        arriba.add(new JScrollPane(tabla), BorderLayout.CENTER);

        JSplitPane division = new JSplitPane(JSplitPane.VERTICAL_SPLIT, arriba, abajo);
        division.setResizeWeight(0.3);
        division.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        setContentPane(division);

        setPreferredSize(new Dimension(920, 680));
        pack();
        setLocationRelativeTo(padre);

        historial.alCambiar(this::recargar);
        recargar();
    }

    /** Refresca la lista conservando el envío seleccionado (si sigue visible; si no, el más nuevo que se vea). */
    private void recargar() {
        EnvioRegistrado anterior = seleccionado();
        modelo.fireTableDataChanged();
        actualizarContador();
        seleccionar(anterior);
    }

    /** Vuelve a evaluar el filtro (texto + resultado) y deja seleccionado el mismo envío si sigue visible. */
    private void aplicarFiltro() {
        EnvioRegistrado anterior = seleccionado();
        FiltroEnvios.Estado estado = (FiltroEnvios.Estado) comboResultado.getSelectedItem();
        String consulta = campoBusqueda.getText();
        sorter.setRowFilter(new RowFilter<ModeloTabla, Integer>() {
            @Override
            public boolean include(Entry<? extends ModeloTabla, ? extends Integer> entrada) {
                return FiltroEnvios.coincide(historial.envios().get(entrada.getIdentifier()), consulta, estado);
            }
        });
        actualizarContador();
        seleccionar(anterior);
    }

    private boolean hayFiltro() {
        return !campoBusqueda.getText().isBlank() || comboResultado.getSelectedItem() != FiltroEnvios.Estado.TODOS;
    }

    private void actualizarContador() {
        int total = historial.envios().size();
        contador.setText(hayFiltro() ? tabla.getRowCount() + " de " + total : total + (total == 1 ? " envío" : " envíos"));
    }

    /** Selecciona {@code preferido} si está visible; si no, la primera fila que se vea (o ninguna). */
    private void seleccionar(EnvioRegistrado preferido) {
        if (tabla.getRowCount() == 0) {
            tabla.clearSelection();
        } else {
            int fila = 0;
            int enModelo = preferido == null ? -1 : historial.envios().indexOf(preferido);
            if (enModelo >= 0) {
                int enVista = tabla.convertRowIndexToView(enModelo);
                if (enVista >= 0) {
                    fila = enVista;
                }
            }
            tabla.setRowSelectionInterval(fila, fila);
            tabla.scrollRectToVisible(tabla.getCellRect(fila, 0, true));
        }
        mostrarSeleccionado();
    }

    private EnvioRegistrado seleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            return null;
        }
        int enModelo = tabla.convertRowIndexToModel(fila);
        List<EnvioRegistrado> envios = historial.envios();
        return enModelo >= 0 && enModelo < envios.size() ? envios.get(enModelo) : null;
    }

    private void mostrarSeleccionado() {
        EnvioRegistrado envio = seleccionado();
        reenviar.setEnabled(envio != null);
        vaciar.setEnabled(!historial.envios().isEmpty());

        if (envio == null) {
            titulo.setText(historial.envios().isEmpty()
                    ? "Sin envíos en esta sesión" : "Ningún envío coincide con el filtro");
            salidaPeticion.setTexto("");
            salidaRespuesta.setTexto("");
            salidaRespuesta.ocultarBadge();
            salidaPlano.setTexto("");
            salidaPlano.ocultarBadge();
            modeloComparar.removeAllElements();
            comboComparar.setEnabled(false);
            actualizarDiferencias();
            return;
        }

        titulo.setText(HORA.format(envio.hora) + " · " + envio.perfilNombre);
        salidaPeticion.setTexto(envio.soapEnviado);

        if (envio.statusCode == null) {
            salidaRespuesta.setBadge("Sin respuesta", false);
            salidaRespuesta.setTexto("No hubo respuesta del WS: " + envio.errorEnvio);
        } else {
            salidaRespuesta.setBadge(envio.statusCode + " · " + envio.tiempoMs + "ms", envio.exitoHttp());
            salidaRespuesta.setTexto(envio.cuerpo);
        }

        if (envio.resultadoPlano != null) {
            salidaPlano.setTexto(envio.resultadoPlano);
            if (envio.codigoNegocio != null) {
                salidaPlano.setBadge("Código " + envio.codigoNegocio, envio.codigoNegocio == 0);
            } else {
                salidaPlano.ocultarBadge();
            }
        } else {
            salidaPlano.ocultarBadge();
            salidaPlano.setTexto(envio.errorDescifrado != null
                    ? "No se pudo descifrar: " + envio.errorDescifrado
                    : "(Sin respuesta en claro para este envío)");
        }

        actualizarComboComparar(envio);
    }

    /**
     * Repuebla el combo "Comparar con" con todos los envíos salvo el seleccionado, y de una vez elige
     * el más útil: el envío anterior de la MISMA transacción si hay uno (p. ej., tras un Reenviar el
     * combo ya queda en el envío original), o si no el inmediatamente anterior en el tiempo.
     */
    private void actualizarComboComparar(EnvioRegistrado seleccionado) {
        List<EnvioRegistrado> envios = historial.envios();
        int indice = envios.indexOf(seleccionado);

        EnvioRegistrado porDefecto = null;
        List<EnvioRegistrado> otros = new ArrayList<>();
        for (int i = 0; i < envios.size(); i++) {
            if (i == indice) {
                continue;
            }
            EnvioRegistrado candidato = envios.get(i);
            otros.add(candidato);
            if (porDefecto == null && i > indice && java.util.Objects.equals(candidato.perfilNombre, seleccionado.perfilNombre)) {
                porDefecto = candidato;
            }
        }
        if (porDefecto == null && !otros.isEmpty()) {
            // Sin otro envío de la misma transacción: el más cercano en el tiempo es mejor que nada.
            porDefecto = indice + 1 < envios.size() ? envios.get(indice + 1) : otros.get(0);
        }

        modeloComparar.removeAllElements();
        for (EnvioRegistrado o : otros) {
            modeloComparar.addElement(o);
        }
        comboComparar.setEnabled(!otros.isEmpty());
        modeloComparar.setSelectedItem(porDefecto);
        actualizarDiferencias();
    }

    private void actualizarDiferencias() {
        EnvioRegistrado actual = seleccionado();
        EnvioRegistrado contraparte = (EnvioRegistrado) modeloComparar.getSelectedItem();

        if (actual == null || contraparte == null) {
            String mensaje = actual == null ? "Selecciona un envío en la lista." : "No hay otro envío con el que comparar.";
            diffPeticion.mostrarVacio(mensaje);
            diffRespuestaHttp.mostrarVacio(mensaje);
            diffRespuestaClaro.mostrarVacio(mensaje);
            return;
        }

        // "A" es siempre el más antiguo de los dos y "B" el más nuevo, sin importar cuál esté "seleccionado"
        // en la tabla: así el diff siempre se lee como "de A a B" en vez de saltar de signo según el orden de clic.
        EnvioRegistrado a = contraparte.hora.isAfter(actual.hora) ? actual : contraparte;
        EnvioRegistrado b = contraparte.hora.isAfter(actual.hora) ? contraparte : actual;

        diffPeticion.mostrar(formatoDiff(a.soapEnviado), formatoDiff(b.soapEnviado));
        diffRespuestaHttp.mostrar(formatoDiff(textoRespuesta(a)), formatoDiff(textoRespuesta(b)));
        diffRespuestaClaro.mostrar(formatoDiff(textoClaro(a)), formatoDiff(textoClaro(b)));
    }

    private static String formatoDiff(String texto) {
        return FormatoSalida.formatear(texto).texto;
    }

    private static String textoRespuesta(EnvioRegistrado e) {
        return e.statusCode == null ? "(Sin respuesta: " + e.errorEnvio + ")" : e.cuerpo;
    }

    private static String textoClaro(EnvioRegistrado e) {
        if (e.resultadoPlano != null) {
            return e.resultadoPlano;
        }
        return e.errorDescifrado != null ? "(No se pudo descifrar: " + e.errorDescifrado + ")" : "(Sin respuesta en claro)";
    }

    private String etiqueta(EnvioRegistrado e) {
        return HORA.format(e.hora) + " · " + e.perfilNombre + " · " + e.resumen();
    }

    private void reenviarSeleccionado() {
        EnvioRegistrado original = seleccionado();
        if (original == null) {
            return;
        }
        // En Producción el mismo diálogo de siempre lleva además el aviso y la URL destino (un solo diálogo).
        String avisoProduccion = ConfirmarEnvioProduccion.esProduccion()
                ? "⚠ AMBIENTE ACTIVO: PRODUCCIÓN\nDestino: " + SoapHttpClient.getUrl() + "\n\n" : "";
        int confirmacion = JOptionPane.showConfirmDialog(this,
                avisoProduccion
                        + "Se enviará otra vez la misma petición de las " + HORA.format(original.hora) + "\n"
                        + original.perfilNombre + "\n\n"
                        + "Si la transacción ya se procesó, el servicio podría duplicarla o rechazarla.\n"
                        + "¿Reenviar?",
                "Reenviar petición", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        AtomicReference<SwingWorker<EnvioRegistrado, Void>> workerActual = new AtomicReference<>();
        EnviandoDialog espera = new EnviandoDialog(this, "Reenviando la petición al webservice...",
                () -> { if (workerActual.get() != null) workerActual.get().cancel(true); });
        SwingWorker<EnvioRegistrado, Void> worker = new SwingWorker<>() {
            @Override
            protected EnvioRegistrado doInBackground() {
                return EnvioRegistrado.enviar(original.perfil, original.soapEnviado);
            }

            @Override
            protected void done() {
                espera.dispose();
                if (isCancelled()) {
                    return;
                }
                try {
                    EnvioRegistrado nuevo = get();
                    historial.agregar(nuevo);
                    // El nuevo envío queda arriba y seleccionado; la pestaña "Diferencias" ya lo compara
                    // contra el original (mismo perfilNombre), sin que haya que elegirlo a mano.
                    // Si el filtro lo esconde se quita, para que el resultado del reenvío no pase desapercibido.
                    if (tabla.convertRowIndexToView(0) < 0) {
                        campoBusqueda.setText("");
                        comboResultado.setSelectedItem(FiltroEnvios.Estado.TODOS);
                    }
                    seleccionar(nuevo);
                } catch (Exception ex) {
                    Registro.error("No se pudo reenviar la petición de " + original.perfilNombre, ex);
                    Throwable causa = ex.getCause() != null ? ex.getCause() : ex;
                    String detalle = causa.getMessage() != null && !causa.getMessage().isBlank()
                            ? causa.getMessage() : causa.getClass().getSimpleName();
                    JOptionPane.showMessageDialog(HistorialDialog.this, "No se pudo reenviar: " + detalle,
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        workerActual.set(worker);
        worker.execute();
        espera.setVisible(true); // modal: bloquea hasta que done() llame espera.dispose()
    }

    private final class ModeloTabla extends AbstractTableModel {
        @Override
        public int getRowCount() {
            return historial.envios().size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNAS.length;
        }

        @Override
        public String getColumnName(int columna) {
            return COLUMNAS[columna];
        }

        @Override
        public Object getValueAt(int fila, int columna) {
            EnvioRegistrado e = historial.envios().get(fila);
            switch (columna) {
                case 0:
                    return HORA.format(e.hora);
                case 1:
                    return e.perfilNombre;
                case 2:
                    return e.statusCode == null ? "—" : String.valueOf(e.statusCode);
                case 3:
                    return e.tiempoMs + " ms";
                default:
                    return e;
            }
        }
    }

    /**
     * Pinta el resultado con un color según cómo terminó, sin perder el contraste al seleccionar la fila.
     * Los colores salen del tema activo (se leen al pintar, así que siguen el cambio claro/oscuro en
     * caliente) y se ajustan al fondo real de la celda, incluidas las filas alternas.
     */
    private final class RenderizadorResultado extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object valor, boolean seleccionada,
                                                       boolean conFoco, int fila, int columna) {
            EnvioRegistrado envio = (EnvioRegistrado) valor;
            super.getTableCellRendererComponent(t, envio.resumen(), seleccionada, conFoco, fila, columna);
            if (!seleccionada) {
                setForeground(color(envio.resultado(), getBackground()));
            }
            return this;
        }

        private Color color(EnvioRegistrado.Resultado resultado, Color fondo) {
            switch (resultado) {
                case EXITO:
                    return ColorLegible.delTema("Actions.Green", new Color(56, 158, 66), fondo);
                case ERROR_NEGOCIO:
                    return ColorLegible.delTema("Actions.Yellow", new Color(230, 120, 0), fondo);
                default:
                    return ColorLegible.delTema("Actions.Red", new Color(220, 60, 60), fondo);
            }
        }
    }
}
