package com.task.all.model.dto;

import com.task.all.model.entity.TaskStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO de salida para una tarea.
 * Incluye el flag {@code overdue} que indica si la tarea está pendiente por ejecutar
 * (estado PROGRAMADO con fecha ya vencida).
 */
public class TaskResponseDto {

    private Long id;
    private String title;
    private String description;
    private LocalDateTime executionDate;
    private TaskStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean overdue;
    private List<ChecklistItemResponseDto> items = new ArrayList<>();

    public TaskResponseDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isOverdue() {
        return overdue;
    }

    public void setOverdue(boolean overdue) {
        this.overdue = overdue;
    }

    public List<ChecklistItemResponseDto> getItems() {
        return items;
    }

    public void setItems(List<ChecklistItemResponseDto> items) {
        this.items = items;
    }
}
