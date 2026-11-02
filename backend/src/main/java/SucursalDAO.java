import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SucursalDAO {
    public static class Resultado {
        public final boolean exito;
        public final int codigoHttp;
        public final String mensaje;
        public Resultado(boolean exito, int codigoHttp, String mensaje) {
            this.exito = exito; this.codigoHttp = codigoHttp; this.mensaje = mensaje;
        }
    }

    public String obtenerJsonSucursales() {
        StringBuilder json = new StringBuilder("[{\"id\":0,\"nombre\":\"🌐 Vista General (Todas)\",\"direccion\":\"\",\"telefono\":\"\",\"email\":\"\"}");
        String sql = "SELECT ID_Sucursal, Nombre, Direccion, Telefono, Email FROM sucursal ORDER BY ID_Sucursal";
        try (Connection con = ConexionBD.obtenerConexion(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                json.append(",{")
                    .append("\"id\":").append(rs.getInt("ID_Sucursal")).append(',')
                    .append("\"nombre\":\"").append(esc(rs.getString("Nombre"))).append("\",")
                    .append("\"direccion\":\"").append(esc(rs.getString("Direccion"))).append("\",")
                    .append("\"telefono\":\"").append(esc(rs.getString("Telefono"))).append("\",")
                    .append("\"email\":\"").append(esc(rs.getString("Email"))).append("\"")
                    .append('}');
            }
        } catch (SQLException e) { throw new RuntimeException("No se pudieron consultar las sucursales", e); }
        return json.append(']').toString();
    }

    public Resultado crear(String nombre, String direccion, String telefono, String email) {
        if (nombre == null || nombre.isBlank()) return new Resultado(false, 400, "El nombre de la sucursal es obligatorio.");
        try (Connection con = ConexionBD.obtenerConexion()) {
            con.setAutoCommit(false);
            try {
                int id;
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO sucursal (Nombre, Direccion, Telefono, Email) VALUES (?, ?, ?, ?)",
                        PreparedStatement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, nombre.trim()); ps.setString(2, nvl(direccion));
                    ps.setString(3, nvl(telefono)); ps.setString(4, nvl(email));
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (!rs.next()) throw new SQLException("No se obtuvo el ID de la sucursal.");
                        id = rs.getInt(1);
                    }
                }
                // Se crea inventario inicial en cero para cada producto ya existente.
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO stock_sucursal (ID_Sucursal, ID_Producto, Cantidad, Stock_Minimo) " +
                        "SELECT ?, p.ID_Producto, 0, 5 FROM productos p ON DUPLICATE KEY UPDATE ID_Producto = VALUES(ID_Producto)")) {
                    ps.setInt(1, id); ps.executeUpdate();
                }
                con.commit();
                return new Resultado(true, 201, "Sucursal creada correctamente.");
            } catch (SQLException e) { con.rollback(); throw e; }
        } catch (SQLException e) {
            System.err.println("Error al crear sucursal: " + e.getMessage());
            return new Resultado(false, 500, "No se pudo crear la sucursal.");
        }
    }

    public Resultado actualizar(int id, String nombre, String direccion, String telefono, String email) {
        if (id <= 0 || nombre == null || nombre.isBlank()) return new Resultado(false, 400, "Datos de sucursal inválidos.");
        String sql = "UPDATE sucursal SET Nombre = ?, Direccion = ?, Telefono = ?, Email = ? WHERE ID_Sucursal = ?";
        try (Connection con = ConexionBD.obtenerConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre.trim()); ps.setString(2, nvl(direccion)); ps.setString(3, nvl(telefono));
            ps.setString(4, nvl(email)); ps.setInt(5, id);
            int changed = ps.executeUpdate();
            return changed > 0 ? new Resultado(true, 200, "Sucursal actualizada correctamente.")
                    : new Resultado(false, 404, "No se encontró la sucursal.");
        } catch (SQLException e) {
            System.err.println("Error al actualizar sucursal: " + e.getMessage());
            return new Resultado(false, 500, "No se pudo actualizar la sucursal.");
        }
    }

    public Resultado eliminar(int id) {
        if (id <= 1) return new Resultado(false, 409, "La sucursal principal (ID 1) está protegida y no se puede eliminar.");
        try (Connection con = ConexionBD.obtenerConexion()) {
            con.setAutoCommit(false);
            try {
                if (!existe(con, "SELECT COUNT(*) FROM sucursal WHERE ID_Sucursal = ?", id)) {
                    con.rollback(); return new Resultado(false, 404, "No se encontró la sucursal.");
                }
                String[] referencias = {
                    "SELECT COUNT(*) FROM empleados WHERE ID_Sucursal = ?",
                    "SELECT COUNT(*) FROM ventas WHERE ID_Sucursal = ?",
                    "SELECT COUNT(*) FROM transferencias WHERE ID_Sucursal_Origen = ? OR ID_Sucursal_Destino = ?",
                    "SELECT COUNT(*) FROM historial_pedidos WHERE ID_Sucursal = ? OR ID_Sucursal_Origen = ? OR ID_Sucursal_Destino = ?",
                    "SELECT COUNT(*) FROM cierre_caja WHERE ID_Sucursal = ?"
                };
                for (int i = 0; i < referencias.length; i++) {
                    try (PreparedStatement ps = con.prepareStatement(referencias[i])) {
                        int parametros = (i == 2 ? 2 : (i == 3 ? 3 : 1));
                        for (int j = 1; j <= parametros; j++) ps.setInt(j, id);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next() && rs.getLong(1) > 0) {
                                con.rollback(); return new Resultado(false, 409,
                                        "No se puede eliminar la sucursal porque tiene empleados, ventas, cierres o movimientos históricos asociados.");
                            }
                        }
                    }
                }
                try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM stock_sucursal WHERE ID_Sucursal = ? AND Cantidad <> 0")) {
                    ps.setInt(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && rs.getLong(1) > 0) {
                            con.rollback(); return new Resultado(false, 409, "La sucursal todavía tiene stock. Transferilo o ajustalo antes de eliminarla.");
                        }
                    }
                }
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM stock_sucursal WHERE ID_Sucursal = ?")) {
                    ps.setInt(1, id); ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM sucursal WHERE ID_Sucursal = ?")) {
                    ps.setInt(1, id); ps.executeUpdate();
                }
                con.commit();
                return new Resultado(true, 200, "Sucursal eliminada correctamente.");
            } catch (SQLException e) { con.rollback(); throw e; }
        } catch (SQLException e) {
            System.err.println("Error al eliminar sucursal: " + e.getMessage());
            return new Resultado(false, 500, "No se pudo eliminar la sucursal por una restricción de la base de datos.");
        }
    }

    private static boolean existe(Connection con, String sql, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() && rs.getLong(1) > 0; }
        }
    }
    private static String nvl(String value) { return value == null ? "" : value.trim(); }
    private static String esc(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
