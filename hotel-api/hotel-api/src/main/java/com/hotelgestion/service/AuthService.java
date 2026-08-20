package com.hotelgestion.service;

import com.hotelgestion.dto.LoginRequestDTO;
import com.hotelgestion.dto.RegistroRequestDTO;
import com.hotelgestion.dto.UsuarioResponseDTO;
import com.hotelgestion.model.Cliente;
import com.hotelgestion.model.PuntosFidelidad;
import com.hotelgestion.model.Rol;
import com.hotelgestion.model.Usuario;
import com.hotelgestion.repository.RolRepository;
import com.hotelgestion.repository.UsuarioRepository;
import com.hotelgestion.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final ClienteService clienteService;

    public UsuarioResponseDTO registrar(RegistroRequestDTO dto) {
        String correo = dto.getCorreo().trim().toLowerCase();

        if (usuarioRepository.findByCorreo(correo).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una cuenta con ese correo");
        }

        if (usuarioRepository.findByNombre(dto.getNombre()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe una cuenta con ese nombre. Como el nombre se usa para iniciar sesion, debe ser unico");
        }

        // El registro publico SOLO puede crear cuentas CLIENTE. No se acepta
        // el rol que venga en el body (aunque alguien intente forzar
        // "ADMINISTRADOR" llamando la API directamente, se ignora). Las
        // cuentas de administrador no se crean por este endpoint: se
        // gestionan aparte (por ejemplo directamente en la base de datos),
        // por seguridad.
        String nombreRol = "CLIENTE";

        // apellido/telefono/documento son obligatorios porque la tabla
        // CLIENTES los exige (igual que en la base de datos real).
        if (dto.getApellido() == null || dto.getApellido().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El apellido es obligatorio");
        }
        if (dto.getTelefono() == null || dto.getTelefono().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El telefono es obligatorio");
        }
        if (dto.getDocumento() == null || dto.getDocumento().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El documento es obligatorio");
        }

        Rol rol = rolRepository.findByNombre(nombreRol)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "El rol " + nombreRol + " no esta configurado en la base de datos"));

        Usuario usuario = new Usuario();
        usuario.setNombre(dto.getNombre());
        usuario.setCorreo(correo);
        usuario.setContrasena(PasswordUtil.hash(dto.getContrasena()));
        usuario.setRol(rol);

        Integer puntos = null;

        // Si se registra como CLIENTE, tambien creamos su fila en CLIENTES
        // para poder usar todo el modulo de reservas/pagos/puntos que ya existe.
        if (nombreRol.equals("CLIENTE")) {
            Cliente cliente = new Cliente();
            cliente.setNombre(dto.getNombre());
            cliente.setApellido(dto.getApellido());
            cliente.setCorreo(correo);
            cliente.setTelefono(dto.getTelefono());
            cliente.setDocumento(dto.getDocumento());
            Cliente clienteCreado = clienteService.crear(cliente);
            usuario.setClienteId(clienteCreado.getId());
            puntos = 0;
        }

        Usuario guardado = usuarioRepository.save(usuario);

        return toDTO(guardado, puntos);
    }

    public UsuarioResponseDTO login(LoginRequestDTO dto) {
        String nombre = dto.getNombre().trim();

        Usuario usuario = usuarioRepository.findByNombre(nombre)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Nombre o contrasena incorrectos"));

        if (!PasswordUtil.matches(dto.getContrasena(), usuario.getContrasena())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Nombre o contrasena incorrectos");
        }

        Integer puntos = null;
        if (usuario.getClienteId() != null) {
            try {
                PuntosFidelidad pf = clienteService.obtenerPuntos(usuario.getClienteId());
                puntos = pf.getPuntosTotales() - pf.getPuntosCanjeados();
            } catch (ResponseStatusException e) {
                puntos = 0;
            }
        }

        return toDTO(usuario, puntos);
    }

    private UsuarioResponseDTO toDTO(Usuario usuario, Integer puntos) {
        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getCorreo(),
                usuario.getRol().getNombre(),
                usuario.getClienteId(),
                puntos
        );
    }
}
