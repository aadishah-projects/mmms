package com.sep.mmms_backend.service;

import com.sep.mmms_backend.config.AppConfig;
import com.sep.mmms_backend.entity.AppUser;
import com.sep.mmms_backend.exceptions.PasswordChangeNotAllowedException;
import com.sep.mmms_backend.exceptions.UnauthorizedUpdateException;
import com.sep.mmms_backend.exceptions.UserDoesNotExistException;
import com.sep.mmms_backend.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes={LocalValidatorFactoryBean.class, AppUserService.class, AppConfig.class})
public class AppUserServiceTest {
    
    @MockitoBean
    private AppUserRepository appUserRepository;
    
    @Autowired
    private AppUserService appUserService;


    private AppUser currentUser;
    private AppUser updatedUserData;
    private final String currentUsername = "testUser";
    
    @BeforeEach
    public void setup() {
        // Initialize a current user
        currentUser = AppUser.builder()
                .uid(1)
                .firstName("John")
                .lastName("Doe")
                .username(currentUsername)
                .email("john.doe@example.com")
                .password("{noop}password123")
                .build();
        
        // Initialize updated user data
        updatedUserData = AppUser.builder()
                .uid(1)
                .firstName("John Updated")
                .lastName("Doe Updated")
                .username(currentUsername)
                .email("john.updated@example.com")
                .password("password123")
                .build();
    }

    //tests the success case
    @Test
    public void updateUser_Success() {
        // Arrange
        when(appUserRepository.findByUsername(currentUsername)).thenReturn(Optional.of(currentUser));
        when(appUserRepository.save(any(AppUser.class))).thenReturn(updatedUserData);
        
        // Act
        AppUser result = appUserService.updateUser(updatedUserData, currentUsername);
        
        // Assert
        assertNotNull(result);
        assertEquals("John Updated", result.getFirstName());
        assertEquals("Doe Updated", result.getLastName());
        assertEquals("john.updated@example.com", result.getEmail());
        verify(appUserRepository, times(1)).findByUsername(currentUsername);
        verify(appUserRepository, times(1)).save(any(AppUser.class));
    }

    //tests the case in which the user with the 'currentUsername' is not found in the database table
    @Test
    public void updateUser_UserNotFound() {
        // Arrange
        when(appUserRepository.findByUsername(currentUsername)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(UserDoesNotExistException.class, () -> {
            appUserService.updateUser(updatedUserData, currentUsername);
        });
        verify(appUserRepository, times(1)).findByUsername(currentUsername);
        verify(appUserRepository, never()).save(any(AppUser.class));
    }




    //tests the case in which the user tries to change password with the /updateUser route
    @Test
    public void updateUser_PasswordChangeAttempt() {
        // Arrange
        when(appUserRepository.findByUsername(currentUsername)).thenReturn(Optional.of(currentUser));
        
        // Create user data with different password
        AppUser userWithChangedPassword = AppUser.builder()
                .uid(1)
                .firstName("John")
                .lastName("Doe")
                .username(currentUsername)
                .email("john.doe@example.com")
                .password("newPassword123") // Different password
                .build();
        
        // Act & Assert
        assertThrows(PasswordChangeNotAllowedException.class, () -> {
            appUserService.updateUser(userWithChangedPassword, currentUsername);
        });
        verify(appUserRepository, times(1)).findByUsername(currentUsername);
        verify(appUserRepository, never()).save(any(AppUser.class));
    }


    //tests the case in which partial data is updated and the unupdated fields are retained in the db
    @Test
    public void updateUser_PartialUpdate() {
        // Arrange
        when(appUserRepository.findByUsername(currentUsername)).thenReturn(Optional.of(currentUser));
        
        // Create user data with only firstName updated
        AppUser partialUpdateUser = AppUser.builder()
                .uid(1)
                .firstName("John Updated")
                .build();
        
        AppUser expectedSavedUser = AppUser.builder()
                .uid(1)
                .firstName("John Updated")
                .lastName("Doe")
                .username(currentUsername)
                .email("john.doe@example.com")
                .password("password123")
                .build();
        
        when(appUserRepository.save(any(AppUser.class))).thenReturn(expectedSavedUser);
        
        // Act
        AppUser result = appUserService.updateUser(partialUpdateUser, currentUsername);
        
        // Assert
        assertNotNull(result);
        assertEquals("John Updated", result.getFirstName());
        assertEquals("Doe", result.getLastName()); // Should remain unchanged
        assertEquals("john.doe@example.com", result.getEmail()); // Should remain unchanged
        verify(appUserRepository, times(1)).findByUsername(currentUsername);
        verify(appUserRepository, times(1)).save(any(AppUser.class));
    }


    //tests the case in which the fields are null
    @Test
    public void updateUser_NullFields() {
        // Arrange
        when(appUserRepository.findByUsername(currentUsername)).thenReturn(Optional.of(currentUser));
        
        // Create user data with null fields
        AppUser nullFieldsUser = AppUser.builder()
                .uid(1)
                .build();
        
        AppUser expectedSavedUser = AppUser.builder()
                .uid(1)
                .firstName("John")
                .lastName("Doe")
                .username(currentUsername)
                .email("john.doe@example.com")
                .password("password123")
                .build();
        
        when(appUserRepository.save(any(AppUser.class))).thenReturn(expectedSavedUser);
        
        // Act
        AppUser result = appUserService.updateUser(nullFieldsUser, currentUsername);
        
        // Assert
        assertNotNull(result);
        assertEquals("John", result.getFirstName()); // Should remain unchanged
        assertEquals("Doe", result.getLastName()); // Should remain unchanged
        assertEquals("john.doe@example.com", result.getEmail()); // Should remain unchanged
        verify(appUserRepository, times(1)).findByUsername(currentUsername);
        verify(appUserRepository, times(1)).save(any(AppUser.class));
    }
}