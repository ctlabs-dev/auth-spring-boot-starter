package dev.ctlabs.starter.auth.infrastructure.exception;

import lombok.Getter;
import org.springframework.http.ProblemDetail;

@Getter
public class ProblemDetailException extends RuntimeException {
    private final ProblemDetail problemDetail;

    public ProblemDetailException(ProblemDetail problemDetail) {
        super(problemDetail.getDetail());
        this.problemDetail = problemDetail;
    }
}
