import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionBD {

    private static final String URL = "jdbc:mysql://localhost:3306/rudafer?useSSL=false&serverTimezone=UTC&useUnicode=true&characterEncoding=utf8";
    private static final String USER = "root"; // Usuario por defecto de XAMPP
    private static final String PASSWORD = ""; // Sin contraseña por defecto en XAMPP

    public static Connection obtenerConexion() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver de MySQL no encontrado: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try (Connection con = obtenerConexion()) {
            if (con != null) {
                System.out.println("---------------------------------------------");
                System.out.println("¡CONEXIÓN EXITOSA A LA BASE DE DATOS RUDAFER!");
                System.out.println("---------------------------------------------");
            }
        } catch (SQLException e) {
            System.err.println("Error de conexión: " + e.getMessage());
        }
    }
}
