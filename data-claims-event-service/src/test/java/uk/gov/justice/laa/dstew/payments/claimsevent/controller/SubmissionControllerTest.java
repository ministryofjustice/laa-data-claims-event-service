package uk.gov.justice.laa.dstew.payments.claimsevent.controller;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import uk.gov.justice.laa.bulk.claim.model.SubmissionResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.BulkClaimValidationException;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.GlobalExceptionHandler;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.BulkClaimService;
import uk.gov.justice.laa.dstew.payments.claimsevent.validator.BulkClaimFileValidator;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@WebAppConfiguration
@DisplayName("Submission Controller Test")
class SubmissionControllerTest {

  @InjectMocks private SubmissionController submissionController;

  @Mock private BulkClaimService bulkClaimService;

  @Mock private BulkClaimFileValidator bulkClaimFileValidator;

  protected MockMvcTester mockMvc;

  protected MockMultipartFile mockMultipartFile;

  @BeforeEach
  void setup() {
    MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter =
        new MappingJackson2HttpMessageConverter();
    mappingJackson2HttpMessageConverter.setObjectMapper(new ObjectMapper());
    mockMvc =
        MockMvcTester.create(
                standaloneSetup(submissionController)
                    .setControllerAdvice(new GlobalExceptionHandler())
                    .build())
            .withHttpMessageConverters(singletonList(mappingJackson2HttpMessageConverter));
    mockMultipartFile =
        new MockMultipartFile("test-file", "test-file.csv", "text/csv", "one,two".getBytes());
  }

  @Nested
  @DisplayName("POST: /api/v1/submissions")
  class PostSubmissionTests {

    @Test
    @DisplayName("Should return 201 response")
    void shouldReturn201Response() throws IOException {
      SubmissionResponse expected = new SubmissionResponse("1234567890");

      when(bulkClaimService.submitBulkClaim(any(), any())).thenReturn(expected);

      // Perform POST with multipart file
      assertThat(
              mockMvc.perform(
                  multipart("/api/v1/submissions?userId=12345")
                      .file("file", mockMultipartFile.getBytes())))
          .hasStatus(201)
          .hasHeader("Location", "http://localhost/api/v1/submissions/1234567890")
          .bodyJson()
          .convertTo(SubmissionResponse.class)
          .isEqualTo(expected);
    }

    @Test
    @DisplayName("Should return 400 response")
    void shouldReturn400Response() throws IOException {
      doThrow(new BulkClaimValidationException("This error was found"))
          .when(bulkClaimFileValidator)
          .validate(any(MockMultipartFile.class));
      // Perform POST with multipart file
      assertThat(
              mockMvc.perform(
                  multipart("/api/v1/submissions")
                      .file("file", mockMultipartFile.getBytes())
                      .param("userId", "12345")))
          .hasStatus(400)
          .bodyText()
          .isEqualTo("This error was found");
    }

    @Test
    @DisplayName("Should return 400 response when user ID is missing")
    void shouldReturn400ResponseWhenUserIdIsMissing() throws IOException {
      doThrow(new IllegalArgumentException("This error was found"))
          .when(bulkClaimFileValidator)
          .validate(any(MockMultipartFile.class));
      // Perform POST with multipart file
      assertThat(
              mockMvc.perform(
                  multipart("/api/v1/submissions").file("file", mockMultipartFile.getBytes())))
          .hasStatus(400)
          .bodyText()
          .contains("Required parameter 'userId' is not present.");
    }

    @Test
    @DisplayName("Should return 415 response when file is missing")
    void shouldReturn415ResponseWhenFileIsMissing() {
      doThrow(new IllegalArgumentException("This error was found"))
          .when(bulkClaimFileValidator)
          .validate(any(MockMultipartFile.class));
      // Perform POST with multipart file
      assertThat(mockMvc.perform(post("/api/v1/submissions").param("userId", "12345")))
          .hasStatus(415);
    }
  }
}
