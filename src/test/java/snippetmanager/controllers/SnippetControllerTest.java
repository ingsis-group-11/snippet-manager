package snippetmanager.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;
import snippetmanager.model.dtos.AllSnippetsSendDto;
import snippetmanager.model.dtos.SnippetReceivedDto;
import snippetmanager.model.dtos.SnippetSendDto;
import snippetmanager.services.CodeSnippetService;

@ActiveProfiles("test")
public class SnippetControllerTest {

  @Mock private CodeSnippetService codeSnippetService;

  @InjectMocks private CodeSnippetController codeSnippetController;

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
  void createSnippet() {
    MultipartFile file = mock(MultipartFile.class);
    String userId = "1";
    String title = "Test Title";
    String language = "Printscript 1.1";
    String extension = "ps";
    String expectedResponse = "Snippet created successfully";

    when(codeSnippetService.createSnippet(any(SnippetReceivedDto.class), eq(userId)))
        .thenReturn(expectedResponse);

    ResponseEntity<String> response =
        codeSnippetController.createSnippet(file, title, language, extension);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(expectedResponse, response.getBody());
    verify(codeSnippetService).createSnippet(any(SnippetReceivedDto.class), eq(userId));
  }

  @Test
  void getSnippet() {
    String snippetId = UUID.randomUUID().toString();
    String userId = "1";
    SnippetSendDto snippetDto =
        SnippetSendDto.builder().version("1.0").language("Java").content("example content").build();

    when(codeSnippetService.getSnippet(snippetId, userId)).thenReturn(snippetDto);

    ResponseEntity<SnippetSendDto> response = codeSnippetController.getSnippet(snippetId);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(snippetDto, response.getBody());
    verify(codeSnippetService).getSnippet(snippetId, userId);
  }

  @Test
  void getAllSnippets() {
    String userId = "1";

    List<SnippetSendDto> snippets =
        Arrays.asList(SnippetSendDto.builder().build(), SnippetSendDto.builder().build());
    AllSnippetsSendDto expectedResponse =
        AllSnippetsSendDto.builder().snippets(snippets).maxSnippets(2).build();

    when(codeSnippetService.getAllSnippets(0, Integer.MAX_VALUE, userId))
        .thenReturn(expectedResponse);

    ResponseEntity<AllSnippetsSendDto> response =
        codeSnippetController.getAllSnippets(0, Integer.MAX_VALUE);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(snippets, Objects.requireNonNull(response.getBody()).getSnippets());
    verify(codeSnippetService).getAllSnippets(0, Integer.MAX_VALUE, userId);
  }

  @Test
  void updateSnippet() {
    String snippetId = UUID.randomUUID().toString();
    MultipartFile file = mockMultipartFile("test content");
    String userId = "1";
    String expectedResponse = "Snippet updated successfully";

    when(codeSnippetService.updateSnippet(eq(snippetId), eq(userId), any(SnippetReceivedDto.class)))
        .thenReturn(expectedResponse);

    ResponseEntity<String> response = codeSnippetController.updateSnippet(snippetId, file);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(expectedResponse, response.getBody());
    verify(codeSnippetService)
        .updateSnippet(eq(snippetId), eq(userId), any(SnippetReceivedDto.class));
  }

  @Test
  void deleteSnippet() {
    String snippetId = UUID.randomUUID().toString();
    String userId = "1";
    String expectedResponse = "Snippet deleted successfully";

    when(codeSnippetService.deleteSnippet(snippetId)).thenReturn(expectedResponse);

    ResponseEntity<String> response = codeSnippetController.deleteSnippet(snippetId);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(expectedResponse, response.getBody());
    verify(codeSnippetService).deleteSnippet(snippetId);
  }

  private MultipartFile mockMultipartFile(String content) {
    return new MockMultipartFile("test-snippet", content.getBytes(StandardCharsets.UTF_8));
  }
}
