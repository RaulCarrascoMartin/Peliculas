"use strict";

const $ = (sel, ctx = document) => ctx.querySelector(sel);
const TAM_PAGINA = 6;

const estado = {
  token: localStorage.getItem("token"),
  favoritas: new Set(),
  vistas: new Set(),
  pagina: 0,
  filtros: {},
  editando: null,
};

// ---------- utilidades ----------

function datosToken() {
  if (!estado.token) return null;
  try {
    const carga = JSON.parse(atob(estado.token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/")));
    return { id: carga.sub, rol: carga.rol, exp: carga.exp * 1000 };
  } catch {
    return null;
  }
}

function esAdmin() {
  return datosToken()?.rol === "ADMIN";
}

function aviso(mensaje, error = false) {
  const caja = $("#aviso");
  caja.textContent = mensaje;
  caja.className = error ? "error" : "";
  caja.hidden = false;
  clearTimeout(aviso.temporizador);
  aviso.temporizador = setTimeout(() => (caja.hidden = true), 4000);
}

/** Llama a la API con el token; si falla, muestra el mensaje del servidor y lanza el error. */
async function api(ruta, { metodo = "GET", cuerpo } = {}) {
  const cabeceras = {};
  if (estado.token) cabeceras.Authorization = `Bearer ${estado.token}`;
  if (cuerpo !== undefined) cabeceras["Content-Type"] = "application/json";
  const respuesta = await fetch(ruta, {
    method: metodo,
    headers: cabeceras,
    body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo),
  });
  if (respuesta.status === 204) return null;
  const texto = await respuesta.text();
  const datos = texto ? JSON.parse(texto) : null;
  if (!respuesta.ok) {
    if (respuesta.status === 401 && estado.token) cerrarSesion(false);
    const detalle = datos?.errores && Object.keys(datos.errores).length
      ? " (" + Object.entries(datos.errores).map(([c, m]) => `${c}: ${m}`).join(", ") + ")"
      : "";
    const mensaje = `${respuesta.status} · ${datos?.mensaje ?? "Error"}${detalle}`;
    aviso(mensaje, true);
    throw new Error(mensaje);
  }
  return datos;
}

function escapar(texto) {
  const nodo = document.createElement("div");
  nodo.textContent = texto ?? "";
  return nodo.innerHTML;
}

// ---------- sesión ----------

function pintarSesion() {
  const datos = datosToken();
  const caja = $("#sesion");
  if (datos) {
    caja.innerHTML = `Usuario <b>#${escapar(datos.id)}</b> · ${escapar(datos.rol)} <button class="secundario" id="salir">Salir</button>`;
    $("#salir").onclick = () => cerrarSesion(true);
  } else {
    caja.textContent = "Sin sesión";
  }
  $("#acceso").hidden = !!datos;
  document.querySelectorAll("[data-requiere-sesion]").forEach((b) => (b.hidden = !datos));
  $("#form-pelicula").hidden = !esAdmin();
}

function guardarToken(token) {
  estado.token = token;
  localStorage.setItem("token", token);
}

function cerrarSesion(conAviso) {
  estado.token = null;
  estado.favoritas.clear();
  estado.vistas.clear();
  localStorage.removeItem("token");
  pintarSesion();
  mostrarVista("catalogo");
  if (conAviso) aviso("Sesión cerrada");
  else aviso("La sesión ha caducado: vuelve a entrar", true);
}

$("#form-acceso").addEventListener("submit", async (e) => {
  e.preventDefault();
  const accion = e.submitter?.dataset.accion ?? "login";
  const datos = Object.fromEntries(new FormData(e.target));
  try {
    if (accion === "registro") {
      await api("/api/auth/registro", { metodo: "POST", cuerpo: datos });
      aviso("Cuenta creada, entrando…");
    }
    const { token } = await api("/api/auth/login", { metodo: "POST", cuerpo: datos });
    guardarToken(token);
    e.target.reset();
    await iniciar();
  } catch { /* el aviso ya se ha mostrado */ }
});

// ---------- listas del usuario ----------

async function cargarMarcas() {
  if (!estado.token) return;
  try {
    const [fav, vis] = await Promise.all([api("/api/usuarios/me/favoritas"), api("/api/usuarios/me/vistas")]);
    estado.favoritas = new Set(fav.map((p) => p.id));
    estado.vistas = new Set(vis.map((p) => p.id));
  } catch { /* sesión inválida: api() ya la cierra */ }
}

async function alternarMarca(id, lista) {
  const conjunto = lista === "favorita" ? estado.favoritas : estado.vistas;
  const yaMarcada = conjunto.has(id);
  await api(`/api/peliculas/${id}/${lista}`, { metodo: yaMarcada ? "DELETE" : "PUT" });
  if (yaMarcada) conjunto.delete(id); else conjunto.add(id);
  aviso(yaMarcada ? `Quitada de ${lista}s` : `Añadida a ${lista}s`);
  await refrescar();
}

// ---------- tarjetas ----------

function tarjeta(p, { acciones = true } = {}) {
  const media = p.valoracionMedia == null ? "Sin valoraciones" : `★ ${p.valoracionMedia.toFixed(2)}`;
  const nodo = document.createElement("article");
  nodo.className = "tarjeta";
  nodo.innerHTML = `
    <h3>${escapar(p.titulo)}</h3>
    <div class="meta">${escapar(p.anio)} · ${escapar(p.genero)} · ${escapar(p.director)}</div>
    <div class="media">${media}</div>`;
  if (!acciones) return nodo;

  const caja = document.createElement("div");
  caja.className = "acciones";
  if (estado.token) {
    for (const [lista, icono] of [["favorita", "♥"], ["vista", "👁"]]) {
      const activa = (lista === "favorita" ? estado.favoritas : estado.vistas).has(p.id);
      const boton = document.createElement("button");
      boton.textContent = `${icono} ${lista}`;
      boton.className = activa ? "activo" : "secundario";
      boton.onclick = () => alternarMarca(p.id, lista).catch(() => {});
      caja.append(boton);
    }
    const form = document.createElement("form");
    form.innerHTML = `
      <select name="puntuacion" aria-label="Puntuación">
        ${[5, 4, 3, 2, 1].map((n) => `<option value="${n}">${"★".repeat(n)}</option>`).join("")}
      </select>
      <input type="text" name="comentario" placeholder="Comentario">
      <button type="submit">Valorar</button>`;
    form.onsubmit = async (e) => {
      e.preventDefault();
      const datos = new FormData(form);
      try {
        await api(`/api/peliculas/${p.id}/valoracion`, {
          metodo: "PUT",
          cuerpo: { puntuacion: Number(datos.get("puntuacion")), comentario: datos.get("comentario") || null },
        });
        aviso("Valoración guardada");
        await refrescar();
      } catch { /* aviso mostrado */ }
    };
    caja.append(form);
  }
  if (esAdmin()) {
    const editar = document.createElement("button");
    editar.textContent = "Editar";
    editar.className = "secundario";
    editar.onclick = () => empezarEdicion(p);
    const borrar = document.createElement("button");
    borrar.textContent = "Eliminar";
    borrar.className = "peligro";
    borrar.onclick = async () => {
      if (!confirm(`¿Eliminar "${p.titulo}" con sus valoraciones y marcas?`)) return;
      try {
        await api(`/api/peliculas/${p.id}`, { metodo: "DELETE" });
        aviso("Película eliminada");
        await cargarMarcas();
        await refrescar();
      } catch { /* aviso mostrado */ }
    };
    caja.append(editar, borrar);
  }
  nodo.append(caja);
  return nodo;
}

// ---------- catálogo ----------

async function cargarCatalogo() {
  const params = new URLSearchParams({ page: estado.pagina, size: TAM_PAGINA, ...estado.filtros });
  const { content, page } = await api(`/api/peliculas?${params}`);
  const lista = $("#lista");
  lista.replaceChildren();
  if (!content.length) {
    lista.innerHTML = '<div class="vacio">No hay películas que coincidan.</div>';
  } else {
    content.forEach((p) => lista.append(tarjeta(p)));
  }
  const paginacion = $("#paginacion");
  paginacion.replaceChildren();
  if (page.totalPages > 1) {
    const anterior = Object.assign(document.createElement("button"), { textContent: "←", disabled: page.number === 0 });
    const siguiente = Object.assign(document.createElement("button"), { textContent: "→", disabled: page.number + 1 >= page.totalPages });
    anterior.onclick = () => { estado.pagina--; cargarCatalogo().catch(() => {}); };
    siguiente.onclick = () => { estado.pagina++; cargarCatalogo().catch(() => {}); };
    const texto = document.createElement("span");
    texto.textContent = `Página ${page.number + 1} de ${page.totalPages} (${page.totalElements})`;
    paginacion.append(anterior, texto, siguiente);
  }
}

$("#form-filtros").addEventListener("submit", (e) => {
  e.preventDefault();
  estado.filtros = Object.fromEntries([...new FormData(e.target)].filter(([, v]) => v.trim() !== ""));
  estado.pagina = 0;
  cargarCatalogo().catch(() => {});
});
$("#form-filtros").addEventListener("reset", () => {
  estado.filtros = {};
  estado.pagina = 0;
  setTimeout(() => cargarCatalogo().catch(() => {}));
});

// ---------- alta y edición (administrador) ----------

function empezarEdicion(p) {
  estado.editando = p.id;
  const form = $("#form-pelicula");
  for (const campo of ["titulo", "anio", "genero", "director"]) form.elements[campo].value = p[campo];
  $("#titulo-form-pelicula").textContent = `Editando "${p.titulo}"`;
  $("#cancelar-edicion").hidden = false;
  form.scrollIntoView({ behavior: "smooth" });
}

function terminarEdicion() {
  estado.editando = null;
  $("#form-pelicula").reset();
  $("#titulo-form-pelicula").textContent = "Nueva película";
  $("#cancelar-edicion").hidden = true;
}

$("#cancelar-edicion").onclick = terminarEdicion;

$("#form-pelicula").addEventListener("submit", async (e) => {
  e.preventDefault();
  const datos = Object.fromEntries(new FormData(e.target));
  datos.anio = Number(datos.anio);
  const edicion = estado.editando !== null;
  try {
    await api(edicion ? `/api/peliculas/${estado.editando}` : "/api/peliculas", {
      metodo: edicion ? "PUT" : "POST",
      cuerpo: datos,
    });
    aviso(edicion ? "Película actualizada" : "Película creada");
    terminarEdicion();
    await refrescar();
  } catch { /* aviso mostrado */ }
});

// ---------- vistas ----------

async function cargarMarcadas(lista) {
  const { favoritas, vistas } = { favoritas: "favoritas", vistas: "vistas" };
  const datos = await api(`/api/usuarios/me/${lista === "favoritas" ? favoritas : vistas}`);
  $("#titulo-marcadas").textContent = lista === "favoritas" ? "♥ Mis favoritas" : "👁 Mis vistas";
  const caja = $("#lista-marcadas");
  caja.replaceChildren();
  if (!datos.length) caja.innerHTML = '<div class="vacio">Todavía no hay ninguna.</div>';
  datos.forEach((p) => caja.append(tarjeta(p, { acciones: false })));
}

let vistaActual = "catalogo";

function mostrarVista(vista) {
  vistaActual = vista;
  document.querySelectorAll("#pestanas button").forEach((b) => b.classList.toggle("activa", b.dataset.vista === vista));
  $("#vista-catalogo").hidden = vista !== "catalogo";
  $("#vista-marcadas").hidden = vista === "catalogo";
  refrescar();
}

async function refrescar() {
  try {
    if (vistaActual === "catalogo") await cargarCatalogo();
    else await cargarMarcadas(vistaActual);
  } catch { /* aviso mostrado */ }
}

document.querySelectorAll("#pestanas button").forEach((b) => (b.onclick = () => mostrarVista(b.dataset.vista)));

// ---------- arranque ----------

async function iniciar() {
  const datos = datosToken();
  if (datos && datos.exp < Date.now()) {
    localStorage.removeItem("token");
    estado.token = null;
  }
  pintarSesion();
  await cargarMarcas();
  pintarSesion();
  mostrarVista("catalogo");
}

iniciar();
