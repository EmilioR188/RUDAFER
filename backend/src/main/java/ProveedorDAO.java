import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ProveedorDAO {

    public String obtenerJsonProveedores() {
    StringBuilder json = new StringBuilder("[");
    // Consulta con subconsultas para contar productos y obtener el total del último pedido
    String sql = "SELECT p.ID_Proveedor, p.Nombre, p.Telefono, p.Email, p.Deuda_Total, "
               + "(SELECT COUNT(*) FROM productos prod WHERE prod.ID_Proveedor = p.ID_Proveedor) AS ProductosAsociados, "
               + "(SELECT Total FROM historial_pedidos hp WHERE hp.Proveedor = p.Nombre ORDER BY hp.ID_Historial DESC LIMIT 1) AS UltimoPedido "
               + "FROM proveedor p"; 
    
    try (Connection con = ConexionBD.obtenerConexion();
         PreparedStatement ps = con.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
        
        boolean primero = true;
        while (rs.next()) {
            if (!primero) json.append(",");
            primero = false;
            
            int id = rs.getInt("ID_Proveedor");
            String nombre = rs.getString("Nombre");
            if (nombre == null) nombre = "Sin Nombre";
            double deuda = rs.getDouble("Deuda_Total");
            int productosCount = rs.getInt("ProductosAsociados");
            double ultimoPedido = rs.getDouble("UltimoPedido");

            json.append("{")
                .append("\"idProveedor\":").append(id).append(",")
                .append("\"nombre\":\"").append(nombre.replace("\"", "\\\"")).append("\",")
                .append("\"deudaTotal\":").append(deuda).append(",")
                .append("\"productosAsociados\":").append(productosCount).append(",")
                .append("\"ultimoPedido\":").append(ultimoPedido)
                .append("}");
        }
    } catch (SQLException e) {
        System.err.println("Error al obtener proveedores: " + e.getMessage());
    }
    
    json.append("]");
    return json.toString();
}

// Registrar proveedor aceptando Deuda Inicial
public boolean registrarProveedor(String nombre, String telefono, String email, double deudaInicial) {
    String sql = "INSERT INTO proveedor (Nombre, Telefono, Email, Deuda_Total) VALUES (?, ?, ?, ?)";
    try (Connection con = ConexionBD.obtenerConexion();
         PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setString(1, nombre);
        ps.setString(2, telefono);
        ps.setString(3, email);
        ps.setDouble(4, deudaInicial);
        return ps.executeUpdate() > 0;
    } catch (SQLException e) {
        System.err.println("Error al registrar proveedor: " + e.getMessage());
        return false;
    }
}

    public boolean registrarProveedor(String nombre, String telefono, String email) {
        String sql = "INSERT INTO proveedor (Nombre, Telefono, Email, Deuda_Total) VALUES (?, ?, ?, 0.00)";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setString(2, telefono);
            ps.setString(3, email);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al registrar proveedor: " + e.getMessage());
            return false;
        }
    }

    public boolean eliminarProveedor(int id) {
        String sql = "DELETE FROM proveedor WHERE ID_Proveedor = ?";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al eliminar proveedor: " + e.getMessage());
            return false;
        }
    }

    public boolean registrarPago(int idProveedor, double monto, String observacion) {
        Connection con = null;
        try {
            con = ConexionBD.obtenerConexion();
            con.setAutoCommit(false);

            // 1. Restar la deuda al proveedor
            String sqlUpdate = "UPDATE proveedor SET deuda_total = deuda_total - ? WHERE ID_Proveedor = ?";
            try (PreparedStatement psU = con.prepareStatement(sqlUpdate)) {
                psU.setDouble(1, monto);
                psU.setInt(2, idProveedor);
                psU.executeUpdate();
            }

            // 2. Registrar el pago en el historial de pagos para los movimientos
            String sqlInsert = "INSERT INTO pagos_proveedores (id_proveedor, monto, observacion) VALUES (?, ?, ?)";
            try (PreparedStatement psI = con.prepareStatement(sqlInsert)) {
                psI.setInt(1, idProveedor);
                psI.setDouble(2, monto);
                psI.setString(3, observacion != null ? observacion : "Pago a proveedor");
                psI.executeUpdate();
            }

            con.commit();
            return true;
        } catch (SQLException e) {
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
    // Actualizar información completa del proveedor
    public boolean actualizarProveedor(int idProveedor, String nombre, String telefono, String email, double deudaTotal) {
        String sql = "UPDATE proveedor SET Nombre = ?, Telefono = ?, Email = ?, Deuda_Total = ? WHERE ID_Proveedor = ?";
        try (Connection con = ConexionBD.obtenerConexion();
            PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setString(2, telefono);
            ps.setString(3, email);
            ps.setDouble(4, deudaTotal);
            ps.setInt(5, idProveedor);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al actualizar proveedor: " + e.getMessage());
            return false;
        }
    }
}
