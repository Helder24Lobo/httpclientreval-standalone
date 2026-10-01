package com.example.httpclientreval.ui;

import com.example.httpclientreval.model.EnvioRegistrado;
import com.example.httpclientreval.model.SoapHttpClient;
import com.example.httpclientreval.util.Registro;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
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
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
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

    private final DefaultComboBoxModel<EnvioRegistrado> modeloComparar = new DefaultComboBoxModel<>();
    private final JComboBox<EnvioRegistrado> comboComparar = new JComboBox<>(modeloComparar);
    private final DiffPanel diffPeticion = new DiffPanel();
    private final DiffPanel diffRespuestaHttp = new DiffPanel();
    private final DiffPanel diffRespuestaClaro = new DiffPanel();

    HistorialDialog(JFrame padre) {
        super(padre, "Historial de envíos", false);
        setDefaultCloseOperation(HIDE_ON_CLOSE);

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

        JSplitPane division = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(tabla), abajo);
        division.setResizeWeight(0.3);
        division.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        setContentPane(division);

        setPreferredSize(new Dimension(920, 680));
        pack();
        setLocationRelativeTo(padre);

        historial.alCambiar(this::recargar);
        recargar();
    }

    /** Refresca la lista conservando el envío seleccionado (si sigue ahí; si no, el más nuevo). */
    private void recargar() {
        EnvioRegistrado anterior = seleccionado();
        modelo.fireTableDataChanged();
        List<EnvioRegistrado> envios = historial.envios();
        if (!envios.isEmpty()) {
            int fila = anterior != null && envios.contains(anterior) ? envios.indexOf(anterior) : 0;
            tabla.setRowSelectionInterval(fila, fila);
        }
        mostrarSeleccionado();
    }

    private EnvioRegistrado seleccionado() {
        int fila = tabla.getSelectedRow();
        List<EnvioRegistrado> envios = historial.envios();
        return fila >= 0 && fila < envios.size() ? envios.get(fila) : null;
    }

    private void mostrarSeleccionado() {
        EnvioRegistrado envio = seleccionado();
        reenviar.setEnabled(envio != null);
        vaciar.setEnabled(!historial.envios().isEmpty());

        if (envio == null) {
            titulo.setText("Sin envíos en esta sesión");
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
            if (porDefecto == null && i > indice && candidato.perfilNombre.equals(seleccionado.perfilNombre)) {
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
        int confirmacion = JOptionPane.showConfirmDialog(this,
                "Se enviará otra vez la misma petición de las " + HORA.format(original.hora) + "\n"
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
                    tabla.setRowSelectionInterval(0, 0);
                    tabla.scrollRectToVisible(tabla.getCellRect(0, 0, true));
                } catch (Exception ex) {
                    Registro.error("No se pudo reenviar la petición de " + original.perfilNombre, ex);
                    JOptionPane.showMessageDialog(HistorialDialog.this, "No se pudo reenviar: " + ex.getMessage(),
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

    /** Pinta el resultado con un color según cómo terminó, sin perder el contraste al seleccionar la fila. */
    private final class RenderizadorResultado extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object valor, boolean seleccionada,
                                                       boolean conFoco, int fila, int columna) {
            EnvioRegistrado envio = (EnvioRegistrado) valor;
            super.getTableCellRendererComponent(t, envio.resumen(), seleccionada, conFoco, fila, columna);
            if (!seleccionada) {
                setForeground(color(envio.resultado()));
            }
            return this;
        }

        private Color color(EnvioRegistrado.Resultado resultado) {
            switch (resultado) {
                case EXITO:
                    return new Color(56, 158, 66);
                case ERROR_NEGOCIO:
                    return new Color(230, 120, 0);
                default:
                    return new Color(220, 60, 60);
            }
        }
    }
}
