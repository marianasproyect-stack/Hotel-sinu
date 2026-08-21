package com.hotelgestion.repository;

import com.hotelgestion.enums.EstadoReserva;
import com.hotelgestion.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByClienteId(Long clienteId);

    List<Reserva> findByHabitacionId(Long habitacionId);

    List<Reserva> findByEstado(EstadoReserva estado);

    // Reservas PENDIENTES cuyo plazo para pagar (24h) ya vencio. Se usan
    // para cancelarlas automaticamente y liberar esas fechas.
    java.util.List<Reserva> findByEstadoAndFechaLimitePagoBefore(EstadoReserva estado, java.time.LocalDateTime momento);

    long countByEstado(EstadoReserva estado);

    // Reservas CONFIRMADAS cuya fecha de entrada es la indicada (se usa para
    // la vista "Llegadas de hoy" de recepcion).
    List<Reserva> findByEstadoAndFechaEntrada(EstadoReserva estado, LocalDate fechaEntrada);

    // Reservas EN_CURSO cuya fecha de salida es la indicada (vista "Salidas
    // de hoy", para saber a quien hay que hacerle check-out).
    List<Reserva> findByEstadoAndFechaSalida(EstadoReserva estado, LocalDate fechaSalida);

    // Ingresos totales de reservas finalizadas con pago PAGADO
    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p WHERE p.estado = 'PAGADO'")
    BigDecimal ingresosTotal();

    // Ingresos del mes actual
    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p WHERE p.estado = 'PAGADO' AND p.fechaPago >= :inicioMes")
    BigDecimal ingresosDesdeFecha(LocalDate inicioMes);

    // Verifica si hay conflicto de fechas para una habitacion.
    // Trata el rango como INCLUSIVO en ambos extremos: si una reserva va del
    // 19 al 22, el 22 tambien cuenta como ocupado (no se libera hasta el 23).
    // Por eso se usa <= / >= en vez de < / > (que es lo estandar en
    // hoteleria, donde el dia de salida ya es el de entrada del siguiente
    // huesped, pero el profesor pidio que aqui NO se comporte asi).
    @Query("""
            SELECT COUNT(r) > 0 FROM Reserva r
            WHERE r.habitacion.id = :habitacionId
            AND r.estado NOT IN ('CANCELADA', 'FINALIZADA')
            AND r.fechaEntrada <= :fechaSalida
            AND r.fechaSalida >= :fechaEntrada
            """)
    boolean existeConflictoFechas(Long habitacionId, LocalDate fechaEntrada, LocalDate fechaSalida);
}
