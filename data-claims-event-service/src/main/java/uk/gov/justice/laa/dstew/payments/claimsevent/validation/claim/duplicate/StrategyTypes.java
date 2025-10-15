package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.duplicate;

import java.util.List;

/** Strategy types for duplicate claim validation. */
public class StrategyTypes {
  public static final List<String> CRIME = List.of("CRIME LOWER", "CRIME");
  public static final List<String> CIVIL = List.of("LEGAL HELP", "CIVIL");
}
