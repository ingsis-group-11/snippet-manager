package snippetmanager.model.dtos;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;
import snippetmanager.util.enums.CodeLanguage;

@Builder
@Getter
@Setter
public class SnippetReceivedDto {
  private String assetId;
  private String language;
  private String version;
  private String name;
  private String extension;
  private MultipartFile content;

  public String getContentInString() {
    try {
      byte[] bytes = content.getBytes();
      return new String(bytes, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException("Error reading file content", e);
    }
  }

  public CodeLanguage getLanguageInEnum() {
    return CodeLanguage.valueOf(language.toUpperCase());
  }
}
