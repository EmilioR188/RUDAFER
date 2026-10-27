import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class EmpleadoDAO {

    public static class SesionUsuario {
        private int idEmpleado;
        private String dni;
        private String nombreCompleto;
        private String rol;
        private int idSucursal; // <--- NUEVO CAMPO

        public SesionUsuario(int idEmpleado, String dni, String nombreCompleto, String rol, int idSucursal) {
            this.idEmpleado = idEmpleado;
            this.dni = dni;
            this.nombreCompleto = nombreCompleto;
            this.rol = rol;
            this.idSucursal = idSucursal;
        }

        public int getIdEmpleado() { return idEmpleado; }
        public String getDni() { return dni; }
        public String getNombreCompleto() { return nombreCompleto; }
        public String getRol() { return rol; }
        public int getIdSucursal() { return idSucursal; } // <--- NUEVO GETTER
    }

    public SesionUsuario validarLogin(String dni, String contrasena) {
        // Añadimos e.ID_Sucursal a la consulta SQL
        String sql = "SELECT e.ID_Empleado, e.DNI, CONCAT(e.Nombres, ' ', e.Apellidos) AS NombreCompleto, r.Rol, e.ID_Sucursal "
                   + "FROM empleados e "
                   + "INNER JOIN roles r ON e.ID_Rol = r.ID_Rol "
                   + "WHERE e.DNI = ? AND e.Contrasena = ? AND e.Activo = 1";

        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, dni.trim());
            ps.setString(2, contrasena.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new SesionUsuario(
                        rs.getInt("ID_Empleado"),
                        rs.getString("DNI"),
                        rs.getString("NombreCompleto"),
                        rs.getString("Rol"),
                        rs.getInt("ID_Sucursal") // <--- CAPTURAMOS EL ID DE SUCURSAL REAL
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al validar usuario: " + e.getMessage());
        }
        return null;
    }

    // Listar empleados activos en formato JSON para la administración
    public String obtenerJsonEmpleados() {
        StringBuilder json = new StringBuilder("[");
        // Solo traemos los que tienen Activo = 1
        String sql = "SELECT e.ID_Empleado, e.DNI, e.Nombres, e.Apellidos, e.Telefono, e.Nombre_Usuario, r.Rol, e.Comision, e.comision_acumulada "
                   + "FROM empleados e INNER JOIN roles r ON e.ID_Rol = r.ID_Rol WHERE e.Activo = 1";
        
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            boolean primero = true;
            while (rs.next()) {
                if (!primero) json.append(",");
                primero = false;
                
                json.append("{")
                    .append("\"idEmpleado\":").append(rs.getInt("ID_Empleado")).append(",")
                    .append("\"dni\":\"").append(rs.getString("DNI")).append("\",")
                    .append("\"nombres\":\"").append(rs.getString("Nombres").replace("\"", "\\\"")).append("\",")
                    .append("\"apellidos\":\"").append(rs.getString("Apellidos").replace("\"", "\\\"")).append("\",")
                    .append("\"telefono\":\"").append(rs.getString("Telefono") != null ? rs.getString("Telefono") : "").append("\",")
                    .append("\"nombreUsuario\":\"").append(rs.getString("Nombre_Usuario")).append("\",")
                    .append("\"rol\":\"").append(rs.getString("Rol")).append("\",")
                    .append("\"comision\":").append(rs.getDouble("Comision")).append(",")
                    .append("\"comisionAcumulada\":").append(rs.getDouble("comision_acumulada"))
                    .append("}");
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener empleados: " + e.getMessage());
        }
        
        json.append("]");
        return json.toString();
    }

    // Registrar nuevo empleado con comisión (por defecto Activo = 1)
    public boolean registrarEmpleado(String dni, String nombres, String apellidos, String telefono, String usuario, String password, int idSucursal, int idRol, double comision) {
        String sql = "INSERT INTO empleados (DNI, Nombres, Apellidos, Telefono, Nombre_Usuario, Contrasena, ID_Sucursal, ID_Rol, Comision, Activo) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1)";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, dni);
            ps.setString(2, nombres);
            ps.setString(3, apellidos);
            ps.setString(4, telefono);
            ps.setString(5, usuario);
            ps.setString(6, password);
            ps.setInt(7, idSucursal);
            ps.setInt(8, idRol);
            ps.setDouble(9, comision);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al registrar empleado: " + e.getMessage());
            return false;
        }
    }

    // Baja lógica del empleado (en lugar de DELETE, hacemos un UPDATE a Activo = 0)
    public boolean eliminarEmpleado(int id) {
        String sql = "UPDATE empleados SET Activo = 0 WHERE ID_Empleado = ?";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al dar de baja al empleado: " + e.getMessage());
            return false;
        }
    }

    // Actualizar información completa del empleado
    public boolean actualizarEmpleado(int idEmpleado, String dni, String nombres, String apellidos, String telefono, String usuario, String password, int idSucursal, int idRol, double comision) {
        String sql = "UPDATE empleados SET DNI = ?, Nombres = ?, Apellidos = ?, Telefono = ?, Nombre_Usuario = ?, Contrasena = ?, ID_Sucursal = ?, ID_Rol = ?, Comision = ? WHERE ID_Empleado = ?";
        try (Connection con = ConexionBD.obtenerConexion();
            PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, dni);
            ps.setString(2, nombres);
            ps.setString(3, apellidos);
            ps.setString(4, telefono);
            ps.setString(5, usuario);
            ps.setString(6, password);
            ps.setInt(7, idSucursal);
            ps.setInt(8, idRol);
            ps.setDouble(9, comision);
            ps.setInt(10, idEmpleado);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al actualizar empleado: " + e.getMessage());
            return false;
        }
    }

    // Resetear las ventas y comisiones acumuladas de un empleado específico (Liquidación de pago)
    public boolean resetearComisionEmpleado(int idEmpleado) {
        String sql = "UPDATE empleados SET ventas_realizadas = 0, comision_acumulada = 0.0 WHERE ID_Empleado = ?";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idEmpleado);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al resetear la comisión del empleado: " + e.getMessage());
            return false;
        }
    }
}