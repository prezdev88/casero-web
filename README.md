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

### Autenticación con PIN único

- El acceso web exige iniciar sesión con un PIN único (sin usuarios/contraseñas convencionales).
- El sistema trae dos perfiles creados desde Flyway:
  - **Administrador**: PIN `1111` (`role: ADMIN`), con permisos completos y acceso al panel “Admin Settings”.
  - **Normal**: PIN `2222` (`role: NORMAL`), con las mismas funciones del sistema pero sin acceso al panel administrativo.
- Puedes cambiar los PIN registrando nuevos usuarios en la tabla `app_user` (columnas `pin_hash`, `pin_salt` y `pin_fingerprint`).

#### Duración de sesión configurable

El tiempo de sesión se controla con la propiedad estándar de Spring Boot:

```properties
server.servlet.session.timeout=1m
```

Puedes reemplazar ese valor por el período que necesites (`45m`, `12h`, `2d`, etc.) en el `application.properties` o en el archivo de perfil correspondiente.***

### Panel “Admin Settings”

Los usuarios con rol `ADMIN` verán una opción extra en la barra superior para administrar PIN y perfiles:
- Listar usuarios existentes con su rol.
- Crear nuevos usuarios con rol `ADMIN` o `NORMAL`.
- Cambiar el PIN de cualquier usuario desde la web (el sistema regenera automáticamente la sal y el fingerprint).

### Migraciones (Flyway)

Al iniciar la aplicación, Flyway ejecuta automáticamente los scripts en `src/main/resources/db/migration`.
- `V1__initial_schema.sql` define las tablas base (sector, customer, statistic, transaction).
- `V2__app_user_security.sql` crea los usuarios con PIN y roles (`ADMIN` y `NORMAL`).
- `V3__adjust_pin_column_types.sql` homologa los tipos de columnas de PIN a `VARCHAR` para que coincidan con el mapeo JPA.
- Agrega nuevos cambios como `V3__lo_que_sea.sql`, `V4__...` siguiendo la convención.

### Ejecución

```bash
cd java-migration
mvn spring-boot:run
```

La aplicación quedará disponible en `http://localhost:8080`.

### Documentación de API (OpenAPI/Swagger UI)

- La especificación OpenAPI está en `http://localhost:8080/v3/api-docs`.
- La UI interactiva (Swagger UI) está en `http://localhost:8080/swagger-ui/index.html`.
- Perfiles:
  - **local / qa**: docs y UI habilitados.
  - **prod**: deshabilitados por configuración (`springdoc.api-docs.enabled=false`, `springdoc.swagger-ui.enabled=false`).
  Ajusta estas flags en los properties de cada perfil si necesitas exponer/ocultar la documentación.

### Tests E2E (Playwright)

- Specs en `e2e/`, config en `playwright.config.js`, runner en `run-tests.sh`.
- Local: `./run-tests.sh` (levanta Postgres con `start-postgre.sh`, instala deps y por defecto inicia la app con `mvn spring-boot:run -Dspring-boot.run.profiles=local`; pasa `E2E_START_COMMAND=""` si ya la tienes arriba). Variables: `BASE_URL` (default `http://localhost:8080`), `ADMIN_PIN` (default `1111`), `PLAYWRIGHT_HEADLESS` (`false` para ver el navegador), `PLAYWRIGHT_WORKERS` (default `1`).


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
