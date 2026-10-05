# Implementación de las cuatro mejoras D04/D05

Implementación de D04 y las tres filas D05 de [improvements.md](improvements.md), aplicando la skill `prezdev`. El usuario solicitó expresamente completar las cuatro separaciones. Se conserva la evidencia del análisis y se actualiza cada decisión según su implementación.

## D04 — Formularios financieros y comandos de aplicación

**Evidencia inicial.** `SaleForm`, `PaymentForm`, `MoneyTransactionForm` y `DebtForgivenessForm` incorporan `@DateTimeFormat` para el adaptador MVC. Las operaciones financieras tienen un único adaptador de entrada en producción: `CustomerTransactionController`. La auditoría web reutiliza los mismos datos. Cuando el servicio recibe el formulario, la fecha ya es `LocalDate`; no interpreta texto HTTP. No se encontró otro adaptador de entrada ni un proceso programado que ejecute estas operaciones.

**Implementado.** Los cuatro formularios residen en `web.form`. `TransactionCommandMapper` convierte sus datos después de la validación HTTP a cuatro records de aplicación. Los servicios financieros reciben únicamente esos comandos, sin anotaciones o tipos MVC. La auditoría permanece en el adaptador web y conserva su payload. Las pruebas de rutas comprueban la conversión de los cinco flujos y las pruebas financieras siguen comprobando saldos, estadísticas y transacciones.

## D05 — Resultados de consulta que exponen entidades

**Evidencia inicial.** Búsqueda y detalle de clientes, listados de movimientos y exportaciones siguen utilizando entidades. Los resultados analíticos que ya tienen forma propia usan DTOs: sector, morosidad, top, series mensuales, cumpleaños, ranking y puntuaciones. Los contratos dejaron de exponer las proyecciones del repositorio para sector y top.

**Implementado.** Las cuatro consultas de clientes que exponían entidades devuelven `CustomerDetails`. Las siete consultas de movimientos que exponían entidades devuelven `TransactionDetails`, con una referencia de cliente limitada a identificador, nombre y sector. Son valores inmutables, construidos dentro del servicio de lectura y sin entidades ni colecciones persistentes. Los agregados analíticos conservan sus DTOs. Se adaptaron vistas, exportación, informe y puntuación; esta última recibe `CustomerScoreInput` con identificador y deuda. Pruebas independientes comprueban datos, ausencia, orden y metadata de páginas, además de cambios posteriores en las entidades y relaciones. Los comandos de creación y otras áreas fuera de estas consultas conservan sus contratos actuales.

## D05 — Datos propios del renderizador PDF

**Evidencia inicial.** `generateTransactionsReport` tiene un solo consumidor de producción, el controlador de informes. La selección y filtrado ya se extraen a `CustomerTransactionReportService`, con criterios propios. El renderizador consume nombre, dirección, sector y deuda del cliente, y fecha, tipo, detalle, importe y saldo de los movimientos. El resultado preparado copia la lista, pero comparte sus entidades.

**Implementado.** La preparación captura nombre, dirección, sector y deuda en `ReportCustomerData`, y las columnas del informe en `ReportTransactionData`. El renderizador recibe un único `CustomerTransactionReportData`, sin imports ni referencias de entidades JPA. La copia es independiente también de los elementos y sus relaciones. Las pruebas comprueban cambios posteriores de entidades, selección, orden y contenido real de los seis tipos y el informe vacío. El nombre de descarga y la respuesta HTTP se conservan.

## D05 — Identidad del principal de seguridad

**Evidencia inicial.** `CaseroUserDetails` conserva `AppUser`. Autenticación lee huella, hash, salt, estado y rol; sus consumidores preparan datos de sesión o el actor de auditoría. `AuditEvent` guarda una relación con el usuario persistente. No se encontró un consumidor que necesite otro modelo de identidad ni una escritura de usuario desde estos consumidores del principal.

**Implementado.** La consulta de credenciales captura `UserCredentials` y `UserIdentity` en solo lectura, sin devolver la entidad. El principal conserva identidad inmutable y serializable, username y hash para mantener el contrato existente de `UserDetails`; no conserva salt ni entidad. Los datos de sesión y todos los emisores de auditoría usan identidad. El emisor resuelve la referencia JPA del actor por identificador dentro de la transacción de escritura, manteniendo eventos anónimos, exclusión de administradores y tratamiento de fallos. La identidad refleja el momento de autenticación; otra autenticación consulta de nuevo rol y estado. No se introduce revocación automática de sesiones existentes. Las pruebas comprueban hash, roles, campos de sesión, copia de valores, omisión de credenciales en diagnósticos, serialización y resolución transaccional del actor.

## Estado y alcance de validación

Las **cuatro filas están implementadas**. La tabla contiene **27 de 27 filas completadas, sin pendientes**. La compilación limpia y suite completa aprobó **267 pruebas** con `mvn -o clean test`; no se ejecutó base de datos real ni navegador. No se cambiaron esquema, rutas, reglas de saldo ni política de revocación de sesiones.
