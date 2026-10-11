package com.finsphere.frontend.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

/** Field errors from binding the form itself (a value that is not a number or a date). */
final class FormErrors {

    private FormErrors() {
    }

    static Map<String, String> of(BindingResult binding) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : binding.getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.isBindingFailure() ? describe(error) : String.valueOf(
                    error.getDefaultMessage()));
        }
        if (errors.isEmpty() && binding.hasGlobalErrors()) {
            errors.put("form", "Check the highlighted fields.");
        }
        return errors;
    }

    private static String describe(FieldError error) {
        String rejected = String.valueOf(error.getRejectedValue());
        return "\"" + (rejected.length() > 40 ? rejected.substring(0, 40) + "…" : rejected)
                + "\" is not a valid value here";
    }
}
