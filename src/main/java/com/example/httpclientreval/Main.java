package com.example.httpclientreval;

import com.example.httpclientreval.crypto.AES256CBC;
import com.example.httpclientreval.model.Envelope;
import com.example.httpclientreval.model.MensajeNegocio;
import com.example.httpclientreval.model.Profile;
import com.example.httpclientreval.ui.MensajeNegocioForm;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

/**
 * Punto de entrada interactivo: corre este archivo con el botón ▶️ de IntelliJ
 * (sin argumentos por CLI) y sigue el menú en consola.
 *
 * La llave AES y los datos de ejemplo ya NO se editan en este archivo: la
 * llave y los defaults del header viven en profiles.json (copia
 * profiles.example.json y complétalo), y el dato a cifrar/descifrar se pega
 * en consola en cada corrida. profiles.json está en .gitignore para que
 * ningún secreto real quede commiteado.
 */
public class Main {

    private static final Path ARCHIVO_PERFILES = Paths.get("profiles.json");

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        List<Profile> perfiles = Profile.loadAll(ARCHIVO_PERFILES);
        Profile perfil = elegirPerfil(scanner, perfiles);
        AES256CBC.validarLlave(perfil.llaveAes);

        System.out.println();
        System.out.println("1) DECRYPT (descifrar un _mensaje/sobre recibido)");
        System.out.println("2) ENCRYPT (generar un _mensaje/sobre nuevo)");
        System.out.print("Elige modo: ");
        String modo = scanner.nextLine().trim();

        switch (modo) {
            case "1":
                decrypt(scanner, perfil);
                break;
            case "2":
                encrypt(scanner, perfil);
                break;
            default:
                throw new IllegalArgumentException("Opción inválida: " + modo);
        }
    }

    private static Profile elegirPerfil(Scanner scanner, List<Profile> perfiles) {
        System.out.println("=== Perfiles disponibles (" + ARCHIVO_PERFILES.toAbsolutePath() + ") ===");
        for (int i = 0; i < perfiles.size(); i++) {
            System.out.println((i + 1) + ") " + perfiles.get(i).nombre);
        }
        System.out.print("Elige un perfil: ");
        int indice = Integer.parseInt(scanner.nextLine().trim()) - 1;
        if (indice < 0 || indice >= perfiles.size()) {
            throw new IllegalArgumentException("Perfil inválido.");
        }
        return perfiles.get(indice);
    }

    private static void decrypt(Scanner scanner, Profile perfil) throws Exception {
        System.out.println();
        System.out.println("Pega el _mensaje en base64, o el sobre JSON completo, y presiona Enter:");
        String entrada = scanner.nextLine().trim();

        String base64Mensaje;
        if (entrada.startsWith("{")) {
            base64Mensaje = Envelope.extraerMensaje(entrada);
            System.out.println("[info] Se detectó un sobre JSON completo; se extrajo _body._mensaje.");
        } else {
            base64Mensaje = entrada;
        }

        String jsonPlano = AES256CBC.decryptWithPrependedIV(base64Mensaje, perfil.llaveAes);

        System.out.println();
        System.out.println("=== JSON DE NEGOCIO EN CLARO ===");
        System.out.println(jsonPlano);
    }

    private static void encrypt(Scanner scanner, Profile perfil) throws Exception {
        MensajeNegocioForm.Resultado datos = MensajeNegocioForm.mostrar(
                new MensajeNegocio.BodyMensaje(),
                new MensajeNegocio.HeaderMensaje(),
                perfil.idClienteDefault,
                perfil.idTransaccionDefault,
                perfil.ipClienteDefault);

        if (datos == null) {
            System.out.println("Cancelado.");
            return;
        }

        String jsonNegocio = MensajeNegocio.build(datos.body, datos.header);
        String mensajeCifrado = AES256CBC.encryptWithRandomIV(jsonNegocio, perfil.llaveAes);

        System.out.println();
        System.out.println("=== _mensaje (BASE64, para pegar suelto si lo necesitas) ===");
        System.out.println(mensajeCifrado);

        String sobreCompleto = Envelope.build(mensajeCifrado, datos.idCliente, datos.idTransaccion, datos.ipCliente);

        System.out.println();
        System.out.println("=== SOBRE COMPLETO (pegar dentro de <tem:data> del XML SOAP) ===");
        System.out.println(sobreCompleto);
    }
}
