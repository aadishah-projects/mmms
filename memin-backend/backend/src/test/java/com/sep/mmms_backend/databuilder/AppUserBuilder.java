package com.sep.mmms_backend.databuilder;

import com.sep.mmms_backend.entity.AppUser;
import com.sep.mmms_backend.entity.Committee;

import java.util.List;

public class AppUserBuilder {
    private String username = "testUser";
    private String firstName = "firstName";
    private String lastName = "lastName";
    private String email = "email";
    private String password = "password";
    private String confirmPassword = "confirmPassword";
    private List<Committee> myCommittees = null;

    /**
     * no need to populate anything, directly build
     */
    public static AppUserBuilder builder() {
        return new AppUserBuilder();
    }

    public AppUserBuilder withUsername(String username) {
        this.username = username;
        return this;
    }


    public AppUserBuilder withFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }


    public AppUserBuilder withLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public AppUserBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public AppUserBuilder withPassword(String password) {
        this.password = password;
        return this;
    }

    public AppUserBuilder withConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
        return this;
    }

    public AppUserBuilder withCommittees(List<Committee> committees) {
        this.myCommittees = committees;
        return this;
    }

    public AppUser build() {
        AppUser appUser = new AppUser();
        appUser.setUsername(this.username);
        appUser.setFirstName(this.firstName);
        appUser.setLastName(this.lastName);
        appUser.setEmail(this.email);
        appUser.setPassword(this.password);
        appUser.setConfirmPassword(this.confirmPassword);
        return appUser;
    }
}
