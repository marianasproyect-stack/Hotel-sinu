package com.hotelgestion.service;

import com.hotelgestion.dto.CanjeDescuentoResponseDTO;
import com.hotelgestion.dto.NivelCanjeDTO;
import com.hotelgestion.dto.PuntosNivelesResponseDTO;
import com.hotelgestion.model.Cliente;
import com.hotelgestion.model.PuntosFidelidad;
import com.hotelgestion.repository.ClienteRepository;
import com.hotelgestion.repository.PuntosFidelidadRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final PuntosFidelidadRepository puntosFidelidadRepository;

    public List<Cliente> listar() {
        return clienteRepository.findAll();
    }

    public Cliente obtenerPorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
    }

    public Cliente crear(@Valid Cliente cliente) {
        if (clienteRepository.findByCorreo(cliente.getCorreo()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un cliente con ese correo");
        }
        if (cliente.getDocumento() != null && clienteRepository.findByDocumento(cliente.getDocumento()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un cliente con ese documento");
        }
        Cliente guardado = clienteRepository.save(cliente);

        // Inicializar puntos de fidelidad
        PuntosFidelidad puntos = new PuntosFidelidad();
        puntos.setCliente(guardado);
        puntos.setPuntosTotales(0);
        puntos.setPuntosCanjeados(0);
        puntos.setCategoria("ESTANDAR");
        puntosFidelidadRepository.save(puntos);

        return guardado;
    }

    public Cliente actualizar(Long id, @Valid Cliente datos) {
        Cliente cliente = obtenerPorId(id);

        // Si cambia el correo, verificar que no exista
        if (!cliente.getCorreo().equals(datos.getCorreo()) &&
                clienteRepository.findByCorreo(datos.getCorreo()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un cliente con ese correo");
        }

        cliente.setNombre(datos.getNombre());
        cliente.setApellido(datos.getApellido());
        cliente.setCorreo(datos.getCorreo());
        cliente.setTelefono(datos.getTelefono());
        cliente.setDocumento(datos.getDocumento());
        return clienteRepository.save(cliente);
    }

    public void eliminar(Long id) {
        if (!clienteRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado");
        }
        clienteRepository.deleteById(id);
    }

    public PuntosFidelidad obtenerPuntos(Long clienteId) {
        obtenerPorId(clienteId); // valida que el cliente existe
        return puntosFidelidadRepository.findByClienteId(clienteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro de puntos no encontrado"));
    }

    public PuntosFidelidad sumarPuntos(Long clienteId, int puntos) {
        PuntosFidelidad pf = obtenerPuntos(clienteId);
        pf.setPuntosTotales(pf.getPuntosTotales() + puntos);
        actualizarCategoria(pf);
        return puntosFidelidadRepository.save(pf);
    }

    public PuntosFidelidad restarPuntos(Long clienteId, int puntos) {
        PuntosFidelidad pf = obtenerPuntos(clienteId);
        int nuevoTotal = pf.getPuntosTotales() - puntos;
        if (nuevoTotal < 0) {
            nuevoTotal = 0;
        }
        pf.setPuntosTotales(nuevoTotal);
        actualizarCategoria(pf);
        return puntosFidelidadRepository.save(pf);
    }

    public PuntosFidelidad canjearPuntos(Long clienteId, int puntos) {
        PuntosFidelidad pf = obtenerPuntos(clienteId);
        int disponibles = pf.getPuntosTotales() - pf.getPuntosCanjeados();
        if (puntos > disponibles) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No tiene suficientes puntos disponibles. Disponibles: " + disponibles);
        }
        pf.setPuntosCanjeados(pf.getPuntosCanjeados() + puntos);
        return puntosFidelidadRepository.save(pf);
    }

    // ================= CANJE DE PUNTOS POR DESCUENTO =================

    // Niveles fijos de canje. Cada uno cuesta cierta cantidad de puntos y
    // otorga un descuento (en fraccion) que se aplica en la siguiente
    // reserva del cliente.
    private static final int[] NIVEL_PUNTOS = {100, 200, 500};
    private static final double[] NIVEL_DESCUENTO = {0.05, 0.10, 0.20};
    private static final String[] NIVEL_NOMBRE = {"Descuento Bronce", "Descuento Plata", "Descuento Oro"};

    // Dias que hay que esperar entre un canje y el siguiente.
    private static final int DIAS_COOLDOWN_CANJE = 7;

    public PuntosNivelesResponseDTO obtenerNiveles(Long clienteId) {
        PuntosFidelidad pf = obtenerPuntos(clienteId);
        int disponibles = pf.getPuntosTotales() - pf.getPuntosCanjeados();

        String proximoCanje = "Disponible ahora";
        if (pf.getFechaUltimoCanje() != null) {
            LocalDate desbloqueaEl = pf.getFechaUltimoCanje().plusDays(DIAS_COOLDOWN_CANJE);
            if (desbloqueaEl.isAfter(LocalDate.now())) {
                proximoCanje = desbloqueaEl.format(DateTimeFormatter.ISO_LOCAL_DATE);
            }
        }

        List<NivelCanjeDTO> niveles = new ArrayList<>();
        for (int i = 0; i < NIVEL_PUNTOS.length; i++) {
            niveles.add(new NivelCanjeDTO(
                    i + 1,
                    NIVEL_NOMBRE[i],
                    Math.round(NIVEL_DESCUENTO[i] * 100) + "%",
                    NIVEL_PUNTOS[i],
                    disponibles >= NIVEL_PUNTOS[i]
            ));
        }

        return new PuntosNivelesResponseDTO(disponibles, proximoCanje, niveles);
    }

    public CanjeDescuentoResponseDTO canjearDescuento(Long clienteId, int nivel) {
        if (nivel < 1 || nivel > NIVEL_PUNTOS.length) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nivel de canje invalido");
        }

        PuntosFidelidad pf = obtenerPuntos(clienteId);

        if (pf.getFechaUltimoCanje() != null) {
            LocalDate desbloqueaEl = pf.getFechaUltimoCanje().plusDays(DIAS_COOLDOWN_CANJE);
            if (desbloqueaEl.isAfter(LocalDate.now())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Todavia estas en tiempo de espera. Podras volver a canjear el "
                                + desbloqueaEl.format(DateTimeFormatter.ISO_LOCAL_DATE));
            }
        }

        int costo = NIVEL_PUNTOS[nivel - 1];
        double descuento = NIVEL_DESCUENTO[nivel - 1];
        int disponibles = pf.getPuntosTotales() - pf.getPuntosCanjeados();

        if (disponibles < costo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No tienes suficientes puntos para este nivel. Disponibles: " + disponibles);
        }

        pf.setPuntosCanjeados(pf.getPuntosCanjeados() + costo);
        pf.setDescuentoDisponible(descuento);
        pf.setFechaUltimoCanje(LocalDate.now());
        puntosFidelidadRepository.save(pf);

        int puntosRestantes = pf.getPuntosTotales() - pf.getPuntosCanjeados();
        return new CanjeDescuentoResponseDTO(Math.round(descuento * 100) + "%", puntosRestantes);
    }

    private void actualizarCategoria(PuntosFidelidad pf) {
        int total = pf.getPuntosTotales();
        if (total >= 1000) {
            pf.setCategoria("PLATINUM");
        } else if (total >= 500) {
            pf.setCategoria("ORO");
        } else if (total >= 200) {
            pf.setCategoria("PLATA");
        } else {
            pf.setCategoria("ESTANDAR");
        }
    }
}
