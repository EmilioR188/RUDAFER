const API_BASE_MOVIMIENTOS = "https://rudafer-api.onrender.com";
let listaMovimientosGlobal = [];
let movimientosFiltrados = [];

document.addEventListener("DOMContentLoaded", () => {
    const selectorSucursal = document.getElementById("selectSucursalGlobal");
    if (selectorSucursal) {
        const sucursalGuardada = localStorage.getItem("rudafer_sucursal_activa");
        if (sucursalGuardada) selectorSucursal.value = sucursalGuardada;
        selectorSucursal.addEventListener("change", cargarMovimientos);
    }

    ["filtroMovimiento", "filtroFechaDesde", "filtroFechaHasta"].forEach(id => {
        const control = document.getElementById(id);
        if (control) control.addEventListener("change", aplicarFiltrosLocales);
    });

    const orden = document.getElementById("ordenMovimientos");
    if (orden) orden.addEventListener("change", aplicarFiltrosLocales);

    const limpiar = document.getElementById("limpiarMovimiento");
    if (limpiar) {
        limpiar.addEventListener("click", () => {
            const tipo = document.getElementById("filtroMovimiento");
            const desde = document.getElementById("filtroFechaDesde");
            const hasta = document.getElementById("filtroFechaHasta");
            if (tipo) tipo.value = "todos";
            if (desde) desde.value = "";
            if (hasta) hasta.value = "";
            aplicarFiltrosLocales();
        });
    }

    cargarMovimientos();
});

async function cargarMovimientos() {
    const selector = document.getElementById("selectSucursalGlobal");
    const idSucursal = selector?.value || localStorage.getItem("rudafer_sucursal_activa") || "1";
    const tbody = document.getElementById("tablaMovimientos");
    if (tbody) tbody.innerHTML = '<tr><td colspan="7">Cargando movimientos...</td></tr>';

    try {
        const response = await fetch(`${API_BASE_MOVIMIENTOS}/api/movimientos?idSucursal=${encodeURIComponent(idSucursal)}`);
        if (!response.ok) throw new Error(`El servidor respondió HTTP ${response.status}`);
        const data = await response.json();

        const resumen = data.resumen || {};
        establecerTexto("resumenVentas", resumen.ventas ?? 0);
        establecerTexto("resumenCompras", resumen.compras ?? 0);
        establecerTexto("resumenTransferencias", resumen.transferencias ?? 0);
        establecerTexto("resumenPagos", resumen.pagos ?? 0);

        listaMovimientosGlobal = Array.isArray(data.lista) ? data.lista : [];
        aplicarFiltrosLocales();
    } catch (error) {
        console.error("Error al cargar movimientos:", error);
        if (tbody) {
            tbody.innerHTML = "";
            const row = tbody.insertRow();
            const cell = row.insertCell();
            cell.colSpan = 7;
            cell.textContent = "No se pudieron cargar los movimientos. Verificá la conexión con el servidor.";
        }
        establecerTexto("infoConteoMovimientos", "Error al cargar movimientos");
    }
}

function aplicarFiltrosLocales() {
    const tipoFiltro = document.getElementById("filtroMovimiento")?.value || "todos";
    const desdeValor = document.getElementById("filtroFechaDesde")?.value || "";
    const hastaValor = document.getElementById("filtroFechaHasta")?.value || "";
    const desde = desdeValor ? new Date(`${desdeValor}T00:00:00`) : null;
    const hasta = hastaValor ? new Date(`${hastaValor}T23:59:59`) : null;

    movimientosFiltrados = listaMovimientosGlobal.filter(item => {
        const tipo = String(item.tipo || "").toLowerCase();
        if (tipoFiltro !== "todos" && tipo !== tipoFiltro) return false;
        if (desde || hasta) {
            const fechaTexto = String(item.fecha || "").replace(" ", "T");
            const fecha = fechaTexto ? new Date(fechaTexto) : null;
            if (fecha && !Number.isNaN(fecha.getTime())) {
                if (desde && fecha < desde) return false;
                if (hasta && fecha > hasta) return false;
            }
        }
        return true;
    });

    const orden = document.getElementById("ordenMovimientos")?.value || "reciente";
    movimientosFiltrados.sort((a, b) => {
        const fechaA = new Date(String(a.fecha || "").replace(" ", "T")).getTime() || 0;
        const fechaB = new Date(String(b.fecha || "").replace(" ", "T")).getTime() || 0;
        return orden === "antiguo" ? fechaA - fechaB : fechaB - fechaA;
    });
    renderizarTablaMovimientos(movimientosFiltrados);
}

function renderizarTablaMovimientos(movimientos) {
    const tbody = document.getElementById("tablaMovimientos");
    if (!tbody) return;
    tbody.replaceChildren();

    if (!movimientos.length) {
        const row = tbody.insertRow();
        const cell = row.insertCell();
        cell.colSpan = 7;
        cell.textContent = "No hay movimientos registrados para los filtros seleccionados.";
        establecerTexto("infoConteoMovimientos", "0 movimientos encontrados");
        return;
    }

    movimientos.forEach(item => {
        const row = tbody.insertRow();
        const tipo = String(item.tipo || "movimiento").toLowerCase();
        const tipoLabel = tipo.charAt(0).toUpperCase() + tipo.slice(1);
        const cantidad = String(item.cantidad ?? "0");

        agregarCelda(row, formatearFecha(item.fecha));
        const tipoCell = row.insertCell();
        const badge = document.createElement("span");
        badge.className = `movement-type ${tipo}`;
        const dot = document.createElement("span");
        dot.className = "movement-dot";
        badge.append(dot, document.createTextNode(tipoLabel));
        tipoCell.appendChild(badge);

        const detalleCell = row.insertCell();
        const detalle = document.createElement("div");
        detalle.className = "movement-product";
        const strong = document.createElement("strong");
        strong.textContent = item.detalle || "-";
        const category = document.createElement("span");
        category.textContent = item.categoria || "-";
        detalle.append(strong, category);
        detalleCell.appendChild(detalle);

        const cantidadCell = row.insertCell();
        const cantidadStrong = document.createElement("strong");
        cantidadStrong.className = cantidad.includes("-") ? "quantity negative" : "quantity positive";
        cantidadStrong.textContent = cantidad;
        cantidadCell.appendChild(cantidadStrong);

        agregarCelda(row, item.sucursal || "Sucursal Central");
        agregarCelda(row, item.responsable || "Administración");
        agregarCelda(row, item.referencia || "-");
    });

    establecerTexto("infoConteoMovimientos", `Mostrando ${movimientos.length} movimientos`);
}

function agregarCelda(row, value) {
    const cell = row.insertCell();
    cell.textContent = value == null || value === "" ? "-" : String(value);
}

function establecerTexto(id, value) {
    const element = document.getElementById(id);
    if (element) element.textContent = String(value);
}

function formatearFecha(value) {
    if (!value) return "-";
    return String(value).replace("T", " ");
}
