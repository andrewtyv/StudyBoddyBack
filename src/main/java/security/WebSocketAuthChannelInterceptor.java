//package security;
//
//import org.springframework.messaging.Message;
//import org.springframework.messaging.MessageChannel;
//import org.springframework.messaging.simp.stomp.StompCommand;
//import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
//import org.springframework.messaging.support.ChannelInterceptor;
//import org.springframework.messaging.support.MessageHeaderAccessor;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//
//@Component
//public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {
//
//    private final JwtUtil jwtUtil;
//
//    public WebSocketAuthChannelInterceptor(JwtUtil jwtUtil) {
//        this.jwtUtil = jwtUtil;
//    }
//
//    @Override
//    public Message<?> preSend(Message<?> message, MessageChannel channel) {
//        StompHeaderAccessor accessor =
//                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
//
//        if (accessor == null) {
//            return message;
//        }
//
//        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
//            String authHeader = accessor.getFirstNativeHeader("Authorization");
//
//            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
//                return message;
//            }
//
//            String token = authHeader.substring(7).trim();
//
//            if (!jwtUtil.isTokenValid(token)) {
//                return message;
//            }
//
//            String username = jwtUtil.extractUsername(token);
//
//            if (username != null && !username.isBlank()) {
//                var auth = new UsernamePasswordAuthenticationToken(
//                        username,
//                        null,
//                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
//                );
//                accessor.setUser(auth);
//            }
//        }
//
//        return message;
//    }
//}