package ru.golovkin.oxford3000.dictionary.model;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND)
public class TermNotFoundException extends IllegalStateException {

    public TermNotFoundException(String message) {
        super(message);
    }
}
