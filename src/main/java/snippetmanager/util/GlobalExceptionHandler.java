package snippetmanager.util;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@ControllerAdvice
public class GlobalExceptionHandler {

  // Handle EntityNotFoundException globally
  @ExceptionHandler(EntityNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ResponseEntity<String> handleEntityNotFoundException(EntityNotFoundException ex) {
    return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
  }

  // Handle IllegalArgumentException globally
  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException ex) {
    return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
  }

  // Handle WebClientResponseException globally (for WebClient errors)
  @ExceptionHandler(WebClientResponseException.class)
  public ResponseEntity<String> handleWebClientResponseException(WebClientResponseException ex) {
    HttpStatusCode status = ex.getStatusCode();
    String errorMessage = "Error from external service: " + ex.getMessage();
    return new ResponseEntity<>(errorMessage, status);
  }

  // Handle PermissionDeniedDataAccessException globally
  @ExceptionHandler(PermissionDeniedDataAccessException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ResponseEntity<String> handlePermissionDeniedDataAccessException(
      PermissionDeniedDataAccessException ex) {
    return new ResponseEntity<>(ex.getMessage(), HttpStatus.FORBIDDEN);
  }

  // Handle other exceptions globally
  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ResponseEntity<String> handleGeneralException(Exception ex) {
    return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
