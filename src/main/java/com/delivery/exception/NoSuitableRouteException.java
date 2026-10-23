package com.delivery.exception;

public class NoSuitableRouteException extends RuntimeException {
    public NoSuitableRouteException(String message) {
        super(message);
    }
}