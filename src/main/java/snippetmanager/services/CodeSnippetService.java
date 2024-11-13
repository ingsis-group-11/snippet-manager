package snippetmanager.services;

import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.multipart.MultipartFile;
import snippetmanager.model.dtos.AllSnippetsRecieveDto;
import snippetmanager.model.dtos.AllSnippetsSendDto;
import snippetmanager.model.dtos.LanguagesDto;
import snippetmanager.model.dtos.SnippetReceivedDto;
import snippetmanager.model.dtos.SnippetSendDto;
import snippetmanager.model.entities.CodeSnippet;
import snippetmanager.model.entities.FormatterRule;
import snippetmanager.model.entities.Languages;
import snippetmanager.model.entities.LintingRule;
import snippetmanager.redis.formatter.FormatterProducer;
import snippetmanager.redis.linter.LintProducer;
import snippetmanager.repositories.CodeSnippetRepository;
import snippetmanager.repositories.FormatterRuleRepository;
import snippetmanager.repositories.LanguagesRepository;
import snippetmanager.repositories.LintingRuleRepository;
import snippetmanager.util.DefaultRulesFactory;
import snippetmanager.util.enums.PermissionType;
import snippetmanager.webservice.asset.AssetManager;
import snippetmanager.webservice.permission.PermissionManager;
import snippetmanager.webservice.printscript.PrintscriptManager;

@Component
public class CodeSnippetService {
  private final FormatterProducer formatterProducer;
  private CodeSnippetRepository codeSnippetRepository;

  private PermissionManager permissionManager;

  private PrintscriptManager printscriptManager;

  private AssetManager assetManager;

  private LintProducer lintProducer;

  private LintingRuleRepository lintingRuleRepository;

  private FormatterRuleRepository formatterRuleRepository;

  private LintingRuleService lintingRuleService;

  private FormatterRuleService formatterRuleService;

  private LanguagesRepository languagesRepository;

  private UserService userService;

  public CodeSnippetService(
      CodeSnippetRepository codeSnippetRepository,
      LintProducer lintProducer,
      PermissionManager permissionManager,
      PrintscriptManager printscriptManager,
      AssetManager assetManager,
      LintingRuleRepository lintingRuleRepository,
      FormatterRuleRepository formatterRuleRepository,
      @Lazy LintingRuleService lintingRuleService,
      @Lazy FormatterRuleService formatterRuleService,
      LanguagesRepository languagesRepository,
      FormatterProducer formatterProducer,
      UserService userService) {
    this.lintProducer = lintProducer;
    this.codeSnippetRepository = codeSnippetRepository;
    this.permissionManager = permissionManager;
    this.printscriptManager = printscriptManager;
    this.assetManager = assetManager;
    this.lintingRuleRepository = lintingRuleRepository;
    this.formatterRuleRepository = formatterRuleRepository;
    this.lintingRuleService = lintingRuleService;
    this.formatterRuleService = formatterRuleService;
    this.languagesRepository = languagesRepository;
    this.formatterProducer = formatterProducer;
    this.userService = userService;
  }

  private final String assetManagerContainer = "snippets";

  @Transactional
  public String createSnippet(SnippetReceivedDto snippet, String userId) {
    compileSnippet(snippet);
    if (snippet.getAssetId() != null) {
      codeSnippetRepository
          .findCodeSnippetByAssetId(snippet.getAssetId())
          .ifPresent(
              codeSnippet -> {
                throw new IllegalArgumentException("Snippet with the same assetId already exists");
              });
    }
    CodeSnippet codeSnippet = createAndSaveCodeSnippet(snippet);

    ResponseEntity<String> permissionResponse =
        createNewPermission(userId, codeSnippet.getAssetId());
    if (permissionResponse.getStatusCode().isError()) {
      TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
      throw new HttpServerErrorException(permissionResponse.getStatusCode());
    }

    ResponseEntity<String> assetResponse = createNewAsset(codeSnippet, snippet.getContent());
    if (assetResponse.getStatusCode().isError()) {
      TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
      deletePermission(codeSnippet.getAssetId());
      throw new HttpServerErrorException(assetResponse.getStatusCode());
    }

    createDefaultRulesIfNeeded(userId);
    publishToRedis(snippet.getContent(), codeSnippet, userId);

    return "Snippet created successfully";
  }

  public SnippetSendDto getSnippet(String assetId, String userId) {

    boolean canAccess = canReadSnippet(assetId);
    if (!canAccess) {
      throw new PermissionDeniedDataAccessException(
          "You don't have permission to access this snippet",
          new Exception("You don't have permission to access this snippet"));
    }

    CodeSnippet codeSnippet = findSnippetByAssetId(assetId);
    String lintResult = codeSnippet.getResultAsString();

    InputStream assetResponse = getAsset(assetId);
    MultipartFile snippetContent = toMultipartFile(assetResponse, assetId);
    SnippetSendDto snippetDto =
        convertToSnippetSendDto(codeSnippet, snippetContent, lintResult, userId);
    snippetDto.setUserId(userId);
    return snippetDto;
  }

  public AllSnippetsSendDto getAllSnippets(Integer from, Integer to, String userId) {
    userService.createUser(userId);
    return getAllSnippetsWithPermission(from, to, userId, PermissionType.READ);
  }

  public List<SnippetSendDto> getAllOwnSnippets(String userId) {
    AllSnippetsSendDto allSnippetsSendDto =
        getAllSnippetsWithPermission(null, null, userId, PermissionType.READ_WRITE);

    return allSnippetsSendDto.getSnippets();
  }

  @Transactional
  public String updateSnippet(String assetId, String userId, SnippetReceivedDto codeSnippet) {
    Optional<CodeSnippet> snippet = codeSnippetRepository.findById(assetId);
    if (snippet.isEmpty()) {
      throw new EntityNotFoundException("Snippet not found with assetId " + assetId);
    }
    codeSnippet.setLanguage(snippet.get().getLanguage().name());
    codeSnippet.setVersion(snippet.get().getVersion());
    compileSnippet(codeSnippet);
    boolean canAccess = canWriteSnippet(assetId);
    if (!canAccess) {
      throw new PermissionDeniedDataAccessException(
          "You don't have permission to write this snippet",
          new Exception("You don't have permission to write this snippet"));
    }
    ResponseEntity<?> assetResponse =
        assetManager.createAsset("snippets", assetId, codeSnippet.getContent());
    if (assetResponse.getStatusCode().isError()) {
      throw new HttpServerErrorException(assetResponse.getStatusCode());
    }

    publishToRedis(codeSnippet.getContent(), snippet.get(), userId);

    return "Snippet updated successfully";
  }

  @Transactional
  public String deleteSnippet(String assetId) {
    boolean canAccess = canWriteSnippet(assetId);
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
    deletePermission(assetId);
    codeSnippetRepository.deleteCodeSnippetByAssetId(assetId);

    return "Snippet deleted successfully";
  }

  // ** Internal methods
  private CodeSnippet createAndSaveCodeSnippet(SnippetReceivedDto snippet) {
    CodeSnippet codeSnippet = new CodeSnippet();
    codeSnippet.setLanguage(snippet.getLanguageInEnum());
    codeSnippet.setVersion(snippet.getVersion());
    codeSnippet.setName(snippet.getName());
    codeSnippet.setExtension(snippet.getExtension());
    try {
      codeSnippetRepository.save(codeSnippet);
    } catch (Exception e) {
      TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
      throw new RuntimeException("Error saving the snippet", e);
    }
    return codeSnippet;
  }

  private SnippetSendDto convertToSnippetSendDto(
      CodeSnippet codeSnippets, MultipartFile content, String lintingResult, String userId) {
    return SnippetSendDto.builder()
        .language(codeSnippets.getLanguage().name())
        .version(codeSnippets.getVersion())
        .name(codeSnippets.getName())
        .assetId(codeSnippets.getAssetId())
        .content(getContentFromMultipartFile(content))
        .compliance(lintingResult)
        .userId(userId)
        .extension(codeSnippets.getExtension())
        .build();
  }

  @NotNull
  private SnippetSendDto getSnippetSendDto(String userId, CodeSnippet codeSnippet) {
    InputStream assetResponse = getAsset(codeSnippet.getAssetId());
    MultipartFile asset = toMultipartFile(assetResponse, codeSnippet.getAssetId());
    String lintResult = codeSnippet.getResultAsString();

    SnippetSendDto snippetDto =
            convertToSnippetSendDto(codeSnippet, asset, lintResult, userId);
    snippetDto.setUserId(userId);
    return snippetDto;
  }

  private AllSnippetsSendDto getAllSnippetsWithPermission(
      Integer from, Integer to, String userId, PermissionType permissionType) {
    AllSnippetsRecieveDto allSnippetsRecieveDto =
        permissionManager
            .getSnippetsUserWithPermission(from, to, permissionType.toString())
            .getBody();

    assert allSnippetsRecieveDto != null;
    List<SnippetSendDto> snippetSendDtos =
        Objects.requireNonNull(
            Objects.requireNonNull(allSnippetsRecieveDto.getSnippetsIds()).stream()
                .map(snippetReceived -> {
                  CodeSnippet snippet = findSnippetByAssetId(snippetReceived.getSnippetId());
                  SnippetSendDto snippetSendDto = getSnippetSendDto(userId, snippet);
                  snippetSendDto.setAuthor(getUserName(snippetReceived.getAuthor()));
                  return snippetSendDto;
                })
                .collect(Collectors.toList()));

    return AllSnippetsSendDto.builder()
        .snippets(snippetSendDtos)
        .maxSnippets(allSnippetsRecieveDto.getMaxSnippets())
        .build();
  }

  private String getUserName(String userId) {
    return userService.getUserName(userId);
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

  private void createDefaultRulesIfNeeded(String userId) {
    List<LintingRule> lintingRules = lintingRuleRepository.findAllByUserId(userId);
    List<FormatterRule> formatterRules = formatterRuleRepository.findAllByUserId(userId);

    if (lintingRules.isEmpty()) {
      lintingRuleService.createOrUpdateRules(DefaultRulesFactory.getDefaultLinterRules(), userId);
    }

    if (formatterRules.isEmpty()) {
      formatterRuleService.createOrUpdateRules(
          DefaultRulesFactory.getDefaultFormatterRules(), userId);
    }
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
    PermissionType permission = PermissionType.READ_WRITE;
    return permissionManager.createNewPermission(snippetId, permission);
  }

  private boolean canReadSnippet(String snippetId) {
    return permissionManager.canRead(snippetId);
  }

  private boolean canWriteSnippet(String snippetId) {
    return permissionManager.canWrite(snippetId);
  }

  private ResponseEntity<String> deletePermission(String snippetId) {
    return permissionManager.deletePermission(snippetId);
  }

  // ** Asset manager

  private ResponseEntity<String> createNewAsset(CodeSnippet snippet, MultipartFile content) {
    return assetManager.createAsset(assetManagerContainer, snippet.getAssetId(), content);
  }

  private InputStream getAsset(String assetId) {
    return assetManager.getAsset(assetManagerContainer, assetId);
  }

  private ResponseEntity<String> deleteAsset(String snippetId) {
    return assetManager.deleteAsset(assetManagerContainer, snippetId);
  }

  // ** Redis
  private void publishToRedis(MultipartFile content, CodeSnippet codeSnippet, String userId) {
    String result = codeSnippet.getResultAsString();
    lintProducer.publishEvent(convertToSnippetSendDto(codeSnippet, content, result, userId));
    formatterProducer.publishEvent(convertToSnippetSendDto(codeSnippet, content, result, userId));
  }

  public List<LanguagesDto> getLanguages() {
    List<Languages> languages = languagesRepository.findAll();
    return languages.stream()
        .map(
            language ->
                LanguagesDto.builder()
                    .language(language.getLanguage())
                    .extension(language.getExtension())
                    .build())
        .collect(Collectors.toList());
  }
}
