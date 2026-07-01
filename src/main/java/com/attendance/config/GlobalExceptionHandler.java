package com.attendance.config;

import com.attendance.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Global exception handler for graceful error handling across the application.
 * Prevents 500 errors from being exposed to users with helpful redirect messages.
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public String handleBusinessException(BusinessException ex, RedirectAttributes redirect) {
        log.warn("BusinessException: {}", ex.getMessage());
        redirect.addFlashAttribute("error", ex.getMessage());
        return "redirect:/admin/dashboard";
    }

    @ExceptionHandler(IllegalStateException.class)
    public String handleIllegalStateException(IllegalStateException ex, RedirectAttributes redirect) {
        log.warn("IllegalStateException: {}", ex.getMessage());
        redirect.addFlashAttribute("error", "Your account or user record is not properly set up. Please contact administration.");
        return "redirect:/login";
    }

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(AccessDeniedException ex, RedirectAttributes redirect) {
        log.warn("AccessDeniedException: {}", ex.getMessage());
        redirect.addFlashAttribute("error", "You don't have permission to access this page.");
        return "redirect:/";
    }

    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception ex, Model model) {
        log.error("Unexpected error occurred", ex);
        model.addAttribute("error", "An unexpected error occurred. Please try again later.");
        model.addAttribute("message", ex.getMessage());
        return "error";
    }
}
