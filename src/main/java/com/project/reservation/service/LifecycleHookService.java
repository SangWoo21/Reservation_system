package com.project.reservation.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.autoscaling.model.CompleteLifecycleActionRequest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@Service
@Slf4j
public class LifecycleHookService {

    private final AutoScalingClient asgClient = AutoScalingClient.builder()
            .region(Region.AP_NORTHEAST_2)
            .build();

    // EC2 시작 완료 신호
    public void completeLaunch(String instanceId) {
        try {
            asgClient.completeLifecycleAction(
                    CompleteLifecycleActionRequest.builder()
                            .autoScalingGroupName("project-asg")
                            .lifecycleHookName("launch-hook")
                            .instanceId(instanceId)
                            .lifecycleActionResult("CONTINUE")
                            .build()
            );
            log.info("Launch hook 완료 신호 전송 - instanceId: {}", instanceId);
        } catch (Exception e) {
            log.error("Launch hook 신호 실패: {}", e.getMessage());
        }
    }

    // EC2 종료 완료 신호 (Graceful Shutdown 후 호출)
    public void completeTermination(String instanceId) {
        try {
            asgClient.completeLifecycleAction(
                    CompleteLifecycleActionRequest.builder()
                            .autoScalingGroupName("project-asg")
                            .lifecycleHookName("terminate-hook")
                            .instanceId(instanceId)
                            .lifecycleActionResult("CONTINUE")
                            .build()
            );
            log.info("Terminate hook 완료 신호 전송 - instanceId: {}", instanceId);
        } catch (Exception e) {
            log.error("Terminate hook 신호 실패: {}", e.getMessage());
        }
    }

    // IMDSv2 방식으로 현재 EC2 인스턴스 ID 조회
    public String fetchInstanceId() {
        try {
            // 1단계: 토큰 발급
            URL tokenUrl = new URL("http://169.254.169.254/latest/api/token");
            HttpURLConnection tokenConn = (HttpURLConnection) tokenUrl.openConnection();
            tokenConn.setRequestMethod("PUT");
            tokenConn.setRequestProperty("X-aws-ec2-metadata-token-ttl-seconds", "21600");
            tokenConn.setConnectTimeout(1000);
            tokenConn.setDoOutput(true);
            String token = new BufferedReader(
                    new InputStreamReader(tokenConn.getInputStream())
            ).readLine();

            // 2단계: 토큰으로 인스턴스 ID 조회
            URL url = new URL("http://169.254.169.254/latest/meta-data/instance-id");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("X-aws-ec2-metadata-token", token);
            conn.setConnectTimeout(1000);
            return new BufferedReader(new InputStreamReader(conn.getInputStream())).readLine();
        } catch (Exception e) {
            log.info("로컬 환경 - EC2 메타데이터 없음 (정상)");
            return null;
        }
    }
}
