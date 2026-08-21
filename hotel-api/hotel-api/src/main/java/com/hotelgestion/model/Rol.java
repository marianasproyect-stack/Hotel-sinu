package com.hotelgestion.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ROLES")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Rol {

    @Id
    @SequenceGenerator(name = "rol_seq", sequenceName = "rol_seq", allocationSize = 1, initialValue = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rol_seq")
    private Long id;

    // CLIENTE o ADMINISTRADOR
    @NotBlank(message = "El nombre del rol es obligatorio")
    @Column(name = "NOMBRE", nullable = false, unique = true, length = 30)
    private String nombre;
}
