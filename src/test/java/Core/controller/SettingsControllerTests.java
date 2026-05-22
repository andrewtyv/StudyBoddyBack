package Core.controller;

import DTO.ApiResponseWrapper;
import DTO.SettingsDTO;
import DTO.StudentProfileDTO;
import DTO.UserDTO;
import controllers.SettingsController;
import model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import repos.FriendshipRepo;
import repos.StudentProfileRepo;
import repos.UserBlockRepo;
import repos.UserRepo;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SettingsControllerTests {

    @InjectMocks
    private SettingsController settingsController;

    @Mock
    private UserRepo userRepo;

    @Mock
    private StudentProfileRepo studentProfileRepo;

    @Mock
    private UserBlockRepo userBlockRepo;

    @Mock
    private FriendshipRepo friendshipRepo;

    @TempDir
    Path tempDir;

    private final Principal principal = () -> "andrew";


    private User user(Number id, String username, UserRole role) {
        User user = new User();

        setIdSafely(user, id);

        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setRole(role);

        return user;
    }

    private void setIdSafely(User user, Number id) {
        try {
            Field idField = User.class.getDeclaredField("id");
            Class<?> idType = idField.getType();

            Object convertedId;

            if (idType.equals(Long.class) || idType.equals(long.class)) {
                convertedId = id.longValue();
            } else if (idType.equals(Integer.class) || idType.equals(int.class)) {
                convertedId = id.intValue();
            } else {
                convertedId = id;
            }

            ReflectionTestUtils.setField(user, "id", convertedId);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("User class does not have field 'id'", e);
        }
    }

    private Set<Subject> subjects() {
        return Set.of(Subject.values()[0]);
    }

    private StudentProfile profile(User user) {
        StudentProfile profile = new StudentProfile();
        profile.setUser(user);
        profile.setSchool("STU");
        profile.setFaculty("FIIT");
        profile.setSubjects(subjects());
        return profile;
    }

    private UserDTO roleBody(UserRole role) {
        UserDTO dto = mock(UserDTO.class);
        lenient().when(dto.getRole()).thenReturn(role);
        return dto;
    }

    private StudentProfileDTO cardBody(String school, String faculty, Set<Subject> subjects) {
        StudentProfileDTO dto = mock(StudentProfileDTO.class);
        lenient().when(dto.getSchool()).thenReturn(school);
        lenient().when(dto.getFaculty()).thenReturn(faculty);
        lenient().when(dto.getSubjects()).thenReturn(subjects);
        return dto;
    }

    private SettingsDTO settingsBody(
            Boolean darkMode,
            Boolean highContrast,
            Boolean shareLocation,
            Boolean studyReminderEnabled,
            Integer studyReminderHour,
            Integer studyReminderMinute,
            Boolean pushNotifications
    ) {
        SettingsDTO dto = mock(SettingsDTO.class);

        lenient().when(dto.getDarkMode()).thenReturn(darkMode);
        lenient().when(dto.getHighContrast()).thenReturn(highContrast);
        lenient().when(dto.getShareLocation()).thenReturn(shareLocation);
        lenient().when(dto.getStudyReminderEnabled()).thenReturn(studyReminderEnabled);
        lenient().when(dto.getStudyReminderHour()).thenReturn(studyReminderHour);
        lenient().when(dto.getStudyReminderMinute()).thenReturn(studyReminderMinute);
        lenient().when(dto.getPushNotifications()).thenReturn(pushNotifications);

        return dto;
    }

    private void assertOk(ApiResponseWrapper<?> response) {
        assertEquals(true, ReflectionTestUtils.getField(response, "success"));
    }

    private void assertError(ApiResponseWrapper<?> response, String expectedMessage) {
        assertEquals(false, ReflectionTestUtils.getField(response, "success"));
        assertEquals(expectedMessage, ReflectionTestUtils.getField(response, "message"));
        assertNull(ReflectionTestUtils.getField(response, "data"));
    }

    @SuppressWarnings("unchecked")
    private <T> T data(ApiResponseWrapper<T> response) {
        return (T) ReflectionTestUtils.getField(response, "data");
    }



    @Test
    void getMe_success() {
        User me = user(1, "andrew", UserRole.STUDENT);

        when(userRepo.findByUsername("andrew")).thenReturn(me);

        ApiResponseWrapper<UserDTO> response = settingsController.getMe(principal);

        assertOk(response);

        UserDTO dto = data(response);
        assertEquals("andrew", dto.getUsername());
        assertEquals(UserRole.STUDENT, dto.getRole());
    }

    @Test
    void getMe_userNotFound_returnsError() {
        when(userRepo.findByUsername("andrew")).thenReturn(null);

        ApiResponseWrapper<UserDTO> response = settingsController.getMe(principal);

        assertError(response, "user not found");
    }

    @Test
    void updateMyRole_success() {
        User me = user(1, "andrew", UserRole.STUDENT);

        when(userRepo.findByUsername("andrew")).thenReturn(me);

        ApiResponseWrapper<UserDTO> response = settingsController.updateMyRole(
                principal,
                roleBody(UserRole.TEACHER)
        );

        assertOk(response);
        assertEquals(UserRole.TEACHER, me.getRole());

        UserDTO dto = data(response);
        assertEquals(UserRole.TEACHER, dto.getRole());

        verify(userRepo).save(me);
    }

    @Test
    void updateMyRole_userNotFound_returnsError() {
        when(userRepo.findByUsername("andrew")).thenReturn(null);

        ApiResponseWrapper<UserDTO> response = settingsController.updateMyRole(
                principal,
                roleBody(UserRole.TEACHER)
        );

        assertError(response, "user not found");
        verify(userRepo, never()).save(any());
    }

    @Test
    void updateMyRole_roleNull_returnsError() {
        User me = user(1, "andrew", UserRole.STUDENT);

        when(userRepo.findByUsername("andrew")).thenReturn(me);

        ApiResponseWrapper<UserDTO> response = settingsController.updateMyRole(
                principal,
                roleBody(null)
        );

        assertError(response, "role is required");
        verify(userRepo, never()).save(any());
    }


    @Test
    void updateMyCard_success_createsNewProfileWhenMissing() {
        User me = user(1, "andrew", UserRole.STUDENT);

        when(userRepo.findByUsername("andrew")).thenReturn(me);
        when(studentProfileRepo.findByUser(me)).thenReturn(null);

        ApiResponseWrapper<String> response = settingsController.updateMyCard(
                principal,
                cardBody("STU", "FIIT", subjects())
        );

        assertOk(response);
        assertEquals("updated successfully", data(response));

        ArgumentCaptor<StudentProfile> captor = ArgumentCaptor.forClass(StudentProfile.class);
        verify(studentProfileRepo).save(captor.capture());

        StudentProfile saved = captor.getValue();
        assertEquals(me, saved.getUser());
        assertEquals("STU", saved.getSchool());
        assertEquals("FIIT", saved.getFaculty());
        assertEquals(subjects(), saved.getSubjects());
    }

    @Test
    void updateMyCard_success_updatesExistingProfile() {
        User me = user(1, "andrew", UserRole.STUDENT);
        StudentProfile existing = profile(me);

        when(userRepo.findByUsername("andrew")).thenReturn(me);
        when(studentProfileRepo.findByUser(me)).thenReturn(existing);

        ApiResponseWrapper<String> response = settingsController.updateMyCard(
                principal,
                cardBody("UK", "Math", subjects())
        );

        assertOk(response);
        assertEquals("updated successfully", data(response));
        assertEquals("UK", existing.getSchool());
        assertEquals("Math", existing.getFaculty());

        verify(studentProfileRepo).save(existing);
    }

    @Test
    void updateMyCard_userNotFound_returnsError() {
        when(userRepo.findByUsername("andrew")).thenReturn(null);

        ApiResponseWrapper<String> response = settingsController.updateMyCard(
                principal,
                cardBody("STU", "FIIT", subjects())
        );

        assertError(response, "user not found");
        verifyNoInteractions(studentProfileRepo);
    }

    @Test
    void updateMyCard_notStudent_returnsError() {
        User me = user(1, "andrew", UserRole.TEACHER);

        when(userRepo.findByUsername("andrew")).thenReturn(me);

        ApiResponseWrapper<String> response = settingsController.updateMyCard(
                principal,
                cardBody("STU", "FIIT", subjects())
        );

        assertError(response, "only students can have a student profile");
        verifyNoInteractions(studentProfileRepo);
    }

    @Test
    void updateMyCard_schoolNull_returnsError() {
        User me = user(1, "andrew", UserRole.STUDENT);

        when(userRepo.findByUsername("andrew")).thenReturn(me);

        ApiResponseWrapper<String> response = settingsController.updateMyCard(
                principal,
                cardBody(null, "FIIT", subjects())
        );

        assertError(response, "school is required");
        verify(studentProfileRepo, never()).save(any());
    }

    @Test
    void updateMyCard_schoolBlank_returnsError() {
        User me = user(1, "andrew", UserRole.STUDENT);

        when(userRepo.findByUsername("andrew")).thenReturn(me);

        ApiResponseWrapper<String> response = settingsController.updateMyCard(
                principal,
                cardBody("   ", "FIIT", subjects())
        );

        assertError(response, "school is required");
        verify(studentProfileRepo, never()).save(any());
    }

    @Test
    void updateMyCard_facultyNull_returnsError() {
        User me = user(1, "andrew", UserRole.STUDENT);

        when(userRepo.findByUsername("andrew")).thenReturn(me);

        ApiResponseWrapper<String> response = settingsController.updateMyCard(
                principal,
                cardBody("STU", null, subjects())
        );

        assertError(response, "faculty is required");
        verify(studentProfileRepo, never()).save(any());
    }

    @Test
    void updateMyCard_facultyBlank_returnsError() {
        User me = user(1, "andrew", UserRole.STUDENT);

        when(userRepo.findByUsername("andrew")).thenReturn(me);

        ApiResponseWrapper<String> response = settingsController.updateMyCard(
                principal,
                cardBody("STU", " ", subjects())
        );

        assertError(response, "faculty is required");
        verify(studentProfileRepo, never()).save(any());
    }

    @Test
    void updateMyCard_subjectsNull_returnsError() {
        User me = user(1, "andrew", UserRole.STUDENT);

        when(userRepo.findByUsername("andrew")).thenReturn(me);

        ApiResponseWrapper<String> response = settingsController.updateMyCard(
                principal,
                cardBody("STU", "FIIT", null)
        );

        assertError(response, "at least one subject is required");
        verify(studentProfileRepo, never()).save(any());
    }

    @Test
    void updateMyCard_subjectsEmpty_returnsError() {
        User me = user(1, "andrew", UserRole.STUDENT);

        when(userRepo.findByUsername("andrew")).thenReturn(me);

        ApiResponseWrapper<String> response = settingsController.updateMyCard(
                principal,
                cardBody("STU", "FIIT", Set.of())
        );

        assertError(response, "at least one subject is required");
        verify(studentProfileRepo, never()).save(any());
    }



    @Test
    void getMyCard_success() {
        User me = user(1, "andrew", UserRole.STUDENT);
        StudentProfile profile = profile(me);

        when(userRepo.findByUsername("andrew")).thenReturn(me);
        when(studentProfileRepo.findByUser(me)).thenReturn(profile);

        ApiResponseWrapper<StudentProfileDTO> response = settingsController.getMyCard(principal);

        assertOk(response);

        StudentProfileDTO dto = data(response);
        assertEquals("STU", dto.getSchool());
        assertEquals("FIIT", dto.getFaculty());
        assertEquals(subjects(), dto.getSubjects());
    }

    @Test
    void getMyCard_userNotFound_returnsError() {
        when(userRepo.findByUsername("andrew")).thenReturn(null);

        ApiResponseWrapper<StudentProfileDTO> response = settingsController.getMyCard(principal);

        assertError(response, "user not found");
    }

    @Test
    void getMyCard_profileNotFound_returnsError() {
        User me = user(1, "andrew", UserRole.STUDENT);

        when(userRepo.findByUsername("andrew")).thenReturn(me);
        when(studentProfileRepo.findByUser(me)).thenReturn(null);

        ApiResponseWrapper<StudentProfileDTO> response = settingsController.getMyCard(principal);

        assertError(response, "profile not found");
    }


    @Test
    void uploadAvatar_success_usesOriginalExtension() throws Exception {
        ReflectionTestUtils.setField(settingsController, "uploadDir", tempDir.toString());

        User me = user(1, "andrew", UserRole.STUDENT);

        when(userRepo.findByUsername("andrew")).thenReturn(me);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.custom",
                "image/png",
                "image-data".getBytes()
        );

        ApiResponseWrapper<String> response = settingsController.uploadAvatar(principal, file);

        assertOk(response);

        String url = data(response);
        assertTrue(url.startsWith("/uploads/user-avatar/user_1/"));
        assertTrue(url.endsWith(".custom"));
        assertEquals(url, me.getPhotoUrl());

        String storedName = url.substring(url.lastIndexOf("/") + 1);
        Path savedFile = tempDir.resolve("user-avatar").resolve("user_1").resolve(storedName);

        assertTrue(Files.exists(savedFile));

        verify(userRepo).save(me);
    }

    @Test
    void uploadAvatar_success_addsPngExtensionWhenOriginalHasNoExtension() {
        ReflectionTestUtils.setField(settingsController, "uploadDir", tempDir.toString());

        User me = user(1, "andrew", UserRole.STUDENT);

        when(userRepo.findByUsername("andrew")).thenReturn(me);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar",
                "image/png",
                "image-data".getBytes()
        );

        ApiResponseWrapper<String> response = settingsController.uploadAvatar(principal, file);

        assertOk(response);
        assertTrue(data(response).endsWith(".png"));
    }

    @Test
    void uploadAvatar_success_addsWebpExtensionWhenOriginalHasNoExtension() {
        ReflectionTestUtils.setField(settingsController, "uploadDir", tempDir.toString());

        User me = user(1, "andrew", UserRole.STUDENT);

        when(userRepo.findByUsername("andrew")).thenReturn(me);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar",
                "image/webp",
                "image-data".getBytes()
        );

        ApiResponseWrapper<String> response = settingsController.uploadAvatar(principal, file);

        assertOk(response);
        assertTrue(data(response).endsWith(".webp"));
    }

    @Test
    void uploadAvatar_success_addsJpgExtensionByDefaultWhenOriginalHasNoExtension() {
        ReflectionTestUtils.setField(settingsController, "uploadDir", tempDir.toString());

        User me = user(1, "andrew", UserRole.STUDENT);

        when(userRepo.findByUsername("andrew")).thenReturn(me);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar",
                "image/jpeg",
                "image-data".getBytes()
        );

        ApiResponseWrapper<String> response = settingsController.uploadAvatar(principal, file);

        assertOk(response);
        assertTrue(data(response).endsWith(".jpg"));
    }

    @Test
    void uploadAvatar_success_originalFilenameNullDefaultsToJpg() {
        ReflectionTestUtils.setField(settingsController, "uploadDir", tempDir.toString());

        User me = user(1, "andrew", UserRole.STUDENT);

        when(userRepo.findByUsername("andrew")).thenReturn(me);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                null,
                "image/jpeg",
                "image-data".getBytes()
        );

        ApiResponseWrapper<String> response = settingsController.uploadAvatar(principal, file);

        assertOk(response);
        assertTrue(data(response).endsWith(".jpg"));
    }

    @Test
    void uploadAvatar_userNotFound_returnsError() {
        when(userRepo.findByUsername("andrew")).thenReturn(null);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                "image-data".getBytes()
        );

        ApiResponseWrapper<String> response = settingsController.uploadAvatar(principal, file);

        assertError(response, "user not found");
        verify(userRepo, never()).save(any());
    }

    @Test
    void uploadAvatar_nullFile_returnsError() {
        User me = user(1, "andrew", UserRole.STUDENT);

        when(userRepo.findByUsername("andrew")).thenReturn(me);

        ApiResponseWrapper<String> response = settingsController.uploadAvatar(principal, null);

        assertError(response, "file is empty");
        verify(userRepo, never()).save(any());
    }

    @Test
    void uploadAvatar_emptyFile_returnsError() {
        User me = user(1, "andrew", UserRole.STUDENT);

        when(userRepo.findByUsername("andrew")).thenReturn(me);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[0]
        );

        ApiResponseWrapper<String> response = settingsController.uploadAvatar(principal, file);

        assertError(response, "file is empty");
        verify(userRepo, never()).save(any());
    }

    @Test
    void uploadAvatar_nullContentType_returnsError() {
        User me = user(1, "andrew", UserRole.STUDENT);

        when(userRepo.findByUsername("andrew")).thenReturn(me);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                null,
                "image-data".getBytes()
        );

        ApiResponseWrapper<String> response = settingsController.uploadAvatar(principal, file);

        assertError(response, "only jpg, jpeg, png, webp are allowed");
        verify(userRepo, never()).save(any());
    }

    @Test
    void uploadAvatar_wrongContentType_returnsError() {
        User me = user(1, "andrew", UserRole.STUDENT);

        when(userRepo.findByUsername("andrew")).thenReturn(me);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.gif",
                "image/gif",
                "image-data".getBytes()
        );

        ApiResponseWrapper<String> response = settingsController.uploadAvatar(principal, file);

        assertError(response, "only jpg, jpeg, png, webp are allowed");
        verify(userRepo, never()).save(any());
    }

    @Test
    void uploadAvatar_ioException_returnsUploadFailed() throws Exception {
        ReflectionTestUtils.setField(settingsController, "uploadDir", tempDir.toString());

        User me = user(1, "andrew", UserRole.STUDENT);

        when(userRepo.findByUsername("andrew")).thenReturn(me);

        MultipartFile file = mock(MultipartFile.class);

        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getOriginalFilename()).thenReturn("avatar.png");
        when(file.getInputStream()).thenThrow(new IOException("boom"));

        ApiResponseWrapper<String> response = settingsController.uploadAvatar(principal, file);

        assertError(response, "upload failed");
        verify(userRepo, never()).save(any());
    }



    @Test
    void getAvatar_success() {
        User me = user(1, "andrew", UserRole.STUDENT);
        me.setPhotoUrl("/uploads/avatar.png");

        when(userRepo.findByUsername("andrew")).thenReturn(me);

        ApiResponseWrapper<String> response = settingsController.getAvatar(principal);

        assertOk(response);
        assertEquals("/uploads/avatar.png", data(response));
    }

    @Test
    void getAvatar_userNotFound_returnsError() {
        when(userRepo.findByUsername("andrew")).thenReturn(null);

        ApiResponseWrapper<String> response = settingsController.getAvatar(principal);

        assertError(response, "user doesnt exist");
    }



    @Test
    void setSettings_success_updatesAllFieldsAndEnablesReminder() {
        User me = user(1, "andrew", UserRole.STUDENT);

        when(userRepo.findByUsername("andrew")).thenReturn(me);

        SettingsDTO dto = settingsBody(
                true,
                true,
                true,
                true,
                18,
                30,
                true
        );

        ApiResponseWrapper<String> response = settingsController.setSettings(principal, dto);

        assertOk(response);
        assertEquals("data set", data(response));

        assertTrue(me.getDarkMode());
        assertTrue(me.getHighContrast());
        assertTrue(me.getShareLocation());
        assertTrue(me.getStudyReminderEnabled());
        assertEquals(18, me.getStudyReminderHour());
        assertEquals(30, me.getStudyReminderMinute());
        assertTrue(me.getPushNotificationsEnabled());

        verify(userRepo).save(me);
    }

    @Test
    void setSettings_success_disablesReminder() {
        User me = user(1, "andrew", UserRole.STUDENT);
        me.setStudyReminderEnabled(true);

        when(userRepo.findByUsername("andrew")).thenReturn(me);

        SettingsDTO dto = settingsBody(
                null,
                null,
                null,
                false,
                null,
                null,
                null
        );

        ApiResponseWrapper<String> response = settingsController.setSettings(principal, dto);

        assertOk(response);
        assertFalse(me.getStudyReminderEnabled());

        verify(userRepo).save(me);
    }

    @Test
    void setSettings_userNotFound_returnsError() {
        when(userRepo.findByUsername("andrew")).thenReturn(null);

        SettingsDTO dto = settingsBody(true, null, null, null, null, null, null);

        ApiResponseWrapper<String> response = settingsController.setSettings(principal, dto);

        assertError(response, "this user doesn't exist");
        verify(userRepo, never()).save(any());
    }

    @Test
    void setSettings_reminderEnabledWithoutHour_returnsError() {
        User me = user(1, "andrew", UserRole.STUDENT);

        when(userRepo.findByUsername("andrew")).thenReturn(me);

        SettingsDTO dto = settingsBody(null, null, null, true, null, 30, null);

        ApiResponseWrapper<String> response = settingsController.setSettings(principal, dto);

        assertError(response, "study reminder hour and minute are required");
        verify(userRepo, never()).save(any());
    }

    @Test
    void setSettings_reminderEnabledWithoutMinute_returnsError() {
        User me = user(1, "andrew", UserRole.STUDENT);

        when(userRepo.findByUsername("andrew")).thenReturn(me);

        SettingsDTO dto = settingsBody(null, null, null, true, 18, null, null);

        ApiResponseWrapper<String> response = settingsController.setSettings(principal, dto);

        assertError(response, "study reminder hour and minute are required");
        verify(userRepo, never()).save(any());
    }

    @Test
    void setSettings_hourLessThanZero_returnsError() {
        User me = user(1, "andrew", UserRole.STUDENT);

        when(userRepo.findByUsername("andrew")).thenReturn(me);

        SettingsDTO dto = settingsBody(null, null, null, true, -1, 30, null);

        ApiResponseWrapper<String> response = settingsController.setSettings(principal, dto);

        assertError(response, "study reminder hour must be between 0 and 23");
        verify(userRepo, never()).save(any());
    }

    @Test
    void setSettings_hourGreaterThan23_returnsError() {
        User me = user(1, "andrew", UserRole.STUDENT);

        when(userRepo.findByUsername("andrew")).thenReturn(me);

        SettingsDTO dto = settingsBody(null, null, null, true, 24, 30, null);

        ApiResponseWrapper<String> response = settingsController.setSettings(principal, dto);

        assertError(response, "study reminder hour must be between 0 and 23");
        verify(userRepo, never()).save(any());
    }

    @Test
    void setSettings_minuteLessThanZero_returnsError() {
        User me = user(1, "andrew", UserRole.STUDENT);

        when(userRepo.findByUsername("andrew")).thenReturn(me);

        SettingsDTO dto = settingsBody(null, null, null, true, 18, -1, null);

        ApiResponseWrapper<String> response = settingsController.setSettings(principal, dto);

        assertError(response, "study reminder minute must be between 0 and 59");
        verify(userRepo, never()).save(any());
    }

    @Test
    void setSettings_minuteGreaterThan59_returnsError() {
        User me = user(1, "andrew", UserRole.STUDENT);

        when(userRepo.findByUsername("andrew")).thenReturn(me);

        SettingsDTO dto = settingsBody(null, null, null, true, 18, 60, null);

        ApiResponseWrapper<String> response = settingsController.setSettings(principal, dto);

        assertError(response, "study reminder minute must be between 0 and 59");
        verify(userRepo, never()).save(any());
    }

    @Test
    void setSettings_allNulls_stillSavesBecauseControllerHasNoAllNullValidation() {
        User me = user(1, "andrew", UserRole.STUDENT);

        when(userRepo.findByUsername("andrew")).thenReturn(me);

        SettingsDTO dto = settingsBody(null, null, null, null, null, null, null);

        ApiResponseWrapper<String> response = settingsController.setSettings(principal, dto);

        assertOk(response);
        assertEquals("data set", data(response));

        verify(userRepo).save(me);
    }



    @Test
    void getSettings_success() {
        User me = user(1, "andrew", UserRole.STUDENT);

        me.setDarkMode(true);
        me.setHighContrast(false);
        me.setShareLocation(true);
        me.setStudyReminderEnabled(true);
        me.setStudyReminderHour(18);
        me.setStudyReminderMinute(30);
        me.setPushNotificationsEnabled(true);

        when(userRepo.findByUsername("andrew")).thenReturn(me);

        ApiResponseWrapper<SettingsDTO> response = settingsController.getSettings(principal);

        assertOk(response);

        SettingsDTO dto = data(response);

        assertEquals(true, dto.getDarkMode());
        assertEquals(false, dto.getHighContrast());
        assertEquals(true, dto.getShareLocation());
        assertEquals(true, dto.getStudyReminderEnabled());
        assertEquals(18, dto.getStudyReminderHour());
        assertEquals(30, dto.getStudyReminderMinute());
        assertEquals(true, dto.getPushNotifications());
    }

    @Test
    void getSettings_userNotFound_returnsError() {
        when(userRepo.findByUsername("andrew")).thenReturn(null);

        ApiResponseWrapper<SettingsDTO> response = settingsController.getSettings(principal);

        assertError(response, "this user doesn't exist");
    }



    @Test
    void savePushToken_success_trimsToken() {
        User me = user(1, "andrew", UserRole.STUDENT);

        when(userRepo.findByUsername("andrew")).thenReturn(me);

        ApiResponseWrapper<String> response = settingsController.savePushToken(
                principal,
                Map.of("expoPushToken", "  ExpoPushToken[abc]  ")
        );

        assertOk(response);
        assertEquals("push token saved", data(response));
        assertEquals("ExpoPushToken[abc]", me.getExpoPushToken());

        verify(userRepo).save(me);
    }

    @Test
    void savePushToken_userNotFound_returnsError() {
        when(userRepo.findByUsername("andrew")).thenReturn(null);

        ApiResponseWrapper<String> response = settingsController.savePushToken(
                principal,
                Map.of("expoPushToken", "ExpoPushToken[abc]")
        );

        assertError(response, "user not found");
        verify(userRepo, never()).save(any());
    }

    @Test
    void savePushToken_bodyNull_returnsError() {
        User me = user(1, "andrew", UserRole.STUDENT);

        when(userRepo.findByUsername("andrew")).thenReturn(me);

        ApiResponseWrapper<String> response = settingsController.savePushToken(principal, null);

        assertError(response, "expoPushToken is required");
        verify(userRepo, never()).save(any());
    }

    @Test
    void savePushToken_tokenMissing_returnsError() {
        User me = user(1, "andrew", UserRole.STUDENT);

        when(userRepo.findByUsername("andrew")).thenReturn(me);

        ApiResponseWrapper<String> response = settingsController.savePushToken(
                principal,
                Map.of()
        );

        assertError(response, "expoPushToken is required");
        verify(userRepo, never()).save(any());
    }

    @Test
    void savePushToken_tokenBlank_returnsError() {
        User me = user(1, "andrew", UserRole.STUDENT);

        when(userRepo.findByUsername("andrew")).thenReturn(me);

        ApiResponseWrapper<String> response = settingsController.savePushToken(
                principal,
                Map.of("expoPushToken", "   ")
        );

        assertError(response, "expoPushToken is required");
        verify(userRepo, never()).save(any());
    }



    @Test
    void clearPushToken_success() {
        User me = user(1, "andrew", UserRole.STUDENT);
        me.setExpoPushToken("ExpoPushToken[abc]");

        when(userRepo.findByUsername("andrew")).thenReturn(me);

        ApiResponseWrapper<String> response = settingsController.clearPushToken(principal);

        assertOk(response);
        assertEquals("push token cleared", data(response));
        assertNull(me.getExpoPushToken());

        verify(userRepo).save(me);
    }

    @Test
    void clearPushToken_userNotFound_returnsError() {
        when(userRepo.findByUsername("andrew")).thenReturn(null);

        ApiResponseWrapper<String> response = settingsController.clearPushToken(principal);

        assertError(response, "user not found");
        verify(userRepo, never()).save(any());
    }


    @Test
    void blockUser_success_withoutExistingFriendship() {
        User me = user(1, "andrew", UserRole.STUDENT);
        User enemy = user(2, "enemy", UserRole.STUDENT);

        when(userRepo.findByUsername("andrew")).thenReturn(me);
        when(userRepo.findByUsername("enemy")).thenReturn(enemy);
        when(userBlockRepo.existsByBlockedAndBlocker(enemy, me)).thenReturn(false);
        when(userBlockRepo.existsByBlockedAndBlocker(me, enemy)).thenReturn(false);

        ApiResponseWrapper<String> response = settingsController.blockUser(
                principal,
                Map.of("username", "enemy")
        );

        assertOk(response);
        assertEquals("blocked successfully", data(response));

        verify(userBlockRepo).save(any(UserBlock.class));
        verify(friendshipRepo, never()).delete(any());
    }

    @Test
    void blockUser_success_deletesExistingFriendship() {
        User me = user(1, "andrew", UserRole.STUDENT);
        User enemy = user(2, "enemy", UserRole.STUDENT);
        Friendship friendship = mock(Friendship.class);

        when(userRepo.findByUsername("andrew")).thenReturn(me);
        when(userRepo.findByUsername("enemy")).thenReturn(enemy);
        when(userBlockRepo.existsByBlockedAndBlocker(enemy, me)).thenReturn(false);
        when(userBlockRepo.existsByBlockedAndBlocker(me, enemy)).thenReturn(false);
        when(friendshipRepo.findByRequester_IdAndAddressee_IdOrRequester_IdAndAddressee_Id(
                me.getId(),
                enemy.getId(),
                enemy.getId(),
                me.getId()
        )).thenReturn(friendship);

        ApiResponseWrapper<String> response = settingsController.blockUser(
                principal,
                Map.of("username", "enemy")
        );

        assertOk(response);
        assertEquals("blocked successfully", data(response));

        verify(userBlockRepo).save(any(UserBlock.class));
        verify(friendshipRepo).delete(friendship);
    }

    @Test
    void blockUser_meNotFound_returnsError() {
        User enemy = user(2, "enemy", UserRole.STUDENT);

        when(userRepo.findByUsername("andrew")).thenReturn(null);
        when(userRepo.findByUsername("enemy")).thenReturn(enemy);

        ApiResponseWrapper<String> response = settingsController.blockUser(
                principal,
                Map.of("username", "enemy")
        );

        assertError(response, "user doesn't exist");
        verify(userBlockRepo, never()).save(any());
    }

    @Test
    void blockUser_enemyNotFound_returnsError() {
        User me = user(1, "andrew", UserRole.STUDENT);

        when(userRepo.findByUsername("andrew")).thenReturn(me);
        when(userRepo.findByUsername("enemy")).thenReturn(null);

        ApiResponseWrapper<String> response = settingsController.blockUser(
                principal,
                Map.of("username", "enemy")
        );

        assertError(response, "user doesn't exist");
        verify(userBlockRepo, never()).save(any());
    }

    @Test
    void blockUser_selfBlock_returnsError() {
        User me = user(1, "andrew", UserRole.STUDENT);

        when(userRepo.findByUsername("andrew")).thenReturn(me);
        when(userRepo.findByUsername("enemy")).thenReturn(me);

        ApiResponseWrapper<String> response = settingsController.blockUser(
                principal,
                Map.of("username", "enemy")
        );

        assertError(response, "you cannot block yourself");
        verify(userBlockRepo, never()).save(any());
    }

    @Test
    void blockUser_alreadyBlocked_returnsError() {
        User me = user(1, "andrew", UserRole.STUDENT);
        User enemy = user(2, "enemy", UserRole.STUDENT);

        when(userRepo.findByUsername("andrew")).thenReturn(me);
        when(userRepo.findByUsername("enemy")).thenReturn(enemy);
        when(userBlockRepo.existsByBlockedAndBlocker(enemy, me)).thenReturn(true);

        ApiResponseWrapper<String> response = settingsController.blockUser(
                principal,
                Map.of("username", "enemy")
        );

        assertError(response, "you already blocked this user");
        verify(userBlockRepo, never()).save(any());
    }

    @Test
    void blockUser_enemyBlockedMe_returnsError() {
        User me = user(1, "andrew", UserRole.STUDENT);
        User enemy = user(2, "enemy", UserRole.STUDENT);

        when(userRepo.findByUsername("andrew")).thenReturn(me);
        when(userRepo.findByUsername("enemy")).thenReturn(enemy);
        when(userBlockRepo.existsByBlockedAndBlocker(enemy, me)).thenReturn(false);
        when(userBlockRepo.existsByBlockedAndBlocker(me, enemy)).thenReturn(true);

        ApiResponseWrapper<String> response = settingsController.blockUser(
                principal,
                Map.of("username", "enemy")
        );

        assertError(response, "this user has blocked you");
        verify(userBlockRepo, never()).save(any());
    }



    @Test
    void getBlocked_success() {
        User me = user(1, "andrew", UserRole.STUDENT);
        User enemy = user(2, "enemy", UserRole.STUDENT);

        UserBlock block = mock(UserBlock.class);

        when(block.getBlocked()).thenReturn(enemy);
        when(userRepo.findByUsername("andrew")).thenReturn(me);
        when(userBlockRepo.findByBlocker(me)).thenReturn(List.of(block));

        ApiResponseWrapper<List<UserDTO>> response = settingsController.getBlocked(principal);

        assertOk(response);

        List<UserDTO> blockedUsers = data(response);

        assertEquals(1, blockedUsers.size());
        assertEquals("enemy", blockedUsers.get(0).getUsername());
    }

    @Test
    void getBlocked_userNotFound_returnsError() {
        when(userRepo.findByUsername("andrew")).thenReturn(null);

        ApiResponseWrapper<List<UserDTO>> response = settingsController.getBlocked(principal);

        assertError(response, "u dont exist");
    }



    @Test
    void unblockUser_success() {
        User me = user(1, "andrew", UserRole.STUDENT);
        User enemy = user(2, "enemy", UserRole.STUDENT);
        UserBlock block = mock(UserBlock.class);

        when(userRepo.findByUsername("andrew")).thenReturn(me);
        when(userRepo.findByUsername("enemy")).thenReturn(enemy);
        when(userBlockRepo.findByBlockedAndAndBlocker(enemy, me)).thenReturn(block);

        ApiResponseWrapper<String> response = settingsController.unblockUser(
                principal,
                Map.of("username", "enemy")
        );

        assertOk(response);
        assertEquals("unblocked succesfully", data(response));

        verify(userBlockRepo).delete(block);
    }

    @Test
    void unblockUser_meNotFound_returnsError() {
        User enemy = user(2, "enemy", UserRole.STUDENT);

        when(userRepo.findByUsername("andrew")).thenReturn(null);
        when(userRepo.findByUsername("enemy")).thenReturn(enemy);

        ApiResponseWrapper<String> response = settingsController.unblockUser(
                principal,
                Map.of("username", "enemy")
        );

        assertError(response, "dont exist");
        verify(userBlockRepo, never()).delete(any());
    }

    @Test
    void unblockUser_enemyNotFound_returnsError() {
        User me = user(1, "andrew", UserRole.STUDENT);

        when(userRepo.findByUsername("andrew")).thenReturn(me);
        when(userRepo.findByUsername("enemy")).thenReturn(null);

        ApiResponseWrapper<String> response = settingsController.unblockUser(
                principal,
                Map.of("username", "enemy")
        );

        assertError(response, "dont exist");
        verify(userBlockRepo, never()).delete(any());
    }

    @Test
    void unblockUser_blockNotFound_returnsError() {
        User me = user(1, "andrew", UserRole.STUDENT);
        User enemy = user(2, "enemy", UserRole.STUDENT);

        when(userRepo.findByUsername("andrew")).thenReturn(me);
        when(userRepo.findByUsername("enemy")).thenReturn(enemy);
        when(userBlockRepo.findByBlockedAndAndBlocker(enemy, me)).thenReturn(null);

        ApiResponseWrapper<String> response = settingsController.unblockUser(
                principal,
                Map.of("username", "enemy")
        );

        assertError(response, "u didnt block this user");
        verify(userBlockRepo, never()).delete(any());
    }


    @Test
    void getAllSubjects_success() {
        User me = user(1, "andrew", UserRole.STUDENT);

        when(userRepo.findByUsername("andrew")).thenReturn(me);

        ApiResponseWrapper<List<Subject>> response = settingsController.getAllSubjects(principal);

        assertOk(response);
        assertEquals(Arrays.asList(Subject.values()), data(response));
    }

    @Test
    void getAllSubjects_userNotFound_returnsError() {
        when(userRepo.findByUsername("andrew")).thenReturn(null);

        ApiResponseWrapper<List<Subject>> response = settingsController.getAllSubjects(principal);

        assertError(response, "user not found");
    }
}
