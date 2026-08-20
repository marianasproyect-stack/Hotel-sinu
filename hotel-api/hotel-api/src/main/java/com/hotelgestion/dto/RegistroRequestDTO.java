package com.hotelgestion.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistroRequestDTO {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    // Los siguientes 3 campos solo se usan cuando el rol es CLIENTE,
    // porque la tabla CLIENTES los pide obligatorios. Se validan a mano
    // en AuthService (no con @NotBlank aqui) porque el administrador
    // no los necesita para registrarse.
    private String apellido;
    private String telefono;
    private String documento;

    @Email(message = "El correo debe tener un formato valido")
    @NotBlank(message = "El correo es obligatorio")
    private String correo;

    @NotBlank(message = "La contrasena es obligatoria")
    private String contrasena;

    // Este campo ya no se usa: el registro publico siempre crea CLIENTE
    // (ver AuthService.registrar). Se deja aqui solo por compatibilidad,
    // pero no es obligatorio ni se tiene en cuenta.
    private String rol;
}
