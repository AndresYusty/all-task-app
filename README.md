# All Task App — Backend (TODO Tasks)

Backend de la aplicación **TODO Tasks** para gestionar actividades del día.
Construido con **Spring Boot 4 + Java 21** siguiendo una arquitectura **MVC por capas**.

---

## Arquitectura

```
com.task.all
├── controller    → Exposición de endpoints REST
├── service       → Lógica de negocio (interfaz + impl)
├── repository    → Acceso a datos con Spring Data JPA
├── model
│   ├── entity    → Entidades JPA (Task, ChecklistItem, TaskStatus)
│   └── dto       → Objetos de transferencia (request / response)
├── mapper        → Conversión Entity ↔ DTO
├── exception     → Excepciones y manejador global (@RestControllerAdvice)
├── config        → Configuraciones (CORS)
└── util          → Utilidades (Specifications de búsqueda dinámica)
```

---

## Modelo de dominio

### Task
| Campo          | Tipo            | Notas                                  |
|----------------|-----------------|----------------------------------------|
| id             | Long            | PK autogenerada                        |
| title          | String (150)    | Obligatorio                            |
| description    | String (2000)   | Opcional                               |
| executionDate  | LocalDateTime   | Obligatorio                            |
| status         | TaskStatus      | PROGRAMADO por defecto                 |
| createdAt      | LocalDateTime   | Automático                             |
| updatedAt      | LocalDateTime   | Automático                             |
| items          | List<ChecklistItem> | Cascada + orphanRemoval           |

### ChecklistItem
| Campo       | Tipo         | Notas                  |
|-------------|--------------|------------------------|
| id          | Long         | PK                     |
| description | String (250) | Obligatorio            |
| completed   | boolean      | Marcado/desmarcado     |
| task        | Task         | Relación ManyToOne     |

### Estados permitidos y transiciones
```
PROGRAMADO   → EN_EJECUCION, CANCELADA
EN_EJECUCION → FINALIZADA,   CANCELADA
FINALIZADA   → (terminal)
CANCELADA    → (terminal)
```
Cualquier transición no permitida devuelve **409 Conflict**.

---

## Endpoints principales

Base path: `http://localhost:8081/api`

### Tareas
| Método | Endpoint                       | Descripción                                |
|--------|--------------------------------|--------------------------------------------|
| POST   | `/v1/tasks`                    | Crea una tarea (con ítems opcionales)      |
| PUT    | `/v1/tasks/{id}`               | Edita una tarea completa                   |
| DELETE | `/v1/tasks/{id}`               | Elimina una tarea                          |
| GET    | `/v1/tasks/{id}`               | Consulta una tarea                         |
| GET    | `/v1/tasks`                    | Listado paginado + búsqueda                |
| PATCH  | `/v1/tasks/{id}/status`        | Cambia el estado                           |
| GET    | `/v1/tasks/pending`            | Tareas pendientes (estado PROGRAMADO)      |
| GET    | `/v1/tasks/overdue`            | Tareas programadas cuya fecha ya llegó     |

### Ítems checkeables
| Método | Endpoint                                       | Descripción                   |
|--------|------------------------------------------------|-------------------------------|
| POST   | `/v1/tasks/{taskId}/items`                     | Agrega un ítem                |
| PUT    | `/v1/tasks/{taskId}/items/{itemId}`            | Actualiza un ítem             |
| PATCH  | `/v1/tasks/{taskId}/items/{itemId}/toggle`     | Marca/desmarca como completado|
| DELETE | `/v1/tasks/{taskId}/items/{itemId}`            | Elimina un ítem               |

### Parámetros de búsqueda en `GET /v1/tasks`
- `q`: texto a buscar en título o descripción (case-insensitive)
- `status`: filtro por estado (`PROGRAMADO | EN_EJECUCION | FINALIZADA | CANCELADA`)
- `from`, `to`: rango de fecha de ejecución (ISO-8601, ej. `2026-04-22T00:00:00`)
- `overdueOnly=true`: solo tareas vencidas (PROGRAMADO cuya fecha ya llegó)
- Paginación Spring Data: `page`, `size`, `sort` (ej. `sort=executionDate,asc`)

El campo `overdue` del response indica si la tarea está **pendiente por ejecutar**
(PROGRAMADO + fecha de ejecución vencida). Esto cubre la historia de usuario 9
(alerta de tareas cuya fecha ya llegó) sin necesidad de endpoints adicionales.

---

## Ejemplos de uso

### Crear una tarea
```bash
curl -X POST http://localhost:8081/api/v1/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Preparar informe",
    "description": "Informe mensual de ventas",
    "executionDate": "2026-04-25T08:00:00",
    "items": [
      { "description": "Revisar datos",    "completed": false },
      { "description": "Consolidar cifras","completed": false },
      { "description": "Enviar reporte",   "completed": false }
    ]
  }'
```

### Cambiar estado
```bash
curl -X PATCH http://localhost:8081/api/v1/tasks/1/status \
  -H "Content-Type: application/json" \
  -d '{ "status": "EN_EJECUCION" }'
```

### Buscar con paginación
```bash
curl "http://localhost:8081/api/v1/tasks?q=informe&status=PROGRAMADO&page=0&size=10&sort=executionDate,asc"
```

### Toggle de ítem
```bash
curl -X PATCH http://localhost:8081/api/v1/tasks/1/items/3/toggle \
  -H "Content-Type: application/json" \
  -d '{ "completed": true }'
```

---

## Cómo ejecutar

Hay dos formas: con Docker (más rápido, no requiere MySQL local) o de
forma clásica con Java + MySQL instalados en la máquina.

### Opción A — Docker (recomendada)

Requisitos: **Docker Desktop** (con Docker Compose).

Desde la raíz del proyecto (`all-task-app/`):

```bash
docker compose up --build
```

Esto levanta dos contenedores:

| Servicio | Imagen         | Puerto en host | Notas                                   |
|----------|----------------|----------------|-----------------------------------------|
| mysql    | `mysql:8.4`    | `3307`         | Para no chocar con un MySQL local en 3306 |
| app      | build local    | `8081`         | API Spring Boot                          |

La API queda disponible en `http://localhost:8081/api`.

Para parar todo: `docker compose down`
Para borrar también los datos de MySQL: `docker compose down -v`

Si quieres conectarte con **MySQL Workbench** al contenedor:

- Host: `localhost`
- Puerto: `3307`
- Usuario: `todo_user`
- Password: `todo_pass`
- Schema: `todo_db`

### Opción B — Ejecución local sin Docker

Requisitos:
- Java 21
- Maven Wrapper (incluido: `./mvnw` / `mvnw.cmd`)
- MySQL Server (se recomienda administrarlo con MySQL Workbench)

**1. Crear la base (opcional, se crea sola si el usuario tiene permisos):**

```sql
CREATE DATABASE IF NOT EXISTS todo_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

**2. Ajustar credenciales en `src/main/resources/application.properties`:**

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/todo_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=UTF-8
spring.datasource.username=TU_USUARIO
spring.datasource.password=TU_PASSWORD
```

**3. Arrancar la aplicación:**

```bash
# Windows
mvnw.cmd spring-boot:run

# Linux / Mac
./mvnw spring-boot:run
```

La API queda expuesta en `http://localhost:8081/api`.

Las tablas (`tasks`, `checklist_items`) las crea Hibernate automáticamente
gracias a `spring.jpa.hibernate.ddl-auto=update`.

---

## Probar la API

Dentro de `docs/` hay dos colecciones listas para usar:

- **Postman:** `docs/postman/All-Task-App.postman_collection.json`
  (Importar → la variable `baseUrl` ya está en `http://localhost:8081/api`.
  Al ejecutar *Crear tarea* guarda automáticamente `taskId` e `itemId` en las
  variables de la colección para encadenar las demás requests.)

