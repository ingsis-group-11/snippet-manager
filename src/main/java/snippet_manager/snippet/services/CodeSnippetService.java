package snippet_manager.snippet.services;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import snippet_manager.snippet.model.dtos.CodeSnippetDTO;
import snippet_manager.snippet.model.entities.CodeSnippet;
import snippet_manager.snippet.permission.PermissionManager;
import snippet_manager.snippet.repositories.CodeSnippetRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CodeSnippetService {
  @Autowired
  private CodeSnippetRepository codeSnippetRepository;

  public String createSnippet(CodeSnippetDTO snippet) {
    CodeSnippet codeSnippet = new CodeSnippet();
    codeSnippet.setTitle(snippet.getTitle());
    codeSnippet.setContent(snippet.getContentInString());
    codeSnippet.setLanguage(snippet.getLanguageInEnum());
    codeSnippetRepository.save(codeSnippet);
    ResponseEntity<String> permissionResponse = new PermissionManager().newPermission(codeSnippet.getId(), codeSnippet.getId());
    if (permissionResponse.getStatusCode().isError()) {
      throw new HttpServerErrorException(permissionResponse.getStatusCode());
    }
    return "Snippet created successfully";
  }

  public CodeSnippetDTO getSnippet(Long snippetId, Long userId) {
    boolean canAccess = new PermissionManager().canAccess(userId, snippetId);
    if (!canAccess) {
      throw new PermissionDeniedDataAccessException("You don't have permission to access this snippet", new Exception("You don't have permission to access this snippet"));
    }
    CodeSnippet codeSnippet = codeSnippetRepository.findById(snippetId)
            .orElseThrow(() -> new EntityNotFoundException("Snippet not found with id " + snippetId));

    return CodeSnippetDTO.builder()
        .title(codeSnippet.getTitle())
        .language(codeSnippet.getLanguage().name())
        .content(codeSnippet.getContentInMultipartFile())
        .build();
  }

  public List<CodeSnippetDTO> getAllSnippets(Long userId) {
    List<CodeSnippet> codeSnippets = codeSnippetRepository.findAll();
    for (CodeSnippet codeSnippet : codeSnippets) {
      boolean canAccess = new PermissionManager().canAccess(userId, codeSnippet.getId());
      if (!canAccess) {
        codeSnippets.remove(codeSnippet);
      }
    }
    return convertToDTO(codeSnippets);
  }

  private List<CodeSnippetDTO> convertToDTO(List<CodeSnippet> codeSnippets) {
    return codeSnippets.stream()
        .map(codeSnippet -> CodeSnippetDTO.builder()
            .title(codeSnippet.getTitle())
            .language(codeSnippet.getLanguage().name())
            .content(codeSnippet.getContentInMultipartFile())
            .build())
        .collect(Collectors.toList());
  }

  public String updateSnippet(Long snippetId, Long userId, CodeSnippetDTO codeSnippet) {
    boolean canAccess = new PermissionManager().canAccess(userId, snippetId);
    if (!canAccess) {
      throw new PermissionDeniedDataAccessException("You don't have permission to access this snippet", new Exception("You don't have permission to access this snippet"));
    }
    CodeSnippet existingCodeSnippet = codeSnippetRepository.findById(snippetId)
            .orElseThrow(() -> new EntityNotFoundException("Snippet not found with id " + snippetId));
    existingCodeSnippet.setTitle(codeSnippet.getTitle());
    existingCodeSnippet.setContent(codeSnippet.getContentInString());
    existingCodeSnippet.setLanguage(codeSnippet.getLanguageInEnum());
    codeSnippetRepository.save(existingCodeSnippet);
    return "Snippet updated successfully";
  }

  public String deleteSnippet(Long snippetId, Long userId) {
    boolean canAccess = new PermissionManager().canAccess(userId, snippetId);
    if (!canAccess) {
      throw new PermissionDeniedDataAccessException("You don't have permission to access this snippet", new Exception("You don't have permission to access this snippet"));
    }
    codeSnippetRepository.findById(snippetId)
            .orElseThrow(() -> new EntityNotFoundException("Snippet not found with id " + snippetId));

    codeSnippetRepository.deleteById(snippetId);
    return "Snippet deleted successfully";
  }
}
