package com.dating.core.common.error;

/** Бросается, когда запрашиваемая сущность не найдена. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
