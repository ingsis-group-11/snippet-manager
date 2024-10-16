package snippet_manager.snippet.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import snippet_manager.snippet.model.dtos.CodeSnippetDTO;
import snippet_manager.snippet.services.CodeSnippetService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/snippet")
public class CodeSnippetController {

  @Autowired
  private CodeSnippetService codeSnippetService;

  //POST http://localhost:8080/api/snippet
  @PostMapping
  public ResponseEntity<String> createSnippet(@RequestParam("file") MultipartFile file,
                                              @RequestParam("version") String version,
                                              @RequestParam("title") String title,
                                              @RequestParam("language") String language,
                                              @RequestParam("userId") Long userId) {
    CodeSnippetDTO snippet = CodeSnippetDTO.builder()
            .title(title)
            .content(file)
            .language(language)
            .version(version)
            .build();
    return ResponseEntity.ok(codeSnippetService.createSnippet(snippet, userId));
  }

  //GET http://localhost:8080/api/snippet/{snippetId}/user/{userId}
  @GetMapping("/{snippetId}/user/{userId}")
  public ResponseEntity<CodeSnippetDTO> getSnippet(@PathVariable UUID snippetId, @RequestParam Long userId) {
    return ResponseEntity.ok(codeSnippetService.getSnippet(snippetId, userId));
  }

  //GET http://localhost:8080/api/snippet/user/{userId}
  @GetMapping("/user/{userId}")
  public ResponseEntity<List<CodeSnippetDTO>> getAllSnippets(@PathVariable Long userId) {
    return ResponseEntity.ok(codeSnippetService.getAllSnippets(userId));
  }

  //PUT http://localhost:8080/api/snippet/42?userId=1
  @PutMapping("/{snippetId}")
  public ResponseEntity<String> updateSnippet(@PathVariable UUID snippetId,
                                              @RequestParam("file") MultipartFile file,
                                              @RequestParam("title") String title,
                                              @RequestParam("version") String version,
                                              @RequestParam("language") String language,
                                              @RequestParam("userId") Long userId) {
    CodeSnippetDTO snippet = CodeSnippetDTO.builder()
            .title(title)
            .content(file)
            .language(language)
            .version(version)
            .build();
    return ResponseEntity.ok(codeSnippetService.updateSnippet(snippetId, userId, snippet));
  }

  //DELETE http://localhost:8080/api/snippet/42?userId=1
  @DeleteMapping("/{snippetId}")
  public ResponseEntity<String> deleteSnippet(@PathVariable UUID snippetId, @RequestParam Long userId) {
    return ResponseEntity.ok(codeSnippetService.deleteSnippet(snippetId, userId));
  }
}

