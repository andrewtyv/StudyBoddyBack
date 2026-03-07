package DTO;

import model.MessageType;

public class MessageDTO {

    private String content;
    private MessageType messageType;
    private String senderUsername;


    public MessageDTO(){}

    public MessageDTO(String content, MessageType messageType, String senderUsername){
        this.content = content;
        this.messageType = messageType;
        this.senderUsername = senderUsername;
    }

    public String getContent() {
        return this.content;
    }

    public MessageType getMessageType() {
        return this.messageType;
    }

    public String getSenderUsername() {
        return this.senderUsername;
    }
}
