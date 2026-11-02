import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.mindrot.jbcrypt.BCrypt;

public class EmpleadoDAO {

    public static class SesionUsuario {
        private int idEmpleado;
        private String dni;
        private String nombreCompleto;
        private String rol;
        private int idSucursal;

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
        public int getIdSucursal() { return idSucursal; }
    }

    public SesionUsuario validarLogin(String dni, String contrasenaIngresada) {
        if (dni == null || contrasenaIngresada == null || contrasenaIngresada.isBlank()) return null;
        final String sql = "SELECT e.ID_Empleado, e.DNI, CONCAT(e.Nombres, ' ', e.Apellidos) AS NombreCompleto, " +
                "r.Rol, e.ID_Sucursal, e.Contrasena FROM empleados e " +
                "INNER JOIN roles r ON e.ID_Rol = r.ID_Rol WHERE e.DNI = ? AND e.Activo = 1";

        try (Connection con = ConexionBD.obtenerConexion()) {
            int idEmpleado;
            String dniDb;
            String nombreCompleto;
            String rol;
            int idSucursal;
            String storedPassword;
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, dni.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return null;
                    idEmpleado = rs.getInt("ID_Empleado");
                    dniDb = rs.getString("DNI");
                    nombreCompleto = rs.getString("NombreCompleto");
                    rol = rs.getString("Rol");
                    idSucursal = rs.getInt("ID_Sucursal");
                    storedPassword = rs.getString("Contrasena");
                }
            }

            if (storedPassword == null) return null;
            boolean isBcrypt = storedPassword.matches("^\\$2[aby]\\$[0-9]{2}\\$.*");
            boolean valid;
            if (isBcrypt) {
                try { valid = BCrypt.checkpw(contrasenaIngresada.trim(), storedPassword); }
                catch (IllegalArgumentException ex) { valid = false; }
            } else {
                // Compatibilidad temporal: si la contraseña antigua era texto plano,
                // al primer inicio de sesión correcto se migra inmediatamente a BCrypt.
                valid = storedPassword.equals(contrasenaIngresada.trim());
                if (valid) {
                    String upgraded = BCrypt.hashpw(contrasenaIngresada.trim(), BCrypt.gensalt(12));
                    try (PreparedStatement update = con.prepareStatement(
                            "UPDATE empleados SET Contrasena = ? WHERE ID_Empleado = ? AND Contrasena = ?")) {
                        update.setString(1, upgraded);
                        update.setInt(2, idEmpleado);
                        update.setString(3, storedPassword);
                        update.executeUpdate();
                    }
                }
            }
            return valid ? new SesionUsuario(idEmpleado, dniDb, nombreCompleto, rol, idSucursal) : null;
        } catch (SQLException e) {
            System.err.println("Error al validar usuario: " + e.getMessage());
            return null;
        }
    }

    public String obtenerJsonEmpleados() {
        StringBuilder json = new StringBuilder("[");
        String sql = "SELECT e.ID_Empleado, e.ID_Sucursal, e.DNI, e.Nombres, e.Apellidos, e.Telefono, e.Nombre_Usuario, r.Rol, e.Comision, e.comision_acumulada " +
                "FROM empleados e INNER JOIN roles r ON e.ID_Rol = r.ID_Rol WHERE e.Activo = 1";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(',');
                first = false;
                json.append("{\"idEmpleado\":").append(rs.getInt("ID_Empleado"))
                    .append(",\"idSucursal\":").append(rs.getInt("ID_Sucursal"))
                    .append(",\"dni\":\"").append(esc(rs.getString("DNI")))
                    .append("\",\"nombres\":\"").append(esc(rs.getString("Nombres")))
                    .append("\",\"apellidos\":\"").append(esc(rs.getString("Apellidos")))
                    .append("\",\"telefono\":\"").append(esc(rs.getString("Telefono")))
                    .append("\",\"nombreUsuario\":\"").append(esc(rs.getString("Nombre_Usuario")))
                    .append("\",\"rol\":\"").append(esc(rs.getString("Rol")))
                    .append("\",\"comision\":").append(rs.getDouble("Comision"))
                    .append(",\"comisionAcumulada\":").append(rs.getDouble("comision_acumulada")).append('}');
            }
        } catch (SQLException e) { System.err.println("Error al obtener empleados: " + e.getMessage()); }
        return json.append(']').toString();
    }

    public boolean registrarEmpleado(String dni, String nombres, String apellidos, String telefono, String usuario,
            String password, int idSucursal, int idRol, double comision) {
        if (password == null || password.isBlank()) return false;
        String sql = "INSERT INTO empleados (DNI, Nombres, Apellidos, Telefono, Nombre_Usuario, Contrasena, ID_Sucursal, ID_Rol, Comision, Activo) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1)";
        try (Connection con = ConexionBD.obtenerConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, dni); ps.setString(2, nombres); ps.setString(3, apellidos); ps.setString(4, telefono);
            ps.setString(5, usuario); ps.setString(6, BCrypt.hashpw(password.trim(), BCrypt.gensalt(12)));
            ps.setInt(7, idSucursal); ps.setInt(8, idRol); ps.setDouble(9, comision);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("Error al registrar empleado: " + e.getMessage()); return false; }
    }

    public boolean eliminarEmpleado(int id) {
        String sql = "UPDATE empleados SET Activo = 0 WHERE ID_Empleado = ?";
        try (Connection con = ConexionBD.obtenerConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id); return ps.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("Error al dar de baja al empleado: " + e.getMessage()); return false; }
    }

    public boolean actualizarEmpleado(int idEmpleado, String dni, String nombres, String apellidos, String telefono,
            String usuario, String password, int idSucursal, int idRol, double comision) {
        boolean changePassword = password != null && !password.isBlank();
        String sql = "UPDATE empleados SET DNI = ?, Nombres = ?, Apellidos = ?, Telefono = ?, Nombre_Usuario = ?, " +
                (changePassword ? "Contrasena = ?, " : "") +
                "ID_Sucursal = ?, ID_Rol = ?, Comision = ? WHERE ID_Empleado = ?";
        try (Connection con = ConexionBD.obtenerConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, dni); ps.setString(i++, nombres); ps.setString(i++, apellidos);
            ps.setString(i++, telefono); ps.setString(i++, usuario);
            if (changePassword) ps.setString(i++, BCrypt.hashpw(password.trim(), BCrypt.gensalt(12)));
            ps.setInt(i++, idSucursal); ps.setInt(i++, idRol); ps.setDouble(i++, comision); ps.setInt(i, idEmpleado);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("Error al actualizar empleado: " + e.getMessage()); return false; }
    }

    public boolean resetearComisionEmpleado(int idEmpleado) {
        String sql = "UPDATE empleados SET ventas_realizadas = 0, comision_acumulada = 0.0 WHERE ID_Empleado = ?";
        try (Connection con = ConexionBD.obtenerConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idEmpleado); return ps.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("Error al resetear comisión: " + e.getMessage()); return false; }
    }

    private static String esc(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
