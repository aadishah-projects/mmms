//package com.sep.mmms_backend.controller.archived;
//
//
//import com.sep.mmms_backend.config.SecurityConfiguration;
//import com.sep.mmms_backend.controller.GenericController;
//import com.sep.mmms_backend.response.Response;
//import com.sep.mmms_backend.service.AppUserService;
//import com.sep.mmms_backend.testing_tools.SerializerDeserializer;
//import org.assertj.core.api.Assertions;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.context.annotation.Import;
//import org.springframework.http.MediaType;
//import org.springframework.security.test.context.support.WithAnonymousUser;
//import org.springframework.security.test.context.support.WithMockUser;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.MvcResult;
//
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@WebMvcTest(GenericController.class)
//@Import(SecurityConfiguration.class)
//public class GenericControllerTest {
//    @Autowired
//    MockMvc mockMvc;
//
//    //note: SecurityConfiguration has UserDetailsService that requires AppUserService dependency
//    @MockitoBean
//    AppUserService appUserService;
//
//    //1: tests the case where an anonymouse user sends a request to /isAuthenticated
//    @Test
//    @WithAnonymousUser
//    public void GenericController_IsAuthenticated_Returns_UNAUTHORIZED() throws Exception {
//        MvcResult result = mockMvc.perform(get("/isAuthenticated").contentType(MediaType.APPLICATION_JSON)).andExpect((status().isUnauthorized())).andReturn();
//
//        Response response = SerializerDeserializer.deserialize(result.getResponse().getContentAsString());
//
//        Assertions.assertThat(response).isNotNull();
//        Assertions.assertThat(response.getMessage()).isNotNull();
//        Assertions.assertThat(response.getMessage().equals("false"));
//    }
//
//
//
//    @Test
//    @WithMockUser(username="username")
//    public void GenericController_IsAuthenticated_Returns_OK() throws Exception {
//        MvcResult result = mockMvc.perform(get("/isAuthenticated").contentType(MediaType.APPLICATION_JSON)).andExpect((status().isOk())).andReturn();
//
//        Response response = SerializerDeserializer.deserialize(result.getResponse().getContentAsString());
//
//        Assertions.assertThat(response).isNotNull();
//        Assertions.assertThat(response.getMessage()).isNotNull();
//        Assertions.assertThat(response.getMessage().equals("true"));
//    }
//}
