package com.example.datarequestsystem.dto;

import com.example.datarequestsystem.model.Status;
import jakarta.validation.constraints.NotNull;

public class UpdateStatusDto {

    @NotNull(message = "Status is required")
    private Status status;

    public UpdateStatusDto() {
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}