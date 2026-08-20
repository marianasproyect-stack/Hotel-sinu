-- Roles del sistema. Se insertan solo si no existen todavia,
-- para que el script se pueda correr varias veces sin romper nada.
INSERT INTO ROLES (ID, NOMBRE)
SELECT rol_seq.NEXTVAL, 'CLIENTE' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ROLES WHERE NOMBRE = 'CLIENTE');

INSERT INTO ROLES (ID, NOMBRE)
SELECT rol_seq.NEXTVAL, 'ADMINISTRADOR' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ROLES WHERE NOMBRE = 'ADMINISTRADOR');

-- Habitaciones de ejemplo para que la pagina no se vea vacia la primera vez.
-- Si ya tienes habitaciones cargadas por Postman esto no las duplica.
INSERT INTO HABITACIONES (ID, NUMERO, TIPO, CAPACIDAD, PRECIO_NOCHE, DESCRIPCION, ESTADO)
SELECT habitacion_seq.NEXTVAL, '101', 'Sencilla', 2, 180000, 'Habitacion sencilla con cama doble, ideal para una o dos personas.', 'DISPONIBLE' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM HABITACIONES WHERE NUMERO = '101');

INSERT INTO HABITACIONES (ID, NUMERO, TIPO, CAPACIDAD, PRECIO_NOCHE, DESCRIPCION, ESTADO)
SELECT habitacion_seq.NEXTVAL, '102', 'Doble', 3, 240000, 'Habitacion doble con dos camas y espacio extra para una tercera persona.', 'DISPONIBLE' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM HABITACIONES WHERE NUMERO = '102');

INSERT INTO HABITACIONES (ID, NUMERO, TIPO, CAPACIDAD, PRECIO_NOCHE, DESCRIPCION, ESTADO)
SELECT habitacion_seq.NEXTVAL, '201', 'Suite', 4, 380000, 'Suite con sala separada, balcon y minibar.', 'DISPONIBLE' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM HABITACIONES WHERE NUMERO = '201');

INSERT INTO HABITACIONES (ID, NUMERO, TIPO, CAPACIDAD, PRECIO_NOCHE, DESCRIPCION, ESTADO)
SELECT habitacion_seq.NEXTVAL, '202', 'Suite Presidencial', 4, 620000, 'La habitacion mas grande del hotel, con terraza privada.', 'DISPONIBLE' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM HABITACIONES WHERE NUMERO = '202');

-- Correccion de datos viejos: antes de arreglar ReservaService, al crear
-- una reserva la habitacion se quedaba marcada como ESTADO = 'RESERVADA'
-- para siempre. Ya se corrigio el codigo para que eso no vuelva a pasar,
-- pero las habitaciones que ya habian quedado asi con el codigo viejo se
-- quedaban atascadas, porque ningun proceso las volvia a poner en
-- 'DISPONIBLE'. Como data.sql se ejecuta en cada arranque, este UPDATE
-- las libera automaticamente. Es seguro dejarlo aqui de forma permanente:
-- una vez liberadas, no queda ninguna en 'RESERVADA' y el UPDATE no
-- vuelve a afectar nada en los proximos arranques.
UPDATE HABITACIONES
SET ESTADO = 'DISPONIBLE'
WHERE ESTADO = 'RESERVADA';
