package com.example.datarequestsystem.controller;

import com.example.datarequestsystem.dto.CreateDataRequestDto;
import com.example.datarequestsystem.model.DataRequest;
import com.example.datarequestsystem.model.RequestType;
import com.example.datarequestsystem.model.Status;
import com.example.datarequestsystem.service.DataRequestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DataRequestController.class)
class DataRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DataRequestService service;

    @Test
    void createRequest_ShouldReturn201_WhenValidInput() throws Exception {
        // Arrange
        CreateDataRequestDto dto = new CreateDataRequestDto();
        dto.setRequestType(RequestType.ACCESS);
        dto.setRequesterId("user123");
        dto.setNotes("Test notes");

        DataRequest mockResponse = new DataRequest();
        mockResponse.setId(1L);
        mockResponse.setRequestType(RequestType.ACCESS);
        mockResponse.setRequesterId("user123");
        mockResponse.setStatus(Status.RECEIVED);

        when(service.createRequest(any())).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(post("/data-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.requestType").value("ACCESS"))
                .andExpect(jsonPath("$.status").value("RECEIVED"));
    }

    @Test
    void listRequests_ShouldReturnList() throws Exception {
        // Arrange
        DataRequest request1 = new DataRequest();
        request1.setId(1L);
        request1.setRequestType(RequestType.ACCESS);
        request1.setRequesterId("user1");

        DataRequest request2 = new DataRequest();
        request2.setId(2L);
        request2.setRequestType(RequestType.DELETE);
        request2.setRequesterId("user2");

        List<DataRequest> mockList = Arrays.asList(request1, request2);
        when(service.listRequests(null, null)).thenReturn(mockList);

        // Act & Assert
        mockMvc.perform(get("/data-requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}