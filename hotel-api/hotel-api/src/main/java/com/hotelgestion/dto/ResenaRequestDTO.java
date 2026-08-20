package com.hotelgestion.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResenaRequestDTO {

    @NotNull(message = "El ID del cliente es obligatorio")
    private Long clienteId;

    @NotNull(message = "El ID de la reserva es obligatorio")
    private Long reservaId;

    @NotNull @Min(1) @Max(5)
    private Integer califGeneral;

    @NotNull @Min(1) @Max(5)
    private Integer califLimpieza;

    @NotNull @Min(1) @Max(5)
    private Integer califAtencion;

    @NotNull @Min(1) @Max(5)
    private Integer califDesayuno;

    @NotNull @Min(1) @Max(5)
    private Integer califInstalaciones;

    private String comentario;
}
