package com.sep.mmms_backend.dto;

import com.sep.mmms_backend.enums.AppRole;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class InviteRequestDtoTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void whenEmailIsStandardGmail_ValidationSucceeds() {
        InviteRequestDto dto = new InviteRequestDto();
        dto.setEmail("member@gmail.com");
        dto.setRole(AppRole.DEPARTMENT_MEMBER);

        Set<ConstraintViolation<InviteRequestDto>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }

    @Test
    void whenEmailIsAnyCustomDomain_ValidationSucceeds() {
        InviteRequestDto dto = new InviteRequestDto();
        dto.setEmail("coordinator@organization.org");
        dto.setRole(AppRole.SECRETARY);

        Set<ConstraintViolation<InviteRequestDto>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }

    @Test
    void whenEmailIsInvalidFormat_ValidationFails() {
        InviteRequestDto dto = new InviteRequestDto();
        dto.setEmail("not-an-email");
        dto.setRole(AppRole.GUEST);

        Set<ConstraintViolation<InviteRequestDto>> violations = validator.validate(dto);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void whenEmailIsBlank_ValidationFails() {
        InviteRequestDto dto = new InviteRequestDto();
        dto.setEmail("   ");
        dto.setRole(AppRole.COMMITTEE_MEMBER);

        Set<ConstraintViolation<InviteRequestDto>> violations = validator.validate(dto);
        assertThat(violations).isNotEmpty();
    }
}
