package snippet_manager.snippet.services;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.multipart.MultipartFile;
import snippet_manager.snippet.model.dtos.CodeSnippetDTO;
import snippet_manager.snippet.model.entities.CodeSnippet;
import snippet_manager.snippet.webservice.asset.AssetManager;
import snippet_manager.snippet.webservice.permission.PermissionManager;
import snippet_manager.snippet.repositories.CodeSnippetRepository;
import snippet_manager.snippet.webservice.printscript.PrintscriptManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CodeSnippetService {
  @Autowired
  private CodeSnippetRepository codeSnippetRepository;

  @Autowired
  private PermissionManager permissionManager;

  @Autowired
  private PrintscriptManager printscriptManager;

  @Autowired
  private AssetManager assetManager;

  public String createSnippet(CodeSnippetDTO snippet, Long userId) throws IOException {
    compileSnippet(snippet);
    CodeSnippet codeSnippet = new CodeSnippet();
    codeSnippet.setAssetId(snippet.getAssetId());
    codeSnippet.setLanguage(snippet.getLanguageInEnum());
    codeSnippet.setVersion(snippet.getVersion());
    codeSnippetRepository.save(codeSnippet);

    ResponseEntity<String> permissionResponse = createNewPermission(userId, codeSnippet.getId());
    if (permissionResponse.getStatusCode().isError()) {
      codeSnippetRepository.deleteById(codeSnippet.getId());
      throw new HttpServerErrorException(permissionResponse.getStatusCode());
    }

    ResponseEntity assetResponse = assetManager.createAsset("snippets", snippet.getAssetId(), snippet.getContent());
    if (assetResponse.getStatusCode().isError()) {
      codeSnippetRepository.deleteById(codeSnippet.getId());
      throw new HttpServerErrorException(assetResponse.getStatusCode());
    }

    return "Snippet created successfully";
  }

  private void compileSnippet(CodeSnippetDTO snippet) {
    ResponseEntity<String> compileSnippetResponse = printscriptManager.compile(snippet.getContentInString(), snippet.getLanguageInEnum(), snippet.getVersion());
    if (compileSnippetResponse.getStatusCode().isError()) {
      throw new HttpServerErrorException(compileSnippetResponse.getStatusCode());
    }
  }

  public CodeSnippetDTO getSnippet(UUID snippetId, Long userId) throws IOException {
    boolean canAccess = canReadSnippet(userId, snippetId);
    if (!canAccess) {
      throw new PermissionDeniedDataAccessException("You don't have permission to access this snippet", new Exception("You don't have permission to access this snippet"));
    }
    CodeSnippet codeSnippet = codeSnippetRepository.findById(snippetId)
            .orElseThrow(() -> new EntityNotFoundException("Snippet not found with id " + snippetId));

    InputStream assetResponse = assetManager.getAsset("snippets", codeSnippet.getAssetId());
    MultipartFile asset = toMultipartFile(assetResponse, codeSnippet.getAssetId());
    return convertToDTO(codeSnippet, asset);
  }

  private MultipartFile toMultipartFile(InputStream assetResponse, String fileName) throws IOException {
    byte[] bytes = assetResponse.readAllBytes();
    return new MockMultipartFile(fileName, fileName, "text/plain" , bytes);
  }

  public List<CodeSnippetDTO> getAllSnippets(Long userId) {
    List<CodeSnippet> codeSnippets = codeSnippetRepository.findAll();

    List<CodeSnippetDTO> codeSnippetDTOS = codeSnippets.stream()
            .filter(codeSnippet -> canReadSnippet(userId, codeSnippet.getId()))
            .map(codeSnippet -> {
              try {
                InputStream assetResponse = assetManager.getAsset("snippets", codeSnippet.getAssetId());
                MultipartFile asset = toMultipartFile(assetResponse, codeSnippet.getAssetId());

                return convertToDTO(codeSnippet, asset);
              } catch (IOException e) {
                throw new RuntimeException("Error reading the file content: " + codeSnippet.getId(), e);
              }
            })
            .collect(Collectors.toList());

    return codeSnippetDTOS;
  }

  public String updateSnippet(UUID snippetId, Long userId, CodeSnippetDTO codeSnippet) throws IOException {
    boolean canAccess = canWriteSnippet(userId, snippetId);
    if (!canAccess) {
      throw new PermissionDeniedDataAccessException("You don't have permission to access this snippet", new Exception("You don't have permission to access this snippet"));
    }
    CodeSnippet existingCodeSnippet = codeSnippetRepository.findById(snippetId)
            .orElseThrow(() -> new EntityNotFoundException("Snippet not found with id " + snippetId));

    ResponseEntity<?> assetResponse = assetManager.createAsset("snippets", existingCodeSnippet.getAssetId(), codeSnippet.getContent());
    if (assetResponse.getStatusCode().isError()) {
      throw new HttpServerErrorException(assetResponse.getStatusCode());
    }
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

    ResponseEntity<?> assetResponse = assetManager.deleteAsset("snippets", snippetId.toString());
    if (assetResponse.getStatusCode().isError()) {
      throw new HttpServerErrorException(assetResponse.getStatusCode());
    }
    codeSnippetRepository.deleteById(snippetId);
    return "Snippet deleted successfully";
  }

  //Internal methods
  private CodeSnippetDTO convertToDTO(CodeSnippet codeSnippets, MultipartFile content) {
    return CodeSnippetDTO.builder()
            .language(codeSnippets.getLanguage().name())
            .version(codeSnippets.getVersion())
            .content(content)
            .build();
  }

  private ResponseEntity<String> createNewPermission(Long userId, UUID snippetId) {
    return permissionManager.createNewPermission(userId, snippetId);
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
