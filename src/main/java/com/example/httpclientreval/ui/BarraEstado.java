package com.example.httpclientreval.ui;

import com.example.httpclientreval.model.EnvioRegistrado;
import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import java.awt.BorderLayout;
import java.awt.Color;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Barra de estado al pie de la ventana principal: cuántas transacciones (perfiles) hay configuradas y
 * cómo terminó el último envío de la sesión, para no tener que abrir el historial a cada rato.
 * El último envío sale de {@link HistorialEnvios}, así que se actualiza solo en cuanto el envío termina.
 */
final class BarraEstado extends JPanel {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final JLabel etiquetaTotal = new JLabel();
    private final JLabel etiquetaEnvio = new JLabel();

    BarraEstado() {
        super(new BorderLayout());
        // El separador (y no un borde con color fijo) se repinta solo al cambiar de tema.
        add(new JSeparator(), BorderLayout.NORTH);

        JPanel contenido = new JPanel(new BorderLayout(16, 0));
        contenido.setOpaque(false);
        contenido.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        etiquetaTotal.putClientProperty(FlatClientProperties.STYLE, "font: -1");
        etiquetaEnvio.putClientProperty(FlatClientProperties.STYLE, "font: -1");
        contenido.add(etiquetaTotal, BorderLayout.WEST);
        contenido.add(etiquetaEnvio, BorderLayout.EAST);
        add(contenido, BorderLayout.CENTER);

        mostrarTotal(0);
        HistorialEnvios.instancia().alCambiar(this::mostrarUltimoEnvio);
        mostrarUltimoEnvio();
    }

    /** Conteo global de transacciones configuradas. */
    void mostrarTotal(int total) {
        etiquetaTotal.setText(textoTotal(total));
    }

    /** El color del estado depende del tema: al cambiarlo se recalcula contra el fondo nuevo. */
    @Override
    public void updateUI() {
        super.updateUI();
        if (etiquetaEnvio != null) {
            mostrarUltimoEnvio();
        }
    }

    private void mostrarUltimoEnvio() {
        List<EnvioRegistrado> envios = HistorialEnvios.instancia().envios();
        if (envios.isEmpty()) {
            etiquetaEnvio.setText(textoSinEnvios());
            etiquetaEnvio.setForeground(null); // vuelve al color normal del tema
            etiquetaEnvio.setToolTipText(null);
            return;
        }
        EnvioRegistrado ultimo = envios.get(0);
        etiquetaEnvio.setText(textoUltimoEnvio(ultimo.resultado(), ultimo.hora));
        // Copia como Color simple: un ColorUIResource lo reemplazaría el tema en el próximo updateUI de la etiqueta.
        etiquetaEnvio.setForeground(new Color(ColorLegible.deResultado(ultimo.resultado(), getBackground()).getRGB()));
        etiquetaEnvio.setToolTipText("<html>Perfil: " + escapar(ultimo.perfilNombre)
                + "<br>Ambiente: " + escapar(ultimo.entornoNombre)
                + "<br>Resultado: " + escapar(ultimo.resumen()) + " · " + ultimo.tiempoMs + " ms"
                + "<br>Ver el historial completo: " + Atajos.texto(Atajos.HISTORIAL) + "</html>");
    }

    static String textoTotal(int total) {
        return "Total de transacciones: " + total;
    }

    static String textoSinEnvios() {
        return "Último envío: ninguno en esta sesión";
    }

    static String textoUltimoEnvio(EnvioRegistrado.Resultado resultado, LocalDateTime hora) {
        return "Último envío: " + estado(resultado) + " - " + HORA.format(hora);
    }

    static String estado(EnvioRegistrado.Resultado resultado) {
        switch (resultado) {
            case EXITO:
                return "Exitoso";
            case ERROR_NEGOCIO:
                return "Error de negocio";
            case ERROR_HTTP:
                return "Error HTTP";
            case INCOMPLETO:
                return "Respuesta incompleta";
            default:
                return "Sin respuesta";
        }
    }

    private static String escapar(String texto) {
        if (texto == null || texto.isBlank()) {
            return "-";
        }
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
