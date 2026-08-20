
const API = "https://hotel-sinu.onrender.com/api";

let usuarioActual = (() => {
  const guardado = localStorage.getItem("usuarioActual");
  try {
    return guardado ? JSON.parse(guardado) : null;
  } catch {
    return null;
  }
})();

// habitacion que se esta reservando en el modal
let habitacionSeleccionada = null;

function normalizarUsuario(usuario) {
  if (!usuario) return null;

  const clienteId = usuario.clienteId ?? usuario.id ?? usuario.cliente?.id ?? usuario.cliente_id;
  return {
    ...usuario,
    clienteId,
    puntos: Number(usuario.puntos ?? usuario.puntosTotales ?? usuario.puntosDisponibles ?? 0)
  };
}

const secciones = {
  login: document.getElementById("login-section"),
  register: document.getElementById("register-section"),
  cliente: document.getElementById("cliente-section"),
  admin: document.getElementById("admin-section")
};

function mostrarSeccion(nombre) {
  Object.keys(secciones).forEach(key => {
    secciones[key].style.display = key === nombre ? (key === "login" || key === "register" ? "flex" : "block") : "none";
  });
}

function iniciarVista() {
  if (!usuarioActual) {
    mostrarSeccion("login");
  } else if (usuarioActual.rol === "ADMINISTRADOR") {
    mostrarSeccion("admin");
    document.getElementById("admin-nombre").textContent = usuarioActual.nombre;
    mostrarMenuAdmin();
  } else {
    mostrarSeccion("cliente");
    document.getElementById("cliente-nombre").textContent = usuarioActual.nombre;
    document.getElementById("cliente-puntos").textContent = (usuarioActual.puntos || 0) + " puntos";
    document.getElementById("bienvenida-titulo").textContent = `¡Bienvenido, ${usuarioActual.nombre}!`;
    mostrarBienvenida();
    refrescarPuntosCliente();
  }
  // Refresca los puntos del cliente desde la API al entrar al panel
async function refrescarPuntosCliente() {
  if (!usuarioActual || !usuarioActual.clienteId) return;
  try {
    const resp = await fetch(`${API}/clientes/${usuarioActual.clienteId}/puntos`);
    if (resp.ok) {
      const pf = await resp.json();
      const disponibles = pf.puntosTotales - pf.puntosCanjeados;
      usuarioActual.puntos = disponibles;
      guardarSesion(usuarioActual);
      document.getElementById("cliente-puntos").textContent = disponibles + " puntos";
    }
  } catch (e) {
    // Si falla, se mantienen los puntos del localStorage
  }
}

}

function guardarSesion(usuario) {
  usuarioActual = normalizarUsuario(usuario);
  localStorage.setItem("usuarioActual", JSON.stringify(usuarioActual));
}

function cerrarSesion() {
  usuarioActual = null;
  localStorage.removeItem("usuarioActual");
  mostrarSeccion("login");
}

/* ================= LOGIN ================= */

document.getElementById("link-registro").addEventListener("click", (e) => {
  e.preventDefault();
  document.getElementById("register-error").textContent = "";
  mostrarSeccion("register");
});

document.getElementById("link-login").addEventListener("click", (e) => {
  e.preventDefault();
  document.getElementById("login-error").textContent = "";
  mostrarSeccion("login");
});

document.getElementById("login-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  const nombre = document.getElementById("login-nombre").value;
  const contrasena = document.getElementById("login-password").value;
  const errorEl = document.getElementById("login-error");
  errorEl.textContent = "";

  try {
    const resp = await fetch(`${API}/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ nombre, contrasena })
    });

    if (!resp.ok) {
      const err = await resp.json().catch(() => ({}));
      errorEl.textContent = err.mensaje || "Nombre o contraseña incorrectos";
      return;
    }

    const usuario = await resp.json();
    guardarSesion(usuario);
    iniciarVista();
  } catch (err) {
    errorEl.textContent = "No se pudo conectar con el servidor. Revisa que la API esté corriendo.";
  }
});

/* ================= REGISTRO ================= */

// El registro público solo crea cuentas de CLIENTE. Las cuentas de
// administrador no se pueden crear desde este formulario por seguridad;
// se gestionan aparte (por ejemplo directamente en la base de datos).
const inputApellido = document.getElementById("reg-apellido");
const inputTelefono = document.getElementById("reg-telefono");
const inputDocumento = document.getElementById("reg-documento");

document.getElementById("register-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  const nombre = document.getElementById("reg-nombre").value;
  const apellido = inputApellido.value;
  const telefono = inputTelefono.value;
  const documento = inputDocumento.value;
  const correo = document.getElementById("reg-correo").value;
  const contrasena = document.getElementById("reg-password").value;
  const errorEl = document.getElementById("register-error");
  errorEl.textContent = "";

  try {
    const resp = await fetch(`${API}/auth/registro`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ nombre, apellido, telefono, documento, correo, contrasena, rol: "CLIENTE" })
    });

    if (!resp.ok) {
      const err = await resp.json().catch(() => ({}));
      if (err.detalle) {
        errorEl.textContent = Object.values(err.detalle).join(" · ");
      } else {
        errorEl.textContent = err.mensaje || "No se pudo crear la cuenta";
      }
      return;
    }

    const usuario = await resp.json();
    guardarSesion(usuario);
    iniciarVista();
  } catch (err) {
    errorEl.textContent = "No se pudo conectar con el servidor. Revisa que la API esté corriendo.";
  }
});

/* ================= CERRAR SESION ================= */

document.getElementById("btn-logout-cliente").addEventListener("click", cerrarSesion);
document.getElementById("btn-logout-admin").addEventListener("click", cerrarSesion);

/* ================= BIENVENIDA / HABITACIONES (CLIENTE) ================= */

function mostrarBienvenida() {
  document.getElementById("bienvenida-section").style.display = "block";
  document.getElementById("habitaciones-section").style.display = "none";
}

document.getElementById("btn-ver-habitaciones").addEventListener("click", () => {
  document.getElementById("bienvenida-section").style.display = "none";
  document.getElementById("habitaciones-section").style.display = "block";
  cargarHabitaciones();
});

document.getElementById("btn-volver-bienvenida").addEventListener("click", mostrarBienvenida);

async function cargarHabitaciones() {
  const msgEl = document.getElementById("habitaciones-msg");
  const listEl = document.getElementById("habitaciones-list");
  msgEl.textContent = "Cargando habitaciones...";
  listEl.innerHTML = "";
  listEl.classList.add("rooms-groups");

  try {
    const resp = await fetch(`${API}/habitaciones`);
    const habitaciones = await resp.json();

    if (!habitaciones.length) {
      msgEl.textContent = "Todavía no hay habitaciones cargadas en la base de datos.";
      return;
    }

    msgEl.textContent = "";
    agruparPorTipo(habitaciones).forEach(grupo => listEl.appendChild(crearGrupoHabitaciones(grupo)));
  } catch (err) {
    msgEl.textContent = "No se pudieron cargar las habitaciones.";
  }
}

/* Fotos reales por tipo de habitación. Si un tipo no tiene fotos aquí,
   se usa automáticamente una galería genérica (una foto representativa
   de cada categoría) como respaldo, para que ninguna tarjeta se quede
   sin imagen. */
const FOTOS_HABITACION = {
  "sencilla": [
    "img/habitaciones/sencilla/sencilla-1.jpg",
    "img/habitaciones/sencilla/sencilla-2.jpg"
  ],
  "doble": [
    "img/habitaciones/doble/doble-1.jpg",
    "img/habitaciones/doble/doble-2.jpg",
    "img/habitaciones/doble/doble-3.jpg"
  ],
  "suite": [
    "img/habitaciones/suite/suite-1.jpg",
    "img/habitaciones/suite/suite-2.jpg",
    "img/habitaciones/suite/suite-3.jpg"
  ],
  "suite presidencial": [
    "img/habitaciones/suite-presidencial/sp-1.jpg",
    "img/habitaciones/suite-presidencial/sp-2.jpg",
    "img/habitaciones/suite-presidencial/sp-3.jpg",
    "img/habitaciones/suite-presidencial/sp-4.jpg"
  ]
};

/* Galería genérica de respaldo: se arma con una foto de cada categoría
   real del hotel, para que un tipo de habitación nuevo (creado desde el
   panel admin) que todavía no tiene fotos propias también se vea bien,
   en lugar de mostrar solo el ícono ilustrado. */
const FOTOS_GENERICAS = [
  "img/habitaciones/sencilla/sencilla-1.jpg",
  "img/habitaciones/doble/doble-1.jpg",
  "img/habitaciones/suite/suite-1.jpg",
  "img/habitaciones/suite-presidencial/sp-1.jpg"
];

function obtenerFotosHabitacion(tipo) {
  const clave = (tipo || "").toLowerCase().trim();
  return FOTOS_HABITACION[clave] || FOTOS_GENERICAS;
}

/* Orden lógico de los tipos: de la más sencilla a la más lujosa. Los
   tipos que no estén en esta lista (creados nuevos desde el panel admin)
   se muestran al final, ordenados alfabéticamente. */
const ORDEN_TIPOS_HABITACION = ["sencilla", "doble", "triple", "suite", "suite presidencial"];

/* Presentación visual de cada tipo: color de acento, ícono y una frase
   corta que ayuda a diferenciar las categorías de un vistazo. */
const ESTILO_TIPO_HABITACION = {
  "sencilla": {
    color: "#1a75c4",
    claro: "#eaf3fc",
    plural: "Habitaciones Sencillas",
    frase: "Cómodas y prácticas, ideales para viajar solo o en pareja.",
    icono: '<path d="M3 20v-7a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v7"/><path d="M3 20v1"/><path d="M21 20v1"/><path d="M5 11V8a2 2 0 0 1 2-2h3v5"/><path d="M3 15h18"/>'
  },
  "doble": {
    color: "#2fae66",
    claro: "#e6f6ee",
    plural: "Habitaciones Dobles",
    frase: "Más espacio y dos camas, perfectas para grupos pequeños.",
    icono: '<path d="M2 20v-7a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v7"/><path d="M2 20v1"/><path d="M22 20v1"/><path d="M4 11V8a2 2 0 0 1 2-2h2.5v5"/><path d="M13.5 11V8a2 2 0 0 1 2-2H18a2 2 0 0 1 2 2v3"/><path d="M2 15h20"/>'
  },
  "triple": {
    color: "#c46a1a",
    claro: "#fbf0e6",
    plural: "Habitaciones Triples",
    frase: "Tres camas y espacio extra, ideales para familias o grupos.",
    icono: '<path d="M2 20v-7a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v7"/><path d="M2 20v1"/><path d="M22 20v1"/><path d="M4 11V8a2 2 0 0 1 2-2h2.3v5"/><path d="M10.8 11V8a2 2 0 0 1 2-2h2.3v5"/><path d="M17.6 11V8a2 2 0 0 1 2-2H20a2 2 0 0 1 2 2v3"/><path d="M2 15h20"/>'
  },
  "suite": {
    color: "#7a4fc4",
    claro: "#f1ebfb",
    plural: "Suites",
    frase: "Sala independiente y detalles extra para una estadía superior.",
    icono: '<path d="M3 20v-7a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v7"/><path d="M3 20v1"/><path d="M21 20v1"/><path d="M5 11V8a2 2 0 0 1 2-2h3v5"/><path d="M3 15h18"/><path d="M17 6l1.5-2L20 6"/>'
  },
  "suite presidencial": {
    color: "#b8860b",
    claro: "#faf1dc",
    plural: "Suites Presidenciales",
    frase: "Lo más exclusivo del hotel: máximo lujo, espacio y vista.",
    icono: '<path d="M3 20v-7a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v7"/><path d="M3 20v1"/><path d="M21 20v1"/><path d="M5 11V8a2 2 0 0 1 2-2h3v5"/><path d="M3 15h18"/><path d="M12 3l1.2 2.5L16 6l-2 2 .5 2.7L12 9.4 9.5 10.7 10 8l-2-2 2.8-.5z"/>'
  }
};

function estiloTipoHabitacion(tipo) {
  const clave = (tipo || "").toLowerCase().trim();
  return ESTILO_TIPO_HABITACION[clave] || {
    color: "#5a6b7d",
    claro: "#eef1f4",
    plural: tipo || "Otras habitaciones",
    frase: "Otra categoría disponible en el hotel.",
    icono: '<path d="M3 20v-7a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v7"/><path d="M3 20v1"/><path d="M21 20v1"/><path d="M5 11V8a2 2 0 0 1 2-2h3v5"/><path d="M3 15h18"/>'
  };
}

/* Agrupa las habitaciones por tipo (respetando el orden lógico de
   categorías) y ordena cada grupo por número de habitación. */
/* Ordena una lista plana de habitaciones por tipo (categoría) y luego
   por número. La usa la tabla del panel de administrador, que muestra
   todas las habitaciones en una sola lista (no agrupada). */
function ordenarHabitaciones(lista) {
  return [...lista].sort((a, b) => {
    const tipoA = (a.tipo || "").toLowerCase().trim();
    const tipoB = (b.tipo || "").toLowerCase().trim();
    let posA = ORDEN_TIPOS_HABITACION.indexOf(tipoA);
    let posB = ORDEN_TIPOS_HABITACION.indexOf(tipoB);
    if (posA === -1) posA = ORDEN_TIPOS_HABITACION.length;
    if (posB === -1) posB = ORDEN_TIPOS_HABITACION.length;

    if (posA !== posB) return posA - posB;
    if (posA === ORDEN_TIPOS_HABITACION.length && tipoA !== tipoB) {
      return tipoA.localeCompare(tipoB);
    }

    const numA = Number(a.numero);
    const numB = Number(b.numero);
    if (!isNaN(numA) && !isNaN(numB)) return numA - numB;
    return String(a.numero).localeCompare(String(b.numero));
  });
}

/* Agrupa las habitaciones por tipo (respetando el orden lógico de
   categorías) y ordena cada grupo por número de habitación. Se usa en
   el panel del cliente para mostrarlas separadas por categoría. */
function agruparPorTipo(lista) {
  const mapa = new Map();

  lista.forEach(h => {
    const clave = (h.tipo || "").toLowerCase().trim();
    if (!mapa.has(clave)) mapa.set(clave, { tipo: h.tipo, habitaciones: [] });
    mapa.get(clave).habitaciones.push(h);
  });

  const claves = [...mapa.keys()].sort((claveA, claveB) => {
    let posA = ORDEN_TIPOS_HABITACION.indexOf(claveA);
    let posB = ORDEN_TIPOS_HABITACION.indexOf(claveB);
    if (posA === -1) posA = ORDEN_TIPOS_HABITACION.length;
    if (posB === -1) posB = ORDEN_TIPOS_HABITACION.length;
    if (posA !== posB) return posA - posB;
    return claveA.localeCompare(claveB);
  });

  return claves.map(clave => {
    const grupo = mapa.get(clave);
    grupo.habitaciones.sort((a, b) => {
      const numA = Number(a.numero);
      const numB = Number(b.numero);
      if (!isNaN(numA) && !isNaN(numB)) return numA - numB;
      return String(a.numero).localeCompare(String(b.numero));
    });
    return grupo;
  });
}

function crearGrupoHabitaciones(grupo) {
  const estilo = estiloTipoHabitacion(grupo.tipo);
  const disponibles = grupo.habitaciones.filter(h => h.estado === "DISPONIBLE").length;
  const precios = grupo.habitaciones.map(h => Number(h.precioNoche)).filter(p => !isNaN(p));
  const desde = precios.length ? Math.min(...precios).toLocaleString("es-CO") : null;

  const contenedor = document.createElement("div");
  contenedor.className = "rooms-type-group";
  contenedor.style.setProperty("--tipo-color", estilo.color);
  contenedor.style.setProperty("--tipo-color-claro", estilo.claro);

  contenedor.innerHTML = `
    <div class="rooms-type-header">
      <div class="rooms-type-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">${estilo.icono}</svg>
      </div>
      <div class="rooms-type-info">
        <h3>${estilo.plural}</h3>
        <p>${estilo.frase}</p>
      </div>
      <div class="rooms-type-meta">
        ${desde ? `<span class="rooms-type-desde">Desde $${desde} / noche</span>` : ""}
        <span class="rooms-type-badge">${disponibles} de ${grupo.habitaciones.length} disponibles</span>
      </div>
    </div>
    <div class="rooms-grid"></div>
  `;

  const grid = contenedor.querySelector(".rooms-grid");
  grupo.habitaciones.forEach((h, indice) => grid.appendChild(crearTarjetaHabitacion(h, indice)));

  return contenedor;
}

function crearTarjetaHabitacion(h, indiceEnGrupo = 0) {
  const div = document.createElement("div");
  div.className = "room-card";

  const disponible = h.estado === "DISPONIBLE";
  const precio = Number(h.precioNoche).toLocaleString("es-CO");
  const fotos = obtenerFotosHabitacion(h.tipo);
  // Cada habitación del mismo tipo arranca en una foto distinta de su
  // propia galería, para que las tarjetas no luzcan todas idénticas.
  const indiceInicial = fotos.length ? indiceEnGrupo % fotos.length : 0;

  div.innerHTML = `
    <div class="room-card-imagen">${crearGaleriaHabitacion(fotos, h.tipo, indiceInicial)}</div>
    <div class="room-card-body">
      <div class="room-card-titulo">
        <h3>Habitación ${h.numero}</h3>
        <span class="room-card-estado-punto ${disponible ? "disponible" : ""}" title="${h.estado}"></span>
      </div>
      <p class="precio">$${precio} / noche</p>
      <p class="descripcion">Capacidad: ${h.capacidad} personas</p>
      <p class="descripcion">${h.descripcion || ""}</p>
      <p class="estado ${disponible ? "disponible" : ""}">${disponible ? "Disponible" : h.estado}</p>
      <button>Reservar</button>
    </div>
  `;

  // El botón siempre está habilitado. El calendario mostrará qué fechas
  // están disponibles (ocupadas = grises, disponibles = clickeables)
  div.querySelector(".room-card-body button").addEventListener("click", () => abrirModalReserva(h));

  inicializarGaleria(div.querySelector(".room-gallery"));

  return div;
}

/* ================= GALERIA DE FOTOS DE HABITACION ================= */

function crearGaleriaHabitacion(fotos, tipo, indiceInicial = 0) {
  const slides = fotos.map((src, i) => `
    <img src="${src}" alt="Habitación ${tipo}" class="room-gallery-img" data-index="${i}" ${i === indiceInicial ? "" : 'style="display:none"'}>
  `).join("");

  const puntos = fotos.map((_, i) => `
    <span class="room-gallery-dot ${i === indiceInicial ? "activo" : ""}" data-index="${i}"></span>
  `).join("");

  return `
    <div class="room-gallery" data-actual="${indiceInicial}">
      ${slides}
      <button type="button" class="room-gallery-flecha izq" aria-label="Foto anterior">&#10094;</button>
      <button type="button" class="room-gallery-flecha der" aria-label="Foto siguiente">&#10095;</button>
      <div class="room-gallery-dots">${puntos}</div>
    </div>
  `;
}

function inicializarGaleria(galeria) {
  if (!galeria) return;
  const imgs = galeria.querySelectorAll(".room-gallery-img");
  const dots = galeria.querySelectorAll(".room-gallery-dot");
  const total = imgs.length;

  function mostrar(indice) {
    const nuevo = (indice + total) % total;
    imgs.forEach((img, i) => (img.style.display = i === nuevo ? "block" : "none"));
    dots.forEach((d, i) => d.classList.toggle("activo", i === nuevo));
    galeria.dataset.actual = nuevo;
  }

  galeria.querySelector(".izq").addEventListener("click", (e) => {
    e.stopPropagation();
    mostrar(Number(galeria.dataset.actual) - 1);
  });
  galeria.querySelector(".der").addEventListener("click", (e) => {
    e.stopPropagation();
    mostrar(Number(galeria.dataset.actual) + 1);
  });
  dots.forEach((dot) => {
    dot.addEventListener("click", (e) => {
      e.stopPropagation();
      mostrar(Number(dot.dataset.index));
    });
  });
}


/* ================================================
   CÓDIGO MEJORADO PARA REEMPLAZAR EN TU APP.JS
   
   BUSCA la sección "// ================= RESERVA =================" 
   y REEMPLÁZALA con este código
   ================================================ */

// ================= SELECTOR DE FECHA PERSONALIZADO =================
// Sustituye a los <input type="date"> nativos: el input nativo solo
// permite deshabilitar un rango continuo (min/max), pero no fechas
// sueltas. Con este calendario propio podemos pintar en gris tanto
// los dias pasados como los dias que ya tienen una reserva.

function fechaAISO(date) {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

function isoAFecha(iso) {
  const [y, m, d] = iso.split("-").map(Number);
  return new Date(y, m - 1, d);
}

function formatearFechaVisible(iso) {
  if (!iso) return "";
  const [y, m, d] = iso.split("-");
  return `${d}/${m}/${y}`;
}

function crearSelectorFecha(inputEl, opciones = {}) {
  const contenedor = inputEl.closest(".date-field");
  const popover = document.createElement("div");
  popover.className = "calendario-popover oculto";
  contenedor.appendChild(popover);

  const hoy = new Date();
  hoy.setHours(0, 0, 0, 0);

  let mesVisible = new Date(hoy.getFullYear(), hoy.getMonth(), 1);

  function esPasada(fecha) {
    if (fecha < hoy) return true;
    if (opciones.minDate && fecha < opciones.minDate) return true;
    return false;
  }

  function render() {
    const nombreMes = mesVisible.toLocaleDateString("es-CO", { month: "long", year: "numeric" });
    const primerDiaSemana = new Date(mesVisible.getFullYear(), mesVisible.getMonth(), 1).getDay();
    const diasEnMes = new Date(mesVisible.getFullYear(), mesVisible.getMonth() + 1, 0).getDate();
    const diasMesAnterior = new Date(mesVisible.getFullYear(), mesVisible.getMonth(), 0).getDate();

    let celdas = "";
    for (let i = primerDiaSemana - 1; i >= 0; i--) {
      celdas += `<button type="button" class="calendario-dia fuera-de-mes" disabled>${diasMesAnterior - i}</button>`;
    }

    for (let dia = 1; dia <= diasEnMes; dia++) {
      const fecha = new Date(mesVisible.getFullYear(), mesVisible.getMonth(), dia);
      const iso = fechaAISO(fecha);
      const pasada = esPasada(fecha);
      const ocupada = !pasada && opciones.fechasOcupadas && opciones.fechasOcupadas.has(iso);
      const deshabilitada = pasada || ocupada;
      const clases = ["calendario-dia"];
      if (deshabilitada) clases.push(ocupada ? "ocupado" : "deshabilitado");
      if (fecha.getTime() === hoy.getTime()) clases.push("hoy");
      if (inputEl.dataset.iso === iso) clases.push("seleccionado");
      celdas += `<button type="button" class="${clases.join(" ")}" data-fecha="${iso}" ${deshabilitada ? "disabled" : ""}>${dia}</button>`;
    }

    const totalCeldas = primerDiaSemana + diasEnMes;
    const restantes = (7 - (totalCeldas % 7)) % 7;
    for (let i = 1; i <= restantes; i++) {
      celdas += `<button type="button" class="calendario-dia fuera-de-mes" disabled>${i}</button>`;
    }

    popover.innerHTML = `
      <div class="calendario-cabecera">
        <span class="calendario-mes-actual">${nombreMes}</span>
        <div class="calendario-nav">
          <button type="button" data-accion="prev">‹</button>
          <button type="button" data-accion="next">›</button>
        </div>
      </div>
      <div class="calendario-semana">
        <span>DO</span><span>LU</span><span>MA</span><span>MI</span><span>JU</span><span>VI</span><span>SA</span>
      </div>
      <div class="calendario-dias">${celdas}</div>
      <div class="calendario-pie">
        <button type="button" data-accion="borrar">Borrar</button>
        <button type="button" data-accion="hoy">Hoy</button>
      </div>
    `;

    popover.querySelector('[data-accion="prev"]').addEventListener("click", () => {
      mesVisible = new Date(mesVisible.getFullYear(), mesVisible.getMonth() - 1, 1);
      render();
    });
    popover.querySelector('[data-accion="next"]').addEventListener("click", () => {
      mesVisible = new Date(mesVisible.getFullYear(), mesVisible.getMonth() + 1, 1);
      render();
    });
    popover.querySelector('[data-accion="borrar"]').addEventListener("click", () => {
      seleccionar("");
    });
    popover.querySelector('[data-accion="hoy"]').addEventListener("click", () => {
      mesVisible = new Date(hoy.getFullYear(), hoy.getMonth(), 1);
      render();
    });
    popover.querySelectorAll(".calendario-dia[data-fecha]:not([disabled])").forEach(boton => {
      boton.addEventListener("click", () => seleccionar(boton.dataset.fecha));
    });
  }

  function seleccionar(iso) {
    inputEl.dataset.iso = iso;
    inputEl.value = formatearFechaVisible(iso);
    cerrar();
    if (typeof opciones.onSelect === "function") opciones.onSelect(iso);
  }

  function abrir() {
    document.querySelectorAll(".calendario-popover").forEach(p => { if (p !== popover) p.classList.add("oculto"); });
    if (inputEl.dataset.iso) {
      const fechaSeleccionada = isoAFecha(inputEl.dataset.iso);
      mesVisible = new Date(fechaSeleccionada.getFullYear(), fechaSeleccionada.getMonth(), 1);
    }
    render();
    popover.classList.remove("oculto");
  }

  function cerrar() {
    popover.classList.add("oculto");
  }

  inputEl.addEventListener("click", (e) => {
    if (inputEl.disabled) return;
    e.stopPropagation();
    if (popover.classList.contains("oculto")) abrir(); else cerrar();
  });

  document.addEventListener("click", (e) => {
    if (!contenedor.contains(e.target)) cerrar();
  });

  return {
    limpiar() {
      inputEl.dataset.iso = "";
      inputEl.value = "";
    },
    establecerFechasOcupadas(set) {
      opciones.fechasOcupadas = set;
      render();
    },
    establecerMinima(fecha) {
      opciones.minDate = fecha;
      render();
    },
    obtenerISO() {
      return inputEl.dataset.iso || "";
    },
    cerrar
  };
}

// ================= RESERVA (CON DESCUENTO) =================

const modal = document.getElementById("reserva-modal");

// Trae todas las reservas activas de la habitacion y arma el conjunto
// de noches ya ocupadas, para poder pintarlas en gris en el calendario
// (igual que se pintan las fechas ya pasadas).
async function obtenerFechasOcupadas(habitacionId) {
  const fechas = new Set();
  try {
    const resp = await fetch(`${API}/reservas`);
    if (!resp.ok) return fechas;
    const reservas = await resp.json();
    reservas
      // Solo bloquear fechas de reservas ACTIVAS (CONFIRMADA o EN_CURSO)
      // No bloquear CANCELADA ni FINALIZADA
      .filter(r => r.habitacionId === habitacionId && (r.estado === "CONFIRMADA" || r.estado === "EN_CURSO"))
      .forEach(r => {
        const actual = isoAFecha(r.fechaEntrada);
        const fin = isoAFecha(r.fechaSalida);
        // < en vez de <=: el dia de salida está disponible para un nuevo cliente
        // desde el mismo día (check-out por la mañana, check-in por la tarde)
        while (actual < fin) {
          fechas.add(fechaAISO(actual));
          actual.setDate(actual.getDate() + 1);
        }
      });
  } catch (err) {
    // Si falla la consulta, simplemente no se pinta ninguna fecha ocupada
    // (el backend igual valida el conflicto al confirmar la reserva).
  }
  return fechas;
}

const inputEntrada = document.getElementById("reserva-entrada");
const inputSalida = document.getElementById("reserva-salida");

const selectorEntrada = crearSelectorFecha(inputEntrada, {
  onSelect(iso) {
    // La salida debe ser posterior a la entrada elegida.
    if (iso) {
      const minSalida = new Date(isoAFecha(iso));
      minSalida.setDate(minSalida.getDate() + 1);
      selectorSalida.establecerMinima(minSalida);
      if (selectorSalida.obtenerISO() && selectorSalida.obtenerISO() <= iso) {
        selectorSalida.limpiar();
      }
    }
  }
});

const selectorSalida = crearSelectorFecha(inputSalida, {});

async function abrirModalReserva(habitacion) {
  habitacionSeleccionada = habitacion;
  document.getElementById("reserva-titulo").textContent = `Reservar: ${habitacion.tipo} - Hab. ${habitacion.numero}`;
  document.getElementById("reserva-error").textContent = "";
  document.getElementById("reserva-ok").textContent = "";

  const hoy = new Date();
  hoy.setHours(0, 0, 0, 0);

  selectorEntrada.limpiar();
  selectorSalida.limpiar();
  selectorEntrada.establecerMinima(hoy);
  selectorSalida.establecerMinima(hoy);

  modal.style.display = "flex";

  // Verificar si tiene descuento disponible
  verificarDescuentoDisponible();

  // Cargar y pintar en gris las fechas que ya estan ocupadas para esta habitacion
  const fechasOcupadas = await obtenerFechasOcupadas(habitacion.id);
  selectorEntrada.establecerFechasOcupadas(fechasOcupadas);
  selectorSalida.establecerFechasOcupadas(fechasOcupadas);
}

async function verificarDescuentoDisponible() {
  let descuentoInfoEl = document.getElementById("descuento-info");
  if (!descuentoInfoEl) {
    // Crear elemento para mostrar descuento si no existe
    descuentoInfoEl = document.createElement("div");
    descuentoInfoEl.id = "descuento-info";
    descuentoInfoEl.style.cssText = "background: #d1fae5; color: #065f46; padding: 12px; border-radius: 6px; margin: 10px 0; font-weight: 600; display: none;";
    document.getElementById("reserva-form").insertBefore(descuentoInfoEl, document.getElementById("reserva-form").firstChild);
  }
  
  try {
    const resp = await fetch(`${API}/clientes/${usuarioActual.clienteId}/puntos`);
    if (resp.ok) {
      const puntos = await resp.json();
      const descuento = puntos.descuentoDisponible || 0;
      
      if (descuento > 0) {
        const porcentaje = (descuento * 100).toFixed(0);
        descuentoInfoEl.innerHTML = `
          🎉 <strong>¡Tienes un ${porcentaje}% de descuento disponible!</strong><br>
          Se aplicará automáticamente a esta reserva.
        `;
        descuentoInfoEl.style.display = "block";
      } else {
        descuentoInfoEl.style.display = "none";
      }
    }
  } catch (err) {
    // Si falla, continuar sin mostrar descuento
    descuentoInfoEl.style.display = "none";
  }
}

document.getElementById("btn-cerrar-modal").addEventListener("click", () => {
  modal.style.display = "none";
});

document.getElementById("reserva-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  const fechaEntrada = selectorEntrada.obtenerISO();
  const fechaSalida = selectorSalida.obtenerISO();
  const errorEl = document.getElementById("reserva-error");
  const okEl = document.getElementById("reserva-ok");
  errorEl.textContent = "";
  okEl.textContent = "";

  if (!fechaEntrada || !fechaSalida) {
    errorEl.textContent = "Elige la fecha de entrada y de salida.";
    return;
  }

  if (fechaSalida <= fechaEntrada) {
    errorEl.textContent = "La fecha de salida debe ser después de la fecha de entrada.";
    return;
  }

  try {
    const resp = await fetch(`${API}/reservas`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        clienteId: usuarioActual.clienteId,
        habitacionId: habitacionSeleccionada.id,
        fechaEntrada,
        fechaSalida
      })
    });

    if (!resp.ok) {
      const err = await resp.json().catch(() => ({}));
      errorEl.textContent = err.mensaje || "No se pudo crear la reserva";
      return;
    }

    const reserva = await resp.json();
    
    // Mostrar mensaje con o sin descuento
    let mensaje = "¡Reserva creada!";
    if (reserva.descuentoAplicado && reserva.descuentoAplicado > 0) {
      const porcentaje = (reserva.descuentoAplicado * 100).toFixed(0);
      const totalOriginal = Number(reserva.totalEstimado);
      const totalFinal = Number(reserva.totalConDescuento);
      const ahorro = totalOriginal - totalFinal;
      mensaje += ` Se aplicó tu descuento del ${porcentaje}% (ahorraste $${ahorro.toLocaleString("es-CO")}). Total a pagar: $${totalFinal.toLocaleString("es-CO")}.`;
    }
    mensaje += " Tienes 24 horas para pagar o se cancelará.";
    
    okEl.textContent = mensaje;

    // Refrescar puntos del cliente
    const respPuntos = await fetch(`${API}/clientes/${usuarioActual.clienteId}/puntos`);
    if (respPuntos.ok) {
      const puntosInfo = await respPuntos.json();
      usuarioActual.puntos = puntosInfo.puntosTotales - puntosInfo.puntosCanjeados;
      guardarSesion(usuarioActual);
      document.getElementById("cliente-puntos").textContent = usuarioActual.puntos + " puntos";
    }

    cargarHabitaciones();

    setTimeout(() => { modal.style.display = "none"; }, 4000);
  } catch (err) {
    errorEl.textContent = "No se pudo conectar con el servidor.";
  }
});


/* ================= ADMIN: MENU Y TABLAS POR ENTIDAD ================= */

const menuAdminSection = document.getElementById("admin-menu-section");
const tablaAdminSection = document.getElementById("admin-tabla-section");

function mostrarMenuAdminBase() {
  menuAdminSection.style.display = "block";
  tablaAdminSection.style.display = "none";
}

function mostrarTablaAdmin() {
  menuAdminSection.style.display = "none";
  tablaAdminSection.style.display = "block";
}

document.getElementById("btn-volver-menu-admin").addEventListener("click", mostrarMenuAdmin);

document.querySelectorAll(".admin-menu-card").forEach(card => {
  card.addEventListener("click", () => {
    const entidad = card.getAttribute("data-entidad");
    mostrarTablaAdmin();
    cargarTablaEntidad(entidad);
  });
});

// Configuracion de cada entidad: titulo, endpoint y columnas a mostrar.
// "campo" puede ser una ruta simple o una funcion que recibe la fila.
const ENTIDADES = {
  clientes: {
    titulo: "Clientes registrados",
    endpoint: "/clientes",
    columnas: [
      { titulo: "ID", campo: "id" },
      { titulo: "Nombre", campo: "nombre" },
      { titulo: "Apellido", campo: r => r.apellido || "-" },
      { titulo: "Correo", campo: "correo" },
      { titulo: "Teléfono", campo: r => r.telefono || "-" },
      { titulo: "Documento", campo: r => r.documento || "-" }
    ]
  },
  habitaciones: {
    titulo: "Habitaciones",
    endpoint: "/habitaciones",
    columnas: [
      { titulo: "ID", campo: "id" },
      { titulo: "Número", campo: "numero" },
      { titulo: "Tipo", campo: "tipo" },
      { titulo: "Capacidad", campo: "capacidad" },
      { titulo: "Precio/noche", campo: r => "$" + Number(r.precioNoche).toLocaleString("es-CO") },
      { titulo: "Estado", campo: r => badgeEstado(r.estado) },
      { titulo: "Acciones", campo: r => `
          <button class="tbl-btn tbl-btn-editar" data-id="${r.id}">Editar</button>
          <button class="tbl-btn tbl-btn-eliminar" data-id="${r.id}">Eliminar</button>
        ` }
    ]
  },
  reservas: {
    titulo: "Reservas",
    endpoint: "/reservas",
    columnas: [
      { titulo: "ID", campo: "id" },
      { titulo: "Cliente", campo: "clienteNombre" },
      { titulo: "Habitación", campo: r => `${r.habitacionTipo} · ${r.habitacionNumero}` },
      { titulo: "Entrada", campo: "fechaEntrada" },
      { titulo: "Salida", campo: "fechaSalida" },
      { titulo: "Estado", campo: r => badgeEstado(r.estado) },
      { titulo: "Total est.", campo: r => r.totalEstimado != null ? "$" + Number(r.totalEstimado).toLocaleString("es-CO") : "-" },
      { titulo: "Obs. Check-in", campo: r => r.observacionesCheckIn || "-" },
      { titulo: "Obs. Check-out", campo: r => r.observacionesCheckOut || "-" },
      { titulo: "Acciones", campo: r => accionesReserva(r) }
    ]
  },
  "llegadas-hoy": {
    titulo: "Llegadas de hoy",
    endpoint: "/reservas/llegadas-hoy",
    columnas: [
      { titulo: "ID", campo: "id" },
      { titulo: "Cliente", campo: "clienteNombre" },
      { titulo: "Documento", campo: r => r.clienteDocumento || "-" },
      { titulo: "Habitación", campo: r => `${r.habitacionTipo} · ${r.habitacionNumero}` },
      { titulo: "Salida prevista", campo: "fechaSalida" },
      { titulo: "Estado", campo: r => badgeEstado(r.estado) },
      { titulo: "Obs. Check-in", campo: r => r.observacionesCheckIn || "-" },
      { titulo: "Acciones", campo: r => accionesReserva(r) }
    ]
  },
  "salidas-hoy": {
    titulo: "Salidas de hoy",
    endpoint: "/reservas/salidas-hoy",
    columnas: [
      { titulo: "ID", campo: "id" },
      { titulo: "Cliente", campo: "clienteNombre" },
      { titulo: "Habitación", campo: r => `${r.habitacionTipo} · ${r.habitacionNumero}` },
      { titulo: "Check-in registrado", campo: r => formatearFechaHora(r.fechaCheckIn) },
      { titulo: "Obs. Check-in", campo: r => r.observacionesCheckIn || "-" },
      { titulo: "Obs. Check-out", campo: r => r.observacionesCheckOut || "-" },
      { titulo: "Estado", campo: r => badgeEstado(r.estado) },
      { titulo: "Acciones", campo: r => accionesReserva(r) }
    ]
  },
  pagos: {
    titulo: "Pagos",
    endpoint: "/pagos",
    columnas: [
      { titulo: "ID", campo: "id" },
      { titulo: "Reserva", campo: "reservaId" },
      { titulo: "Monto", campo: r => "$" + Number(r.monto).toLocaleString("es-CO") },
      { titulo: "Método", campo: "metodoPago" },
      { titulo: "Estado", campo: r => badgeEstado(r.estado) },
      { titulo: "Fecha", campo: "fechaPago" }
    ]
  },
  resenas: {
    titulo: "Reseñas",
    endpoint: "/resenas",
    columnas: [
      { titulo: "ID", campo: "id" },
      { titulo: "Cliente", campo: "clienteNombre" },
      { titulo: "General", campo: "califGeneral" },
      { titulo: "Limpieza", campo: "califLimpieza" },
      { titulo: "Atención", campo: "califAtencion" },
      { titulo: "Comentario", campo: r => r.comentario || "-" },
      { titulo: "Fecha", campo: "fechaResena" }
    ]
  }
};

// Botones de accion para una fila de reserva, segun su estado actual.
// Se usa en las 3 vistas que muestran reservas: "reservas", "llegadas-hoy"
// y "salidas-hoy".
function accionesReserva(r) {
  if (r.estado === "CONFIRMADA") {
    return `<button class="tbl-btn tbl-btn-checkin" data-id="${r.id}">🛎️ Check-in</button>`;
  }
  if (r.estado === "EN_CURSO") {
    // Solo permitir checkout a partir de la fecha de salida
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    const fechaSalida = new Date(r.fechaSalida);
    fechaSalida.setHours(0, 0, 0, 0);
    
    const puedeHacerCheckout = hoy >= fechaSalida;
    
    if (puedeHacerCheckout) {
      return `<button class="tbl-btn tbl-btn-checkout" data-id="${r.id}">🧳 Check-out</button>`;
    } else {
      // Mostrar botón deshabilitado con tooltip
      const diasFalta = Math.ceil((fechaSalida - hoy) / (1000 * 60 * 60 * 24));
      return `<button class="tbl-btn tbl-btn-checkout" disabled title="Check-out disponible el ${r.fechaSalida}">🧳 Check-out (en ${diasFalta}d)</button>`;
    }
  }
  return "-";
}

function formatearFechaHora(valor) {
  if (!valor) return "-";
  const fecha = new Date(valor);
  if (isNaN(fecha.getTime())) return "-";
  return fecha.toLocaleString("es-CO", {
    day: "2-digit", month: "2-digit", year: "numeric",
    hour: "2-digit", minute: "2-digit"
  });
}

function badgeEstado(valor) {
  if (!valor) return "-";
  const v = String(valor).toUpperCase();
  let clase = "badge-gris";
  if (["DISPONIBLE", "CONFIRMADA", "PAGADO", "FINALIZADA"].includes(v)) clase = "badge-verde";
  else if (["OCUPADA", "EN_CURSO", "RESERVADA"].includes(v)) clase = "badge-azul";
  else if (["EN_LIMPIEZA", "PENDIENTE", "PENDIENTE_LIMPIEZA"].includes(v)) clase = "badge-amarillo";
  else if (["CANCELADA", "FUERA_DE_SERVICIO", "REEMBOLSADO"].includes(v)) clase = "badge-rojo";
  return `<span class="badge ${clase}">${valor}</span>`;
}

let entidadActual = null;
let ultimasHabitaciones = [];
let ultimosClientes = [];
let ultimasReservas = [];
let ultimasFilasTabla = [];

function renderFilasTabla(config, filas, entidad) {
  const msgEl = document.getElementById("admin-tabla-msg");
  const tablaEl = document.getElementById("admin-tabla");
  const headEl = document.getElementById("admin-tabla-head");
  const bodyEl = document.getElementById("admin-tabla-body");

  headEl.innerHTML = "";
  bodyEl.innerHTML = "";

  if (!filas.length) {
    tablaEl.style.display = "none";
    msgEl.textContent = "No se encontraron registros.";
    return;
  }

  const trHead = document.createElement("tr");
  config.columnas.forEach(col => {
    const th = document.createElement("th");
    th.textContent = col.titulo;
    trHead.appendChild(th);
  });
  headEl.appendChild(trHead);

  filas.forEach(fila => {
    const tr = document.createElement("tr");
    config.columnas.forEach(col => {
      const td = document.createElement("td");
      const valor = typeof col.campo === "function" ? col.campo(fila) : fila[col.campo];
      td.innerHTML = (valor === null || valor === undefined || valor === "") ? "-" : valor;
      tr.appendChild(td);
    });
    bodyEl.appendChild(tr);
  });

  msgEl.textContent = "";
  tablaEl.style.display = "table";

  if (entidad === "habitaciones") {
    bodyEl.querySelectorAll(".tbl-btn-editar").forEach(btn => {
      btn.addEventListener("click", () => {
        const hab = ultimasHabitaciones.find(h => String(h.id) === btn.getAttribute("data-id"));
        if (hab) abrirModalHabitacion("editar", hab);
      });
    });
    bodyEl.querySelectorAll(".tbl-btn-eliminar").forEach(btn => {
      btn.addEventListener("click", () => eliminarHabitacion(btn.getAttribute("data-id")));
    });
  }

  if (entidad === "reservas" || entidad === "llegadas-hoy" || entidad === "salidas-hoy") {
    bodyEl.querySelectorAll(".tbl-btn-checkin").forEach(btn => {
      btn.addEventListener("click", () => {
        const fila = ultimasFilasTabla.find(r => String(r.id) === btn.getAttribute("data-id"));
        abrirModalCheckIn(btn.getAttribute("data-id"), fila);
      });
    });
    bodyEl.querySelectorAll(".tbl-btn-checkout").forEach(btn => {
      btn.addEventListener("click", () => {
        const fila = ultimasFilasTabla.find(r => String(r.id) === btn.getAttribute("data-id"));
        abrirModalCheckOut(btn.getAttribute("data-id"), fila);
      });
    });
  }
}

// ================= ADMIN: CHECK-IN DE RESERVAS =================

const checkinModal = document.getElementById("checkin-modal");

function abrirModalCheckIn(reservaId, fila) {
  document.getElementById("checkin-reserva-id").value = reservaId;
  document.getElementById("checkin-documento").value = "";
  document.getElementById("checkin-acompanantes").value = "0";
  document.getElementById("checkin-observaciones").value = "";
  document.getElementById("checkin-error").textContent = "";
  document.getElementById("checkin-ok").textContent = "";

  const infoEl = document.getElementById("checkin-huesped-info");
  if (fila) {
    infoEl.textContent = `${fila.clienteNombre} · Hab. ${fila.habitacionNumero} (${fila.habitacionTipo})`
      + (fila.clienteDocumento ? ` · Documento registrado: ${fila.clienteDocumento}` : "");
  } else {
    infoEl.textContent = "";
  }

  checkinModal.style.display = "flex";
}

document.getElementById("btn-cerrar-checkin-modal").addEventListener("click", () => {
  checkinModal.style.display = "none";
});

document.getElementById("checkin-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  const reservaId = document.getElementById("checkin-reserva-id").value;
  const documentoHuesped = document.getElementById("checkin-documento").value.trim();
  const numAcompanantes = Number(document.getElementById("checkin-acompanantes").value) || 0;
  const observaciones = document.getElementById("checkin-observaciones").value.trim();

  const errorEl = document.getElementById("checkin-error");
  const okEl = document.getElementById("checkin-ok");
  errorEl.textContent = "";
  okEl.textContent = "";

  try {
    const resp = await fetch(`${API}/reservas/${reservaId}/checkin`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ documentoHuesped, numAcompanantes, observaciones })
    });

    if (!resp.ok) {
      const err = await resp.json().catch(() => ({}));
      errorEl.textContent = err.mensaje || "No se pudo hacer el check-in.";
      return;
    }

    okEl.textContent = "¡Check-in realizado! El huésped ya quedó registrado.";
    setTimeout(() => {
      checkinModal.style.display = "none";
      cargarTablaEntidad(entidadActual);
    }, 1200);
  } catch (err) {
    errorEl.textContent = "No se pudo conectar con el servidor.";
  }
});

// ================= ADMIN: CHECK-OUT DE RESERVAS =================

const checkoutModal = document.getElementById("checkout-modal");

function abrirModalCheckOut(reservaId, fila) {
  document.getElementById("checkout-reserva-id").value = reservaId;
  document.getElementById("checkout-estado-habitacion").value = "OK";
  document.getElementById("checkout-observaciones").value = "";
  document.getElementById("checkout-error").textContent = "";
  document.getElementById("checkout-ok").textContent = "";

  const infoEl = document.getElementById("checkout-huesped-info");
  infoEl.textContent = fila
    ? `${fila.clienteNombre} · Hab. ${fila.habitacionNumero} (${fila.habitacionTipo})`
    : "";

  checkoutModal.style.display = "flex";
}

document.getElementById("btn-cerrar-checkout-modal").addEventListener("click", () => {
  checkoutModal.style.display = "none";
});

document.getElementById("checkout-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  const reservaId = document.getElementById("checkout-reserva-id").value;
  const estadoHabitacion = document.getElementById("checkout-estado-habitacion").value;
  const observaciones = document.getElementById("checkout-observaciones").value.trim();

  const errorEl = document.getElementById("checkout-error");
  const okEl = document.getElementById("checkout-ok");
  errorEl.textContent = "";
  okEl.textContent = "";

  try {
    const resp = await fetch(`${API}/reservas/${reservaId}/checkout`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ estadoHabitacion, observaciones })
    });

    if (!resp.ok) {
      const err = await resp.json().catch(() => ({}));
      errorEl.textContent = err.mensaje || "No se pudo hacer el check-out.";
      return;
    }

    okEl.textContent = estadoHabitacion === "DANOS"
      ? "Check-out registrado. La habitación quedó marcada Fuera de servicio."
      : "¡Check-out realizado correctamente!";

    setTimeout(() => {
      checkoutModal.style.display = "none";
      cargarTablaEntidad(entidadActual);
    }, 1500);
  } catch (err) {
    errorEl.textContent = "No se pudo conectar con el servidor.";
  }
});

async function cargarTablaEntidad(entidad) {
  entidadActual = entidad;
  const config = ENTIDADES[entidad];
  const tituloEl = document.getElementById("admin-tabla-titulo");
  const msgEl = document.getElementById("admin-tabla-msg");
  const tablaEl = document.getElementById("admin-tabla");
  const btnAgregarHab = document.getElementById("btn-agregar-habitacion");
  const buscarApellidoInput = document.getElementById("buscar-apellido");
  const filtroEstadoSelect = document.getElementById("filtro-estado-reserva");

  btnAgregarHab.style.display = entidad === "habitaciones" ? "inline-block" : "none";
  buscarApellidoInput.style.display = entidad === "clientes" ? "inline-block" : "none";
  filtroEstadoSelect.style.display = (entidad === "reservas" || entidad === "llegadas-hoy" || entidad === "salidas-hoy") ? "inline-block" : "none";
  buscarApellidoInput.value = "";
  filtroEstadoSelect.value = "";

  tituloEl.textContent = config.titulo;
  msgEl.textContent = "Cargando...";
  tablaEl.style.display = "none";

  try {
    const resp = await fetch(`${API}${config.endpoint}`);
    if (!resp.ok) throw new Error("respuesta no ok");
    let filas = await resp.json();

    if (entidad === "habitaciones") {
      filas = ordenarHabitaciones(filas);
      ultimasHabitaciones = filas;
    }
    if (entidad === "clientes") ultimosClientes = filas;
    if (entidad === "reservas" || entidad === "llegadas-hoy" || entidad === "salidas-hoy") ultimasReservas = filas;

    ultimasFilasTabla = filas;

    if (!filas.length) {
      msgEl.textContent = "Todavía no hay registros para mostrar.";
      return;
    }

    renderFilasTabla(config, filas, entidad);
  } catch (err) {
    msgEl.textContent = "No se pudo cargar la información de " + config.titulo.toLowerCase() + ".";
  }
}

document.getElementById("buscar-apellido").addEventListener("input", (e) => {
  const query = e.target.value.trim().toLowerCase();
  const filtrados = query
    ? ultimosClientes.filter(c => (c.apellido || "").toLowerCase().includes(query))
    : ultimosClientes;
  renderFilasTabla(ENTIDADES.clientes, filtrados, "clientes");
});

document.getElementById("filtro-estado-reserva").addEventListener("change", (e) => {
  const estado = e.target.value.trim();
  const filtrados = estado
    ? ultimasReservas.filter(r => r.estado === estado)
    : ultimasReservas;
  renderFilasTabla(ENTIDADES[entidadActual], filtrados, entidadActual);
});

/* ================= ADMIN: CRUD DE HABITACIONES ================= */

const habitacionModal = document.getElementById("habitacion-modal");
const habitacionForm = document.getElementById("habitacion-form");
let modoHabitacion = "crear";

document.getElementById("btn-agregar-habitacion").addEventListener("click", () => {
  abrirModalHabitacion("crear");
});

document.getElementById("btn-cerrar-habitacion-modal").addEventListener("click", () => {
  habitacionModal.style.display = "none";
});

function abrirModalHabitacion(modo, hab) {
  modoHabitacion = modo;
  document.getElementById("habitacion-error").textContent = "";
  document.getElementById("habitacion-modal-titulo").textContent =
    modo === "crear" ? "Nueva habitación" : "Editar habitación";
  document.getElementById("habitacion-submit-btn").textContent =
    modo === "crear" ? "Crear habitación" : "Guardar cambios";

  document.getElementById("hab-id").value = hab ? hab.id : "";
  document.getElementById("hab-numero").value = hab ? hab.numero : "";
  document.getElementById("hab-tipo").value = hab ? hab.tipo : "";
  document.getElementById("hab-capacidad").value = hab ? hab.capacidad : "";
  document.getElementById("hab-precio").value = hab ? hab.precioNoche : "";
  document.getElementById("hab-descripcion").value = hab ? (hab.descripcion || "") : "";
  document.getElementById("hab-estado").value = hab ? hab.estado : "DISPONIBLE";

  habitacionModal.style.display = "flex";
}

habitacionForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  const errorEl = document.getElementById("habitacion-error");
  errorEl.textContent = "";

  const id = document.getElementById("hab-id").value;
  const payload = {
    numero: document.getElementById("hab-numero").value,
    tipo: document.getElementById("hab-tipo").value,
    capacidad: Number(document.getElementById("hab-capacidad").value),
    precioNoche: Number(document.getElementById("hab-precio").value),
    descripcion: document.getElementById("hab-descripcion").value,
    estado: document.getElementById("hab-estado").value
  };

  const url = modoHabitacion === "crear" ? `${API}/habitaciones` : `${API}/habitaciones/${id}`;
  const metodo = modoHabitacion === "crear" ? "POST" : "PUT";

  try {
    const resp = await fetch(url, {
      method: metodo,
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });

    if (!resp.ok) {
      const err = await resp.json().catch(() => ({}));
      errorEl.textContent = err.detalle ? Object.values(err.detalle).join(" · ") : (err.mensaje || "No se pudo guardar la habitación");
      return;
    }

    habitacionModal.style.display = "none";
    cargarTablaEntidad("habitaciones");
  } catch (err) {
    errorEl.textContent = "No se pudo conectar con el servidor.";
  }
});

async function eliminarHabitacion(id) {
  if (!confirm("¿Seguro que quieres eliminar esta habitación? Esta acción no se puede deshacer.")) {
    return;
  }
  try {
    const resp = await fetch(`${API}/habitaciones/${id}`, { method: "DELETE" });
    if (!resp.ok) {
      const err = await resp.json().catch(() => ({}));
      alert(err.mensaje || "No se pudo eliminar la habitación.");
      return;
    }
    cargarTablaEntidad("habitaciones");
  } catch (err) {
    alert("No se pudo conectar con el servidor.");
  }
}

/* ================= ILUSTRACIONES (SVG simples, sin usar fotos externas) ================= */

function svgHotel() {
  return `
  <svg viewBox="0 0 500 280" xmlns="http://www.w3.org/2000/svg">
    <defs>
      <linearGradient id="cielo" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" stop-color="#bfe3fb"/>
        <stop offset="100%" stop-color="#eaf6ff"/>
      </linearGradient>
      <linearGradient id="rio" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" stop-color="#5fb3e0"/>
        <stop offset="100%" stop-color="#2f8fc4"/>
      </linearGradient>
    </defs>
    <rect x="0" y="0" width="500" height="280" fill="url(#cielo)"/>
    <circle cx="440" cy="45" r="24" fill="#ffd873"/>

    <!-- orilla -->
    <path d="M0 190 Q125 172 250 190 T500 188 V220 H0 Z" fill="#d9c79c"/>

    <!-- hotel -->
    <rect x="110" y="55" width="270" height="140" rx="6" fill="#ffffff" stroke="#1a75c4" stroke-width="3"/>
    <rect x="110" y="55" width="270" height="26" fill="#1a75c4"/>
    <text x="245" y="73" font-family="Poppins, sans-serif" font-size="13" fill="#fff" text-anchor="middle">HOTEL SINÚ</text>
    ${[0,1,2,3].map(row => [0,1,2,3].map(col => `
      <rect x="${128 + col*54}" y="${94 + row*24}" width="34" height="16" rx="2" fill="${(row+col)%2===0 ? '#bfe0fb' : '#eaf5fd'}" stroke="#1a75c4" stroke-width="1.3"/>
    `).join("")).join("")}
    <rect x="225" y="165" width="40" height="30" fill="#0f4c86"/>
    <rect x="230" y="172" width="10" height="10" fill="#bfe0fb"/>
    <rect x="252" y="172" width="10" height="10" fill="#bfe0fb"/>

    <!-- palmeras -->
    <g>
      <rect x="66" y="150" width="7" height="34" fill="#8a6538" rx="2"/>
      <path d="M69 152 q-24 -10 -34 4 q16 -2 34 -1" fill="#2fae66"/>
      <path d="M69 152 q26 -12 36 2 q-18 -2 -36 1" fill="#2fae66"/>
      <path d="M69 150 q-4 -22 10 -30 q0 18 -10 30" fill="#2fae66"/>
    </g>
    <g>
      <rect x="418" y="140" width="7" height="40" fill="#8a6538" rx="2"/>
      <path d="M421 142 q-26 -10 -36 4 q17 -2 36 -1" fill="#2fae66"/>
      <path d="M421 142 q28 -12 38 2 q-19 -2 -38 1" fill="#2fae66"/>
      <path d="M421 140 q-4 -22 11 -30 q0 18 -11 30" fill="#2fae66"/>
    </g>

    <!-- rio sinu -->
    <path d="M0 218 Q100 200 180 222 T340 220 Q420 210 500 226 V280 H0 Z" fill="url(#rio)"/>
    <path d="M20 232 Q120 218 220 236 T420 232" stroke="#ffffff" stroke-opacity="0.35" stroke-width="4" fill="none"/>
    <path d="M0 250 Q140 236 260 254 T500 248" stroke="#ffffff" stroke-opacity="0.25" stroke-width="5" fill="none"/>
    <text x="250" y="270" font-family="Nunito, sans-serif" font-size="12" fill="#0f4c86" opacity="0.55" text-anchor="middle">Río Sinú</text>
  </svg>`;
}

function svgHabitacion(tipo) {
  const t = (tipo || "").toLowerCase();
  let color = "#1a75c4";
  if (t.includes("suite presidencial")) color = "#b8860b";
  else if (t.includes("suite")) color = "#7a4fc4";
  else if (t.includes("triple")) color = "#c46a1a";
  else if (t.includes("doble")) color = "#2fae66";

  return `
  <svg viewBox="0 0 120 120" xmlns="http://www.w3.org/2000/svg">
    <rect x="10" y="60" width="100" height="35" rx="5" fill="${color}" opacity="0.15"/>
    <rect x="14" y="45" width="30" height="22" rx="4" fill="${color}"/>
    <rect x="76" y="45" width="30" height="22" rx="4" fill="${color}" opacity="0.7"/>
    <rect x="10" y="60" width="100" height="10" fill="${color}"/>
    <rect x="14" y="70" width="92" height="24" rx="4" fill="#ffffff" stroke="${color}" stroke-width="2"/>
    <circle cx="60" cy="30" r="14" fill="${color}" opacity="0.25"/>
    <path d="M50 30a10 10 0 0 1 20 0" stroke="${color}" stroke-width="3" fill="none" stroke-linecap="round"/>
  </svg>`;
}


/* ===============================================
   CÓDIGO NUEVO PARA AGREGAR AL FINAL DE APP.JS
   =============================================== */

// ================= MIS RESERVAS (CLIENTE) =================

document.getElementById("btn-ver-habitaciones").insertAdjacentHTML("afterend", `
  <button id="btn-ver-mis-reservas" class="btn-secundario" style="margin-left: 10px;">Ver mis reservas</button>
`);

document.getElementById("btn-ver-mis-reservas").addEventListener("click", () => {
  document.getElementById("bienvenida-section").style.display = "none";
  document.getElementById("habitaciones-section").style.display = "none";
  mostrarMisReservas();
});

function mostrarMisReservas() {
  let section = document.getElementById("mis-reservas-section");
  if (!section) {
    section = document.createElement("section");
    section.id = "mis-reservas-section";
    section.className = "habitaciones-section";
    document.getElementById("cliente-section").appendChild(section);
  }

  section.style.display = "block";
  section.innerHTML = `
    <div class="habitaciones-header">
      <h2>Mis Reservas</h2>
      <button id="btn-volver-bienvenida-reservas" class="btn-secundario">← Volver</button>
    </div>
    <p id="reservas-msg">Cargando reservas...</p>
    <div id="reservas-list" class="rooms-grid"></div>
  `;

  document.getElementById("btn-volver-bienvenida-reservas").addEventListener("click", () => {
    section.style.display = "none";
    mostrarBienvenida();
  });

  cargarMisReservas();
}

async function cargarMisReservas() {
  const msgEl = document.getElementById("reservas-msg");
  const listEl = document.getElementById("reservas-list");
  msgEl.textContent = "Cargando reservas...";
  listEl.innerHTML = "";

  try {
    const resp = await fetch(`${API}/reservas/cliente/${usuarioActual.clienteId}`);
    const data = await resp.json().catch(() => null);

    if (!resp.ok) {
      // El servidor respondió con un error real (no es que falten reservas).
      // Mostramos el mensaje real para no confundirlo con "no tienes reservas".
      msgEl.textContent = (data && data.mensaje)
        ? `No se pudieron cargar las reservas: ${data.mensaje}`
        : "No se pudieron cargar las reservas.";
      return;
    }

    const reservas = data || [];
    if (!reservas.length) {
      msgEl.textContent = "No tienes reservas todavía.";
      return;
    }

    msgEl.textContent = "";
    reservas.forEach(r => listEl.appendChild(crearTarjetaReserva(r)));
  } catch (err) {
    msgEl.textContent = "No se pudieron cargar las reservas.";
  }
}

function crearTarjetaReserva(r) {
  const div = document.createElement("div");
  div.className = "room-card";

  const estadoClase = {
    PENDIENTE: "badge-amarillo",
    CONFIRMADA: "badge-verde",
    EN_CURSO: "badge-azul",
    FINALIZADA: "badge-gris",
    CANCELADA: "badge-rojo"
  }[r.estado] || "badge-gris";

  const totalOriginal = Number(r.totalEstimado);
  const totalFinal = Number(r.totalConDescuento || r.totalEstimado);
  const tieneDescuento = r.descuentoAplicado && r.descuentoAplicado > 0;

  let precioHtml = "";
  if (tieneDescuento) {
    const porcentajeDesc = (r.descuentoAplicado * 100).toFixed(0);
    const ahorro = totalOriginal - totalFinal;
    precioHtml = `
      <p class="precio-original">$${totalOriginal.toLocaleString("es-CO")}</p>
      <p class="precio precio-descuento">$${totalFinal.toLocaleString("es-CO")}
        <span class="badge-descuento">-${porcentajeDesc}%</span>
      </p>
      <p class="ahorro-texto">Ahorraste: $${ahorro.toLocaleString("es-CO")}</p>
    `;
  } else {
    precioHtml = `<p class="precio">$${totalFinal.toLocaleString("es-CO")}</p>`;
  }

  let limiteHtml = "";
  if (r.estado === "PENDIENTE" && r.fechaLimitePago) {
    const limite = new Date(r.fechaLimitePago);
    const ahora = new Date();
    const horasRestantes = Math.max(0, Math.floor((limite - ahora) / (1000 * 60 * 60)));
    const minutosRestantes = Math.max(0, Math.floor(((limite - ahora) % (1000 * 60 * 60)) / (1000 * 60)));
    limiteHtml = `<p class="limite-pago">⏰ Pagar en ${horasRestantes}h ${minutosRestantes}min o se cancelará</p>`;
  }

  let checkinHtml = "";
  if (r.fechaCheckIn) {
    checkinHtml += `<p class="checkin-hecho">🛎️ Check-in: <strong>${formatearFechaHora(r.fechaCheckIn)}</strong></p>`;
  }
  if (r.fechaCheckOut) {
    checkinHtml += `<p class="checkin-hecho">🧳 Check-out: <strong>${formatearFechaHora(r.fechaCheckOut)}</strong></p>`;
  }

  let acciones = "";
  if (r.estado === "PENDIENTE") {
    acciones = `
      <button class="btn-pagar" data-id="${r.id}" data-monto="${totalFinal}">💳 Pagar ahora</button>
      <button class="btn-cancelar-reserva" data-id="${r.id}">✖ Cancelar reserva</button>
    `;
  } else if (r.estado === "CONFIRMADA") {
    acciones = `<button class="btn-cancelar-reserva" data-id="${r.id}">✖ Cancelar reserva</button>`;
  } else if (r.estado === "FINALIZADA") {
    acciones = r.tieneResena
      ? `<p class="resena-enviada">✅ Ya dejaste tu reseña. ¡Gracias!</p>`
      : `<button class="btn-resena" data-id="${r.id}">⭐ Dejar reseña</button>`;
  }

  div.innerHTML = `
    <div class="room-card-body">
      <h3>${r.habitacionTipo} · Hab. ${r.habitacionNumero}</h3>
      ${precioHtml}
      <p>📅 Entrada: <strong>${r.fechaEntrada}</strong></p>
      <p>📅 Salida: <strong>${r.fechaSalida}</strong></p>
      <p><span class="badge ${estadoClase}">${r.estado}</span></p>
      ${checkinHtml}
      ${limiteHtml}
      ${acciones}
    </div>
  `;

  const btnPagar = div.querySelector(".btn-pagar");
  if (btnPagar) {
    btnPagar.addEventListener("click", () => abrirModalPago(r.id, totalFinal));
  }

  const btnResena = div.querySelector(".btn-resena");
  if (btnResena) {
    btnResena.addEventListener("click", () => abrirModalResena(r.id));
  }

  const btnCancelar = div.querySelector(".btn-cancelar-reserva");
  if (btnCancelar) {
    btnCancelar.addEventListener("click", () => abrirModalCancelarReserva(r.id));
  }

  return div;
}

// ================= MODAL CANCELAR RESERVA =================

const cancelarReservaModal = document.createElement("div");
cancelarReservaModal.id = "cancelar-reserva-modal";
cancelarReservaModal.className = "modal";
cancelarReservaModal.style.display = "none";
cancelarReservaModal.innerHTML = `
  <div class="modal-content">
    <h3>✖ Cancelar reserva</h3>
    <p>¿Seguro que quieres cancelar esta reserva? Esta acción no se puede deshacer.</p>
    <p id="cancelar-reserva-error" class="error-msg"></p>
    <p id="cancelar-reserva-ok" class="ok-msg"></p>
    <input type="hidden" id="cancelar-reserva-id">
    <button type="button" id="btn-confirmar-cancelar-reserva" class="btn-cancelar-reserva" style="margin-top:10px;">Sí, cancelar reserva</button>
    <button type="button" id="btn-cerrar-cancelar-reserva" class="btn-secundario" style="margin-top:10px;">Volver</button>
  </div>
`;
document.body.appendChild(cancelarReservaModal);

function abrirModalCancelarReserva(reservaId) {
  document.getElementById("cancelar-reserva-id").value = reservaId;
  document.getElementById("cancelar-reserva-error").textContent = "";
  document.getElementById("cancelar-reserva-ok").textContent = "";
  document.getElementById("btn-confirmar-cancelar-reserva").disabled = false;
  cancelarReservaModal.style.display = "flex";
}

document.getElementById("btn-cerrar-cancelar-reserva").addEventListener("click", () => {
  cancelarReservaModal.style.display = "none";
});

document.getElementById("btn-confirmar-cancelar-reserva").addEventListener("click", async () => {
  const reservaId = document.getElementById("cancelar-reserva-id").value;
  const errorEl = document.getElementById("cancelar-reserva-error");
  const okEl = document.getElementById("cancelar-reserva-ok");
  const btn = document.getElementById("btn-confirmar-cancelar-reserva");
  errorEl.textContent = "";
  okEl.textContent = "";
  btn.disabled = true;

  try {
    const resp = await fetch(`${API}/reservas/${reservaId}/cancelar`, {
      method: "POST"
    });

    if (!resp.ok) {
      const err = await resp.json().catch(() => ({}));
      errorEl.textContent = err.mensaje || "No se pudo cancelar la reserva.";
      btn.disabled = false;
      return;
    }

    okEl.textContent = "Reserva cancelada correctamente.";
    setTimeout(() => {
      cancelarReservaModal.style.display = "none";
      cargarMisReservas();
    }, 1500);
  } catch (err) {
    errorEl.textContent = "No se pudo conectar con el servidor.";
    btn.disabled = false;
  }
});

// ================= MODAL PAGO =================

const pagoModal = document.createElement("div");
pagoModal.id = "pago-modal";
pagoModal.className = "modal";
pagoModal.style.display = "none";
pagoModal.innerHTML = `
  <div class="modal-content">
    <h3>Pagar Reserva</h3>
    <p id="pago-error" class="error-msg"></p>
    <p id="pago-ok" class="ok-msg"></p>
    <form id="pago-form">
      <input type="hidden" id="pago-reserva-id">

      <label>Monto a pagar</label>
      <input type="text" id="pago-monto" readonly style="background:#f3f4f6;">

      <label for="pago-metodo">Método de pago</label>
      <select id="pago-metodo" required>
        <option value="Tarjeta">💳 Tarjeta de crédito/débito</option>
        <option value="Efectivo">💵 Efectivo</option>
        <option value="Transferencia">🏦 Transferencia bancaria</option>
      </select>

      <button type="submit" class="btn-principal">Confirmar pago</button>
      <button type="button" id="btn-cerrar-pago" class="btn-secundario">Cancelar</button>
    </form>
  </div>
`;
document.body.appendChild(pagoModal);

function abrirModalPago(reservaId, monto) {
  document.getElementById("pago-reserva-id").value = reservaId;
  document.getElementById("pago-monto").value = "$" + Number(monto).toLocaleString("es-CO");
  document.getElementById("pago-error").textContent = "";
  document.getElementById("pago-ok").textContent = "";
  pagoModal.style.display = "flex";
}

document.getElementById("btn-cerrar-pago").addEventListener("click", () => {
  pagoModal.style.display = "none";
});

document.getElementById("pago-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  const reservaId = document.getElementById("pago-reserva-id").value;
  const metodoPago = document.getElementById("pago-metodo").value;
  const montoTexto = document.getElementById("pago-monto").value.replace(/[^0-9]/g, "");
  const monto = Number(montoTexto);

  const errorEl = document.getElementById("pago-error");
  const okEl = document.getElementById("pago-ok");
  errorEl.textContent = "";
  okEl.textContent = "";

  try {
    const resp = await fetch(`${API}/pagos`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ reservaId, monto, metodoPago })
    });

    if (!resp.ok) {
      const err = await resp.json().catch(() => ({}));
      errorEl.textContent = err.mensaje || "No se pudo procesar el pago";
      return;
    }

    okEl.textContent = "¡Pago confirmado! Tu reserva está activa y ganaste 50 puntos 🎉";

    // Actualizar puntos en la UI
    const respPuntos = await fetch(`${API}/clientes/${usuarioActual.clienteId}/puntos`);
    if (respPuntos.ok) {
      const puntosInfo = await respPuntos.json();
      usuarioActual.puntos = puntosInfo.puntosTotales - puntosInfo.puntosCanjeados;
      guardarSesion(usuarioActual);
      document.getElementById("cliente-puntos").textContent = usuarioActual.puntos + " puntos";
    }

    setTimeout(() => {
      pagoModal.style.display = "none";
      cargarMisReservas();
    }, 2500);
  } catch (err) {
    errorEl.textContent = "No se pudo conectar con el servidor.";
  }
});

// ================= MODAL RESEÑA =================

const resenaModal = document.createElement("div");
resenaModal.id = "resena-modal";
resenaModal.className = "modal";
resenaModal.style.display = "none";
resenaModal.innerHTML = `
  <div class="modal-content">
    <h3>⭐ Dejar Reseña</h3>
    <p id="resena-error" class="error-msg"></p>
    <p id="resena-ok" class="ok-msg"></p>
    <form id="resena-form">
      <input type="hidden" id="resena-reserva-id">

      <label>Calificación General (1-5) <span class="req">*</span></label>
      <input type="number" id="resena-general" min="1" max="5" required>

      <label>Limpieza (1-5) <span class="req">*</span></label>
      <input type="number" id="resena-limpieza" min="1" max="5" required>

      <label>Atención (1-5) <span class="req">*</span></label>
      <input type="number" id="resena-atencion" min="1" max="5" required>

      <label>Desayuno (1-5) <span class="req">*</span></label>
      <input type="number" id="resena-desayuno" min="1" max="5" required>

      <label>Instalaciones (1-5) <span class="req">*</span></label>
      <input type="number" id="resena-instalaciones" min="1" max="5" required>

      <label>Comentario (opcional)</label>
      <textarea id="resena-comentario" rows="3" placeholder="Cuéntanos tu experiencia..."></textarea>

      <button type="submit" class="btn-principal">Enviar reseña</button>
      <button type="button" id="btn-cerrar-resena" class="btn-secundario">Cancelar</button>
    </form>
  </div>
`;
document.body.appendChild(resenaModal);

function abrirModalResena(reservaId) {
  document.getElementById("resena-reserva-id").value = reservaId;
  document.getElementById("resena-error").textContent = "";
  document.getElementById("resena-ok").textContent = "";
  document.getElementById("resena-form").reset();
  document.getElementById("resena-reserva-id").value = reservaId;
  resenaModal.style.display = "flex";
}

document.getElementById("btn-cerrar-resena").addEventListener("click", () => {
  resenaModal.style.display = "none";
});

document.getElementById("resena-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  const reservaId = document.getElementById("resena-reserva-id").value;
  const califGeneral = Number(document.getElementById("resena-general").value);
  const califLimpieza = Number(document.getElementById("resena-limpieza").value);
  const califAtencion = Number(document.getElementById("resena-atencion").value);
  const califDesayuno = Number(document.getElementById("resena-desayuno").value);
  const califInstalaciones = Number(document.getElementById("resena-instalaciones").value);
  const comentario = document.getElementById("resena-comentario").value;

  const errorEl = document.getElementById("resena-error");
  const okEl = document.getElementById("resena-ok");
  errorEl.textContent = "";
  okEl.textContent = "";

  try {
    const resp = await fetch(`${API}/resenas`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        clienteId: usuarioActual.clienteId,
        reservaId: Number(reservaId),
        califGeneral,
        califLimpieza,
        califAtencion,
        califDesayuno,
        califInstalaciones,
        comentario
      })
    });

    if (!resp.ok) {
      const err = await resp.json().catch(() => ({}));
      errorEl.textContent = err.mensaje || "No se pudo enviar la reseña";
      return;
    }

    okEl.textContent = "¡Gracias por tu reseña! Ganaste 20 puntos 🎉";

    // Actualizar puntos
    const respPuntos = await fetch(`${API}/clientes/${usuarioActual.clienteId}/puntos`);
    if (respPuntos.ok) {
      const puntosInfo = await respPuntos.json();
      usuarioActual.puntos = puntosInfo.puntosTotales - puntosInfo.puntosCanjeados;
      guardarSesion(usuarioActual);
      document.getElementById("cliente-puntos").textContent = usuarioActual.puntos + " puntos";
    }

    setTimeout(() => {
      resenaModal.style.display = "none";
      cargarMisReservas();
    }, 2500);
  } catch (err) {
    errorEl.textContent = "No se pudo conectar con el servidor.";
  }
});

// ================= CANJE DE PUNTOS =================

document.getElementById("cliente-puntos").insertAdjacentHTML("afterend", `
  <button id="btn-canjear-puntos" class="btn-puntos">🎁 Canjear</button>
`);

document.getElementById("btn-canjear-puntos").addEventListener("click", abrirModalCanjePuntos);

const canjePuntosModal = document.createElement("div");
canjePuntosModal.id = "canje-puntos-modal";
canjePuntosModal.className = "modal";
canjePuntosModal.style.display = "none";
canjePuntosModal.innerHTML = `
  <div class="modal-content">
    <h3>🎁 Canjear Puntos por Descuento</h3>
    <div id="canje-info"></div>
    <p id="canje-error" class="error-msg"></p>
    <p id="canje-ok" class="ok-msg"></p>
    <div id="niveles-list"></div>
    <button type="button" id="btn-cerrar-canje" class="btn-secundario" style="margin-top:16px;">Cerrar</button>
  </div>
`;
document.body.appendChild(canjePuntosModal);

document.getElementById("btn-cerrar-canje").addEventListener("click", () => {
  canjePuntosModal.style.display = "none";
});

async function abrirModalCanjePuntos() {
  document.getElementById("canje-error").textContent = "";
  document.getElementById("canje-ok").textContent = "";
  document.getElementById("canje-info").innerHTML = "Cargando...";
  document.getElementById("niveles-list").innerHTML = "";
  canjePuntosModal.style.display = "flex";

  try {
    const resp = await fetch(`${API}/clientes/${usuarioActual.clienteId}/puntos/niveles`);
    const data = await resp.json();

    const puntosDisp = data.puntosDisponibles;
    const proximoCanje = data.proximoCanjeDisponible;
    const cooldownActivo = proximoCanje !== "Disponible ahora";

    let infoHtml = `<div class="canje-puntos-resumen"><strong>${puntosDisp} puntos disponibles</strong></div>`;
    if (cooldownActivo) {
      const diasRestantes = Math.max(1, Math.ceil((new Date(proximoCanje) - new Date()) / (1000 * 60 * 60 * 24)));
      infoHtml += `<p class="limite-pago">⏰ Próximo canje disponible en ${diasRestantes} día${diasRestantes !== 1 ? "s" : ""}</p>`;
    }

    document.getElementById("canje-info").innerHTML = infoHtml;

    document.getElementById("niveles-list").innerHTML = data.niveles.map(n => `
      <div class="nivel-card ${n.disponible && !cooldownActivo ? "" : "nivel-bloqueado"}">
        <h4>${n.nombre}</h4>
        <p class="nivel-descuento">${n.descuento}</p>
        <p>${n.puntosRequeridos} puntos</p>
        <button
          class="btn-canjear-nivel"
          data-nivel="${n.nivel}"
          ${!n.disponible || cooldownActivo ? "disabled" : ""}
        >
          ${!n.disponible ? "Sin puntos" : cooldownActivo ? "En cooldown" : "Canjear"}
        </button>
      </div>
    `).join("");

    document.querySelectorAll(".btn-canjear-nivel").forEach(btn => {
      btn.addEventListener("click", () => canjearNivel(Number(btn.dataset.nivel)));
    });
  } catch (err) {
    document.getElementById("canje-error").textContent = "No se pudo cargar la información de puntos.";
  }
}

async function canjearNivel(nivel) {
  const errorEl = document.getElementById("canje-error");
  const okEl = document.getElementById("canje-ok");
  errorEl.textContent = "";
  okEl.textContent = "";

  try {
    const resp = await fetch(`${API}/clientes/${usuarioActual.clienteId}/puntos/canjear-descuento`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ nivel })
    });

    if (!resp.ok) {
      const err = await resp.json().catch(() => ({}));
      errorEl.textContent = err.mensaje || "No se pudo canjear el descuento";
      return;
    }

    const data = await resp.json();
    okEl.textContent = `✅ Descuento del ${data.porcentaje} guardado. Se aplicará en tu próxima reserva. Te quedan ${data.puntosRestantes} puntos.`;

    // Actualizar puntos en la UI
    const respPuntos = await fetch(`${API}/clientes/${usuarioActual.clienteId}/puntos`);
    if (respPuntos.ok) {
      const puntosInfo = await respPuntos.json();
      usuarioActual.puntos = puntosInfo.puntosTotales - puntosInfo.puntosCanjeados;
      guardarSesion(usuarioActual);
      document.getElementById("cliente-puntos").textContent = usuarioActual.puntos + " puntos";
    }

    setTimeout(() => abrirModalCanjePuntos(), 2500);
  } catch (err) {
    errorEl.textContent = "No se pudo conectar con el servidor.";
  }
}


/* ================================================
   DASHBOARD ADMIN — AGREGAR AL FINAL DE app.js
   ================================================ */

// ================= DASHBOARD ADMIN =================

async function cargarDashboard() {
  const menuGrid = document.getElementById("admin-menu-grid");
  
  // Agregar tarjeta de Dashboard al menú si no existe
  if (!document.getElementById("dashboard-card")) {
    const dashCard = document.createElement("button");
    dashCard.className = "admin-menu-card";
    dashCard.id = "dashboard-card";
    dashCard.innerHTML = `
      <span class="admin-menu-icono">📊</span>
      <span class="admin-menu-titulo">Dashboard</span>
      <span class="admin-menu-desc">Métricas y resumen general</span>
    `;
    menuGrid.insertBefore(dashCard, menuGrid.firstChild);
    dashCard.addEventListener("click", mostrarDashboard);
  }
}

async function mostrarDashboard() {
  document.getElementById("admin-menu-section").style.display = "none";
  document.getElementById("admin-tabla-section").style.display = "none";

  let dashSection = document.getElementById("admin-dashboard-section");
  if (!dashSection) {
    dashSection = document.createElement("section");
    dashSection.id = "admin-dashboard-section";
    dashSection.className = "admin-contenido";
    document.getElementById("admin-section").appendChild(dashSection);
  }

  dashSection.style.display = "block";
  dashSection.innerHTML = `
    <div class="admin-tabla-header">
      <h1>📊 Dashboard</h1>
      <button id="btn-volver-menu-dashboard" class="btn-secundario">← Volver al panel</button>
    </div>
    <p id="dashboard-msg" style="color:#6b7280;">Cargando métricas...</p>
    <div id="dashboard-content"></div>
  `;

  document.getElementById("btn-volver-menu-dashboard").addEventListener("click", () => {
    dashSection.style.display = "none";
    document.getElementById("admin-menu-section").style.display = "block";
  });

  try {
    const resp = await fetch(`${API}/dashboard`);
    if (!resp.ok) throw new Error("Error al cargar dashboard");
    const d = await resp.json();

    document.getElementById("dashboard-msg").textContent = "";
    document.getElementById("dashboard-content").innerHTML = renderDashboard(d);
  } catch (err) {
    document.getElementById("dashboard-msg").textContent = "No se pudo cargar el dashboard.";
  }
}

function renderDashboard(d) {
  const fmt = n => Number(n || 0).toLocaleString("es-CO");
  const pesos = n => "$" + fmt(n);

  // Alertas
  const alertasHtml = d.alertas && d.alertas.length
    ? d.alertas.map(a => `<div class="dash-alerta">${a}</div>`).join("")
    : `<div class="dash-alerta dash-alerta-ok">✅ Todo en orden, sin alertas activas</div>`;

  return `
    <!-- Alertas -->
    <div class="dash-seccion">
      <h2 class="dash-titulo">⚠️ Alertas</h2>
      <div class="dash-alertas-list">${alertasHtml}</div>
    </div>

    <!-- KPIs principales -->
    <div class="dash-seccion">
      <h2 class="dash-titulo">📈 Resumen General</h2>
      <div class="dash-grid">
        <div class="dash-card dash-card-azul">
          <div class="dash-card-valor">${d.porcentajeOcupacion}%</div>
          <div class="dash-card-label">Ocupación actual</div>
        </div>
        <div class="dash-card dash-card-verde">
          <div class="dash-card-valor">${pesos(d.ingresosMes)}</div>
          <div class="dash-card-label">Ingresos este mes</div>
        </div>
        <div class="dash-card dash-card-morado">
          <div class="dash-card-valor">${pesos(d.ingresosTotal)}</div>
          <div class="dash-card-label">Ingresos totales</div>
        </div>
        <div class="dash-card dash-card-naranja">
          <div class="dash-card-valor">${d.calificacionPromedio != null ? Number(d.calificacionPromedio).toFixed(1) + " ⭐" : "Sin datos"}</div>
          <div class="dash-card-label">Calificación promedio</div>
        </div>
      </div>
    </div>

    <!-- Habitaciones -->
    <div class="dash-seccion">
      <h2 class="dash-titulo">🛏️ Habitaciones (${d.totalHabitaciones} total)</h2>
      <div class="dash-grid">
        <div class="dash-card dash-card-verde">
          <div class="dash-card-valor">${d.habitacionesDisponibles}</div>
          <div class="dash-card-label">Disponibles</div>
        </div>
        <div class="dash-card dash-card-azul">
          <div class="dash-card-valor">${d.habitacionesOcupadas}</div>
          <div class="dash-card-label">Ocupadas</div>
        </div>
        <div class="dash-card dash-card-amarillo">
          <div class="dash-card-valor">${d.habitacionesReservadas}</div>
          <div class="dash-card-label">Reservadas</div>
        </div>
        <div class="dash-card dash-card-gris">
          <div class="dash-card-valor">${d.habitacionesPendienteLimpieza + d.habitacionesEnLimpieza}</div>
          <div class="dash-card-label">En limpieza</div>
        </div>
        <div class="dash-card dash-card-rojo">
          <div class="dash-card-valor">${d.habitacionesFueraServicio}</div>
          <div class="dash-card-label">Fuera de servicio</div>
        </div>
      </div>
    </div>

    <!-- Reservas -->
    <div class="dash-seccion">
      <h2 class="dash-titulo">📅 Reservas (${d.totalReservas} total)</h2>
      <div class="dash-grid">
        <div class="dash-card dash-card-amarillo">
          <div class="dash-card-valor">${d.reservasPendientes}</div>
          <div class="dash-card-label">Pendientes pago</div>
        </div>
        <div class="dash-card dash-card-verde">
          <div class="dash-card-valor">${d.reservasConfirmadas}</div>
          <div class="dash-card-label">Confirmadas</div>
        </div>
        <div class="dash-card dash-card-azul">
          <div class="dash-card-valor">${d.reservasEnCurso}</div>
          <div class="dash-card-label">En curso</div>
        </div>
        <div class="dash-card dash-card-gris">
          <div class="dash-card-valor">${d.reservasFinalizadas}</div>
          <div class="dash-card-label">Finalizadas</div>
        </div>
        <div class="dash-card dash-card-rojo">
          <div class="dash-card-valor">${d.reservasCanceladas}</div>
          <div class="dash-card-label">Canceladas</div>
        </div>
      </div>
    </div>

    <!-- Clientes -->
    <div class="dash-seccion">
      <h2 class="dash-titulo">👤 Clientes</h2>
      <div class="dash-grid">
        <div class="dash-card dash-card-azul">
          <div class="dash-card-valor">${d.totalClientes}</div>
          <div class="dash-card-label">Total clientes</div>
        </div>
        <div class="dash-card dash-card-morado">
          <div class="dash-card-valor">${d.clientesFrecuentes}</div>
          <div class="dash-card-label">Clientes ORO/PLATINUM</div>
        </div>
        <div class="dash-card dash-card-amarillo">
          <div class="dash-card-valor">${d.pagosPendientes}</div>
          <div class="dash-card-label">Pagos pendientes</div>
        </div>
      </div>
    </div>
  `;
}

// Inicializar dashboard al cargar el panel admin
function mostrarMenuAdmin() {
  mostrarMenuAdminBase();
  cargarDashboard();
}
