//package com.sep.mmms_backend.controller.archived;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
//import com.sep.mmms_backend.config.JacksonConfig;
//import com.sep.mmms_backend.config.SecurityConfiguration;
//import com.sep.mmms_backend.controller.MeetingController;
//import com.sep.mmms_backend.entity.Meeting;
//import com.sep.mmms_backend.global_constants.ValidationErrorMessages;
//import com.sep.mmms_backend.response.Response;
//import com.sep.mmms_backend.response.ResponseMessages;
//import com.sep.mmms_backend.service.AppUserService;
//import com.sep.mmms_backend.service.MeetingService;
//import com.sep.mmms_backend.testing_tools.SerializerDeserializer;
//import lombok.extern.slf4j.Slf4j;
//import org.assertj.core.api.Assertions;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
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
//import java.time.LocalDate;
//import java.util.ArrayList;
//import java.util.HashMap;
//
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//
//@WebMvcTest(controllers= MeetingController.class)
//@Import({SecurityConfiguration.class, JacksonConfig.class})
//@Slf4j
//public class MeetingControllerTest {
//    @Autowired
//    private MockMvc mockMvc;
//
//    @MockitoBean
//    AppUserService appUserService;
//
//    @MockitoBean
//    MeetingService meetingService;
//
//    /**
//     * A valid 'Meeting' object
//     */
//    Meeting meeting;
//
//    @BeforeEach
//    public void init() {
//        //a valid meeting:
//       meeting = Meeting.builder().title("Meeting11").heldDate(LocalDate.now()).build();
//    }
//
//    private Response performRequestAndGetResponse(String destination, Object body, HttpStatus expectedHttpResponseStatus) throws Exception {
//        String requestBody = new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(meeting);
//        MvcResult result = mockMvc.perform(post(destination).contentType(MediaType.APPLICATION_JSON).content(requestBody)).andReturn();
//        int actualStatusCode = result.getResponse().getStatus();
//        Assertions.assertThat(actualStatusCode).isEqualTo(expectedHttpResponseStatus.value());
//        return SerializerDeserializer.deserialize(result.getResponse().getContentAsString());
//    }
//
//    //1) tests the case in which a valid meeting is created by an 'ANONYMOUS USER'
//    @Test
//    @WithAnonymousUser
//    public void createMeeting_ShouldReturnUnauthorized_WhenRequestIsMadeByAnonymousUser() throws Exception {
//        meeting.setTitle(null);
//        Response response = performRequestAndGetResponse("/api/createMeeting", meeting, HttpStatus.UNAUTHORIZED);
//    }
//
//
//    //2) tests the case in which the meeting name is empty
//    @Test
//    @WithMockUser()
//    public void createMeeting_ShouldReturnBadRequest_WhenMeetingNameIsBlank() throws Exception {
//        meeting.setTitle(null);
//        Response response = performRequestAndGetResponse("/api/createMeeting", meeting, HttpStatus.BAD_REQUEST);
//
//        Assertions.assertThat(response).isNotNull();
//        Assertions.assertThat(response.getMessage()).isNotNull();
//        Assertions.assertThat(response.getMessage()).isEqualTo(ResponseMessages.MEETING_CREATION_FAILED.toString());
//
//
//        @SuppressWarnings("unchecked")
//        HashMap<String, ArrayList<String>> mainBody = (HashMap<String, ArrayList<String>>)(response.getMainBody());
//        Assertions.assertThat(mainBody.containsKey("meetingTitle"));
//
//        //filter out the `FIELD_CANNOT_BE_EMPTY` messages
//        Assertions.assertThat(mainBody.get("meetingTitle")).filteredOn(e->e.equals(ValidationErrorMessages.FIELD_CANNOT_BE_EMPTY)).hasSize(1);
//    }
//
//
//
//    //3) tests the case in which the meetingHeldDate  is empty
//    @Test
//    @WithMockUser()
//    public void createMeeting_ShouldReturnBadRequest_WhenMeetingHeldDateIsEmpty() throws Exception {
//        meeting.setHeldDate(null);
//        Response response = performRequestAndGetResponse("/api/createMeeting", meeting, HttpStatus.BAD_REQUEST);
//
//        Assertions.assertThat(response).isNotNull();
//        Assertions.assertThat(response.getMessage()).isNotNull();
//        Assertions.assertThat(response.getMessage()).isEqualTo(ResponseMessages.MEETING_CREATION_FAILED.toString());
//
//
//        @SuppressWarnings("unchecked")
//        HashMap<String, ArrayList<String>> mainBody = (HashMap<String, ArrayList<String>>)(response.getMainBody());
//        Assertions.assertThat(mainBody.containsKey("meetingHeldDate"));
//
//        //filter out the `FIELD_CANNOT_BE_EMPTY` messages
//        Assertions.assertThat(mainBody.get("meetingHeldDate")).filteredOn(e->e.equals(ValidationErrorMessages.FIELD_CANNOT_BE_EMPTY)).hasSize(1);
//    }
//
//
//}
//
