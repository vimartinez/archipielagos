package com.mijuego.archipielago.servidor.sala;

import java.security.SecureRandom;
import java.util.function.Supplier;

/** Genera códigos de sala de 4 caracteres sin caracteres ambiguos ({@code 0}, {@code O}, {@code 1}, {@code I}). */
public final class GeneradorCodigos implements Supplier<String> {

    public static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    public static final int LONGITUD = 4;

    private final SecureRandom azar = new SecureRandom();

    @Override
    public String get() {
        StringBuilder codigo = new StringBuilder(LONGITUD);
        for (int i = 0; i < LONGITUD; i++) {
            codigo.append(ALFABETO.charAt(azar.nextInt(ALFABETO.length())));
        }
        return codigo.toString();
    }
}
