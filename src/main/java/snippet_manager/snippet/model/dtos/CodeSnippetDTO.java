package snippet_manager.snippet.model.dtos;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;
import snippet_manager.snippet.util.CodeLanguage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Builder
@Getter
@Setter
public class CodeSnippetDTO {
  private String title;
  private String language;
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
