package com.task.all.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO usado al crear o actualizar un ítem checkeable dentro de una tarea.
 * El id es opcional: si viene se reutiliza, si no se crea un ítem nuevo.
 */
public class ChecklistItemRequestDto {

    private Long id;

    @NotBlank(message = "La descripción del ítem es obligatoria")
    @Size(max = 250, message = "La descripción del ítem no puede superar 250 caracteres")
    private String description;

    private boolean completed;

    public ChecklistItemRequestDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
