package com.hotelgestion.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
public class DashboardDTO {

    // Ocupación
    private long totalHabitaciones;
    private long habitacionesDisponibles;
    private long habitacionesOcupadas;
    private long habitacionesReservadas;
    private long habitacionesEnLimpieza;
    private long habitacionesPendienteLimpieza;
    private long habitacionesFueraServicio;
    private double porcentajeOcupacion;

    // Reservas
    private long totalReservas;
    private long reservasPendientes;
    private long reservasConfirmadas;
    private long reservasEnCurso;
    private long reservasFinalizadas;
    private long reservasCanceladas;

    // Clientes
    private long totalClientes;
    private long clientesFrecuentes; // categoria ORO o PLATINUM

    // Finanzas
    private BigDecimal ingresosTotal;
    private BigDecimal ingresosMes;
    private long pagosPendientes;

    // Calidad
    private Double calificacionPromedio;
    private List<String> alertas;
}
