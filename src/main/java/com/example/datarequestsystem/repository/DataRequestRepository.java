package com.example.datarequestsystem.repository;

import com.example.datarequestsystem.model.DataRequest;
import com.example.datarequestsystem.model.RequestType;
import com.example.datarequestsystem.model.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataRequestRepository extends JpaRepository<DataRequest, Long> {
    List<DataRequest> findByStatus(Status status);
    List<DataRequest> findByRequestType(RequestType requestType);
    List<DataRequest> findByStatusAndRequestType(Status status, RequestType requestType);
}