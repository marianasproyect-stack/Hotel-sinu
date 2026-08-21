package com.hotelgestion.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hotelgestion.enums.EstadoReserva;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "RESERVAS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Reserva {

    @Id
    @SequenceGenerator(name = "reserva_seq", sequenceName = "reserva_seq", allocationSize = 1, initialValue = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "reserva_seq")
    private Long id;

    @NotNull(message = "El cliente es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CLIENTE_ID", nullable = false)
    private Cliente cliente;

    @NotNull(message = "La habitacion es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "HABITACION_ID", nullable = false)
    private Habitacion habitacion;

    @NotNull(message = "La fecha de entrada es obligatoria")
    @Column(name = "FECHA_ENTRADA", nullable = false)
    private LocalDate fechaEntrada;

    @NotNull(message = "La fecha de salida es obligatoria")
    @Column(name = "FECHA_SALIDA", nullable = false)
    private LocalDate fechaSalida;

    @NotNull(message = "El estado es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false, length = 30)
    private EstadoReserva estado = EstadoReserva.PENDIENTE;

    @Column(name = "FECHA_CREACION", nullable = false)
    private LocalDate fechaCreacion = LocalDate.now();

    // Plazo (24h despues de crear la reserva) para pagar antes de que se
    // cancele automaticamente. Solo aplica mientras el estado es PENDIENTE.
    @Column(name = "FECHA_LIMITE_PAGO")
    private java.time.LocalDateTime fechaLimitePago;

    // Descuento (en fraccion, ej. 0.10 = 10%) que se aplico a esta reserva
    // al momento de crearla, si el cliente tenia un descuento por canje de
    // puntos disponible (ver ClienteService.canjearDescuento).
    @Column(name = "DESCUENTO_APLICADO")
    private Double descuentoAplicado = 0.0;

    @OneToMany(mappedBy = "reserva", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Pago> pagos = new ArrayList<>();

    // ---- Datos reales del check-in (se llenan cuando recepcion lo hace) ----

    @Column(name = "FECHA_CHECK_IN")
    private java.time.LocalDateTime fechaCheckIn;

    @Column(name = "DOCUMENTO_CHECK_IN", length = 50)
    private String documentoHuespedCheckIn;

    @Column(name = "NUM_ACOMPANANTES")
    private Integer numAcompanantes;

    @Column(name = "OBSERVACIONES_CHECK_IN", length = 500)
    private String observacionesCheckIn;

    // ---- Datos reales del check-out ----

    @Column(name = "FECHA_CHECK_OUT")
    private java.time.LocalDateTime fechaCheckOut;

    // Estado en que quedo la habitacion al salir el huesped (ej. "OK",
    // "DANOS"), para que quede registro antes de mandarla a limpieza.
    @Column(name = "ESTADO_HABITACION_CHECK_OUT", length = 20)
    private String estadoHabitacionCheckOut;

    @Column(name = "OBSERVACIONES_CHECK_OUT", length = 500)
    private String observacionesCheckOut;
}
