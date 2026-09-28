package com.example.httpclientreval.ui;

import com.formdev.flatlaf.FlatLaf;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.FlowLayout;
import java.io.IOException;
import java.io.InputStream;

/**
 * Vista de diferencias línea por línea entre dos textos (estilo {@code git diff}): cada línea lleva
 * un prefijo (" ", "+", "-") y un fondo de color según si es igual, se agregó o se quitó. Solo lectura.
 */
final class DiffPanel extends JPanel {

    private static final String TEMA_OSCURO = "/org/fife/ui/rsyntaxtextarea/themes/dark.xml";
    private static final String TEMA_CLARO = "/org/fife/ui/rsyntaxtextarea/themes/idea.xml";

    private static final Color FONDO_AGREGADA_CLARO = new Color(214, 255, 214);
    private static final Color FONDO_ELIMINADA_CLARO = new Color(255, 219, 219);
    private static final Color FONDO_AGREGADA_OSCURO = new Color(29, 66, 35);
    private static final Color FONDO_ELIMINADA_OSCURO = new Color(77, 32, 32);

    private final RSyntaxTextArea area = new RSyntaxTextArea();
    private final JLabel resumen = new JLabel(" ");
    private DiffTexto.Resultado ultimoResultado;

    DiffPanel() {
        super(new BorderLayout(0, 4));

        area.setEditable(false);
        area.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        area.setHighlightCurrentLine(false);
        area.setCodeFoldingEnabled(false);
        aplicarTema();
        Accesibilidad.nombrar(area, "Diferencias", "Texto de solo lectura; líneas con + son nuevas, con - se quitaron");
        Accesibilidad.tabulacionLibre(area);

        resumen.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "foreground: $Label.disabledForeground");

        JPanel norte = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        norte.add(resumen);

        RTextScrollPane scroll = new RTextScrollPane(area);
        scroll.setFoldIndicatorEnabled(false);
        scroll.setLineNumbersEnabled(false);

        add(norte, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    /** Sin nada que comparar todavía (no hay envío A o B elegido). */
    void mostrarVacio(String mensaje) {
        ultimoResultado = null;
        area.removeAllLineHighlights();
        area.setText("");
        resumen.setText(mensaje);
    }

    /** Recalcula y repinta el diff entre {@code a} (texto anterior) y {@code b} (texto nuevo). */
    void mostrar(String a, String b) {
        DiffTexto.Resultado resultado = DiffTexto.calcular(a, b);

        if (resultado.demasiadoGrande) {
            resumen.setText("Los textos son demasiado grandes para comparar línea a línea.");
            area.removeAllLineHighlights();
            area.setText("");
            return;
        }

        if (!resultado.hayDiferencias()) {
            resumen.setText(resultado.iguales == 0 ? "Ambos están vacíos." : "Sin diferencias (" + resultado.iguales + " líneas).");
        } else {
            resumen.setText(resultado.eliminadas + " eliminada(s) · " + resultado.agregadas + " agregada(s) · "
                    + resultado.iguales + " igual(es)");
        }

        StringBuilder texto = new StringBuilder();
        for (int i = 0; i < resultado.lineas.size(); i++) {
            DiffTexto.Linea linea = resultado.lineas.get(i);
            String prefijo = switch (linea.tipo) {
                case AGREGADA -> "+ ";
                case ELIMINADA -> "- ";
                default -> "  ";
            };
            texto.append(prefijo).append(linea.texto);
            if (i < resultado.lineas.size() - 1) {
                texto.append('\n');
            }
        }
        area.setText(texto.toString());
        area.setCaretPosition(0);
        ultimoResultado = resultado;
        pintarFondos();
    }

    private void pintarFondos() {
        if (ultimoResultado == null) {
            return;
        }
        area.removeAllLineHighlights();
        boolean oscuro = FlatLaf.isLafDark();
        Color fondoAgregada = oscuro ? FONDO_AGREGADA_OSCURO : FONDO_AGREGADA_CLARO;
        Color fondoEliminada = oscuro ? FONDO_ELIMINADA_OSCURO : FONDO_ELIMINADA_CLARO;
        for (int i = 0; i < ultimoResultado.lineas.size(); i++) {
            DiffTexto.Tipo tipo = ultimoResultado.lineas.get(i).tipo;
            if (tipo == DiffTexto.Tipo.IGUAL) {
                continue;
            }
            try {
                area.addLineHighlight(i, tipo == DiffTexto.Tipo.AGREGADA ? fondoAgregada : fondoEliminada);
            } catch (BadLocationException ignorada) {
                // No debería pasar: i siempre es una línea real del texto que se acaba de poner.
            }
        }
    }

    /** Al cambiar de tema (claro/oscuro) hay que recargar los colores del editor y recalcular los del diff. */
    @Override
    public void updateUI() {
        super.updateUI();
        if (area != null) {
            aplicarTema();
            pintarFondos();
        }
    }

    private void aplicarTema() {
        String recurso = FlatLaf.isLafDark() ? TEMA_OSCURO : TEMA_CLARO;
        try (InputStream in = DiffPanel.class.getResourceAsStream(recurso)) {
            Theme.load(in).apply(area);
        } catch (IOException | RuntimeException ex) {
            // Sin tema se usan los colores por defecto del editor; no es motivo para fallar.
        }
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, UIManager.getFont("Label.font").getSize()));
    }
}
