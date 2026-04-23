package com.task.all.controller;

import com.task.all.model.dto.ChecklistItemRequestDto;
import com.task.all.model.dto.ChecklistItemResponseDto;
import com.task.all.model.dto.ChecklistItemToggleDto;
import com.task.all.model.dto.PageResponseDto;
import com.task.all.model.dto.TaskRequestDto;
import com.task.all.model.dto.TaskResponseDto;
import com.task.all.model.dto.TaskStatusUpdateDto;
import com.task.all.model.entity.TaskStatus;
import com.task.all.service.TaskService;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

/**
 * API REST para la gestión de tareas (TODO Tasks).
 *
 * Endpoints principales:
 * - CRUD de tareas
 * - Listado paginado y búsqueda por texto, estado y fechas
 * - Cambios de estado
 * - Vistas especiales: pendientes y vencidas
 * - Gestión de ítems checkeables
 */
@RestController
@RequestMapping("/v1/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    // -------------------- Tareas --------------------

    @PostMapping
    public ResponseEntity<TaskResponseDto> create(@Valid @RequestBody TaskRequestDto dto) {
        TaskResponseDto created = taskService.create(dto);
        return ResponseEntity
                .created(URI.create("/v1/tasks/" + created.getId()))
                .body(created);
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<TaskResponseDto> update(@PathVariable Long taskId,
                                                  @Valid @RequestBody TaskRequestDto dto) {
        return ResponseEntity.ok(taskService.update(taskId, dto));
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> delete(@PathVariable Long taskId) {
        taskService.delete(taskId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponseDto> findById(@PathVariable Long taskId) {
        return ResponseEntity.ok(taskService.findById(taskId));
    }

    /**
     * Listado paginado con búsqueda dinámica.
     *
     * Parámetros (todos opcionales):
     * - q: texto a buscar en título o descripción
     * - status: estado exacto (PROGRAMADO, EN_EJECUCION, FINALIZADA, CANCELADA)
     * - from / to: rango de fecha de ejecución (ISO-8601)
     * - overdueOnly: true para devolver solo tareas programadas cuya fecha ya llegó
     * - page, size, sort: paginación y orden (Spring Data)
     */
    @GetMapping
    public ResponseEntity<PageResponseDto<TaskResponseDto>> search(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "status", required = false) TaskStatus status,
            @RequestParam(value = "from", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(value = "to", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(value = "overdueOnly", required = false) Boolean overdueOnly,
            @PageableDefault(size = 10, sort = "executionDate", direction = Sort.Direction.ASC) Pageable pageable) {

        Page<TaskResponseDto> page = taskService.search(q, status, from, to, overdueOnly, pageable);
        return ResponseEntity.ok(PageResponseDto.from(page));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<TaskResponseDto>> findPending() {
        return ResponseEntity.ok(taskService.findPending());
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<TaskResponseDto>> findOverdue() {
        return ResponseEntity.ok(taskService.findOverdue());
    }

    @PatchMapping("/{taskId}/status")
    public ResponseEntity<TaskResponseDto> updateStatus(@PathVariable Long taskId,
                                                        @Valid @RequestBody TaskStatusUpdateDto dto) {
        return ResponseEntity.ok(taskService.updateStatus(taskId, dto.getStatus()));
    }

    // -------------------- Ítems checkeables --------------------

    @PostMapping("/{taskId}/items")
    public ResponseEntity<ChecklistItemResponseDto> addItem(@PathVariable Long taskId,
                                                            @Valid @RequestBody ChecklistItemRequestDto dto) {
        ChecklistItemResponseDto item = taskService.addItem(taskId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }

    @PutMapping("/{taskId}/items/{itemId}")
    public ResponseEntity<ChecklistItemResponseDto> updateItem(@PathVariable Long taskId,
                                                               @PathVariable Long itemId,
                                                               @Valid @RequestBody ChecklistItemRequestDto dto) {
        return ResponseEntity.ok(taskService.updateItem(taskId, itemId, dto));
    }

    @PatchMapping("/{taskId}/items/{itemId}/toggle")
    public ResponseEntity<ChecklistItemResponseDto> toggleItem(@PathVariable Long taskId,
                                                               @PathVariable Long itemId,
                                                               @Valid @RequestBody ChecklistItemToggleDto dto) {
        return ResponseEntity.ok(taskService.toggleItem(taskId, itemId, dto.getCompleted()));
    }

    @DeleteMapping("/{taskId}/items/{itemId}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long taskId, @PathVariable Long itemId) {
        taskService.deleteItem(taskId, itemId);
        return ResponseEntity.noContent().build();
    }
}
