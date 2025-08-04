package uk.gov.justice.laa.bulk.claim.converter;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.justice.laa.bulk.claim.exception.BulkClaimFileReadException;
import uk.gov.justice.laa.bulk.claim.model.FileExtension;
import uk.gov.justice.laa.bulk.claim.model.xml.XmlSubmission;

/** Converter responsible for converting bulk claim submissions in XML format. */
@Slf4j
@Component
public class BulkClaimXmlConverter implements BulkClaimConverter {

  private final XmlMapper xmlMapper;

  @Autowired
  public BulkClaimXmlConverter(XmlMapper xmlMapper) {
    this.xmlMapper = xmlMapper;
  }

  /**
   * Converts the given file to a {@link XmlSubmission} object.
   *
   * @param file the input file
   * @return the {@link XmlSubmission} object.
   */
  @Override
  public XmlSubmission convert(MultipartFile file) {
    XmlSubmission submission;

    try {
      submission = xmlMapper.readValue(file.getInputStream(), XmlSubmission.class);
    } catch (IOException e) {
      throw new BulkClaimFileReadException("Failed to read xml bulk claim file", e);
    }

    return submission;
  }

  /**
   * Determines whether this converter handles the given {@link FileExtension}.
   *
   * @param fileExtension the file extension to check
   * @return true if the converter can handle files with the given extension, false otherwise
   */
  @Override
  public boolean handles(FileExtension fileExtension) {
    return FileExtension.XML.equals(fileExtension);
  }
}
