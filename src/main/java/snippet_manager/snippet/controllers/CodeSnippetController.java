package snippet_manager.snippet.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import snippet_manager.snippet.model.dtos.SnippetReceivedDTO;
import snippet_manager.snippet.model.dtos.SnippetSendDTO;
import snippet_manager.snippet.services.CodeSnippetService;

import java.util.List;

@RestController
@RequestMapping("/api/snippet")
public class CodeSnippetController {

  @Autowired
  private CodeSnippetService codeSnippetService;

  private String getUserId(){
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Jwt jwt = (Jwt) authentication.getPrincipal();

    return jwt.getClaimAsString("sub");
  }

  //PUT http://localhost:8080/api/snippet/
  @PutMapping
  public ResponseEntity<String> createSnippet(
                                              @RequestParam("file") MultipartFile file,
                                              @RequestParam("version") String version,
                                              @RequestParam("name") String fileName,
                                              @RequestParam("language") String language) {
    SnippetReceivedDTO snippet = SnippetReceivedDTO.builder()
            .content(file)
            .assetId(fileName)
            .language(language)
            .version(version)
            .build();
    return ResponseEntity.ok(codeSnippetService.createSnippet(snippet, getUserId()));
  }

  //GET http://localhost:8080/api/snippet/{snippetId}
  @GetMapping("/{snippetId}")
  public ResponseEntity<SnippetSendDTO> getSnippet(@PathVariable String snippetId) {
    return ResponseEntity.ok(codeSnippetService.getSnippet(snippetId, getUserId()));
  }

  //GET http://localhost:8080/api/snippet/
  @GetMapping
  public ResponseEntity<List<SnippetSendDTO>> getAllSnippets() {
    return ResponseEntity.ok(codeSnippetService.getAllSnippets(getUserId()));
  }

  //PUT http://localhost:8080/api/snippet/{snippetId}
  @PutMapping("/{snippetId}")
  public ResponseEntity<String> updateSnippet(@PathVariable String snippetId,
                                              @RequestParam("file") MultipartFile file,
                                              @RequestParam("name") String fileName,
                                              @RequestParam("version") String version,
                                              @RequestParam("language") String language) {
    SnippetReceivedDTO snippet = SnippetReceivedDTO.builder()
            .content(file)
            .assetId(fileName)
            .language(language)
            .version(version)
            .build();
    return ResponseEntity.ok(codeSnippetService.updateSnippet(snippetId, getUserId(), snippet));
  }

  //DELETE http://localhost:8080/api/snippet/{snippetId}
  @DeleteMapping("/{snippetId}")
  public ResponseEntity<String> deleteSnippet(@PathVariable String snippetId) {
    return ResponseEntity.ok(codeSnippetService.deleteSnippet(snippetId, getUserId()));
  }
}

