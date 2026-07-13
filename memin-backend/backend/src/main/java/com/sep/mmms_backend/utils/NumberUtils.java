package com.sep.mmms_backend.utils;

import org.springframework.stereotype.Component;

@Component("numUtils")  // This name will be used in Thymeleaf expressions
public class NumberUtils {

    private static final String[] NEPALI_DIGITS = {"०","१","२","३","४","५","६","७","८","९"};

    public String toNepaliNumber(int number) {
        StringBuilder nepaliNum = new StringBuilder();
        for (char digit : String.valueOf(number).toCharArray()) {
            nepaliNum.append(NEPALI_DIGITS[digit - '0']);
        }
        return nepaliNum.toString();
    }
}
