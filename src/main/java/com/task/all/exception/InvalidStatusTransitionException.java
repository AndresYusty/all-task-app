package com.task.all.exception;

import com.task.all.model.entity.TaskStatus;

/**
 * Se lanza cuando se intenta pasar una tarea a un estado inválido
 * según las reglas de transición.
 */
public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(TaskStatus from, TaskStatus to) {
        super("Transición de estado no permitida: " + from + " -> " + to);
    }
}
