import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProductoDAO {

    public static class Producto {
        private int idProducto;
        private String codigo;
        private String nombre;
        private String marca;
        private String categoria;
        private double precioVenta;
        private int stockActual;
        private int stockMinimo;
        private String proveedor;

        public Producto(int idProducto, String codigo, String nombre, String marca, String categoria, double precioVenta, int stockActual, int stockMinimo, String proveedor) {
            this.idProducto = idProducto;
            this.codigo = codigo;
            this.nombre = nombre;
            this.marca = marca;
            this.categoria = categoria;
            this.precioVenta = precioVenta;
            this.stockActual = stockActual;
            this.stockMinimo = stockMinimo;
            this.proveedor = proveedor != null && !proveedor.isEmpty() ? proveedor : "Sin Proveedor";
        }

        public int getIdProducto() { return idProducto; }
        public String getCodigo() { return codigo; }
        public String getNombre() { return nombre; }
        public String getMarca() { return marca; }
        public String getCategoria() { return categoria; }
        public double getPrecioVenta() { return precioVenta; }
        public int getStockActual() { return stockActual; }
        public int getStockMinimo() { return stockMinimo; }
        public String getProveedor() { return proveedor; }
    }

    public List<Producto> listarProductos(int idSucursal) {
        List<Producto> lista = new ArrayList<>();
        
        String sql;
        if (idSucursal == 0) {
            sql = "SELECT p.ID_Producto, p.Codigo_Barras, p.Nombre, p.Marca, p.Precio_Venta, " +
                  "COALESCE(MAX(s.Stock_Minimo), 5) AS Stock_Minimo, " +
                  "COALESCE(SUM(s.Stock_Actual), 0) AS Stock_Actual, " +
                  "COALESCE(pr.Nombre, 'Sin Proveedor') AS Nombre_Proveedor " +
                  "FROM productos p " +
                  "LEFT JOIN stock_sucursal s ON p.ID_Producto = s.ID_Producto " +
                  "LEFT JOIN proveedor pr ON p.ID_Proveedor = pr.ID_Proveedor " +
                  "GROUP BY p.ID_Producto, p.Codigo_Barras, p.Nombre, p.Marca, p.Precio_Venta, pr.Nombre";
        } else {
            // CORREGIDO: LEFT JOIN para garantizar que aparezcan productos con stock 0 o sin registro previo en la sucursal
            sql = "SELECT p.ID_Producto, p.Codigo_Barras, p.Nombre, p.Marca, p.Precio_Venta, " +
                  "COALESCE(s.Stock_Minimo, 5) AS Stock_Minimo, " +
                  "COALESCE(s.Stock_Actual, 0) AS Stock_Actual, " +
                  "COALESCE(pr.Nombre, 'Sin Proveedor') AS Nombre_Proveedor " +
                  "FROM productos p " +
                  "LEFT JOIN stock_sucursal s ON p.ID_Producto = s.ID_Producto AND s.ID_Sucursal = ? " +
                  "LEFT JOIN proveedor pr ON p.ID_Proveedor = pr.ID_Proveedor";
        }

        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            if (idSucursal != 0) {
                ps.setInt(1, idSucursal);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Producto(
                        rs.getInt("ID_Producto"),
                        rs.getString("Codigo_Barras"),
                        rs.getString("Nombre"),
                        rs.getString("Marca"),
                        "General",
                        rs.getDouble("Precio_Venta"),
                        rs.getInt("Stock_Actual"),
                        rs.getInt("Stock_Minimo"),
                        rs.getString("Nombre_Proveedor")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al listar productos: " + e.getMessage());
        }
        return lista;
    }

    public String obtenerJsonProductos(int idSucursal) {
        StringBuilder json = new StringBuilder("[");
        String sql;
        
        if (idSucursal == 0) {
            // Vista General: Suma stock de todas las sucursales
            sql = "SELECT p.ID_Producto, p.Codigo_Barras, p.Nombre, MAX(p.Descripcion) AS Descripcion, p.Marca, p.Estante, p.Fila, p.ID_Categoria, " +
                  "COALESCE(p.Precio_Compra, 0) AS Precio_Compra, " +
                  "COALESCE(p.Precio_Venta, 0) AS Precio_Venta, " +
                  "COALESCE(MAX(s.Stock_Minimo), 5) AS Stock_Minimo, " +
                  "COALESCE(SUM(s.Stock_Actual), 0) AS Stock_Actual, " +
                  "p.ID_Proveedor, " +
                  "COALESCE(pr.Nombre, 'Sin Proveedor Asignado') AS Nombre_Proveedor, " +
                  "COALESCE(c.Nombre_Categoria, 'General') AS Nombre_Categoria " +
                  "FROM productos p " +
                  "LEFT JOIN stock_sucursal s ON p.ID_Producto = s.ID_Producto " +
                  "LEFT JOIN proveedor pr ON p.ID_Proveedor = pr.ID_Proveedor " +
                  "LEFT JOIN categoria c ON p.ID_Categoria = c.ID_Categoria " +
                  "GROUP BY p.ID_Producto, p.Codigo_Barras, p.Nombre, p.Marca, p.Estante, p.Fila, p.ID_Categoria, p.Precio_Compra, p.Precio_Venta, p.ID_Proveedor, pr.Nombre, c.Nombre_Categoria";
        } else {
            // Sucursal Específica - CORREGIDO con LEFT JOIN para mostrar productos con stock 0
            sql = "SELECT p.ID_Producto, p.Codigo_Barras, p.Nombre, p.Descripcion, p.Marca, p.Estante, p.Fila, p.ID_Categoria, " +
                  "COALESCE(p.Precio_Compra, 0) AS Precio_Compra, " +
                  "COALESCE(p.Precio_Venta, 0) AS Precio_Venta, " +
                  "COALESCE(s.Stock_Minimo, 5) AS Stock_Minimo, " +
                  "COALESCE(s.Stock_Actual, 0) AS Stock_Actual, " +
                  "p.ID_Proveedor, " +
                  "COALESCE(pr.Nombre, 'Sin Proveedor Asignado') AS Nombre_Proveedor, " +
                  "COALESCE(c.Nombre_Categoria, 'General') AS Nombre_Categoria " +
                  "FROM productos p " +
                  "LEFT JOIN stock_sucursal s ON p.ID_Producto = s.ID_Producto AND s.ID_Sucursal = ? " +
                  "LEFT JOIN proveedor pr ON p.ID_Proveedor = pr.ID_Proveedor " +
                  "LEFT JOIN categoria c ON p.ID_Categoria = c.ID_Categoria";
        }
        
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            if (idSucursal != 0) {
                ps.setInt(1, idSucursal);
            }
            try (ResultSet rs = ps.executeQuery()) {
                boolean primero = true;
                while (rs.next()) {
                    if (!primero) {
                        json.append(",");
                    }
                    primero = false;
                    
                    int idProd = rs.getInt("ID_Producto");
                    int idProv = rs.getInt("ID_Proveedor");
                    String proveedor = rs.getString("Nombre_Proveedor");
                    if (proveedor == null) proveedor = "Sin Proveedor Asignado";
                    
                    String categoria = rs.getString("Nombre_Categoria");
                    if (categoria == null) categoria = "General";

                    String estante = rs.getString("Estante");
                    if (estante == null) estante = "";
                    
                    String fila = rs.getString("Fila");
                    if (fila == null) fila = "";

                    String descripcion = rs.getString("Descripcion");
                    if (descripcion == null) descripcion = "";

                    json.append("{")
                        .append("\"id\":").append(idProd).append(",")
                        .append("\"idProducto\":").append(idProd).append(",")
                        .append("\"codigo\":\"").append(escaparJsonString(rs.getString("Codigo_Barras"))).append("\",")
                        .append("\"nombre\":\"").append(escaparJsonString(rs.getString("Nombre"))).append("\",")
                        .append("\"marca\":\"").append(escaparJsonString(rs.getString("Marca"))).append("\",")
                        .append("\"descripcion\":\"").append(escaparJsonString(descripcion)).append("\",")
                        .append("\"idCategoria\":").append(rs.getInt("ID_Categoria")).append(",")
                        .append("\"stock\":").append(rs.getInt("Stock_Actual")).append(",")
                        .append("\"stockMinimo\":").append(rs.getInt("Stock_Minimo")).append(",")
                        .append("\"precioCompra\":").append(rs.getBigDecimal("Precio_Compra") != null ? rs.getBigDecimal("Precio_Compra").toPlainString() : "0.00").append(",")
                        .append("\"precioVenta\":").append(rs.getBigDecimal("Precio_Venta") != null ? rs.getBigDecimal("Precio_Venta").toPlainString() : "0.00").append(",")
                        // Campo legacy para compatibilidad con clientes que esperan la clave "precio".
                        .append("\"precio\":").append(rs.getBigDecimal("Precio_Venta") != null ? rs.getBigDecimal("Precio_Venta").toPlainString() : "0.00").append(",")
                        .append("\"estante\":\"").append(escaparJsonString(estante)).append("\",")
                        .append("\"fila\":\"").append(escaparJsonString(fila)).append("\",")
                        .append("\"idProveedor\":").append(idProv).append(",")
                        .append("\"proveedor\":\"").append(escaparJsonString(proveedor)).append("\",")
                        .append("\"categoria\":\"").append(escaparJsonString(categoria)).append("\"")
                        .append("}");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        json.append("]");
        return json.toString();
    }

    private static String escaparJsonString(String valor) {
        if (valor == null) return "";
        StringBuilder escapado = new StringBuilder(valor.length() + 16);
        for (int i = 0; i < valor.length(); i++) {
            char c = valor.charAt(i);
            switch (c) {
                case '\\': escapado.append("\\\\"); break;
                case '\"': escapado.append("\\\""); break;
                case '\b': escapado.append("\\b"); break;
                case '\f': escapado.append("\\f"); break;
                case '\n': escapado.append("\\n"); break;
                case '\r': escapado.append("\\r"); break;
                case '\t': escapado.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        escapado.append(String.format("\\u%04x", (int) c));
                    } else {
                        escapado.append(c);
                    }
            }
        }
        return escapado.toString();
    }

    public boolean registrarProducto(String codigoBarras, String nombre, String descripcion, String marca, 
                                     String estante, String fila, double precioCompra, double precioVenta, 
                                     int idCategoria, int idProveedor, int stockActual, int stockMinimo, int idSucursal) {
        Connection con = null;
        try {
            con = ConexionBD.obtenerConexion();
            con.setAutoCommit(false);

            String sqlProd = "INSERT INTO productos (Codigo_Barras, Nombre, Descripcion, Marca, Estante, Fila, Precio_Compra, Precio_Venta, ID_Categoria, ID_Proveedor) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            int idProductoGenerado = 0;
            try (PreparedStatement ps = con.prepareStatement(sqlProd, PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, codigoBarras);
                ps.setString(2, nombre);
                ps.setString(3, descripcion);
                ps.setString(4, marca);
                ps.setString(5, estante);
                ps.setString(6, fila);
                ps.setDouble(7, precioCompra);
                ps.setDouble(8, precioVenta);
                
                if (idCategoria > 0) ps.setInt(9, idCategoria); else ps.setNull(9, java.sql.Types.INTEGER);
                if (idProveedor > 0) ps.setInt(10, idProveedor); else ps.setNull(10, java.sql.Types.INTEGER);
                
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        idProductoGenerado = rs.getInt(1);
                    }
                }
            }

            if (idProductoGenerado <= 0) {
                throw new SQLException("No se obtuvo el ID del producto recién creado.");
            }

            // Garantiza una fila de stock por cada sucursal. Si el trigger after_producto_insert
            // ya las creó, el ON DUPLICATE KEY UPDATE conserva sus cantidades y mínimos actuales.
            String sqlInicializarStock =
                "INSERT INTO stock_sucursal (ID_Producto, ID_Sucursal, Cantidad, Stock_Minimo) " +
                "SELECT ?, s.ID_Sucursal, 0, 5 FROM sucursal s WHERE 1 = 1 " +
                "ON DUPLICATE KEY UPDATE ID_Producto = VALUES(ID_Producto)";
            try (PreparedStatement psInicializar = con.prepareStatement(sqlInicializarStock)) {
                psInicializar.setInt(1, idProductoGenerado);
                int filasProcesadas = psInicializar.executeUpdate();
                if (filasProcesadas <= 0) {
                    throw new SQLException("No se pudo inicializar el stock: verificá que exista al menos una sucursal.");
                }
            }

            // Guarda el stock inicial y mínimo para la sucursal elegida.
            String sqlStock = "INSERT INTO stock_sucursal (ID_Producto, ID_Sucursal, Cantidad, Stock_Minimo) " +
                              "VALUES (?, ?, ?, ?) " +
                              "ON DUPLICATE KEY UPDATE Cantidad = VALUES(Cantidad), Stock_Minimo = VALUES(Stock_Minimo)";
            try (PreparedStatement psStock = con.prepareStatement(sqlStock)) {
                psStock.setInt(1, idProductoGenerado);
                psStock.setInt(2, idSucursal > 0 ? idSucursal : 1);
                psStock.setInt(3, stockActual);
                psStock.setInt(4, stockMinimo);
                psStock.executeUpdate();
            }

            con.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("Error al registrar producto: " + e.getMessage());
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

    /** Resultado del intento de eliminación, con un código HTTP apto para la API. */
    public static class ResultadoEliminacion {
        public final boolean exito;
        public final int codigoHttp;
        public final String mensaje;

        public ResultadoEliminacion(boolean exito, int codigoHttp, String mensaje) {
            this.exito = exito;
            this.codigoHttp = codigoHttp;
            this.mensaje = mensaje;
        }
    }

    /**
     * Elimina un producto solo si no forma parte del historial de ventas o transferencias.
     * El stock por sucursal es dependiente y se elimina en la misma transacción.
     */
    public ResultadoEliminacion eliminarProducto(int idProducto) {
        if (idProducto <= 0) {
            return new ResultadoEliminacion(false, 400, "El ID del producto no es válido.");
        }

        Connection con = null;
        try {
            con = ConexionBD.obtenerConexion();
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT COUNT(*) FROM productos WHERE ID_Producto = ?")) {
                ps.setInt(1, idProducto);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || rs.getLong(1) == 0) {
                        con.rollback();
                        return new ResultadoEliminacion(false, 404, "No se encontró el producto indicado.");
                    }
                }
            }

            long ventas = contarReferencias(con,
                "SELECT COUNT(*) FROM detalles_venta WHERE ID_Producto = ?", idProducto);
            long transferencias = contarReferencias(con,
                "SELECT COUNT(*) FROM detalles_transferencia WHERE ID_Producto = ?", idProducto);

            if (ventas > 0 || transferencias > 0) {
                con.rollback();
                return new ResultadoEliminacion(false, 409,
                    "No se puede eliminar este producto porque tiene ventas o transferencias históricas asociadas. Se conservó para proteger el historial.");
            }

            try (PreparedStatement psStock = con.prepareStatement(
                    "DELETE FROM stock_sucursal WHERE ID_Producto = ?")) {
                psStock.setInt(1, idProducto);
                psStock.executeUpdate();
            }

            try (PreparedStatement psProducto = con.prepareStatement(
                    "DELETE FROM productos WHERE ID_Producto = ?")) {
                psProducto.setInt(1, idProducto);
                if (psProducto.executeUpdate() == 0) {
                    con.rollback();
                    return new ResultadoEliminacion(false, 404, "No se encontró el producto indicado.");
                }
            }

            con.commit();
            return new ResultadoEliminacion(true, 200, "Producto eliminado correctamente.");
        } catch (SQLException e) {
            if (con != null) {
                try { con.rollback(); } catch (SQLException ex) { e.addSuppressed(ex); }
            }
            System.err.println("Error al eliminar producto ID " + idProducto + ": " + e.getMessage());
            if (e.getErrorCode() == 1451 || (e.getSQLState() != null && e.getSQLState().startsWith("23"))) {
                return new ResultadoEliminacion(false, 409,
                    "No se puede eliminar el producto porque existen registros relacionados. Se conservaron los datos históricos.");
            }
            return new ResultadoEliminacion(false, 500,
                "Ocurrió un error de base de datos al intentar eliminar el producto.");
        } finally {
            if (con != null) {
                try { con.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    private long contarReferencias(Connection con, String sql, int idProducto) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        }
    }

    public boolean actualizarProducto(int idProducto, String codigo, String nombre, String descripcion, String marca, 
                                     String estante, String fila, double precioCompra, double precioVenta, 
                                     int idCategoria, int idProveedor, int stockActual, int stockMinimo, int idSucursal) {
        Connection con = null;
        try {
            con = ConexionBD.obtenerConexion();
            con.setAutoCommit(false);

            String sqlProd = "UPDATE productos SET Codigo_Barras = ?, Nombre = ?, Descripcion = ?, Marca = ?, " +
                             "Estante = ?, Fila = ?, Precio_Compra = ?, Precio_Venta = ?, ID_Categoria = ?, ID_Proveedor = ? WHERE ID_Producto = ?";

            try (PreparedStatement ps = con.prepareStatement(sqlProd)) {
                ps.setString(1, codigo);
                ps.setString(2, nombre);
                ps.setString(3, descripcion);
                ps.setString(4, marca);
                ps.setString(5, estante);
                ps.setString(6, fila);
                ps.setDouble(7, precioCompra);
                ps.setDouble(8, precioVenta);
                
                if (idCategoria > 0) ps.setInt(9, idCategoria); else ps.setNull(9, java.sql.Types.INTEGER);
                if (idProveedor > 0) ps.setInt(10, idProveedor); else ps.setNull(10, java.sql.Types.INTEGER);
                ps.setInt(11, idProducto);
                ps.executeUpdate();
            }

            String sqlStock = "INSERT INTO stock_sucursal (ID_Producto, ID_Sucursal, Cantidad, Stock_Minimo) " +
                              "VALUES (?, ?, ?, ?) " +
                              "ON DUPLICATE KEY UPDATE Cantidad = VALUES(Cantidad), Stock_Minimo = VALUES(Stock_Minimo)";

            try (PreparedStatement psStock = con.prepareStatement(sqlStock)) {
                psStock.setInt(1, idProducto);
                psStock.setInt(2, idSucursal > 0 ? idSucursal : 1);
                psStock.setInt(3, stockActual);
                psStock.setInt(4, stockMinimo);
                psStock.executeUpdate();
            }

            con.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("Error al actualizar producto: " + e.getMessage());
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
}
