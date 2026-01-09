package com.example.datarequestsystem.service;

import com.example.datarequestsystem.dto.CreateDataRequestDto;
import com.example.datarequestsystem.dto.UpdateStatusDto;
import com.example.datarequestsystem.exception.InvalidStatusTransitionException;
import com.example.datarequestsystem.exception.ResourceNotFoundException;
import com.example.datarequestsystem.model.DataRequest;
import com.example.datarequestsystem.model.RequestType;
import com.example.datarequestsystem.model.Status;
import com.example.datarequestsystem.repository.DataRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DataRequestService {

    private final DataRequestRepository repository;
    private final AIService aiService;

    public DataRequestService(DataRequestRepository repository, AIService aiService) {
        this.repository = repository;
        this.aiService = aiService;
    }

    @Transactional
    public DataRequest createRequest(CreateDataRequestDto dto) {
        DataRequest request = new DataRequest();
        request.setRequestType(dto.getRequestType());
        request.setRequesterId(dto.getRequesterId());
        request.setNotes(dto.getNotes());
        request.setStatus(Status.RECEIVED);
        request.setCreatedAt(LocalDateTime.now());
        request.setLastUpdatedAt(LocalDateTime.now());

        // Generate AI summary
        String summary = aiService.generateRequestSummary(request);
        request.setAiSummary(summary);

        return repository.save(request);
    }

    public DataRequest getRequestById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found with id: " + id));
    }

    public List<DataRequest> listRequests(Status status, RequestType requestType) {
        if (status != null && requestType != null) {
            return repository.findByStatusAndRequestType(status, requestType);
        } else if (status != null) {
            return repository.findByStatus(status);
        } else if (requestType != null) {
            return repository.findByRequestType(requestType);
        }
        return repository.findAll();
    }

    @Transactional
    public DataRequest updateStatus(Long id, UpdateStatusDto dto) {
        DataRequest request = getRequestById(id);

        if (!request.getStatus().canTransitionTo(dto.getStatus())) {
            throw new InvalidStatusTransitionException(
                    String.format("Cannot transition from %s to %s",
                            request.getStatus(), dto.getStatus())
            );
        }

        request.setStatus(dto.getStatus());
        request.setLastUpdatedAt(LocalDateTime.now());

        return repository.save(request);
    }
}