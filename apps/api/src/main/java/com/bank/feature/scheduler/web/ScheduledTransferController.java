package com.bank.feature.scheduler.web;

import com.bank.feature.scheduler.domain.SchedulerService;
import com.bank.feature.scheduler.web.dto.ScheduledTransferView;
import com.bank.shared.utils.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Tag(name = "Scheduled Transfers", description = "Recurring and future-dated transfers")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/v1/scheduled-transfers")
public class ScheduledTransferController {

    public record ScheduleRequest(
            @NotNull UUID fromWalletId, @NotNull UUID toWalletId,
            @NotNull @Positive BigDecimal amount,
            String memo, String recurrenceRule,
            @NotNull Instant runAt) {}

    private final SchedulerService scheduler;
    private final CurrentUser currentUser;

    public ScheduledTransferController(SchedulerService scheduler, CurrentUser currentUser) {
        this.scheduler = scheduler;
        this.currentUser = currentUser;
    }

    @Operation(summary = "Schedule a transfer")
    @PostMapping
    @PreAuthorize("hasAuthority('transfer:create')")
    @ResponseStatus(HttpStatus.CREATED)
    public ScheduledTransferView schedule(@RequestBody ScheduleRequest req) {
        UUID userId = currentUser.id().orElseThrow();
        return scheduler.schedule(userId, req.fromWalletId(), req.toWalletId(),
                req.amount(), req.memo(), req.recurrenceRule(), req.runAt());
    }

    @Operation(summary = "List scheduled transfers")
    @GetMapping
    @PreAuthorize("hasAuthority('transfer:read')")
    public Page<ScheduledTransferView> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return scheduler.list(currentUser.id().orElseThrow(), pageable);
    }

    @Operation(summary = "Cancel a scheduled transfer")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('transfer:create')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable UUID id) {
        scheduler.cancel(id, currentUser.id().orElseThrow());
    }
}
