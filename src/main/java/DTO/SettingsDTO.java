package DTO;

public class SettingsDTO {

    private Boolean darkMode;
    private Boolean highContrast;
    private Boolean shareLocation;
    private Boolean pushNotifications;

    public SettingsDTO(Boolean darkMode, Boolean highContrast, Boolean shareLocation, Boolean pushNotifications){
        this.darkMode = darkMode;
        this.shareLocation = shareLocation;
        this.highContrast = highContrast;
        this.pushNotifications = pushNotifications;
    }

    public void setDarkMode(Boolean darkMode) {
        this.darkMode = darkMode;
    }

    public void setHighContrast(Boolean highContrast) {
        this.highContrast = highContrast;
    }

    public void setShareLocation(Boolean shareLocation) {
        this.shareLocation = shareLocation;
    }

    public void setPushNotifications(Boolean pushNotifications) {
        this.pushNotifications = pushNotifications;
    }

    public Boolean getDarkMode(){
        return this.darkMode;
    }
    public Boolean getHighContrast(){
        return this.highContrast;
    }
    public Boolean getShareLocation(){
        return this.shareLocation;
    }
    public Boolean getPushNotifications() {return this.pushNotifications;}
}
