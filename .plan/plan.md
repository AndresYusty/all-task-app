
## Stack

- Java 21 + Spring Boot 4.0.5.
- Spring Web + Spring Data JPA + Bean Validation.
- MySQL 8 (lo manejo con MySQL Workbench).
- Maven Wrapper para no depender del Maven instalado en la máquina.

No meto librerías "mágicas" (tipo Lombok o MapStruct) para que el código sea
fácil de leer sin tener que configurar nada extra en el IDE del evaluador.

---

## Arquitectura

El enunciado traía un diagrama de paquetes y lo respeto tal cual:

```
com.task.all
├── controller    → endpoints REST
├── service       → lógica de negocio (interfaz + impl)
├── repository    → acceso a datos con Spring Data JPA
├── model
│   ├── entity    → entidades JPA
│   └── dto       → objetos de transferencia
├── mapper        → entity ↔ dto
├── exception     → excepciones + manejador global
├── config        → CORS
└── util          → Specifications para búsqueda dinámica
```

La idea es que cada capa tenga una sola responsabilidad y no se salten capas
(el controller no habla con el repositorio, etc.).

---

## Cómo voy a atacarlo

1. **Dependencias y configuración base.**
   `pom.xml` con los starters, `application.properties` con MySQL, puerto y
   `context-path = /api`.

2. **Dominio primero.**
   - `TaskStatus` como enum (PROGRAMADO, EN_EJECUCION, FINALIZADA, CANCELADA).
   - `Task` con título, descripción, fecha de ejecución, estado, timestamps
     automáticos y relación `@OneToMany` con `ChecklistItem` (cascade +
     orphanRemoval para no tener que manejar los ítems por separado al borrar
     una tarea).
   - `ChecklistItem` con descripción y flag `completed`.

3. **DTOs separados de las entidades.**
   No quiero exponer proxies de Hibernate ni amarrar el contrato REST al modelo
   de BD. Tengo DTOs de request, de response, uno para cambiar solo el estado
   y uno para el toggle de ítems. Además un `PageResponseDto<T>` para que la
   paginación no dependa de cómo serialice Spring internamente `Page`.

4. **Mapper.**
   Una sola clase `TaskMapper` con métodos `toEntity` / `toResponseDto`.
   Aquí también calculo el flag `overdue` (tarea PROGRAMADO con fecha ya
   vencida) — así no necesito cron ni scheduler; la "alerta" es un dato
   calculado cada vez que se consulta.

5. **Repositorios.**
   - `TaskRepository` extiende `JpaRepository` + `JpaSpecificationExecutor`.
   - `ChecklistItemRepository` como `JpaRepository` simple.
   - `TaskSpecifications` con predicados componibles (texto libre, estado,
     rango de fechas, vencidas). Así un solo `GET /v1/tasks` cubre búsqueda,
     filtro y paginación.

6. **Servicio (interfaz + impl).**
   Separo contrato e implementación. En `TaskServiceImpl` meto:
   - CRUD.
   - Búsqueda paginada armando las Specifications.
   - **Máquina de estados** explícita (un `Map<TaskStatus, Set<TaskStatus>>`
     con las transiciones permitidas). Si alguien intenta saltarse el flujo
     (por ejemplo FINALIZADA → PROGRAMADO) lanzo excepción y devuelvo 409.
   - Sincronización de ítems en el `update`: los que vienen con id se
     actualizan, los que no traen id se crean, y los que ya no vengan en el
     request se eliminan. Así un solo PUT cubre la historia de "editar tarea
     con sus ítems".

7. **Controller.**
   `TaskController` en `/v1/tasks`. Endpoints:
   - CRUD normal de tareas.
   - `GET /v1/tasks` con `q`, `status`, `from`, `to`, `overdueOnly` + paginación.
   - `PATCH /{id}/status` para cambiar estado.
   - `GET /pending` y `GET /overdue` como accesos directos a los dos casos
     más útiles para el usuario final.
   - Subrecurso `/items` para la gestión fina de ítems checkeables.

8. **Manejo de errores.**
   Un `@RestControllerAdvice` devolviendo siempre el mismo formato
   (`ApiErrorDto`): 404 para recursos que no existen, 409 para transiciones
   inválidas, 400 para validaciones de `@Valid` y parseo, 500 controlado al
   final. Nada de stack traces en el JSON.

9. **CORS.**
   Abierto para `http://localhost:4200` para que cuando conecte el Angular no
   haya sorpresas.

10. **Documentación y pruebas manuales.**
    README con alcance, cómo arrancar MySQL y la app, y cómo probar.
    Colección de Postman en `docs/postman/` y un `.http` en `docs/` como
    alternativa para quien no use Postman.

---

## Mapeo historia ↔ cómo la resuelvo

| HU | Qué pide                               | Cómo lo cumplo                                                    |
|----|----------------------------------------|-------------------------------------------------------------------|
| 1  | Crear tarea con ítems                  | `POST /v1/tasks` acepta los ítems en el mismo cuerpo              |
| 2  | Editar tarea (incluidos ítems)         | `PUT /v1/tasks/{id}` con la sincronización de ítems              |
| 3  | Eliminar                               | `DELETE /v1/tasks/{id}`                                          |
| 4  | Gestionar estado                       | `PATCH /v1/tasks/{id}/status` + tabla de transiciones válidas    |
| 5  | Ítems checkeables                      | Endpoints bajo `/v1/tasks/{id}/items` + toggle dedicado          |
| 6  | Listar paginado                        | `GET /v1/tasks?page=&size=&sort=`                                |
| 7  | Buscar                                 | Los mismos parámetros del listado: `q`, `status`, `from`, `to`   |
| 8  | Ver pendientes por ejecutar            | `GET /v1/tasks/pending` o `?status=PROGRAMADO`                   |
| 9  | Alertar tareas cuya fecha ya llegó     | Campo `overdue` en el response + `GET /v1/tasks/overdue`         |

---

## Decisiones que quiero dejar documentadas

- **Calcular `overdue` en tiempo de consulta en vez de persistirlo.**
  Evita tener un cron que actualice el estado de las tareas. El estado real
  sigue siendo PROGRAMADO (porque nadie la inició); lo único que agrego es
  una señal para que la UI la pinte como "pendiente por iniciar".

- **Máquina de estados en código y no solo a nivel de UI.**
  Aunque el front valide, cualquier cliente podría mandar un PATCH saltándose
  el flujo. Validarlo en el backend evita datos inconsistentes.

- **Especificaciones (Specifications) en vez de query methods.**
  Con tantos filtros opcionales combinables (texto, estado, rango de fecha,
  vencidas), hacer un `findByTitleContainingAndStatusAnd...` se vuelve
  inmanejable. Las Specifications dejan el código del servicio más limpio.

- **DTOs separados de entidades JPA.**
  No expongo la entidad en el JSON. Así puedo cambiar columnas de BD sin
  romper el contrato REST, y tampoco serializo proxies ni colecciones
  lazy que revienten fuera de la transacción.

- **`PageResponseDto<T>` propio.**
  La serialización por defecto de `Page` de Spring Data cambia entre versiones
  y trae campos innecesarios. Con un DTO propio el front siempre recibe la
  misma forma (`content`, `page`, `size`, `totalElements`, `totalPages`,
  `first`, `last`).

---

## Cómo verifico que todo funciona

1. Levantar MySQL (con Workbench o servicio local).
2. `mvnw.cmd spring-boot:run` y revisar que Hibernate cree `tasks` y
   `checklist_items` solo.
3. Importar la colección de Postman de `docs/postman/`.
4. Correr el flujo feliz: crear → consultar → cambiar a EN_EJECUCION →
   marcar ítems → cambiar a FINALIZADA.
5. Correr los casos de error: pedir una tarea inexistente (404), crear sin
   título (400), intentar volver de FINALIZADA a PROGRAMADO (409).
6. Dejar una tarea con fecha pasada en estado PROGRAMADO y confirmar que
   aparece en `GET /v1/tasks/overdue` y con `overdue: true` en el listado
   general.

---

## Cómo trabajé con la IA

Como la prueba permitía usar IA, la usé como compañera de trabajo, no como
autocompletar glorificado. Básicamente hice lo que haría con otro dev
pair-programming: darle el contexto completo, discutir el enfoque antes de
tocar código, y revisar cada cambio.

Lo primero que le pasé no fue "hazme un CRUD de tareas". Fue el enunciado
entero de la prueba junto con la imagen del diagrama de paquetes, insistiendo
en que respetara los nombres del diagrama. Eso fue importante porque si no
se lo aclaras termina inventando un paquete `services` o `models` que no
estaba en la estructura pedida.

A partir de ahí iba por bloques: dominio, luego repositorios y Specifications,
luego servicio, luego controller, y al final documentación y colección. En
cada bloque prefería revisar la propuesta antes de dejar que escribiera código
para poder ajustar cosas a tiempo. Por ejemplo, la forma de resolver la
historia 9 (alertar tareas vencidas) la hablamos un par de veces antes de
decidir que fuera un flag calculado en vez de un campo persistido.

Cuando algo fallaba, funcionaba mejor pegarle el error tal cual que intentar
describirlo. Así salieron los arreglos del cambio a MySQL, el problema con
`Specification.unrestricted()` que en Spring Data JPA 4 devuelve
`Specification<Object>` y rompía el tipado (se resolvió usando
`Specification.allOf(...)` con una lista tipada), la propiedad de Jackson
que ya no existe en la versión 3, y un par de warnings menores de Hibernate.

La colección de Postman y el `.http` también salieron con su ayuda. Ahí sí
le fui bastante específico: que los scripts guardaran el `taskId` y el
`itemId` automáticamente entre requests, que hubiera una carpeta aparte para
los casos de error, y que el `baseUrl` quedara configurado como variable.
Era trabajo repetitivo que tenía sentido delegar.

Lo que **no** delegué fueron las decisiones de diseño. El stack, modelar
`overdue` como calculado en vez de persistido, poner la máquina de estados
en el backend y no solo en la UI, no meter Lombok ni MapStruct para que el
evaluador pueda abrir el proyecto sin configurar nada, usar un
`PageResponseDto` propio en vez de la serialización interna de `Page`, y
dónde cortar el alcance (por ejemplo, no hacer tests automáticos porque no
era parte de los criterios funcionales) — todo eso lo fui definiendo yo y
la IA solo implementaba.

El resultado final queda documentado en tres niveles: este plan con el
razonamiento, `prompts.md` con los prompts concretos en orden cronológico
para que se pueda reconstruir la conversación, y el `README.md` con las
instrucciones operativas.

---

## Pendientes / posibles mejoras (fuera de alcance de la prueba)

- Autenticación y usuarios (ahora la API es anónima).
- Migraciones con Flyway o Liquibase en vez de `ddl-auto=update`.
- Tests de integración con `@SpringBootTest` y Testcontainers.
- Paginación también en el listado de ítems si una tarea llegara a tener
  muchísimos.
- OpenAPI / Swagger UI para no depender solo de Postman.
