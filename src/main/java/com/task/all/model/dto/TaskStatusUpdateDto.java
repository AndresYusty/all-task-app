package com.task.all.model.dto;

import com.task.all.model.entity.TaskStatus;

import jakarta.validation.constraints.NotNull;

/**
 * DTO para la actualización puntual del estado de una tarea.
 */
public class TaskStatusUpdateDto {

    @NotNull(message = "El nuevo estado es obligatorio")
    private TaskStatus status;

    public TaskStatusUpdateDto() {
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }
}
