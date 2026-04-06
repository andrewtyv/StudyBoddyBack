package DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import model.StudentProfile;
import model.Subject;

import java.util.HashSet;
import java.util.Set;

public class StudentProfileDTO {
    @NotBlank
    private String school;

    @NotBlank
    private String faculty;

    @NotNull
    @Size(min = 1)
    private Set<Subject> subjects;

    public StudentProfileDTO(String school, String faculty, Set<Subject> subjects){
        this.school = school;
        this.faculty = faculty;
        this.subjects = new HashSet<>(subjects);
    }

    public StudentProfileDTO(StudentProfile profile){
        this.school = profile.getSchool();
        this.faculty = profile.getFaculty();
        this.subjects = new HashSet<>(profile.getSubjects());
    }
    public StudentProfileDTO(){
        this.subjects = new HashSet<>();
    }


    public Set<Subject> getSubjects() {
        return subjects;
    }

    public String getFaculty() {
        return faculty;
    }

    public String getSchool() {
        return school;
    }

    public void setFaculty(String faculty) {
        this.faculty = faculty;
    }

    public void setSubjects(Set<Subject> subjects) {
        this.subjects = subjects;
    }

    public void setSchool(String school) {
        this.school = school;
    }

}
