package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.duplicate;

import java.util.List;
import lombok.NoArgsConstructor;

/**
 * Strategy types for duplicate claim validation.
 *
 * @author Jamie Briggs
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class StrategyTypes {

  public static final List<String> CRIME = List.of("CRIME LOWER", "CRIME");
  public static final List<String> CIVIL = List.of("LEGAL HELP", "CIVIL");
  public static final List<String> MEDIATION = List.of("MEDIATION");
}
