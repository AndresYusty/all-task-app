package com.task.all.exception;

/**
 * Se lanza cuando un recurso (tarea, ítem) no existe en la base de datos.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException task(Long id) {
        return new ResourceNotFoundException("No existe una tarea con id " + id);
    }

    public static ResourceNotFoundException item(Long id) {
        return new ResourceNotFoundException("No existe un ítem checkeable con id " + id);
    }
}
