package com.example.httpclientreval.model;

import com.example.httpclientreval.crypto.AES256CBC;
import com.example.httpclientreval.util.ArchivoAtomico;
import com.example.httpclientreval.util.Registro;
import com.example.httpclientreval.util.RutasApp;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * La lista de ambientes configurados y cuál está activo, guardada en entornos.json (junto a
 * profiles.json, en la carpeta de datos del usuario). Es la fuente de la URL, el SOAPAction, el
 * timeout y las credenciales con las que {@link SoapHttpClient} y el cifrado trabajan.
 *
 * La primera vez crea dos ambientes base: <b>Pruebas</b> (la URL de siempre, con las credenciales que
 * ya tuvieran los perfiles) y <b>Producción</b> (vacío, pendiente de configurar). Se pueden crear los
 * que haga falta. Reglas: nombres únicos; no se puede borrar el activo ni el último; solo se puede
 * activar un ambiente completo ({@link Entorno#listoParaUsar()}).
 */
public final class Entornos {

    public static final String NOMBRE_PRUEBAS = "Pruebas";
    public static final String NOMBRE_PRODUCCION = "Producción";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter MARCA = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private static Entornos instancia;

    private final Path archivo;
    private final List<Entorno> entornos = new ArrayList<>();
    private String activo;
    private int revision;

    /** Forma del archivo en disco. */
    private static final class Archivo {
        String activo;
        List<Entorno> entornos;
    }

    /**
     * Carga {@code archivo}; si falta crea los ambientes base (tomando las credenciales de
     * {@code perfiles} para Pruebas y, si lo hay, el ambiente personalizado que {@code legado} describe)
     * y lo guarda; si está dañado lo aparta como {@code .corrupto-<fecha>} y empieza de cero.
     */
    public Entornos(Path archivo, List<Profile> perfiles, Entorno legado) {
        this.archivo = archivo;
        if (Files.exists(archivo)) {
            try {
                cargar();
                return;
            } catch (Exception ex) {
                Registro.error("No se pudo leer " + archivo + "; se apartó y se crean los ambientes base", ex);
                apartarDanado();
                entornos.clear();
                activo = null;
            }
        }
        crearBase(perfiles == null ? List.of() : perfiles, legado);
    }

    // --- acceso global ---------------------------------------------------------------------------

    /** Se llama al arrancar con los perfiles ya cargados, para que los ambientes base hereden sus credenciales. */
    public static synchronized void inicializar(List<Profile> perfiles) {
        if (instancia == null) {
            instancia = new Entornos(RutasApp.archivoEntornos(), perfiles, EntornoLegado.leer());
        }
    }

    public static synchronized Entornos instancia() {
        if (instancia == null) {
            instancia = new Entornos(RutasApp.archivoEntornos(), List.of(), null);
        }
        return instancia;
    }

    // --- consulta --------------------------------------------------------------------------------

    public synchronized List<Entorno> lista() {
        return Collections.unmodifiableList(new ArrayList<>(entornos));
    }

    public synchronized Entorno activo() {
        return buscar(activo);
    }

    public synchronized Entorno buscar(String nombre) {
        for (Entorno e : entornos) {
            if (e.nombre.equalsIgnoreCase(nombre == null ? "" : nombre.trim())) {
                return e;
            }
        }
        return null;
    }

    /** Sube con cualquier cambio (guardar, activar, borrar): sirve para detectar que algo cambió desde que se leyó. */
    public synchronized int revision() {
        return revision;
    }

    // --- cambios ---------------------------------------------------------------------------------

    /**
     * Agrega {@code nuevo} (si {@code nombreAnterior} es null) o reemplaza al ambiente que se llamaba
     * {@code nombreAnterior} (permite renombrar). Valida y guarda; si algo falla no cambia nada.
     */
    public synchronized void guardar(String nombreAnterior, Entorno nuevo) throws IOException {
        Entorno limpio = normalizar(nuevo);
        validar(limpio);

        Entorno existente = nombreAnterior == null ? null : buscar(nombreAnterior);
        if (nombreAnterior != null && existente == null) {
            throw new IllegalArgumentException("El ambiente \"" + nombreAnterior + "\" ya no existe.");
        }
        Entorno mismoNombre = buscar(limpio.nombre);
        if (mismoNombre != null && mismoNombre != existente) {
            throw new IllegalArgumentException("Ya existe un ambiente llamado \"" + limpio.nombre + "\".");
        }
        if (existente == activo() && !limpio.listoParaUsar()) {
            throw new IllegalArgumentException("El ambiente activo no puede quedar incompleto (falta "
                    + String.join(", ", limpio.faltantes()) + "). Activa otro ambiente antes de vaciarlo.");
        }

        List<Entorno> antes = new ArrayList<>(entornos);
        String activoAntes = activo;
        if (existente == null) {
            entornos.add(limpio);
        } else {
            entornos.set(entornos.indexOf(existente), limpio);
            if (existente.nombre.equalsIgnoreCase(activo)) {
                activo = limpio.nombre;
            }
        }
        persistirORevertir(antes, activoAntes);
    }

    /** Hace activo a {@code nombre}. Solo si está completo: así nunca se envía con una URL o llave vacía. */
    public synchronized void activar(String nombre) throws IOException {
        Entorno e = buscar(nombre);
        if (e == null) {
            throw new IllegalArgumentException("No existe el ambiente \"" + nombre + "\".");
        }
        if (!e.listoParaUsar()) {
            throw new IllegalArgumentException("\"" + e.nombre + "\" todavía no se puede activar: falta "
                    + String.join(", ", e.faltantes()) + ".");
        }
        List<Entorno> antes = new ArrayList<>(entornos);
        String activoAntes = activo;
        activo = e.nombre;
        persistirORevertir(antes, activoAntes);
    }

    public synchronized void eliminar(String nombre) throws IOException {
        Entorno e = buscar(nombre);
        if (e == null) {
            return;
        }
        if (e == activo()) {
            throw new IllegalArgumentException("No se puede eliminar el ambiente activo. Activa otro primero.");
        }
        if (entornos.size() <= 1) {
            throw new IllegalArgumentException("Tiene que quedar al menos un ambiente.");
        }
        List<Entorno> antes = new ArrayList<>(entornos);
        String activoAntes = activo;
        entornos.remove(e);
        persistirORevertir(antes, activoAntes);
    }

    // --- validación ------------------------------------------------------------------------------

    /** Lanza IllegalArgumentException con un mensaje claro si el ambiente tiene un dato inválido (los vacíos se permiten). */
    public static void validar(Entorno e) {
        if (Entorno.esVacio(e.nombre)) {
            throw new IllegalArgumentException("El ambiente necesita un nombre.");
        }
        if (!Entorno.esVacio(e.url)) {
            SoapHttpClient.validarUrl(e.url);
        }
        if (e.timeoutSegundos < SoapHttpClient.TIMEOUT_MINIMO_SEGUNDOS
                || e.timeoutSegundos > SoapHttpClient.TIMEOUT_MAXIMO_SEGUNDOS) {
            throw new IllegalArgumentException("El timeout debe estar entre " + SoapHttpClient.TIMEOUT_MINIMO_SEGUNDOS
                    + " y " + SoapHttpClient.TIMEOUT_MAXIMO_SEGUNDOS + " segundos.");
        }
        if (!Entorno.esVacio(e.llaveAes)) {
            AES256CBC.validarLlave(e.llaveAes);
        }
    }

    // --- internos --------------------------------------------------------------------------------

    private static Entorno normalizar(Entorno e) {
        Entorno c = e.copia();
        c.nombre = c.nombre == null ? "" : c.nombre.trim();
        c.url = c.url == null ? "" : c.url.trim();
        c.soapAction = c.soapAction == null ? "" : c.soapAction.trim();
        c.llaveAes = c.llaveAes == null ? "" : c.llaveAes.trim();
        c.wsseUsername = c.wsseUsername == null ? "" : c.wsseUsername;
        c.wssePassword = c.wssePassword == null ? "" : c.wssePassword;
        return c;
    }

    private void crearBase(List<Profile> perfiles, Entorno legado) {
        Entorno pruebas = new Entorno(NOMBRE_PRUEBAS);
        pruebas.url = SoapHttpClient.URL_POR_DEFECTO;
        // Las credenciales que ya usaban los perfiles pasan a ser las de Pruebas (es lo único que se conocía).
        for (Profile p : perfiles) {
            if (!Entorno.esVacio(p.llaveAes) && !p.llaveAes.startsWith("REEMPLAZA")) {
                pruebas.llaveAes = p.llaveAes;
                pruebas.wsseUsername = p.wsseUsername == null ? "" : p.wsseUsername;
                pruebas.wssePassword = p.wssePassword == null ? "" : p.wssePassword;
                break;
            }
        }
        Entorno produccion = new Entorno(NOMBRE_PRODUCCION);
        produccion.produccion = true;

        entornos.add(pruebas);
        entornos.add(produccion);
        activo = NOMBRE_PRUEBAS;

        if (legado != null) {
            // Quien ya había cambiado la URL en la versión anterior no pierde esa configuración.
            Entorno personalizado = legado.copia();
            personalizado.llaveAes = pruebas.llaveAes;
            personalizado.wsseUsername = pruebas.wsseUsername;
            personalizado.wssePassword = pruebas.wssePassword;
            entornos.add(personalizado);
            if (personalizado.listoParaUsar()) {
                activo = personalizado.nombre;
            }
        }
        try {
            guardarArchivo();
        } catch (IOException ex) {
            Registro.error("No se pudo crear " + archivo + "; se trabaja con los ambientes en memoria", ex);
        }
    }

    private void cargar() throws IOException {
        Archivo leido = GSON.fromJson(Files.readString(archivo), Archivo.class);
        if (leido == null || leido.entornos == null || leido.entornos.isEmpty()) {
            throw new IllegalStateException("no tiene ambientes definidos");
        }
        for (Entorno e : leido.entornos) {
            entornos.add(normalizar(e));
        }
        activo = leido.activo;
        if (buscar(activo) == null) {
            throw new IllegalStateException("el ambiente activo \"" + activo + "\" no existe");
        }
    }

    private void apartarDanado() {
        try {
            Files.move(archivo, archivo.resolveSibling(
                    archivo.getFileName() + ".corrupto-" + MARCA.format(LocalDateTime.now())));
        } catch (IOException ex) {
            Registro.error("No se pudo apartar " + archivo, ex);
        }
    }

    private void guardarArchivo() throws IOException {
        Archivo salida = new Archivo();
        salida.activo = activo;
        salida.entornos = entornos;
        ArchivoAtomico.escribir(archivo, GSON.toJson(salida));
        revision++;
    }

    private void persistirORevertir(List<Entorno> antes, String activoAntes) throws IOException {
        try {
            guardarArchivo();
        } catch (IOException | RuntimeException ex) {
            entornos.clear();
            entornos.addAll(antes);
            activo = activoAntes;
            throw ex;
        }
    }
}
