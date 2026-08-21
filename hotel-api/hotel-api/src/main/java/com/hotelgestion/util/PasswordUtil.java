package com.hotelgestion.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

// Hash simple de contrasenas con SHA-256. No es lo que se usaria en un
// sistema en produccion (ahi se usaria BCrypt con salt), pero evita
// guardar la contrasena en texto plano para el alcance de este proyecto.
public class PasswordUtil {

    private PasswordUtil() {
    }

    public static String hash(String textoPlano) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(textoPlano.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("No se pudo hashear la contrasena", e);
        }
    }

    public static boolean matches(String textoPlano, String hashGuardado) {
        return hash(textoPlano).equals(hashGuardado);
    }
}
