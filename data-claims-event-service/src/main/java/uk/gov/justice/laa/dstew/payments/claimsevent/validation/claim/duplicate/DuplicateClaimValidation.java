package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.duplicate;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResultSet;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionStatus;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

/** Base class for duplicate claim validation. */
@Slf4j
public abstract class DuplicateClaimValidation implements DuplicateClaimValidationStrategy {

  protected static final List<ClaimStatus> DUPLICATE_CHECK_TARGET_CLAIM_STATUSES =
      List.of(ClaimStatus.READY_TO_PROCESS, ClaimStatus.VALID);

  static final List<SubmissionStatus> DUPLICATE_CHECK_TARGET_SUBMISSION_STATUSES =
      List.of(
          SubmissionStatus.CREATED,
          SubmissionStatus.VALIDATION_IN_PROGRESS,
          SubmissionStatus.READY_FOR_VALIDATION,
          SubmissionStatus.VALIDATION_SUCCEEDED);

  protected final DataClaimsRestClient dataClaimsRestClient;

  protected DuplicateClaimValidation(DataClaimsRestClient dataClaimsRestClient) {
    this.dataClaimsRestClient = dataClaimsRestClient;
  }

  /**
   * Search for duplicate claims across all submissions for the given office, matching on fee code,
   * unique file number, and unique client number. Only claims belonging to submissions in the
   * non-terminal submission statuses defined by {@code submissionStatuses}, and claims with a
   * non-invalid claim status (see {@code listOfNonInvalidStatus}), are considered. This method is
   * null-safe: if the REST client returns a null body or null content, an empty list is returned.
   *
   * @param officeCode the unique identifier for the office
   * @param feeCode the fee code
   * @param uniqueFileNumber the unique file number for the claim
   * @param uniqueClientNumber the unique client number for the claim
   * @return the list of matching claims found, or an empty list if none are found or the response
   *     body/content is null
   */
  protected List<ClaimResponse> getDuplicateClaims(
      final String officeCode,
      final String feeCode,
      final String uniqueFileNumber,
      final String uniqueClientNumber) {

    ClaimResultSet body =
        dataClaimsRestClient
            .getClaims(
                officeCode,
                null,
                DUPLICATE_CHECK_TARGET_SUBMISSION_STATUSES,
                feeCode,
                uniqueFileNumber,
                uniqueClientNumber,
                null,
                DUPLICATE_CHECK_TARGET_CLAIM_STATUSES,
                null)
            .getBody();

    if (body == null || body.getContent() == null) {
      return List.of();
    }

    log.debug(
        "[{}] Duplicate claims returned from API ({})",
        getClass().getSimpleName(),
        body.getContent().size());

    // Log basic details for each returned claim to help debug filtering behaviour
    // Promote candidate-level logs to INFO so they appear in test output
    body.getContent()
        .forEach(
            c ->
                log.info(
                    "[{}] -> candidate id={} submissionId={} feeCode={} ufn={} ucn={}",
                    getClass().getSimpleName(),
                    c.getId(),
                    c.getSubmissionId(),
                    c.getFeeCode(),
                    c.getUniqueFileNumber(),
                    c.getUniqueClientNumber()));

    return body.getContent().stream().toList();
  }

  /**
   * Evaluate a list of potential duplicate claims for the supplied {@code claim} and, if any
   * duplicates are present, log them and add the provided {@code validationError} to the {@code
   * SubmissionValidationContext} for the claim's id.
   *
   * <p>This helper centralizes the behaviour used for both duplicates detected within the same
   * submission and duplicates detected in other submissions for the same office. No action is taken
   * when the {@code duplicateClaims} list is empty or null.
   *
   * @param claim the claim being validated for duplicates
   * @param duplicateClaims candidate duplicate claims (may be null or empty)
   * @param validationError the validation error to add to the context when duplicates are found
   * @param context the validation context where claim-level errors will be recorded
   */
  protected void findDuplicateClaims(
      ClaimResponse claim,
      List<ClaimResponse> duplicateClaims,
      ClaimValidationError validationError,
      SubmissionValidationContext context) {
    if (!CollectionUtils.isEmpty(duplicateClaims)) {
      log.debug("[{}] - {}", claim.getId(), validationError.getDisplayMessage());
      logDuplicates(claim, duplicateClaims);
      context.addClaimError(claim.getId(), validationError);
    }
  }

  /**
   * Log the duplicate claims found for a given claim.
   *
   * @param claim The claim to log duplicates for.
   * @param duplicateClaims The list of duplicate claims.
   */
  protected void logDuplicates(
      final ClaimResponse claim, final List<ClaimResponse> duplicateClaims) {
    String csvDuplicateClaimIds =
        duplicateClaims.stream().map(ClaimResponse::getId).collect(Collectors.joining(","));
    log.debug(
        "{} duplicate claims found matching claim {}. Duplicates: {}",
        duplicateClaims.size(),
        claim.getId(),
        csvDuplicateClaimIds);
  }

  protected List<ClaimResponse> filterDuplicateClaimsInSameSubmission(
      ClaimResponse currentClaim, List<ClaimResponse> submissionClaims) {
    return filterDuplicateClaims(
        currentClaim,
        submissionClaims,
        claimToCompare ->
            Objects.equals(currentClaim.getSubmissionId(), claimToCompare.getSubmissionId()));
  }

  protected List<ClaimResponse> filterDuplicateClaimsInPreviousSubmission(
      ClaimResponse currentClaim, List<ClaimResponse> submissionClaims) {
    return filterDuplicateClaims(
        currentClaim,
        submissionClaims,
        claimToCompare ->
            !Objects.equals(currentClaim.getSubmissionId(), claimToCompare.getSubmissionId()));
  }

  protected List<ClaimResponse> filterDuplicateClaims(
      ClaimResponse currentClaim,
      List<ClaimResponse> submissionClaims,
      Predicate<ClaimResponse> duplicatePredicate) {
    // Detailed debug: show initial candidates and reasons for inclusion/exclusion
    log.debug(
        "Filtering duplicates for claim {} (submissionId={}): initial candidate count={}",
        currentClaim.getId(),
        currentClaim.getSubmissionId(),
        submissionClaims == null ? 0 : submissionClaims.size());

    if (submissionClaims == null || submissionClaims.isEmpty()) {
      return List.of();
    }

    List<ClaimResponse> filtered =
        submissionClaims.stream()
            .filter(
                submissionClaim -> !Objects.equals(submissionClaim.getId(), currentClaim.getId()))
            .filter(duplicatePredicate)
            .toList();

    // Log which candidates remain after filtering
    log.debug("Claim {}: filtered duplicates count={}", currentClaim.getId(), filtered.size());
    // Promote kept-candidate logs to INFO so they are visible in CI/test runs
    filtered.forEach(
        c ->
            log.info(
                "Claim {} -> kept candidate id={} submissionId={} feeCode={} ufn={} ucn={}",
                currentClaim.getId(),
                c.getId(),
                c.getSubmissionId(),
                c.getFeeCode(),
                c.getUniqueFileNumber(),
                c.getUniqueClientNumber()));

    // For visibility, also log which ones were removed due to having the same id or predicate
    // failing
    submissionClaims.stream()
        .filter(submissionClaim -> Objects.equals(submissionClaim.getId(), currentClaim.getId()))
        .forEach(
            c ->
                log.debug(
                    "Claim {} -> excluded (same id) id={} submissionId={}",
                    currentClaim.getId(),
                    c.getId(),
                    c.getSubmissionId()));

    submissionClaims.stream()
        .filter(submissionClaim -> !Objects.equals(submissionClaim.getId(), currentClaim.getId()))
        .filter(duplicatePredicate.negate())
        .forEach(
            c ->
                log.debug(
                    "Claim {} -> excluded (predicate false) id={} submissionId={} feeCode={} ufn={} ucn={}",
                    currentClaim.getId(),
                    c.getId(),
                    c.getSubmissionId(),
                    c.getFeeCode(),
                    c.getUniqueFileNumber(),
                    c.getUniqueClientNumber()));

    return filtered;
  }
}
