package com.hotelgestion.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hotelgestion.enums.EstadoHabitacion;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "HABITACIONES")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Habitacion {

    @Id
    @SequenceGenerator(name = "habitacion_seq", sequenceName = "habitacion_seq", allocationSize = 1, initialValue = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "habitacion_seq")
    private Long id;

    @NotBlank(message = "El numero de habitacion es obligatorio")
    @Column(name = "NUMERO", nullable = false, unique = true, length = 10)
    private String numero;

    @NotBlank(message = "El tipo de habitacion es obligatorio")
    @Column(name = "TIPO", nullable = false, length = 50)
    private String tipo; // Ej: Individual, Doble, Suite

    @NotNull(message = "La capacidad es obligatoria")
    @Positive(message = "La capacidad debe ser mayor a 0")
    @Column(name = "CAPACIDAD", nullable = false)
    private Integer capacidad;

    @NotNull(message = "El precio es obligatorio")
    @Positive(message = "El precio debe ser mayor a 0")
    @Column(name = "PRECIO_NOCHE", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioNoche;

    @Column(name = "DESCRIPCION", length = 500)
    private String descripcion;

    @NotNull(message = "El estado es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false, length = 30)
    private EstadoHabitacion estado = EstadoHabitacion.DISPONIBLE;

    @OneToMany(mappedBy = "habitacion", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Reserva> reservas = new ArrayList<>();
}
