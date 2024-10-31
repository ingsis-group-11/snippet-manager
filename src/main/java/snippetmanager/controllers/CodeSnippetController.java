package snippetmanager.controllers;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import snippetmanager.model.dtos.SnippetReceivedDto;
import snippetmanager.model.dtos.SnippetSendDto;
import snippetmanager.services.CodeSnippetService;

@RestController
@RequestMapping("/api/snippet")
public class CodeSnippetController {

  @Autowired private CodeSnippetService codeSnippetService;

  private String getUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Jwt jwt = (Jwt) authentication.getPrincipal();

    return jwt.getClaimAsString("sub");
  }

  // PUT http://localhost:8080/api/snippet/
  @PutMapping
  public ResponseEntity<String> createSnippet(
      @RequestParam("file") MultipartFile file,
      @RequestParam("version") String version,
      @RequestParam("name") String fileName,
      @RequestParam("language") String language) {
    SnippetReceivedDto snippet =
        SnippetReceivedDto.builder()
            .content(file)
            .assetId(fileName)
            .language(language)
            .version(version)
            .build();
    return ResponseEntity.ok(codeSnippetService.createSnippet(snippet, getUserId()));
  }

  // GET http://localhost:8080/api/snippet/{snippetId}
  @GetMapping("/{assetId}")
  public ResponseEntity<SnippetSendDto> getSnippet(@PathVariable String assetId) {
    return ResponseEntity.ok(codeSnippetService.getSnippet(assetId, getUserId()));
  }

  // GET http://localhost:8080/api/snippet/
  @GetMapping
  public ResponseEntity<List<SnippetSendDto>> getAllSnippets() {
    return ResponseEntity.ok(codeSnippetService.getAllSnippets(getUserId()));
  }

  // PUT http://localhost:8080/api/snippet/{snippetId}
  @PutMapping("/{snippetId}")
  public ResponseEntity<String> updateSnippet(
      @PathVariable String snippetId,
      @RequestParam("file") MultipartFile file,
      @RequestParam("name") String fileName,
      @RequestParam("version") String version,
      @RequestParam("language") String language) {
    SnippetReceivedDto snippet =
        SnippetReceivedDto.builder()
            .content(file)
            .assetId(fileName)
            .language(language)
            .version(version)
            .build();
    return ResponseEntity.ok(codeSnippetService.updateSnippet(snippetId, getUserId(), snippet));
  }

  // DELETE http://localhost:8080/api/snippet/{snippetId}
  @DeleteMapping("/{snippetId}")
  public ResponseEntity<String> deleteSnippet(@PathVariable String snippetId) {
    return ResponseEntity.ok(codeSnippetService.deleteSnippet(snippetId, getUserId()));
  }
}