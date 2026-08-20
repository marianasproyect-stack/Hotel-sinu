package com.hotelgestion.dto;

import com.hotelgestion.enums.EstadoReserva;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class ReservaResponseDTO {

    private Long id;
    private Long clienteId;
    private String clienteNombre;
    private String clienteDocumento;
    private Long habitacionId;
    private String habitacionNumero;
    private String habitacionTipo;
    private BigDecimal precioNoche;
    private LocalDate fechaEntrada;
    private LocalDate fechaSalida;
    private EstadoReserva estado;
    private LocalDate fechaCreacion;
    private java.time.LocalDateTime fechaLimitePago;
    private BigDecimal totalEstimado;
    private Double descuentoAplicado;      // fraccion, ej. 0.10 = 10%. 0 si no aplico ninguno.
    private BigDecimal totalConDescuento;  // igual a totalEstimado si no hubo descuento
    private Boolean tieneResena;           // true si esta reserva ya tiene una reseña asociada

    // Datos reales del check-in / check-out (null si aun no ha pasado)
    private java.time.LocalDateTime fechaCheckIn;
    private String documentoHuespedCheckIn;
    private Integer numAcompanantes;
    private String observacionesCheckIn;
    private java.time.LocalDateTime fechaCheckOut;
    private String estadoHabitacionCheckOut;
    private String observacionesCheckOut;
}
