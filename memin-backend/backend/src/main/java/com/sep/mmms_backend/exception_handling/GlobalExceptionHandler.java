package com.sep.mmms_backend.exception_handling;


import com.sep.mmms_backend.exceptions.*;
import com.sep.mmms_backend.response.Response;
import com.sep.mmms_backend.response.ResponseMessages;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * NOTE: all the exceptions are handled by this class. Except if Authentication fails, which is then handled by AuthenticationEntryPoint
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(UserDoesNotExistException.class)
    public ResponseEntity<Response> handleUserDoesNotExist(UserDoesNotExistException ex) {
        log.error("Exception handled: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Response(ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedUpdateException.class)
    public ResponseEntity<Response> handleUnauthorizedUpdate(UnauthorizedUpdateException ex) {
        log.error("Exception handled: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new Response(ex.getMessage()));
    }

    @ExceptionHandler(PasswordChangeNotAllowedException.class)
    public ResponseEntity<Response> handlePasswordChangeNotAllowed(PasswordChangeNotAllowedException ex) {
        log.error("Exception handled: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Response(ex.getMessage()));
    }

    //handles validation errors
    @ExceptionHandler(ValidationFailureException.class)
    public ResponseEntity<Object> handleConstraintViolation(ValidationFailureException ex) {
        Errors errors = ex.getErrors();

        HashMap<String, ArrayList<String>> errorMessages = new HashMap<>();
        errors.getFieldErrors().forEach(
                error-> {
                    //if the key is not already present in the Map, create the key as well as new ArrayList for the value
                    if(!errorMessages.containsKey(error.getField())){
                        ArrayList<String> list = new ArrayList<>();
                        list.add(error.getDefaultMessage());
                        errorMessages.put(error.getField(),list);
                    } else {
                        errorMessages.get(error.getField()).add(error.getDefaultMessage());
                    }
                }
        );

       log.error("Exception Handled: {}: {}", ex.getMessage(), errorMessages);
       return ResponseEntity.badRequest().body(new Response(ex.getMessage() + ": " + errorMessages));
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<Response> usernameAlreadyExists(UsernameAlreadyExistsException ex) {
        log.error("User with the username `{}` already exists", ex.getUsername());
        return ResponseEntity.badRequest().body(new Response(ex.getMessage()));
    }

    @ExceptionHandler(MemberDoesNotExistException.class)
    public ResponseEntity<Response> memberDoesNotExists(MemberDoesNotExistException ex) {
        log.error("Member with the memberId `{}` does not exist", ex.getMemberIds());
        return ResponseEntity.badRequest().body(new Response(ex.getMessage()+ " [id: " + ex.getMemberIds() + "]"));
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<Response> invalidRequest(InvalidRequestException ex) {
        log.error("Invalid Request- Request criteria not met: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(new Response(ex.getMessage()));
    }

    @ExceptionHandler(CommitteeDoesNotExistException.class)
    public ResponseEntity<Response> committeeDoesNotExists(CommitteeDoesNotExistException ex) {
        log.error("Committee with the committeeId `{}` does not exist", ex.getCommitteeId());
        return ResponseEntity.badRequest().body(new Response(ex.getMessage()));
    }


    @ExceptionHandler(MemberNotInCommitteeException.class)
    public ResponseEntity<Response> memberNotInCommittee(MemberNotInCommitteeException ex) {
        log.error("MemberId: {} does not belong to CommitteeId: {}  ", ex.getMemberId(), ex.getCommitteeId());
        return ResponseEntity.badRequest().body(new Response(ex.getMessage()));
    }


    @ExceptionHandler(IllegalOperationException.class)
    public ResponseEntity<Response> illegalOperation(IllegalOperationException ex) {
        log.error("Illegal Operation Attempted: {} ", ex.getMessage());
        return ResponseEntity.badRequest().body(new Response(ex.getMessage()));
    }


    @ExceptionHandler(MeetingDoesNotExistException.class)
    public ResponseEntity<Response> meetingDoesNotExist(MeetingDoesNotExistException ex) {
        log.error("Meeting with id {} does not exist", ex.getMeetingId() );
        return ResponseEntity.badRequest().body(new Response(ex.getMessage()+ " [id: " + ex.getMeetingId() + "]"));
    }


    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Response> httpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.error("HttpMessageNotReadableException: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(new Response(ResponseMessages.HTTP_MESSAGE_NOT_READABLE + ": " + ex.getMessage()));
    }


    @ExceptionHandler(MembershipAlreadyExistsException.class)
    public ResponseEntity<Response> memberAlreadyExists(MembershipAlreadyExistsException ex) {
        log.error("MembershipAlreadExistsException: {}", ex.getMessage() + "[memberId, role]: " + ex.getMemberIdAndRole());
        return ResponseEntity.badRequest().body(new Response(ex.getMessage() + "[memberId, role]: " + ex.getMemberIdAndRole()));
    }


    //TODO: Rewrite the getMessage method like below for all of the above methods
    @ExceptionHandler(CommitteeNotAccessibleException.class)
    public ResponseEntity<Response> committeeNotAccessible(CommitteeNotAccessibleException ex) {
        log.error("CommitteeNotAccessibleException: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(new Response(ex.getMessage()));
    }

    @ExceptionHandler(MeetingDoesNotBelongToCommittee.class)
    public ResponseEntity<Response> meetingDoesNotBelongToCommittee(MeetingDoesNotBelongToCommittee ex) {
        log.error("MeetingDoesNotBelongToCommittee: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(new Response(ex.getMessage()));
    }

    @ExceptionHandler(MeetingNotAccessibleException.class)
    public ResponseEntity<Response> meetingNotAccessible(MeetingNotAccessibleException ex) {
        log.error("MeetingNotAccessibleException: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(new Response(ex.getMessage()));
    }

    @ExceptionHandler(MembershipDoesNotExistException.class)
    public ResponseEntity<Response> memberDoesNotExist(MembershipDoesNotExistException ex) {
        log.error("MembershipDoesNotExistException: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(new Response(ex.getMessage()));
    }
}
