package com.task.all.service.impl;

import com.task.all.exception.InvalidStatusTransitionException;
import com.task.all.exception.ResourceNotFoundException;
import com.task.all.mapper.TaskMapper;
import com.task.all.model.dto.ChecklistItemRequestDto;
import com.task.all.model.dto.ChecklistItemResponseDto;
import com.task.all.model.dto.TaskRequestDto;
import com.task.all.model.dto.TaskResponseDto;
import com.task.all.model.entity.ChecklistItem;
import com.task.all.model.entity.Task;
import com.task.all.model.entity.TaskStatus;
import com.task.all.repository.TaskRepository;
import com.task.all.service.TaskService;
import com.task.all.util.TaskSpecifications;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementación de la lógica de negocio de tareas e ítems checkeables.
 */
@Service
@Transactional
public class TaskServiceImpl implements TaskService {

    /**
     * Reglas de transición de estado permitidas.
     */
    private static final Map<TaskStatus, Set<TaskStatus>> ALLOWED_TRANSITIONS = Map.of(
            TaskStatus.PROGRAMADO, Set.of(TaskStatus.EN_EJECUCION, TaskStatus.CANCELADA),
            TaskStatus.EN_EJECUCION, Set.of(TaskStatus.FINALIZADA, TaskStatus.CANCELADA),
            TaskStatus.FINALIZADA, Set.of(),
            TaskStatus.CANCELADA, Set.of()
    );

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;

    public TaskServiceImpl(TaskRepository taskRepository, TaskMapper taskMapper) {
        this.taskRepository = taskRepository;
        this.taskMapper = taskMapper;
    }

    @Override
    public TaskResponseDto create(TaskRequestDto dto) {
        Task task = taskMapper.toEntity(dto);
        Task saved = taskRepository.save(task);
        return taskMapper.toResponseDto(saved);
    }

    @Override
    public TaskResponseDto update(Long taskId, TaskRequestDto dto) {
        Task task = getTaskOrThrow(taskId);

        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setExecutionDate(dto.getExecutionDate());

        if (dto.getStatus() != null && dto.getStatus() != task.getStatus()) {
            validateTransition(task.getStatus(), dto.getStatus());
            task.setStatus(dto.getStatus());
        }

        syncItems(task, dto.getItems());

        Task saved = taskRepository.save(task);
        return taskMapper.toResponseDto(saved);
    }

    @Override
    public void delete(Long taskId) {
        Task task = getTaskOrThrow(taskId);
        taskRepository.delete(task);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponseDto findById(Long taskId) {
        Task task = getTaskOrThrow(taskId);
        return taskMapper.toResponseDto(task);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskResponseDto> search(String text,
                                        TaskStatus status,
                                        LocalDateTime from,
                                        LocalDateTime to,
                                        Boolean overdueOnly,
                                        Pageable pageable) {

        List<Specification<Task>> specs = new ArrayList<>();
        addIfNotNull(specs, TaskSpecifications.matchesText(text));
        addIfNotNull(specs, TaskSpecifications.hasStatus(status));
        addIfNotNull(specs, TaskSpecifications.executionBetween(from, to));
        if (Boolean.TRUE.equals(overdueOnly)) {
            specs.add(TaskSpecifications.isPendingToExecute());
        }

        Specification<Task> spec = Specification.allOf(specs);
        return taskRepository.findAll(spec, pageable).map(taskMapper::toResponseDto);
    }

    private static <T> void addIfNotNull(List<Specification<T>> specs, Specification<T> spec) {
        if (spec != null) {
            specs.add(spec);
        }
    }

    @Override
    public TaskResponseDto updateStatus(Long taskId, TaskStatus newStatus) {
        Task task = getTaskOrThrow(taskId);
        if (task.getStatus() != newStatus) {
            validateTransition(task.getStatus(), newStatus);
            task.setStatus(newStatus);
            task = taskRepository.save(task);
        }
        return taskMapper.toResponseDto(task);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponseDto> findPending() {
        return taskRepository.findAll(TaskSpecifications.isScheduled()).stream()
                .map(taskMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponseDto> findOverdue() {
        return taskRepository.findAll(TaskSpecifications.isPendingToExecute()).stream()
                .map(taskMapper::toResponseDto)
                .toList();
    }

    @Override
    public ChecklistItemResponseDto addItem(Long taskId, ChecklistItemRequestDto itemDto) {
        Task task = getTaskOrThrow(taskId);
        ChecklistItem item = new ChecklistItem(itemDto.getDescription(), itemDto.isCompleted());
        task.addItem(item);
        taskRepository.save(task);
        return taskMapper.toItemResponseDto(item);
    }

    @Override
    public ChecklistItemResponseDto updateItem(Long taskId, Long itemId, ChecklistItemRequestDto itemDto) {
        Task task = getTaskOrThrow(taskId);
        ChecklistItem item = findItem(task, itemId);
        item.setDescription(itemDto.getDescription());
        item.setCompleted(itemDto.isCompleted());
        taskRepository.save(task);
        return taskMapper.toItemResponseDto(item);
    }

    @Override
    public ChecklistItemResponseDto toggleItem(Long taskId, Long itemId, boolean completed) {
        Task task = getTaskOrThrow(taskId);
        ChecklistItem item = findItem(task, itemId);
        item.setCompleted(completed);
        taskRepository.save(task);
        return taskMapper.toItemResponseDto(item);
    }

    @Override
    public void deleteItem(Long taskId, Long itemId) {
        Task task = getTaskOrThrow(taskId);
        ChecklistItem item = findItem(task, itemId);
        task.removeItem(item);
        taskRepository.save(task);
    }

    private Task getTaskOrThrow(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.task(id));
    }

    private ChecklistItem findItem(Task task, Long itemId) {
        return task.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> ResourceNotFoundException.item(itemId));
    }

    /**
     * Sincroniza los ítems persistidos de una tarea con los del request:
     * - Conserva (y actualiza) los existentes por id.
     * - Agrega los nuevos (sin id).
     * - Elimina los que ya no vienen.
     */
    private void syncItems(Task task, List<ChecklistItemRequestDto> requestedItems) {
        if (requestedItems == null) {
            return;
        }

        Map<Long, ChecklistItem> existingById = new HashMap<>();
        for (ChecklistItem item : task.getItems()) {
            if (item.getId() != null) {
                existingById.put(item.getId(), item);
            }
        }

        List<ChecklistItem> toRemove = new ArrayList<>(task.getItems());

        for (ChecklistItemRequestDto reqItem : requestedItems) {
            if (reqItem.getId() != null && existingById.containsKey(reqItem.getId())) {
                ChecklistItem existing = existingById.get(reqItem.getId());
                existing.setDescription(reqItem.getDescription());
                existing.setCompleted(reqItem.isCompleted());
                toRemove.remove(existing);
            } else {
                ChecklistItem newItem = new ChecklistItem(reqItem.getDescription(), reqItem.isCompleted());
                task.addItem(newItem);
            }
        }

        for (ChecklistItem remove : toRemove) {
            task.removeItem(remove);
        }
    }

    private void validateTransition(TaskStatus from, TaskStatus to) {
        Set<TaskStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            throw new InvalidStatusTransitionException(from, to);
        }
    }
}
