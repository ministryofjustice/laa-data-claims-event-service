package uk.gov.justice.laa.dstew.payments.claimsevent.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.BulkSubmissionResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.BulkParsingService;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1")
public class TestController {

  private final BulkParsingService bulkParsingService;

  @GetMapping(path = "/test")
  public ResponseEntity<Void> createBulkSubmission() throws IOException {
    ObjectMapper objectMapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    String json = Files.readString(
        Path.of("src/test/resources/bulk-submission-response.json")
    );

    BulkSubmissionResponse bulkSubmission =
        objectMapper.readValue(json, BulkSubmissionResponse.class);

    UUID submissionId = UUID.randomUUID();

    // Get the bean from Spring context instead of using @Autowired
    bulkParsingService.parseData(bulkSubmission, submissionId);

    // Return response entity
    return ResponseEntity.noContent().build();
  }




}
