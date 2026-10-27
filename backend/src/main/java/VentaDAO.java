import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;

public class VentaDAO {

    public boolean procesarVenta(String jsonItems, double total, int idPago, int cuotas, int idEmpleado, int idSucursal) {
        Connection con = null;
        try {
            con = ConexionBD.obtenerConexion();
            con.setAutoCommit(false); // Transacción segura

            if (jsonItems == null || jsonItems.trim().isEmpty() || jsonItems.equals("[]")) {
                return false;
            }

            if (idSucursal <= 0) idSucursal = 1;
            if (idEmpleado <= 0) idEmpleado = 1;
            if (idPago <= 0) idPago = 1;
            if (cuotas <= 0) cuotas = 1;

            String limpio = jsonItems.replace("[", "").replace("]", "").trim();
            String[] objetos = limpio.split("\\},\\s*\\{");

            int[] idsProductos = new int[objetos.length];
            int[] cantidades = new int[objetos.length];
            double[] precios = new double[objetos.length];
            int contadorItems = 0;

            for (String obj : objetos) {
                obj = obj.replace("{", "").replace("}", "").replace("\"", "");
                String[] pares = obj.split(",");
                
                int idProducto = 0;
                int cantidadVendida = 0;
                double precioUnitario = 0.0;

                for (String par : pares) {
                    String[] keyValue = par.split(":");
                    if (keyValue.length >= 2) {
                        String clave = keyValue[0].trim();
                        String valor = keyValue[1].trim();
                        if (clave.equals("idProducto")) {
                            idProducto = Integer.parseInt(valor);
                        } else if (clave.equals("cantidad")) {
                            cantidadVendida = Integer.parseInt(valor);
                        } else if (clave.equals("precio")) {
                            precioUnitario = Double.parseDouble(valor);
                        }
                    }
                }

                if (idProducto > 0 && cantidadVendida > 0) {
                    // VALIDACIÓN DE STOCK POR SUCURSAL (Problema 2): Verificar si hay stock suficiente en la sucursal activa
                    String sqlCheckStock = "SELECT Cantidad FROM stock_sucursal WHERE ID_Producto = ? AND ID_Sucursal = ?";
                    try (PreparedStatement psCheck = con.prepareStatement(sqlCheckStock)) {
                        psCheck.setInt(1, idProducto);
                        psCheck.setInt(2, idSucursal);
                        try (ResultSet rsStock = psCheck.executeQuery()) {
                            if (!rsStock.next() || rsStock.getInt("Cantidad") < cantidadVendida) {
                                // Stock insuficiente o el producto no pertenece al inventario de esta sucursal
                                con.rollback();
                                return false;
                            }
                        }
                    }

                    idsProductos[contadorItems] = idProducto;
                    cantidades[contadorItems] = cantidadVendida;
                    precios[contadorItems] = precioUnitario;
                    contadorItems++;
                }
            }

            // 1. Descontar stock de la sucursal correspondiente
            String sqlUpdateStock = "UPDATE stock_sucursal SET Cantidad = Cantidad - ? WHERE ID_Producto = ? AND ID_Sucursal = ?";
            try (PreparedStatement psStock = con.prepareStatement(sqlUpdateStock)) {
                for (int i = 0; i < contadorItems; i++) {
                    psStock.setInt(1, cantidades[i]);
                    psStock.setInt(2, idsProductos[i]);
                    psStock.setInt(3, idSucursal);
                    psStock.addBatch();
                }
                psStock.executeBatch();
            }

            // 2. Obtener porcentaje de comisión del empleado
            double porcentajeComision = 0.0;
            String sqlBuscaEmp = "SELECT Comision FROM empleados WHERE ID_Empleado = ?";
            try (PreparedStatement psEmp = con.prepareStatement(sqlBuscaEmp)) {
                psEmp.setInt(1, idEmpleado);
                try (ResultSet rs = psEmp.executeQuery()) {
                    if (rs.next()) {
                        porcentajeComision = rs.getDouble("Comision");
                    }
                }
            }

            // 3. Registrar la venta vinculada a la sucursal y empleado activos
            String sqlInsertVenta = "INSERT INTO ventas (Total_Venta, Fecha_Hora, ID_Pago, Cuotas, ID_Sucursal, ID_Empleado) VALUES (?, NOW(), ?, ?, ?, ?)";
            int idVentaGenerada = 0;
            
            try (PreparedStatement psVenta = con.prepareStatement(sqlInsertVenta, Statement.RETURN_GENERATED_KEYS)) {
                psVenta.setDouble(1, total);
                psVenta.setInt(2, idPago);
                psVenta.setInt(3, cuotas);
                psVenta.setInt(4, idSucursal);
                psVenta.setInt(5, idEmpleado);
                psVenta.executeUpdate();

                try (ResultSet rsKeys = psVenta.getGeneratedKeys()) {
                    if (rsKeys.next()) {
                        idVentaGenerada = rsKeys.getInt(1);
                    }
                }
            }

            // 4. Registrar los ítems en detalles_venta
            if (idVentaGenerada > 0 && contadorItems > 0) {
                String sqlInsertDetalle = "INSERT INTO detalles_venta (ID_Venta, ID_Producto, Cantidad, Precio_Unitario, Subtotal) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement psDetalle = con.prepareStatement(sqlInsertDetalle)) {
                    for (int i = 0; i < contadorItems; i++) {
                        double subtotalItem = cantidades[i] * precios[i];
                        
                        psDetalle.setInt(1, idVentaGenerada);
                        psDetalle.setInt(2, idsProductos[i]);
                        psDetalle.setInt(3, cantidades[i]);
                        psDetalle.setDouble(4, precios[i]);
                        psDetalle.setDouble(5, subtotalItem);
                        psDetalle.addBatch();
                    }
                    psDetalle.executeBatch();
                }
            }

            // 5. Actualizar estadísticas del empleado (Problema 3: Garantiza el incremento para el Top Vendedores)
            double comisionGanada = (total * porcentajeComision) / 100.0;
            String sqlComision = "UPDATE empleados SET ventas_realizadas = ventas_realizadas + 1, comision_acumulada = comision_acumulada + ? WHERE ID_Empleado = ?";
            
            try (PreparedStatement psCom = con.prepareStatement(sqlComision)) {
                psCom.setDouble(1, comisionGanada);
                psCom.setInt(2, idEmpleado);
                psCom.executeUpdate();
            }

            con.commit();
            return true;

        } catch (Exception e) {
            System.err.println("Error al procesar la venta multisucursal: " + e.getMessage());
            e.printStackTrace();
            if (con != null) {
                try { con.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            return false;
        } finally {
            if (con != null) {
                try { con.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    public boolean procesarCierreDiario(String fechaSql, int idSucursal) {
        boolean usarFecha = (fechaSql != null && !fechaSql.isEmpty());
        if (idSucursal <= 0) idSucursal = 1;
        
        String sqlConsolidado = 
            "INSERT INTO cierre_caja (Fecha, Total_Vendido, Cantidad_Ventas, Unidades_Vendidas, Fecha_Cierre, ID_Sucursal) " +
            "SELECT " +
            "   DATE(v.Fecha_Hora) AS Fecha, " +
            "   COALESCE(SUM(v.Total_Venta), 0) AS Total_Vendido, " +
            "   COUNT(DISTINCT v.ID_Venta) AS Cantidad_Ventas, " +
            "   COALESCE(SUM(dv.Cantidad), 0) AS Unidades_Vendidas, " +
            "   NOW() AS Fecha_Cierre, " +
            "   v.ID_Sucursal " +
            "FROM ventas v " +
            "LEFT JOIN detalles_venta dv ON v.ID_Venta = dv.ID_Venta " +
            "WHERE v.ID_Sucursal = ? " + (usarFecha ? "AND DATE(v.Fecha_Hora) = ? " : "AND DATE(v.Fecha_Hora) = CURDATE() ") +
            "GROUP BY DATE(v.Fecha_Hora), v.ID_Sucursal " +
            "ON DUPLICATE KEY UPDATE " +
            "   Total_Vendido = VALUES(Total_Vendido), " +
            "   Cantidad_Ventas = VALUES(Cantidad_Ventas), " +
            "   Unidades_Vendidas = VALUES(Unidades_Vendidas), " +
            "   Fecha_Cierre = NOW()";

        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sqlConsolidado)) {
            
            ps.setInt(1, idSucursal);
            if (usarFecha) {
                ps.setString(2, fechaSql);
            }
            return ps.executeUpdate() >= 0;
        } catch (SQLException e) {
            System.err.println("Error al procesar el cierre de caja diario por sucursal: " + e.getMessage());
            return false;
        }
    }

    public String obtenerJsonHistorialCierres(int idSucursal) {
        StringBuilder json = new StringBuilder("[");
        if (idSucursal <= 0) idSucursal = 1;

        String sql = "SELECT ID_Cierre, DATE_FORMAT(Fecha, '%d/%m/%Y') AS FechaFormateada, " +
                     "Total_Vendido, Cantidad_Ventas, Unidades_Vendidas, " +
                     "DATE_FORMAT(Fecha_Cierre, '%H:%i:%s') AS HoraCierre " +
                     "FROM cierre_caja WHERE ID_Sucursal = ? ORDER BY Fecha DESC";

        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, idSucursal);
            try (ResultSet rs = ps.executeQuery()) {
                boolean primero = true;
                while (rs.next()) {
                    if (!primero) json.append(",");
                    primero = false;

                    json.append("{")
                        .append("\"idCierre\":").append(rs.getInt("ID_Cierre")).append(",")
                        .append("\"fecha\":\"").append(rs.getString("FechaFormateada")).append("\",")
                        .append("\"totalVendido\":").append(rs.getDouble("Total_Vendido")).append(",")
                        .append("\"cantidadVentas\":").append(rs.getInt("Cantidad_Ventas")).append(",")
                        .append("\"unidadesVendidas\":").append(rs.getInt("Unidades_Vendidas")).append(",")
                        .append("\"horaCierre\":\"").append(rs.getString("HoraCierre")).append("\"")
                        .append("}");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener historial de cierres: " + e.getMessage());
        }

        json.append("]");
        return json.toString();
    }
}
