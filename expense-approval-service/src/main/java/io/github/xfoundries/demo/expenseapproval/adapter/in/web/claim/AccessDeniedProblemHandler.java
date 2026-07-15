package io.github.xfoundries.demo.expenseapproval.adapter.in.web.claim;

import io.github.xfoundries.demo.expenseapproval.application.claim.query.ClaimAccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AccessDeniedProblemHandler {

    @ExceptionHandler(ClaimAccessDeniedException.class)
    ResponseEntity<ProblemDetail> handle(ClaimAccessDeniedException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, exception.getMessage());
        problem.setTitle("Forbidden");
        problem.setProperty("code", "CLAIM_ACCESS_DENIED");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }
}
