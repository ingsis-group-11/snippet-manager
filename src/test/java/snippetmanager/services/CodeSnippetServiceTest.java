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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;
import snippetmanager.model.dtos.AllSnippetsRecieveDto;
import snippetmanager.model.dtos.SnippetIdAuthorDto;
import snippetmanager.model.dtos.SnippetReceivedDto;
import snippetmanager.model.dtos.SnippetSendDto;
import snippetmanager.model.dtos.webservice.PermissionDto;
import snippetmanager.model.entities.CodeSnippet;
import snippetmanager.model.entities.FormatterRule;
import snippetmanager.model.entities.LintingRule;
import snippetmanager.redis.linter.LintProducer;
import snippetmanager.repositories.CodeSnippetRepository;
import snippetmanager.repositories.FormatterRuleRepository;
import snippetmanager.repositories.LanguagesRepository;
import snippetmanager.repositories.LintingRuleRepository;
import snippetmanager.util.enums.CodeLanguage;
import snippetmanager.util.enums.LintResult;
import snippetmanager.util.enums.PermissionType;
import snippetmanager.webservice.WebClientUtility;
import snippetmanager.webservice.asset.AssetManager;
import snippetmanager.webservice.permission.PermissionManager;
import snippetmanager.webservice.printscript.PrintscriptManager;

@ActiveProfiles("test")
@SpringBootTest
class CodeSnippetServiceTest extends AbstractTransactionalJUnit4SpringContextTests {
  @MockBean private CodeSnippetRepository codeSnippetRepository;

  @MockBean private WebClientUtility webClientUtility;

  @MockBean private PermissionManager permissionManager;

  @MockBean private AssetManager assetManager;

  @MockBean private PrintscriptManager printscriptManager;

  @MockBean private UserService userService;

  @MockBean private LintProducer lintProducer;

  @MockBean private LintingRuleRepository lintingRuleRepository;

  @MockBean private FormatterRuleRepository formatterRuleRepository;

  @MockBean private LintingRuleService lintingRuleService;

  @MockBean private FormatterRuleService formatterRuleService;

  @MockBean private LanguagesRepository languagesRepository;

  @Autowired private CodeSnippetService codeSnippetService;

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

    when(printscriptManager.compile(anyString(), any(CodeLanguage.class), anyString()))
        .thenReturn(new ResponseEntity<>("Snippet compiled successfully", HttpStatus.OK));

    LintingRule lintingRule = new LintingRule();
    lintingRule.setName("Test");
    lintingRule.setValue("false");
    lintingRule.setIsActive(true);
    lintingRule.setUserId("1");

    FormatterRule formatterRule = new FormatterRule();
    formatterRule.setName("Test");
    formatterRule.setValue("false");
    formatterRule.setIsActive(true);
    formatterRule.setUserId("1");

    when(lintingRuleRepository.findAll()).thenReturn(new ArrayList<>(List.of(lintingRule)));
    when(formatterRuleRepository.findAll()).thenReturn(new ArrayList<>(List.of(formatterRule)));

    String snippetId = UUID.randomUUID().toString();

    when(codeSnippetRepository.save(any(CodeSnippet.class)))
        .thenAnswer(
            invocation -> {
              CodeSnippet snippet = invocation.getArgument(0);
              snippet.setAssetId(snippetId);
              return snippet;
            });

    when(permissionManager.createNewPermission(eq(snippetId), any(PermissionType.class)))
        .thenReturn(new ResponseEntity<>("Permission created", HttpStatus.OK));

    when(assetManager.createAsset(eq("snippets"), eq(snippetId), any(MultipartFile.class)))
        .thenReturn(new ResponseEntity<>("Asset created", HttpStatus.OK));

    MultipartFile contentFile = mockMultipartFile("test content");

    SnippetReceivedDto snippetDto =
        SnippetReceivedDto.builder()
            .language("PRINTSCRIPT")
            .version("1.1")
            .content(contentFile)
            .build();

    String response = codeSnippetService.createSnippet(snippetDto, "1");

    assertEquals("Snippet created successfully", response);
    verify(printscriptManager).compile(anyString(), any(CodeLanguage.class), anyString());
    verify(codeSnippetRepository).save(any(CodeSnippet.class));
    verify(assetManager).createAsset(eq("snippets"), eq(snippetId), any(MultipartFile.class));
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

    when(permissionManager.createNewPermission(eq(snippetId), any(PermissionType.class)))
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

    when(permissionManager.canRead(eq(snippetId))).thenReturn(true);
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

    when(permissionManager.canRead(eq(snippetId))).thenReturn(true);
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

    when(permissionManager.canRead(eq(snippetId))).thenReturn(false);

    assertThrows(
        PermissionDeniedDataAccessException.class,
        () -> {
          codeSnippetService.getSnippet(snippetId, userId);
        });
  }

  @Test
  void updateSnippetSuccess() {
    String snippetId = UUID.randomUUID().toString();
    String userId = "1";

    CodeSnippet existingSnippet = new CodeSnippet();
    existingSnippet.setAssetId(snippetId);
    existingSnippet.setVersion("1.1");
    existingSnippet.setLanguage(CodeLanguage.PRINTSCRIPT);

    when(codeSnippetRepository.findById(snippetId)).thenReturn(Optional.of(existingSnippet));

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

    when(permissionManager.canWrite(eq(snippetId))).thenReturn(true);
    when(assetManager.createAsset(eq("snippets"), eq(snippetId), any(MultipartFile.class)))
        .thenReturn(new ResponseEntity<>("Asset updated", HttpStatus.OK));

    when(printscriptManager.compile(anyString(), any(CodeLanguage.class), anyString()))
        .thenReturn(new ResponseEntity<>("Snippet compiled successfully", HttpStatus.OK));

    String response = codeSnippetService.updateSnippet(snippetId, userId, snippetDto);

    assertEquals("Snippet updated successfully", response);
  }

  @Test
  void updateSnippetNotFound() {
    String snippetId = UUID.randomUUID().toString();
    String userId = "1";

    SnippetReceivedDto snippetDto =
        SnippetReceivedDto.builder().language("PRINTSCRIPT").version("1.1").build();

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

    when(permissionManager.canWrite(eq(assetId))).thenReturn(true);
    when(codeSnippetRepository.findCodeSnippetByAssetId(assetId))
        .thenReturn(Optional.of(new CodeSnippet()));
    when(assetManager.deleteAsset(eq("snippets"), eq(assetId)))
        .thenReturn(new ResponseEntity<>("Asset deleted", HttpStatus.OK));

    String response = codeSnippetService.deleteSnippet(assetId);
    assertEquals("Snippet deleted successfully", response);
    verify(codeSnippetRepository).deleteCodeSnippetByAssetId(assetId);
  }

  @Test
  void deleteSnippetPermissionDenied() {
    String snippetId = UUID.randomUUID().toString();

    when(permissionManager.canDelete(eq(snippetId))).thenReturn(false);

    assertThrows(
        PermissionDeniedDataAccessException.class,
        () -> {
          codeSnippetService.deleteSnippet(snippetId);
        });
  }

  //   public AllSnippetsSendDto getAllSnippets(Integer from, Integer to, String userId) {
  //    userService.createUser(userId);
  //    return getAllSnippetsWithPermission(from, to, userId, PermissionType.READ);
  //  }
  //
  //  public List<SnippetSendDto> getAllOwnSnippets(String userId) {
  //    AllSnippetsSendDto allSnippetsSendDto =
  //        getAllSnippetsWithPermission(null, null, userId, PermissionType.READ_WRITE);
  //
  //    return allSnippetsSendDto.getSnippets();
  //  }

  @Test
  public void getAllSnippetsSuccess() {
    String userId = UUID.randomUUID().toString();
    String assetId = UUID.randomUUID().toString();

    AllSnippetsRecieveDto allSnippetsRecieveDto =
        AllSnippetsRecieveDto.builder()
            .snippetsIds(
                List.of(SnippetIdAuthorDto.builder().author(userId).snippetId(assetId).build()))
            .build();

    when(permissionManager.getSnippetsUserWithPermission(0, 10, PermissionType.READ.toString()))
        .thenReturn(ResponseEntity.ok(allSnippetsRecieveDto));
    when(assetManager.getAsset(eq("snippets"), eq(assetId)))
        .thenReturn(new ByteArrayInputStream("test content".getBytes()));
    when(userService.getUserName(eq(userId))).thenReturn("userName");

    CodeSnippet codeSnippet = new CodeSnippet();
    codeSnippet.setAssetId(assetId);
    codeSnippet.setLanguage(CodeLanguage.PRINTSCRIPT);
    codeSnippet.setVersion("1.1");
    codeSnippet.setExtension("prs");
    codeSnippet.setName("test-snippet");
    codeSnippet.setLintResult(LintResult.SUCCESS);
    when(codeSnippetRepository.findCodeSnippetByAssetId(eq(assetId)))
        .thenReturn(Optional.of(codeSnippet));

    codeSnippetService.getAllSnippets(0, 10, userId);

    verify(permissionManager).getSnippetsUserWithPermission(0, 10, PermissionType.READ.toString());
  }

  private MultipartFile mockMultipartFile(String content) {
    return new MockMultipartFile("test-snippet", content.getBytes(StandardCharsets.UTF_8));
  }
}
