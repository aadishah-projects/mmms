package com.sep.mmms_backend.controller;

import com.sep.mmms_backend.dto.GlobalSearchResultDto;
import com.sep.mmms_backend.response.Response;
import com.sep.mmms_backend.service.GlobalSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequestMapping("/api")
public class GlobalSearchController {

    GlobalSearchService globalSearchService;
    GlobalSearchController(GlobalSearchService globalSearchService) {
        this.globalSearchService = globalSearchService;
    }

    @GetMapping("/global-search")
    public ResponseEntity<Response> globalSearch(@RequestParam String input, Authentication authentication) {
        GlobalSearchResultDto globalSearchResultDto = globalSearchService.globalSearch(input, authentication.getName());
        return ResponseEntity.ok().body(new Response(globalSearchResultDto));
    }
}
