package DTO;

public class UploadPhotoResponse {
    private String fileUrl;       // наприклад /uploads/chat/uuid.jpg
    private String fileName;
    private String contentType;

    public UploadPhotoResponse() {
    }

    public UploadPhotoResponse(String fileUrl, String fileName, String contentType) {
        this.fileUrl = fileUrl;
        this.fileName = fileName;
        this.contentType = contentType;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}