package com.hotelgestion.service;

import com.hotelgestion.dto.CheckInRequestDTO;
import com.hotelgestion.dto.CheckOutRequestDTO;
import com.hotelgestion.dto.ReservaRequestDTO;
import com.hotelgestion.dto.ReservaResponseDTO;
import com.hotelgestion.enums.EstadoHabitacion;
import com.hotelgestion.enums.EstadoReserva;
import com.hotelgestion.model.Cliente;
import com.hotelgestion.model.Habitacion;
import com.hotelgestion.model.PuntosFidelidad;
import com.hotelgestion.model.Reserva;
import com.hotelgestion.repository.ClienteRepository;
import com.hotelgestion.repository.HabitacionRepository;
import com.hotelgestion.repository.PuntosFidelidadRepository;
import com.hotelgestion.repository.ResenaRepository;
import com.hotelgestion.repository.ReservaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final ClienteRepository clienteRepository;
    private final HabitacionRepository habitacionRepository;
    private final PuntosFidelidadRepository puntosFidelidadRepository;
    private final ResenaRepository resenaRepository;
    private final ClienteService clienteService;


    // Horas que tiene el cliente para pagar antes de que la reserva se
    // cancele automaticamente.
    private static final int HORAS_LIMITE_PAGO = 24;

    public List<ReservaResponseDTO> listar() {
        cancelarPendientesVencidas();
        finalizarReservasVencidas();
        return reservaRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public ReservaResponseDTO obtenerPorId(Long id) {
        cancelarPendientesVencidas();
        finalizarReservasVencidas();
        return toDTO(buscarReserva(id));
    }

    public List<ReservaResponseDTO> obtenerPorCliente(Long clienteId) {
        cancelarPendientesVencidas();
        finalizarReservasVencidas();
        return reservaRepository.findByClienteId(clienteId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // Cancela automaticamente las reservas PENDIENTES cuyo plazo de 24h
    // para pagar ya vencio, para que dejen de bloquear esas fechas.
    private void cancelarPendientesVencidas() {
        List<Reserva> vencidas = reservaRepository
                .findByEstadoAndFechaLimitePagoBefore(EstadoReserva.PENDIENTE, LocalDateTime.now());
        for (Reserva r : vencidas) {
            r.setEstado(EstadoReserva.CANCELADA);
        }
        if (!vencidas.isEmpty()) {
            reservaRepository.saveAll(vencidas);
        }
    }

    // Finaliza automaticamente las reservas EN_CURSO cuya fecha de salida
    // ya paso, para que los clientes puedan dejar su reseña sin necesidad
    // de hacer check-out manual. Esto evita inconsistencias donde un cliente
    // no puede calificar porque la reserva sigue en EN_CURSO.
    private void finalizarReservasVencidas() {
        LocalDate hoy = LocalDate.now();
        List<Reserva> vencidas = reservaRepository.findAll().stream()
                .filter(r -> r.getEstado() == EstadoReserva.EN_CURSO && r.getFechaSalida().isBefore(hoy))
                .collect(Collectors.toList());
        
        for (Reserva r : vencidas) {
            r.setEstado(EstadoReserva.FINALIZADA);
            // Nota: no se asignan puntos aqui porque se asignaron en checkOut.
            // Si no hubo check-out manual, los puntos no se dieron, lo cual es correcto
            // (se premian al cliente responsable que hace checkout, no al que desaparece).
        }
        if (!vencidas.isEmpty()) {
            reservaRepository.saveAll(vencidas);
        }
    }

    public ReservaResponseDTO crear(ReservaRequestDTO dto) {
        if (!dto.getFechaSalida().isAfter(dto.getFechaEntrada())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha de salida debe ser posterior a la fecha de entrada");
        }

        // Antes de revisar conflictos, liberamos las reservas PENDIENTES que
        // ya vencieron (nadie pago en 24h), para que no bloqueen fechas que
        // en realidad ya estan libres.
        cancelarPendientesVencidas();

        Cliente cliente = clienteRepository.findById(dto.getClienteId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));

        Habitacion habitacion = habitacionRepository.findById(dto.getHabitacionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Habitacion no encontrada"));

        // Solo bloqueamos por el estado "fisico" de la habitacion cuando
        // realmente no se puede reservar (fuera de servicio o en limpieza).
        // La disponibilidad por FECHA se valida aparte, mas abajo, contra
        // las reservas ya existentes. Asi una habitacion puede tener varias
        // reservas mientras no se crucen las fechas, en vez de quedar
        // bloqueada para siempre despues de la primera reserva.
        if (habitacion.getEstado() == EstadoHabitacion.FUERA_DE_SERVICIO
                || habitacion.getEstado() == EstadoHabitacion.EN_LIMPIEZA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La habitacion no esta disponible. Estado actual: " + habitacion.getEstado());
        }

        if (reservaRepository.existeConflictoFechas(habitacion.getId(), dto.getFechaEntrada(), dto.getFechaSalida())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La habitacion ya tiene una reserva entre esas fechas. Elige otras fechas disponibles.");
        }

        Reserva reserva = new Reserva();
        reserva.setCliente(cliente);
        reserva.setHabitacion(habitacion);
        reserva.setFechaEntrada(dto.getFechaEntrada());
        reserva.setFechaSalida(dto.getFechaSalida());
        // Nace PENDIENTE: el cliente tiene 24h para pagar (ver PagoService),
        // si no se paga en ese plazo se cancela sola.
        reserva.setEstado(EstadoReserva.PENDIENTE);
        reserva.setFechaLimitePago(LocalDateTime.now().plusHours(HORAS_LIMITE_PAGO));

        // Nota: el estado de la habitacion (DISPONIBLE/OCUPADA/...) ya NO se
        // cambia aqui. Ese estado representa la situacion actual del cuarto
        // (por ejemplo si hay un huesped alojado ahora mismo), y eso se
        // actualiza en checkIn()/checkOut(). La disponibilidad por fecha para
        // futuras reservas se sigue controlando con existeConflictoFechas.

        // Si el cliente tiene un descuento pendiente por canje de puntos
        // (ver ClienteService.canjearDescuento), se aplica aqui, una sola
        // vez, y se deja en 0 para que no se reutilice en otra reserva.
        PuntosFidelidad pf = puntosFidelidadRepository.findByClienteId(cliente.getId()).orElse(null);
        if (pf != null && pf.getDescuentoDisponible() != null && pf.getDescuentoDisponible() > 0) {
            reserva.setDescuentoAplicado(pf.getDescuentoDisponible());
            pf.setDescuentoDisponible(0.0);
            puntosFidelidadRepository.save(pf);
        }

        Reserva guardada = reservaRepository.save(reserva);

        // Nota: los puntos de fidelidad por esta reserva YA NO se dan aqui.
        // Se otorgan cuando se registra el pago (ver PagoService.crear),
        // para no premiar reservas que nunca se llegan a pagar.

        return toDTO(guardada);
    }

    public ReservaResponseDTO cancelar(Long id) {
        Reserva reserva = buscarReserva(id);

        if (reserva.getEstado() == EstadoReserva.EN_CURSO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No se puede cancelar una reserva que esta en curso");
        }
        if (reserva.getEstado() == EstadoReserva.FINALIZADA || reserva.getEstado() == EstadoReserva.CANCELADA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La reserva ya esta " + reserva.getEstado().name().toLowerCase());
        }

        // Si la reserva esta CONFIRMADA (fue pagada), revertir los puntos asignados
        if (reserva.getEstado() == EstadoReserva.CONFIRMADA) {
            // 50 puntos fueron asignados al pagar
            clienteService.restarPuntos(reserva.getCliente().getId(), 50);
        }

        reserva.setEstado(EstadoReserva.CANCELADA);

        // No hace falta tocar el estado de la habitacion: como ya no se
        // marca RESERVADA al crear la reserva, cancelarla simplemente libera
        // esas fechas (existeConflictoFechas ignora las reservas CANCELADA).

        return toDTO(reservaRepository.save(reserva));
    }


    public ReservaResponseDTO checkIn(Long id, CheckInRequestDTO dto) {
        Reserva reserva = buscarReserva(id);

        if (reserva.getEstado() != EstadoReserva.CONFIRMADA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Solo se puede hacer check-in en reservas CONFIRMADAS. Estado actual: " + reserva.getEstado());
        }

        // Verificar que el documento presentado coincida con el del cliente registrado
        String documentoRegistrado = reserva.getCliente().getDocumento();
        String documentoPresentado = dto.getDocumentoHuesped().trim();
        
        if (!documentoRegistrado.equalsIgnoreCase(documentoPresentado)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El documento presentado no coincide con el documento registrado. " +
                    "Documento esperado: " + documentoRegistrado + ", documento presentado: " + documentoPresentado);
        }

        // Como en un hotel real: no se puede hacer check-in antes del dia de
        // llegada (evita que recepcion adelante por error una estadia que
        // todavia no empieza).
        java.time.LocalDate hoy = java.time.LocalDate.now();
        if (hoy.isBefore(reserva.getFechaEntrada())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Todavia no se puede hacer check-in: la reserva empieza el " + reserva.getFechaEntrada());
        }

        reserva.setEstado(EstadoReserva.EN_CURSO);
        reserva.setFechaCheckIn(LocalDateTime.now());
        reserva.setDocumentoHuespedCheckIn(dto.getDocumentoHuesped());
        reserva.setNumAcompanantes(dto.getNumAcompanantes() == null ? 0 : dto.getNumAcompanantes());
        reserva.setObservacionesCheckIn(dto.getObservaciones());

        reserva.getHabitacion().setEstado(EstadoHabitacion.OCUPADA);
        habitacionRepository.save(reserva.getHabitacion());

        return toDTO(reservaRepository.save(reserva));
    }

    public ReservaResponseDTO checkOut(Long id, CheckOutRequestDTO dto) {
        Reserva reserva = buscarReserva(id);

        if (reserva.getEstado() != EstadoReserva.EN_CURSO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Solo se puede hacer check-out en reservas EN_CURSO. Estado actual: " + reserva.getEstado());
        }

        // Solo permitir check-out a partir del dia de salida (no antes)
        LocalDate hoy = LocalDate.now();
        if (hoy.isBefore(reserva.getFechaSalida())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No se puede hacer check-out antes de la fecha de salida. El check-out se puede hacer a partir del " + reserva.getFechaSalida());
        }

        String estadoHabCheckOut = (dto.getEstadoHabitacion() == null || dto.getEstadoHabitacion().isBlank())
                ? "OK" : dto.getEstadoHabitacion().toUpperCase();

        reserva.setEstado(EstadoReserva.FINALIZADA);
        reserva.setFechaCheckOut(LocalDateTime.now());
        reserva.setEstadoHabitacionCheckOut(estadoHabCheckOut);
        reserva.setObservacionesCheckOut(dto.getObservaciones());

        // Si recepcion reporta daños, la habitacion no vuelve a limpieza
        // normal: queda FUERA_DE_SERVICIO hasta que alguien la revise. Si
        // todo esta bien, sigue el flujo normal a PENDIENTE_LIMPIEZA.
        if ("DANOS".equals(estadoHabCheckOut)) {
            reserva.getHabitacion().setEstado(EstadoHabitacion.FUERA_DE_SERVICIO);
        } else {
            reserva.getHabitacion().setEstado(EstadoHabitacion.PENDIENTE_LIMPIEZA);
        }
        habitacionRepository.save(reserva.getHabitacion());

        // Sumar puntos adicionales por finalizar estadía
        clienteService.sumarPuntos(reserva.getCliente().getId(), 30);

        return toDTO(reservaRepository.save(reserva));
    }

    // Reservas CONFIRMADAS que llegan hoy: la lista que usa recepcion para
    // saber a quien tiene que atender, en vez de buscar entre todas.
    public List<ReservaResponseDTO> obtenerLlegadasHoy() {
        cancelarPendientesVencidas();
        finalizarReservasVencidas();
        return reservaRepository.findByEstadoAndFechaEntrada(EstadoReserva.CONFIRMADA, LocalDate.now()).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // Reservas EN_CURSO cuya salida es hoy: a quien hay que hacerle check-out.
    public List<ReservaResponseDTO> obtenerSalidasHoy() {
        cancelarPendientesVencidas();
        finalizarReservasVencidas();
        return reservaRepository.findByEstadoAndFechaSalida(EstadoReserva.EN_CURSO, LocalDate.now()).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private Reserva buscarReserva(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no encontrada"));
    }

    private ReservaResponseDTO toDTO(Reserva r) {
        ReservaResponseDTO dto = new ReservaResponseDTO();
        dto.setId(r.getId());
        dto.setClienteId(r.getCliente().getId());
        dto.setClienteNombre(r.getCliente().getNombre() + " " + r.getCliente().getApellido());
        dto.setClienteDocumento(r.getCliente().getDocumento());
        dto.setHabitacionId(r.getHabitacion().getId());
        dto.setHabitacionNumero(r.getHabitacion().getNumero());
        dto.setHabitacionTipo(r.getHabitacion().getTipo());
        dto.setPrecioNoche(r.getHabitacion().getPrecioNoche());
        dto.setFechaEntrada(r.getFechaEntrada());
        dto.setFechaSalida(r.getFechaSalida());
        dto.setEstado(r.getEstado());
        dto.setFechaCreacion(r.getFechaCreacion());
        dto.setFechaLimitePago(r.getFechaLimitePago());

        long noches = ChronoUnit.DAYS.between(r.getFechaEntrada(), r.getFechaSalida());
        BigDecimal totalEstimado = r.getHabitacion().getPrecioNoche().multiply(BigDecimal.valueOf(noches));
        dto.setTotalEstimado(totalEstimado);

        double descuento = r.getDescuentoAplicado() == null ? 0.0 : r.getDescuentoAplicado();
        dto.setDescuentoAplicado(descuento);
        if (descuento > 0) {
            BigDecimal factor = BigDecimal.valueOf(1 - descuento);
            dto.setTotalConDescuento(totalEstimado.multiply(factor));
        } else {
            dto.setTotalConDescuento(totalEstimado);
        }

        // Le dice al frontend si esta reserva ya tiene una reseña, para que
        // muestre "Ya dejaste tu reseña" en vez del boton de nuevo.
        dto.setTieneResena(resenaRepository.countByReservaId(r.getId()) > 0);

        dto.setFechaCheckIn(r.getFechaCheckIn());
        dto.setDocumentoHuespedCheckIn(r.getDocumentoHuespedCheckIn());
        dto.setNumAcompanantes(r.getNumAcompanantes());
        dto.setObservacionesCheckIn(r.getObservacionesCheckIn());
        dto.setFechaCheckOut(r.getFechaCheckOut());
        dto.setEstadoHabitacionCheckOut(r.getEstadoHabitacionCheckOut());
        dto.setObservacionesCheckOut(r.getObservacionesCheckOut());

        return dto;
    }
}
