package com.helpdesk.exception;

import com.helpdesk.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ForbiddenException.class)
    public ProblemDetail handleForbidden(
            ForbiddenException exception,
            HttpServletRequest request) {

        return buildProblemDetail(
                HttpStatus.FORBIDDEN,
                "Forbidden",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthenticationException(
            AuthenticationException exception,
            HttpServletRequest request) {
        return buildProblemDetail(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                "Invalid email or password",
                request
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(
            AccessDeniedException exception,
            HttpServletRequest request) {

        return buildProblemDetail(
                HttpStatus.FORBIDDEN,
                "Forbidden",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBadRequest(
            IllegalArgumentException exception,
            HttpServletRequest request) {

        return buildProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(
            Exception exception,
            HttpServletRequest request) {

        return buildProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred",
                request
        );
    }

    private ProblemDetail buildProblemDetail(
            HttpStatus status,
            String title,
            String detail,
            HttpServletRequest request) {

        String requestId =
                (String) request.getAttribute(
                        CorrelationIdFilter.ATTRIBUTE_NAME
                );

        if (requestId == null || requestId.isBlank()) {
            requestId = request.getHeader(
                    CorrelationIdFilter.HEADER_NAME
            );
        }

        ProblemDetail problemDetail =
                ProblemDetail.forStatusAndDetail(
                        status,
                        detail != null && !detail.isBlank()
                                ? detail
                                : title
                );

        problemDetail.setTitle(title);
        problemDetail.setProperty("requestId", requestId);

        return problemDetail;
    }
}
