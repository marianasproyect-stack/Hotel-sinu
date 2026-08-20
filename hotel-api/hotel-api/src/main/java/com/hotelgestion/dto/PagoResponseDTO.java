package com.hotelgestion.dto;

import com.hotelgestion.enums.EstadoPago;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class PagoResponseDTO {

    private Long id;
    private Long reservaId;
    private BigDecimal monto;
    private String metodoPago;
    private EstadoPago estado;
    private LocalDate fechaPago;
}
