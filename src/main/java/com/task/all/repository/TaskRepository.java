package com.task.all.repository;

import com.task.all.model.entity.Task;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA para la entidad Task.
 * Extiende {@link JpaSpecificationExecutor} para permitir búsquedas dinámicas
 * combinando múltiples filtros (título, descripción, estado, rango de fechas).
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {
}
