package com.hotelgestion.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CanjeDescuentoResponseDTO {
    private String porcentaje;       // texto para mostrar, ej. "10%"
    private int puntosRestantes;
}
