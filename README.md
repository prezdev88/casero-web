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
2. Ajusta credenciales en `src/main/resources/application.properties` si es necesario.
3. Al iniciar la aplicación, Flyway creará automáticamente el esquema inicial.

### Migraciones (Flyway)

Al iniciar la aplicación, Flyway ejecuta automáticamente los scripts en `src/main/resources/db/migration`.
- `V1__initial_schema.sql` define las tablas base (sector, customer, statistic, transaction).
- `V2__pin_security_config.sql` crea la tabla `pin_security_config` y carga el PIN por defecto (`0000`).
- Agrega nuevos cambios como `V3__lo_que_sea.sql`, `V4__...` siguiendo la convención.

### Ejecución

```bash
cd java-migration
mvn spring-boot:run
```

La aplicación quedará disponible en `http://localhost:8080`.

### Publicación de versiones

Este repositorio usa [`standard-version`](https://github.com/conventional-changelog/standard-version) para generar changelog y bumps semánticos a partir de commits convencionales. Luego de preparar tus cambios:

```bash
npm run release        # patch
npm run release:minor  # o release:major según corresponda
```

El comando actualiza `pom.xml`, `package.json`, el `CHANGELOG.md` (si existe) y crea un tag anotado. Solo queda revisar los cambios, hacer commit y `git push --follow-tags`.

### Funcionalidades cubiertas

- CRUD de clientes con búsqueda acento-insensible (la lista solo se llena cuando usas el buscador).
- Visualización de detalle y movimientos por cliente.
- Registro de ventas (incluye tipo de venta e items), pagos, devoluciones y condonaciones.
- Eliminación de movimientos con recálculo automático de deuda y estadísticas.
- Estadísticas globales (deuda total, promedio, mejores/peores clientes) y reportes por rango de fechas (tarjetas terminadas, nuevas, mantenciones, prendas, cobros y ventas).
- Gestión de sectores desde BD (seleccionas uno existente al crear clientes).
- Plantillas Thymeleaf autosuficientes servidas desde el mismo backend.
