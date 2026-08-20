package com.hotelgestion.service;

import com.hotelgestion.enums.EstadoHabitacion;
import com.hotelgestion.model.Habitacion;
import com.hotelgestion.repository.HabitacionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class HabitacionService {

    private final HabitacionRepository habitacionRepository;

    public List<Habitacion> listar(EstadoHabitacion estado) {
        if (estado != null) {
            return habitacionRepository.findByEstado(estado);
        }
        return habitacionRepository.findAll();
    }

    public Habitacion obtenerPorId(Long id) {
        return habitacionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Habitacion no encontrada"));
    }

    public Habitacion crear(Habitacion habitacion) {
        if (habitacionRepository.findByNumero(habitacion.getNumero()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una habitacion con ese numero");
        }
        return habitacionRepository.save(habitacion);
    }

    public Habitacion actualizar(Long id, Habitacion datos) {
        Habitacion habitacion = obtenerPorId(id);

        if (!habitacion.getNumero().equals(datos.getNumero()) &&
                habitacionRepository.findByNumero(datos.getNumero()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una habitacion con ese numero");
        }

        habitacion.setNumero(datos.getNumero());
        habitacion.setTipo(datos.getTipo());
        habitacion.setCapacidad(datos.getCapacidad());
        habitacion.setPrecioNoche(datos.getPrecioNoche());
        habitacion.setDescripcion(datos.getDescripcion());
        habitacion.setEstado(datos.getEstado());
        return habitacionRepository.save(habitacion);
    }

    public Habitacion cambiarEstado(Long id, EstadoHabitacion nuevoEstado) {
        Habitacion habitacion = obtenerPorId(id);
        habitacion.setEstado(nuevoEstado);
        return habitacionRepository.save(habitacion);
    }

    public void eliminar(Long id) {
        if (!habitacionRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Habitacion no encontrada");
        }
        habitacionRepository.deleteById(id);
    }
}
