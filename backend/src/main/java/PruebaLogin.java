public class PruebaLogin {
    public static void main(String[] args) {
        EmpleadoDAO dao = new EmpleadoDAO();

        // Probamos con el usuario administrador inicial cargado en la base de datos
        // DNI: 12345678 | Contraseña: admin123
        EmpleadoDAO.SesionUsuario usuario = dao.validarLogin("12345678", "admin123");

        if (usuario != null) {
            System.out.println("---------------------------------------------");
            System.out.println("¡INICIO DE SESIÓN EXITOSO!");
            System.out.println("Usuario: " + usuario.getNombreCompleto());
            System.out.println("DNI: " + usuario.getDni());
            System.out.println("Rol: " + usuario.getRol());
            System.out.println("---------------------------------------------");
        } else {
            System.out.println("Error: Credenciales incorrectas o usuario no encontrado.");
        }
    }
}
