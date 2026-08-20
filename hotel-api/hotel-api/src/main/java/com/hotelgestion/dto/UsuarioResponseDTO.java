package com.hotelgestion.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponseDTO {

    private Long id;
    private String nombre;
    private String correo;
    private String rol;        // CLIENTE o ADMINISTRADOR
    private Long clienteId;    // solo si rol = CLIENTE
    private Integer puntos;    // solo si rol = CLIENTE
}
