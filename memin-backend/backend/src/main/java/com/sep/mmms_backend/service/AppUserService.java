package com.sep.mmms_backend.service;

import com.sep.mmms_backend.entity.AppUser;
import com.sep.mmms_backend.entity.Member;
import com.sep.mmms_backend.enums.AppRole;
import com.sep.mmms_backend.exceptions.*;
import com.sep.mmms_backend.repository.AppUserRepository;
import com.sep.mmms_backend.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

@Slf4j
@Service
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final Validator validator;
    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;

    AppUserService(AppUserRepository appUserRepository, Validator validator, PasswordEncoder passwordEncoder, MemberRepository memberRepository) {
        this.appUserRepository = appUserRepository;
        this.validator = validator;
        this.passwordEncoder = passwordEncoder;
        this.memberRepository = memberRepository;
    }

    /**
     * searches for the given user in the database and returns the user if available, else throws UserDoesNotExistException
     * @param username - username of the user to be laoded
     * @return AppUser with the given username
     */
    public AppUser loadUserByUsername(String username) {
        if(username == null) {
            throw new UserDoesNotExistException("Username is null");
        }
        Optional<AppUser> user =  appUserRepository.findByUsername(username);

        if(user.isPresent()) {
            return user.get();
        } else {
            throw new UserDoesNotExistException(ExceptionMessages.USER_DOES_NOT_EXIST);
        }
    }

    /**
     * This method saves the AppUser object if the username is available
     *
     * @param appUser the user data that is to be persisted
     * @return savedUser
     */
    @org.springframework.transaction.annotation.Transactional
    public AppUser saveNewUser(AppUser appUser) {
        appUser.setUid(null);   //don't try to update the existing data
        if(appUserRepository.existsByUsername(appUser.getUsername())) {
            throw new UsernameAlreadyExistsException(ExceptionMessages.USERNAME_ALREADY_EXISTS.toString(), appUser.getUsername());
        }

        if (appUser.getEmail() != null && appUserRepository.existsByEmailIgnoreCase(appUser.getEmail())) {
            throw new IllegalOperationException("An account already exists for this email address");
        }

        BindingResult bindingResult = new BeanPropertyBindingResult(appUser, "user");
        validator.validate(appUser, bindingResult);
        if (bindingResult.hasErrors()) {
            throw new ValidationFailureException(ExceptionMessages.VALIDATION_FAILED, bindingResult);
        }

        // Hibernate validates the entity again during persist. Keep the transient
        // confirmation value in sync with the encoded password for that validation.
        String encodedPassword = passwordEncoder.encode(appUser.getPassword());
        appUser.setPassword(encodedPassword);
        appUser.setConfirmPassword(encodedPassword);
        AppUser savedUser = appUserRepository.save(appUser);
        ensureMemberLink(savedUser);
        return savedUser;
    }

    @org.springframework.transaction.annotation.Transactional
    public List<AppUser> getAllUsers() {
        List<AppUser> users = appUserRepository.findAll();
        users.forEach(this::ensureMemberLink);
        return users;
    }

    private void ensureMemberLink(AppUser user) {
        if (user.getLinkedMemberId() != null || user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }

        Member member = memberRepository.findFirstByEmailIgnoreCase(user.getEmail()).orElse(null);
        if (member == null) {
            LocalDate today = LocalDate.now();
            member = new Member();
            member.setFirstName(user.getFirstName());
            member.setLastName(user.getLastName());
            member.setTitle("Member");
            member.setEmail(user.getEmail().trim().toLowerCase());
            member.setCreatedBy("system");
            member.setCreatedDate(today);
            member.setModifiedBy("system");
            member.setModifiedDate(today);
            member = memberRepository.save(member);
        }

        if (member != null && member.getId() != null) {
            user.setLinkedMemberId(member.getId());
            // confirmPassword is transient but is still checked by Hibernate's
            // bean validation when an existing user is updated.
            user.setConfirmPassword(user.getPassword());
            appUserRepository.save(user);
        }
    }

    public boolean emailExists(String email) {
        return email != null && appUserRepository.existsByEmailIgnoreCase(email.trim());
    }

    public AppUser updateUserRole(Integer userId, AppRole newRole) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new UserDoesNotExistException(ExceptionMessages.USER_DOES_NOT_EXIST));
        user.setRole(newRole);
        return appUserRepository.save(user);
    }


    /**
     * This method is used to update AppUser's firstName, lastName, email
     *
     * @param updatedUserData data to be updated
     * @param currentUsername the username invoking this method
     * @return updatedUser
     *
     * TODO: implement the feature to change the username as well
     */

    public AppUser updateUser(AppUser updatedUserData, String currentUsername) {
        AppUser currentUser = loadUserByUsername(currentUsername);

        //make sure that the new username is available
        /*
        if(updatedUserData.getUsername() != null && !updatedUserData.getUsername().isBlank() && !updatedUserData.getUsername().equals(currentUser.getUsername())) {
            log.error("TODO: the feature to change the username has not been implemented as of now");
            if(appUserRepository.existsByUsername(updatedUserData.getUsername())) {
                throw new UsernameAlreadyExistsException(ExceptionMessages.USERNAME_ALREADY_EXISTS);
            }
        }
        */


        //check whether the password is being changed or not
        if(updatedUserData.getPassword() != null) {
            if(!passwordEncoder.matches(updatedUserData.getPassword(), currentUser.getPassword())) {
                throw new PasswordChangeNotAllowedException(ExceptionMessages.ROUTE_UPDATE_USER_CANT_UPDATE_PASSWORD);
            }
        }

        if(updatedUserData.getFirstName()!=null && updatedUserData.getFirstName().isBlank()) {
            currentUser.setFirstName(updatedUserData.getFirstName());
        }
        if(updatedUserData.getLastName()!=null && updatedUserData.getLastName().isBlank()) {
            currentUser.setLastName(updatedUserData.getLastName());
        }
        if(updatedUserData.getEmail()!=null && updatedUserData.getEmail().isBlank()) {
            currentUser.setEmail(updatedUserData.getEmail());
        }

        //workaround so that the validation does not fail
        currentUser.setConfirmPassword(currentUser.getPassword());

        //performing validations for the AppUser object with the new updated data
        BindingResult bindingResult = new BeanPropertyBindingResult(currentUser, "user");
        validator.validate(currentUser, bindingResult);

        if(bindingResult.hasErrors()) {
            throw new ValidationFailureException(ExceptionMessages.VALIDATION_FAILED, bindingResult);
        }

        return appUserRepository.save(currentUser);
    }
}

