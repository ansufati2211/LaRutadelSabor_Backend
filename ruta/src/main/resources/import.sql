-- ============================================================
-- SCRIPT DE INICIALIZACIÓN: LA RUTA DEL SABOR (RAILWAY)
-- MODO: TEXTO PLANO (SIN ENCRIPTACIÓN)
-- ============================================================

-- 1. LIMPIEZA (Opcional, útil para resetear errores)
-- DELETE FROM pedido_detallado; DELETE FROM pedido; DELETE FROM cliente; DELETE FROM producto; DELETE FROM categoria; DELETE FROM rol;

-- 2. ROLES
INSERT INTO rol (name, aud_anulado, created_at, updated_at) VALUES ('ADMIN', false, NOW(), NOW()) ON CONFLICT (name) DO NOTHING;
INSERT INTO rol (name, aud_anulado, created_at, updated_at) VALUES ('USER', false, NOW(), NOW()) ON CONFLICT (name) DO NOTHING;
INSERT INTO rol (name, aud_anulado, created_at, updated_at) VALUES ('VENDEDOR', false, NOW(), NOW()) ON CONFLICT (name) DO NOTHING;
INSERT INTO rol (name, aud_anulado, created_at, updated_at) VALUES ('DELIVERY', false, NOW(), NOW()) ON CONFLICT (name) DO NOTHING;

-- 3. CATEGORÍA
INSERT INTO categoria (categoria, icono, aud_anulado, created_at, updated_at) 
VALUES ('Hamburguesas', '🍔', false, NOW(), NOW());

-- 4. PRODUCTO
INSERT INTO producto (producto, descripcion, precio, stock, imagen, aud_anulado, created_at, updated_at, categoria_id) 
VALUES (
    'Hamburguesa Clásica', 
    'Deliciosa carne 100% res, lechuga fresca, tomate y nuestras salsas secretas.', 
    15.00, 
    50, 
    'https://photos.app.goo.gl/Rcu5etrBh7jKwDZS9', 
    false, 
    NOW(), 
    NOW(), 
    (SELECT id FROM categoria WHERE categoria = 'Hamburguesas' LIMIT 1)
);

-- 5. USUARIOS (CONTRASEÑAS EN TEXTO PLANO)
-- ATENCIÓN: Las contraseñas aquí deben coincidir EXACTAMENTE con lo que escribes en el login.

-- A. ADMINISTRADOR
-- Usuario: admin@ruta.com | Pass: admin
INSERT INTO cliente (nombre, apellido, correo, contraseña, telefono, direccion, aud_anulado, created_at, updated_at, rol_id)
VALUES ('Super', 'Admin', 'admin@ruta.com', 'admin', '999111222', 'Oficina Central', false, NOW(), NOW(), (SELECT id FROM rol WHERE name = 'ADMIN'));

-- B. VENDEDOR
-- Usuario: vendedor@ruta.com | Pass: vendedor
INSERT INTO cliente (nombre, apellido, correo, contraseña, telefono, direccion, aud_anulado, created_at, updated_at, rol_id)
VALUES ('Juan', 'Vendedor', 'vendedor@ruta.com', 'vendedor', '999333444', 'Local Principal', false, NOW(), NOW(), (SELECT id FROM rol WHERE name = 'VENDEDOR'));

-- C. DELIVERY
-- Usuario: delivery@ruta.com | Pass: delivery
INSERT INTO cliente (nombre, apellido, correo, contraseña, telefono, direccion, aud_anulado, created_at, updated_at, rol_id)
VALUES ('Pedro', 'Motorizado', 'delivery@ruta.com', 'delivery', '999555666', 'En Ruta', false, NOW(), NOW(), (SELECT id FROM rol WHERE name = 'DELIVERY'));

-- D. CLIENTE
-- Usuario: cliente@prueba.com | Pass: cliente
INSERT INTO cliente (nombre, apellido, correo, contraseña, telefono, direccion, aud_anulado, created_at, updated_at, rol_id)
VALUES ('Cliente', 'Fiel', 'cliente@prueba.com', 'cliente', '999777888', 'Av. Siempre Viva 123', false, NOW(), NOW(), (SELECT id FROM rol WHERE name = 'USER'));