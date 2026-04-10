package DTO;

public class SettingsDTO {

    private Boolean darkMode;
    private Boolean highContrast;
    private Boolean shareLocation;
    private Boolean studyReminderEnabled;
    private Integer studyReminderHour;
    private Integer studyReminderMinute;


    public SettingsDTO(Boolean darkMode, Boolean highContrast, Boolean shareLocation, Boolean studyReminderEnabled,Integer studyReminderHour, Integer studyReminderMinute){
        this.darkMode = darkMode;
        this.shareLocation = shareLocation;
        this.highContrast = highContrast;
        this.studyReminderEnabled =studyReminderEnabled;
        this.studyReminderHour =studyReminderHour;
        this.studyReminderMinute = studyReminderMinute;
    }

    public Boolean getStudyReminderEnabled() {
        return studyReminderEnabled;
    }

    public Integer getStudyReminderHour() {
        return studyReminderHour;
    }

    public Integer getStudyReminderMinute() {
        return studyReminderMinute;
    }

    public void setStudyReminderHour(Integer studyReminderHour) {
        this.studyReminderHour = studyReminderHour;
    }

    public void setStudyReminderMinute(Integer studyReminderMinute) {
        this.studyReminderMinute = studyReminderMinute;
    }

    public void setStudyReminderEnabled(Boolean studyReminderEnabled) {
        this.studyReminderEnabled = studyReminderEnabled;
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
