package com.example.httpclientreval.ui;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Editor de tabla de dos columnas (Campo / Valor) para objetos con un conjunto fijo de campos, como
 * {@code bodyMensajeDefault}/{@code headerMensajeDefault}: los nombres los define el WS (no se pueden
 * inventar), pero en vez de un formulario largo de un campo por fila se editan en una tabla compacta,
 * donde solo la columna "Valor" se puede tocar (doble clic o Enter para editar la celda).
 */
final class TablaClaveValor extends JPanel {

    private final DefaultTableModel modelo = new DefaultTableModel(new Object[]{"Campo", "Valor"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return columna == 1;
        }

        @Override
        public Class<?> getColumnClass(int columna) {
            return String.class;
        }
    };
    private final JTable tabla = new JTable(modelo);
    /** Valores con los que se creó la tabla, para poder restablecerlos con "Limpiar" sin perder el orden. */
    private final LinkedHashMap<String, String> valoresIniciales;

    TablaClaveValor(String nombreAccesible, LinkedHashMap<String, String> valores) {
        super(new BorderLayout());
        this.valoresIniciales = new LinkedHashMap<>(valores);

        for (Map.Entry<String, String> entrada : valores.entrySet()) {
            modelo.addRow(new Object[]{entrada.getKey(), entrada.getValue() == null ? "" : entrada.getValue()});
        }

        tabla.setRowHeight(tabla.getRowHeight() + 6);
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(220);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(420);
        // La columna "Campo" se ve como una etiqueta (en negrita, no editable), no como un dato más.
        DefaultTableCellRenderer rotulo = new DefaultTableCellRenderer();
        rotulo.setFont(rotulo.getFont().deriveFont(Font.BOLD));
        tabla.getColumnModel().getColumn(0).setCellRenderer(rotulo);
        Accesibilidad.nombrar(tabla, nombreAccesible,
                "Tabla de dos columnas, Campo y Valor; doble clic o Enter en una fila para editar su valor");

        add(new JScrollPane(tabla), BorderLayout.CENTER);
    }

    /** El valor actual de {@code clave}, o cadena vacía si no existe esa fila. Confirma antes cualquier edición en curso. */
    String obtener(String clave) {
        confirmarEdicion();
        for (int fila = 0; fila < modelo.getRowCount(); fila++) {
            if (clave.equals(modelo.getValueAt(fila, 0))) {
                Object valor = modelo.getValueAt(fila, 1);
                return valor == null ? "" : valor.toString();
            }
        }
        return "";
    }

    /** Vuelve todas las filas a los valores con los que se creó la tabla (mismo orden, mismas claves). */
    void restablecer() {
        confirmarEdicion();
        int fila = 0;
        for (String valor : valoresIniciales.values()) {
            modelo.setValueAt(valor == null ? "" : valor, fila++, 1);
        }
    }

    /** Si hay una celda en edición (el usuario escribió pero no confirmó con Enter/Tab), la confirma primero. */
    private void confirmarEdicion() {
        if (tabla.isEditing()) {
            tabla.getCellEditor().stopCellEditing();
        }
    }
}
