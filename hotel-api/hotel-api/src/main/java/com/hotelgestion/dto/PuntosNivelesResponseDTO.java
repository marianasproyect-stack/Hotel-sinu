package com.hotelgestion.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class PuntosNivelesResponseDTO {
    private int puntosDisponibles;

    // "Disponible ahora" si no hay cooldown activo, o una fecha ISO
    // (yyyy-MM-dd) desde la cual se podra volver a canjear.
    private String proximoCanjeDisponible;

    private List<NivelCanjeDTO> niveles;
}
