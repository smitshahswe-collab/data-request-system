package com.example.datarequestsystem.service;

import com.example.datarequestsystem.model.DataRequest;

public interface AIService {
    String generateRequestSummary(DataRequest request);
}