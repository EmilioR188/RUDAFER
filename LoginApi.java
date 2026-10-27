import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;

public class LoginApi {

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        
        server.createContext("/api/login", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
                exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");

                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    InputStream is = exchange.getRequestBody();
                    String body = new String(is.readAllBytes());

                    String dni = obtenerValorJson(body, "dni");
                    String contrasena = obtenerValorJson(body, "contrasena");

                    EmpleadoDAO dao = new EmpleadoDAO();
                    EmpleadoDAO.SesionUsuario usuario = dao.validarLogin(dni, contrasena);

                    String respuestaJson;
                    int statusCode;

                    if (usuario != null) {
                        statusCode = 200;
                        respuestaJson = String.format(
                            "{\"exito\": true, \"nombre\": \"%s\", \"rol\": \"%s\"}",
                            usuario.getNombreCompleto(), usuario.getRol()
                        );
                    } else {
                        statusCode = 401;
                        respuestaJson = "{\"exito\": false, \"mensaje\": \"Credenciales incorrectas\"}";
                    }

                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    byte[] bytes = respuestaJson.getBytes("UTF-8");
                    exchange.sendResponseHeaders(statusCode, bytes.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(bytes);
                    os.close();
                }
            }
        });

        server.setExecutor(null);
        System.out.println("Servidor Java listo y escuchando en http://localhost:8080/api/login");
        server.start();
    }

    private static String obtenerValorJson(String json, String clave) {
        String patron = "\"" + clave + "\":\"";
        int inicio = json.indexOf(patron);
        if (inicio == -1) return "";
        inicio += patron.length();
        int fin = json.indexOf("\"", inicio);
        return (fin != -1) ? json.substring(inicio, fin) : "";
    }
}