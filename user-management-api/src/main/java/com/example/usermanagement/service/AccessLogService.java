package com.example.usermanagement.service;

import com.example.usermanagement.model.AccessLog;
import com.example.usermanagement.repository.AccessLogRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccessLogService {

    private final AccessLogRepository accessLogRepository;

    public AccessLogService(AccessLogRepository accessLogRepository) {
        this.accessLogRepository = accessLogRepository;
    }

    @Async("applicationTaskExecutor")
    @Transactional
    public void save(AccessLog accessLog) {
        accessLogRepository.save(accessLog);
    }
}
