package com.task.all.model.dto;

/**
 * DTO de salida para un ítem checkeable.
 */
public class ChecklistItemResponseDto {

    private Long id;
    private String description;
    private boolean completed;

    public ChecklistItemResponseDto() {
    }

    public ChecklistItemResponseDto(Long id, String description, boolean completed) {
        this.id = id;
        this.description = description;
        this.completed = completed;
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
