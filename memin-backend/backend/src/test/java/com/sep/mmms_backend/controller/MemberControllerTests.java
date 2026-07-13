package com.sep.mmms_backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sep.mmms_backend.config.AppConfig;
import com.sep.mmms_backend.config.SecurityConfiguration;
import com.sep.mmms_backend.dto.MemberCreationDto;
import com.sep.mmms_backend.dto.MemberSummaryDto;
import com.sep.mmms_backend.entity.Committee;
import com.sep.mmms_backend.entity.Member;
import com.sep.mmms_backend.response.Response;
import com.sep.mmms_backend.response.ResponseMessages;
import com.sep.mmms_backend.service.AppUserService;
import com.sep.mmms_backend.service.CommitteeService;
import com.sep.mmms_backend.service.MemberService;
import com.sep.mmms_backend.testing_tools.SerializerDeserializer;
import com.sep.mmms_backend.testing_tools.TestDataHelper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@WebMvcTest(controllers = MemberController.class)
@Import({SecurityConfiguration.class, AppConfig.class})
@Slf4j
public class MemberControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private AppUserService appUserService;

    @MockitoBean
    private CommitteeService committeeService;


   //test the /createMember route

   /*
        /createMember route expects the following information in the request body:

        1. firstName
        2. lastName
        3. firstNameNepali
        4. lastNameNepali
        5. institution
        6. post
        7. email
        8. role

        and the committeeId in the query parameter.

        1. Test when additional data is sent in the request bdoy
        2. Test when committeeId is not sent in the query parameter
        3. check whether the response message is ResponseMeesages.MEMBER_CREATION_SUCCESS
        4. check whether the resonse mainBody contains the same information as returned by the mocked memberService.saveNewMember() function
    */

    @Nested
    @DisplayName("Testing /createMember route")
    class CreateMemberRoute {
        private Committee committee;
        private final String username = "testUser";
        private MemberCreationDto memberCreationDto;
        private Member createdMember;

        @BeforeEach
        void setUp() {
            TestDataHelper helper = new TestDataHelper();
            committee = helper.getCommittee();

            // Create MemberCreationDto with required fields
            memberCreationDto = new MemberCreationDto();
            // Use reflection to set fields since MemberCreationDto only has getters
            memberCreationDto.setFirstName( "John");
            memberCreationDto.setLastName( "Doe");


            memberCreationDto.setPost("Test Post");


            // Create Member that will be returned by the service
            createdMember = helper.getMember();
            createdMember.setFirstName("John");
            createdMember.setLastName("Doe");
            createdMember.setPost("Test Post");
            createdMember.setId(1);

            Mockito.when(committeeService.findCommitteeById(anyInt())).thenReturn(committee);

        }

        private Response performPostRequestAndGetResponse(String url, Object body, HttpStatus expectedStatus) throws Exception {
            String requestBody = new ObjectMapper().writeValueAsString(body);

            MvcResult result = mockMvc.perform(post(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andReturn();

            int actualStatusCode = result.getResponse().getStatus();
            assertThat(actualStatusCode).isEqualTo(expectedStatus.value());

            return SerializerDeserializer.deserialize(result.getResponse().getContentAsString());
        }

        // 1. Test when additional data is sent in the request body
        @Test
        @WithMockUser(username = "testUser")
        @DisplayName("Should ignore additional data in request body")
        void testAdditionalDataInRequestBody() throws Exception {
            // Arrange ie convert memberCreationDto to json with additional field

            ObjectMapper mapper = new ObjectMapper();

            JsonNode node = mapper.valueToTree(memberCreationDto);
            ObjectNode objectNode = (ObjectNode) node;
            objectNode.put("additionField", "additionalValue");
            String requestBody = mapper.writeValueAsString(objectNode);

            Mockito.when(memberService.saveNewMember(any(MemberCreationDto.class), eq(username)))
                    .thenReturn(createdMember);

            // Act
            Response response = performPostRequestAndGetResponse("/api/createMember?committeeId=" + committee.getId(),
                    requestBody, HttpStatus.BAD_REQUEST);

            //Assert
            assertThat(response).isNotNull();
            assertThat(response.getMessage()).contains(ResponseMessages.HTTP_MESSAGE_NOT_READABLE.toString()); // customize based on actual error message

            // Verify that service was NEVER called
            Mockito.verifyNoInteractions(memberService);
        }

        // 2. Test when committeeId is not sent in the query parameter
        @Test
        @WithMockUser(username = "testUser")
        @DisplayName("Should return BAD_REQUEST when committeeId is not provided")
        void testMissingCommitteeId() throws Exception {
            // Act & Assert
            MvcResult result = mockMvc.perform(post("/api/createMember")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(new ObjectMapper().writeValueAsString(memberCreationDto)))
                    .andReturn();

            int actualStatusCode = result.getResponse().getStatus();
            assertThat(actualStatusCode).isEqualTo(HttpStatus.BAD_REQUEST.value());

            // Verify
            Mockito.verify(memberService, Mockito.never())
                    .saveNewMember(any(MemberCreationDto.class), anyString());
        }

        // 3. Check whether the response message is ResponseMessages.MEMBER_CREATION_SUCCESS
        // 4. Check whether the response mainBody contains the same information as returned by the mocked memberService.saveNewMember() function
        @Test
        @WithMockUser(username = "testUser")
        @DisplayName("Should return success message and correct member data")
        void testSuccessfulMemberCreation() throws Exception {
            // Arrange
            Mockito.when(memberService.saveNewMember(any(MemberCreationDto.class), eq(username)))
                    .thenReturn(createdMember);

            // Act
            Response response = performPostRequestAndGetResponse("/api/createMember?committeeId=" + committee.getId(),
                    memberCreationDto, HttpStatus.OK);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getMessage()).isEqualTo(ResponseMessages.MEMBER_CREATION_SUCCESS.toString());

            // Convert mainBody to MemberSummaryDto using ObjectMapper
            ObjectMapper objectMapper = new ObjectMapper();
            MemberSummaryDto result = objectMapper.convertValue(response.getMainBody(), MemberSummaryDto.class);

            // Verify member details
            assertThat(result.getMemberId()).isEqualTo(createdMember.getId());
            assertThat(result.getFirstName()).isEqualTo(createdMember.getFirstName());
            assertThat(result.getLastName()).isEqualTo(createdMember.getLastName());
            assertThat(result.getPost()).isEqualTo(createdMember.getPost());

            // Verify
            Mockito.verify(memberService, Mockito.times(1))
                    .saveNewMember(any(MemberCreationDto.class),eq(username));
        }
    }




//    @Nested
//    @DisplayName("Testing /getMemberDetails route")
//    class GetMemberDetailsRoute {
//        private final int memberId = 1;
//        private final String username = "testUser";
//        private Member member;
//        private MemberDetailsDto memberDetailsDto;
//
//        @BeforeEach
//        void setUp() {
//            TestDataHelper helper  = new TestDataHelper();
//            Meeting meeting = helper.getMeeting();
//            Committee committee = helper.getCommittee();
//            member = helper.getMember();
//
//
//            //creating MemberDetailsDto object to be returned by the route
//
//            MemberDetailsDto.CommitteeInfo committeeInfo = new MemberDetailsDto.CommitteeInfo(
//                    committee.getId(), committee.getName(), committee.getDescription(), "Member");
//
//            MemberDetailsDto.MeetingInfo meetingInfo = new MemberDetailsDto.MeetingInfo(
//                    meeting.getId(), meeting.getTitle(), meeting.getDescription(), true);
//
//            List<MemberDetailsDto.MeetingInfo> meetingInfos = new ArrayList<>();
//            meetingInfos.add(meetingInfo);
//
//            MemberDetailsDto.CommitteeWithMeetings committeeWithMeetings =
//                    new MemberDetailsDto.CommitteeWithMeetings(committeeInfo, meetingInfos);
//
//            List<MemberDetailsDto.CommitteeWithMeetings> committeeWithMeetingsList = new ArrayList<>();
//            committeeWithMeetingsList.add(committeeWithMeetings);
//
//            memberDetailsDto = new MemberDetailsDto(member, committeeWithMeetingsList);
//
//            Mockito.when(committeeService.findCommitteeById(anyInt())).thenReturn(committee);
//        }
//
//        private Response performRequestAndGetResponse(String url, HttpStatus expectedStatus) throws Exception {
//            MvcResult result = mockMvc.perform(get(url)).andReturn();
//
//            int actualStatusCode = result.getResponse().getStatus();
//            assertThat(actualStatusCode).isEqualTo(expectedStatus.value());
//
//            return SerializerDeserializer.deserialize(result.getResponse().getContentAsString());
//        }
//
//        //1. Tests the case in which the member does not exist
//        @Test
//        @WithMockUser(username = "testUser")
//        @DisplayName("Should return NOT_FOUND when member does not exist")
//        void testMemberNotFound() throws Exception {
//            // Arrange
//            Mockito.when(memberService.getMemberDetails(anyInt(), anyString()))
//                    .thenThrow(new MemberDoesNotExistException(ExceptionMessages.MEMBER_DOES_NOT_EXIST, memberId));
//
//            // Act
//            Response response = performRequestAndGetResponse("/api/getMemberDetails?memberId=" + memberId, HttpStatus.BAD_REQUEST);
//
//            // Assert
//            assertThat(response).isNotNull();
//            assertThat(response.getMessage()).contains(ExceptionMessages.MEMBER_DOES_NOT_EXIST.toString());
//
//            // Verify
//            Mockito.verify(memberService, Mockito.times(1)).getMemberDetails(memberId, username);
//        }
//
//        //2. Tests the case in which the member is not accessible by the current user
//        @Test
//        @WithMockUser(username = "testUser")
//        @DisplayName("Should return FORBIDDEN when member is not accessible by current user")
//        void testMemberNotAccessible() throws Exception {
//            // Arrange
//            Mockito.when(memberService.getMemberDetails(anyInt(), anyString()))
//                    .thenThrow(new IllegalOperationException(ExceptionMessages.MEMBER_NOT_ACCESSIBLE));
//
//            // Act
//            Response response = performRequestAndGetResponse("/api/getMemberDetails?memberId=" + memberId, HttpStatus.BAD_REQUEST);
//
//            // Assert
//            assertThat(response).isNotNull();
//            assertThat(response.getMessage()).contains(ExceptionMessages.MEMBER_NOT_ACCESSIBLE.toString());
//
//            // Verify
//            Mockito.verify(memberService, Mockito.times(1)).getMemberDetails(memberId, username);
//        }
//
//        //3. Tests the case success case
//        @Test
//        @WithMockUser(username = "testUser")
//        @DisplayName("Should return OK with member details when successful")
//        void testSuccessCase() throws Exception {
//            // Arrange
//            Mockito.when(memberService.getMemberDetails(memberId, username)).thenReturn(memberDetailsDto);
//
//            // Act
//            Response response = performRequestAndGetResponse("/api/getMemberDetails?memberId=" + memberId, HttpStatus.OK);
//
//            // Assert
//            assertThat(response).isNotNull();
//
//            // Convert mainBody to MemberDetailsDto using ObjectMapper
//            ObjectMapper objectMapper = new ObjectMapper();
//            MemberDetailsDto result = objectMapper.convertValue(response.getMainBody(), MemberDetailsDto.class);
//
//            // Verify member details
//            assertThat(result.getMemberId()).isEqualTo(memberId);
//            assertThat(result.getFirstName()).isEqualTo(member.getFirstName());
//            assertThat(result.getLastName()).isEqualTo(member.getLastName());
//            assertThat(result.getInstitution()).isEqualTo(member.getInstitution());
//            assertThat(result.getPost()).isEqualTo(member.getPost());
////            assertThat(result.getQualification()).isEqualTo(member.getQualification());
//
//            // Verify committee and meeting info
//            assertThat(result.getCommitteeWithMeetings()).isNotEmpty();
//
//            // Verify
//            Mockito.verify(memberService, Mockito.times(1)).getMemberDetails(memberId, username);
//        }
//    }
}
