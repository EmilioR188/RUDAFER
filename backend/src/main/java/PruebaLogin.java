public class PruebaLogin {
    public static void main(String[] args) {
        String dni = System.getenv("TEST_DNI");
        String password = System.getenv("TEST_PASSWORD");
        if (dni == null || dni.isBlank() || password == null || password.isBlank()) {
            System.err.println("Definí TEST_DNI y TEST_PASSWORD como variables de entorno para ejecutar esta prueba.");
            System.exit(1);
        }

        EmpleadoDAO dao = new EmpleadoDAO();
        EmpleadoDAO.SesionUsuario usuario = dao.validarLogin(dni, password);
        if (usuario != null) {
            System.out.println("Inicio de sesión de prueba correcto para: " + usuario.getNombreCompleto());
        } else {
            System.err.println("Las credenciales de prueba no son válidas.");
            System.exit(1);
        }
    }
}
