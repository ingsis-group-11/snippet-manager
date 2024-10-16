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
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CodeSnippetService {
  @Autowired
  private CodeSnippetRepository codeSnippetRepository;

  @Autowired
  private PermissionManager permissionManager;

  public String createSnippet(CodeSnippetDTO snippet, Long userId) {
    CodeSnippet codeSnippet = new CodeSnippet();
    codeSnippet.setTitle(snippet.getTitle());
    codeSnippet.setContent(snippet.getContentInString());
    codeSnippet.setLanguage(snippet.getLanguageInEnum());
    codeSnippet.setVersion(snippet.getVersion());
    codeSnippetRepository.save(codeSnippet);
    ResponseEntity<String> permissionResponse = createNewPermission(userId, codeSnippet);
    if (permissionResponse.getStatusCode().isError()) {
      throw new HttpServerErrorException(permissionResponse.getStatusCode());
    }
    return "Snippet created successfully";
  }

  public CodeSnippetDTO getSnippet(UUID snippetId, Long userId) {
    boolean canAccess = canReadSnippet(userId, snippetId);
    if (!canAccess) {
      throw new PermissionDeniedDataAccessException("You don't have permission to access this snippet", new Exception("You don't have permission to access this snippet"));
    }
    CodeSnippet codeSnippet = codeSnippetRepository.findById(snippetId)
            .orElseThrow(() -> new EntityNotFoundException("Snippet not found with id " + snippetId));

    return CodeSnippetDTO.builder()
        .title(codeSnippet.getTitle())
        .language(codeSnippet.getLanguage().name())
        .content(codeSnippet.getContentInMultipartFile())
        .version(codeSnippet.getVersion())
        .build();
  }

  public List<CodeSnippetDTO> getAllSnippets(Long userId) {
    List<CodeSnippet> codeSnippets = codeSnippetRepository.findAll();
    for (CodeSnippet codeSnippet : codeSnippets) {
      boolean canAccess = canReadSnippet(userId, codeSnippet.getId());
      if (!canAccess) {
        codeSnippets.remove(codeSnippet);
      }
    }
    return convertToDTO(codeSnippets);
  }

  public String updateSnippet(UUID snippetId, Long userId, CodeSnippetDTO codeSnippet) {
    boolean canAccess = canWriteSnippet(userId, snippetId);
    if (!canAccess) {
      throw new PermissionDeniedDataAccessException("You don't have permission to access this snippet", new Exception("You don't have permission to access this snippet"));
    }
    CodeSnippet existingCodeSnippet = codeSnippetRepository.findById(snippetId)
            .orElseThrow(() -> new EntityNotFoundException("Snippet not found with id " + snippetId));
    existingCodeSnippet.setTitle(codeSnippet.getTitle());
    existingCodeSnippet.setContent(codeSnippet.getContentInString());
    existingCodeSnippet.setLanguage(codeSnippet.getLanguageInEnum());
    existingCodeSnippet.setVersion(codeSnippet.getVersion());
    codeSnippetRepository.save(existingCodeSnippet);
    return "Snippet updated successfully";
  }

  public String deleteSnippet(UUID snippetId, Long userId) {
    boolean canAccess = canDeleteSnippet(userId, snippetId);
    if (!canAccess) {
      throw new PermissionDeniedDataAccessException("You don't have permission to access this snippet", new Exception("You don't have permission to access this snippet"));
    }
    codeSnippetRepository.findById(snippetId)
            .orElseThrow(() -> new EntityNotFoundException("Snippet not found with id " + snippetId));

    codeSnippetRepository.deleteById(snippetId);
    return "Snippet deleted successfully";
  }

  //Internal methods

  private List<CodeSnippetDTO> convertToDTO(List<CodeSnippet> codeSnippets) {
    return codeSnippets.stream()
            .map(codeSnippet -> CodeSnippetDTO.builder()
                    .title(codeSnippet.getTitle())
                    .language(codeSnippet.getLanguage().name())
                    .content(codeSnippet.getContentInMultipartFile())
                    .version(codeSnippet.getVersion())
                    .build())
            .collect(Collectors.toList());
  }

  private ResponseEntity<String> createNewPermission(Long userId, CodeSnippet codeSnippet) {
    return permissionManager.createNewPermission(userId, codeSnippet.getId());
  }

  private boolean canReadSnippet(Long userId, UUID snippetId) {
    return permissionManager.canRead(userId, snippetId);
  }

  private boolean canWriteSnippet(Long userId, UUID snippetId) {
    return permissionManager.canWrite(userId, snippetId);
  }

  private boolean canDeleteSnippet(Long userId, UUID snippetId) {
    return permissionManager.canDelete(userId, snippetId);
  }
}
