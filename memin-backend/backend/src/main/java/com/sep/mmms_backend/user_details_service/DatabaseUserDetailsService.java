package com.sep.mmms_backend.user_details_service;

import com.sep.mmms_backend.entity.AppUser;
import com.sep.mmms_backend.exceptions.UserDoesNotExistException;
import com.sep.mmms_backend.service.AppUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Slf4j
public class DatabaseUserDetailsService implements UserDetailsService {
    AppUserService appUserService;
    public DatabaseUserDetailsService(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            AppUser registeredUser = appUserService.loadUserByUsername(username);
            log.info("UserDetailsService invoked by: {}", username);

            if(registeredUser != null) {
                UserDetails user = User.withUsername(username)
                        .password(registeredUser.getPassword())
                        .roles(registeredUser.getRole().name())
                        .build();
                return user;
            } else {
                throw new UserDoesNotExistException();
            }
        } catch(UserDoesNotExistException e) {
            throw new UsernameNotFoundException("@ " +username + "username not found");
        }
    }
}
