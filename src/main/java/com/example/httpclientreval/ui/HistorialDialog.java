package com.example.httpclientreval.ui;

import com.example.httpclientreval.model.EnvioRegistrado;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
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
import java.util.List;

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
    private final OutputBlock salidaRespuesta = new OutputBlock("Respuesta HTTP");
    private final OutputBlock salidaPlano = new OutputBlock("Respuesta en claro");
    private final JButton reenviar = new JButton("Reenviar", Icons.enviar());
    private final JButton vaciar = new JButton("Vaciar historial", Icons.limpiar());

    HistorialDialog(JFrame padre) {
        super(padre, "Historial de envíos", false);
        setDefaultCloseOperation(HIDE_ON_CLOSE);

        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
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

        JTabbedPane detalle = new JTabbedPane();
        detalle.addTab("Petición", salidaPeticion);
        detalle.addTab("Respuesta HTTP", salidaRespuesta);
        detalle.addTab("Respuesta en claro", salidaPlano);

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

        EnviandoDialog espera = new EnviandoDialog(this, "Reenviando la petición al webservice...");
        SwingWorker<EnvioRegistrado, Void> worker = new SwingWorker<>() {
            @Override
            protected EnvioRegistrado doInBackground() {
                return EnvioRegistrado.enviar(original.perfil, original.soapEnviado);
            }

            @Override
            protected void done() {
                espera.dispose();
                try {
                    EnvioRegistrado nuevo = get();
                    historial.agregar(nuevo);
                    // El nuevo envío queda arriba y seleccionado, listo para comparar con el original.
                    tabla.setRowSelectionInterval(0, 0);
                    tabla.scrollRectToVisible(tabla.getCellRect(0, 0, true));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(HistorialDialog.this, "No se pudo reenviar: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
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
