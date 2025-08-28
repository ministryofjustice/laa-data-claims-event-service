package uk.gov.justice.laa.dstew.payments.claimsevent.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({BulkClaimQueueProperties.class})
public class BulkClaimQueuePropertiesEnablerConfiguration {}
