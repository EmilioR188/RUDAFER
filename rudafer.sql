-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Servidor: 127.0.0.1
-- Tiempo de generación: 27-10-2026 a las 19:04:33
-- Versión del servidor: 10.4.32-MariaDB
-- Versión de PHP: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de datos: `rudafer`
--

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `categoria`
--

CREATE TABLE `categoria` (
  `ID_Categoria` int(11) NOT NULL,
  `Nombre_Categoria` varchar(50) NOT NULL,
  PRIMARY KEY (`ID_Categoria`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `categoria`
--

INSERT INTO `categoria` (`ID_Categoria`, `Nombre_Categoria`) VALUES
(1, 'Ferretería'),
(2, 'Pinturas'),
(3, 'Herramientas'),
(4, 'Sanitarios'),
(5, 'Corralón'),
(6, 'Electricidad');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `categorias`
--

CREATE TABLE `categorias` (
  `ID_Categoria` int(11) NOT NULL,
  `Nombre` varchar(100) NOT NULL,
  PRIMARY KEY (`ID_Categoria`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `categorias`
--

INSERT INTO `categorias` (`ID_Categoria`, `Nombre`) VALUES
(1, 'Ferretería'),
(2, 'Pinturas'),
(3, 'Herramientas'),
(4, 'Sanitarios'),
(5, 'Corralón'),
(6, 'Electricidad');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `cierre_caja`
--

CREATE TABLE `cierre_caja` (
  `ID_Cierre` int(11) NOT NULL,
  `Fecha` date NOT NULL,
  `Total_Vendido` decimal(12,2) NOT NULL DEFAULT 0.00,
  `Cantidad_Ventas` int(11) NOT NULL DEFAULT 0,
  `Unidades_Vendidas` int(11) NOT NULL DEFAULT 0,
  `Fecha_Cierre` datetime NOT NULL,
  `ID_Sucursal` int(11) DEFAULT 1,
  PRIMARY KEY (`ID_Cierre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `cierre_caja`
--

INSERT INTO `cierre_caja` (`ID_Cierre`, `Fecha`, `Total_Vendido`, `Cantidad_Ventas`, `Unidades_Vendidas`, `Fecha_Cierre`, `ID_Sucursal`) VALUES
(1, '2026-10-26', 387100.00, 9, 28, '2026-10-26 23:47:15', 1);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `detalles_transferencia`
--

CREATE TABLE `detalles_transferencia` (
  `ID_Detalle` int(11) NOT NULL,
  `ID_Transferencia` int(11) NOT NULL,
  `ID_Producto` int(11) NOT NULL,
  `Cantidad` int(11) NOT NULL,
  PRIMARY KEY (`ID_Detalle`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `detalles_venta`
--

CREATE TABLE `detalles_venta` (
  `ID_Detalle` int(11) NOT NULL,
  `ID_Venta` int(11) NOT NULL,
  `ID_Producto` int(11) NOT NULL,
  `Cantidad` int(11) NOT NULL,
  `Precio_Unitario` decimal(10,2) NOT NULL,
  `Subtotal` decimal(10,2) NOT NULL,
  PRIMARY KEY (`ID_Detalle`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `detalles_venta`
--

INSERT INTO `detalles_venta` (`ID_Detalle`, `ID_Venta`, `ID_Producto`, `Cantidad`, `Precio_Unitario`, `Subtotal`) VALUES
(1, 17, 23, 1, 0.00, 0.00),
(2, 18, 20, 1, 0.00, 0.00),
(3, 18, 21, 1, 0.00, 0.00),
(4, 18, 22, 1, 0.00, 0.00),
(5, 18, 23, 1, 0.00, 0.00),
(6, 18, 19, 1, 0.00, 0.00),
(7, 18, 24, 1, 0.00, 0.00),
(8, 19, 19, 5, 0.00, 0.00),
(9, 20, 20, 1, 0.00, 0.00),
(10, 21, 20, 1, 0.00, 0.00),
(11, 22, 20, 1, 0.00, 0.00),
(12, 22, 21, 1, 0.00, 0.00),
(13, 23, 21, 7, 0.00, 0.00),
(14, 24, 20, 1, 0.00, 0.00),
(15, 25, 22, 4, 0.00, 0.00),
(16, 26, 20, 1, 0.00, 0.00),
(17, 27, 20, 5, 0.00, 0.00),
(18, 28, 20, 1, 0.00, 0.00),
(19, 29, 22, 3, 0.00, 0.00);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `empleados`
--

CREATE TABLE `empleados` (
  `ID_Empleado` int(11) NOT NULL,
  `DNI` varchar(15) NOT NULL,
  `Nombres` varchar(50) NOT NULL,
  `Apellidos` varchar(50) NOT NULL,
  `Telefono` varchar(20) DEFAULT NULL,
  `Nombre_Usuario` varchar(40) DEFAULT NULL,
  `Contrasena` varchar(255) NOT NULL,
  `ID_Sucursal` int(11) NOT NULL,
  `ID_Rol` int(11) NOT NULL,
  `Comision` decimal(5,2) DEFAULT 2.50,
  `comision_acumulada` decimal(10,2) DEFAULT 0.00,
  `ventas_realizadas` int(11) DEFAULT 0,
  `Activo` tinyint(1) DEFAULT 1,
  PRIMARY KEY (`ID_Empleado`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `empleados`
--

INSERT INTO `empleados` (`ID_Empleado`, `DNI`, `Nombres`, `Apellidos`, `Telefono`, `Nombre_Usuario`, `Contrasena`, `ID_Sucursal`, `ID_Rol`, `Comision`, `comision_acumulada`, `ventas_realizadas`, `Activo`) VALUES
(1, '48112080', 'Facundo', 'Vazquez', '1199998888', 'Facundo', 'Campeon2019', 1, 1, 2.50, 0.00, 0, 1),
(2, '92785553', 'Felicita', 'Lopez', '1199998888', 'FeliLo', 'Rudafer1', 2, 2, 2.50, 637.50, 2, 1),
(3, '22425927', 'Julio', 'Vazquez', '1128947557', 'Julio El Ferretero', 'Rudafer1', 1, 2, 2.50, 750.00, 1, 1),
(4, '95989480', 'Santiago', 'Barrera', '1121132321', 'El mas gay', 'PENE123', 1, 2, 2.50, 0.00, 0, 0);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `historial_pedidos`
--

CREATE TABLE `historial_pedidos` (
  `id_historial` int(11) NOT NULL,
  `proveedor` varchar(150) DEFAULT NULL,
  `detalle_pedido` text NOT NULL,
  `fecha` timestamp NOT NULL DEFAULT current_timestamp(),
  `estado` varchar(50) DEFAULT 'Completado',
  `total` decimal(10,2) DEFAULT 0.00,
  `ID_Sucursal` int(11) DEFAULT 1,
  PRIMARY KEY (`id_historial`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `historial_pedidos`
--

INSERT INTO `historial_pedidos` (`id_historial`, `proveedor`, `detalle_pedido`, `fecha`, `estado`, `total`, `ID_Sucursal`) VALUES
(10, 'Proveedor General', '[{\"idProducto\":\"3\",\"nombre\":\"Cinta\",\"cantidad\":5}]', '2026-10-21 01:07:20', 'cancelado', 6000.00, 1),
(11, 'SyR', '[{\"idProducto\":\"8\",\"nombre\":\"Tarubo 10\",\"cantidad\":1000},{\"idProducto\":\"11\",\"nombre\":\"Mecha de madera\",\"cantidad\":2}]', '2026-10-25 00:18:41', 'entregado', 47222.00, 1),
(12, 'Andina', '[{\"idProducto\":\"9\",\"nombre\":\"Taladro\",\"cantidad\":2}]', '2026-10-25 00:40:08', 'entregado', 0.00, 1),
(13, 'undefined', '[{\"idProducto\":\"21\",\"nombre\":\"Latex 1L\",\"cantidad\":1},{\"idProducto\":\"20\",\"nombre\":\"Enduido 1Kg\",\"cantidad\":1},{\"idProducto\":\"22\",\"nombre\":\"Esmalte Sintetico Blanco Brillante 1/4\",\"cantidad\":1}]', '2026-10-25 05:16:44', 'cancelado', 14999.98, 1),
(14, 'undefined', '[{\"idProducto\":\"19\",\"nombre\":\"Lampara LEDD 10w\",\"cantidad\":1}]', '2026-10-25 05:17:17', 'cancelado', 1100.00, 1),
(15, 'Andina', '[{\"idProducto\":\"23\",\"nombre\":\"Entonador Azul Grande\",\"cantidad\":5}]', '2026-10-25 18:44:01', 'entregado', 18250.00, 1),
(16, 'POXIPOL', '[{\"idProducto\":\"24\",\"nombre\":\"La Gotita Chica\",\"cantidad\":5}]', '2026-10-25 19:30:36', 'cancelado', 8250.00, 1),
(17, 'Andina', '[{\"idProducto\":\"20\",\"nombre\":\"Enduido 1Kg\",\"cantidad\":3,\"precioUnitario\":3700}]', '2026-10-25 20:16:43', 'entregado', 11100.00, 1),
(18, 'Andina', '[{\"idProducto\":\"20\",\"nombre\":\"Enduido 1Kg\",\"cantidad\":10,\"precioUnitario\":3850},{\"idProducto\":\"22\",\"nombre\":\"Esmalte Sintetico Blanco Brillante 1/4\",\"cantidad\":10,\"precioUnitario\":3799.98},{\"idProducto\":\"21\",\"nombre\":\"Latex 1L\",\"cantidad\":10,\"precioUnitario\":7500}]', '2026-10-26 02:23:39', 'entregado', 151499.80, 1),
(19, 'Andina', '[{\"idProducto\":\"23\",\"nombre\":\"Entonador Azul Grande\",\"cantidad\":10,\"precioUnitario\":3650}]', '2026-10-26 20:12:41', 'entregado', 36500.00, 1),
(20, 'Andina', '[{\"idProducto\":\"20\",\"nombre\":\"Enduido 1Kg\",\"cantidad\":1,\"precioUnitario\":3850}]', '2026-10-26 20:14:52', 'cancelado', 3850.00, 1),
(21, 'Andina', '[{\"idProducto\":\"20\",\"nombre\":\"Enduido 1Kg\",\"cantidad\":5,\"precioUnitario\":3850},{\"idProducto\":\"21\",\"nombre\":\"Latex 1L\",\"cantidad\":5,\"precioUnitario\":7500}]', '2026-10-27 01:59:20', 'entregado', 56750.00, 1),
(22, 'SyR', '[{\"idProducto\":\"19\",\"nombre\":\"Lampara LEDD 10w\",\"cantidad\":3,\"precioUnitario\":1100}]', '2026-10-27 02:19:04', 'entregado', 3300.00, 1),
(23, 'Andina', '[{\"idProducto\":\"20\",\"nombre\":\"Enduido 1Kg\",\"cantidad\":4,\"precioUnitario\":3850}]', '2026-10-27 03:35:27', 'entregado', 38500.00, 1),
(24, 'POXIPOL', '[{\"idProducto\":\"24\",\"nombre\":\"La Gotita Chica\",\"cantidad\":10,\"precioUnitario\":1650}]', '2026-10-27 03:46:24', 'entregado', 16500.00, 1),
(25, 'Andina', '[{\"idProducto\":\"22\",\"nombre\":\"Esmalte Sintetico Blanco Brillante 1/4\",\"cantidad\":4,\"precioUnitario\":3799.98}]', '2026-10-27 06:00:58', 'entregado', 15199.92, 2),
(26, 'Andina', '[{\"idProducto\":\"22\",\"nombre\":\"Esmalte Sintetico Blanco Brillante 1/4\",\"cantidad\":3,\"precioUnitario\":3799.98}]', '2026-10-27 06:03:42', 'entregado', 11399.94, 1);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `pago`
--

CREATE TABLE `pago` (
  `ID_Pago` int(11) NOT NULL,
  `Metodo_Pago` varchar(40) NOT NULL,
  PRIMARY KEY (`ID_Pago`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `pago`
--

INSERT INTO `pago` (`ID_Pago`, `Metodo_Pago`) VALUES
(1, 'Efectivo'),
(2, 'Transferencia'),
(3, 'QR'),
(4, 'Tarjeta de Débito'),
(5, 'Tarjeta de Crédito'),
(6, 'Otros');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `pagos_proveedores`
--

CREATE TABLE `pagos_proveedores` (
  `ID_Pago` int(11) NOT NULL,
  `ID_Proveedor` int(11) NOT NULL,
  `Monto` decimal(10,2) NOT NULL,
  `Fecha` timestamp NOT NULL DEFAULT current_timestamp(),
  `Observacion` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ID_Pago`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `pagos_proveedores`
--

INSERT INTO `pagos_proveedores` (`ID_Pago`, `ID_Proveedor`, `Monto`, `Fecha`, `Observacion`) VALUES
(1, 2, 100000.00, '2026-10-25 19:36:04', ''),
(2, 4, 10000.00, '2026-10-27 03:49:26', '');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `productos`
--

CREATE TABLE `productos` (
  `ID_Producto` int(11) NOT NULL,
  `Codigo_Barras` varchar(50) NOT NULL,
  `Nombre` varchar(100) NOT NULL,
  `Descripcion` varchar(255) DEFAULT NULL,
  `Marca` varchar(50) DEFAULT NULL,
  `Estante` varchar(20) DEFAULT NULL,
  `Fila` varchar(20) DEFAULT NULL,
  `Precio_Compra` decimal(10,2) NOT NULL DEFAULT 0.00,
  `Precio_Venta` decimal(10,2) NOT NULL DEFAULT 0.00,
  `Stock_Minimo` int(11) DEFAULT 0,
  `Stock_Actual` int(11) DEFAULT 0,
  `ID_Categoria` int(11) DEFAULT NULL,
  `ID_Proveedor` int(11) DEFAULT NULL,
  PRIMARY KEY (`ID_Producto`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `productos`
--

INSERT INTO `productos` (`ID_Producto`, `Codigo_Barras`, `Nombre`, `Descripcion`, `Marca`, `Estante`, `Fila`, `Precio_Compra`, `Precio_Venta`, `Stock_Minimo`, `Stock_Actual`, `ID_Categoria`, `ID_Proveedor`) VALUES
(19, '5', 'Lampara LEDD 10w', 'Lampara LEDD 10w', 'CANDELA', '22', '22', 1100.00, 2000.00, 5, 100, 6, 3),
(20, '1', 'Enduido 1Kg', 'Enduido 1Kg', 'ANDINA', '1', '1', 3850.00, 6000.00, 4, 14, 2, 2),
(21, '2', 'Latex 1L', 'Latex 1L', 'ANDINA', '1', '2', 7500.00, 12000.00, 2, 5, 2, 2),
(22, '3', 'Esmalte Sintetico Blanco Brillante 1/4', ',', 'Obra Color', '1', '4', 3799.98, 0.00, 2, 5, 1, 2),
(23, '4', 'Entonador Azul Grande', ',', 'ANDINA', '22', '2', 3680.00, 5500.00, 2, 0, 1, 2),
(24, '6', 'La Gotita Chica', 'La Gotita Chica', 'POXIPOL', '1111', '111', 1650.00, 2600.00, 4, 11, 1, 4),
(26, '101', 'Asiento de Hinodoro Universal Blanco', ',', 'Faplast', '222', '22', 1000.00, 0.00, 2, 0, 1, NULL);

-- Modificación solicitada: el stock ahora se administra por sucursal.
ALTER TABLE `productos` DROP COLUMN `Stock_Actual`;
ALTER TABLE `productos` DROP COLUMN `Stock_Minimo`;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `proveedor`
--

CREATE TABLE `proveedor` (
  `ID_Proveedor` int(11) NOT NULL,
  `Nombre` varchar(50) NOT NULL,
  `Telefono` varchar(20) DEFAULT NULL,
  `Email` varchar(50) DEFAULT NULL,
  `Deuda_Total` decimal(10,2) DEFAULT 0.00,
  PRIMARY KEY (`ID_Proveedor`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `proveedor`
--

INSERT INTO `proveedor` (`ID_Proveedor`, `Nombre`, `Telefono`, `Email`, `Deuda_Total`) VALUES
(2, 'Andina', ',', ',', 26599.86),
(3, 'SyR', NULL, NULL, 0.00),
(4, 'POXIPOL', ',', '', 6500.00),
(5, 'Jorge Calefones', ',', ',', 304408.00);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `roles`
--

CREATE TABLE `roles` (
  `ID_Rol` int(11) NOT NULL,
  `Rol` varchar(50) NOT NULL,
  PRIMARY KEY (`ID_Rol`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `roles`
--

INSERT INTO `roles` (`ID_Rol`, `Rol`) VALUES
(1, 'ADMINISTRADOR'),
(2, 'EMPLEADO');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `stock_sucursal`
--

CREATE TABLE `stock_sucursal` (
  `ID_Sucursal` int(11) NOT NULL,
  `ID_Producto` int(11) NOT NULL,
  `Cantidad` int(11) NOT NULL DEFAULT 0,
  `Stock_Minimo` int(11) NOT NULL DEFAULT 5,
  PRIMARY KEY (`ID_Sucursal`, `ID_Producto`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `stock_sucursal`
--

INSERT INTO `stock_sucursal` (`ID_Sucursal`, `ID_Producto`, `Cantidad`, `Stock_Minimo`) VALUES
(1, 19, 100, 5),
(1, 20, 8, 4),
(1, 21, 5, 2),
(1, 22, 2, 2),
(1, 23, 0, 2),
(1, 24, 11, 4),
(1, 26, 5, 2),
(2, 22, 4, 2);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `sucursal`
--

CREATE TABLE `sucursal` (
  `ID_Sucursal` int(11) NOT NULL,
  `Nombre` varchar(50) NOT NULL,
  `Direccion` varchar(100) NOT NULL,
  `Telefono` varchar(20) DEFAULT NULL,
  `Email` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`ID_Sucursal`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `sucursal`
--

INSERT INTO `sucursal` (`ID_Sucursal`, `Nombre`, `Direccion`, `Telefono`, `Email`) VALUES
(1, 'Sucursal Central', 'Av. Principal 123', '1112345678', 'admin@rudafer.com'),
(2, 'Sucursal Norte', 'Av. Principal 123', '011-5555-1234', 'norte@rudafer.com');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `sucursales`
--

CREATE TABLE `sucursales` (
  `ID_Sucursal` int(11) NOT NULL,
  `Nombre` varchar(100) NOT NULL,
  `Direccion` varchar(255) DEFAULT NULL,
  `Telefono` varchar(50) DEFAULT NULL,
  `Activo` tinyint(1) DEFAULT 1,
  PRIMARY KEY (`ID_Sucursal`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `sucursales`
--

INSERT INTO `sucursales` (`ID_Sucursal`, `Nombre`, `Direccion`, `Telefono`, `Activo`) VALUES
(1, 'Sucursal Central', 'Av. Principal 123', '011-5555-1234', 1);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `transferencias`
--

CREATE TABLE `transferencias` (
  `ID_Transferencia` int(11) NOT NULL,
  `ID_Sucursal_Origen` int(11) NOT NULL,
  `ID_Sucursal_Destino` int(11) NOT NULL,
  `ID_Empleado` int(11) NOT NULL,
  `Fecha` datetime DEFAULT current_timestamp(),
  `Estado` varchar(50) DEFAULT 'COMPLETADA',
  `Observacion` text DEFAULT NULL,
  PRIMARY KEY (`ID_Transferencia`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `ventas`
--

CREATE TABLE `ventas` (
  `ID_Venta` int(11) NOT NULL,
  `Total_Venta` decimal(10,2) NOT NULL,
  `Fecha_Hora` datetime NOT NULL DEFAULT current_timestamp(),
  `ID_Pago` int(11) NOT NULL,
  `Cuotas` int(11) DEFAULT 1,
  `ID_Sucursal` int(11) NOT NULL,
  `ID_Empleado` int(11) NOT NULL,
  PRIMARY KEY (`ID_Venta`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `ventas`
--

INSERT INTO `ventas` (`ID_Venta`, `Total_Venta`, `Fecha_Hora`, `ID_Pago`, `Cuotas`, `ID_Sucursal`, `ID_Empleado`) VALUES
(17, 5500.00, '2026-10-26 15:29:01', 1, 1, 1, 3),
(18, 34600.00, '2026-10-26 15:33:34', 1, 1, 1, 3),
(19, 10000.00, '2026-10-26 16:31:02', 1, 1, 1, 2),
(20, 6000.00, '2026-10-26 17:17:09', 1, 1, 1, 4),
(21, 6000.00, '2026-10-26 21:54:04', 1, 1, 1, 3),
(22, 18000.00, '2026-10-26 21:54:20', 1, 1, 1, 2),
(23, 84000.00, '2026-10-26 21:54:56', 1, 1, 1, 2),
(24, 6000.00, '2026-10-26 22:06:01', 1, 1, 1, 3),
(25, 26000.00, '2026-10-26 22:21:14', 5, 3, 1, 3),
(26, 6000.00, '2026-10-27 00:13:35', 3, 1, 1, 3),
(27, 30000.00, '2026-10-27 03:13:47', 4, 1, 1, 3),
(28, 6000.00, '2026-10-27 03:14:57', 1, 1, 1, 2),
(29, 19500.00, '2026-10-27 03:15:12', 6, 1, 1, 2);

--
-- Índices para tablas volcadas
--

--
-- Indices de la tabla `categoria`
--


--
-- Indices de la tabla `categorias`
--


--
-- Indices de la tabla `cierre_caja`
--
ALTER TABLE `cierre_caja`
  ADD UNIQUE KEY `Fecha` (`Fecha`);

--
-- Indices de la tabla `detalles_transferencia`
--
ALTER TABLE `detalles_transferencia`
  ADD KEY `fk_det_trans` (`ID_Transferencia`),
  ADD KEY `fk_det_prod` (`ID_Producto`);

--
-- Indices de la tabla `detalles_venta`
--
ALTER TABLE `detalles_venta`
  ADD KEY `FK_Detalle_Venta` (`ID_Venta`),
  ADD KEY `FK_Detalle_Producto` (`ID_Producto`);

--
-- Indices de la tabla `empleados`
--
ALTER TABLE `empleados`
  ADD UNIQUE KEY `DNI` (`DNI`),
  ADD KEY `FK_Empleado_Sucursal` (`ID_Sucursal`),
  ADD KEY `FK_Empleado_Rol` (`ID_Rol`);

--
-- Indices de la tabla `historial_pedidos`
--


--
-- Indices de la tabla `pago`
--


--
-- Indices de la tabla `pagos_proveedores`
--
ALTER TABLE `pagos_proveedores`
  ADD KEY `ID_Proveedor` (`ID_Proveedor`);

--
-- Indices de la tabla `productos`
--
ALTER TABLE `productos`
  ADD UNIQUE KEY `Codigo_Barras` (`Codigo_Barras`),
  ADD KEY `FK_Producto_Categoria` (`ID_Categoria`),
  ADD KEY `FK_Producto_Proveedor` (`ID_Proveedor`);

--
-- Indices de la tabla `proveedor`
--


--
-- Indices de la tabla `roles`
--


--
-- Indices de la tabla `stock_sucursal`
--
ALTER TABLE `stock_sucursal`
  ADD KEY `FK_Stock_Producto` (`ID_Producto`);

--
-- Indices de la tabla `sucursal`
--


--
-- Indices de la tabla `sucursales`
--


--
-- Indices de la tabla `transferencias`
--
ALTER TABLE `transferencias`
  ADD KEY `fk_trans_origen` (`ID_Sucursal_Origen`),
  ADD KEY `fk_trans_destino` (`ID_Sucursal_Destino`),
  ADD KEY `fk_trans_empleado` (`ID_Empleado`);

--
-- Indices de la tabla `ventas`
--
ALTER TABLE `ventas`
  ADD KEY `FK_Venta_Pago` (`ID_Pago`),
  ADD KEY `FK_Venta_Sucursal` (`ID_Sucursal`),
  ADD KEY `FK_Venta_Empleado` (`ID_Empleado`);

--
-- AUTO_INCREMENT de las tablas volcadas
--

--
-- AUTO_INCREMENT de la tabla `categoria`
--
ALTER TABLE `categoria`
  MODIFY `ID_Categoria` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- AUTO_INCREMENT de la tabla `categorias`
--
ALTER TABLE `categorias`
  MODIFY `ID_Categoria` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- AUTO_INCREMENT de la tabla `cierre_caja`
--
ALTER TABLE `cierre_caja`
  MODIFY `ID_Cierre` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=8;

--
-- AUTO_INCREMENT de la tabla `detalles_transferencia`
--
ALTER TABLE `detalles_transferencia`
  MODIFY `ID_Detalle` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `detalles_venta`
--
ALTER TABLE `detalles_venta`
  MODIFY `ID_Detalle` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=20;

--
-- AUTO_INCREMENT de la tabla `empleados`
--
ALTER TABLE `empleados`
  MODIFY `ID_Empleado` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT de la tabla `historial_pedidos`
--
ALTER TABLE `historial_pedidos`
  MODIFY `id_historial` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=27;

--
-- AUTO_INCREMENT de la tabla `pago`
--
ALTER TABLE `pago`
  MODIFY `ID_Pago` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- AUTO_INCREMENT de la tabla `pagos_proveedores`
--
ALTER TABLE `pagos_proveedores`
  MODIFY `ID_Pago` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT de la tabla `productos`
--
ALTER TABLE `productos`
  MODIFY `ID_Producto` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=27;

--
-- AUTO_INCREMENT de la tabla `proveedor`
--
ALTER TABLE `proveedor`
  MODIFY `ID_Proveedor` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- AUTO_INCREMENT de la tabla `roles`
--
ALTER TABLE `roles`
  MODIFY `ID_Rol` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT de la tabla `sucursal`
--
ALTER TABLE `sucursal`
  MODIFY `ID_Sucursal` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT de la tabla `sucursales`
--
ALTER TABLE `sucursales`
  MODIFY `ID_Sucursal` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT de la tabla `transferencias`
--
ALTER TABLE `transferencias`
  MODIFY `ID_Transferencia` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `ventas`
--
ALTER TABLE `ventas`
  MODIFY `ID_Venta` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=30;

--
-- Restricciones para tablas volcadas
--

--
-- Filtros para la tabla `detalles_transferencia`
--
ALTER TABLE `detalles_transferencia`
  ADD CONSTRAINT `fk_det_prod` FOREIGN KEY (`ID_Producto`) REFERENCES `productos` (`ID_Producto`),
  ADD CONSTRAINT `fk_det_trans` FOREIGN KEY (`ID_Transferencia`) REFERENCES `transferencias` (`ID_Transferencia`) ON DELETE CASCADE;

--
-- Filtros para la tabla `detalles_venta`
--
ALTER TABLE `detalles_venta`
  ADD CONSTRAINT `FK_Detalle_Producto` FOREIGN KEY (`ID_Producto`) REFERENCES `productos` (`ID_Producto`),
  ADD CONSTRAINT `FK_Detalle_Venta` FOREIGN KEY (`ID_Venta`) REFERENCES `ventas` (`ID_Venta`) ON DELETE CASCADE;

--
-- Filtros para la tabla `empleados`
--
ALTER TABLE `empleados`
  ADD CONSTRAINT `FK_Empleado_Rol` FOREIGN KEY (`ID_Rol`) REFERENCES `roles` (`ID_Rol`),
  ADD CONSTRAINT `FK_Empleado_Sucursal` FOREIGN KEY (`ID_Sucursal`) REFERENCES `sucursal` (`ID_Sucursal`);

--
-- Filtros para la tabla `pagos_proveedores`
--
ALTER TABLE `pagos_proveedores`
  ADD CONSTRAINT `pagos_proveedores_ibfk_1` FOREIGN KEY (`ID_Proveedor`) REFERENCES `proveedor` (`ID_Proveedor`);

--
-- Filtros para la tabla `stock_sucursal`
--
ALTER TABLE `stock_sucursal`
  ADD CONSTRAINT `FK_Stock_Producto` FOREIGN KEY (`ID_Producto`) REFERENCES `productos` (`ID_Producto`),
  ADD CONSTRAINT `FK_Stock_Sucursal` FOREIGN KEY (`ID_Sucursal`) REFERENCES `sucursal` (`ID_Sucursal`);

--
-- Filtros para la tabla `transferencias`
--
ALTER TABLE `transferencias`
  ADD CONSTRAINT `fk_trans_destino` FOREIGN KEY (`ID_Sucursal_Destino`) REFERENCES `sucursal` (`ID_Sucursal`),
  ADD CONSTRAINT `fk_trans_empleado` FOREIGN KEY (`ID_Empleado`) REFERENCES `empleados` (`ID_Empleado`),
  ADD CONSTRAINT `fk_trans_origen` FOREIGN KEY (`ID_Sucursal_Origen`) REFERENCES `sucursal` (`ID_Sucursal`);

--
-- Filtros para la tabla `ventas`
--
ALTER TABLE `ventas`
  ADD CONSTRAINT `FK_Venta_Empleado` FOREIGN KEY (`ID_Empleado`) REFERENCES `empleados` (`ID_Empleado`),
  ADD CONSTRAINT `FK_Venta_Pago` FOREIGN KEY (`ID_Pago`) REFERENCES `pago` (`ID_Pago`),
  ADD CONSTRAINT `FK_Venta_Sucursal` FOREIGN KEY (`ID_Sucursal`) REFERENCES `sucursal` (`ID_Sucursal`);

-- Inicializa el stock por sucursal al insertar un nuevo producto.
DELIMITER //
CREATE TRIGGER `after_producto_insert`
AFTER INSERT ON `productos`
FOR EACH ROW
BEGIN
    INSERT INTO `stock_sucursal` (`ID_Producto`, `ID_Sucursal`, `Cantidad`, `Stock_Minimo`)
    SELECT NEW.`ID_Producto`, `ID_Sucursal`, 0, 5
    FROM `sucursal`;
END//
DELIMITER ;

COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
