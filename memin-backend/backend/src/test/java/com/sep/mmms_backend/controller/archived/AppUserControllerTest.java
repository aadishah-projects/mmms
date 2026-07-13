//package com.sep.mmms_backend.controller.archived;
//
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.sep.mmms_backend.config.AppConfig;
//import com.sep.mmms_backend.config.SecurityConfiguration;
//import com.sep.mmms_backend.controller.AppUserController;
//import com.sep.mmms_backend.entity.AppUser;
//import com.sep.mmms_backend.exceptions.ExceptionMessages;
//import com.sep.mmms_backend.exceptions.PasswordChangeNotAllowedException;
//import com.sep.mmms_backend.exceptions.UnauthorizedUpdateException;
//import com.sep.mmms_backend.exceptions.UsernameAlreadyExistsException;
//import com.sep.mmms_backend.global_constants.ValidationErrorMessages;
//import com.sep.mmms_backend.response.Response;
//import com.sep.mmms_backend.response.ResponseMessages;
//import com.sep.mmms_backend.service.AppUserService;
//import com.sep.mmms_backend.testing_tools.SerializerDeserializer;
//import lombok.extern.slf4j.Slf4j;
//import org.assertj.core.api.Assertions;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mockito;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.context.annotation.Import;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.security.test.context.support.WithAnonymousUser;
//import org.springframework.security.test.context.support.WithMockUser;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.MvcResult;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//
//@WebMvcTest(controllers={AppUserController.class})
//@Import({SecurityConfiguration.class, AppConfig.class})
//@Slf4j
//public class AppUserControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @MockitoBean
//    AppUserService appUserService;
//
//    /**
//     * A valid user initialized with @BeforeEach
//     */
//    AppUser user;
//
//
//
//    @BeforeEach
//    public void init() {
//        //a valid user
//        user = AppUser.builder().firstName("firstName").lastName("lastName").username("username").email("email@gmail.com").password("password").confirmPassword("password").build();
//    }
//
//    private Response performRequestAndGetResponse(String destination, Object body, HttpStatus expectedHttpResponseStatus) throws Exception {
//
//        String requestBody = new ObjectMapper().writeValueAsString(body);
//
//        MvcResult result = mockMvc.perform(post(destination).contentType(MediaType.APPLICATION_JSON).content(requestBody)).andReturn();
//
//        int actualStatusCode = result.getResponse().getStatus();
//
//        Assertions.assertThat(actualStatusCode).isEqualTo(expectedHttpResponseStatus.value());
//
//        return SerializerDeserializer.deserialize(result.getResponse().getContentAsString());
//    }
//
//    //TESTING /register ROUTE:
//
//
//    //1) tests the case in which the first name is empty
//    @Test
//    @WithAnonymousUser
//    public void registerUser_ShouldReturnBadRequest_WhenFirstNameIsBlank() throws Exception {
//        //create the request body with invalid user
//        user.setFirstName(null);
//        Response response = performRequestAndGetResponse("/register", user, HttpStatus.BAD_REQUEST);
//
//        Assertions.assertThat(response).isNotNull();
//        Assertions.assertThat(response.getMessage()).isNotNull();
//        Assertions.assertThat(response.getMessage()).isEqualTo(ExceptionMessages.VALIDATION_FAILED.toString());
//
//        @SuppressWarnings("unchecked")
//        HashMap<String, ArrayList<String>> mainBody = (HashMap<String, ArrayList<String>>)(response.getMainBody());
//        Assertions.assertThat(mainBody.containsKey("firstName"));
//
//        Assertions.assertThat(mainBody.get("firstName")).filteredOn(e->e.equals(ValidationErrorMessages.FIELD_CANNOT_BE_EMPTY)).hasSize(1);
//    }
//
//
//
//
//    //2) tests the case in which the `last name` is empty
//    @Test
//    @WithAnonymousUser
//    public void registerUser_ShouldReturnBadRequest_WhenLastNameIsBlank() throws Exception {
//        //create the request body with invalid user
//        user.setLastName(null);
//
//        Response response = performRequestAndGetResponse("/register", user, HttpStatus.BAD_REQUEST);
//
//        Assertions.assertThat(response).isNotNull();
//        Assertions.assertThat(response.getMessage()).isNotNull();
//        Assertions.assertThat(response.getMessage()).isEqualTo(ExceptionMessages.VALIDATION_FAILED.toString());
//
//        @SuppressWarnings("unchecked")
//        HashMap<String, ArrayList<String>> mainBody = (HashMap<String, ArrayList<String>>)(response.getMainBody());
//        Assertions.assertThat(mainBody.containsKey("lastName"));
//
//
//        Assertions.assertThat(mainBody.get("lastName")).filteredOn(e->e.equals(ValidationErrorMessages.FIELD_CANNOT_BE_EMPTY)).hasSize(1);
//    }
//
//
//
//    //3) tests the case in which the `username` is empty
//    @Test
//    @WithAnonymousUser
//    public void registerUser_ShouldReturnBadRequest_WhenUsernameIsBlank() throws Exception {
//        //create the request body with invalid user
//        user.setUsername(null);
//
//        Response response = performRequestAndGetResponse("/register", user, HttpStatus.BAD_REQUEST);
//
//        Assertions.assertThat(response).isNotNull();
//        Assertions.assertThat(response.getMessage()).isNotNull();
//        Assertions.assertThat(response.getMessage()).isEqualTo(ExceptionMessages.VALIDATION_FAILED.toString());
//
//        @SuppressWarnings("unchecked")
//        HashMap<String, ArrayList<String>> mainBody = (HashMap<String, ArrayList<String>>)(response.getMainBody());
//        Assertions.assertThat(mainBody.containsKey("username"));
//
//
//        //filter out the `FIELD_CANNOT_BE_EMPTY` messages
//        Assertions.assertThat(mainBody.get("username")).filteredOn(e->e.equals(ValidationErrorMessages.FIELD_CANNOT_BE_EMPTY)).hasSize(1);
//
//    }
//
//
//
//    //4) tests the case in which the `username` format is not proper
//    @Test
//    @WithAnonymousUser
//    public void registerUser_ShouldReturnBadRequest_WhenUsernameFormatIsNotProper() throws Exception {
//
//        //create the request body with invalid user
//        user.setUsername("user$$$");
//        Response response = performRequestAndGetResponse("/register", user, HttpStatus.BAD_REQUEST);
//
//        Assertions.assertThat(response).isNotNull();
//        Assertions.assertThat(response.getMessage()).isNotNull();
//        Assertions.assertThat(response.getMessage()).isEqualTo(ExceptionMessages.VALIDATION_FAILED.toString());
//
//        //get the main body of the response
//        @SuppressWarnings("unchecked")
//        HashMap<String, ArrayList<String>> mainBody = (HashMap<String, ArrayList<String>>)(response.getMainBody());
//        Assertions.assertThat(mainBody.containsKey("username"));
//
//        //filter out the `INVALID_USERNAME_FORMAT` messages
//        Assertions.assertThat(mainBody.get("username")).filteredOn(e->e.equals(ValidationErrorMessages.INVALID_USERNAME_FORMAT)).hasSize(1);
//    }
//
//
//
//    //5) tests the case in which the `username` already exists in the database
//    @Test
//    @WithAnonymousUser
//    public void registerUser_ShouldReturnBadRequest_WhenUsernameAlreadyExists() throws Exception {
//        Mockito.when(appUserService.saveNewUser(any())).thenThrow(new UsernameAlreadyExistsException(ExceptionMessages.USERNAME_ALREADY_EXISTS.toString(), user.getUsername()));
//
//        //create the request body with invalid user
//        user.setUsername("username");
//
//        Response response = performRequestAndGetResponse("/register", user, HttpStatus.BAD_REQUEST);
//
//        Assertions.assertThat(response).isNotNull();
//        Assertions.assertThat(response.getMessage()).isNotNull();
//        Assertions.assertThat(response.getMessage()).isEqualTo(ExceptionMessages.USERNAME_ALREADY_EXISTS.toString());
//    }
//
//
//    //6) tests the case in which the email is empty
//    @Test
//    @WithAnonymousUser
//    public void registerUser_ShouldReturnBadRequest_WhenEmailIsBlank() throws Exception {
//        //create the request body with invalid user
//        user.setEmail(null);
//
//        Response response = performRequestAndGetResponse("/register", user, HttpStatus.BAD_REQUEST);
//
//        Assertions.assertThat(response).isNotNull();
//        Assertions.assertThat(response.getMessage()).isNotNull();
//        Assertions.assertThat(response.getMessage()).isEqualTo(ExceptionMessages.VALIDATION_FAILED.toString());
//
//        @SuppressWarnings("unchecked")
//        HashMap<String, ArrayList<String>> mainBody = (HashMap<String, ArrayList<String>>)(response.getMainBody());
//        Assertions.assertThat(mainBody.containsKey("email"));
//
//
//        //filter out the `FIELD_CANNOT_BE_EMPTY` messages
//        Assertions.assertThat(mainBody.get("email")).filteredOn(e->e.equals(ValidationErrorMessages.FIELD_CANNOT_BE_EMPTY)).hasSize(1);
//    }
//
//
//
//    //7) tests the case in which the email is invalid
//    @Test
//    @WithAnonymousUser
//    public void registerUser_ShouldReturnBadRequest_WhenEmailIsInvalid() throws Exception {
//        //create the request body with invalid user
//        user.setEmail("invalidEmail");
//
//        Response response = performRequestAndGetResponse("/register", user, HttpStatus.BAD_REQUEST);
//
//        Assertions.assertThat(response).isNotNull();
//        Assertions.assertThat(response.getMessage()).isNotNull();
//        Assertions.assertThat(response.getMessage()).isEqualTo(ExceptionMessages.VALIDATION_FAILED.toString());
//
//        @SuppressWarnings("unchecked")
//        HashMap<String, ArrayList<String>> mainBody = (HashMap<String, ArrayList<String>>)(response.getMainBody());
//        Assertions.assertThat(mainBody.containsKey("email"));
//
//
//        //filter out the `VALID_EMAIL_REQUIRED` messages
//        Assertions.assertThat(mainBody.get("email")).filteredOn(e->e.equals(ValidationErrorMessages.VALID_EMAIL_REQUIRED)).hasSize(1);
//    }
//
//
//    //8) tests the case in which password is empty
//    @Test
//    @WithAnonymousUser
//    public void registerUser_ShouldReturnBadRequest_WhenPasswordIsEmpty() throws Exception {
//        //create the request body with invalid user
//        user.setPassword(null);
//
//        Response response = performRequestAndGetResponse("/register", user, HttpStatus.BAD_REQUEST);
//
//        Assertions.assertThat(response).isNotNull();
//        Assertions.assertThat(response.getMessage()).isNotNull();
//        Assertions.assertThat(response.getMessage()).isEqualTo(ExceptionMessages.VALIDATION_FAILED.toString());
//
//
//        @SuppressWarnings("unchecked")
//        HashMap<String, ArrayList<String>> mainBody = (HashMap<String, ArrayList<String>>)(response.getMainBody());
//        Assertions.assertThat(mainBody.containsKey("password"));
//
//
//        //filter out the `FIELD_CANNOT_BE_EMPTY` messages
//        Assertions.assertThat(mainBody.get("password")).filteredOn(e->e.equals(ValidationErrorMessages.FIELD_CANNOT_BE_EMPTY)).hasSize(1);
//    }
//
//
//
//    //8.1) tests the case in which password is shorter than 5 letters
//    @Test
//    @WithAnonymousUser
//    public void registerUser_ShouldReturnBadRequest_WhenPasswordIsShort() throws Exception {
//        //create the request body with invalid user
//        user.setPassword("abcd");
//
//        Response response = performRequestAndGetResponse("/register", user, HttpStatus.BAD_REQUEST);
//
//        Assertions.assertThat(response).isNotNull();
//        Assertions.assertThat(response.getMessage()).isNotNull();
//        Assertions.assertThat(response.getMessage()).isEqualTo(ExceptionMessages.VALIDATION_FAILED.toString());
//
//        @SuppressWarnings("unchecked")
//        HashMap<String, ArrayList<String>> mainBody = (HashMap<String, ArrayList<String>>)(response.getMainBody());
//        Assertions.assertThat(mainBody.containsKey("password"));
//
//
//        //filter out the `CHOOSE_STRONGER_PASSWORD` messages
//        Assertions.assertThat(mainBody.get("password")).filteredOn(e->e.equals(ValidationErrorMessages.CHOOSE_STRONGER_PASSWORD)).hasSize(1);
//    }
//
//
//
//    //9) tests the case in which confirmPassword is blank
//    @Test
//    @WithAnonymousUser
//    public void registerUser_ShouldReturnBadRequest_WhenConfirmPasswordIsBlank() throws Exception {
//        //create the request body with invalid user
//        user.setConfirmPassword(null);
//
//        Response response = performRequestAndGetResponse("/register", user, HttpStatus.BAD_REQUEST);
//
//        Assertions.assertThat(response).isNotNull();
//        Assertions.assertThat(response.getMessage()).isNotNull();
//        Assertions.assertThat(response.getMessage()).isEqualTo(ExceptionMessages.VALIDATION_FAILED.toString());
//
//        @SuppressWarnings("unchecked")
//        HashMap<String, ArrayList<String>> mainBody = (HashMap<String, ArrayList<String>>)(response.getMainBody());
//        Assertions.assertThat(mainBody.containsKey("confirmPassword"));
//
//
//        //filter out the `FIELD_CANNOT_BE_EMPTY` messages
//        Assertions.assertThat(mainBody.get("confirmPassword")).filteredOn(e->e.equals(ValidationErrorMessages.FIELD_CANNOT_BE_EMPTY)).hasSize(1);
//    }
//
//
//
//    //10) tests the case in which password and confirm password fields have differnet values
//    @Test
//    @WithAnonymousUser
//    public void registerUser_ShouldReturnBadRequest_WhenPasswordAndConfirmPasswordDontMatch() throws Exception {
//        //create the request body with invalid user
//        user.setPassword("notConfirm");
//        user.setConfirmPassword("confirm");
//
//        Response response = performRequestAndGetResponse("/register", user, HttpStatus.BAD_REQUEST);
//
//        Assertions.assertThat(response).isNotNull();
//        Assertions.assertThat(response.getMessage()).isNotNull();
//        Assertions.assertThat(response.getMessage()).isEqualTo(ExceptionMessages.VALIDATION_FAILED.toString());
//        @SuppressWarnings("unchecked")
//        HashMap<String, ArrayList<String>> mainBody = (HashMap<String, ArrayList<String>>)(response.getMainBody());
//        Assertions.assertThat(mainBody.containsKey("password"));
//
//
//        //filter out the `PASSWORD_CONFIRMPASSWORD_MISMATCH` messages
//        Assertions.assertThat(mainBody.get("password")).filteredOn(e->e.equals(ValidationErrorMessages.PASSWORD_CONFIRMPASSWORD_MISMATCH)).hasSize(1);
//    }
//
//
//
//    //11) tests the success case
//    @Test
//    @WithAnonymousUser
//    public void registerUser_ShouldReturnOK_WhenAllValidationsAreSatisfied() throws Exception {
//
//        Response response = performRequestAndGetResponse("/register", user, HttpStatus.OK);
//
//        Assertions.assertThat(response).isNotNull();
//        Assertions.assertThat(response.getMessage()).isNotNull();
//        Assertions.assertThat(response.getMessage()).isEqualTo(ResponseMessages.USER_REGISTER_SUCCESS.toString());
//    }
//
//
//
//    //TESTING /updateUser route
//
//    //1) trying to access this route without logging in
//
//    @Test
//    @WithAnonymousUser
//    public void updateUser_ShouldReturn_Unauthorized_WhenAccessedWithoutLogginIn() throws Exception {
//
//        Response response = performRequestAndGetResponse("/api/updateUser", user, HttpStatus.UNAUTHORIZED);
//
//        Assertions.assertThat(response).isNotNull();
//        Assertions.assertThat(response.getMessage()).isNotNull();
//        Assertions.assertThat(response.getMessage()).contains(ResponseMessages.AUTHENTICATION_FAILED.toString());
//    }
//
//
//
//
//    //2. trying to change some other user's data
//    @Test
//    @WithMockUser(username="username")
//    public void updateUser_ShouldReturn_BadRequest_WhenAUserTriesToUpdateOtherUsersData() throws Exception {
//        user.setUid(100);
//        Mockito.when(appUserService.updateUser(any(), any())).thenThrow(new UnauthorizedUpdateException(ExceptionMessages.USER_CAN_ONLY_UPDATE_THEIR_OWN_DATA));
//
//        //some other user
//        AppUser newUser = AppUser.builder().uid(54).firstName("firstName").lastName("lastName").build();
//        Response response = performRequestAndGetResponse("/api/updateUser", newUser, HttpStatus.FORBIDDEN);
//
//        Assertions.assertThat(response).isNotNull();
//        Assertions.assertThat(response.getMessage()).isNotNull();
//        Assertions.assertThat(response.getMessage()).contains(ExceptionMessages.USER_CAN_ONLY_UPDATE_THEIR_OWN_DATA.toString());
//    }
//
//
//
//    //3. trying to change the password
//    @Test
//    @WithMockUser(username="username")
//    public void updateUser_ShouldReturn_BadRequest_WhenAUserTriesToChangePassword() throws Exception {
//        user.setUid(100);
//        Mockito.when(appUserService.updateUser(any(), any())).thenThrow(new PasswordChangeNotAllowedException(ExceptionMessages.ROUTE_UPDATE_USER_CANT_UPDATE_PASSWORD));
//
//        //updated user:
//        AppUser newUser = AppUser.builder().uid(100).password("notOldPassword").build();
//        Response response = performRequestAndGetResponse("/api/updateUser", newUser, HttpStatus.BAD_REQUEST);
//
//        Assertions.assertThat(response).isNotNull();
//        Assertions.assertThat(response.getMessage()).isNotNull();
//        Assertions.assertThat(response.getMessage()).isEqualTo(ExceptionMessages.ROUTE_UPDATE_USER_CANT_UPDATE_PASSWORD.toString());
//    }
//
//
//    //5. TODO: when the user with the given username does not exist on the database
//
//    //6 success case:
//
//    @Test
//    @WithMockUser(username="username")
//    public void updateUser_ShouldReturn_Ok_WhenEverythingIsFine() throws Exception {
//        user.setUid(100);
//        Mockito.when(appUserService.loadUserByUsername("username")).thenReturn(user);
//
//        //updated user
//        AppUser newUser = AppUser.builder().uid(100).firstName("newFirstName").lastName("newLastName").email("email@gmail.com").password("password").confirmPassword("password").username("username").build();
//
//        Response response = performRequestAndGetResponse("/api/updateUser", newUser, HttpStatus.OK);
//
//        Assertions.assertThat(response).isNotNull();
//        Assertions.assertThat(response.getMessage()).isNotNull();
//        Assertions.assertThat(response.getMessage()).isEqualTo(ResponseMessages.USER_UPDATION_SUCCESS.toString());
//    }
//
//}
