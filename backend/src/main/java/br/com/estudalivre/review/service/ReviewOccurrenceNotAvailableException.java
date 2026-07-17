package br.com.estudalivre.review.service;

public class ReviewOccurrenceNotAvailableException extends RuntimeException {
    public ReviewOccurrenceNotAvailableException() {
        super("A revisão não está disponível para execução.");
    }
}
