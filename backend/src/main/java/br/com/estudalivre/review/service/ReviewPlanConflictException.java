package br.com.estudalivre.review.service;

public class ReviewPlanConflictException extends RuntimeException {

    public ReviewPlanConflictException() {
        super("O plano de revisão foi alterado ou não permite mais esta operação.");
    }
}
