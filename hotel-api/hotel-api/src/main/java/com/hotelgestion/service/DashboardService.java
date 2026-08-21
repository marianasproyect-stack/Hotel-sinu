package com.hotelgestion.service;

import com.hotelgestion.dto.DashboardDTO;
import com.hotelgestion.enums.EstadoHabitacion;
import com.hotelgestion.enums.EstadoPago;
import com.hotelgestion.enums.EstadoReserva;
import com.hotelgestion.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DashboardService {

    private static final double UMBRAL_ALERTA = 3.0;

    private final HabitacionRepository habitacionRepository;
    private final ReservaRepository reservaRepository;
    private final ClienteRepository clienteRepository;
    private final PagoRepository pagoRepository;
    private final ResenaRepository resenaRepository;
    private final PuntosFidelidadRepository puntosFidelidadRepository;

    public DashboardDTO obtenerResumen() {
        long totalHab = habitacionRepository.count();
        long disponibles = habitacionRepository.findByEstado(EstadoHabitacion.DISPONIBLE).size();
        long ocupadas = habitacionRepository.findByEstado(EstadoHabitacion.OCUPADA).size();
        long reservadas = habitacionRepository.findByEstado(EstadoHabitacion.RESERVADA).size();
        long enLimpieza = habitacionRepository.findByEstado(EstadoHabitacion.EN_LIMPIEZA).size();
        long pendienteLimpieza = habitacionRepository.findByEstado(EstadoHabitacion.PENDIENTE_LIMPIEZA).size();
        long fueraServicio = habitacionRepository.findByEstado(EstadoHabitacion.FUERA_DE_SERVICIO).size();

        double porcentajeOcupacion = totalHab > 0
                ? Math.round(((ocupadas + reservadas) * 100.0 / totalHab) * 10.0) / 10.0
                : 0.0;

        BigDecimal ingresosTotal = reservaRepository.ingresosTotal();
        LocalDate inicioMes = LocalDate.now().withDayOfMonth(1);
        BigDecimal ingresosMes = reservaRepository.ingresosDesdeFecha(inicioMes);

        long clientesFrecuentes = puntosFidelidadRepository
                .findByCategoriaIn(List.of("ORO", "PLATINUM")).size();

        List<String> alertas = generarAlertas();

        return DashboardDTO.builder()
                .totalHabitaciones(totalHab)
                .habitacionesDisponibles(disponibles)
                .habitacionesOcupadas(ocupadas)
                .habitacionesReservadas(reservadas)
                .habitacionesEnLimpieza(enLimpieza)
                .habitacionesPendienteLimpieza(pendienteLimpieza)
                .habitacionesFueraServicio(fueraServicio)
                .porcentajeOcupacion(porcentajeOcupacion)
                .totalReservas(reservaRepository.count())
                .reservasPendientes(reservaRepository.countByEstado(EstadoReserva.PENDIENTE))
                .reservasConfirmadas(reservaRepository.countByEstado(EstadoReserva.CONFIRMADA))
                .reservasEnCurso(reservaRepository.countByEstado(EstadoReserva.EN_CURSO))
                .reservasFinalizadas(reservaRepository.countByEstado(EstadoReserva.FINALIZADA))
                .reservasCanceladas(reservaRepository.countByEstado(EstadoReserva.CANCELADA))
                .totalClientes(clienteRepository.count())
                .clientesFrecuentes(clientesFrecuentes)
                .ingresosTotal(ingresosTotal)
                .ingresosMes(ingresosMes)
                .pagosPendientes(pagoRepository.countByEstado(EstadoPago.PENDIENTE))
                .calificacionPromedio(resenaRepository.promedioGeneral())
                .alertas(alertas)
                .build();
    }

    private List<String> generarAlertas() {
        List<String> alertas = new ArrayList<>();
        LocalDate hace30Dias = LocalDate.now().minusDays(30);

        Double promedioDesayuno = resenaRepository.promedioDesayunoDesde(hace30Dias);
        Double promedioLimpieza = resenaRepository.promedioLimpiezaDesde(hace30Dias);
        Double promedioAtencion = resenaRepository.promedioAtencionDesde(hace30Dias);
        Double promedioInstalaciones = resenaRepository.promedioInstalacionesDesde(hace30Dias);

        if (promedioDesayuno != null && promedioDesayuno < UMBRAL_ALERTA) {
            alertas.add(String.format("⚠️ El servicio de desayuno presenta una baja calificacion promedio (%.1f/5) en los ultimos 30 dias", promedioDesayuno));
        }
        if (promedioLimpieza != null && promedioLimpieza < UMBRAL_ALERTA) {
            alertas.add(String.format("⚠️ El servicio de limpieza presenta una baja calificacion promedio (%.1f/5) en los ultimos 30 dias", promedioLimpieza));
        }
        if (promedioAtencion != null && promedioAtencion < UMBRAL_ALERTA) {
            alertas.add(String.format("⚠️ La atencion al cliente presenta una baja calificacion promedio (%.1f/5) en los ultimos 30 dias", promedioAtencion));
        }
        if (promedioInstalaciones != null && promedioInstalaciones < UMBRAL_ALERTA) {
            alertas.add(String.format("⚠️ Las instalaciones presentan una baja calificacion promedio (%.1f/5) en los ultimos 30 dias", promedioInstalaciones));
        }

        long pendienteLimpieza = habitacionRepository.findByEstado(EstadoHabitacion.PENDIENTE_LIMPIEZA).size();
        if (pendienteLimpieza > 0) {
            alertas.add(String.format("🧹 Hay %d habitacion(es) pendientes de limpieza", pendienteLimpieza));
        }

        return alertas;
    }
}
