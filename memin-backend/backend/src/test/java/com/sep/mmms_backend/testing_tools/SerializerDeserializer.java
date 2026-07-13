package com.sep.mmms_backend.testing_tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep.mmms_backend.response.Response;

public class SerializerDeserializer {


    /**
     *this static method takes 'string' format of Response.java, and reconstructos an objects of Response.java

     * only invoke this method if the passed string(argument) is serialized  version of Response.java
     */
    public static Response deserialize(String response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Response deserializedResponse = mapper.readValue(response, Response.class);
            return deserializedResponse;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
