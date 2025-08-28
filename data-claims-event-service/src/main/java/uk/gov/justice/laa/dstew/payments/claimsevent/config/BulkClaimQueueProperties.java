package uk.gov.justice.laa.dstew.payments.claimsevent.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@AllArgsConstructor
@ConfigurationProperties(prefix = "laa.bulk-claim-queue")
public class BulkClaimQueueProperties {

  private String name;
}
