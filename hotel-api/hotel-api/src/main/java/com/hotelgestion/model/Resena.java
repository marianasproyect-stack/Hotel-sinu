package com.hotelgestion.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "RESENAS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Resena {

    @Id
    @SequenceGenerator(name = "resena_seq", sequenceName = "resena_seq", allocationSize = 1, initialValue = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "resena_seq")
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CLIENTE_ID", nullable = false)
    private Cliente cliente;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RESERVA_ID", nullable = false)
    private Reserva reserva;

    @NotNull
    @Min(1) @Max(5)
    @Column(name = "CALIF_GENERAL", nullable = false)
    private Integer califGeneral;

    @NotNull
    @Min(1) @Max(5)
    @Column(name = "CALIF_LIMPIEZA", nullable = false)
    private Integer califLimpieza;

    @NotNull
    @Min(1) @Max(5)
    @Column(name = "CALIF_ATENCION", nullable = false)
    private Integer califAtencion;

    @NotNull
    @Min(1) @Max(5)
    @Column(name = "CALIF_DESAYUNO", nullable = false)
    private Integer califDesayuno;

    @NotNull
    @Min(1) @Max(5)
    @Column(name = "CALIF_INSTALACIONES", nullable = false)
    private Integer califInstalaciones;

    @Column(name = "COMENTARIO", length = 1000)
    private String comentario;

    @Column(name = "FECHA_RESENA", nullable = false)
    private LocalDate fechaResena = LocalDate.now();
}
