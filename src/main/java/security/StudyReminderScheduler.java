package services;

import model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import repos.UserRepo;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StudyReminderScheduler {

    @Autowired
    private UserRepo userRepo;

    private final RestTemplate restTemplate = new RestTemplate();

    @Scheduled(cron = "0 * * * * *")
    public void checkStudyReminders() {
        LocalDateTime now = LocalDateTime.now();

        int currentHour = now.getHour();
        int currentMinute = now.getMinute();

        List<User> users = userRepo.findByStudyReminderEnabledTrueAndExpoPushTokenIsNotNull();

        for (User user : users) {
            try {
                if (user.getStudyReminderHour() == null || user.getStudyReminderMinute() == null) {
                    continue;
                }

                if (user.getPushNotificationsEnabled() != null && !user.getPushNotificationsEnabled()) {
                    continue;
                }

                boolean sameHour = user.getStudyReminderHour().equals(currentHour);
                boolean sameMinute = user.getStudyReminderMinute().equals(currentMinute);

                if (sameHour && sameMinute) {
                    sendStudyReminderPush(user);
                }

            } catch (Exception e) {
                System.out.println("STUDY REMINDER ERROR FOR USER " + user.getUsername() + ": " + e.getMessage());
            }
        }
    }

    private void sendStudyReminderPush(User user) {
        try {
            if (user == null) {
                return;
            }

            if (user.getExpoPushToken() == null || user.getExpoPushToken().isBlank()) {
                return;
            }

            if (user.getPushNotificationsEnabled() != null && !user.getPushNotificationsEnabled()) {
                return;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> data = new HashMap<>();
            data.put("type", "study_reminder");
            data.put("username", user.getUsername());

            Map<String, Object> payload = new HashMap<>();
            payload.put("to", user.getExpoPushToken());
            payload.put("title", "StudyBuddy");
            payload.put("body", "Час вчитися 📚");
            payload.put("sound", "default");
            payload.put("data", data);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            restTemplate.postForEntity(
                    "https://exp.host/--/api/v2/push/send",
                    request,
                    String.class
            );

        } catch (Exception e) {
            System.out.println("STUDY PUSH SEND ERROR: " + e.getMessage());
        }
    }
}