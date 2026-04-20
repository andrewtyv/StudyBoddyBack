package security;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class WebSocketRoomPresenceTracker {

    private static final Pattern ROOM_TOPIC_PATTERN =
            Pattern.compile("^/topic/rooms/(\\d+)$");

    private final Map<Long, Set<String>> roomSessions = new ConcurrentHashMap<>();

    private final Map<String, Map<String, Long>> sessionSubscriptions = new ConcurrentHashMap<>();

    private final Map<String, String> sessionUsernames = new ConcurrentHashMap<>();

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        String destination = accessor.getDestination();
        String sessionId = accessor.getSessionId();
        String subscriptionId = accessor.getSubscriptionId();

        if (destination == null || sessionId == null || subscriptionId == null) return;

        Matcher m = ROOM_TOPIC_PATTERN.matcher(destination);
        if (!m.matches()) return;

        Long roomId = Long.parseLong(m.group(1));
        String username = resolveUsername(accessor.getUser(), event.getUser());
        if (username == null) return;

        sessionUsernames.put(sessionId, username);

        roomSessions
                .computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet())
                .add(sessionId);

        sessionSubscriptions
                .computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>())
                .put(subscriptionId, roomId);
    }

    @EventListener
    public void handleUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        String sessionId = accessor.getSessionId();
        String subscriptionId = accessor.getSubscriptionId();

        if (sessionId == null || subscriptionId == null) return;

        Map<String, Long> subs = sessionSubscriptions.get(sessionId);
        if (subs == null) return;

        Long roomId = subs.remove(subscriptionId);
        if (roomId == null) return;

        removeSessionFromRoom(roomId, sessionId);

        if (subs.isEmpty()) {
            sessionSubscriptions.remove(sessionId);
        }
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        if (sessionId == null) return;

        Map<String, Long> subs = sessionSubscriptions.remove(sessionId);
        sessionUsernames.remove(sessionId);

        if (subs != null) {
            for (Long roomId : subs.values()) {
                removeSessionFromRoom(roomId, sessionId);
            }
        }
    }

    private void removeSessionFromRoom(Long roomId, String sessionId) {
        Set<String> sessions = roomSessions.get(roomId);
        if (sessions == null) return;

        sessions.remove(sessionId);

        if (sessions.isEmpty()) {
            roomSessions.remove(roomId);
        }
    }

    private String resolveUsername(Principal fromAccessor, Principal fromEvent) {
        if (fromAccessor != null && fromAccessor.getName() != null) return fromAccessor.getName();
        if (fromEvent != null && fromEvent.getName() != null) return fromEvent.getName();
        return null;
    }

    public boolean isUserInRoom(Long roomId, String username) {
        if (roomId == null || username == null) return false;

        Set<String> sessions = roomSessions.get(roomId);
        if (sessions == null || sessions.isEmpty()) return false;

        for (String sessionId : sessions) {
            String sessionUsername = sessionUsernames.get(sessionId);
            if (username.equals(sessionUsername)) {
                return true;
            }
        }

        return false;
    }
}