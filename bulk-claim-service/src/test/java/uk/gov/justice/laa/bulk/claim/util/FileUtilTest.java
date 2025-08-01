package uk.gov.justice.laa.bulk.claim.util;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import uk.gov.justice.laa.bulk.claim.exception.BulkClaimFileReadException;

@DisplayName("File util test")
class FileUtilTest {

  @Test
  @DisplayName("Should create temporary file")
  void shouldCreateTemporaryFile() {
    // Given
    MockMultipartFile mockMultipartFile =
        new MockMultipartFile(
            "test-file", "test-file.txt", "text/plain", "This is a test file".getBytes());

    // When
    File tempFile = FileUtil.createTempFile(mockMultipartFile);

    // Then
    assertThat(tempFile).isNotNull();
    assertThat(tempFile).exists();
    assertThat(tempFile.getName()).contains("upload-");
    assertThat(tempFile).hasSize(mockMultipartFile.getSize());
  }

  @Test
  @DisplayName("Should throw BulkClaimFileReadException")
  void shouldThrowException() throws IOException {
    // Given
    MockMultipartFile mockMultipartFile =
        new MockMultipartFile("test-file", "test-file.txt", "text/plain", new byte[0]);

    MockMultipartFile spyMultipartFile = spy(mockMultipartFile);
    doThrow(IOException.class).when(spyMultipartFile).transferTo(any(File.class));

    // Then
    assertThatThrownBy(() -> FileUtil.createTempFile(spyMultipartFile))
        .isInstanceOf(BulkClaimFileReadException.class)
        .hasMessageContaining("Could not open file");
  }
}
