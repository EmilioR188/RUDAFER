<?php
// Reemplazá estos valores con los que te de Render en su panel
$host = "://render.com"; 
$db   = "rudafer-dbb";
$user = "rudafer_1_user";
$pass = "BsyoENN4kDEaRnPopJtH6dheUk7Asmxi";
$port = "5432"; // El puerto por defecto de PostgreSQL siempre es 5432

try {
    // Configuramos el DSN para PostgreSQL (fijate que dice 'pgsql:')
    $dsn = "pgsql:host=$host;port=$port;dbname=$db;";
    
    // Creamos la conexión PDO
    $conexion = new PDO($dsn, $user, $pass, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC
    ]);

    echo "¡Conexión exitosa a la base de datos de Render desde Buenos Aires!";

} catch (PDOException $e) {
    // Si algo falla (ej. internet cortado o datos mal puestos), te avisa acá
    echo "Error al conectar: " . $e->getMessage();
}
?>
