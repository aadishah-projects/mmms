package com.sep.mmms_backend.controller;

import com.sep.mmms_backend.dto.UpdateRoleDto;
import com.sep.mmms_backend.entity.AppUser;
import com.sep.mmms_backend.exceptions.ExceptionMessages;
import com.sep.mmms_backend.exceptions.UsernameAlreadyExistsException;
import com.sep.mmms_backend.exceptions.ValidationFailureException;
import com.sep.mmms_backend.response.Response;
import com.sep.mmms_backend.response.ResponseMessages;
import com.sep.mmms_backend.service.AppUserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class AppUserController {
    private final AppUserService appUserService;

    AppUserController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }


    @PostMapping("/register")
    @Deprecated
    public ResponseEntity<Response> registerUser(@RequestBody @Valid AppUser appUser, Errors errors) {
        if(errors.hasErrors()){
            throw new ValidationFailureException(ExceptionMessages.VALIDATION_FAILED, errors);
        }
        AppUser savedUser = appUserService.saveNewUser(appUser);

        log.info("User with the username @{} registered successfully", appUser.getUsername());
        return ResponseEntity.ok().body(new Response(ResponseMessages.USER_REGISTER_SUCCESS));
    }


    /**
     * this route does not allow the users to change the password
     * validation for the updated user data is performed inside the service class
     */
    @PostMapping("/api/updateUser")
    @Deprecated
    public ResponseEntity<Response> updateUser(@RequestBody AppUser appUser, Authentication authentication) {
        appUserService.updateUser(appUser,authentication.getName());
        return ResponseEntity.ok().body(new Response(ResponseMessages.USER_UPDATION_SUCCESS));
    }


    /**
     * Returns a list of all users with their id, name, email, username, and role.
     * Restricted to DEPARTMENT_HEAD via SecurityConfiguration.
     */
    @GetMapping("/api/users")
    public ResponseEntity<Response> getAllUsers() {
        List<AppUser> users = appUserService.getAllUsers();
        List<Map<String, Object>> userList = users.stream().map(user -> {
            Map<String, Object> map = new HashMap<>();
            map.put("uid", user.getUid());
            map.put("firstName", user.getFirstName());
            map.put("lastName", user.getLastName());
            map.put("username", user.getUsername());
            map.put("email", user.getEmail());
            map.put("role", user.getRole().name());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(new Response("Users loaded successfully", userList));
    }


    /**
     * Updates a user's role. Restricted to DEPARTMENT_HEAD via SecurityConfiguration.
     */
    @PatchMapping("/api/users/{id}/role")
    public ResponseEntity<Response> updateUserRole(@PathVariable Integer id, @RequestBody UpdateRoleDto updateRoleDto) {
        AppUser updatedUser = appUserService.updateUserRole(id, updateRoleDto.getRole());
        log.info("User @{} role updated to {}", updatedUser.getUsername(), updatedUser.getRole());
        return ResponseEntity.ok(new Response("User role updated successfully"));
    }
}
