package com.bank.feature.webhooks.web;

import com.bank.feature.webhooks.domain.WebhookService;
import com.bank.feature.webhooks.web.dto.WebhookView;
import com.bank.shared.utils.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Webhooks", description = "Event delivery to third-party endpoints")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/v1/webhooks")
public class WebhookController {

    public record RegisterRequest(@NotBlank String url, @NotBlank String secret,
                                   @NotBlank String eventTypes) {}

    private final WebhookService webhooks;
    private final CurrentUser currentUser;

    public WebhookController(WebhookService webhooks, CurrentUser currentUser) {
        this.webhooks = webhooks;
        this.currentUser = currentUser;
    }

    @Operation(summary = "Register a webhook endpoint")
    @PostMapping
    @PreAuthorize("hasAuthority('webhook:manage')")
    @ResponseStatus(HttpStatus.CREATED)
    public WebhookView register(@RequestBody RegisterRequest req) {
        return webhooks.register(currentUser.id().orElseThrow(), req.url(), req.secret(), req.eventTypes());
    }

    @Operation(summary = "List registered endpoints")
    @GetMapping
    @PreAuthorize("hasAuthority('webhook:manage')")
    public List<WebhookView> list() {
        return webhooks.list(currentUser.id().orElseThrow());
    }

    @Operation(summary = "Deregister an endpoint")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('webhook:manage')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deregister(@PathVariable UUID id) {
        webhooks.deregister(id, currentUser.id().orElseThrow());
    }
}
