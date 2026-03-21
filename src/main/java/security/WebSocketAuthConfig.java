package security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
public class WebSocketAuthConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtil jwtService;

    @Autowired
    public WebSocketAuthConfig(JwtUtil jwtUtil) {
        this.jwtService = jwtUtil;
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor == null) {
                    return message;
                }

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    System.out.println("STOMP Authorization = " + authHeader);

                    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                        return message;
                    }

                    String token = authHeader.substring(7);

                    try {
                        String username = jwtService.extractUsername(token);

                        if (username != null && jwtService.isTokenValid(token)) {
                            accessor.setUser(new StompPrincipal(username));
                            System.out.println("WebSocket authenticated as " + username);
                        } else {
                            System.out.println("Invalid STOMP token");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                return message;
            }
        });
    }
}