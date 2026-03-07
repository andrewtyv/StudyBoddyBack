package model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "message_recipient",
        uniqueConstraints = @UniqueConstraint(columnNames = {"message_id", "recipient_id"})
)
public class MessageRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Column(nullable = false)
    private boolean read = false;

    private Instant readAt;

    public void markRead() {
        this.read = true;
        this.readAt = Instant.now();
    }

    public MessageRecipient(User recipient, Message message){
        this.recipient =recipient;
        this.message=message;
    }
    public Message getMessage(){
        return this.message;
    }

    public MessageRecipient(){}

}