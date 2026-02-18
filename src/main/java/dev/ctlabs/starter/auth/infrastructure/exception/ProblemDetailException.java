package dev.ctlabs.starter.auth.infrastructure.exception;

import lombok.Getter;
import org.springframework.http.ProblemDetail;

@Getter
/**
 * Exception that wraps a {@link ProblemDetail} for consistent error reporting.
 */
public class ProblemDetailException extends RuntimeException {
    private final ProblemDetail problemDetail;

    public ProblemDetailException(ProblemDetail problemDetail) {
        super(problemDetail.getDetail());
        this.problemDetail = problemDetail;
    }
}
