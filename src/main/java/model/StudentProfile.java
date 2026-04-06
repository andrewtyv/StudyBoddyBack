package model;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
public class StudentProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(nullable = false)
    private String school;

    @Column(nullable = false)
    private String faculty;

    @ElementCollection(targetClass = Subject.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "student_subjects", joinColumns = @JoinColumn(name = "student_profile_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "subject", nullable = false)
    private Set<Subject> subjects = new HashSet<>();


    public StudentProfile (User user, String school, String faculty, Set<Subject> subjects){
        this.faculty=faculty;
        this.subjects=subjects;
        this.school=school;
        this.user=user;
    }
    public StudentProfile(){}


    public String getSchool() {
        return school;
    }

    public String getFaculty() {
        return faculty;
    }

    public Set<Subject> getSubjects() {
        return subjects;
    }

    public void setSchool(String school) {
        this.school = school;
    }

    public void setSubjects(Set<Subject> subjects) {
        this.subjects = subjects;
    }

    public void setFaculty(String faculty) {
        this.faculty = faculty;
    }

    public void setUser(User user) {
        this.user = user;
    }

}
