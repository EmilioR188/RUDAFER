<?php
$servername = "localhost";
$username = "root";
$password = "usbw";
$dbname = "empresa";

try {
    $conn = new PDO("mysql:host=$servername;dbname=$dbname;charset=utf8", $username, $password);
    $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    $conn->exec("SET NAMES utf8");
} catch(PDOException $e) {
    echo "Error en la conexión: " . $e->getMessage();
}
?>
