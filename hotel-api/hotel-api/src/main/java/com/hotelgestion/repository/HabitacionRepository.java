package com.hotelgestion.repository;

import com.hotelgestion.enums.EstadoHabitacion;
import com.hotelgestion.model.Habitacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HabitacionRepository extends JpaRepository<Habitacion, Long> {

    Optional<Habitacion> findByNumero(String numero);

    List<Habitacion> findByEstado(EstadoHabitacion estado);
}
