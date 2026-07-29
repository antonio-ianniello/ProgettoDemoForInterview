package com.example.usermanagement.audit;

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
