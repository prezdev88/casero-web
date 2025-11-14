## Casero Web (Spring Boot)

Proyecto web que replica todas las funciones de la app Android original (gestión de clientes, ventas, pagos, devoluciones, condonaciones, saldos iniciales, consultas y estadísticas).

### Requisitos

- Java 17
- Maven 3.9+
- PostgreSQL 14+ (con extensión `unaccent`)

### Configuración rápida

1. Crear la base de datos:
   ```sql
   CREATE DATABASE casero;
   CREATE USER casero WITH PASSWORD 'casero';
   GRANT ALL PRIVILEGES ON DATABASE casero TO casero;
   ```
2. Ejecutar `java-migration/script.sql` para crear las tablas:
   ```bash
   psql -U casero -d casero -f java-migration/script.sql
   ```
3. Copia el `casero.sqlite` original junto al `jar` (por ejemplo dentro de `java-migration/` o montado como volumen) o indica su ruta con `casero.import.sqlite-path`.
4. Ajusta credenciales en `src/main/resources/application.yml` si es necesario.

### Ejecución

```bash
cd java-migration
mvn spring-boot:run
```

La aplicación quedará disponible en `http://localhost:8080`.

### Funcionalidades cubiertas

- CRUD de clientes con búsqueda acento-insensible (la lista solo se llena cuando usas el buscador).
- Visualización de detalle y movimientos por cliente.
- Registro de ventas (incluye tipo de venta e items), pagos, devoluciones y condonaciones.
- Eliminación de movimientos con recálculo automático de deuda y estadísticas.
- Estadísticas globales (deuda total, promedio, mejores/peores clientes) y reportes por rango de fechas (tarjetas terminadas, nuevas, mantenciones, prendas, cobros y ventas).
- Gestión de sectores desde BD (seleccionas uno existente al crear clientes).
- Plantillas Thymeleaf autosuficientes servidas desde el mismo backend.

### Importar datos legacy desde SQLite

La aplicación importa automáticamente los datos del archivo `casero.sqlite` al iniciarse, siempre y cuando:

1. `casero.import.enabled` se mantenga en `true` (valor por defecto).
2. El archivo indicado por `casero.import.sqlite-path` exista (por defecto `casero.sqlite` en el directorio de ejecución).
3. Las tablas de PostgreSQL estén vacías (el import solo se ejecuta una vez).

Puedes sobreescribir la ruta del archivo al ejecutar:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--casero.import.sqlite-path=/ruta/al/casero.sqlite"
```

En Docker, monta el archivo dentro del contenedor (por ejemplo `-v $(pwd)/casero.sqlite:/app/casero.sqlite`) para que el importador lo detecte en `/app/casero.sqlite`.

Si prefieres una migración manual más tradicional, aún puedes usar herramientas como `pgloader` siguiendo los pasos anteriores.
