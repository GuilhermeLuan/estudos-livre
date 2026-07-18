package br.com.estudalivre.review.service;

public class ReviewPlanNotFoundException extends RuntimeException {

    public ReviewPlanNotFoundException() {
        super("O plano de revisão não foi encontrado.");
    }
}
