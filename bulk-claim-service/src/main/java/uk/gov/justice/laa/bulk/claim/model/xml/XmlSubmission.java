package uk.gov.justice.laa.bulk.claim.model.xml;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import uk.gov.justice.laa.bulk.claim.model.FileSubmission;

/**
 * Record holding bulk claim submission details sourced from an XML file.
 *
 * @param schemaLocation schema location from xml headers
 * @param office the office submitting the claim
 */
@JacksonXmlRootElement(localName = "submission")
public record XmlSubmission(
    @JacksonXmlProperty(isAttribute = true) String schemaLocation,
    @JacksonXmlProperty @JsonProperty(required = true) XmlOffice office)
    implements FileSubmission {}
