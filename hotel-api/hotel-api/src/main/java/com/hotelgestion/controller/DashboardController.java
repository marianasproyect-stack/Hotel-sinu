package com.hotelgestion.controller;

import com.hotelgestion.dto.DashboardDTO;
import com.hotelgestion.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    // GET /api/dashboard
    @GetMapping
    public ResponseEntity<DashboardDTO> resumen() {
        return ResponseEntity.ok(dashboardService.obtenerResumen());
    }
}
