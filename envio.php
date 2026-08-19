<?php
// Incluimos la conexión con la base de datos
include_once("conexion.php");

// Capturamos los datos del formulario (enviados por POST)
$dni = $_POST['DNI'] ?? '';
$clave = $_POST['clave'] ?? '';
$pin = $_POST['pin'] ?? '';

// Verificamos que no estén vacíos (por seguridad)
if (empty($dni) || empty($clave) || empty($pin)) {
    echo "<div class='mensaje error'>Por favor, complete todos los campos.</div>";
    exit;
}

// Preparamos la consulta
$verificar = $conn->prepare("
    SELECT DNI, clave, pin 
    FROM usuarios 
    WHERE DNI = :dni AND clave = :clave AND pin = :pin
");

// Enlazamos los valores
$verificar->bindParam(":dni", $dni, PDO::PARAM_INT);
$verificar->bindParam(":clave", $clave, PDO::PARAM_STR);
$verificar->bindParam(":pin", $pin, PDO::PARAM_STR);

// Ejecutamos la consulta
$verificar->execute();

// Verificamos si hay resultados
if ($verificar->rowCount() > 0) {
    // Si el login es exitoso, redirigimos a la página de ofertas
    header("Location: NEX/ofertas.html");
    exit;
} else {
    // Si es incorrecto, mostramos mensaje de error con estilo
    ?>
    <!DOCTYPE html>
    <html lang="es">
    <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Login - Error</title>
        <link rel="stylesheet" href="estilo.css"> <!-- tu CSS principal -->
        <style>
            /* Estilo básico por si no carga el CSS */
            body {
                font-family: Arial, sans-serif;
                background-color: #f2f2f2;
                text-align: center;
            }
            .mensaje {
                width: 300px;
                margin: 100px auto;
                padding: 20px;
                border-radius: 10px;
                font-size: 20px;
                color: white;
            }
            .error {
                background-color: #ff4d4d;
            }
        </style>
    </head>
    <body>
        <div class="mensaje error">Usuario, clave o PIN incorrectos</div>
        <p><a href="index.php">Volver al login</a></p>
    </body>
    </html>
    <?php
}
?>
