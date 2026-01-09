package com.example.datarequestsystem.dto;

import com.example.datarequestsystem.model.RequestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateDataRequestDto {

    @NotNull(message = "Request type is required")
    private RequestType requestType;

    @NotBlank(message = "Requester ID is required")
    private String requesterId;

    private String notes;

    public CreateDataRequestDto() {
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(RequestType requestType) {
        this.requestType = requestType;
    }

    public String getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(String requesterId) {
        this.requesterId = requesterId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}