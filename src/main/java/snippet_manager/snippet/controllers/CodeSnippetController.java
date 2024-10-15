package snippet_manager.snippet.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import snippet_manager.snippet.model.dtos.CodeSnippetDTO;
import snippet_manager.snippet.services.CodeSnippetService;

import java.util.List;

@RestController
@RequestMapping("/api/snippet")
public class CodeSnippetController {

  @Autowired
  private CodeSnippetService codeSnippetService;

  //POST http://localhost:8080/api/snippet
  @PostMapping
  public ResponseEntity<String> createSnippet(@RequestBody CodeSnippetDTO snippet) {
    return ResponseEntity.ok(codeSnippetService.createSnippet(snippet));
  }

  //GET http://localhost:8080/api/snippet?userId=1&snippetId=42
  @GetMapping("/{snippetId}")
  public ResponseEntity<CodeSnippetDTO> getSnippet(@PathVariable Long snippetId, @RequestParam Long userId) {
    return ResponseEntity.ok(codeSnippetService.getSnippet(userId, snippetId));
  }

  //GET http://localhost:8080/api/snippet/1
  @GetMapping("/getAllSnippets/{userId}")
  public ResponseEntity<List<CodeSnippetDTO>> getAllSnippets(@PathVariable Long userId) {
    return ResponseEntity.ok(codeSnippetService.getAllSnippets(userId));
  }

  //PUT http://localhost:8080/api/snippet/42?userId=1
  @PutMapping("/{snippetId}")
  public ResponseEntity<String> updateSnippet(@PathVariable Long snippetId, @RequestParam Long userId, @RequestBody CodeSnippetDTO codeSnippet) {
    return ResponseEntity.ok(codeSnippetService.updateSnippet(snippetId, userId, codeSnippet));
  }

  //DELETE http://localhost:8080/api/snippet/42?userId=1
  @DeleteMapping("/{snippetId}")
  public ResponseEntity<String> deleteSnippet(@PathVariable Long snippetId, @RequestParam Long userId) {
    return ResponseEntity.ok(codeSnippetService.deleteSnippet(snippetId, userId));
  }
}

