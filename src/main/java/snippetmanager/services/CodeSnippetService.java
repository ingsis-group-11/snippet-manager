package snippetmanager.services;

import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.multipart.MultipartFile;
import snippetmanager.model.dtos.SnippetReceivedDto;
import snippetmanager.model.dtos.SnippetSendDto;
import snippetmanager.model.entities.CodeSnippet;
import snippetmanager.repositories.CodeSnippetRepository;
import snippetmanager.webservice.asset.AssetManager;
import snippetmanager.webservice.permission.PermissionManager;
import snippetmanager.webservice.printscript.PrintscriptManager;

@Service
public class CodeSnippetService {
  @Autowired private CodeSnippetRepository codeSnippetRepository;

  @Autowired private PermissionManager permissionManager;

  @Autowired private PrintscriptManager printscriptManager;

  @Autowired private AssetManager assetManager;

  private final String assetManagerContainer = "snippets";

  public String createSnippet(SnippetReceivedDto snippet, String userId) {
    compileSnippet(snippet);
    codeSnippetRepository
        .findCodeSnippetByAssetId(snippet.getAssetId())
        .ifPresent(
            codeSnippet -> {
              throw new IllegalArgumentException("Snippet with the same assetId already exists");
            });
    CodeSnippet codeSnippet = createAndSaveCodeSnippet(snippet);

    ResponseEntity<String> permissionResponse = createNewPermission(userId, codeSnippet.getId());
    if (permissionResponse.getStatusCode().isError()) {
      codeSnippetRepository.deleteById(codeSnippet.getId());
      throw new HttpServerErrorException(permissionResponse.getStatusCode());
    }

    ResponseEntity<String> assetResponse = createNewAsset(snippet);
    if (assetResponse.getStatusCode().isError()) {
      codeSnippetRepository.deleteById(codeSnippet.getId());
      deletePermission(userId, codeSnippet.getId());
      throw new HttpServerErrorException(assetResponse.getStatusCode());
    }

    return "Snippet created successfully";
  }

  public SnippetSendDto getSnippet(String assetId, String userId) {
    boolean canAccess = canReadSnippet(userId, assetId);
    if (!canAccess) {
      throw new PermissionDeniedDataAccessException(
          "You don't have permission to access this snippet",
          new Exception("You don't have permission to access this snippet"));
    }
    CodeSnippet codeSnippet = findSnippetByAssetId(assetId);

    InputStream assetResponse = getAsset(assetId);
    MultipartFile snippetContent = toMultipartFile(assetResponse, assetId);
    return convertToSnippetSendDto(codeSnippet, snippetContent);
  }

  public List<SnippetSendDto> getAllSnippets(String userId) {
    List<CodeSnippet> codeSnippets = codeSnippetRepository.findAll();

    List<SnippetSendDto> codeSnippetDtos =
        codeSnippets.stream()
            .filter(codeSnippet -> canReadSnippet(userId, codeSnippet.getId()))
            .map(
                codeSnippet -> {
                  InputStream assetResponse = getAsset(codeSnippet.getAssetId());
                  MultipartFile asset = toMultipartFile(assetResponse, codeSnippet.getAssetId());

                  return convertToSnippetSendDto(codeSnippet, asset);
                })
            .collect(Collectors.toList());

    return codeSnippetDtos;
  }

  public String updateSnippet(String assetId, String userId, SnippetReceivedDto codeSnippet) {
    boolean canAccess = canWriteSnippet(userId, assetId);
    if (!canAccess) {
      throw new PermissionDeniedDataAccessException(
          "You don't have permission to write this snippet",
          new Exception("You don't have permission to write this snippet"));
    }
    CodeSnippet existingCodeSnippet = findSnippetByAssetId(assetId);

    ResponseEntity<?> assetResponse =
        assetManager.createAsset("snippets", assetId, codeSnippet.getContent());
    if (assetResponse.getStatusCode().isError()) {
      throw new HttpServerErrorException(assetResponse.getStatusCode());
    }
    existingCodeSnippet.setLanguage(codeSnippet.getLanguageInEnum());
    existingCodeSnippet.setVersion(codeSnippet.getVersion());
    codeSnippetRepository.save(existingCodeSnippet);
    return "Snippet updated successfully";
  }

  public String deleteSnippet(String assetId, String userId) {
    boolean canAccess = canDeleteSnippet(userId, assetId);
    if (!canAccess) {
      throw new PermissionDeniedDataAccessException(
          "You don't have permission to access this snippet",
          new Exception("You don't have permission to access this snippet"));
    }
    findSnippetByAssetId(assetId);

    ResponseEntity<?> assetResponse = deleteAsset(assetId);
    if (assetResponse.getStatusCode().isError()) {
      throw new HttpServerErrorException(assetResponse.getStatusCode());
    }
    codeSnippetRepository.deleteCodeSnippetByAssetId(assetId);
    return "Snippet deleted successfully";
  }

  // ** Internal methods
  private CodeSnippet createAndSaveCodeSnippet(SnippetReceivedDto snippet) {
    CodeSnippet codeSnippet = new CodeSnippet();
    codeSnippet.setAssetId(snippet.getAssetId());
    codeSnippet.setLanguage(snippet.getLanguageInEnum());
    codeSnippet.setVersion(snippet.getVersion());
    codeSnippetRepository.save(codeSnippet);
    return codeSnippet;
  }

  private SnippetSendDto convertToSnippetSendDto(CodeSnippet codeSnippets, MultipartFile content) {
    return SnippetSendDto.builder()
        .language(codeSnippets.getLanguage().name())
        .version(codeSnippets.getVersion())
        .assetId(codeSnippets.getAssetId())
        .content(getContentFromMultipartFile(content))
        .build();
  }

  private String getContentFromMultipartFile(MultipartFile content) {
    try {
      return new String(content.getBytes());
    } catch (IOException e) {
      throw new RuntimeException("Error reading the file content", e);
    }
  }

  private MultipartFile toMultipartFile(InputStream assetResponse, String fileName) {
    try {
      return new MockMultipartFile(fileName, assetResponse);
    } catch (IOException e) {
      throw new RuntimeException("Error reading the file content: " + fileName, e);
    }
  }

  private CodeSnippet findSnippetByAssetId(String assetId) {
    return codeSnippetRepository
        .findCodeSnippetByAssetId(assetId)
        .orElseThrow(
            () -> new EntityNotFoundException("Snippet not found with assetId " + assetId));
  }

  // ** Printscript manager

  private void compileSnippet(SnippetReceivedDto snippet) {
    ResponseEntity<String> compileSnippetResponse =
        printscriptManager.compile(
            snippet.getContentInString(), snippet.getLanguageInEnum(), snippet.getVersion());
    if (compileSnippetResponse.getStatusCode().isError()) {
      throw new HttpServerErrorException(compileSnippetResponse.getStatusCode());
    }
  }

  // ** Permission manager

  private ResponseEntity<String> createNewPermission(String userId, String snippetId) {
    return permissionManager.createNewPermission(userId, snippetId);
  }

  private boolean canReadSnippet(String userId, String snippetId) {
    return permissionManager.canRead(userId, snippetId);
  }

  private boolean canWriteSnippet(String userId, String snippetId) {
    return permissionManager.canWrite(userId, snippetId);
  }

  private boolean canDeleteSnippet(String userId, String snippetId) {
    return permissionManager.canDelete(userId, snippetId);
  }

  private ResponseEntity<String> deletePermission(String userId, String snippetId) {
    return permissionManager.deletePermission(userId, snippetId);
  }

  // ** Asset manager

  private ResponseEntity<String> createNewAsset(SnippetReceivedDto snippet) {
    return assetManager.createAsset(
        assetManagerContainer, snippet.getAssetId(), snippet.getContent());
  }

  private InputStream getAsset(String assetId) {
    return assetManager.getAsset(assetManagerContainer, assetId);
  }

  private ResponseEntity<String> deleteAsset(String snippetId) {
    return assetManager.deleteAsset(assetManagerContainer, snippetId);
  }
}
