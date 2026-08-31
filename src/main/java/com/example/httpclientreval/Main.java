package com.example.httpclientreval;

import com.example.httpclientreval.crypto.AES256CBC;
import com.example.httpclientreval.model.Envelope;

/**
 * Punto de entrada manual: edita las constantes de abajo y corre este archivo
 * con el botón ▶️ de IntelliJ (sin argumentos por CLI).
 *
 * IMPORTANTE (seguridad):
 * - LLAVE_AES es un secreto real de negocio. No la dejes commiteada en git.
 * - Úsala solo en local para pruebas; bórrala de este archivo y de la consola
 *   cuando termines. Considera pasarla por variable de entorno si vas a
 *   compartir este repo con más gente.
 */
public class Main {

    // ================= CONFIGURACIÓN EDITABLE =================

    // "DECRYPT" para descifrar un _mensaje/sobre recibido,
    // "ENCRYPT" para generar un _mensaje/sobre nuevo.
    private static final String MODO = "ENCRYPT";

    // Llave AES de 32 bytes (llega por login del backend). Pégala solo en local.
    private static final String LLAVE_AES = "68f12b60-d1dd-4eeb-85ce-b0b95f83";

    // --- Usado si MODO = "DECRYPT" ---
    // Puede ser SOLO el valor de "_mensaje" en base64, o el sobre JSON completo
    // {"_header":{...},"_body":{"_mensaje":"..."}}. Main detecta cuál es.
    private static final String CIFRADO_A_DESCIFRAR = "BUvYy5Hmt8tEuhfMvQTZkJOVSxykwmspet0K1y0wIDSAK3BPvDz6MRFOVPb/ezjayOoL3WGGBCo5LQiR1DrfxhLh+aBCLxsLhEtt2SC7g5MpPqtLLE2WuqKHY+yDkVenmhX/LBErGF9or+u+HJeAXt1dtS7rpVmqMh8U8YAXuchAGV2yLt0Q3BXtJ/I+VSbPafQFD8iXsblf8a1vjw7s7Kgjw/sEmj+V7E/OU4mMHCY9XosL0XFRdsFnhUUqznd/oR1JnM2+TAGaTm2671SRPWyWjemqTeU1RqR55F3wGnjnSRUhFzbz20XP2kLn52S0uhSR1jA39NuWNhzefC+HZ/SdySxHVNAtlaT9SrGtJ+U89lMnW6DaQbXo53eJ/baami6zdShgPVeGGjBgMGm23mb0RdV198BGkg3Nd8J/C2JN9HhsGNa/OK6t9SnKHCxUPGICS9IBKxAS1vuX7KNhH77ndEw+xGhc1SKAAxVaEiKbsS6p36sNAF7dDK8LZQW3LVneaA2inrJ9PxFxETNUV7FZTHu3w0vhUT4+IH1vSAlG9cQj3bpp3oY/tY/+J0lc2s1WEeTCqo0wHp7D8NK7FjTyBuVrwia7ht5KW/hfEgI5X5bONxHuNpgEYcYyq7+gi6bdb7Ht+Y0OwHdgrlMzQ74tvW/6YMy9lJlvHIyF1A4Pr/6w0KUvpuI0FCpc/xNemqAE4sn8HecxMw8oakC+wVf+ONBjzBQKgGZCwO2xF3zz6e/7WPxMyYwCadzwgibPPKcfQTiGhpunnV13LSAXWrznnZSXGBKvRr0w/GmnwWERd6OgQexVGfTx1lRZ+hyiZxf0rEU+xRMeq03tLOSMNdi6r8aTiA4VAm3YFgo+Il+CAVXdx5f+wQnk7bSVQRWiLGtmkILkaiFvi5LMgb2j6Y/V0cCNy2eIdLfWfWH4BnTTsqtnL4W+rttr+p1czYA5LCEhLuhKDw2RBq8A7k0eHCumy6120KkB4JSuhD4od3uNwhltSwHgbK/xvHFjwxctdKf5iFzavyJx7GWk4mM0ffj8KcSb2cWg98k/U27bHa1zjIyrqM9QKUM1l8LYcvsYWEB6IveO7+oLUy0e9Uu4jR2hmg0V8uPJOYBpP0j9GmAenFMOEnd3CCY4bgYgbEFWegU1gGbi8OzyXjlKnkK1h6d9FHariyoB9NoAIL1dbAa1GjbAG9Bzpnj34qs7Uf8OIx10D8vhr0NVFSIIRm3GUWqnEKYVKu7rtTLVf1JO5G8=";

    // --- Usado si MODO = "ENCRYPT" ---
    private static final String JSON_A_CIFRAR =
            "{\n" +
                    "  \"_body\": {\n" +
                    "    \"_BodyMensaje\": {\n" +
                    "      \"Autorizacion\": \"\",\n" +
                    "      \"CodBarras\": \"\",\n" +
                    "      \"Convenio\": \"\",\n" +
                    "      \"FechaVencimiento\": \"\",\n" +
                    "      \"Iac\": \"\",\n" +
                    "      \"IdPersona\": \"226\",\n" +
                    "      \"IdTransaccion\": \"131911\",\n" +
                    "      \"NoIdentificacionUsuario\": \"4045175\",\n" +
                    "      \"NombreUsuario\": \"\",\n" +
                    "      \"NumCelular\": \"\",\n" +
                    "      \"Observacion\": \"\",\n" +
                    "      \"Otp\": \"\",\n" +
                    "      \"Referencia1\": \"439212030262\",\n" +
                    "      \"Referencia10\": \"\",\n" +
                    "      \"Referencia11\": \"\",\n" +
                    "      \"Referencia12\": \"0\",\n" +
                    "      \"Referencia13\": \"0\",\n" +
                    "      \"Referencia14\": \"0\",\n" +
                    "      \"Referencia15\": \"0\",\n" +
                    "      \"Referencia2\": \"4\",\n" +
                    "      \"Referencia3\": \"\",\n" +
                    "      \"Referencia4\": \"\",\n" +
                    "      \"Referencia5\": \"\",\n" +
                    "      \"Referencia6\": \"\",\n" +
                    "      \"Referencia7\": \"\",\n" +
                    "      \"Referencia8\": \"\",\n" +
                    "      \"Referencia9\": \"\",\n" +
                    "      \"TipoIdentificacion\": \"1\",\n" +
                    "      \"Valor\": \"1000\"\n" +
                    "    },\n" +
                    "    \"_headerMensaje\": {\n" +
                    "      \"NoIdentificacionCajero\": \"H3R000200014598\"\n" +
                    "    }\n" +
                    "  }\n" +
                    "}";
    private static final int ID_CLIENTE = 26;
    private static final int ID_TRANSACCION = 1;
    private static final String IP_CLIENTE = "172.17.0.4";

    // ============================================================

    public static void main(String[] args) throws Exception {
        // Falla rápido y con mensaje claro si la llave no tiene 32 bytes.
        AES256CBC.validarLlave(LLAVE_AES);

        switch (MODO) {
            case "DECRYPT":
                decrypt();
                break;
            case "ENCRYPT":
                encrypt();
                break;
            default:
                throw new IllegalArgumentException(
                        "MODO debe ser \"DECRYPT\" o \"ENCRYPT\". Valor actual: " + MODO);
        }
    }

    private static void decrypt() throws Exception {
        String entrada = CIFRADO_A_DESCIFRAR.trim();
        String base64Mensaje;

        if (entrada.startsWith("{")) {
            base64Mensaje = Envelope.extraerMensaje(entrada);
            System.out.println("[info] Se detectó un sobre JSON completo; se extrajo _body._mensaje.");
        } else {
            base64Mensaje = entrada;
        }

        String jsonPlano = AES256CBC.decryptWithPrependedIV(base64Mensaje, LLAVE_AES);

        System.out.println();
        System.out.println("=== JSON DE NEGOCIO EN CLARO ===");
        System.out.println(jsonPlano);
    }

    private static void encrypt() throws Exception {
        String mensajeCifrado = AES256CBC.encryptWithRandomIV(JSON_A_CIFRAR, LLAVE_AES);

        System.out.println("=== _mensaje (BASE64, para pegar suelto si lo necesitas) ===");
        System.out.println(mensajeCifrado);

        String sobreCompleto = Envelope.build(mensajeCifrado, ID_CLIENTE, ID_TRANSACCION, IP_CLIENTE);

        System.out.println();
        System.out.println("=== SOBRE COMPLETO (pegar dentro de <tem:data> del XML SOAP) ===");
        System.out.println(sobreCompleto);
    }
}
