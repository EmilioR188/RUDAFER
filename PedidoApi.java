import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.OutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PedidoApi {

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);

        // Endpoint de sugerencias de pedidos para WhatsApp
        server.createContext("/api/pedidos/sugerencias", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
                exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");

                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    try {
                        PedidoDAO dao = new PedidoDAO();
                        List<PedidoDAO.ItemPedido> items = dao.listarProductosParaReponer();

                        Map<String, List<PedidoDAO.ItemPedido>> agrupadosPorProveedor = items.stream()
                            .collect(Collectors.groupingBy(PedidoDAO.ItemPedido::getNombreProveedor));

                        StringBuilder json = new StringBuilder("[");
                        int index = 0;
                        
                        for (Map.Entry<String, List<PedidoDAO.ItemPedido>> entry : agrupadosPorProveedor.entrySet()) {
                            String proveedor = entry.getKey().replace("\"", "\\\"");
                            List<PedidoDAO.ItemPedido> productosProveedor = entry.getValue();

                            StringBuilder sbMensaje = new StringBuilder();
                            sbMensaje.append("Hola ").append(proveedor).append(", te paso el pedido a realizar:\\n");
                            
                            for (PedidoDAO.ItemPedido p : productosProveedor) {
                                String nom = p.getNombreProducto().replace("\"", "\\\"");
                                sbMensaje.append("• ").append(p.getCantidadSugerida()).append(" u. de ").append(nom).append("\\n");
                            }
                            sbMensaje.append("\\nMuchas gracias");

                            json.append("{")
                                .append("\"proveedor\":\"").append(proveedor).append("\",")
                                .append("\"mensajeWs\":\"").append(sbMensaje.toString()).append("\",")
                                .append("\"cantidadItems\":").append(productosProveedor.size())
                                .append("}");

                            if (index < agrupadosPorProveedor.size() - 1) {
                                json.append(",");
                            }
                            index++;
                        }
                        json.append("]");

                        byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                        exchange.sendResponseHeaders(200, bytes.length);
                        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }

                    } catch (Exception e) {
                        e.printStackTrace();
                        exchange.sendResponseHeaders(500, -1);
                    }
                }
            }
        });

        // Nuevo Endpoint para recibir la llegada del pedido y actualizar el stock
        server.createContext("/api/pedidos/recibir", new HttpHandler() {
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
                    try {
                        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                        
                        PedidoDAO dao = new PedidoDAO();
                        boolean exito = dao.procesarLlegadaPedido(body);

                        String respuestaJson = "{\"exito\":" + exito + "}";
                        byte[] bytes = respuestaJson.getBytes(StandardCharsets.UTF_8);
                        
                        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                        exchange.sendResponseHeaders(exito ? 200 : 400, bytes.length);
                        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }

                    } catch (Exception e) {
                        e.printStackTrace();
                        exchange.sendResponseHeaders(500, -1);
                    }
                }
            }
        });

        server.setExecutor(null);
        System.out.println("API de Pedidos activa en http://localhost:8081/api/pedidos");
        server.start();
    }
}