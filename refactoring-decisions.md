# Decisiones sobre mejoras condicionales

Evaluación de D04 y las tres filas D05 de [improvements.md](improvements.md), aplicando la skill `prezdev`: separar responsabilidades cuando aporta valor y evitar abstracciones motivadas solo por consumidores hipotéticos. Las cuatro decisiones están **evaluadas y diferidas**, sin un cambio de código que elimine estos acoplamientos.

## D04 — Formularios financieros y comandos de aplicación

**Evidencia.** `SaleForm`, `PaymentForm`, `MoneyTransactionForm` y `DebtForgivenessForm` incorporan `@DateTimeFormat` para el adaptador MVC. Las operaciones financieras tienen un único adaptador de entrada en producción: `CustomerTransactionController`. La auditoría web reutiliza los mismos datos. Cuando el servicio recibe el formulario, la fecha ya es `LocalDate`; no interpreta texto HTTP. No se encontró otro adaptador de entrada ni un proceso programado que ejecute estas operaciones.

**Decisión actual.** Conservar los formularios compartidos. El acoplamiento a las anotaciones de MVC existe, pero añadir cuatro comandos equivalentes y sus mapeos no resuelve actualmente una necesidad independiente. No se declara que los contratos financieros estén libres de dependencias de presentación.

**Cuándo retomarlo.** Una API con otra representación, importación o tarea programada necesita ejecutar estos comandos; o formulario y entrada de negocio requieren campos o validaciones distintos. Entonces definir comandos sin anotaciones MVC y convertirlos en el adaptador, manteniendo saldo, importes, fecha, cantidad de artículos y comportamiento transaccional.

## D05 — Resultados de consulta que exponen entidades

**Evidencia.** Búsqueda y detalle de clientes, listados de movimientos y exportaciones siguen utilizando entidades. Los resultados analíticos que ya tienen forma propia usan DTOs: sector, morosidad, top, series mensuales, cumpleaños, ranking y puntuaciones. Los contratos dejaron de exponer las proyecciones del repositorio para sector y top.

**Decisión actual.** Mantener la conversión selectiva realizada. No reemplazar todos los resultados por copias de entidades con idénticos campos. Los consumidores actuales todavía conocen entidades mutables; una transacción de lectura o una copia de la lista no elimina ese acoplamiento.

**Cuándo retomarlo.** Un consumidor necesita un conjunto estrecho de datos, aislamiento de JPA, una salida pública estable o valores inmutables. Crear el resultado para ese caso y convertirlo dentro del servicio correspondiente. Verificar ausencia, filtros de habilitación, orden, paginación y los datos realmente utilizados por cada consumidor antes de retirar el contrato anterior.

## D05 — Datos propios del renderizador PDF

**Evidencia.** `generateTransactionsReport` tiene un solo consumidor de producción, el controlador de informes. La selección y filtrado ya se extraen a `CustomerTransactionReportService`, con criterios propios. El renderizador consume nombre, dirección, sector y deuda del cliente, y fecha, tipo, detalle, importe y saldo de los movimientos. El resultado preparado copia la lista, pero comparte sus entidades.

**Decisión actual.** Conservar esa frontera y la composición especializada del PDF. El renderizador sigue dependiendo de la forma de las entidades; no se afirma que el modelo del informe sea una instantánea profunda. No se encontró un segundo consumidor o un requisito de reproducción independiente que justifique otra representación en esta tanda.

**Cuándo retomarlo.** Archivado de informes reproducibles, generación en segundo plano, cierre de sesión de persistencia antes del renderizado o un segundo formato requieren datos autónomos. Preparar entonces una instantánea del cliente y filas del informe antes de renderizar; comprobar períodos, selección por tipo, orden, etiquetas, signos de importes, datos de sector y contenido del PDF.

## D05 — Identidad del principal de seguridad

**Evidencia.** `CaseroUserDetails` conserva `AppUser`. Autenticación lee huella, hash, salt, estado y rol; sus consumidores preparan datos de sesión o el actor de auditoría. `AuditEvent` guarda una relación con el usuario persistente. No se encontró un consumidor que necesite otro modelo de identidad ni una escritura de usuario desde estos consumidores del principal.

**Decisión actual.** Conservar la representación actual. La separación realizada en I03 reduce las capacidades recibidas por autenticación, pero no elimina la exposición de la entidad. Una identidad independiente exigiría resolver cómo obtener la referencia persistente para auditoría y cuándo refrescar rol y estado de la sesión; no se cambia esa política implícitamente.

**Cuándo retomarlo.** Una integración de identidad, un requisito de sesión inmutable o una política explícita de revocación y actualización de permisos exige autonomía respecto de la entidad. Definir los datos mínimos, la resolución del actor y el ciclo de actualización; comprobar PIN, roles, usuarios deshabilitados, sesiones, auditoría anónima y exclusión de administradores.

## Estado y alcance de validación

Se revisaron las **15 filas que quedaban al comenzar la tanda**: diez se resolvieron mediante cambios de código, una mediante [documentación contractual](service-contracts.md) y cuatro mediante estas decisiones condicionales. El total de la tabla queda en **23 filas completadas y 4 evaluadas y diferidas**, de 27 originales. Estas cuatro no cuentan como refactorizaciones implementadas.

La última ejecución de `mvn -o test` aprobó **252 pruebas**. Las decisiones se apoyan en inspección de código, referencias de producción y contratos actuales; no en una ejecución con base de datos real o navegador. Sus condiciones pueden cambiar al aparecer nuevos consumidores o requisitos.
