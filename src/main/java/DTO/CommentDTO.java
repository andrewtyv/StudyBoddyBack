package DTO;

public class CommentDTO {
    Long id;
    private String username;
    private String content;

    public CommentDTO(Long id,String username, String content){
        this.id = id;
        this.username = username;
        this.content = content;
    }
    public String getUsername(){
        return this.username;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
