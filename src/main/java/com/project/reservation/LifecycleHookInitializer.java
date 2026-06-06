package com.project.reservation;

import com.project.reservation.service.LifecycleHookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LifecycleHookInitializer implements ApplicationRunner {

    private final LifecycleHookService lifecycleHookService;

    @Override
    public void run(ApplicationArguments args) {
        String instanceId = lifecycleHookService.fetchInstanceId();

        if (instanceId != null) {
            lifecycleHookService.completeLaunch(instanceId);
        }
    }
}
