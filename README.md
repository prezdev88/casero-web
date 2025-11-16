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
2. Ejecutar `script.sql` para crear las tablas:
   ```bash
   psql -U casero -d casero -f script.sql
   ```
3. Ajusta credenciales en `src/main/resources/application.properties` si es necesario.

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
