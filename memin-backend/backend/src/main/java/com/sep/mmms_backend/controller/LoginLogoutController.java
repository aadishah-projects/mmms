package com.sep.mmms_backend.controller;

import com.sep.mmms_backend.response.Response;
import com.sep.mmms_backend.response.ResponseMessages;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;

//NOTE: CORS is handled globally from SecurityConfiguration.java
//@CrossOrigin(origins = "http://localhost:5173" ,  allowCredentials="true", exposedHeaders="*", allowedHeaders = "*")

@RestController
@RequestMapping("api")
public class LoginLogoutController {

    private final com.sep.mmms_backend.service.AppUserService appUserService;
    private final com.sep.mmms_backend.repository.CommitteeRepository committeeRepository;

    public LoginLogoutController(com.sep.mmms_backend.service.AppUserService appUserService, com.sep.mmms_backend.repository.CommitteeRepository committeeRepository) {
        this.appUserService = appUserService;
        this.committeeRepository = committeeRepository;
    }

    // "/login" is a secure rest end point handled by Spring Security
    @GetMapping("/login")
    public ResponseEntity<Response> tryLogin(Authentication authentication) {
        com.sep.mmms_backend.entity.AppUser user = appUserService.loadUserByUsername(authentication.getName());
        boolean isSecretary = false;
        if (user.getLinkedMemberId() != null) {
            isSecretary = !committeeRepository.getSecretaryActiveCommittees(user.getLinkedMemberId()).isEmpty();
        }
        
        java.util.Map<String, Object> loginData = java.util.Map.of(
            "role", user.getRole().name(),
            "isSecretary", isSecretary
        );
        return new ResponseEntity<Response>(new Response(ResponseMessages.LOGIN_SUCCESSFUL, loginData), HttpStatus.OK);
    }

    //TODO: configure the csrf and its tokens and make these request as POST.

    /**
    *  NOTE: if an anonymous user sends a request to this route, the global Authentication Failure Handler responds with HTTP.UNAUTHORIZED as this route is a protected route
     */
    @GetMapping("/logout")
    public ResponseEntity<Response> logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if(authentication != null){
            //clear the SecurityContextHolder and the following code also invalidates the HttpSession
            new SecurityContextLogoutHandler().logout(request, response, authentication);
        }
        return ResponseEntity.ok().body(new Response(ResponseMessages.LOGOUT_SUCCESSFUL));
    }
}
