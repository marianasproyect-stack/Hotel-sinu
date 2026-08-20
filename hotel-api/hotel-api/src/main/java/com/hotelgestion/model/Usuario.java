package com.hotelgestion.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Tabla de login. Un usuario tiene un rol (CLIENTE o ADMINISTRADOR).
// Si el rol es CLIENTE, tambien queda creado un registro en CLIENTES
// (clienteId) para poder reutilizar todo lo que ya existe de reservas,
// pagos y puntos de fidelidad sobre esa tabla.
@Entity
@Table(name = "USUARIOS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @SequenceGenerator(name = "usuario_seq", sequenceName = "usuario_seq", allocationSize = 1, initialValue = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "usuario_seq")
    private Long id;

    // Ahora el nombre se usa para iniciar sesion (junto con la
    // contrasena), por eso debe ser unico entre todos los usuarios.
    @NotBlank(message = "El nombre es obligatorio")
    @Column(name = "NOMBRE", nullable = false, unique = true, length = 150)
    private String nombre;

    @Email(message = "El correo debe tener un formato valido")
    @NotBlank(message = "El correo es obligatorio")
    @Column(name = "CORREO", nullable = false, unique = true, length = 150)
    private String correo;

    @NotBlank(message = "La contrasena es obligatoria")
    @JsonIgnore
    @Column(name = "CONTRASENA", nullable = false, length = 255)
    private String contrasena;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ROL_ID", nullable = false)
    private Rol rol;

    // Solo se llena cuando rol = CLIENTE. Apunta al id de CLIENTES.
    @Column(name = "CLIENTE_ID")
    private Long clienteId;
}
