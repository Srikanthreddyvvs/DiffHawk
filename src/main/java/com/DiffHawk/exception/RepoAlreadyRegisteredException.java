package com.DiffHawk.exception;

public class RepoAlreadyRegisteredException extends RuntimeException {
    public RepoAlreadyRegisteredException(String message) {
        super(message);
    }
}
