package com.sep.mmms_backend.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
public class Response {
    String message;
    Object mainBody;

    public Response(Object mainBody) {
        this.mainBody = mainBody;
    }

    public Response(String message) {
        this.message = message;
    }

    public Response(ResponseMessages responseMessage) {
        this.message = responseMessage.toString();
    }

    public Response(ResponseMessages responseMessage, Object mainBody) {
        this.message = responseMessage.toString();
        this.mainBody = mainBody;
    }
}
