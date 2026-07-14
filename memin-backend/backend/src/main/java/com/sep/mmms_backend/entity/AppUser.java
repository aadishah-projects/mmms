package com.sep.mmms_backend.entity;

import com.sep.mmms_backend.enums.AppRole;
import com.sep.mmms_backend.validators.annotations.FieldsValueMatch;
import com.sep.mmms_backend.validators.annotations.UsernameFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

import static com.sep.mmms_backend.global_constants.ValidationErrorMessages.*;

@Getter
@Setter
@Entity
@Table(name = "app_users")
@AllArgsConstructor
@Builder
@NoArgsConstructor

@FieldsValueMatch.List({
        @FieldsValueMatch(field = "password", fieldMatch = "confirmPassword", message = PASSWORD_CONFIRMPASSWORD_MISMATCH),
})
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer uid;


    @NotBlank(message = FIELD_CANNOT_BE_EMPTY)
    @Column(name = "firstname")
    String firstName;

    @NotBlank(message = FIELD_CANNOT_BE_EMPTY)
    @Column(name = "lastname")
    String lastName;

    @Column(name = "username")
    @NotBlank(message = FIELD_CANNOT_BE_EMPTY)
    @UsernameFormat
    String username;

    @NotBlank(message = FIELD_CANNOT_BE_EMPTY)
    @Email(message = VALID_EMAIL_REQUIRED)
    @Column(name = "email")
    String email;

    @NotBlank(message = FIELD_CANNOT_BE_EMPTY)
    @Column(name = "password")
//    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Size(min = 5, message = CHOOSE_STRONGER_PASSWORD)
    String password;

    @NotEmpty(message = FIELD_CANNOT_BE_EMPTY)
    @Transient
    String confirmPassword;

    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    AppRole role = AppRole.DEPARTMENT_MEMBER;

    @Column(name = "linked_member_id")
    Integer linkedMemberId;
}

/*
{
    "uid": 1,
    "firstName": "John",
    "lastName": "Doe",
    "username": "John",
    "email": "JohnDoe@gmail.com",
    "password":"johndoe",
    "confirmPassword":"johndoe"
}
 */
