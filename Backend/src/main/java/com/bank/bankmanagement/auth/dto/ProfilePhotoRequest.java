package com.bank.bankmanagement.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Profile photo sent by the frontend as a small resized image data URL,
 * e.g. "data:image/jpeg;base64,/9j/4AAQ...".
 */
public record ProfilePhotoRequest(
        @NotBlank @Size(max = 250000) String photo
) {}
