package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

/**
 * Interface for a claim validator. Implementations should be annotated with @Component.
 *
 * @author Jamie Briggs
 */
public interface ClaimValidator {

  int priority();
}
