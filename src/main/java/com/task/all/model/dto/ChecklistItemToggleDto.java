package com.task.all.model.dto;

import jakarta.validation.constraints.NotNull;

/**
 * DTO para marcar/desmarcar un ítem checkeable.
 */
public class ChecklistItemToggleDto {

    @NotNull(message = "El campo completed es obligatorio")
    private Boolean completed;

    public ChecklistItemToggleDto() {
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }
}
