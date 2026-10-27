import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PedidoDAO {

    public static class ItemPedido {
        private int idProducto;
        private String nombreProducto;
        private String codigoBarras;
        private int stockActual;
        private int stockMinimo;
        private int cantidadSugerida;
        private String nombreProveedor;

        public ItemPedido(int idProducto, String nombreProducto, String codigoBarras, int stockActual, int stockMinimo, String nombreProveedor) {
            this.idProducto = idProducto;
            this.nombreProducto = nombreProducto;
            this.codigoBarras = codigoBarras;
            this.stockActual = stockActual;
            this.stockMinimo = stockMinimo;
            this.cantidadSugerida = 0; 
            this.nombreProveedor = nombreProveedor != null && !nombreProveedor.isEmpty() ? nombreProveedor : "Sin Proveedor Asignado";
        }

        public int getIdProducto() { return idProducto; }
        public String getNombreProducto() { return nombreProducto; }
        public String getCodigoBarras() { return codigoBarras; }
        public int getStockActual() { return stockActual; }
        public int getStockMinimo() { return stockMinimo; }
        public int getCantidadSugerida() { return cantidadSugerida; }
        public String getNombreProveedor() { return nombreProveedor; }
    }

    public List<ItemPedido> listarProductosParaReponer() {
        List<ItemPedido> lista = new ArrayList<>();
        
        String sql = "SELECT p.ID_Producto, p.Nombre, p.Codigo_Barras, p.Stock_Minimo, " +
                     "pr.Nombre AS Proveedor " +
                     "FROM productos p " +
                     "LEFT JOIN proveedor pr ON p.ID_Proveedor = pr.ID_Proveedor";

        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(new ItemPedido(
                    rs.getInt("ID_Producto"),
                    rs.getString("Nombre"),
                    rs.getString("Codigo_Barras"),
                    0,
                    rs.getInt("Stock_Minimo"),
                    rs.getString("Proveedor")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error al listar productos para reposición: " + e.getMessage());
        }
        return lista;
    }

    public boolean guardarPedidoRealizado(String proveedor, String detalleJson, double total, int idSucursal) {
        if (detalleJson != null && detalleJson.endsWith("\",\"total")) {
            detalleJson = detalleJson.substring(0, detalleJson.length() - 8);
        }
        
        String sql = "INSERT INTO historial_pedidos (proveedor, detalle_pedido, total, estado, ID_Sucursal) VALUES (?, ?, ?, 'realizado', ?)";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, proveedor);
            ps.setString(2, detalleJson);
            ps.setDouble(3, total);
            ps.setInt(4, idSucursal > 0 ? idSucursal : 1);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String obtenerJsonHistorial() {
        StringBuilder json = new StringBuilder("[");
        String sql = "SELECT h.id_historial, h.proveedor, h.detalle_pedido, h.total, h.fecha, h.estado, h.ID_Sucursal, s.Nombre AS NombreSucursal " +
                     "FROM historial_pedidos h " +
                     "LEFT JOIN sucursal s ON h.ID_Sucursal = s.ID_Sucursal " +
                     "ORDER BY h.fecha DESC";
        
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            boolean primero = true;
            while (rs.next()) {
                if (!primero) json.append(",");
                primero = false;
                
                String detalle = rs.getString("detalle_pedido");
                if (detalle == null) detalle = "[]";
                detalle = detalle.replace("\"", "\\\"");
                
                String proveedor = rs.getString("proveedor");
                if (proveedor == null) proveedor = "Sin Proveedor";

                String estado = rs.getString("estado");
                if (estado == null) estado = "realizado";

                String nombreSucursal = rs.getString("NombreSucursal");
                if (nombreSucursal == null) nombreSucursal = "Sucursal Central";

                json.append("{")
                    .append("\"id\":").append(rs.getInt("id_historial")).append(",")
                    .append("\"proveedor\":\"").append(proveedor).append("\",")
                    .append("\"total\":").append(rs.getDouble("total")).append(",")
                    .append("\"estado\":\"").append(estado).append("\",")
                    .append("\"nombreSucursal\":\"").append(nombreSucursal).append("\",")
                    .append("\"detalle\":\"").append(detalle).append("\",")
                    .append("\"fecha\":\"").append(rs.getString("fecha")).append("\"")
                    .append("}");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        json.append("]");
        return json.toString();
    }

    public boolean procesarLlegadaPedido(String jsonBody, int idHistorialPedido) {
        Connection con = null;
        try {
            con = ConexionBD.obtenerConexion();
            con.setAutoCommit(false); 

            if (jsonBody == null || jsonBody.trim().isEmpty() || jsonBody.equals("[]")) {
                return false;
            }

            String proveedorPedido = "";
            int idSucursalPedido = 1;
            
            String sqlGetInfo = "SELECT proveedor, ID_Sucursal FROM historial_pedidos WHERE id_historial = ?";
            try (PreparedStatement psP = con.prepareStatement(sqlGetInfo)) {
                psP.setInt(1, idHistorialPedido);
                try (ResultSet rsP = psP.executeQuery()) {
                    if (rsP.next()) {
                        proveedorPedido = rsP.getString("proveedor");
                        idSucursalPedido = rsP.getInt("ID_Sucursal");
                        if (idSucursalPedido <= 0) idSucursalPedido = 1;
                    }
                }
            }

            String limpio = jsonBody.replace("[", "").replace("]", "").trim();
            String[] objetos = limpio.split("\\},\\s*\\{");
            
            double nuevoTotalPedido = 0.0;
            List<DatosItemRecepcion> itemsProcesar = new ArrayList<>();

            for (String obj : objetos) {
                obj = obj.replace("{", "").replace("}", "").replace("\"", "");
                String[] pares = obj.split(",");
                
                int idProducto = 0;
                int cantidadRecibida = 0;
                double nuevoPrecioCompra = 0.0;

                for (String par : pares) {
                    String[] keyValue = par.split(":");
                    if (keyValue.length >= 2) {
                        String clave = keyValue[0].trim();
                        String valor = keyValue[1].trim();
                        if (clave.equals("idProducto")) {
                            idProducto = Integer.parseInt(valor);
                        } else if (clave.equals("cantidad")) {
                            cantidadRecibida = Integer.parseInt(valor);
                        } else if (clave.equals("nuevoPrecioCompra") || clave.equals("precioUnitario")) {
                            nuevoPrecioCompra = Double.parseDouble(valor);
                        }
                    }
                }

                if (idProducto > 0) {
                    nuevoTotalPedido += (cantidadRecibida * nuevoPrecioCompra);
                    itemsProcesar.add(new DatosItemRecepcion(idProducto, cantidadRecibida, nuevoPrecioCompra));
                }
            }

            String sqlUpdateEstadoTotal = "UPDATE historial_pedidos SET estado = 'entregado', total = ? WHERE id_historial = ?";
            try (PreparedStatement psE = con.prepareStatement(sqlUpdateEstadoTotal)) {
                psE.setDouble(1, nuevoTotalPedido);
                psE.setInt(2, idHistorialPedido);
                psE.executeUpdate();
            }

            // CORREGIDO: Sin Stock_Minimo en stock_sucursal
            String sqlUpdateStock = "INSERT INTO stock_sucursal (ID_Producto, ID_Sucursal, Cantidad) " +
                                    "VALUES (?, ?, ?) " +
                                    "ON DUPLICATE KEY UPDATE Cantidad = Cantidad + ?";
            try (PreparedStatement ps = con.prepareStatement(sqlUpdateStock)) {
                for (DatosItemRecepcion item : itemsProcesar) {
                    ps.setInt(1, item.idProducto);
                    ps.setInt(2, idSucursalPedido);
                    ps.setInt(3, item.cantidad);
                    ps.setInt(4, item.cantidad);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            // Actualizar precio de compra unitario en la tabla productos
            String sqlUpdatePrecio = "UPDATE productos SET Precio_Compra = ? WHERE ID_Producto = ?";
            try (PreparedStatement ps = con.prepareStatement(sqlUpdatePrecio)) {
                for (DatosItemRecepcion item : itemsProcesar) {
                    ps.setDouble(1, item.precio);
                    ps.setInt(2, item.idProducto);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            // Sumar el total del pedido a la deuda total del proveedor correspondiente
            if (proveedorPedido != null && !proveedorPedido.isEmpty() && !proveedorPedido.equalsIgnoreCase("Proveedor General")) {
                String sqlSumaDeuda = "UPDATE proveedor SET deuda_total = deuda_total + ? WHERE Nombre = ?";
                try (PreparedStatement psD = con.prepareStatement(sqlSumaDeuda)) {
                    psD.setDouble(1, nuevoTotalPedido);
                    psD.setString(2, proveedorPedido);
                    psD.executeUpdate();
                }
            }
            con.commit();
            return true;

        } catch (Exception e) {
            System.err.println("Error al procesar la llegada del pedido: " + e.getMessage());
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

    public boolean cancelarPedido(int idHistorial) {
        String sql = "UPDATE historial_pedidos SET Estado = 'cancelado' WHERE ID_Historial = ?";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idHistorial);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al cancelar pedido: " + e.getMessage());
            return false;
        }
    }

    private static class DatosItemRecepcion {
        int idProducto;
        int cantidad;
        double precio;

        public DatosItemRecepcion(int idProducto, int cantidad, double precio) {
            this.idProducto = idProducto;
            this.cantidad = cantidad;
            this.precio = precio;
        }
    }
}
