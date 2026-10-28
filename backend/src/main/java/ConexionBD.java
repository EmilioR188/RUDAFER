import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionBD {

    public static Connection obtenerConexion() throws SQLException {

        String host = System.getenv()
            .getOrDefault("MYSQLHOST", "localhost");

        String port = System.getenv()
            .getOrDefault("MYSQLPORT", "3306");

        String database = System.getenv()
            .getOrDefault("MYSQLDATABASE", "rudafer");

        String user = System.getenv()
            .getOrDefault("MYSQLUSER", "root");

        String password = System.getenv()
            .getOrDefault("MYSQLPASSWORD", "");

        String url = "jdbc:mysql://" + host + ":" + port + "/" +
            database +
            "?sslMode=REQUIRED&allowPublicKeyRetrieval=true" +
            "&serverTimezone=UTC&useUnicode=true" +
            "&characterEncoding=UTF-8";

        return DriverManager.getConnection(url, user, password);
    }
}
