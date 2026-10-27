import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ServidorPrincipal {

    public static void main(String[] args) throws IOException {
        String portEnv = System.getenv("PORT");
        int puerto = portEnv != null ? Integer.parseInt(portEnv) : 8081;

        HttpServer server = HttpServer.create(new InetSocketAddress(puerto), 0);

        // =========================================================
        // 0. ENDPOINT: SUCURSALES DINÁMICAS (/api/sucursales) [GET]
        // =========================================================
        server.createContext("/api/sucursales", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                configurarHeadersCORS(exchange);
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    try {
                        StringBuilder json = new StringBuilder("[{\"id\": 0, \"nombre\": \"🌐 Vista General (Todas)\"}");
                        try (Connection con = ConexionBD.obtenerConexion();
                             PreparedStatement ps = con.prepareStatement("SELECT ID_Sucursal, Nombre FROM sucursal");
                             ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                json.append(",{\"id\":").append(rs.getInt("ID_Sucursal"))
                                    .append(",\"nombre\":\"").append(rs.getString("Nombre").replace("\"", "\\\"")).append("\"}");
                            }
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

        // =========================================================
        // 1. ENDPOINT: PRODUCTOS (GET, POST, PUT, DELETE) (/api/productos)
        // =========================================================
        server.createContext("/api/productos", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                configurarHeadersCORS(exchange);
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                String method = exchange.getRequestMethod();

                if ("GET".equalsIgnoreCase(method)) {
                    try {
                        String query = exchange.getRequestURI().getQuery();
                        int idSucursal = 1; // Por defecto Sucursal Central
                        if (query != null) {
                            if (query.contains("idSucursal=")) {
                                try {
                                    idSucursal = Integer.parseInt(query.split("idSucursal=")[1].split("&")[0]);
                                } catch (Exception ignored) {}
                            } else if (query.contains("sucursal=")) {
                                try {
                                    String sucStr = query.split("sucursal=")[1].split("&")[0];
                                    if (sucStr.matches("-?\\d+")) {
                                        idSucursal = Integer.parseInt(sucStr);
                                    } else if (sucStr.toLowerCase().contains("sucursal")) {
                                        idSucursal = Integer.parseInt(sucStr.replaceAll("[^0-9]", ""));
                                    }
                                } catch (Exception ignored) {}
                            }
                        }

                        ProductoDAO dao = new ProductoDAO();
                        String jsonProductos = dao.obtenerJsonProductos(idSucursal);

                        byte[] bytes = jsonProductos.getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                        exchange.sendResponseHeaders(200, bytes.length);
                        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
                    } catch (Exception e) {
                        e.printStackTrace();
                        exchange.sendResponseHeaders(500, -1);
                    }
                } else if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8));
                        StringBuilder bodyBuilder = new StringBuilder();
                        String linea;
                        while ((linea = reader.readLine()) != null) {
                            bodyBuilder.append(linea);
                        }
                        String body = bodyBuilder.toString();

                        String codigo = obtenerValorJson(body, "codigo");
                        String nombre = obtenerValorJson(body, "nombre");
                        String descripcion = obtenerValorJson(body, "descripcion");
                        String marca = obtenerValorJson(body, "marca");
                        String estante = obtenerValorJson(body, "estante");
                        String fila = obtenerValorJson(body, "fila");
                        
                        double precioCompra = 0.0;
                        try { 
                            String v = obtenerValorJson(body, "precioCompra"); 
                            if(!v.isEmpty() && !"undefined".equalsIgnoreCase(v)) precioCompra = Double.parseDouble(v); 
                        } catch(Exception ignored){}

                        double precioVenta = 0.0;
                        try { 
                            String v = obtenerValorJson(body, "precioVenta"); 
                            if(!v.isEmpty() && !"undefined".equalsIgnoreCase(v)) precioVenta = Double.parseDouble(v); 
                        } catch(Exception ignored){}

                        int stock = 0;
                        try { 
                            String v = obtenerValorJson(body, "stockActual"); 
                            if(v.isEmpty()) v = obtenerValorJson(body, "stock");
                            if(!v.isEmpty() && !"undefined".equalsIgnoreCase(v)) stock = Integer.parseInt(v); 
                        } catch(Exception ignored){}

                        int stockMinimo = 5;
                        try { 
                            String v = obtenerValorJson(body, "stockMinimo"); 
                            if(!v.isEmpty() && !"undefined".equalsIgnoreCase(v)) stockMinimo = Integer.parseInt(v); 
                        } catch(Exception ignored){}

                        int idCategoria = 1;
                        try { 
                            String v = obtenerValorJson(body, "idCategoria"); 
                            if(!v.isEmpty() && !"undefined".equalsIgnoreCase(v)) idCategoria = Integer.parseInt(v); 
                        } catch(Exception ignored){}

                        int idProveedor = 0;
                        try { 
                            String v = obtenerValorJson(body, "idProveedor"); 
                            if (v.isEmpty() || "undefined".equalsIgnoreCase(v)) {
                                v = obtenerValorJson(body, "id_proveedor"); 
                            }
                            if (!v.isEmpty() && !"undefined".equalsIgnoreCase(v)) {
                                idProveedor = Integer.parseInt(v); 
                            }
                        } catch(Exception ignored){}

                        int idSucursal = 1;
                        try {
                            String v = obtenerValorJson(body, "idSucursal");
                            if (v.isEmpty()) v = obtenerValorJson(body, "sucursal");
                            if (!v.isEmpty() && !"undefined".equalsIgnoreCase(v)) {
                                if (v.matches("-?\\d+")) idSucursal = Integer.parseInt(v);
                                else if (v.toLowerCase().contains("sucursal")) idSucursal = Integer.parseInt(v.replaceAll("[^0-9]", ""));
                            }
                        } catch(Exception ignored){}

                        ProductoDAO dao = new ProductoDAO();
                        boolean exito = false;

                        if ("PUT".equalsIgnoreCase(method)) {
                            int idProducto = 0;
                            try { 
                                String v = obtenerValorJson(body, "id"); 
                                if(v.isEmpty()) v = obtenerValorJson(body, "idProducto");
                                if(!v.isEmpty() && !"undefined".equalsIgnoreCase(v)) idProducto = Integer.parseInt(v); 
                            } catch(Exception ignored){}
                            exito = dao.actualizarProducto(idProducto, codigo, nombre, descripcion, marca, estante, fila, precioCompra, precioVenta, idCategoria, idProveedor, stock, stockMinimo, idSucursal);
                        } else {
                            exito = dao.registrarProducto(codigo, nombre, descripcion, marca, estante, fila, precioCompra, precioVenta, idCategoria, idProveedor, stock, stockMinimo, idSucursal);
                        }

                        String respuestaJson = "{\"exito\": " + exito + "}";
                        byte[] bytes = respuestaJson.getBytes(StandardCharsets.UTF_8);
                        
                        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                        exchange.sendResponseHeaders(200, bytes.length);
                        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }

                    } catch (Exception e) {
                        e.printStackTrace();
                        exchange.sendResponseHeaders(500, -1);
                    }
                } else if ("DELETE".equalsIgnoreCase(method)) {
                    try {
                        String query = exchange.getRequestURI().getQuery();
                        int id = 0;
                        if (query != null && query.contains("id=")) {
                            String[] partes = query.split("id=");
                            if (partes.length > 1) {
                                String idStr = partes[1].split("&")[0].trim();
                                if (!idStr.isEmpty() && !"undefined".equalsIgnoreCase(idStr)) {
                                    id = Integer.parseInt(idStr);
                                }
                            }
                        }

                        ProductoDAO dao = new ProductoDAO();
                        boolean exito = id > 0 && dao.eliminarProducto(id);

                        String respuestaJson = "{\"exito\": " + exito + "}";
                        byte[] bytes = respuestaJson.getBytes(StandardCharsets.UTF_8);
                        
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

        // =========================================================
        // 2. ENDPOINT: PROVEEDORES (/api/proveedores) [GET, POST, PUT, DELETE]
        // =========================================================
        server.createContext("/api/proveedores", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                configurarHeadersCORS(exchange);
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                String method = exchange.getRequestMethod();
                ProveedorDAO dao = new ProveedorDAO();

                if ("GET".equalsIgnoreCase(method)) {
                    try {
                        String jsonProveedores = dao.obtenerJsonProveedores();
                        byte[] bytes = jsonProveedores.getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                        exchange.sendResponseHeaders(200, bytes.length);
                        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
                    } catch (Exception e) {
                        e.printStackTrace();
                        exchange.sendResponseHeaders(500, -1);
                    }
                } else if ("POST".equalsIgnoreCase(method)) {
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) sb.append(line);
                        String body = sb.toString();

                        String nombre = obtenerValorJson(body, "nombre");
                        String telefono = obtenerValorJson(body, "telefono");
                        String email = obtenerValorJson(body, "email");
                        double deudaInicial = 0.0;
                        try { deudaInicial = Double.parseDouble(obtenerValorJson(body, "deudaInicial")); } catch(Exception ignored){}

                        boolean exito = dao.registrarProveedor(nombre, telefono, email, deudaInicial);
                        String resp = "{\"exito\": " + exito + "}";
                        byte[] bytes = resp.getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                        exchange.sendResponseHeaders(200, bytes.length);
                        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
                    } catch (Exception e) {
                        e.printStackTrace();
                        exchange.sendResponseHeaders(500, -1);
                    }
                } else if ("PUT".equalsIgnoreCase(method)) {
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) sb.append(line);
                        String body = sb.toString();

                        int id = 0;
                        try { id = Integer.parseInt(obtenerValorJson(body, "idProveedor")); } catch(Exception ignored){}
                        String nombre = obtenerValorJson(body, "nombre");
                        String telefono = obtenerValorJson(body, "telefono");
                        String email = obtenerValorJson(body, "email");
                        double deudaTotal = 0.0;
                        try { deudaTotal = Double.parseDouble(obtenerValorJson(body, "deudaTotal")); } catch(Exception ignored){}

                        boolean exito = id > 0 && dao.actualizarProveedor(id, nombre, telefono, email, deudaTotal);
                        String resp = "{\"exito\": " + exito + "}";
                        byte[] bytes = resp.getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                        exchange.sendResponseHeaders(200, bytes.length);
                        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
                    } catch (Exception e) {
                        e.printStackTrace();
                        exchange.sendResponseHeaders(500, -1);
                    }
                } else if ("DELETE".equalsIgnoreCase(method)) {
                    try {
                        String query = exchange.getRequestURI().getQuery();
                        int id = 0;
                        if (query != null && query.contains("id=")) {
                            String idStr = query.split("id=")[1].split("&")[0].trim();
                            id = Integer.parseInt(idStr);
                        }
                        boolean exito = id > 0 && dao.eliminarProveedor(id);
                        String resp = "{\"exito\": " + exito + "}";
                        byte[] bytes = resp.getBytes(StandardCharsets.UTF_8);
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

        // =========================================================
        // 2.1. ENDPOINT: PAGOS A PROVEEDORES (/api/proveedores/pago)
        // =========================================================
        server.createContext("/api/proveedores/pago", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                configurarHeadersCORS(exchange);
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) sb.append(line);
                        String body = sb.toString();

                        int idProveedor = 0;
                        try { idProveedor = Integer.parseInt(obtenerValorJson(body, "idProveedor")); } catch(Exception ignored){}

                        double monto = 0.0;
                        try { monto = Double.parseDouble(obtenerValorJson(body, "monto")); } catch(Exception ignored){}

                        String observacion = obtenerValorJson(body, "observacion");

                        ProveedorDAO dao = new ProveedorDAO();
                        boolean exito = idProveedor > 0 && monto > 0 && dao.registrarPago(idProveedor, monto, observacion);

                        String resp = "{\"exito\": " + exito + "}";
                        byte[] bytes = resp.getBytes(StandardCharsets.UTF_8);
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

        // =========================================================
        // 3. ENDPOINT: CREAR PEDIDO NUEVO (/api/pedidos/crear)
        // =========================================================
        server.createContext("/api/pedidos/crear", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                configurarHeadersCORS(exchange);
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8));
                        StringBuilder bodyBuilder = new StringBuilder();
                        String linea;
                        while ((linea = reader.readLine()) != null) {
                            bodyBuilder.append(linea);
                        }
                        String body = bodyBuilder.toString();
                        
                        String proveedor = "Proveedor General";
                        int idxProv = body.indexOf("\"proveedor\":\"");
                        if (idxProv != -1) {
                            int inicio = idxProv + 13;
                            int fin = body.indexOf("\"", inicio);
                            if (fin != -1) proveedor = body.substring(inicio, fin);
                        }

                        double total = 0.0;
                        int idxTotal = body.indexOf("\"total\":");
                        if (idxTotal != -1) {
                            int inicio = idxTotal + 8;
                            int fin = body.indexOf(",", inicio);
                            if (fin == -1) fin = body.indexOf("}", inicio);
                            if (fin != -1) {
                                try { total = Double.parseDouble(body.substring(inicio, fin).trim()); } catch (Exception ignored) {}
                            }
                        }

                        // Capturar el ID de la sucursal seleccionada
                        int idSucursal = 1;
                        try {
                            String sucStr = obtenerValorJson(body, "idSucursal");
                            if (!sucStr.isEmpty() && !"undefined".equalsIgnoreCase(sucStr)) {
                                idSucursal = Integer.parseInt(sucStr);
                            }
                        } catch (Exception ignored) {}

                        String detalle = "[]";
                        int idxDetalle = body.indexOf("\"detalle\":\"");
                        if (idxDetalle != -1) {
                            int inicio = idxDetalle + 11;
                            int fin = body.lastIndexOf("\"");
                            if (fin > inicio) {
                                detalle = body.substring(inicio, fin).replace("\\\"", "\"").replace("\\\\", "\\");
                            }
                        }

                        PedidoDAO dao = new PedidoDAO();
                        boolean exito = dao.guardarPedidoRealizado(proveedor, detalle, total, idSucursal);

                        String respuestaJson = "{\"exito\": " + exito + "}";
                        byte[] bytes = respuestaJson.getBytes(StandardCharsets.UTF_8);
                        
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

        // =========================================================
        // 4. ENDPOINT: RECIBIR PEDIDO Y ACTUALIZAR STOCK (/api/pedidos/recibir)
        // =========================================================
        server.createContext("/api/pedidos/recibir", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                configurarHeadersCORS(exchange);
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8));
                        StringBuilder bodyBuilder = new StringBuilder();
                        String linea;
                        while ((linea = reader.readLine()) != null) {
                            bodyBuilder.append(linea);
                        }
                        String body = bodyBuilder.toString();
                        
                        int idHistorial = 0;
                        try {
                            String idStr = obtenerValorJson(body, "idHistorial");
                            if (!idStr.isEmpty() && !"undefined".equalsIgnoreCase(idStr)) idHistorial = Integer.parseInt(idStr);
                        } catch (Exception ignored) {}

                        String jsonItems = "[]";
                        int idxItems = body.indexOf("\"items\":");
                        if (idxItems != -1) {
                            jsonItems = body.substring(idxItems + 8).trim();
                            if (jsonItems.endsWith("}")) {
                                jsonItems = jsonItems.substring(0, jsonItems.length() - 1).trim();
                            }
                        }

                        PedidoDAO dao = new PedidoDAO();
                        boolean exito = dao.procesarLlegadaPedido(jsonItems, idHistorial);

                        String respuestaJson = "{\"exito\": " + exito + "}";
                        byte[] bytes = respuestaJson.getBytes(StandardCharsets.UTF_8);
                        
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

        // =========================================================
        // 5. ENDPOINT: HISTORIAL DE PEDIDOS (/api/pedidos/historial)
        // =========================================================
        server.createContext("/api/pedidos/historial", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                configurarHeadersCORS(exchange);
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    try {
                        PedidoDAO dao = new PedidoDAO();
                        String jsonHistorial = dao.obtenerJsonHistorial();

                        byte[] bytes = jsonHistorial.getBytes(StandardCharsets.UTF_8);
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

        // =========================================================
        // 6. ENDPOINT: LOGIN (/api/login) -> DEVUELVE ID_EMPLEADO E ID_SUCURSAL
        // =========================================================
        server.createContext("/api/login", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                configurarHeadersCORS(exchange);
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8));
                        StringBuilder bodyBuilder = new StringBuilder();
                        String linea;
                        while ((linea = reader.readLine()) != null) {
                            bodyBuilder.append(linea);
                        }
                        String body = bodyBuilder.toString();
                        
                        String dni = obtenerValorJson(body, "dni");
                        if (dni.isEmpty()) dni = obtenerValorJson(body, "usuario");
                        
                        String password = obtenerValorJson(body, "password");
                        if (password.isEmpty()) password = obtenerValorJson(body, "contrasena");

                        EmpleadoDAO dao = new EmpleadoDAO();
                        EmpleadoDAO.SesionUsuario sesion = dao.validarLogin(dni, password);

                        String respuestaJson;
                        if (sesion != null) {
                            respuestaJson = "{\"exito\": true, \"rol\": \"" + sesion.getRol() + 
                                            "\", \"nombre\": \"" + sesion.getNombreCompleto().replace("\"", "\\\"") + 
                                            "\", \"ID_Empleado\": " + sesion.getIdEmpleado() + 
                                            ", \"ID_Sucursal\": " + sesion.getIdSucursal() + 
                                            ", \"mensaje\": \"Acceso concedido\"}";
                        } else {
                            respuestaJson = "{\"exito\": false, \"mensaje\": \"DNI o contraseña incorrectos\"}";
                        }

                        byte[] bytes = respuestaJson.getBytes(StandardCharsets.UTF_8);
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

        // =========================================================
        // 7. ENDPOINT: CANCELAR PEDIDO (/api/pedidos/cancelar)
        // =========================================================
        server.createContext("/api/pedidos/cancelar", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                configurarHeadersCORS(exchange);
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8));
                        StringBuilder bodyBuilder = new StringBuilder();
                        String linea;
                        while ((linea = reader.readLine()) != null) {
                            bodyBuilder.append(linea);
                        }
                        String body = bodyBuilder.toString();
                        
                        int idHistorial = 0;
                        try {
                            String idStr = obtenerValorJson(body, "idHistorial");
                            if (!idStr.isEmpty() && !"undefined".equalsIgnoreCase(idStr)) idHistorial = Integer.parseInt(idStr);
                        } catch (Exception ignored) {}

                        PedidoDAO dao = new PedidoDAO();
                        boolean exito = idHistorial > 0 && dao.cancelarPedido(idHistorial);

                        String respuestaJson = "{\"exito\": " + exito + "}";
                        byte[] bytes = respuestaJson.getBytes(StandardCharsets.UTF_8);
                        
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

        // =========================================================
        // 8. ENDPOINT: GESTIÓN DE EMPLEADOS (/api/empleados) [POST, PUT, DELETE, GET]
        // =========================================================
        server.createContext("/api/empleados", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                configurarHeadersCORS(exchange);
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                String method = exchange.getRequestMethod();
                EmpleadoDAO dao = new EmpleadoDAO();

                if ("GET".equalsIgnoreCase(method)) {
                    try {
                        String json = dao.obtenerJsonEmpleados();
                        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                        exchange.sendResponseHeaders(200, bytes.length);
                        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
                    } catch (Exception e) {
                        e.printStackTrace();
                        exchange.sendResponseHeaders(500, -1);
                    }
                } else if ("POST".equalsIgnoreCase(method)) {
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) sb.append(line);
                        String body = sb.toString();

                        String dni = obtenerValorJson(body, "dni");
                        String nombres = obtenerValorJson(body, "nombres");
                        String apellidos = obtenerValorJson(body, "apellidos");
                        String telefono = obtenerValorJson(body, "telefono");
                        String usuario = obtenerValorJson(body, "usuario");
                        String password = obtenerValorJson(body, "password");
                        
                        int idSucursal = 1;
                        try { idSucursal = Integer.parseInt(obtenerValorJson(body, "idSucursal")); } catch(Exception ignored){}

                        int idRol = 2;
                        try { idRol = Integer.parseInt(obtenerValorJson(body, "idRol")); } catch(Exception ignored){}

                        double comision = 2.5;
                        try { comision = Double.parseDouble(obtenerValorJson(body, "comision")); } catch(Exception ignored){}

                        boolean exito = dao.registrarEmpleado(dni, nombres, apellidos, telefono, usuario, password, idSucursal, idRol, comision);

                        String resp = "{\"exito\": " + exito + "}";
                        byte[] bytes = resp.getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                        exchange.sendResponseHeaders(200, bytes.length);
                        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
                    } catch (Exception e) {
                        e.printStackTrace();
                        exchange.sendResponseHeaders(500, -1);
                    }
                } else if ("PUT".equalsIgnoreCase(method)) {
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) sb.append(line);
                        String body = sb.toString();

                        int id = 0;
                        try { id = Integer.parseInt(obtenerValorJson(body, "idEmpleado")); } catch(Exception ignored){}
                        String dni = obtenerValorJson(body, "dni");
                        String nombres = obtenerValorJson(body, "nombres");
                        String apellidos = obtenerValorJson(body, "apellidos");
                        String telefono = obtenerValorJson(body, "telefono");
                        String usuario = obtenerValorJson(body, "usuario");
                        String password = obtenerValorJson(body, "password");
                        
                        int idSucursal = 1;
                        try { idSucursal = Integer.parseInt(obtenerValorJson(body, "idSucursal")); } catch(Exception ignored){}

                        int idRol = 2;
                        try { idRol = Integer.parseInt(obtenerValorJson(body, "idRol")); } catch(Exception ignored){}

                        double comision = 2.5;
                        try { comision = Double.parseDouble(obtenerValorJson(body, "comision")); } catch(Exception ignored){}

                        boolean exito = id > 0 && dao.actualizarEmpleado(id, dni, nombres, apellidos, telefono, usuario, password, idSucursal, idRol, comision);

                        String resp = "{\"exito\": " + exito + "}";
                        byte[] bytes = resp.getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                        exchange.sendResponseHeaders(200, bytes.length);
                        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
                    } catch (Exception e) {
                        e.printStackTrace();
                        exchange.sendResponseHeaders(500, -1);
                    }
                } else if ("DELETE".equalsIgnoreCase(method)) {
                    try {
                        String query = exchange.getRequestURI().getQuery();
                        int id = 0;
                        if (query != null && query.contains("id=")) {
                            String idStr = query.split("id=")[1].split("&")[0].trim();
                            id = Integer.parseInt(idStr);
                        }
                        boolean exito = id > 0 && dao.eliminarEmpleado(id);
                        String resp = "{\"exito\": " + exito + "}";
                        byte[] bytes = resp.getBytes(StandardCharsets.UTF_8);
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

        // =========================================================
        // 9. ENDPOINT: PROCESAR VENTA (/api/ventas/crear) -> SOPORTA ID_SUCURSAL
        // =========================================================
        server.createContext("/api/ventas/crear", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                configurarHeadersCORS(exchange);
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8));
                        StringBuilder bodyBuilder = new StringBuilder();
                        String linea;
                        while ((linea = reader.readLine()) != null) {
                            bodyBuilder.append(linea);
                        }
                        String body = bodyBuilder.toString();
                        
                        double total = 0.0;
                        try { total = Double.parseDouble(obtenerValorJson(body, "total")); } catch(Exception ignored){}

                        int idPago = 1;
                        try { idPago = Integer.parseInt(obtenerValorJson(body, "idPago")); } catch(Exception ignored){}

                        int cuotas = 1;
                        try { cuotas = Integer.parseInt(obtenerValorJson(body, "cuotas")); } catch(Exception ignored){}
                        
                        int idEmpleado = 1;
                        try {
                            String usuarioStr = obtenerValorJson(body, "usuario");
                            if (usuarioStr.isEmpty()) {
                                usuarioStr = obtenerValorJson(body, "idEmpleado");
                            }
                            if (!usuarioStr.isEmpty()) {
                                idEmpleado = Integer.parseInt(usuarioStr);
                            }
                        } catch (Exception ignored) {}

                        int idSucursal = 1;
                        try {
                            String sucStr = obtenerValorJson(body, "idSucursal");
                            if (sucStr.isEmpty()) sucStr = obtenerValorJson(body, "sucursal");
                            if (!sucStr.isEmpty() && !"undefined".equalsIgnoreCase(sucStr)) {
                                if (sucStr.matches("-?\\d+")) idSucursal = Integer.parseInt(sucStr);
                                else if (sucStr.toLowerCase().contains("sucursal")) idSucursal = Integer.parseInt(sucStr.replaceAll("[^0-9]", ""));
                            }
                        } catch (Exception ignored) {}

                        String jsonItems = "[]";
                        int idxItems = body.indexOf("\"items\":");
                        if (idxItems != -1) {
                            jsonItems = body.substring(idxItems + 8).trim();
                            if (jsonItems.endsWith("}")) {
                                jsonItems = jsonItems.substring(0, jsonItems.length() - 1).trim();
                            }
                        }

                        VentaDAO dao = new VentaDAO();
                        boolean exito = dao.procesarVenta(jsonItems, total, idPago, cuotas, idEmpleado, idSucursal);

                        String respuestaJson = "{\"exito\": " + exito + "}";
                        byte[] bytes = respuestaJson.getBytes(StandardCharsets.UTF_8);
                        
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

        // =========================================================
        // 10. ENDPOINT: ESTADÍSTICAS DEL EMPLEADO (/api/empleado/stats)
        // =========================================================
        server.createContext("/api/empleado/stats", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                configurarHeadersCORS(exchange);
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    try {
                        String query = exchange.getRequestURI().getQuery();
                        int idEmpleado = 0;
                        if (query != null && query.contains("ID_Empleado=")) {
                            try {
                                idEmpleado = Integer.parseInt(query.split("ID_Empleado=")[1].split("&")[0]);
                            } catch (Exception ignored) {}
                        }

                        int ventasRealizadas = 0;
                        double comisionAcumulada = 0.0;

                        try (Connection con = ConexionBD.obtenerConexion();
                             PreparedStatement ps = con.prepareStatement("SELECT ventas_realizadas, comision_acumulada FROM empleados WHERE ID_Empleado = ?")) {
                            ps.setInt(1, idEmpleado);
                            try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) {
                                    ventasRealizadas = rs.getInt("ventas_realizadas");
                                    comisionAcumulada = rs.getDouble("comision_acumulada");
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        String jsonRespuesta = "{\"ventasRealizadas\": " + ventasRealizadas + ", \"comisionAcumulada\": " + comisionAcumulada + "}";
                        byte[] bytes = jsonRespuesta.getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                        exchange.sendResponseHeaders(200, bytes.length);
                        try (OutputStream os = exchange.getResponseBody()) { 
                            os.write(bytes); 
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        exchange.sendResponseHeaders(500, -1);
                    }
                }
            }
        });

        // =========================================================
        // 11. ENDPOINT: VENTAS DE HOY DEL EMPLEADO (/api/empleado/ventas-hoy)
        // =========================================================
        server.createContext("/api/empleado/ventas-hoy", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                configurarHeadersCORS(exchange);
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    try {
                        String query = exchange.getRequestURI().getQuery();
                        int idEmpleado = 0;
                        if (query != null && query.contains("ID_Empleado=")) {
                            try {
                                idEmpleado = Integer.parseInt(query.split("ID_Empleado=")[1].split("&")[0]);
                            } catch (Exception ignored) {}
                        }

                        StringBuilder jsonBuilder = new StringBuilder("[");
                        boolean primero = true;

                        String sql = "SELECT v.ID_Venta, DATE_FORMAT(v.Fecha_Hora, '%H:%i') as Hora, v.Total_Venta, v.Cuotas, p.Metodo_Pago AS NombrePago " +
                                     "FROM ventas v " +
                                     "JOIN pago p ON v.ID_Pago = p.ID_Pago " +
                                     "WHERE v.ID_Empleado = ? AND DATE(v.Fecha_Hora) = CURDATE() ORDER BY v.ID_Venta DESC";

                        try (Connection con = ConexionBD.obtenerConexion();
                             PreparedStatement ps = con.prepareStatement(sql)) {
                            ps.setInt(1, idEmpleado);
                            try (ResultSet rs = ps.executeQuery()) {
                                while (rs.next()) {
                                    if (!primero) jsonBuilder.append(",");
                                    int idVenta = rs.getInt("ID_Venta");
                                    String hora = rs.getString("Hora");
                                    double total = rs.getDouble("Total_Venta");
                                    int cuotas = rs.getInt("Cuotas");
                                    String nombrePago = rs.getString("NombrePago");

                                    jsonBuilder.append("{")
                                               .append("\"idVenta\": ").append(idVenta).append(",")
                                               .append("\"hora\": \"").append(hora).append("\",")
                                               .append("\"total\": ").append(total).append(",")
                                               .append("\"cuotas\": ").append(cuotas).append(",")
                                               .append("\"metodoPago\": \"").append(nombrePago).append("\"")
                                               .append("}");
                                    primero = false;
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        jsonBuilder.append("]");

                        byte[] bytes = jsonBuilder.toString().getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                        exchange.sendResponseHeaders(200, bytes.length);
                        try (OutputStream os = exchange.getResponseBody()) { 
                            os.write(bytes); 
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        exchange.sendResponseHeaders(500, -1);
                    }
                }
            }
        });

        // =========================================================
        // 12. ENDPOINT: RESETEAR COMISIÓN (/api/empleados/resetear-uno)
        // =========================================================
        server.createContext("/api/empleados/resetear-uno", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                configurarHeadersCORS(exchange);
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) sb.append(line);
                        String body = sb.toString();

                        int idEmpleado = 0;
                        try { idEmpleado = Integer.parseInt(obtenerValorJson(body, "idEmpleado")); } catch(Exception ignored){}

                        EmpleadoDAO dao = new EmpleadoDAO();
                        boolean exito = idEmpleado > 0 && dao.resetearComisionEmpleado(idEmpleado);

                        String resp = "{\"exito\": " + exito + "}";
                        byte[] bytes = resp.getBytes(StandardCharsets.UTF_8);
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

        // =========================================================
        // 13. ENDPOINT: HISTORIAL DE CIERRES Y CIERRE DIARIO (/api/cierre-caja)
        // =========================================================
        server.createContext("/api/cierre-caja", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                configurarHeadersCORS(exchange);
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                String method = exchange.getRequestMethod();
                VentaDAO dao = new VentaDAO();

                if ("GET".equalsIgnoreCase(method)) {
                    try {
                        String query = exchange.getRequestURI().getQuery();
                        int idSucursal = 1;
                        if (query != null && query.contains("idSucursal=")) {
                            try { idSucursal = Integer.parseInt(query.split("idSucursal=")[1].split("&")[0]); } catch(Exception ignored){}
                        }

                        String jsonHistorial = dao.obtenerJsonHistorialCierres(idSucursal);
                        byte[] bytes = jsonHistorial.getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                        exchange.sendResponseHeaders(200, bytes.length);
                        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
                    } catch (Exception e) {
                        e.printStackTrace();
                        exchange.sendResponseHeaders(500, -1);
                    }
                } else if ("POST".equalsIgnoreCase(method)) {
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) sb.append(line);
                        String body = sb.toString();

                        String fecha = obtenerValorJson(body, "fecha"); 
                        int idSucursal = 1;
                        try {
                            String suc = obtenerValorJson(body, "idSucursal");
                            if (!suc.isEmpty()) idSucursal = Integer.parseInt(suc);
                        } catch(Exception ignored){}

                        boolean exito = dao.procesarCierreDiario(fecha.isEmpty() ? null : fecha, idSucursal);

                        String resp = "{\"exito\": " + exito + "}";
                        byte[] bytes = resp.getBytes(StandardCharsets.UTF_8);
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

        // =========================================================
        // 14. ENDPOINT: ESTADÍSTICAS DEL DASHBOARD Y REPORTES (/api/dashboard/stats)
        // =========================================================
        server.createContext("/api/dashboard/stats", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                configurarHeadersCORS(exchange);
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    try {
                        String query = exchange.getRequestURI().getQuery();
                        String periodo = "semanal";
                        int idSucursal = 1;

                        if (query != null) {
                            if (query.contains("periodo=")) {
                                try { periodo = query.split("periodo=")[1].split("&")[0]; } catch(Exception ignored){}
                            }
                            if (query.contains("idSucursal=")) {
                                try { idSucursal = Integer.parseInt(query.split("idSucursal=")[1].split("&")[0]); } catch(Exception ignored){}
                            }
                        }

                        double totalVendido = 0.0;
                        double costoTotalMercaderia = 0.0;
                        double valorTotalStock = 0.0; 
                        int cantidadVentas = 0;
                        int unidadesVendidas = 0;
                        int stockDisponible = 0; 
                        int stockBajo = 0;       
                        int stockAgotado = 0;    

                        StringBuilder jsonMetodosPago = new StringBuilder("[]");
                        StringBuilder jsonTopProductos = new StringBuilder("[]");
                        StringBuilder jsonTopEmpleados = new StringBuilder("[]");
                        StringBuilder jsonGrafico = new StringBuilder("[]");

                        String filtroFecha = "v.Fecha_Hora >= NOW() - INTERVAL 7 DAY";
                        if ("diario".equalsIgnoreCase(periodo) || "hoy".equalsIgnoreCase(periodo)) {
                            filtroFecha = "DATE(v.Fecha_Hora) = CURDATE()";
                        } else if ("mensual".equalsIgnoreCase(periodo) || "mes".equalsIgnoreCase(periodo)) {
                            filtroFecha = "MONTH(v.Fecha_Hora) = MONTH(NOW()) AND YEAR(v.Fecha_Hora) = YEAR(NOW())";
                        } else if ("todo".equalsIgnoreCase(periodo)) {
                            filtroFecha = "1=1";
                        }

                        try (Connection con = ConexionBD.obtenerConexion()) {
                            // Total vendido filtrado por sucursal (si idSucursal == 0, se toma general omitiendo el filtro o sumando todo)
                            String sqlVentas = (idSucursal == 0) 
                                ? "SELECT COALESCE(SUM(Total_Venta), 0) AS Total, COUNT(*) AS Cantidad FROM ventas v WHERE " + filtroFecha
                                : "SELECT COALESCE(SUM(Total_Venta), 0) AS Total, COUNT(*) AS Cantidad FROM ventas v WHERE v.ID_Sucursal = ? AND " + filtroFecha;
                            
                            try (PreparedStatement ps = con.prepareStatement(sqlVentas)) {
                                if (idSucursal != 0) ps.setInt(1, idSucursal);
                                try (ResultSet rs = ps.executeQuery()) {
                                    if (rs.next()) {
                                        totalVendido = rs.getDouble("Total");
                                        cantidadVentas = rs.getInt("Cantidad");
                                    }
                                }
                            }

                            // Costo exacto de mercadería vendida filtrado por sucursal
                            String sqlCosto = (idSucursal == 0)
                                ? "SELECT COALESCE(SUM(dv.Cantidad * p.Precio_Compra), 0) AS CostoTotal, COALESCE(SUM(dv.Cantidad), 0) AS Unidades " +
                                  "FROM detalles_venta dv JOIN ventas v ON dv.ID_Venta = v.ID_Venta JOIN productos p ON dv.ID_Producto = p.ID_Producto WHERE " + filtroFecha
                                : "SELECT COALESCE(SUM(dv.Cantidad * p.Precio_Compra), 0) AS CostoTotal, COALESCE(SUM(dv.Cantidad), 0) AS Unidades " +
                                  "FROM detalles_venta dv JOIN ventas v ON dv.ID_Venta = v.ID_Venta JOIN productos p ON dv.ID_Producto = p.ID_Producto WHERE v.ID_Sucursal = ? AND " + filtroFecha;

                            try (PreparedStatement ps = con.prepareStatement(sqlCosto)) {
                                if (idSucursal != 0) ps.setInt(1, idSucursal);
                                try (ResultSet rs = ps.executeQuery()) {
                                    if (rs.next()) {
                                        costoTotalMercaderia = rs.getDouble("CostoTotal");
                                        unidadesVendidas = rs.getInt("Unidades");
                                    }
                                }
                            }

                            // Estado del stock y cálculo del Valor Total del Inventario
                           String sqlStock = (idSucursal == 0)
                            ? "SELECT s.Cantidad, p.stock_minimo, COALESCE(p.Precio_Compra, 0) AS Precio_Compra FROM stock_sucursal s JOIN productos p ON s.ID_Producto = p.ID_Producto"
                            : "SELECT s.Cantidad, p.stock_minimo, COALESCE(p.Precio_Compra, 0) AS Precio_Compra FROM stock_sucursal s JOIN productos p ON s.ID_Producto = p.ID_Producto WHERE s.ID_Sucursal = ?";
                            try (PreparedStatement ps = con.prepareStatement(sqlStock)) {
                                if (idSucursal != 0) ps.setInt(1, idSucursal);
                                try (ResultSet rs = ps.executeQuery()) {
                                    while (rs.next()) {
                                        int stock = rs.getInt("Cantidad");
                                        int min = rs.getInt("Stock_Minimo");
                                        double precioCompra = rs.getDouble("Precio_Compra");

                                        valorTotalStock += (stock * precioCompra);
                                        
                                        if (stock == 0) {
                                            stockAgotado++; 
                                        } else if (stock <= min) {
                                            stockBajo++;    
                                        } else {
                                            stockDisponible++;    
                                        }
                                    }
                                }
                            }

                            // Métodos de pago reales
                            jsonMetodosPago = new StringBuilder("[");
                            String sqlPagos = (idSucursal == 0)
                                ? "SELECT p.Metodo_Pago AS NombrePago, COUNT(*) AS Cantidad, SUM(v.Total_Venta) AS Total FROM ventas v JOIN pago p ON v.ID_Pago = p.ID_Pago WHERE " + filtroFecha + " GROUP BY p.ID_Pago, p.Metodo_Pago"
                                : "SELECT p.Metodo_Pago AS NombrePago, COUNT(*) AS Cantidad, SUM(v.Total_Venta) AS Total FROM ventas v JOIN pago p ON v.ID_Pago = p.ID_Pago WHERE v.ID_Sucursal = ? AND " + filtroFecha + " GROUP BY p.ID_Pago, p.Metodo_Pago";

                            try (PreparedStatement ps = con.prepareStatement(sqlPagos)) {
                                if (idSucursal != 0) ps.setInt(1, idSucursal);
                                try (ResultSet rs = ps.executeQuery()) {
                                    boolean primeroPago = true;
                                    while (rs.next()) {
                                        if (!primeroPago) jsonMetodosPago.append(",");
                                        jsonMetodosPago.append("{")
                                                       .append("\"metodo\":\"").append(rs.getString("NombrePago")).append("\",")
                                                       .append("\"cantidad\":").append(rs.getInt("Cantidad")).append(",")
                                                       .append("\"total\":").append(rs.getDouble("Total"))
                                                       .append("}");
                                        primeroPago = false;
                                    }
                                }
                            }
                            jsonMetodosPago.append("]");

                            // Top 4 productos más vendidos
                            jsonTopProductos = new StringBuilder("[");
                            String sqlProductos = (idSucursal == 0)
                                ? "SELECT p.Nombre, COALESCE(c.Nombre_Categoria, 'General') AS Categoria, SUM(dv.Cantidad) AS UnidadesVendidas, SUM(dv.Cantidad * dv.Precio_Unitario) AS Total " +
                                  "FROM detalles_venta dv JOIN ventas v ON dv.ID_Venta = v.ID_Venta JOIN productos p ON dv.ID_Producto = p.ID_Producto LEFT JOIN categoria c ON p.ID_Categoria = c.ID_Categoria WHERE " + filtroFecha + " GROUP BY p.ID_Producto, p.Nombre, c.Nombre_Categoria ORDER BY UnidadesVendidas DESC LIMIT 4"
                                : "SELECT p.Nombre, COALESCE(c.Nombre_Categoria, 'General') AS Categoria, SUM(dv.Cantidad) AS UnidadesVendidas, SUM(dv.Cantidad * dv.Precio_Unitario) AS Total " +
                                  "FROM detalles_venta dv JOIN ventas v ON dv.ID_Venta = v.ID_Venta JOIN productos p ON dv.ID_Producto = p.ID_Producto LEFT JOIN categoria c ON p.ID_Categoria = c.ID_Categoria WHERE v.ID_Sucursal = ? AND " + filtroFecha + " GROUP BY p.ID_Producto, p.Nombre, c.Nombre_Categoria ORDER BY UnidadesVendidas DESC LIMIT 4";

                            try (PreparedStatement ps = con.prepareStatement(sqlProductos)) {
                                if (idSucursal != 0) ps.setInt(1, idSucursal);
                                try (ResultSet rs = ps.executeQuery()) {
                                    boolean primeroProd = true;
                                    while (rs.next()) {
                                        if (!primeroProd) jsonTopProductos.append(",");
                                        jsonTopProductos.append("{")
                                                       .append("\"nombre\":\"").append(rs.getString("Nombre").replace("\"", "\\\"")).append("\",")
                                                       .append("\"categoria\":\"").append(rs.getString("Categoria")).append("\",")
                                                       .append("\"unidades\":").append(rs.getInt("UnidadesVendidas")).append(",")
                                                       .append("\"total\":").append(rs.getDouble("Total"))
                                                       .append("}");
                                        primeroProd = false;
                                    }
                                }
                            }
                            jsonTopProductos.append("]");

                            // Top Vendedores
                            jsonTopEmpleados = new StringBuilder("[");
                            String sqlEmpleados = (idSucursal == 0)
                                ? "SELECT CONCAT(e.Nombres, ' ', e.Apellidos) AS NombreCompleto, COUNT(v.ID_Venta) AS Ventas, COALESCE(SUM(v.Total_Venta), 0) AS Total FROM empleados e LEFT JOIN ventas v ON e.ID_Empleado = v.ID_Empleado AND (" + filtroFecha + ") GROUP BY e.ID_Empleado, e.Nombres, e.Apellidos ORDER BY Total DESC, Ventas DESC LIMIT 3"
                                : "SELECT CONCAT(e.Nombres, ' ', e.Apellidos) AS NombreCompleto, COUNT(v.ID_Venta) AS Ventas, COALESCE(SUM(v.Total_Venta), 0) AS Total FROM empleados e LEFT JOIN ventas v ON e.ID_Empleado = v.ID_Empleado AND v.ID_Sucursal = ? AND (" + filtroFecha + ") WHERE e.ID_Sucursal = ? GROUP BY e.ID_Empleado, e.Nombres, e.Apellidos ORDER BY Total DESC, Ventas DESC LIMIT 3";

                            try (PreparedStatement ps = con.prepareStatement(sqlEmpleados)) {
                                if (idSucursal != 0) {
                                    ps.setInt(1, idSucursal);
                                    ps.setInt(2, idSucursal);
                                }
                                try (ResultSet rs = ps.executeQuery()) {
                                    boolean primeroEmp = true;
                                    while (rs.next()) {
                                        if (!primeroEmp) jsonTopEmpleados.append(",");
                                        jsonTopEmpleados.append("{")
                                                       .append("\"nombre\":\"").append(rs.getString("NombreCompleto")).append("\",")
                                                       .append("\"ventas\":").append(rs.getInt("Ventas")).append(",")
                                                       .append("\"total\":").append(rs.getDouble("Total"))
                                                       .append("}");
                                        primeroEmp = false;
                                    }
                                }
                            }
                            jsonTopEmpleados.append("]");

                            // Gráfico de rendimiento temporal
                            jsonGrafico = new StringBuilder("[");
                            String sqlGrafico = (idSucursal == 0)
                                ? "SELECT DATE(v.Fecha_Hora) AS FechaDia, SUM(v.Total_Venta) AS TotalDia FROM ventas v WHERE " + filtroFecha + " GROUP BY DATE(v.Fecha_Hora) ORDER BY FechaDia ASC"
                                : "SELECT DATE(v.Fecha_Hora) AS FechaDia, SUM(v.Total_Venta) AS TotalDia FROM ventas v WHERE v.ID_Sucursal = ? AND " + filtroFecha + " GROUP BY DATE(v.Fecha_Hora) ORDER BY FechaDia ASC";

                            try (PreparedStatement ps = con.prepareStatement(sqlGrafico)) {
                                if (idSucursal != 0) ps.setInt(1, idSucursal);
                                try (ResultSet rs = ps.executeQuery()) {
                                    boolean primeroGraf = true;
                                    while (rs.next()) {
                                        if (!primeroGraf) jsonGrafico.append(",");
                                        jsonGrafico.append("{")
                                                   .append("\"etiqueta\":\"").append(rs.getString("FechaDia")).append("\",")
                                                   .append("\"valor\":").append(rs.getDouble("TotalDia"))
                                                   .append("}");
                                        primeroGraf = false;
                                    }
                                }
                            }
                            jsonGrafico.append("]");

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        double gananciaNeta = totalVendido - costoTotalMercaderia;

                        StringBuilder json = new StringBuilder();
                        json.append("{")
                            .append("\"totalVendido\":").append(totalVendido).append(",")
                            .append("\"costoTotalMercaderia\":").append(costoTotalMercaderia).append(",")
                            .append("\"gananciaNeta\":").append(gananciaNeta).append(",")
                            .append("\"cantidadVentas\":").append(cantidadVentas).append(",")
                            .append("\"unidadesVendidas\":").append(unidadesVendidas).append(",")
                            .append("\"valorTotalStock\":").append(valorTotalStock).append(",")
                            .append("\"stockDisponible\":").append(stockDisponible).append(",")
                            .append("\"stockBien\":").append(stockDisponible).append(",")
                            .append("\"stockIntermedio\":").append(stockBajo).append(",")
                            .append("\"stockBajo\":").append(stockBajo).append(",") 
                            .append("\"stockAgotado\":").append(stockAgotado).append(",")
                            .append("\"metodosPago\":").append(jsonMetodosPago).append(",")
                            .append("\"topProductos\":").append(jsonTopProductos).append(",")
                            .append("\"topEmpleados\":").append(jsonTopEmpleados).append(",")
                            .append("\"grafico\":").append(jsonGrafico)
                            .append("}");

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

        // =========================================================
        // 15. ENDPOINT: MOVIMIENTOS DEL SISTEMA (/api/movimientos)
        // =========================================================
        server.createContext("/api/movimientos", exchange -> {
            configurarHeadersCORS(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                try {
                    String query = exchange.getRequestURI().getQuery();
                    int idSucursal = 1;
                    if (query != null && query.contains("idSucursal=")) {
                        try { idSucursal = Integer.parseInt(query.split("idSucursal=")[1].split("&")[0]); } catch(Exception ignored){}
                    }

                    StringBuilder json = new StringBuilder("{");
                    
                    int totalVentas = 0;
                    int totalCompras = 0;
                    int totalTransferencias = 0;
                    int totalPagos = 0;

                    try (Connection con = ConexionBD.obtenerConexion()) {
                        String sqlCount = (idSucursal == 0)
                            ? "SELECT " +
                              "(SELECT COUNT(*) FROM ventas) AS ventas, " +
                              "(SELECT COUNT(*) FROM historial_pedidos WHERE Estado = 'entregado' OR Estado = 'Recibido') AS compras, " +
                              "(SELECT COUNT(*) FROM pagos_proveedores) AS pagos, " +
                              "(SELECT COUNT(*) FROM transferencias) AS transferencias"
                            : "SELECT " +
                              "(SELECT COUNT(*) FROM ventas WHERE ID_Sucursal = ?) AS ventas, " +
                              "(SELECT COUNT(*) FROM historial_pedidos WHERE (Estado = 'entregado' OR Estado = 'Recibido') AND ID_Sucursal = ?) AS compras, " +
                              "(SELECT COUNT(*) FROM pagos_proveedores) AS pagos, " +
                              "(SELECT COUNT(*) FROM transferencias WHERE ID_Sucursal_Origen = ? OR ID_Sucursal_Destino = ?) AS transferencias";
                        
                        try (PreparedStatement ps = con.prepareStatement(sqlCount)) {
                            if (idSucursal != 0) {
                                ps.setInt(1, idSucursal);
                                ps.setInt(2, idSucursal);
                                ps.setInt(3, idSucursal);
                                ps.setInt(4, idSucursal);
                            }
                            try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) {
                                    totalVentas = rs.getInt("ventas");
                                    totalCompras = rs.getInt("compras");
                                    totalPagos = rs.getInt("pagos");
                                    totalTransferencias = rs.getInt("transferencias");
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    json.append("\"resumen\": {")
                        .append("\"ventas\": ").append(totalVentas).append(",")
                        .append("\"compras\": ").append(totalCompras).append(",")
                        .append("\"pagos\": ").append(totalPagos).append(",")
                        .append("\"transferencias\": ").append(totalTransferencias)
                        .append("},");

                    json.append("\"lista\": [");
                    boolean primero = true;

                    String sqlMovimientos = (idSucursal == 0)
                        ? "SELECT v.Fecha_Hora AS Fecha, 'venta' AS Tipo, CONCAT('Venta #', v.ID_Venta) AS Detalle, 'Ventas' AS Categoria, v.Total_Venta AS Cantidad, COALESCE(su.Nombre, 'Sucursal Central') AS Sucursal, CONCAT(e.Nombres, ' ', e.Apellidos) AS Responsable, CONCAT('VTA-', v.ID_Venta) AS Referencia FROM ventas v JOIN empleados e ON v.ID_Empleado = e.ID_Empleado LEFT JOIN sucursal su ON v.ID_Sucursal = su.ID_Sucursal " +
                          "UNION ALL " +
                          "SELECT h.fecha AS Fecha, 'compra' AS Tipo, COALESCE(h.proveedor, 'Proveedor General') AS Detalle, 'Compras / Pedidos' AS Categoria, CONCAT('-', h.total) AS Cantidad, COALESCE(su.Nombre, 'Sucursal Central') AS Sucursal, 'Administración' AS Responsable, CONCAT('PED-', h.id_historial) AS Referencia FROM historial_pedidos h LEFT JOIN sucursal su ON h.ID_Sucursal = su.ID_Sucursal WHERE (h.estado = 'entregado' OR h.estado = 'Recibido') " +
                          "UNION ALL " +
                          "SELECT t.Fecha AS Fecha, 'transferencia' AS Tipo, CONCAT('Transferencia a ', su2.Nombre) AS Detalle, 'Transferencias' AS Categoria, CONCAT('Traspaso #', t.ID_Transferencia) AS Cantidad, COALESCE(su1.Nombre, 'Sucursal Central') AS Sucursal, CONCAT(e.Nombres, ' ', e.Apellidos) AS Responsable, CONCAT('TRF-', t.ID_Transferencia) AS Referencia FROM transferencias t JOIN empleados e ON t.ID_Empleado = e.ID_Empleado LEFT JOIN sucursal su1 ON t.ID_Sucursal_Origen = su1.ID_Sucursal LEFT JOIN sucursal su2 ON t.ID_Sucursal_Destino = su2.ID_Sucursal " +
                          "ORDER BY Fecha DESC LIMIT 50"
                        : "SELECT v.Fecha_Hora AS Fecha, 'venta' AS Tipo, CONCAT('Venta #', v.ID_Venta) AS Detalle, 'Ventas' AS Categoria, v.Total_Venta AS Cantidad, COALESCE(su.Nombre, 'Sucursal Central') AS Sucursal, CONCAT(e.Nombres, ' ', e.Apellidos) AS Responsable, CONCAT('VTA-', v.ID_Venta) AS Referencia FROM ventas v JOIN empleados e ON v.ID_Empleado = e.ID_Empleado LEFT JOIN sucursal su ON v.ID_Sucursal = su.ID_Sucursal WHERE v.ID_Sucursal = ? " +
                          "UNION ALL " +
                          "SELECT h.fecha AS Fecha, 'compra' AS Tipo, COALESCE(h.proveedor, 'Proveedor General') AS Detalle, 'Compras / Pedidos' AS Categoria, CONCAT('-', h.total) AS Cantidad, COALESCE(su.Nombre, 'Sucursal Central') AS Sucursal, 'Administración' AS Responsable, CONCAT('PED-', h.id_historial) AS Referencia FROM historial_pedidos h LEFT JOIN sucursal su ON h.ID_Sucursal = su.ID_Sucursal WHERE (h.estado = 'entregado' OR h.estado = 'Recibido') AND h.ID_Sucursal = ? " +
                          "UNION ALL " +
                          "SELECT t.Fecha AS Fecha, 'transferencia' AS Tipo, CONCAT('Transferencia a ', su2.Nombre) AS Detalle, 'Transferencias' AS Categoria, CONCAT('Traspaso #', t.ID_Transferencia) AS Cantidad, COALESCE(su1.Nombre, 'Sucursal Central') AS Sucursal, CONCAT(e.Nombres, ' ', e.Apellidos) AS Responsable, CONCAT('TRF-', t.ID_Transferencia) AS Referencia FROM transferencias t JOIN empleados e ON t.ID_Empleado = e.ID_Empleado LEFT JOIN sucursal su1 ON t.ID_Sucursal_Origen = su1.ID_Sucursal LEFT JOIN sucursal su2 ON t.ID_Sucursal_Destino = su2.ID_Sucursal WHERE t.ID_Sucursal_Origen = ? OR t.ID_Sucursal_Destino = ? " +
                          "ORDER BY Fecha DESC LIMIT 50";

                    try (Connection con = ConexionBD.obtenerConexion();
                         PreparedStatement ps = con.prepareStatement(sqlMovimientos)) {
                        if (idSucursal != 0) {
                            ps.setInt(1, idSucursal);
                            ps.setInt(2, idSucursal);
                            ps.setInt(3, idSucursal);
                            ps.setInt(4, idSucursal);
                        }
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                if (!primero) json.append(",");
                                json.append("{")
                                    .append("\"fecha\": \"").append(rs.getString("Fecha")).append("\",")
                                    .append("\"tipo\": \"").append(rs.getString("Tipo")).append("\",")
                                    .append("\"detalle\": \"").append(rs.getString("Detalle").replace("\"", "\\\"")).append("\",")
                                    .append("\"categoria\": \"").append(rs.getString("Categoria")).append("\",")
                                    .append("\"cantidad\": \"").append(rs.getString("Cantidad")).append("\",")
                                    .append("\"sucursal\": \"").append(rs.getString("Sucursal")).append("\",")
                                    .append("\"responsable\": \"").append(rs.getString("Responsable")).append("\",")
                                    .append("\"referencia\": \"").append(rs.getString("Referencia")).append("\"")
                                    .append("}");
                                primero = false;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    json.append("]}");

                    byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) { 
                        os.write(bytes); 
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    exchange.sendResponseHeaders(500, -1);
                }
            }
        });

        server.setExecutor(null);
        System.out.println("=====================================================");
        System.out.println("Servidor unificado RUDAFER activo en http://localhost:" + puerto);
        System.out.println("=====================================================");
        server.start();
    }

    private static void configurarHeadersCORS(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
    }

    private static String obtenerValorJson(String json, String clave) {
        String patron = "\"" + clave + "\":";
        int inicio = json.indexOf(patron);
        if (inicio == -1) return "";
        inicio += patron.length();
        while (inicio < json.length() && (json.charAt(inicio) == ' ' || json.charAt(inicio) == '"')) {
            inicio++;
        }
        int fin = inicio;
        boolean esString = json.charAt(inicio - 1) == '"';
        if (esString) {
            fin = json.indexOf("\"", inicio);
        } else {
            while (fin < json.length() && json.charAt(fin) != ',' && json.charAt(fin) != '}') {
                fin++;
            }
        }
        return (fin != -1) ? json.substring(inicio, fin).trim() : "";
    }
}