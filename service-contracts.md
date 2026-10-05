# Contratos de servicios

Documento de L03 en [improvements.md](improvements.md). Describe el comportamiento actual que deben conservar implementaciones alternativas. No se ha demostrado una ruptura de LSP; estas garantías permiten revisar una sustitución antes de introducirla.

## Convenciones y límites

- Los importes representan pesos enteros, sin conversión de moneda ni redondeo decimal. Los tipos existentes son `int`/`Integer` y `long` para algunos agregados; devolver `long` no cambia el tipo de la suma realizada por el repositorio.
- Las fechas financieras son `LocalDate`. Los rangos con `BETWEEN` incluyen ambos extremos. Los meses son meses calendario, no bloques de treinta días.
- Los servicios reciben entradas ya validadas por el adaptador MVC. Las anotaciones de los formularios no garantizan validación automática al invocar directamente un servicio. Identificadores, fechas y paginación deben ser válidos salvo los casos opcionales descritos abajo.
- Las consultas no modifican el dominio. Las implementaciones de `CustomerQueries`, `TransactionQueries`, `AuditQueries` y `UserCredentialLookup` usan transacciones de solo lectura a través del proxy de Spring. Esto no convierte sus entidades devueltas en objetos inmutables.
- Los comandos de clientes y movimientos usan transacciones de escritura a través del proxy. La anotación no ofrece la misma garantía al construir una instancia directamente ni demuestra protección frente a escrituras concurrentes.
- No hay un criterio universal de desempate. Se documentan los órdenes explícitos; los empates restantes conservan las limitaciones de la consulta existente.

## CustomerQueries

| Operación | Ausencia, selección y orden |
| --- | --- |
| `search(filter, pageable)` | Filtro nulo o en blanco: página vacía con la paginación recibida. En otro caso aplica `trim` y busca clientes habilitados por nombre, dirección o sector, ignorando mayúsculas y los caracteres acentuados contemplados por la consulta. Orden principal por nombre ascendente. |
| `get(id)` | Devuelve un cliente habilitado con sector precargado. Ausente o deshabilitado: `CustomerNotFoundException`. La traducción a HTTP 404 corresponde al adaptador web. |
| `getTopDebtors(pageable)` | Clientes habilitados, deuda descendente. |
| `getBestCustomers(pageable)` | Clientes habilitados, deuda ascendente. |
| `getOverdueCustomers(pageable, months)` | Meses menores que uno se normalizan a uno. Contenido de clientes habilitados con deuda positiva, sin abonos o con último abono anterior al umbral. El umbral usa `CURRENT_DATE` de la base de datos. Orden: clientes con abonos antes que los que nunca abonaron; dentro del primer grupo, último abono descendente. |
| `getOverdueDebt(months)` | Misma normalización de meses; suma de deuda de clientes habilitados con deuda positiva y criterio de último abono. Devuelve cero cuando no hay deuda seleccionada. |
| `count()` | Cantidad de clientes habilitados. |
| `getCustomersCountBySector(pageable)` | Agrupa clientes habilitados por nombre de sector, en orden ascendente del nombre. Devuelve `SectorCustomerCount`, con total `long`. La conversión conserva contenido y metadata de la página recibida. |
| `getBirthdaysThisMonthCount(month)` | Cuenta clientes habilitados del mes indicado con día de nacimiento informado. El año de nacimiento puede faltar. |
| `getBirthdaysThisMonth(month)` | Mismo criterio; orden por día ascendente. Devuelve datos sin formato localizado, con deuda y fecha del último movimiento `PAYMENT`, si existe. |

El servicio no corrige páginas fuera de rango ni valida meses del calendario. `OverdueCustomerSummary` conserva el último pago como texto: `YYYY-MM-DD` o `Nunca ha abonado`; los meses de atraso pueden ser nulos.

**Limitación existente del total de morosidad:** el `countQuery` de `findOverdueCustomers` no repite el filtro `enabled = true` del contenido. Cuando se ejecuta ese conteo, la metadata puede incluir clientes deshabilitados. El servicio conserva la metadata del repositorio; no se promete igualdad entre ese total y un conteo independiente de habilitados. Corregir esta diferencia requiere un cambio de comportamiento y una comprobación con datos reales, separado de esta documentación contractual.

## CustomerCommands

Todas las operaciones de actualización y baja buscan un cliente habilitado; ausencia o baja previa producen `CustomerNotFoundException`.

| Operación | Efecto |
| --- | --- |
| `create(form)` | Nombre y dirección con `trim`; sector obtenido mediante `SectorService`; deuda inicial cero y cliente habilitado. Devuelve el resultado de persistencia. |
| `delete(id)` | Baja lógica mediante `enabled = false`; conserva el registro y sus movimientos. |
| `updateName(id, name)` | Guarda el nombre con `trim`. |
| `updateAddress(id, address)` | Guarda la dirección recibida sin normalizarla. Esta diferencia respecto de la creación se conserva. |
| `updateSector(id, sectorId)` | Resuelve el sector y actualiza su referencia. |
| `updateBirthdate(id, day, month, year)` | Guarda los tres valores recibidos; admite nulos para retirar datos del cumpleaños. |

Una alternativa debe conservar la baja lógica, las diferencias de normalización y la propagación de fallos al resolver el sector. Estos comandos no emiten por sí mismos la auditoría HTTP.

## TransactionCommands

Los registros financieros requieren un cliente habilitado. La secuencia común actualiza su deuda, guarda el cliente, guarda el movimiento y guarda la estadística, dentro de una misma transacción de escritura.

| Operación | Saldo y registro |
| --- | --- |
| `registerSale` | Suma el importe al saldo previo. Venta `NEW_SALE` si el saldo previo era cero; `MAINTENANCE` en otro caso. Conserva fecha, detalle y cantidad de artículos recibidos. |
| `registerPayment` | Saldo `max(0, deuda - importe)`. Guarda el importe completo solicitado, incluso si supera la deuda; detalle `[Abono]: $importe`. |
| `registerRefund` / `registerFaultDiscount` | Misma reducción de saldo con mínimo cero; conserva importe completo, fecha y detalle del formulario, con el tipo correspondiente. |
| `forgiveDebt` | Si la deuda es menor o igual a cero, no escribe. En otro caso registra el importe de la deuda previa y deja saldo cero. |
| `delete(transactionId)` | Ausencia: `NoSuchElementException`. Elimina estadísticas coincidentes por tipo, importe y fecha; elimina el movimiento; restaura la deuda al saldo del último movimiento visible por `createdAt DESC, id DESC`, o cero si no queda ninguno. |

`date` proviene del formulario. `createdAt` se obtiene con `OffsetDateTime.now` en `America/Santiago`. Borrar un movimiento conserva la regla de restaurar el último saldo guardado; no recalcula todos los movimientos históricos.

La atomicidad exigida a una alternativa incluye saldo, movimiento y estadística. Las pruebas actuales comprueban el uso de la transacción y el orden de llamadas; no demuestran rollback de una base de datos real ni ausencia de actualizaciones perdidas bajo concurrencia.

## TransactionQueries

Visible significa que el cliente del movimiento está habilitado. Las consultas que devuelven entidades precargan cliente y sector.

| Operación | Resultado y períodos |
| --- | --- |
| `listAll(type, pageable)` | Tipo nulo incluye todas las categorías; otro valor selecciona ese tipo. Conserva la paginación solicitada. |
| `listByCustomer(customerId, pageable)` | Página de movimientos visibles del cliente. No consulta antes su existencia; puede devolver una página vacía. |
| `listAllByCustomer(customerId)` | Lista visible en orden `date DESC, id DESC`. |
| `listRecentByCustomer(customerId, limit)` | Conserva el mismo orden. Límite menor o igual a cero devuelve todos; límite positivo devuelve los primeros elementos disponibles. |
| `getMonthlySummary(start, end)` | Fin nulo: hoy según `reportClock`. Inicio nulo: fin menos cinco meses. Invierte extremos si están intercambiados y expande a meses completos. Devuelve todos los meses en orden ascendente, con ventas y abonos cero cuando faltan datos. Solo suma `SALE` y `PAYMENT`. |
| `getFinishedCardsThisMonth()` | Mes completo del reloj inyectado; movimientos visibles con saldo cero, orden por fecha descendente. No equivale a contar clientes distintos. |
| `getSalesThisMonth()` | Ventas visibles del mes completo del reloj; fecha descendente. |
| `getTopCustomersThisMonth()` | Mes completo del reloj; hasta tres clientes por importe abonado descendente. Devuelve `TopCustomerSummary` y conserva el orden de la proyección. |
| `getSalesSum(start, end)` / `getPaymentsSum(start, end)` | Suma del tipo indicado entre extremos inclusivos; agregado nulo se transforma en cero. Estos métodos no intercambian ni expanden los extremos. |

`reportClock` se configura en `ReportConfiguration` con la zona predeterminada del sistema. Una sustitución debe permitir mantener esa referencia temporal. No se garantiza un desempate adicional entre movimientos de la misma fecha en las consultas mensuales ni entre importes iguales en el top.

## Puntuación y ranking

### CustomerScoreService

- Colección nula o vacía: mapa vacío. Clientes sin identificador no generan resultados. Los elementos de una colección no deben ser nulos: el primer recorrido obtiene sus identificadores.
- Consulta ciclos una vez por lote de identificadores y transmite la ventana de pago perfecto del calculador: actualmente 45 días. La SQL clasifica como tardíos los intervalos estrictamente mayores que esa ventana.
- Conserva el orden de ciclos producido por la consulta, los numera desde uno y evalúa cada uno con `CustomerScoreCalculator`.
- Sin ciclos: puntuación mínima actual, 1, y lista vacía. Con ciclos: promedio aritmético de sus puntuaciones, redondeado a dos decimales.
- `calculateScores` conserva los valores del resumen. `calculateScore(null)` devuelve el mínimo.
- Los resúmenes protegen sus listas de ciclos frente a modificación. La fórmula y su interpretación continúan siendo responsabilidad del calculador y del narrador existentes.

### CustomerRankingService

- Obtiene clientes habilitados y calcula sus resúmenes por lote.
- Paginación nula: página cero de veinte elementos. Requiere paginación efectiva; no se documenta soporte para `Pageable.unpaged()`.
- Orden primario por puntuación, ascendente o descendente según la solicitud. Desempate por número de ciclos descendente y nombre ascendente sin distinguir mayúsculas; nombre nulo se compara como cadena vacía.
- Empates completos mantienen el orden de entrada. El repositorio no impone un orden universal para ese caso.
- Página fuera del contenido disponible: devuelve la última página, conservando tamaño y metadata de orden. Sin clientes devuelve una página vacía con la paginación solicitada y sin calcular ciclos.
- El `Sort` de la página se conserva como metadata; el orden efectivo del ranking sigue las reglas anteriores.

### CustomerScorePresentationService

- Cliente nulo: mínimo, explicación vacía y ciclos vacíos.
- Cliente presente sin historial, o resumen nulo pasado a `present`: mínimo y explicación existente de falta de historial.
- Conserva la puntuación y el orden de ciclos del resumen; prepara el texto mediante el narrador. Mostrar ciclos en orden inverso en una vista no modifica el resumen original.

## StatisticsService

`getMonthlyStatistic(month, year)` obtiene el primer y último día del mes y delega en la consulta por rango inclusivo. La variante por fechas recibe extremos válidos; no los normaliza como `getMonthlySummary`.

- Tarjetas terminadas: cuenta movimientos de clientes habilitados con saldo cero, dentro del rango.
- Ventas nuevas y mantenimiento: cuenta registros estadísticos por tipo de venta. Artículos, ventas y abonos se agregan desde estadísticas.
- `salesCount` y `paymentsCount` contienen **importes**, pese al sufijo `Count`; los otros conteos conservan sus unidades propias. Sumas nulas se convierten en cero.
- Deuda total y promedio: clientes habilitados; nulo se transforma en cero. El promedio se convierte a entero mediante `intValue`, sin redondear al entero más cercano.
- `getCustomersCount()` cuenta todos los clientes persistidos, incluidos los deshabilitados. No es equivalente a `CustomerQueries.count()`.

Estos agregados proceden de distintas consultas. No se promete una instantánea global consistente entre todos ellos.

## Dashboard y cumpleaños

`DashboardService.prepare(referenceDate)` requiere fecha explícita. Usa su mes y año para cumpleaños, estadísticas y seis meses de series. La comparación del mes anterior va desde su primer día hasta `referenceDate.minusMonths(1)`, con el ajuste de calendario de `LocalDate`, por ejemplo 31 de marzo → último día de febrero.

El top usa el reloj propio de `TransactionQueries`; la morosidad usa la fecha de la base. Pasar otra fecha al dashboard no sustituye esas referencias. Sus listas de resultados se copian y no se modifican al preparar la vista.

La localización pertenece a los presentadores. El gráfico recibe listas estructuradas serializadas por Thymeleaf. El presentador de cumpleaños devuelve etiqueta vacía si falta día o mes y edad que se cumple **durante el año**, si se conoce el año de nacimiento. El formato del cliente muestra la edad **actual**, con referencia explícita, y devuelve nulo ante fecha incompleta. Ambas reglas son distintas y se conservan.

## Preparación del informe

`CustomerTransactionReportService.prepare(customerId, criteria)` obtiene un cliente habilitado y sus movimientos visibles ordenados por fecha e identificador descendentes. `criteria` debe existir y su rango no puede ser nulo.

- Rango `ALL`: conserva todos los movimientos; ignora la cantidad de meses.
- Rango `MONTHS`: meses nulos o menores que uno usan doce; valores mayores se limitan a sesenta. El corte inclusivo es el primer día del mes actual menos `months - 1` meses, según el reloj inyectado.
- El filtro mensual elimina fechas nulas y anteriores al corte. No introduce un extremo superior: conserva fechas futuras si cumplen el corte.
- Tipo nulo incluye todas las categorías; otro tipo filtra por igualdad, sin alterar el orden.
- El resultado copia la colección de movimientos, pero comparte las entidades. No representa una instantánea profunda e inmutable del cliente o sus relaciones.

El renderizador conserva la responsabilidad de componer el PDF y sus etiquetas. La entrega HTTP y la traducción de la ausencia del cliente pertenecen al controlador y al manejador web.

## Auditoría

### AuditPolicy

`ConfiguredAuditPolicy` consulta `audit.logging.enabled`. Ausencia o valor nulo habilitan auditoría. Tras quitar espacios exteriores y pasar a minúsculas, `true`, `1`, `yes`, `on` habilitan; `false`, `0`, `no`, `off` deshabilitan. Conserva la conversión a minúsculas con el locale predeterminado.

Un valor que comienza con `{` se interpreta como JSON ya normalizado: `enabled` booleano gobierna la política. Valor desconocido, JSON inválido, atributo ausente o de otro tipo habilitan auditoría. No se usa este contrato para administrar configuración.

### AuditEventService

`logEvent(eventType, payload, context)` aplica la política antes de registrar:

1. Política deshabilitada o tipo nulo: retorna sin guardar.
2. Contexto con administrador: retorna sin guardar.
3. En los demás casos guarda tipo, usuario opcional, IP, agente y una copia superficial del payload. Payload nulo se transforma en mapa vacío.

El contexto es obligatorio cuando el flujo alcanza su lectura; un usuario nulo representa una acción anónima. El servicio no recibe objetos Servlet. Si `X-Forwarded-For` contiene texto, la capa web toma el primer segmento separado por coma y aplica `trim`; en otro caso usa la dirección remota. No busca el primer segmento no vacío. Conserva `User-Agent`; una solicitud nula produce IP y agente nulos.

Las excepciones dentro de `persistEvent`, incluida la llamada a `save`, se registran como advertencia y no se propagan desde ese bloque. No se promete absorber errores de lectura de política, contexto inválido ni fallos de commit que sucedan al salir del proxy transaccional. Capturar un error de `save` tampoco prueba que la transacción pueda confirmarse después de un fallo real de persistencia.

`AuditAction` conserva identificadores persistidos, omisión de nulos y dos convenciones: acciones envueltas en `type`/`data`, y cambios de nombre/cumpleaños planos con `action`. Sus opciones filtrables excluyen las acciones planas y conservan alias históricos.

### AuditQueries

`search(criteria)` requiere criterios y paginación válidos. Tipo de evento y tipo de payload son opcionales. Selecciona una de cuatro consultas: ambos filtros, solo payload, solo evento o ninguno. El filtro de payload compara la clave `type`, no `action`; el usuario se precarga.

El servicio no normaliza cadenas ni limita páginas. El adaptador HTTP normaliza página a mínimo cero, tamaño entre uno y cien, orden descendente por `createdAt`, payload sin espacios exteriores y tipo de evento con coincidencia exacta. Un tipo de evento inválido se ignora; un payload desconocido se conserva como filtro válido para datos históricos. Las búsquedas sin payload tienen orden descendente explícito; las de payload reciben el orden mediante la paginación.

## Credenciales y administración de usuarios

`UserCredentialLookup.findByPinFingerprint(fingerprint)` consulta en solo lectura y devuelve `Optional<AppUser>`; ausencia es `Optional.empty()`. No administra usuarios ni valida por sí solo el PIN.

El proveedor de autenticación obtiene la huella, consulta ese contrato y verifica el hash con su salt. Usuario ausente o PIN incorrecto producen `BadCredentialsException`; usuario deshabilitado produce `DisabledException`. Una autenticación correcta conserva el usuario y sus autoridades, elimina las credenciales del token resultante y copia los detalles de solicitud. Un token de otro proveedor devuelve nulo sin consultar credenciales.

`AppUserService` conserva listado, creación y actualización de PIN. La separación no cambia validación de PIN, detección de duplicados, generación de salt ni asignación de roles. El principal sigue compartiendo la entidad; su posible sustitución se evalúa en [refactoring-decisions.md](refactoring-decisions.md).

## Evidencia y uso al sustituir implementaciones

La suite actual pasó con **252 pruebas, cero fallos, errores u omisiones**, mediante `mvn -o test`. Casos relevantes:

| Contrato | Pruebas existentes |
| --- | --- |
| Clientes y movimientos | `CustomerServicesTest`, `TransactionCommandServiceTest`, `TransactionQueryServiceTest` |
| Evaluación, ranking y umbral | `CustomerScoreServicesTest`, `CustomerRankingServiceTest`, `CustomerScoreWindowTest` |
| Dashboard, cumpleaños y presentación | `DashboardServiceTest`, `DashboardTopCustomersTest`, `CustomerBirthdayPresenterTest`, `CustomerBirthDateFormatterTest`, `CustomerBirthDateViewsTest`, `TransactionPresentationViewsTest` |
| Preparación del PDF y rutas | `CustomerTransactionReportServiceTest`, `CustomerRoutesTest` |
| Auditoría | `ConfiguredAuditPolicyTest`, `AuditEventServiceImplTest`, `AuditQueryServiceTest`, `AuditActionTest`, `AuditEmittersTest`, `AuditContextFactoryTest` |
| Credenciales | `UserCredentialLookupServiceTest` |

Hay repositorios simulados, pruebas del hash real, verificaciones transaccionales mediante proxies y renderizado real de Thymeleaf. No se ejecutó SQL nativa contra PostgreSQL, rollback real, navegador ni carga concurrente. La parametrización de la ventana en SQL está verificada en las llamadas al repositorio y por inspección del código.

Para introducir una alternativa, ejecutar los mismos escenarios de comportamiento contra ella y comprobar también sus consultas, paginación y transacciones con el almacenamiento elegido. Conservar los resultados de ausencia, unidades, orden y referencias temporales documentados. Si se quiere cambiar una regla existente, explicitar ese cambio y sus consumidores; no presentarlo como una sustitución equivalente.
