document.addEventListener('DOMContentLoaded', () => {
    cargarMovimientos();

    // Escuchar cambios en los filtros para recargar dinámicamente
    document.getElementById('sucursalMovimiento').addEventListener('change', cargarMovimientos);
    document.getElementById('filtroMovimiento').addEventListener('change', filtrarTablaLocal);
    document.getElementById('filtroSucursal').addEventListener('change', filtrarTablaLocal);
    document.getElementById('ordenMovimientos').addEventListener('change', ordenarTablaLocal);

    // Botón limpiar filtros
    document.getElementById('limpiarMovimiento').addEventListener('click', () => {
        document.getElementById('filtroMovimiento').value = 'todos';
        document.getElementById('filtroFechaDesde').value = '';
        document.getElementById('filtroFechaHasta').value = '';
        document.getElementById('filtroSucursal').value = 'todas';
        document.getElementById('sucursalMovimiento').value = 'general';
        cargarMovimientos();
    });
});

async function cargarMovimientos() {
    const sucursalSelect = document.getElementById('sucursalMovimiento').value;
    
    try {
        const response = await fetch(`http://localhost:8081/api/movimientos?sucursal=${sucursalSelect}`);
        if (!response.ok) throw new Error('Error al conectar con el servidor');

        const data = await response.json();

        // Actualizar tarjetas de resumen (si el resumen incluye pagos, lo manejamos de forma segura)
       // Actualizar tarjetas de resumen
        const cards = document.querySelectorAll('.movement-summary-card strong');
        if (cards.length >= 4 && data.resumen) {
            cards[0].textContent = data.resumen.ventas;
            cards[1].textContent = data.resumen.compras;
            cards[2].textContent = data.resumen.transferencias;
            cards[3].textContent = data.resumen.pagos || 0;
        }

        // Renderizar la tabla si trae elementos
        if (data.lista && data.lista.length > 0) {
            const tbody = document.getElementById('tablaMovimientos');
            tbody.innerHTML = '';

            data.lista.forEach(item => {
                const esPositivo = !item.cantidad.toString().includes('-');
                const claseCantidad = esPositivo ? 'quantity positive' : 'quantity negative';
                
                // Formatear el texto legible del tipo de movimiento
                let tipoTexto = item.tipo;
                if (tipoTexto === 'venta') tipoTexto = 'Venta';
                else if (tipoTexto === 'compra') tipoTexto = 'Compra';
                else if (tipoTexto === 'pago') tipoTexto = 'Pago';
                else if (tipoTexto === 'transferencia') tipoTexto = 'Transferencia';
                else tipoTexto = item.tipo.charAt(0).toUpperCase() + item.tipo.slice(1);

                tbody.innerHTML += `
                    <tr data-movimiento="${item.tipo}">
                        <td>${item.fecha}</td>
                        <td>
                            <span class="movement-type ${item.tipo}">
                                <span class="movement-dot"></span>
                                ${tipoTexto}
                            </span>
                        </td>
                        <td>
                            <div class="movement-product">
                                <strong>${item.detalle}</strong>
                                <span>${item.categoria}</span>
                            </div>
                        </td>
                        <td><strong class="${claseCantidad}">${item.cantidad}</strong></td>
                        <td>${item.sucursal}</td>
                        <td>${item.responsable}</td>
                        <td>${item.referencia}</td>
                    </tr>
                `;
            });
        }
    } catch (error) {
        console.warn("Aviso: No se pudo conectar con el endpoint de movimientos, operando de forma local.", error);
    }
}

// Filtros locales interactivos para la tabla ya renderizada
function filtrarTablaLocal() {
    const tipoFiltro = document.getElementById('filtroMovimiento').value;
    const sucursalFiltro = document.getElementById('filtroSucursal').value;
    const filas = document.querySelectorAll('#tablaMovimientos tr');

    filas.forEach(fila => {
        const tipoMov = fila.getAttribute('data-movimiento');
        const textoFila = fila.textContent.toLowerCase();
        
        let cumpleTipo = (tipoFiltro === 'todos' || tipoMov === tipoFiltro);
        let cumpleSucursal = (sucursalFiltro === 'todas' || textoFila.includes(sucursalFiltro.toLowerCase()));

        if (cumpleTipo && cumpleSucursal) {
            fila.style.display = '';
        } else {
            fila.style.display = 'none';
        }
    });
}

function ordenarTablaLocal() {
    const orden = document.getElementById('ordenMovimientos').value;
    const tbody = document.getElementById('tablaMovimientos');
    const filasArray = Array.from(tbody.querySelectorAll('tr'));

    filasArray.reverse(); // Invierte el orden visual entre recientes y antiguos

    filasArray.forEach(fila => tbody.appendChild(fila));
}
