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

  public String createSnippet(CodeSnippetDTO snippet, String userId) {
    //compileSnippet(snippet);
    System.out.println(userId);
    codeSnippetRepository.findCodeSnippetByAssetId(snippet.getAssetId()).ifPresent(codeSnippet -> {
      throw new IllegalArgumentException("Snippet with the same assetId already exists");
    });
    CodeSnippet codeSnippet = createAndSaveCodeSnippet(snippet);

    /*ResponseEntity<String> permissionResponse = createNewPermission(userId, codeSnippet.getId());
    if (permissionResponse.getStatusCode().isError()) {
      codeSnippetRepository.deleteById(codeSnippet.getId());
      throw new HttpServerErrorException(permissionResponse.getStatusCode());
    }*/

    ResponseEntity<String> assetResponse = createNewAsset(snippet);
    if (assetResponse.getStatusCode().isError()) {
      codeSnippetRepository.deleteById(codeSnippet.getId());
      throw new HttpServerErrorException(assetResponse.getStatusCode());
    }

    return "Snippet created successfully";
  }

  public CodeSnippetDTO getSnippet(UUID snippetId, String userId) {
    boolean canAccess = canReadSnippet(userId, snippetId);
    if (!canAccess) {
      throw new PermissionDeniedDataAccessException("You don't have permission to access this snippet", new Exception("You don't have permission to access this snippet"));
    }
    CodeSnippet codeSnippet = findSnippet(snippetId);

    InputStream assetResponse = getAsset(codeSnippet);
    MultipartFile snippetContent = toMultipartFile(assetResponse, codeSnippet.getAssetId());
    return convertToDTO(codeSnippet, snippetContent);
  }

  public List<CodeSnippetDTO> getAllSnippets(String userId) {
    List<CodeSnippet> codeSnippets = codeSnippetRepository.findAll();

    List<CodeSnippetDTO> codeSnippetDTOS = codeSnippets.stream()
            /*.filter(codeSnippet -> canReadSnippet(userId, codeSnippet.getId()))*/
            .map(codeSnippet -> {
              InputStream assetResponse = getAsset(codeSnippet);
              MultipartFile asset = toMultipartFile(assetResponse, codeSnippet.getAssetId());

              return convertToDTO(codeSnippet, asset);
            })
            .collect(Collectors.toList());

    return codeSnippetDTOS;
  }

  public String updateSnippet(UUID snippetId, String userId, CodeSnippetDTO codeSnippet) {
    boolean canAccess = canWriteSnippet(userId, snippetId);
    if (!canAccess) {
      throw new PermissionDeniedDataAccessException("You don't have permission to write this snippet", new Exception("You don't have permission to write this snippet"));
    }
    CodeSnippet existingCodeSnippet = findSnippet(snippetId);

    ResponseEntity<?> assetResponse = assetManager.createAsset("snippets", existingCodeSnippet.getAssetId(), codeSnippet.getContent());
    if (assetResponse.getStatusCode().isError()) {
      throw new HttpServerErrorException(assetResponse.getStatusCode());
    }
    existingCodeSnippet.setLanguage(codeSnippet.getLanguageInEnum());
    existingCodeSnippet.setVersion(codeSnippet.getVersion());
    codeSnippetRepository.save(existingCodeSnippet);
    return "Snippet updated successfully";
  }

  public String deleteSnippet(UUID snippetId, String userId) {
    boolean canAccess = canDeleteSnippet(userId, snippetId);
    if (!canAccess) {
      throw new PermissionDeniedDataAccessException("You don't have permission to access this snippet", new Exception("You don't have permission to access this snippet"));
    }
    CodeSnippet codeSnippet = findSnippet(snippetId);

    ResponseEntity<?> assetResponse = deleteAsset(snippetId);
    if (assetResponse.getStatusCode().isError()) {
      throw new HttpServerErrorException(assetResponse.getStatusCode());
    }
    codeSnippetRepository.delete(codeSnippet);
    return "Snippet deleted successfully";
  }

  // Internal methods
  private CodeSnippet createAndSaveCodeSnippet(CodeSnippetDTO snippet) {
    CodeSnippet codeSnippet = new CodeSnippet();
    codeSnippet.setAssetId(snippet.getAssetId());
    codeSnippet.setLanguage(snippet.getLanguageInEnum());
    codeSnippet.setVersion(snippet.getVersion());
    codeSnippetRepository.save(codeSnippet);
    return codeSnippet;
  }

  private CodeSnippetDTO convertToDTO(CodeSnippet codeSnippets, MultipartFile content) {
    return CodeSnippetDTO.builder()
            .language(codeSnippets.getLanguage().name())
            .version(codeSnippets.getVersion())
            .content(content)
            .build();
  }

  private CodeSnippet findSnippet(UUID snippetId) {
    return codeSnippetRepository.findById(snippetId)
            .orElseThrow(() -> new EntityNotFoundException("Snippet not found with id " + snippetId));
  }

  // ** Printscript manager

  private void compileSnippet(CodeSnippetDTO snippet) {
    ResponseEntity<String> compileSnippetResponse = printscriptManager.compile(snippet.getContentInString(), snippet.getLanguageInEnum(), snippet.getVersion());
    if (compileSnippetResponse.getStatusCode().isError()) {
      throw new HttpServerErrorException(compileSnippetResponse.getStatusCode());
    }
  }

  // ** Permission manager

  private ResponseEntity<String> createNewPermission(String userId, UUID snippetId) {
    return permissionManager.createNewPermission(userId, snippetId);
  }

  private boolean canReadSnippet(String userId, UUID snippetId) {
    return permissionManager.canRead(userId, snippetId);
  }

  private boolean canWriteSnippet(String userId, UUID snippetId) {
    return permissionManager.canWrite(userId, snippetId);
  }

  private boolean canDeleteSnippet(String userId, UUID snippetId) {
    return permissionManager.canDelete(userId, snippetId);
  }

  // ** Asset manager

  private ResponseEntity<String> createNewAsset(CodeSnippetDTO snippet) {
    return assetManager.createAsset("snippets", snippet.getAssetId(), snippet.getContent());
  }

  private InputStream getAsset(CodeSnippet codeSnippet) {
    return assetManager.getAsset("snippets", codeSnippet.getAssetId());
  }

  private ResponseEntity<String> deleteAsset(UUID snippetId) {
    return assetManager.deleteAsset("snippets", snippetId.toString());
  }

  private MultipartFile toMultipartFile(InputStream assetResponse, String fileName) {
    try{
      return new MockMultipartFile(fileName, assetResponse);
    } catch (IOException e) {
      throw new RuntimeException("Error reading the file content: " + fileName, e);
    }
  }
}
