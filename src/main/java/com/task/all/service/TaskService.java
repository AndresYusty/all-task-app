package com.task.all.service;

import com.task.all.model.dto.ChecklistItemRequestDto;
import com.task.all.model.dto.ChecklistItemResponseDto;
import com.task.all.model.dto.TaskRequestDto;
import com.task.all.model.dto.TaskResponseDto;
import com.task.all.model.entity.TaskStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Contrato de la lógica de negocio sobre tareas y sus ítems checkeables.
 */
public interface TaskService {

    TaskResponseDto create(TaskRequestDto dto);

    TaskResponseDto update(Long taskId, TaskRequestDto dto);

    void delete(Long taskId);

    TaskResponseDto findById(Long taskId);

    Page<TaskResponseDto> search(String text,
                                 TaskStatus status,
                                 LocalDateTime from,
                                 LocalDateTime to,
                                 Boolean overdueOnly,
                                 Pageable pageable);

    TaskResponseDto updateStatus(Long taskId, TaskStatus newStatus);

    List<TaskResponseDto> findPending();

    List<TaskResponseDto> findOverdue();

    ChecklistItemResponseDto addItem(Long taskId, ChecklistItemRequestDto itemDto);

    ChecklistItemResponseDto updateItem(Long taskId, Long itemId, ChecklistItemRequestDto itemDto);

    ChecklistItemResponseDto toggleItem(Long taskId, Long itemId, boolean completed);

    void deleteItem(Long taskId, Long itemId);
}
