package Core.controller;

import DTO.ApiResponseWrapper;
import DTO.LocationUpdateRequestDTO;
import DTO.UserLocationDTO;
import controllers.LocationController;
import model.StudentProfile;
import model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repos.StudentProfileRepo;
import repos.UserRepo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LocationControllerTests {

    @Mock
    private UserRepo userRepo;

    @Mock
    private StudentProfileRepo studentProfileRepo;

    @InjectMocks
    private LocationController locationController;

    @Test
    void updateMyLocation_shouldReturnUnauthorized_whenPrincipalIsNull() {
        ApiResponseWrapper<String> response =
                locationController.updateMyLocation(null, locationRequest(48.1, 17.1));

        assertFalse(success(response));
        assertEquals("Unauthorized", message(response));
        verifyNoInteractions(userRepo, studentProfileRepo);
    }

    @Test
    void updateMyLocation_shouldReturnUnauthorized_whenPrincipalNameIsBlank() {
        ApiResponseWrapper<String> response =
                locationController.updateMyLocation(principal("   "), locationRequest(48.1, 17.1));

        assertFalse(success(response));
        assertEquals("Unauthorized", message(response));
        verifyNoInteractions(userRepo, studentProfileRepo);
    }

    @Test
    void updateMyLocation_shouldReturnError_whenRequestIsNull() {
        ApiResponseWrapper<String> response =
                locationController.updateMyLocation(principal("andrew"), null);

        assertFalse(success(response));
        assertEquals("Latitude and longitude are required", message(response));
        verifyNoInteractions(userRepo, studentProfileRepo);
    }

    @Test
    void updateMyLocation_shouldReturnError_whenLatitudeIsNull() {
        ApiResponseWrapper<String> response =
                locationController.updateMyLocation(principal("andrew"), locationRequest(null, 17.1));

        assertFalse(success(response));
        assertEquals("Latitude and longitude are required", message(response));
        verifyNoInteractions(userRepo, studentProfileRepo);
    }

    @Test
    void updateMyLocation_shouldReturnError_whenLongitudeIsNull() {
        ApiResponseWrapper<String> response =
                locationController.updateMyLocation(principal("andrew"), locationRequest(48.1, null));

        assertFalse(success(response));
        assertEquals("Latitude and longitude are required", message(response));
        verifyNoInteractions(userRepo, studentProfileRepo);
    }

    @Test
    void updateMyLocation_shouldReturnError_whenLatitudeIsLessThanMinus90() {
        ApiResponseWrapper<String> response =
                locationController.updateMyLocation(principal("andrew"), locationRequest(-91.0, 17.1));

        assertFalse(success(response));
        assertEquals("Latitude must be between -90 and 90", message(response));
        verifyNoInteractions(userRepo, studentProfileRepo);
    }

    @Test
    void updateMyLocation_shouldReturnError_whenLatitudeIsGreaterThan90() {
        ApiResponseWrapper<String> response =
                locationController.updateMyLocation(principal("andrew"), locationRequest(91.0, 17.1));

        assertFalse(success(response));
        assertEquals("Latitude must be between -90 and 90", message(response));
        verifyNoInteractions(userRepo, studentProfileRepo);
    }

    @Test
    void updateMyLocation_shouldReturnError_whenLongitudeIsLessThanMinus180() {
        ApiResponseWrapper<String> response =
                locationController.updateMyLocation(principal("andrew"), locationRequest(48.1, -181.0));

        assertFalse(success(response));
        assertEquals("Longitude must be between -180 and 180", message(response));
        verifyNoInteractions(userRepo, studentProfileRepo);
    }

    @Test
    void updateMyLocation_shouldReturnError_whenLongitudeIsGreaterThan180() {
        ApiResponseWrapper<String> response =
                locationController.updateMyLocation(principal("andrew"), locationRequest(48.1, 181.0));

        assertFalse(success(response));
        assertEquals("Longitude must be between -180 and 180", message(response));
        verifyNoInteractions(userRepo, studentProfileRepo);
    }

    @Test
    void updateMyLocation_shouldReturnUserNotFound_whenUserDoesNotExist() {
        when(userRepo.findByUsername("andrew")).thenReturn(null);

        ApiResponseWrapper<String> response =
                locationController.updateMyLocation(principal("andrew"), locationRequest(48.1486, 17.1077));

        assertFalse(success(response));
        assertEquals("User not found", message(response));
        verify(userRepo).findByUsername("andrew");
        verify(userRepo, never()).save(any());
        verifyNoInteractions(studentProfileRepo);
    }

    @Test
    void updateMyLocation_shouldReturnError_whenShareLocationIsDisabled() {
        User user = user(1L, "andrew", false, null, null, null);
        when(userRepo.findByUsername("andrew")).thenReturn(user);

        ApiResponseWrapper<String> response =
                locationController.updateMyLocation(principal("andrew"), locationRequest(48.1486, 17.1077));

        assertFalse(success(response));
        assertEquals("Share location disabled", message(response));
        verify(userRepo).findByUsername("andrew");
        verify(userRepo, never()).save(any());
        verifyNoInteractions(studentProfileRepo);
    }

    @Test
    void updateMyLocation_shouldUpdateLocationAndSaveUser_whenDataIsValid() {
        User user = user(1L, "andrew", true, null, null, null);
        when(userRepo.findByUsername("andrew")).thenReturn(user);

        ApiResponseWrapper<String> response =
                locationController.updateMyLocation(principal("andrew"), locationRequest(48.1486, 17.1077));

        assertTrue(success(response));
        assertEquals("Location updated successfully", message(response));
        assertNull(data(response));

        assertEquals(48.1486, doubleValue(read(user, "latitude")), 0.000001);
        assertEquals(17.1077, doubleValue(read(user, "longitude")), 0.000001);
        assertNotNull(read(user, "locationUpdatedAt"));

        verify(userRepo).findByUsername("andrew");
        verify(userRepo).save(user);
        verifyNoInteractions(studentProfileRepo);
    }

    @Test
    void getMyLocation_shouldReturnUnauthorized_whenPrincipalIsNull() {
        ApiResponseWrapper<UserLocationDTO> response =
                locationController.getMyLocation(null);

        assertFalse(success(response));
        assertEquals("Unauthorized", message(response));
        verifyNoInteractions(userRepo, studentProfileRepo);
    }

    @Test
    void getMyLocation_shouldReturnUnauthorized_whenPrincipalNameIsBlank() {
        ApiResponseWrapper<UserLocationDTO> response =
                locationController.getMyLocation(principal(" "));

        assertFalse(success(response));
        assertEquals("Unauthorized", message(response));
        verifyNoInteractions(userRepo, studentProfileRepo);
    }

    @Test
    void getMyLocation_shouldReturnUserNotFound_whenUserDoesNotExist() {
        when(userRepo.findByUsername("andrew")).thenReturn(null);

        ApiResponseWrapper<UserLocationDTO> response =
                locationController.getMyLocation(principal("andrew"));

        assertFalse(success(response));
        assertEquals("User not found", message(response));
        verify(userRepo).findByUsername("andrew");
        verifyNoInteractions(studentProfileRepo);
    }

    @Test
    void getMyLocation_shouldReturnLocationNotSet_whenLatitudeIsNull() {
        User user = user(1L, "andrew", true, null, 17.1077, null);
        when(userRepo.findByUsername("andrew")).thenReturn(user);

        ApiResponseWrapper<UserLocationDTO> response =
                locationController.getMyLocation(principal("andrew"));

        assertFalse(success(response));
        assertEquals("Location not set", message(response));
        verify(userRepo).findByUsername("andrew");
        verifyNoInteractions(studentProfileRepo);
    }

    @Test
    void getMyLocation_shouldReturnLocationNotSet_whenLongitudeIsNull() {
        User user = user(1L, "andrew", true, 48.1486, null, null);
        when(userRepo.findByUsername("andrew")).thenReturn(user);

        ApiResponseWrapper<UserLocationDTO> response =
                locationController.getMyLocation(principal("andrew"));

        assertFalse(success(response));
        assertEquals("Location not set", message(response));
        verify(userRepo).findByUsername("andrew");
        verifyNoInteractions(studentProfileRepo);
    }

    @Test
    void getMyLocation_shouldReturnError_whenShareLocationIsDisabled() {
        User user = user(1L, "andrew", false, 48.1486, 17.1077, LocalDateTime.now());
        when(userRepo.findByUsername("andrew")).thenReturn(user);

        ApiResponseWrapper<UserLocationDTO> response =
                locationController.getMyLocation(principal("andrew"));

        assertFalse(success(response));
        assertEquals("Share location disabled", message(response));
        verify(userRepo).findByUsername("andrew");
        verifyNoInteractions(studentProfileRepo);
    }

    @Test
    void getMyLocation_shouldReturnCurrentUserLocation_whenLocationExists() {
        LocalDateTime updatedAt = LocalDateTime.of(2026, 4, 10, 21, 30);
        User user = user(1L, "andrew", true, 48.1486, 17.1077, updatedAt);
        StudentProfile profile = profile("STU", "FIIT");

        when(userRepo.findByUsername("andrew")).thenReturn(user);
        when(studentProfileRepo.findByUser(user)).thenReturn(profile);

        ApiResponseWrapper<UserLocationDTO> response =
                locationController.getMyLocation(principal("andrew"));

        assertTrue(success(response));
        assertEquals("Location fetched successfully", message(response));

        Object dto = data(response);
        assertNotNull(dto);
        assertEquals(1L, longValue(read(dto, "userId")));
        assertEquals("andrew", read(dto, "username"));
        assertEquals(48.1486, doubleValue(read(dto, "latitude")), 0.000001);
        assertEquals(17.1077, doubleValue(read(dto, "longitude")), 0.000001);
        assertEquals(updatedAt, read(dto, "updatedAt"));
        assertEquals("STU", read(dto, "school"));
        assertEquals("FIIT", read(dto, "faculty"));

        verify(userRepo).findByUsername("andrew");
        verify(studentProfileRepo).findByUser(user);
    }


    @Test
    void getNearbyUsers_shouldReturnUnauthorized_whenPrincipalIsNull() {
        ApiResponseWrapper<List<UserLocationDTO>> response =
                locationController.getNearbyUsers(null);

        assertFalse(success(response));
        assertEquals("Unauthorized", message(response));
        verifyNoInteractions(userRepo, studentProfileRepo);
    }

    @Test
    void getNearbyUsers_shouldReturnUnauthorized_whenPrincipalNameIsBlank() {
        ApiResponseWrapper<List<UserLocationDTO>> response =
                locationController.getNearbyUsers(principal("   "));

        assertFalse(success(response));
        assertEquals("Unauthorized", message(response));
        verifyNoInteractions(userRepo, studentProfileRepo);
    }

    @Test
    void getNearbyUsers_shouldReturnUserNotFound_whenCurrentUserDoesNotExist() {
        when(userRepo.findByUsername("andrew")).thenReturn(null);

        ApiResponseWrapper<List<UserLocationDTO>> response =
                locationController.getNearbyUsers(principal("andrew"));

        assertFalse(success(response));
        assertEquals("User not found", message(response));
        verify(userRepo).findByUsername("andrew");
        verify(userRepo, never()).findAll();
        verifyNoInteractions(studentProfileRepo);
    }

    @Test
    void getNearbyUsers_shouldReturnError_whenShareLocationIsDisabledForCurrentUser() {
        User me = user(1L, "andrew", false, 48.1486, 17.1077, LocalDateTime.now());
        when(userRepo.findByUsername("andrew")).thenReturn(me);

        ApiResponseWrapper<List<UserLocationDTO>> response =
                locationController.getNearbyUsers(principal("andrew"));

        assertFalse(success(response));
        assertEquals("Share location disabled", message(response));
        verify(userRepo).findByUsername("andrew");
        verify(userRepo, never()).findAll();
        verifyNoInteractions(studentProfileRepo);
    }

    @Test
    void getNearbyUsers_shouldReturnError_whenCurrentUserLocationIsNotSet() {
        User me = user(1L, "andrew", true, null, 17.1077, null);
        when(userRepo.findByUsername("andrew")).thenReturn(me);

        ApiResponseWrapper<List<UserLocationDTO>> response =
                locationController.getNearbyUsers(principal("andrew"));

        assertFalse(success(response));
        assertEquals("Your location is not set", message(response));
        verify(userRepo).findByUsername("andrew");
        verify(userRepo, never()).findAll();
        verifyNoInteractions(studentProfileRepo);
    }

    @Test
    void getNearbyUsers_shouldReturnUsersSortedByDistanceAndSkipCurrentUserAndUsersWithoutLocation() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 10, 21, 30);

        User me = user(1L, "andrew", true, 48.1486, 17.1077, now);
        User near = user(2L, "near", true, 48.1490, 17.1080, now);
        User far = user(3L, "far", true, 48.1700, 17.1500, now);
        User withoutLocation = user(4L, "noLocation", true, null, null, null);

        when(userRepo.findByUsername("andrew")).thenReturn(me);
        when(userRepo.findAll()).thenReturn(List.of(far, withoutLocation, me, near));
        when(studentProfileRepo.findByUser(near)).thenReturn(null);
        when(studentProfileRepo.findByUser(far)).thenReturn(null);

        ApiResponseWrapper<List<UserLocationDTO>> response =
                locationController.getNearbyUsers(principal("andrew"));

        assertTrue(success(response));
        assertEquals("Nearby users fetched successfully", message(response));

        List<?> result = (List<?>) data(response);
        assertNotNull(result);
        assertEquals(2, result.size());

        Object first = result.get(0);
        Object second = result.get(1);

        assertEquals("near", read(first, "username"));
        assertEquals("far", read(second, "username"));

        assertTrue(doubleValue(read(first, "distanceKm")) < doubleValue(read(second, "distanceKm")));

        verify(userRepo).findByUsername("andrew");
        verify(userRepo).findAll();
        verify(studentProfileRepo).findByUser(near);
        verify(studentProfileRepo).findByUser(far);
        verify(studentProfileRepo, never()).findByUser(me);
        verify(studentProfileRepo, never()).findByUser(withoutLocation);
    }


    @Test
    void getUserLocation_shouldReturnError_whenUsernameIsNull() {
        ApiResponseWrapper<UserLocationDTO> response =
                locationController.getUserLocation(null);

        assertFalse(success(response));
        assertEquals("Username is required", message(response));
        verifyNoInteractions(userRepo, studentProfileRepo);
    }

    @Test
    void getUserLocation_shouldReturnError_whenUsernameIsBlank() {
        ApiResponseWrapper<UserLocationDTO> response =
                locationController.getUserLocation("   ");

        assertFalse(success(response));
        assertEquals("Username is required", message(response));
        verifyNoInteractions(userRepo, studentProfileRepo);
    }

    @Test
    void getUserLocation_shouldTrimUsernameAndReturnUserNotFound_whenUserDoesNotExist() {
        when(userRepo.findByUsername("john")).thenReturn(null);

        ApiResponseWrapper<UserLocationDTO> response =
                locationController.getUserLocation("  john  ");

        assertFalse(success(response));
        assertEquals("User not found", message(response));
        verify(userRepo).findByUsername("john");
        verifyNoInteractions(studentProfileRepo);
    }

    @Test
    void getUserLocation_shouldReturnLocationNotSet_whenUserHasNoLatitude() {
        User user = user(2L, "john", true, null, 17.1077, null);
        when(userRepo.findByUsername("john")).thenReturn(user);

        ApiResponseWrapper<UserLocationDTO> response =
                locationController.getUserLocation("john");

        assertFalse(success(response));
        assertEquals("Location not set", message(response));
        verify(userRepo).findByUsername("john");
        verifyNoInteractions(studentProfileRepo);
    }

    @Test
    void getUserLocation_shouldReturnLocationNotSet_whenUserHasNoLongitude() {
        User user = user(2L, "john", true, 48.1486, null, null);
        when(userRepo.findByUsername("john")).thenReturn(user);

        ApiResponseWrapper<UserLocationDTO> response =
                locationController.getUserLocation("john");

        assertFalse(success(response));
        assertEquals("Location not set", message(response));
        verify(userRepo).findByUsername("john");
        verifyNoInteractions(studentProfileRepo);
    }

    @Test
    void getUserLocation_shouldReturnError_whenTargetUserShareLocationIsDisabled() {
        User user = user(2L, "john", false, 48.1486, 17.1077, LocalDateTime.now());
        when(userRepo.findByUsername("john")).thenReturn(user);

        ApiResponseWrapper<UserLocationDTO> response =
                locationController.getUserLocation("john");

        assertFalse(success(response));
        assertEquals("Share location disabled", message(response));
        verify(userRepo).findByUsername("john");
        verifyNoInteractions(studentProfileRepo);
    }

    @Test
    void getUserLocation_shouldReturnTargetUserLocation_whenEverythingIsValid() {
        LocalDateTime updatedAt = LocalDateTime.of(2026, 4, 10, 21, 31);
        User user = user(2L, "john", true, 48.1501, 17.1102, updatedAt);
        StudentProfile profile = profile("Comenius University", "Mathematics");

        when(userRepo.findByUsername("john")).thenReturn(user);
        when(studentProfileRepo.findByUser(user)).thenReturn(profile);

        ApiResponseWrapper<UserLocationDTO> response =
                locationController.getUserLocation(" john ");

        assertTrue(success(response));
        assertEquals("Location fetched successfully", message(response));

        Object dto = data(response);
        assertNotNull(dto);
        assertEquals(2L, longValue(read(dto, "userId")));
        assertEquals("john", read(dto, "username"));
        assertEquals(48.1501, doubleValue(read(dto, "latitude")), 0.000001);
        assertEquals(17.1102, doubleValue(read(dto, "longitude")), 0.000001);
        assertEquals(updatedAt, read(dto, "updatedAt"));
        assertEquals("Comenius University", read(dto, "school"));
        assertEquals("Mathematics", read(dto, "faculty"));

        verify(userRepo).findByUsername("john");
        verify(studentProfileRepo).findByUser(user);
    }

    private Principal principal(String username) {
        return () -> username;
    }

    private LocationUpdateRequestDTO locationRequest(Double latitude, Double longitude) {
        LocationUpdateRequestDTO request = mock(LocationUpdateRequestDTO.class);
        lenient().when(request.getLatitude()).thenReturn(latitude);
        lenient().when(request.getLongitude()).thenReturn(longitude);
        return request;
    }

    private User user(
            Long id,
            String username,
            Boolean shareLocation,
            Double latitude,
            Double longitude,
            LocalDateTime updatedAt
    ) {
        User user = new User();
        write(user, "id", id);
        write(user, "username", username);
        write(user, "shareLocation", shareLocation);
        write(user, "latitude", latitude);
        write(user, "longitude", longitude);
        write(user, "locationUpdatedAt", updatedAt);
        return user;
    }

    private StudentProfile profile(String school, String faculty) {
        StudentProfile profile = new StudentProfile();
        write(profile, "school", school);
        write(profile, "faculty", faculty);
        write(profile, "subjects", null);
        return profile;
    }

    private boolean success(ApiResponseWrapper<?> response) {
        return Boolean.TRUE.equals(read(response, "success"));
    }

    private String message(ApiResponseWrapper<?> response) {
        return (String) read(response, "message");
    }

    private Object data(ApiResponseWrapper<?> response) {
        return read(response, "data");
    }

    private static Object read(Object target, String fieldName) {
        if (target == null) {
            return null;
        }

        String capitalized = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);

        for (String methodName : List.of("get" + capitalized, "is" + capitalized)) {
            try {
                Method method = target.getClass().getMethod(methodName);
                method.setAccessible(true);
                return method.invoke(target);
            } catch (Exception ignored) {
            }
        }

        Class<?> current = target.getClass();
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            } catch (Exception ignored) {
                current = current.getSuperclass();
            }
        }

        throw new IllegalStateException("Cannot read field/getter: " + fieldName + " from " + target.getClass());
    }

    private static void write(Object target, String fieldName, Object value) {
        Class<?> current = target.getClass();

        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);

                Object convertedValue = convertValue(value, field.getType());
                field.set(target, convertedValue);
                return;
            } catch (Exception ignored) {
                current = current.getSuperclass();
            }
        }

        throw new IllegalStateException("Cannot write field: " + fieldName + " to " + target.getClass());
    }

    private static Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        if (targetType.equals(Long.class) || targetType.equals(long.class)) {
            return ((Number) value).longValue();
        }

        if (targetType.equals(Integer.class) || targetType.equals(int.class)) {
            return ((Number) value).intValue();
        }

        if (targetType.equals(Double.class) || targetType.equals(double.class)) {
            return ((Number) value).doubleValue();
        }

        if (targetType.equals(Float.class) || targetType.equals(float.class)) {
            return ((Number) value).floatValue();
        }

        return value;
    }

    private static double doubleValue(Object value) {
        return ((Number) value).doubleValue();
    }

    private static long longValue(Object value) {
        return ((Number) value).longValue();
    }
}