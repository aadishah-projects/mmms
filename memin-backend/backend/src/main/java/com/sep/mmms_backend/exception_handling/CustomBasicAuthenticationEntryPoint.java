package com.sep.mmms_backend.exception_handling;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep.mmms_backend.response.Response;
import com.sep.mmms_backend.response.ResponseMessages;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

@Slf4j
public class CustomBasicAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        String requestedUri = request.getRequestURI();
        Response errorResponse = new Response();

        errorResponse.setMessage(ResponseMessages.AUTHENTICATION_FAILED.toString() + ": " + authException.getMessage());

        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(response.getWriter(), errorResponse);

        log.error("{} for {} : {}", ResponseMessages.AUTHENTICATION_FAILED.toString(), requestedUri , authException.getMessage());
    }
}
