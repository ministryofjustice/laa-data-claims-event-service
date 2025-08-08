package uk.gov.justice.laa.dstew.payments.claimsevent.mapper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.BulkClaimMatterStarts;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.BulkClaimOffice;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.BulkClaimOutcome;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.BulkClaimSchedule;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.BulkClaimSubmission;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.FileSubmission;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.csv.CsvMatterStarts;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.csv.CsvOffice;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.csv.CsvOutcome;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.csv.CsvSchedule;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.csv.CsvSubmission;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.xml.XmlOffice;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.xml.XmlOutcome;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.xml.XmlSchedule;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.xml.XmlSubmission;

/** Mapping interface for the mapping of bulk claim submission objects. */
@Mapper(componentModel = "spring")
public interface BulkClaimSubmissionMapper {

  /**
   * Maps the given {@code FileSubmission} to a {@code BulkClaimSubmission}.
   *
   * @param submission the java representation of the bulk claim file submission
   * @return the API model for the bulk claim submission.
   */
  default BulkClaimSubmission toBulkClaimSubmission(FileSubmission submission) {
    if (submission instanceof CsvSubmission csvSubmission) {
      return toBulkClaimSubmission(csvSubmission);
    } else if (submission instanceof XmlSubmission xmlSubmission) {
      return toBulkClaimSubmission(xmlSubmission);
    } else {
      throw new IllegalArgumentException("Unsupported submission type: " + submission.getClass());
    }
  }

  /**
   * Maps the given {@code XmlSubmission} to a {@code BulkClaimSubmission}.
   *
   * @param submission the java representation of the xml bulk claim file submission
   * @return the API model for the bulk claim submission.
   */
  @Mapping(target = "schedule", source = "office.schedule")
  @Mapping(target = "outcomes", source = "office.schedule.outcomes")
  @Mapping(target = "matterStarts", expression = "java(new ArrayList<>())")
  BulkClaimSubmission toBulkClaimSubmission(XmlSubmission submission);

  /**
   * Maps the given {@code CsvSubmission} to a {@code BulkClaimSubmission}.
   *
   * @param submission the java representation of the csv bulk claim file submission
   * @return the API model for the bulk claim submission.
   */
  @Mapping(
      target = "matterStarts",
      source = "matterStarts",
      defaultExpression = "java(new ArrayList<>())")
  BulkClaimSubmission toBulkClaimSubmission(CsvSubmission submission);

  /**
   * Map to a {@link BulkClaimOffice}.
   *
   * @param office the {@link XmlOffice} to map.
   * @return a mapped {@link BulkClaimOffice} object.
   */
  BulkClaimOffice toBulkClaimOffice(XmlOffice office);

  /**
   * Map to a {@link BulkClaimOffice}.
   *
   * @param office the {@link CsvOffice} to map.
   * @return a mapped {@link BulkClaimOffice} object.
   */
  BulkClaimOffice toBulkClaimOffice(CsvOffice office);

  /**
   * Map to a {@link BulkClaimSchedule}.
   *
   * @param schedule the {@link XmlSchedule} to map.
   * @return a mapped {@link BulkClaimSchedule} object.
   */
  BulkClaimSchedule toBulkClaimSchedule(XmlSchedule schedule);

  /**
   * Map to a {@link BulkClaimSchedule}.
   *
   * @param schedule the {@link CsvSchedule} to map.
   * @return a mapped {@link BulkClaimSchedule} object.
   */
  BulkClaimSchedule toBulkClaimSchedule(CsvSchedule schedule);

  /**
   * Map to a {@link BulkClaimOutcome}.
   *
   * @param outcome the {@link XmlOutcome} to map.
   * @return a mapped {@link BulkClaimOutcome} object.
   */
  @Mapping(
      target = "caseStartDate",
      source = "caseStartDate",
      qualifiedByName = "outcomeFieldToLocalDate")
  @Mapping(
      target = "clientDateOfBirth",
      source = "clientDateOfBirth",
      qualifiedByName = "outcomeFieldToLocalDate")
  @Mapping(
      target = "workConcludedDate",
      source = "workConcludedDate",
      qualifiedByName = "outcomeFieldToLocalDate")
  @Mapping(
      target = "transferDate",
      source = "transferDate",
      qualifiedByName = "outcomeFieldToLocalDate")
  @Mapping(
      target = "surgeryDate",
      source = "surgeryDate",
      qualifiedByName = "outcomeFieldToLocalDate")
  @Mapping(
      target = "repOrderDate",
      source = "repOrderDate",
      qualifiedByName = "outcomeFieldToLocalDate")
  @Mapping(
      target = "client2DateOfBirth",
      source = "client2DateOfBirth",
      qualifiedByName = "outcomeFieldToLocalDate")
  @Mapping(
      target = "medConcludedDate",
      source = "medConcludedDate",
      qualifiedByName = "outcomeFieldToLocalDate")
  @Mapping(
      target = "vatIndicator",
      source = "vatIndicator",
      qualifiedByName = "outcomeFieldToBoolean")
  @Mapping(
      target = "londonNonlondonRate",
      source = "londonNonlondonRate",
      qualifiedByName = "outcomeFieldToBoolean")
  @Mapping(
      target = "toleranceIndicator",
      source = "toleranceIndicator",
      qualifiedByName = "outcomeFieldToBoolean")
  @Mapping(target = "legacyCase", source = "legacyCase", qualifiedByName = "outcomeFieldToBoolean")
  @Mapping(
      target = "postalApplAccp",
      source = "postalApplAccp",
      qualifiedByName = "outcomeFieldToBoolean")
  @Mapping(
      target = "substantiveHearing",
      source = "substantiveHearing",
      qualifiedByName = "outcomeFieldToBoolean")
  @Mapping(
      target = "additionalTravelPayment",
      source = "additionalTravelPayment",
      qualifiedByName = "outcomeFieldToBoolean")
  BulkClaimOutcome toBulkClaimOutcome(XmlOutcome outcome);

  /**
   * Map to a {@link BulkClaimOutcome}.
   *
   * @param outcome the {@link CsvOutcome} to map.
   * @return a mapped {@link BulkClaimOutcome} object.
   */
  @Mapping(
      target = "caseStartDate",
      source = "caseStartDate",
      qualifiedByName = "outcomeFieldToLocalDate")
  @Mapping(
      target = "clientDateOfBirth",
      source = "clientDateOfBirth",
      qualifiedByName = "outcomeFieldToLocalDate")
  @Mapping(
      target = "workConcludedDate",
      source = "workConcludedDate",
      qualifiedByName = "outcomeFieldToLocalDate")
  @Mapping(
      target = "transferDate",
      source = "transferDate",
      qualifiedByName = "outcomeFieldToLocalDate")
  @Mapping(
      target = "surgeryDate",
      source = "surgeryDate",
      qualifiedByName = "outcomeFieldToLocalDate")
  @Mapping(
      target = "repOrderDate",
      source = "repOrderDate",
      qualifiedByName = "outcomeFieldToLocalDate")
  @Mapping(
      target = "client2DateOfBirth",
      source = "client2DateOfBirth",
      qualifiedByName = "outcomeFieldToLocalDate")
  @Mapping(
      target = "medConcludedDate",
      source = "medConcludedDate",
      qualifiedByName = "outcomeFieldToLocalDate")
  @Mapping(
      target = "vatIndicator",
      source = "vatIndicator",
      qualifiedByName = "outcomeFieldToBoolean")
  @Mapping(
      target = "londonNonlondonRate",
      source = "londonNonlondonRate",
      qualifiedByName = "outcomeFieldToBoolean")
  @Mapping(
      target = "toleranceIndicator",
      source = "toleranceIndicator",
      qualifiedByName = "outcomeFieldToBoolean")
  @Mapping(target = "legacyCase", source = "legacyCase", qualifiedByName = "outcomeFieldToBoolean")
  @Mapping(
      target = "postalApplAccp",
      source = "postalApplAccp",
      qualifiedByName = "outcomeFieldToBoolean")
  @Mapping(
      target = "substantiveHearing",
      source = "substantiveHearing",
      qualifiedByName = "outcomeFieldToBoolean")
  @Mapping(
      target = "additionalTravelPayment",
      source = "additionalTravelPayment",
      qualifiedByName = "outcomeFieldToBoolean")
  BulkClaimOutcome toBulkClaimOutcome(CsvOutcome outcome);

  /**
   * Map to a {@link BulkClaimMatterStarts}.
   *
   * @param csvMatterStarts the {@link CsvMatterStarts} to map.
   * @return a mapped {@link BulkClaimMatterStarts} object.
   */
  BulkClaimMatterStarts toBulkClaimMatterStarts(CsvMatterStarts csvMatterStarts);

  /**
   * Map to a {@link Boolean}.
   *
   * @param outcomeField the outcome field to map.
   * @return a boolean representation of the outcome field.
   */
  @Named("outcomeFieldToBoolean")
  default Boolean toBoolean(String outcomeField) {
    if (outcomeField == null) {
      return null;
    }
    return "Y".equals(outcomeField);
  }

  /**
   * Map to a {@link LocalDate}.
   *
   * @param outcomeField the outcome field to map.
   * @return a local date representation of the outcome field.
   */
  @Named("outcomeFieldToLocalDate")
  default LocalDate toLocalDate(String outcomeField) {
    if (outcomeField == null) {
      return null;
    }
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    return LocalDate.parse(outcomeField, formatter);
  }
}
