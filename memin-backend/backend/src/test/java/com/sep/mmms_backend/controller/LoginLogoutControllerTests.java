package com.sep.mmms_backend.controller;

import com.sep.mmms_backend.config.AppConfig;
import com.sep.mmms_backend.config.SecurityConfiguration;
import com.sep.mmms_backend.entity.AppUser;
import com.sep.mmms_backend.response.Response;
import com.sep.mmms_backend.response.ResponseMessages;
import com.sep.mmms_backend.service.AppUserService;
import com.sep.mmms_backend.testing_tools.SerializerDeserializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@WebMvcTest(controllers = LoginLogoutController.class)
@Import({SecurityConfiguration.class, AppConfig.class})
public class LoginLogoutControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppUserService appUserService;

    @Nested
    @DisplayName("Testing /login route")
    class LoginRoute {

        private Response performGetRequestAndReturnResponse(String url, HttpStatus expectedStatus, HttpHeaders headers) throws Exception {

            MvcResult result = mockMvc.perform(get(url).headers(headers)).andReturn();

            int actualStatusCode = result.getResponse().getStatus();
            assertThat(actualStatusCode).isEqualTo(expectedStatus.value());
            return SerializerDeserializer.deserialize(result.getResponse().getContentAsString());
        }


        @Test
        @DisplayName("Should return unauthorized when Authorization headers are missing")
        public void testEmptyCredentials() throws Exception {
            // Act
            Response response = performGetRequestAndReturnResponse("/api/login", HttpStatus.UNAUTHORIZED, new HttpHeaders());
        }

        /**
         * Test when the provided username does not exist in the database
         */
        @Test
        @DisplayName("Should return unauthorized when username does not exist")
        public void testNonExistentUsername() throws Exception {
            // Arrange
            HttpHeaders headers = new HttpHeaders();
            String username = "nonexistent";
            String password = "nonexistent";
            headers.add(HttpHeaders.AUTHORIZATION, "Basic " +  HttpHeaders.encodeBasicAuth(username, password, StandardCharsets.UTF_8));

            Mockito.when(appUserService.loadUserByUsername(username)).thenThrow(UsernameNotFoundException.class);
            //Act
            Response response = performGetRequestAndReturnResponse("/api/login", HttpStatus.UNAUTHORIZED, headers);

            //Assert
            assertThat(response).isNotNull();
            Mockito.verify(appUserService, Mockito.times(1)).loadUserByUsername(username);
            assertThat(response.getMessage()).contains(ResponseMessages.AUTHENTICATION_FAILED.toString());
        }

        /**
         * Test when the provided username exists, but password is incorrect
         */
        @Test
        @DisplayName("Should return unauthorized when password is incorrect")
        public void testIncorrectPassword() throws Exception {
            // Arrange
            String username = "existinguser";
            String correctPassword = "{noop}correctpassword";
            String wrongPassword = "wrongpassword";

            AppUser appUser = new AppUser();
            appUser.setUsername(username);
            appUser.setPassword(correctPassword); // In real scenario, this would be encoded

            Mockito.when(appUserService.loadUserByUsername(username))
                    .thenReturn(appUser);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.AUTHORIZATION, "Basic " +  HttpHeaders.encodeBasicAuth(username, wrongPassword, StandardCharsets.UTF_8));

            // Act
            Response response = performGetRequestAndReturnResponse("/api/login", HttpStatus.UNAUTHORIZED , headers);

            // Assert
            Mockito.verify(appUserService, Mockito.times(1)).loadUserByUsername(username);
            assertThat(response).isNotNull();
            assertThat(response.getMessage()).contains(ResponseMessages.AUTHENTICATION_FAILED.toString());
        }

        /**
         * Test the success case where both the provided username and password is present in the database
         */
        @Test
        @DisplayName("Should return success when credentials are valid")
        public void testSuccessfulLogin() throws Exception {
            // Arrange
            String username = "existinguser";
            String correctPassword = "correctpassword";

            AppUser appUser = new AppUser();
            appUser.setUsername(username);
            appUser.setPassword("{noop}"+correctPassword); // In real scenario, this would be encoded

            Mockito.when(appUserService.loadUserByUsername(username))
                    .thenReturn(appUser);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.AUTHORIZATION, "Basic " +  HttpHeaders.encodeBasicAuth(username, correctPassword, StandardCharsets.UTF_8));

            Response response = performGetRequestAndReturnResponse("/api/login", HttpStatus.OK, headers);

            assertThat(response).isNotNull();
            assertThat(response.getMessage()).contains(ResponseMessages.LOGIN_SUCCESSFUL.toString());
            Mockito.verify(appUserService, Mockito.times(1)).loadUserByUsername(username);
        }
    }

    @Nested
    @DisplayName("Testing /logout route")
    class LogoutRoute {

        /**
         * Test the case in which an anonymous user tries to log out
         */
        @Test
        @WithAnonymousUser
        @DisplayName("Should return unauthorized when anonymous user tries to logout")
        public void testAnonymousUserLogout() throws Exception {
            // Act
            MvcResult result = mockMvc.perform(get("/api/logout")).andReturn();

            // Assert
            assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        }

        /**
         * Test the case when a logged in user tries to log out
         */
        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Should return success when authenticated user logs out")
        public void testAuthenticatedUserLogout() throws Exception {
            // Act
            MvcResult result = mockMvc.perform(get("/api/logout")).andReturn();
            Response response = SerializerDeserializer.deserialize(result.getResponse().getContentAsString());

            // Assert
            assertThat(response).isNotNull();
            assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.getMessage()).isEqualTo(ResponseMessages.LOGOUT_SUCCESSFUL.toString());
        }
    }
}
