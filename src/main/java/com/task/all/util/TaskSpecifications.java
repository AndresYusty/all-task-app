package com.task.all.util;

import com.task.all.model.entity.Task;
import com.task.all.model.entity.TaskStatus;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilidades para construir {@link Specification} dinámicos sobre la entidad Task.
 * Permite componer filtros opcionales para búsquedas flexibles.
 */
public final class TaskSpecifications {

    private TaskSpecifications() {
    }

    /**
     * Busca por texto libre dentro de título o descripción (case-insensitive).
     */
    public static Specification<Task> matchesText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        final String like = "%" + text.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), like),
                cb.like(cb.lower(root.get("description")), like)
        );
    }

    /**
     * Filtra por estado exacto.
     */
    public static Specification<Task> hasStatus(TaskStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    /**
     * Filtra por un rango (inclusivo) de fecha de ejecución. Ambos extremos son opcionales.
     */
    public static Specification<Task> executionBetween(LocalDateTime from, LocalDateTime to) {
        if (from == null && to == null) {
            return null;
        }
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("executionDate"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("executionDate"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Tareas pendientes por ejecutar:
     * estado PROGRAMADO cuya fecha de ejecución ya llegó o pasó.
     */
    public static Specification<Task> isPendingToExecute() {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), TaskStatus.PROGRAMADO),
                cb.lessThanOrEqualTo(root.get("executionDate"), LocalDateTime.now())
        );
    }

    /**
     * Todas las tareas pendientes (estado PROGRAMADO), sin importar si ya vencieron.
     * Sirve para "Visualizar tareas pendientes por ejecutar".
     */
    public static Specification<Task> isScheduled() {
        return (root, query, cb) -> cb.equal(root.get("status"), TaskStatus.PROGRAMADO);
    }
}
