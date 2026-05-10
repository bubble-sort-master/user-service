package com.innowise.userservice.controller;

import com.innowise.userservice.config.InternalAuthProperties;
import com.innowise.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalController {
    private final InternalAuthProperties internalAuthProperties;
    private final UserService userService;

    @DeleteMapping("/users/{id}/rollback")
    public ResponseEntity<Void> rollbackUser(
            @PathVariable Long id,
            @RequestHeader("X-Internal-Secret") String secret) {

        if (!internalAuthProperties.secret().equals(secret)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        userService.changeUserActiveStatus(id, false);
        return ResponseEntity.noContent().build();
    }
}
