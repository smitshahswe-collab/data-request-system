package com.example.datarequestsystem.service;

import com.example.datarequestsystem.dto.CreateDataRequestDto;
import com.example.datarequestsystem.dto.UpdateStatusDto;
import com.example.datarequestsystem.exception.InvalidStatusTransitionException;
import com.example.datarequestsystem.exception.ResourceNotFoundException;
import com.example.datarequestsystem.model.DataRequest;
import com.example.datarequestsystem.model.RequestType;
import com.example.datarequestsystem.model.Status;
import com.example.datarequestsystem.repository.DataRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataRequestServiceTest {

    @Mock
    private DataRequestRepository repository;

    @Mock
    private AIService aiService;

    @InjectMocks
    private DataRequestService service;

    private CreateDataRequestDto createDto;
    private DataRequest savedRequest;

    @BeforeEach
    void setUp() {
        createDto = new CreateDataRequestDto();
        createDto.setRequestType(RequestType.ACCESS);
        createDto.setRequesterId("user123");
        createDto.setNotes("Need my data");

        savedRequest = new DataRequest();
        savedRequest.setId(1L);
        savedRequest.setRequestType(RequestType.ACCESS);
        savedRequest.setRequesterId("user123");
        savedRequest.setStatus(Status.RECEIVED);
        savedRequest.setNotes("Need my data");
    }

    @Test
    void createRequest_ShouldSetInitialStatusToReceived() {
        // Arrange
        when(aiService.generateRequestSummary(any())).thenReturn("Test summary");
        when(repository.save(any(DataRequest.class))).thenReturn(savedRequest);

        // Act
        DataRequest result = service.createRequest(createDto);

        // Assert
        assertNotNull(result);
        assertEquals(Status.RECEIVED, result.getStatus());
        assertEquals(RequestType.ACCESS, result.getRequestType());
        assertEquals("user123", result.getRequesterId());
        verify(aiService, times(1)).generateRequestSummary(any());
        verify(repository, times(1)).save(any(DataRequest.class));
    }

    @Test
    void getRequestById_ShouldReturnRequest_WhenExists() {
        // Arrange
        when(repository.findById(1L)).thenReturn(Optional.of(savedRequest));

        // Act
        DataRequest result = service.getRequestById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(repository, times(1)).findById(1L);
    }

    @Test
    void getRequestById_ShouldThrowException_WhenNotFound() {
        // Arrange
        when(repository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> service.getRequestById(999L));
    }

    @Test
    void updateStatus_ShouldUpdateSuccessfully_WhenTransitionIsValid() {
        // Arrange
        UpdateStatusDto updateDto = new UpdateStatusDto();
        updateDto.setStatus(Status.IN_REVIEW);

        savedRequest.setStatus(Status.RECEIVED);
        when(repository.findById(1L)).thenReturn(Optional.of(savedRequest));
        when(repository.save(any(DataRequest.class))).thenReturn(savedRequest);

        // Act
        DataRequest result = service.updateStatus(1L, updateDto);

        // Assert
        assertNotNull(result);
        verify(repository, times(1)).save(any(DataRequest.class));
    }

    @Test
    void updateStatus_ShouldThrowException_WhenTransitionIsInvalid() {
        // Arrange
        UpdateStatusDto updateDto = new UpdateStatusDto();
        updateDto.setStatus(Status.IN_REVIEW);

        savedRequest.setStatus(Status.COMPLETED);
        when(repository.findById(1L)).thenReturn(Optional.of(savedRequest));

        // Act & Assert
        assertThrows(InvalidStatusTransitionException.class,
                () -> service.updateStatus(1L, updateDto));
        verify(repository, never()).save(any(DataRequest.class));
    }
}