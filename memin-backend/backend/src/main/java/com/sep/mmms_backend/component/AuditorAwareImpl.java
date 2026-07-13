package com.sep.mmms_backend.component;

//this class is used by Auditor to fetch infor that is needed by Auditing related annotations such as @CreatedBy

import com.sep.mmms_backend.service.AppUserService;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("auditAwareImpl")
public class AuditorAwareImpl implements AuditorAware<String> {
    @Override
    public Optional<String> getCurrentAuditor() {

       Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

       if(authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
           return Optional.empty();
       }

       System.out.println("Auditor Aware Executed");
       return Optional.ofNullable(authentication.getName());
    }
}
