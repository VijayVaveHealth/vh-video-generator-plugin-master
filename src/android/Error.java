package com.xmartlabs.cordova.frame2video;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

enum Error {
    CONTEXT_NOT_INITIALIZED("1001", "Context was not yet initialized."),
    ILLEGAL_STATE("1002", "Running in invalid state."),

    INVALID_NUMBER_OF_ARGUMENTS("1101", "Invalid number of arguments received."),
    INVALID_ARGUMENT_TYPE("1102", "Invalid type for argument."),
    INVALID_JSON_ARGUMENT("1103", "Failed to parse JSON argument."),

    GENERIC_IO_ERROR("1206", "An unexpected I/O error was got."),

    UNKNOWN_ERROR("1999", "Failed with an unexpected error."),
    ;

    private final static String codePrefix = "F2V-";
    private final String code;
    private final String message;

    Error(@NonNull String code, @NonNull String message) {
        this.code = code;
        this.message = message;
    }

    @NonNull
    String getErrorMessage() {
        return getErrorMessage(null);
    }

    @NonNull
    String getErrorMessage(@Nullable String additionalInfo, Object... args) {
        return String.format("%s%s: %s.%s", codePrefix, code, message,
            additionalInfo == null ? "" : (" " + String.format(additionalInfo, args)));
    }
}
