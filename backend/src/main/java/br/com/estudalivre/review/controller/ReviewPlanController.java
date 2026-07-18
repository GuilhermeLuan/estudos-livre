package br.com.estudalivre.review.controller;

import br.com.estudalivre.identity.service.IdentityPrincipal;
import br.com.estudalivre.review.dto.ReviewPlanResponse;
import br.com.estudalivre.review.dto.ReviewPlanSummaryResponse;
import br.com.estudalivre.review.dto.ReviewPlanVersionRequest;
import br.com.estudalivre.review.dto.UpdateReviewScheduleRequest;
import br.com.estudalivre.review.service.ReviewService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/api/review-plans")
public class ReviewPlanController {

    private final ReviewService reviewService;

    public ReviewPlanController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public List<ReviewPlanSummaryResponse> list(
            @AuthenticationPrincipal IdentityPrincipal principal) {
        return reviewService.listPlans(principal.id());
    }

    @GetMapping("/{planId}")
    public ReviewPlanResponse get(
            @AuthenticationPrincipal IdentityPrincipal principal,
            @PathVariable UUID planId) {
        return reviewService.getPlan(principal.id(), planId);
    }

    @PutMapping("/{planId}/schedule")
    public ReviewPlanResponse updateSchedule(
            @AuthenticationPrincipal IdentityPrincipal principal,
            @PathVariable UUID planId,
            @Valid @RequestBody UpdateReviewScheduleRequest request) {
        return reviewService.updateSchedule(principal.id(), principal.timeZone(), planId, request);
    }

    @PostMapping("/{planId}/cancel")
    public ReviewPlanResponse cancel(
            @AuthenticationPrincipal IdentityPrincipal principal,
            @PathVariable UUID planId,
            @Valid @RequestBody ReviewPlanVersionRequest request) {
        return reviewService.cancelPlan(principal.id(), planId, request.expectedVersion());
    }

    @PostMapping("/{planId}/reactivate")
    public ReviewPlanResponse reactivate(
            @AuthenticationPrincipal IdentityPrincipal principal,
            @PathVariable UUID planId,
            @Valid @RequestBody ReviewPlanVersionRequest request) {
        return reviewService.reactivatePlan(principal.id(), planId, request.expectedVersion());
    }
}
