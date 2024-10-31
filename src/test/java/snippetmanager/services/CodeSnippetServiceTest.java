package snippetmanager.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityNotFoundException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;
import snippetmanager.model.dtos.SnippetReceivedDto;
import snippetmanager.model.dtos.SnippetSendDto;
import snippetmanager.model.dtos.webservice.PermissionDto;
import snippetmanager.model.entities.CodeSnippet;
import snippetmanager.repositories.CodeSnippetRepository;
import snippetmanager.util.CodeLanguage;
import snippetmanager.webservice.WebClientUtility;
import snippetmanager.webservice.asset.AssetManager;
import snippetmanager.webservice.permission.PermissionManager;
import snippetmanager.webservice.printscript.PrintscriptManager;

class CodeSnippetServiceTest {

  @Mock private CodeSnippetRepository codeSnippetRepository;

  @Mock private WebClientUtility webClientUtility;

  @Mock private PermissionManager permissionManager;

  @Mock private AssetManager assetManager;

  @Mock private PrintscriptManager printscriptManager;

  @InjectMocks private CodeSnippetService codeSnippetService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    SecurityContext securityContext = mock(SecurityContext.class);
    Authentication authentication = mock(Authentication.class);
    Jwt jwt = mock(Jwt.class);

    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn(jwt);
    when(jwt.getClaimAsString("sub")).thenReturn("1");

    SecurityContextHolder.setContext(securityContext);
  }

  @Test
  void createSnippetSuccess() {
    MultipartFile contentFile = mockMultipartFile("test content");
    String snippetId = "snippet-test";

    SnippetReceivedDto snippetDto =
        SnippetReceivedDto.builder()
            .assetId(snippetId)
            .language("PRINTSCRIPT")
            .version("1.1")
            .content(contentFile)
            .build();

    when(codeSnippetRepository.save(any(CodeSnippet.class)))
        .thenAnswer(
            invocation -> {
              CodeSnippet snippet = invocation.getArgument(0);
              snippet.setAssetId(snippetId);
              return snippet;
            });

    when(assetManager.createAsset(eq("snippets"), eq(snippetId), any(MultipartFile.class)))
        .thenReturn(new ResponseEntity<>("Asset created", HttpStatus.OK));

    Mono<ResponseEntity<String>> mockResponse =
        Mono.just(new ResponseEntity<>("Permission granted", HttpStatus.OK));
    when(webClientUtility.postAsync(anyString(), any(PermissionDto.class), eq(String.class)))
        .thenReturn(mockResponse);

    when(permissionManager.createNewPermission(any(String.class), eq(snippetId)))
        .thenReturn(new ResponseEntity<>("Permission granted", HttpStatus.OK));

    when(printscriptManager.compile(
            eq(snippetDto.getContentInString()),
            eq(snippetDto.getLanguageInEnum()),
            eq(snippetDto.getVersion())))
        .thenReturn(new ResponseEntity<>("Snippet compiled successfully", HttpStatus.OK));

    String response = codeSnippetService.createSnippet(snippetDto, "1");

    assertEquals("Snippet created successfully", response);
  }

  @Test
  void createSnippetPermissionError() {
    MultipartFile contentFile = mockMultipartFile("test content");

    SnippetReceivedDto snippetDto =
        SnippetReceivedDto.builder()
            .content(contentFile)
            .language("PRINTSCRIPT")
            .version("1.1")
            .build();

    String snippetId = UUID.randomUUID().toString();

    when(codeSnippetRepository.save(any(CodeSnippet.class)))
        .thenAnswer(
            invocation -> {
              CodeSnippet snippet = invocation.getArgument(0);
              snippet.setAssetId(snippetId);
              return snippet;
            });

    Mono<ResponseEntity<String>> mockResponse =
        Mono.just(new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED));
    when(webClientUtility.postAsync(anyString(), any(PermissionDto.class), eq(String.class)))
        .thenReturn(mockResponse);

    when(permissionManager.createNewPermission(any(String.class), eq(snippetId)))
        .thenReturn(new ResponseEntity<>("Permission error", HttpStatus.INTERNAL_SERVER_ERROR));

    when(printscriptManager.compile(
            eq(snippetDto.getContentInString()),
            eq(snippetDto.getLanguageInEnum()),
            eq(snippetDto.getVersion())))
        .thenReturn(new ResponseEntity<>("Snippet compiled successfully", HttpStatus.OK));

    assertThrows(
        HttpServerErrorException.class,
        () -> {
          codeSnippetService.createSnippet(snippetDto, "1");
        });
  }

  @Test
  void getSnippetSuccess() {
    String snippetId = UUID.randomUUID().toString();
    String userId = "1";

    CodeSnippet codeSnippet = new CodeSnippet();
    codeSnippet.setAssetId(snippetId);
    codeSnippet.setLanguage(CodeLanguage.PRINTSCRIPT);

    when(permissionManager.canRead(eq(userId), eq(snippetId))).thenReturn(true);
    when(codeSnippetRepository.findCodeSnippetByAssetId(eq(snippetId)))
        .thenReturn(Optional.of(codeSnippet));
    when(assetManager.getAsset(eq("snippets"), eq(snippetId)))
        .thenReturn(new ByteArrayInputStream("test content".getBytes()));

    SnippetSendDto result = codeSnippetService.getSnippet(snippetId, userId);

    assertEquals(snippetId, result.getAssetId());
    assertEquals("PRINTSCRIPT", result.getLanguage());
  }

  @Test
  void getSnippetNotFound() {
    String snippetId = UUID.randomUUID().toString();
    String userId = "1";

    when(permissionManager.canRead(eq(userId), eq(snippetId))).thenReturn(true);
    when(codeSnippetRepository.findById(snippetId)).thenReturn(Optional.empty());

    assertThrows(
        EntityNotFoundException.class,
        () -> {
          codeSnippetService.getSnippet(snippetId, userId);
        });
  }

  @Test
  void getSnippetPermissionDenied() {
    String snippetId = UUID.randomUUID().toString();
    String userId = "1";

    when(permissionManager.canRead(eq(userId), eq(snippetId))).thenReturn(false);

    assertThrows(
        PermissionDeniedDataAccessException.class,
        () -> {
          codeSnippetService.getSnippet(snippetId, userId);
        });
  }

  @Test
  void updateSnippetSuccess() {
    String snippetId = UUID.randomUUID().toString();
    String assetId = "snippet-test";
    String userId = "1";

    CodeSnippet existingSnippet = new CodeSnippet();
    existingSnippet.setAssetId(snippetId);
    existingSnippet.setAssetId(assetId);
    existingSnippet.setVersion("1.1");
    existingSnippet.setLanguage(CodeLanguage.PRINTSCRIPT);

    when(codeSnippetRepository.findCodeSnippetByAssetId(assetId))
        .thenReturn(Optional.of(existingSnippet));

    SnippetReceivedDto snippetDto =
        SnippetReceivedDto.builder()
            .language("PRINTSCRIPT")
            .version("1.1")
            .content(mockMultipartFile("test content"))
            .build();

    when(codeSnippetRepository.save(any(CodeSnippet.class)))
        .thenAnswer(
            invocation -> {
              CodeSnippet snippet = invocation.getArgument(0);
              snippet.setAssetId(snippetId);
              return snippet;
            });

    when(permissionManager.canWrite(eq(userId), eq(assetId))).thenReturn(true);
    when(assetManager.createAsset(eq("snippets"), eq(assetId), any(MultipartFile.class)))
        .thenReturn(new ResponseEntity<>("Asset updated", HttpStatus.OK));

    String response = codeSnippetService.updateSnippet(assetId, userId, snippetDto);

    assertEquals("Snippet updated successfully", response);

    verify(codeSnippetRepository).save(existingSnippet);
  }

  @Test
  void updateSnippetNotFound() {
    String snippetId = UUID.randomUUID().toString();
    String userId = "1";

    SnippetReceivedDto snippetDto =
        SnippetReceivedDto.builder().language("PRINTSCRIPT").version("1.1").build();

    when(permissionManager.canWrite(eq(userId), eq(snippetId))).thenReturn(true);
    when(codeSnippetRepository.findById(snippetId)).thenReturn(Optional.empty());

    assertThrows(
        EntityNotFoundException.class,
        () -> {
          codeSnippetService.updateSnippet(snippetId, userId, snippetDto);
        });
  }

  @Test
  void deleteSnippetSuccess() {
    String assetId = "snippet-test";
    String userId = "1";

    when(permissionManager.canDelete(eq(userId), eq(assetId))).thenReturn(true);
    when(codeSnippetRepository.findCodeSnippetByAssetId(assetId))
        .thenReturn(Optional.of(new CodeSnippet()));
    when(assetManager.deleteAsset(eq("snippets"), eq(assetId)))
        .thenReturn(new ResponseEntity<>("Asset deleted", HttpStatus.OK));

    String response = codeSnippetService.deleteSnippet(assetId, userId);
    assertEquals("Snippet deleted successfully", response);
    verify(codeSnippetRepository).deleteCodeSnippetByAssetId(assetId);
  }

  @Test
  void deleteSnippetPermissionDenied() {
    String snippetId = UUID.randomUUID().toString();
    String userId = "1";

    when(permissionManager.canDelete(eq(userId), eq(snippetId))).thenReturn(false);

    assertThrows(
        PermissionDeniedDataAccessException.class,
        () -> {
          codeSnippetService.deleteSnippet(snippetId, userId);
        });
  }

  private MultipartFile mockMultipartFile(String content) {
    return new MockMultipartFile("test-snippet", content.getBytes(StandardCharsets.UTF_8));
  }
}
