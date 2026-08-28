package com.sep.mmms_backend.config;

import com.sep.mmms_backend.exception_handling.CustomAccessDeniedHandler;
import com.sep.mmms_backend.exception_handling.CustomBasicAuthenticationEntryPoint;
import com.sep.mmms_backend.global_constants.GlobalConstants;
import com.sep.mmms_backend.service.AppUserService;
import com.sep.mmms_backend.user_details_service.DatabaseUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Autowired
    AppUserService appUserService;
    @Bean
    public SecurityFilterChain getSecurityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests((config)-> {
            config.requestMatchers("/h2-console/**","/css/**", "/templates/**", "/isAuthenticated", "/register", "/previewMeetingMinute", "/api/register-with-token", "/error").permitAll();
            config.requestMatchers(org.springframework.http.HttpMethod.GET, "/api/invite/*").permitAll();
            config.requestMatchers(org.springframework.http.HttpMethod.POST, "/api/invite").hasRole("DEPARTMENT_HEAD");
            config.requestMatchers(org.springframework.http.HttpMethod.POST, "/api/committee").hasRole("DEPARTMENT_HEAD");
            config.requestMatchers(org.springframework.http.HttpMethod.POST, "/api/meeting").authenticated();
            config.requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/minute").authenticated();
            config.requestMatchers("/api/users/**").hasRole("DEPARTMENT_HEAD");
            config.requestMatchers("/api/settings/**").hasRole("DEPARTMENT_HEAD");
            config.requestMatchers("/api/**").authenticated();
        });


        http.httpBasic(config-> {

            config.securityContextRepository(new DelegatingSecurityContextRepository(new HttpSessionSecurityContextRepository(), new RequestAttributeSecurityContextRepository()));

            //handles authorization exception
            config.authenticationEntryPoint(new CustomBasicAuthenticationEntryPoint());
        });



        //handles access denied exception
        http.exceptionHandling(config-> {
            config.accessDeniedHandler(new CustomAccessDeniedHandler());
        });


        //stores the security context in the request object as well as the http session object
        http.securityContext((config)-> {
            config.securityContextRepository(new DelegatingSecurityContextRepository(new HttpSessionSecurityContextRepository(), new RequestAttributeSecurityContextRepository()));
        });


        //by default all the POST routes are csrf protected
        //TODO: for now CSRF is disabled, will configure later
        http.csrf(AbstractHttpConfigurer::disable);

        http.cors(Customizer.withDefaults());

        //NOTE: this is for h2 database urls to work fine
        http.headers(AbstractHttpConfigurer::disable);

        return http.build();
    }


    //configuring the CORS
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                      .allowedOrigins(
                              "https://kushvlshresthv.github.io",
                              "https://memin-frontend.vercel.app",
                              "https://mmms.itprojects.pcampus.edu.np",
                              "http://localhost:4200",
                              GlobalConstants.FRONTEND_URL)
                        .allowedMethods("*")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }



    @Bean
    public UserDetailsService userDetailsService() {
        return new DatabaseUserDetailsService(appUserService);
    }

}
