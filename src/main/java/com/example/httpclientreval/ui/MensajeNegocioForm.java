package com.example.httpclientreval.ui;

import com.example.httpclientreval.model.MensajeNegocio;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Formulario Swing modal para llenar el mensaje de negocio y el header del
 * sobre a mano, campo por campo, en vez de escribirlos uno por uno en
 * consola. Se abre y bloquea (SwingUtilities.invokeAndWait + JDialog modal)
 * hasta que el usuario presiona "Generar" o "Cancelar" / cierra la ventana.
 */
public class MensajeNegocioForm {

    public static class Resultado {
        public MensajeNegocio.BodyMensaje body;
        public MensajeNegocio.HeaderMensaje header;
        public int idCliente;
        public int idTransaccion;
        public String ipCliente;
    }

    private MensajeNegocioForm() {
    }

    /**
     * Muestra el formulario precargado con los defaults dados.
     * Devuelve null si el usuario canceló o cerró la ventana.
     */
    public static Resultado mostrar(MensajeNegocio.BodyMensaje bodyDefaults,
                                     MensajeNegocio.HeaderMensaje headerDefaults,
                                     int idClienteDefault,
                                     int idTransaccionDefault,
                                     String ipClienteDefault) throws Exception {
        Resultado[] resultadoHolder = new Resultado[1];

        SwingUtilities.invokeAndWait(() -> {
            JDialog dialog = new JDialog((Frame) null, "Mensaje de negocio (ENCRYPT)", true);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

            Map<String, JTextField> campos = new LinkedHashMap<>();
            JPanel panelCampos = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(3, 6, 3, 6);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            int[] fila = {0};

            agregarSeccion(panelCampos, gbc, fila, "Datos del sobre (_header)");
            agregarCampo(panelCampos, gbc, fila, campos, "IdCliente", String.valueOf(idClienteDefault));
            agregarCampo(panelCampos, gbc, fila, campos, "IdTransaccion (sobre)", String.valueOf(idTransaccionDefault));
            agregarCampo(panelCampos, gbc, fila, campos, "IpCliente", ipClienteDefault);

            agregarSeccion(panelCampos, gbc, fila, "Mensaje de negocio (_body._BodyMensaje)");
            agregarCampo(panelCampos, gbc, fila, campos, "Autorizacion", bodyDefaults.autorizacion);
            agregarCampo(panelCampos, gbc, fila, campos, "CodBarras", bodyDefaults.codBarras);
            agregarCampo(panelCampos, gbc, fila, campos, "Convenio", bodyDefaults.convenio);
            agregarCampo(panelCampos, gbc, fila, campos, "FechaVencimiento", bodyDefaults.fechaVencimiento);
            agregarCampo(panelCampos, gbc, fila, campos, "Iac", bodyDefaults.iac);
            agregarCampo(panelCampos, gbc, fila, campos, "IdPersona", bodyDefaults.idPersona);
            agregarCampo(panelCampos, gbc, fila, campos, "IdTransaccion (negocio)", bodyDefaults.idTransaccion);
            agregarCampo(panelCampos, gbc, fila, campos, "NoIdentificacionUsuario", bodyDefaults.noIdentificacionUsuario);
            agregarCampo(panelCampos, gbc, fila, campos, "NombreUsuario", bodyDefaults.nombreUsuario);
            agregarCampo(panelCampos, gbc, fila, campos, "NumCelular", bodyDefaults.numCelular);
            agregarCampo(panelCampos, gbc, fila, campos, "Observacion", bodyDefaults.observacion);
            agregarCampo(panelCampos, gbc, fila, campos, "Otp", bodyDefaults.otp);
            agregarCampo(panelCampos, gbc, fila, campos, "Referencia1", bodyDefaults.referencia1);
            agregarCampo(panelCampos, gbc, fila, campos, "Referencia2", bodyDefaults.referencia2);
            agregarCampo(panelCampos, gbc, fila, campos, "Referencia3", bodyDefaults.referencia3);
            agregarCampo(panelCampos, gbc, fila, campos, "Referencia4", bodyDefaults.referencia4);
            agregarCampo(panelCampos, gbc, fila, campos, "Referencia5", bodyDefaults.referencia5);
            agregarCampo(panelCampos, gbc, fila, campos, "Referencia6", bodyDefaults.referencia6);
            agregarCampo(panelCampos, gbc, fila, campos, "Referencia7", bodyDefaults.referencia7);
            agregarCampo(panelCampos, gbc, fila, campos, "Referencia8", bodyDefaults.referencia8);
            agregarCampo(panelCampos, gbc, fila, campos, "Referencia9", bodyDefaults.referencia9);
            agregarCampo(panelCampos, gbc, fila, campos, "Referencia10", bodyDefaults.referencia10);
            agregarCampo(panelCampos, gbc, fila, campos, "Referencia11", bodyDefaults.referencia11);
            agregarCampo(panelCampos, gbc, fila, campos, "Referencia12", bodyDefaults.referencia12);
            agregarCampo(panelCampos, gbc, fila, campos, "Referencia13", bodyDefaults.referencia13);
            agregarCampo(panelCampos, gbc, fila, campos, "Referencia14", bodyDefaults.referencia14);
            agregarCampo(panelCampos, gbc, fila, campos, "Referencia15", bodyDefaults.referencia15);
            agregarCampo(panelCampos, gbc, fila, campos, "TipoIdentificacion", bodyDefaults.tipoIdentificacion);
            agregarCampo(panelCampos, gbc, fila, campos, "Valor", bodyDefaults.valor);

            agregarSeccion(panelCampos, gbc, fila, "Header del mensaje (_body._headerMensaje)");
            agregarCampo(panelCampos, gbc, fila, campos, "NoIdentificacionCajero", headerDefaults.noIdentificacionCajero);

            JScrollPane scroll = new JScrollPane(panelCampos);
            scroll.getVerticalScrollBar().setUnitIncrement(16);
            scroll.setPreferredSize(new Dimension(460, 520));

            JLabel error = new JLabel(" ");
            error.setForeground(Color.RED);

            JButton generar = new JButton("Generar");
            JButton cancelar = new JButton("Cancelar");
            cancelar.addActionListener(e -> dialog.dispose());

            generar.addActionListener(e -> {
                try {
                    Resultado resultado = new Resultado();
                    resultado.idCliente = Integer.parseInt(campos.get("IdCliente").getText().trim());
                    resultado.idTransaccion = Integer.parseInt(campos.get("IdTransaccion (sobre)").getText().trim());
                    resultado.ipCliente = campos.get("IpCliente").getText().trim();

                    MensajeNegocio.BodyMensaje body = new MensajeNegocio.BodyMensaje();
                    body.autorizacion = campos.get("Autorizacion").getText();
                    body.codBarras = campos.get("CodBarras").getText();
                    body.convenio = campos.get("Convenio").getText();
                    body.fechaVencimiento = campos.get("FechaVencimiento").getText();
                    body.iac = campos.get("Iac").getText();
                    body.idPersona = campos.get("IdPersona").getText();
                    body.idTransaccion = campos.get("IdTransaccion (negocio)").getText();
                    body.noIdentificacionUsuario = campos.get("NoIdentificacionUsuario").getText();
                    body.nombreUsuario = campos.get("NombreUsuario").getText();
                    body.numCelular = campos.get("NumCelular").getText();
                    body.observacion = campos.get("Observacion").getText();
                    body.otp = campos.get("Otp").getText();
                    body.referencia1 = campos.get("Referencia1").getText();
                    body.referencia2 = campos.get("Referencia2").getText();
                    body.referencia3 = campos.get("Referencia3").getText();
                    body.referencia4 = campos.get("Referencia4").getText();
                    body.referencia5 = campos.get("Referencia5").getText();
                    body.referencia6 = campos.get("Referencia6").getText();
                    body.referencia7 = campos.get("Referencia7").getText();
                    body.referencia8 = campos.get("Referencia8").getText();
                    body.referencia9 = campos.get("Referencia9").getText();
                    body.referencia10 = campos.get("Referencia10").getText();
                    body.referencia11 = campos.get("Referencia11").getText();
                    body.referencia12 = campos.get("Referencia12").getText();
                    body.referencia13 = campos.get("Referencia13").getText();
                    body.referencia14 = campos.get("Referencia14").getText();
                    body.referencia15 = campos.get("Referencia15").getText();
                    body.tipoIdentificacion = campos.get("TipoIdentificacion").getText();
                    body.valor = campos.get("Valor").getText();
                    resultado.body = body;

                    MensajeNegocio.HeaderMensaje header = new MensajeNegocio.HeaderMensaje();
                    header.noIdentificacionCajero = campos.get("NoIdentificacionCajero").getText();
                    resultado.header = header;

                    resultadoHolder[0] = resultado;
                    dialog.dispose();
                } catch (NumberFormatException ex) {
                    error.setText("IdCliente e IdTransaccion (sobre) deben ser números enteros.");
                }
            });

            JPanel botones = new JPanel();
            botones.add(generar);
            botones.add(cancelar);

            JPanel sur = new JPanel(new BorderLayout());
            sur.add(error, BorderLayout.NORTH);
            sur.add(botones, BorderLayout.SOUTH);

            dialog.setLayout(new BorderLayout());
            dialog.add(scroll, BorderLayout.CENTER);
            dialog.add(sur, BorderLayout.SOUTH);
            dialog.pack();
            dialog.setLocationRelativeTo(null);

            // Fuerza la ventana al frente: si Main se corre desde una terminal/IDE
            // en foco, Windows a veces abre la ventana nueva detrás de esa terminal
            // en vez de robarle el foco.
            dialog.setAlwaysOnTop(true);
            dialog.toFront();
            dialog.requestFocus();

            dialog.setVisible(true); // bloquea aquí (modal) hasta dispose()
        });

        return resultadoHolder[0];
    }

    private static void agregarSeccion(JPanel panel, GridBagConstraints gbc, int[] fila, String titulo) {
        gbc.gridx = 0;
        gbc.gridy = fila[0]++;
        gbc.gridwidth = 2;
        JLabel etiqueta = new JLabel(titulo);
        etiqueta.setFont(etiqueta.getFont().deriveFont(Font.BOLD));
        panel.add(etiqueta, gbc);
        gbc.gridwidth = 1;
    }

    private static void agregarCampo(JPanel panel, GridBagConstraints gbc, int[] fila,
                                      Map<String, JTextField> campos, String etiqueta, String valorPorDefecto) {
        gbc.gridx = 0;
        gbc.gridy = fila[0]++;
        gbc.weightx = 0;
        panel.add(new JLabel(etiqueta + ":"), gbc);

        JTextField campo = new JTextField(valorPorDefecto == null ? "" : valorPorDefecto, 22);
        campos.put(etiqueta, campo);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(campo, gbc);
    }
}
