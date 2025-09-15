package uk.gov.justice.laa.dstew.payments.claimsevent.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessagePatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessageType;

/**
 * Class responsible for holding information about a claim under validation, including the claim ID
 * and validation messages.
 */
@Slf4j
@Getter
@EqualsAndHashCode
public class ClaimValidationReport {

  private final String claimId;
  private final List<ValidationMessagePatch> messages = new ArrayList<>();
  private boolean flaggedForRetry = false;

  public ClaimValidationReport(String claimId) {
    this.claimId = claimId;
  }

  public ClaimValidationReport(String claimId, Collection<ValidationMessagePatch> messages) {
    this.claimId = claimId;
    this.messages.addAll(messages);
  }

  /** Add an error from enum. */
  public void addError(ClaimValidationError error) {
    messages.add(error.toPatch());
  }

  /** Add an error directly. */
  public void addError(String message, String source, String technicalMessage) {
    messages.add(buildPatch(message, source, technicalMessage, ValidationMessageType.ERROR));
  }

  /** Add a warning directly. */
  public void addWarning(String message, String source, String technicalMessage) {
    messages.add(buildPatch(message, source, technicalMessage, ValidationMessageType.WARNING));
  }

  /** Bulk add errors from enums. */
  public void addErrors(List<ClaimValidationError> errorList) {
    errorList.forEach(e -> messages.add(e.toPatch()));
  }

  /** Bulk add prebuilt patches. */
  public void addMessages(List<ValidationMessagePatch> patches) {
    messages.addAll(patches);
  }

  public boolean hasErrors() {
    return messages.stream().anyMatch(m -> m.getType() == ValidationMessageType.ERROR);
  }

  public void flagForRetry() {
    log.debug("Flagging claim {} for retry", this.claimId);
    this.flaggedForRetry = true;
  }

  private ValidationMessagePatch buildPatch(
      String message, String source, String technicalMessage, ValidationMessageType type) {
    return new ValidationMessagePatch()
        .displayMessage(message)
        .technicalMessage(technicalMessage)
        .source(source)
        .type(type);
  }
}
