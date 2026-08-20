package com.hotelgestion.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class NivelCanjeDTO {
    private int nivel;
    private String nombre;
    private String descuento;        // texto para mostrar, ej. "10%"
    private int puntosRequeridos;
    private boolean disponible;      // el cliente tiene puntos suficientes
}
