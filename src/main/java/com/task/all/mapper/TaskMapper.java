package com.task.all.mapper;

import com.task.all.model.dto.ChecklistItemRequestDto;
import com.task.all.model.dto.ChecklistItemResponseDto;
import com.task.all.model.dto.TaskRequestDto;
import com.task.all.model.dto.TaskResponseDto;
import com.task.all.model.entity.ChecklistItem;
import com.task.all.model.entity.Task;
import com.task.all.model.entity.TaskStatus;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Convierte entre entidades JPA y DTOs para aislar la capa de persistencia
 * de la capa de exposición (REST).
 */
@Component
public class TaskMapper {

    /**
     * Convierte una entidad Task a su DTO de respuesta incluyendo el cálculo
     * del flag {@code overdue}.
     */
    public TaskResponseDto toResponseDto(Task task) {
        if (task == null) {
            return null;
        }

        TaskResponseDto dto = new TaskResponseDto();
        dto.setId(task.getId());
        dto.setTitle(task.getTitle());
        dto.setDescription(task.getDescription());
        dto.setExecutionDate(task.getExecutionDate());
        dto.setStatus(task.getStatus());
        dto.setCreatedAt(task.getCreatedAt());
        dto.setUpdatedAt(task.getUpdatedAt());
        dto.setOverdue(isOverdue(task));

        List<ChecklistItemResponseDto> itemDtos = task.getItems().stream()
                .map(this::toItemResponseDto)
                .toList();
        dto.setItems(itemDtos);

        return dto;
    }

    public ChecklistItemResponseDto toItemResponseDto(ChecklistItem item) {
        if (item == null) {
            return null;
        }
        return new ChecklistItemResponseDto(item.getId(), item.getDescription(), item.isCompleted());
    }

    /**
     * Aplica los datos del DTO sobre la entidad (crear).
     * El estado inicial se fuerza a PROGRAMADO si no viene informado.
     */
    public Task toEntity(TaskRequestDto dto) {
        Task task = new Task();
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setExecutionDate(dto.getExecutionDate());
        task.setStatus(dto.getStatus() == null ? TaskStatus.PROGRAMADO : dto.getStatus());

        for (ChecklistItemRequestDto itemDto : dto.getItems()) {
            ChecklistItem item = new ChecklistItem(itemDto.getDescription(), itemDto.isCompleted());
            task.addItem(item);
        }
        return task;
    }

    /**
     * Indica si la tarea está pendiente por ejecutar: estado PROGRAMADO y
     * fecha de ejecución ya vencida o igual a la actual.
     */
    public boolean isOverdue(Task task) {
        return task.getStatus() == TaskStatus.PROGRAMADO
                && task.getExecutionDate() != null
                && !task.getExecutionDate().isAfter(LocalDateTime.now());
    }
}
