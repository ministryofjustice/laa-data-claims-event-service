package uk.gov.justice.laa.dstew.payments.claimsevent.service.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Record representing a request to the claims data API to update a claim.
 *
 * @param claimStatus the new claim status.
 * @param errors an optional list of validation errors.
 */
public record UpdateClaimRequest(@NotNull ClaimStatus claimStatus, List<String> errors) {}
