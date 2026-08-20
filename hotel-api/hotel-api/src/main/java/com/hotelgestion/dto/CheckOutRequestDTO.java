package com.hotelgestion.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckOutRequestDTO {

    // "OK" si la habitacion queda en buen estado, "DANOS" si hay que
    // reportar algo antes de mandarla a limpieza/mantenimiento.
    private String estadoHabitacion = "OK";

    // Notas libres de recepcion (ej. "faltan toallas", "aire acondicionado
    // no enfria", "todo en orden").
    private String observaciones;
}
