package com.sep.mmms_backend.global_constants;

public class GlobalConstants {
    // Falls back to localhost:4200 if FRONTEND_URL isn't set in the environment
    public static final String FRONTEND_URL = System.getenv("FRONTEND_URL") != null ?
            System.getenv("FRONTEND_URL") : "http://localhost:4200";
}
