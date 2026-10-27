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
                  "COALESCE(p.Stock_Minimo, 5) AS Stock_Minimo, " +
                  "COALESCE(SUM(s.Cantidad), 0) AS Stock_Actual, " +
                  "COALESCE(pr.Nombre, 'Sin Proveedor') AS Nombre_Proveedor " +
                  "FROM productos p " +
                  "LEFT JOIN stock_sucursal s ON p.ID_Producto = s.ID_Producto " +
                  "LEFT JOIN proveedor pr ON p.ID_Proveedor = pr.ID_Proveedor " +
                  "GROUP BY p.ID_Producto, p.Codigo_Barras, p.Nombre, p.Marca, p.Precio_Venta, p.Stock_Minimo, pr.Nombre";
        } else {
            // CORREGIDO: LEFT JOIN para garantizar que aparezcan productos con stock 0 o sin registro previo en la sucursal
            sql = "SELECT p.ID_Producto, p.Codigo_Barras, p.Nombre, p.Marca, p.Precio_Venta, " +
                  "COALESCE(p.Stock_Minimo, 5) AS Stock_Minimo, " +
                  "COALESCE(s.Cantidad, 0) AS Stock_Actual, " +
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
            sql = "SELECT p.ID_Producto, p.Codigo_Barras, p.Nombre, p.Marca, p.Estante, p.Fila, " +
                  "COALESCE(p.Precio_Compra, 0) AS Precio_Compra, " +
                  "COALESCE(p.Precio_Venta, 0) AS Precio_Venta, " +
                  "COALESCE(p.Stock_Minimo, 5) AS Stock_Minimo, " +
                  "COALESCE(SUM(s.Cantidad), 0) AS Stock_Actual, " +
                  "p.ID_Proveedor, " +
                  "COALESCE(pr.Nombre, 'Sin Proveedor Asignado') AS Nombre_Proveedor, " +
                  "COALESCE(c.Nombre_Categoria, 'General') AS Nombre_Categoria " +
                  "FROM productos p " +
                  "LEFT JOIN stock_sucursal s ON p.ID_Producto = s.ID_Producto " +
                  "LEFT JOIN proveedor pr ON p.ID_Proveedor = pr.ID_Proveedor " +
                  "LEFT JOIN categoria c ON p.ID_Categoria = c.ID_Categoria " +
                  "GROUP BY p.ID_Producto, p.Codigo_Barras, p.Nombre, p.Marca, p.Estante, p.Fila, p.Precio_Compra, p.Precio_Venta, p.Stock_Minimo, p.ID_Proveedor, pr.Nombre, c.Nombre_Categoria";
        } else {
            // Sucursal Específica - CORREGIDO con LEFT JOIN para mostrar productos con stock 0
            sql = "SELECT p.ID_Producto, p.Codigo_Barras, p.Nombre, p.Marca, p.Estante, p.Fila, " +
                  "COALESCE(p.Precio_Compra, 0) AS Precio_Compra, " +
                  "COALESCE(p.Precio_Venta, 0) AS Precio_Venta, " +
                  "COALESCE(p.Stock_Minimo, 5) AS Stock_Minimo, " +
                  "COALESCE(s.Cantidad, 0) AS Stock_Actual, " +
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

                    json.append("{")
                        .append("\"id\":").append(idProd).append(",")
                        .append("\"idProducto\":").append(idProd).append(",")
                        .append("\"codigo\":\"").append(rs.getString("Codigo_Barras") != null ? rs.getString("Codigo_Barras") : "").append("\",")
                        .append("\"nombre\":\"").append(rs.getString("Nombre").replace("\"", "\\\"")).append("\",")
                        .append("\"marca\":\"").append(rs.getString("Marca") != null ? rs.getString("Marca").replace("\"", "\\\"") : "").append("\",")
                        .append("\"stock\":").append(rs.getInt("Stock_Actual")).append(",")
                        .append("\"stockMinimo\":").append(rs.getInt("Stock_Minimo")).append(",")
                        .append("\"precioCompra\":").append(rs.getDouble("Precio_Compra")).append(",")
                        .append("\"precioVenta\":").append(rs.getDouble("Precio_Venta")).append(",")
                        .append("\"estante\":\"").append(estante.replace("\"", "\\\"")).append("\",")
                        .append("\"fila\":\"").append(fila.replace("\"", "\\\"")).append("\",")
                        .append("\"idProveedor\":").append(idProv).append(",")
                        .append("\"proveedor\":\"").append(proveedor.replace("\"", "\\\"")).append("\",")
                        .append("\"categoria\":\"").append(categoria.replace("\"", "\\\"")).append("\"")
                        .append("}");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        json.append("]");
        return json.toString();
    }

    public boolean registrarProducto(String codigoBarras, String nombre, String descripcion, String marca, 
                                     String estante, String fila, double precioCompra, double precioVenta, 
                                     int idCategoria, int idProveedor, int stockActual, int stockMinimo, int idSucursal) {
        Connection con = null;
        try {
            con = ConexionBD.obtenerConexion();
            con.setAutoCommit(false);

            String sqlProd = "INSERT INTO productos (Codigo_Barras, Nombre, Descripcion, Marca, Estante, Fila, Precio_Compra, Precio_Venta, Stock_Minimo, ID_Categoria, ID_Proveedor) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

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
                ps.setInt(9, stockMinimo);
                
                if (idCategoria > 0) ps.setInt(10, idCategoria); else ps.setNull(10, java.sql.Types.INTEGER);
                if (idProveedor > 0) ps.setInt(11, idProveedor); else ps.setNull(11, java.sql.Types.INTEGER);
                
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        idProductoGenerado = rs.getInt(1);
                    }
                }
            }

            if (idProductoGenerado > 0) {
                String sqlStock = "INSERT INTO stock_sucursal (ID_Producto, ID_Sucursal, Cantidad) " +
                                  "VALUES (?, ?, ?)";
                try (PreparedStatement psStock = con.prepareStatement(sqlStock)) {
                    psStock.setInt(1, idProductoGenerado);
                    psStock.setInt(2, idSucursal > 0 ? idSucursal : 1);
                    psStock.setInt(3, stockActual);
                    psStock.executeUpdate();
                }
            }

            con.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("Error al registrar producto: " + e.getMessage());
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

    public boolean eliminarProducto(int idProducto) {
        String sql = "DELETE FROM productos WHERE ID_Producto = ?";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al eliminar producto: " + e.getMessage());
            return false;
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
                             "Estante = ?, Fila = ?, Precio_Compra = ?, Precio_Venta = ?, Stock_Minimo = ?, ID_Categoria = ?, ID_Proveedor = ? WHERE ID_Producto = ?";

            try (PreparedStatement ps = con.prepareStatement(sqlProd)) {
                ps.setString(1, codigo);
                ps.setString(2, nombre);
                ps.setString(3, descripcion);
                ps.setString(4, marca);
                ps.setString(5, estante);
                ps.setString(6, fila);
                ps.setDouble(7, precioCompra);
                ps.setDouble(8, precioVenta);
                ps.setInt(9, stockMinimo);
                
                if (idCategoria > 0) ps.setInt(10, idCategoria); else ps.setNull(10, java.sql.Types.INTEGER);
                if (idProveedor > 0) ps.setInt(11, idProveedor); else ps.setNull(11, java.sql.Types.INTEGER);
                ps.setInt(12, idProducto);
                ps.executeUpdate();
            }

            String sqlStock = "INSERT INTO stock_sucursal (ID_Producto, ID_Sucursal, Cantidad) " +
                              "VALUES (?, ?, ?) " +
                              "ON DUPLICATE KEY UPDATE Cantidad = VALUES(Cantidad)";

            try (PreparedStatement psStock = con.prepareStatement(sqlStock)) {
                psStock.setInt(1, idProducto);
                psStock.setInt(2, idSucursal > 0 ? idSucursal : 1);
                psStock.setInt(3, stockActual);
                psStock.executeUpdate();
            }

            con.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("Error al actualizar producto: " + e.getMessage());
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
