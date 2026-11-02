import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class TransferenciaDAO {
    public static class Resultado {
        public final boolean exito;
        public final int codigoHttp;
        public final String mensaje;
        public Resultado(boolean exito, int codigoHttp, String mensaje) {
            this.exito = exito; this.codigoHttp = codigoHttp; this.mensaje = mensaje;
        }
    }

    public Resultado crearDesdeJson(String body) {
        int origen, destino, empleado;
        JsonArray items;
        try {
            JsonObject obj = JsonParser.parseString(body).getAsJsonObject();
            origen = obj.get("idSucursalOrigen").getAsInt();
            destino = obj.get("idSucursalDestino").getAsInt();
            empleado = obj.has("idEmpleado") ? obj.get("idEmpleado").getAsInt() : 0;
            items = obj.getAsJsonArray("items");
        } catch (Exception e) { return new Resultado(false, 400, "El cuerpo de la transferencia no tiene un formato válido."); }

        if (origen <= 0 || destino <= 0 || origen == destino)
            return new Resultado(false, 400, "Seleccioná dos sucursales diferentes.");
        if (empleado <= 0) return new Resultado(false, 400, "No se pudo identificar al empleado que realiza la transferencia. Cerrá sesión e ingresá nuevamente.");
        if (items == null || items.size() == 0) return new Resultado(false, 400, "La transferencia debe incluir al menos un producto.");

        try (Connection con = ConexionBD.obtenerConexion()) {
            con.setAutoCommit(false);
            try {
                validarExiste(con, "SELECT COUNT(*) FROM sucursal WHERE ID_Sucursal = ?", origen, "La sucursal de origen no existe.");
                validarExiste(con, "SELECT COUNT(*) FROM sucursal WHERE ID_Sucursal = ?", destino, "La sucursal de destino no existe.");
                validarExiste(con, "SELECT COUNT(*) FROM empleados WHERE ID_Empleado = ? AND Activo = 1", empleado, "El empleado no existe o está inactivo.");

                int idTransferencia;
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO transferencias (ID_Sucursal_Origen, ID_Sucursal_Destino, ID_Empleado, Estado, Observacion) VALUES (?, ?, ?, 'COMPLETADA', 'Transferencia de stock')",
                        PreparedStatement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, origen); ps.setInt(2, destino); ps.setInt(3, empleado); ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (!rs.next()) throw new SQLException("No se pudo generar el número de transferencia.");
                        idTransferencia = rs.getInt(1);
                    }
                }

                String detalleJson = JsonParser.parseString(items.toString()).toString();
                for (JsonElement element : items) {
                    if (!element.isJsonObject()) throw new IllegalArgumentException("Hay un artículo con formato inválido.");
                    JsonObject item = element.getAsJsonObject();
                    int idProducto = item.get("idProducto").getAsInt();
                    int cantidad = item.get("cantidad").getAsInt();
                    if (idProducto <= 0 || cantidad <= 0) throw new IllegalArgumentException("Los productos y cantidades deben ser válidos.");

                    try (PreparedStatement ps = con.prepareStatement(
                            "UPDATE stock_sucursal SET Cantidad = Cantidad - ? WHERE ID_Sucursal = ? AND ID_Producto = ? AND Cantidad >= ?")) {
                        ps.setInt(1, cantidad); ps.setInt(2, origen); ps.setInt(3, idProducto); ps.setInt(4, cantidad);
                        if (ps.executeUpdate() != 1) throw new IllegalArgumentException("Stock insuficiente o inexistente para el producto ID " + idProducto + " en la sucursal de origen.");
                    }
                    try (PreparedStatement ps = con.prepareStatement(
                            "INSERT INTO stock_sucursal (ID_Sucursal, ID_Producto, Cantidad, Stock_Minimo) VALUES (?, ?, ?, 5) " +
                            "ON DUPLICATE KEY UPDATE Cantidad = Cantidad + VALUES(Cantidad)")) {
                        ps.setInt(1, destino); ps.setInt(2, idProducto); ps.setInt(3, cantidad); ps.executeUpdate();
                    }
                    try (PreparedStatement ps = con.prepareStatement(
                            "INSERT INTO detalles_transferencia (ID_Transferencia, ID_Producto, Cantidad) VALUES (?, ?, ?)")) {
                        ps.setInt(1, idTransferencia); ps.setInt(2, idProducto); ps.setInt(3, cantidad); ps.executeUpdate();
                    }
                }

                // La tabla historial_pedidos es la fuente del listado visual de Pedidos.
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO historial_pedidos (proveedor, detalle_pedido, total, estado, ID_Sucursal, tipo, ID_Sucursal_Origen, ID_Sucursal_Destino) " +
                        "VALUES (?, ?, 0, 'entregado', ?, 'transferencia', ?, ?)")) {
                    ps.setString(1, "Transferencia #" + idTransferencia);
                    ps.setString(2, detalleJson);
                    ps.setInt(3, origen); ps.setInt(4, origen); ps.setInt(5, destino); ps.executeUpdate();
                }
                con.commit();
                return new Resultado(true, 201, "Transferencia " + idTransferencia + " realizada correctamente.");
            } catch (IllegalArgumentException e) {
                con.rollback(); return new Resultado(false, 409, e.getMessage());
            } catch (SQLException e) {
                con.rollback(); throw e;
            }
        } catch (SQLException e) {
            System.err.println("Error al registrar transferencia: " + e.getMessage());
            return new Resultado(false, 500, "No se pudo completar la transferencia. No se modificó el stock.");
        }
    }

    private static void validarExiste(Connection con, String sql, int id, String mensaje) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || rs.getLong(1) == 0) throw new IllegalArgumentException(mensaje);
            }
        }
    }
}
