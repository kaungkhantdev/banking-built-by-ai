package com.bank.feature.sessions.web;

import com.bank.feature.sessions.domain.SessionService;
import com.bank.feature.sessions.web.dto.SessionView;
import com.bank.shared.utils.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Sessions", description = "Device & session management")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/v1/sessions")
public class SessionController {

    private final SessionService sessions;
    private final CurrentUser currentUser;

    public SessionController(SessionService sessions, CurrentUser currentUser) {
        this.sessions = sessions;
        this.currentUser = currentUser;
    }

    @Operation(summary = "List active sessions")
    @GetMapping
    public List<SessionView> list() {
        return sessions.listSessions(currentUser.id().orElseThrow());
    }

    @Operation(summary = "Revoke a specific session")
    @DeleteMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable UUID sessionId) {
        sessions.revokeSession(currentUser.id().orElseThrow(), sessionId);
    }

    @Operation(summary = "Revoke all sessions except current")
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeAll(@RequestParam(required = false) UUID currentSessionId) {
        UUID uid = currentUser.id().orElseThrow();
        sessions.revokeAllSessions(uid, currentSessionId != null ? currentSessionId : UUID.randomUUID());
    }
}
