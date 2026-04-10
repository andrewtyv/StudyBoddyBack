package DTO;

public class SettingsDTO {

    private Boolean darkMode;
    private Boolean highContrast;
    private Boolean shareLocation;

    public SettingsDTO(Boolean darkMode, Boolean highContrast, Boolean shareLocation){
        this.darkMode = darkMode;
        this.shareLocation = shareLocation;
        this.highContrast = highContrast;
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

    public Boolean getDarkMode(){
        return this.darkMode;
    }
    public Boolean getHighContrast(){
        return this.highContrast;
    }
    public Boolean getShareLocation(){
        return this.shareLocation;
    }
}
