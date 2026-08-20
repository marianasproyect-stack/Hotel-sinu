package com.hotelgestion.controller;

import com.hotelgestion.dto.LoginRequestDTO;
import com.hotelgestion.dto.RegistroRequestDTO;
import com.hotelgestion.dto.UsuarioResponseDTO;
import com.hotelgestion.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // POST /api/auth/registro
    // Este endpoint SOLO crea cuentas de tipo CLIENTE (por seguridad, el
    // registro publico no permite crear administradores).
    // Body: { "nombre": "...", "apellido": "...", "correo": "...", "telefono": "...",
    //         "documento": "...", "contrasena": "..." }
    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponseDTO> registrar(@Valid @RequestBody RegistroRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(dto));
    }

    // POST /api/auth/login
    // Body: { "nombre": "...", "contrasena": "..." }
    @PostMapping("/login")
    public ResponseEntity<UsuarioResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        return ResponseEntity.ok(authService.login(dto));
    }
}
