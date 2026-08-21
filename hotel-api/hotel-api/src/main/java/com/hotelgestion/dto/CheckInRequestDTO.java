package com.hotelgestion.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckInRequestDTO {

    // Documento de identidad que el huesped presenta al llegar. Recepcion lo
    // verifica contra el documento del cliente registrado.
    @NotBlank(message = "El documento del huesped es obligatorio para el check-in")
    private String documentoHuesped;

    // Cuantas personas adicionales al titular se estan alojando (opcional).
    private Integer numAcompanantes = 0;

    // Notas libres de recepcion (ej. "llego con mascota", "pidio piso alto").
    private String observaciones;
}
