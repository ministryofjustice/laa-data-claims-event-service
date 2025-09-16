package uk.gov.justice.laa.dstew.payments.claimsevent.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configuration of beans related to submission events. */
@Configuration
public class SubmissionEventConfiguration {

  /**
   * Configure an {@link ObjectMapper} that will fail on unknown properties. This allows the event
   * listener to convert the message to the correct type.
   *
   * @return the configured {@code ObjectMapper}
   */
  @Bean
  @Qualifier("submissionEventMapper")
  public ObjectMapper submissionEventMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    return mapper;
  }
}
