package io.github.xfoundries.demo.expenseapproval.application.claim.query;

import org.jfoundry.application.exception.ApplicationException;

public final class ClaimAccessDeniedException extends ApplicationException {

    public ClaimAccessDeniedException(String message) {
        super(message);
    }
}
