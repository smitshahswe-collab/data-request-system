package com.example.datarequestsystem.controller;

import com.example.datarequestsystem.dto.CreateDataRequestDto;
import com.example.datarequestsystem.dto.UpdateStatusDto;
import com.example.datarequestsystem.model.DataRequest;
import com.example.datarequestsystem.model.RequestType;
import com.example.datarequestsystem.model.Status;
import com.example.datarequestsystem.service.DataRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/data-requests")
@CrossOrigin(origins = "http://localhost:3000")
public class DataRequestController {

    private final DataRequestService service;

    public DataRequestController(DataRequestService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<DataRequest> createRequest(@Valid @RequestBody CreateDataRequestDto dto) {
        DataRequest created = service.createRequest(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DataRequest> getRequest(@PathVariable Long id) {
        DataRequest request = service.getRequestById(id);
        return ResponseEntity.ok(request);
    }

    @GetMapping
    public ResponseEntity<List<DataRequest>> listRequests(
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) RequestType requestType) {
        List<DataRequest> requests = service.listRequests(status, requestType);
        return ResponseEntity.ok(requests);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<DataRequest> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusDto dto) {
        DataRequest updated = service.updateStatus(id, dto);
        return ResponseEntity.ok(updated);
    }
}