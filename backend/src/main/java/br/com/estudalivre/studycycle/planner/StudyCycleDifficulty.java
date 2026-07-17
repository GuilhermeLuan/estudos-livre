package br.com.estudalivre.studycycle.planner;

import java.math.BigDecimal;

public enum StudyCycleDifficulty {
    EASY("1.00"),
    MEDIUM("1.25"),
    HARD("1.50");

    private final BigDecimal factor;

    StudyCycleDifficulty(String factor) {
        this.factor = new BigDecimal(factor);
    }

    public BigDecimal factor() {
        return factor;
    }
}
