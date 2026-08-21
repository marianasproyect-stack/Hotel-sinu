package com.hotelgestion.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ResenaResponseDTO {

    private Long id;
    private Long clienteId;
    private String clienteNombre;
    private Long reservaId;
    private Integer califGeneral;
    private Integer califLimpieza;
    private Integer califAtencion;
    private Integer califDesayuno;
    private Integer califInstalaciones;
    private String comentario;
    private LocalDate fechaResena;
}
