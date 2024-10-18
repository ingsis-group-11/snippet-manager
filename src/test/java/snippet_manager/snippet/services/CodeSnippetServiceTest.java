package snippet_manager.snippet.services;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;
import snippet_manager.snippet.model.dtos.CodeSnippetDTO;
import snippet_manager.snippet.model.dtos.webservice.PermissionDTO;
import snippet_manager.snippet.model.entities.CodeSnippet;
import snippet_manager.snippet.webservice.permission.PermissionManager;
import snippet_manager.snippet.repositories.CodeSnippetRepository;
import snippet_manager.snippet.util.CodeLanguage;
import snippet_manager.snippet.webservice.WebClientUtility;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CodeSnippetServiceTest {

  @Mock
  private CodeSnippetRepository codeSnippetRepository;

  @Mock
  private WebClientUtility webClientUtility;

  @Mock
  private PermissionManager permissionManager;

  @InjectMocks
  private CodeSnippetService codeSnippetService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void createSnippetSuccess() {
    MultipartFile contentFile = new MockMultipartFile("content", "test content".getBytes());

    CodeSnippetDTO snippetDTO = CodeSnippetDTO.builder()
            .title("Test Snippet")
            .content(contentFile)
            .language("PRINTSCRIPT")
            .version("1.1")
            .build();

    UUID snippetId = UUID.randomUUID();

    when(codeSnippetRepository.save(any(CodeSnippet.class))).thenAnswer(invocation -> {
      CodeSnippet snippet = invocation.getArgument(0);
      snippet.setId(snippetId);
      return snippet;
    });

    Mono<ResponseEntity<String>> mockResponse = Mono.just(new ResponseEntity<>("Permission granted", HttpStatus.OK));
    when(webClientUtility.postAsync(anyString(), any(PermissionDTO.class), eq(String.class)))
            .thenReturn(mockResponse);

    when(permissionManager.createNewPermission(any(Long.class), eq(snippetId)))
            .thenReturn(new ResponseEntity<>("Permission granted", HttpStatus.OK));

    String response = codeSnippetService.createSnippet(snippetDTO, 1L);

    assertEquals("Snippet created successfully", response);
  }



  @Test
  void createSnippetPermissionError() {
    MultipartFile contentFile = new MockMultipartFile("content", "test content".getBytes());

    CodeSnippetDTO snippetDTO = CodeSnippetDTO.builder()
            .title("Test Snippet")
            .content(contentFile)
            .language("PRINTSCRIPT")
            .version("1.1")
            .build();

    UUID snippetId = UUID.randomUUID();

    when(codeSnippetRepository.save(any(CodeSnippet.class))).thenAnswer(invocation -> {
      CodeSnippet snippet = invocation.getArgument(0);
      snippet.setId(snippetId);
      return snippet;
    });

    Mono<ResponseEntity<String>> mockResponse = Mono.just(new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED));
    when(webClientUtility.postAsync(anyString(), any(PermissionDTO.class), eq(String.class)))
            .thenReturn(mockResponse);

    when(permissionManager.createNewPermission(any(Long.class), eq(snippetId)))
            .thenReturn(new ResponseEntity<>("Permission error", HttpStatus.INTERNAL_SERVER_ERROR));

    assertThrows(HttpServerErrorException.class, () -> {
      codeSnippetService.createSnippet(snippetDTO, 1L);
    });
  }

  @Test
  void getSnippetSuccess() {
    UUID snippetId = UUID.randomUUID();
    Long userId = 1L;

    CodeSnippet codeSnippet = new CodeSnippet();
    codeSnippet.setId(snippetId);
    codeSnippet.setTitle("Test Snippet");
    codeSnippet.setLanguage(CodeLanguage.PRINTSCRIPT);

    when(permissionManager.canRead(eq(userId), eq(snippetId))).thenReturn(true);
    when(codeSnippetRepository.findById(snippetId)).thenReturn(Optional.of(codeSnippet));

    CodeSnippetDTO result = codeSnippetService.getSnippet(snippetId, userId);

    assertEquals("Test Snippet", result.getTitle());
    assertEquals("PRINTSCRIPT", result.getLanguage());
  }

  @Test
  void getSnippetNotFound() {
    UUID snippetId = UUID.randomUUID();
    Long userId = 1L;

    when(permissionManager.canRead(eq(userId), eq(snippetId))).thenReturn(true);
    when(codeSnippetRepository.findById(snippetId)).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class, () -> {
      codeSnippetService.getSnippet(snippetId, userId);
    });
  }

  @Test
  void getSnippetPermissionDenied() {
    UUID snippetId = UUID.randomUUID();
    Long userId = 1L;

    when(permissionManager.canRead(eq(userId), eq(snippetId))).thenReturn(false);

    assertThrows(PermissionDeniedDataAccessException.class, () -> {
      codeSnippetService.getSnippet(snippetId, userId);
    });
  }

  @Test
  void updateSnippetSuccess() {
    MultipartFile contentFile = new MockMultipartFile("content", "test content".getBytes());

    UUID snippetId = UUID.randomUUID();
    Long userId = 1L;

    CodeSnippet existingSnippet = new CodeSnippet();
    existingSnippet.setId(snippetId);
    existingSnippet.setTitle("Old Title");

    when(codeSnippetRepository.findById(snippetId)).thenReturn(Optional.of(existingSnippet));

    CodeSnippetDTO snippetDTO = CodeSnippetDTO.builder()
            .title("Updated Title")
            .content(contentFile)
            .language("PRINTSCRIPT")
            .version("1.1")
            .build();

    when(codeSnippetRepository.save(any(CodeSnippet.class))).thenAnswer(invocation -> {
      CodeSnippet snippet = invocation.getArgument(0);
      snippet.setId(snippetId); // Se mantiene el mismo ID
      return snippet;
    });

    when(permissionManager.canWrite(eq(userId), eq(snippetId))).thenReturn(true);

    String response = codeSnippetService.updateSnippet(snippetId, userId, snippetDTO);

    assertEquals("Snippet updated successfully", response);

    verify(codeSnippetRepository).save(existingSnippet);
  }


  @Test
  void updateSnippetNotFound() {
    UUID snippetId = UUID.randomUUID();
    Long userId = 1L;

    CodeSnippetDTO snippetDTO = CodeSnippetDTO.builder()
            .title("Updated Title")
            .content(mock(MultipartFile.class))
            .language("PRINTSCRIPT")
            .version("1.1")
            .build();

    when(permissionManager.canWrite(eq(userId), eq(snippetId))).thenReturn(true);
    when(codeSnippetRepository.findById(snippetId)).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class, () -> {
      codeSnippetService.updateSnippet(snippetId, userId, snippetDTO);
    });
  }

  @Test
  void deleteSnippetSuccess() {
    UUID snippetId = UUID.randomUUID();
    Long userId = 1L;

    when(permissionManager.canDelete(eq(userId), eq(snippetId))).thenReturn(true);
    when(codeSnippetRepository.findById(snippetId)).thenReturn(Optional.of(new CodeSnippet()));

    String response = codeSnippetService.deleteSnippet(snippetId, userId);
    assertEquals("Snippet deleted successfully", response);
    verify(codeSnippetRepository).deleteById(snippetId);
  }

  @Test
  void deleteSnippetPermissionDenied() {
    UUID snippetId = UUID.randomUUID();
    Long userId = 1L;

    when(permissionManager.canDelete(eq(userId), eq(snippetId))).thenReturn(false);

    assertThrows(PermissionDeniedDataAccessException.class, () -> {
      codeSnippetService.deleteSnippet(snippetId, userId);
    });
  }
}
