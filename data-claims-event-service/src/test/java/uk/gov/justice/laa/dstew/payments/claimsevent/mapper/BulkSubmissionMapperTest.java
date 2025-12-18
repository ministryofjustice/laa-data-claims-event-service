package uk.gov.justice.laa.dstew.payments.claimsevent.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw.CRIME_LOWER;
import static uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw.LEGAL_HELP;
import static uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw.MEDIATION;
import static uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationSource.EVENT_SERVICE;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.*;

class BulkSubmissionMapperTest {

  private BulkSubmissionMapper mapper;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    mapper = new BulkSubmissionMapperImpl();
    objectMapper =
        new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  @ParameterizedTest(name = "AreaOfLaw: {0}")
  @EnumSource(AreaOfLaw.class)
  void shouldMapBulkSubmissionResponseToSubmissionPost(AreaOfLaw areaOfLaw) throws IOException {
    // Arrange
    String json = Files.readString(Path.of("src/test/resources/bulk-submission-response.json"));
    String expectedRef = "2Q286D/SCH";

    GetBulkSubmission200Response bulkSubmission =
        objectMapper.readValue(json, GetBulkSubmission200Response.class);
    bulkSubmission.getDetails().getSchedule().setAreaOfLaw(areaOfLaw.toString());
    bulkSubmission.getDetails().getSchedule().setScheduleNum(expectedRef);
    UUID submissionId = UUID.randomUUID();

    SubmissionPost result = mapper.mapToSubmissionPost(bulkSubmission, submissionId);

    assertThat(result).isNotNull();
    assertThat(result.getSubmissionId()).isEqualTo(submissionId);

    assertThat(result.getLegalHelpSubmissionReference())
        .isEqualTo(areaOfLaw == LEGAL_HELP ? expectedRef : null);
    assertThat(result.getCrimeLowerScheduleNumber())
        .isEqualTo(areaOfLaw == CRIME_LOWER ? expectedRef : null);
    assertThat(result.getMediationSubmissionReference())
        .isEqualTo(areaOfLaw == MEDIATION ? expectedRef : null);

    assertThat(result.getBulkSubmissionId())
        .isEqualTo(UUID.fromString("c4ef2c36-35dc-4627-b5d8-7db10d86be3d"));
    assertThat(result.getNumberOfClaims()).isEqualTo(1);
    assertThat(result.getIsNilSubmission()).isFalse();
    assertThat(result.getOfficeAccountNumber()).isEqualTo("2Q286D");
    assertThat(result.getStatus())
        .isEqualTo(SubmissionStatus.CREATED); // or enum value depending on type
    assertThat(result.getProviderUserId()).isEqualTo("test123");
    assertThat(result.getCreatedByUserId()).isEqualTo(EVENT_SERVICE);
  }

  @ParameterizedTest
  @CsvSource({
    "src/test/resources/bulk-submission-response.json, LEGAL_HELP",
    "src/test/resources/bulk-submission-response-crime-lower.json, CRIME_LOWER",
    "src/test/resources/bulk-submission-response-mediation.json, MEDIATION"
  })
  void shouldMapOutcomesToClaimPostsAndApplyPostMappingAdjustments(
      String filePath, String areaOfLawName) throws IOException {
    String json = Files.readString(Path.of(filePath));
    GetBulkSubmission200Response bulkSubmission =
        objectMapper.readValue(json, GetBulkSubmission200Response.class);
    AreaOfLaw areaOfLaw = AreaOfLaw.valueOf(areaOfLawName);

    var claims = mapper.mapToClaimPosts(bulkSubmission.getDetails().getOutcomes(), areaOfLaw);

    assertThat(claims).hasSize(1);
    ClaimPost claim = claims.getFirst();
    assertThat(claim.getStatus()).isEqualTo(ClaimStatus.READY_TO_PROCESS);
    BulkSubmissionOutcome bulkSubmissionOutcome =
        bulkSubmission.getDetails().getOutcomes().getFirst();
    switch (areaOfLaw) {
      case LEGAL_HELP -> {
        assertThat(claim.getCaseConcludedDate()).isEqualTo("2023-09-30");
        assertThat(claim.getStageReachedCode()).isEqualTo("PROK");
        assertThat(claim.getStandardFeeCategoryCode()).isEqualTo("1A-LSF");
        assertThat(claim.getMatterTypeCode()).isEqualTo("FAMX:FAPP");
        assertThat(claim.getIsVatApplicable()).isFalse();
        assertThat(claim.getNetWaitingCostsAmount()).isNull();
        assertThat(claim.getMedicalReportsCount()).isEqualTo(3);
        assertThat(claim.getSurgeryClientsCount()).isEqualTo(4);
        assertThat(claim.getSurgeryMattersCount()).isEqualTo(2);
        assertThat(claim.getDetentionTravelWaitingCostsAmount()).isEqualByComparingTo("12");
      }
      case CRIME_LOWER -> {
        assertThat(claim.getCaseConcludedDate()).isEqualTo("2023-09-30");
        assertThat(claim.getStageReachedCode()).isEqualTo("FAMX");
        assertThat(claim.getTravelWaitingCostsAmount())
            .isEqualTo(bulkSubmissionOutcome.getTravelCosts());
        assertThat(claim.getNetWaitingCostsAmount())
            .isEqualTo(bulkSubmissionOutcome.getTravelWaitingCosts());
        assertThat(claim.getIsVatApplicable()).isTrue();
      }
      case MEDIATION -> {
        assertThat(claim.getCaseConcludedDate()).isEqualTo("2024-09-30");
        assertThat(claim.getStageReachedCode()).isEqualTo("PROK");
      }
    }
    assertThat(claim.getCaseStageCode()).isEqualTo("FPC01");
    assertThat(claim.getUniqueFileNumber()).isEqualTo("220422/013");
    assertThat(claim.getCaseReferenceNumber()).isEqualTo("JI/OKUSU");
    assertThat(claim.getScheduleReference()).isEqualTo("01/2Q286D/2024/01");
    assertThat(claim.getCreatedByUserId()).isEqualTo(EVENT_SERVICE);
  }

  @Test
  void shouldMapBulkSubmissionResponseToMatterStarts() throws IOException {
    String json = Files.readString(Path.of("src/test/resources/bulk-submission-response-3.json"));
    GetBulkSubmission200Response bulkSubmission =
        objectMapper.readValue(json, GetBulkSubmission200Response.class);

    var matterStartPosts =
        mapper.mapToMatterStartRequests(bulkSubmission.getDetails().getMatterStarts());

    assertThat(matterStartPosts).hasSize(1);
    MatterStartPost matterStartPost = matterStartPosts.getFirst();
    assertThat(matterStartPost.getAccessPointCode()).isEqualTo("AP00137");
    assertThat(matterStartPost.getScheduleReference()).isEqualTo("0U733A/2018/02");
    assertThat(matterStartPost.getCategoryCode()).isEqualTo(CategoryCode.COM);
    assertThat(matterStartPost.getCreatedByUserId()).isEqualTo(EVENT_SERVICE);
    assertThat(matterStartPost.getDeliveryLocation()).isEqualTo("test-loc");
    assertThat(matterStartPost.getProcurementAreaCode()).isEqualTo("PA00136");
  }

  @Test
  void shouldMapValueOfCostsPaNumberAndLegalHelpTravelCosts() {
    BulkSubmissionOutcome outcome =
        new BulkSubmissionOutcome()
            .valueOfCosts(new BigDecimal("150.75"))
            .costsDamagesRecovered(new BigDecimal("999.99"))
            .paNumber("PA-123")
            .prisonLawPriorApproval("SHOULD_NOT_BE_USED")
            .travelCosts(new BigDecimal("12.50"))
            .detentionTravelWaitingCosts(new BigDecimal("1.00"))
            .travelWaitingCosts(new BigDecimal("3.25"))
            .workConcludedDate("2024-01-01");

    ClaimPost claim = mapper.mapToClaimPost(outcome, LEGAL_HELP);

    assertThat(claim.getCostsDamagesRecoveredAmount()).isEqualByComparingTo("150.75");
    assertThat(claim.getPrisonLawPriorApprovalNumber()).isEqualTo("PA-123");
    assertThat(claim.getDetentionTravelWaitingCostsAmount()).isEqualByComparingTo("12.50");
    assertThat(claim.getTravelWaitingCostsAmount()).isEqualByComparingTo("3.25");
    assertThat(claim.getCaseConcludedDate()).isEqualTo("2024-01-01");
  }

  @Test
  void stringToIntegerHandlesInvalidValues() {
    assertThat(mapper.stringToInteger("abc")).isNull();
    assertThat(mapper.stringToInteger(null)).isNull();
    assertThat(mapper.stringToInteger("7")).isEqualTo(7);
  }
}
