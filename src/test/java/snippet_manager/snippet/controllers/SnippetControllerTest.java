package snippet_manager.snippet.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import snippet_manager.snippet.model.dtos.CodeSnippetDTO;
import snippet_manager.snippet.services.CodeSnippetService;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SnippetControllerTest {
  /*
  @Mock
  private CodeSnippetService codeSnippetService;

  @InjectMocks
  private CodeSnippetController codeSnippetController;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void createSnippet() {
    MultipartFile file = mock(MultipartFile.class);
    Long userId = 1L;
    String version = "1.0";
    String title = "Test Title";
    String language = "Java";
    String expectedResponse = "Snippet created successfully";

    when(codeSnippetService.createSnippet(any(CodeSnippetDTO.class), eq(userId))).thenReturn(expectedResponse);

    ResponseEntity<String> response = codeSnippetController.createSnippet(file, version, title, language, userId);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(expectedResponse, response.getBody());
    verify(codeSnippetService).createSnippet(any(CodeSnippetDTO.class), eq(userId));
  }

  @Test
  void getSnippet() {
    UUID snippetId = UUID.randomUUID();
    Long userId = 1L;
    CodeSnippetDTO snippetDTO = CodeSnippetDTO.builder()
            .title("Test Title")
            .version("1.0")
            .language("Java")
            .build();

    when(codeSnippetService.getSnippet(snippetId, userId)).thenReturn(snippetDTO);

    ResponseEntity<CodeSnippetDTO> response = codeSnippetController.getSnippet(snippetId, userId);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(snippetDTO, response.getBody());
    verify(codeSnippetService).getSnippet(snippetId, userId);
  }

  @Test
  void getAllSnippets() {
    Long userId = 1L;
    List<CodeSnippetDTO> snippets = Arrays.asList(
            CodeSnippetDTO.builder().title("Test Title 1").build(),
            CodeSnippetDTO.builder().title("Test Title 2").build()
    );

    when(codeSnippetService.getAllSnippets(userId)).thenReturn(snippets);

    ResponseEntity<List<CodeSnippetDTO>> response = codeSnippetController.getAllSnippets(userId);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(snippets, response.getBody());
    verify(codeSnippetService).getAllSnippets(userId);
  }

  @Test
  void updateSnippet() {
    UUID snippetId = UUID.randomUUID();
    MultipartFile file = mock(MultipartFile.class);
    Long userId = 1L;
    String title = "Updated Title";
    String version = "1.1";
    String language = "PRINTSCRIPT";
    String expectedResponse = "Snippet updated successfully";

    when(codeSnippetService.updateSnippet(eq(snippetId), eq(userId), any(CodeSnippetDTO.class))).thenReturn(expectedResponse);

    ResponseEntity<String> response = codeSnippetController.updateSnippet(snippetId, file, title, version, language, userId);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(expectedResponse, response.getBody());
    verify(codeSnippetService).updateSnippet(eq(snippetId), eq(userId), any(CodeSnippetDTO.class));
  }

  @Test
  void deleteSnippet() {
    UUID snippetId = UUID.randomUUID();
    Long userId = 1L;
    String expectedResponse = "Snippet deleted successfully";

    when(codeSnippetService.deleteSnippet(snippetId, userId)).thenReturn(expectedResponse);

    ResponseEntity<String> response = codeSnippetController.deleteSnippet(snippetId, userId);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(expectedResponse, response.getBody());
    verify(codeSnippetService).deleteSnippet(snippetId, userId);
  }*/
}
