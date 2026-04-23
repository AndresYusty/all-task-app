package com.task.all.model.dto;

import com.task.all.model.entity.TaskStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO de entrada para crear o editar una tarea.
 * El estado es opcional al crear (se usará PROGRAMADO por defecto).
 */
public class TaskRequestDto {

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 150, message = "El título no puede superar 150 caracteres")
    private String title;

    @Size(max = 2000, message = "La descripción no puede superar 2000 caracteres")
    private String description;

    @NotNull(message = "La fecha de ejecución es obligatoria")
    private LocalDateTime executionDate;

    private TaskStatus status;

    @Valid
    private List<ChecklistItemRequestDto> items = new ArrayList<>();

    public TaskRequestDto() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getExecutionDate() {
        return executionDate;
    }

    public void setExecutionDate(LocalDateTime executionDate) {
        this.executionDate = executionDate;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public List<ChecklistItemRequestDto> getItems() {
        return items;
    }

    public void setItems(List<ChecklistItemRequestDto> items) {
        this.items = items == null ? new ArrayList<>() : items;
    }
}
