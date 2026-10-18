package com.websec.websec.exception;

import com.websec.websec.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

// Only short, generic messages go back to the client. No stack traces, no SQL, no internals.
@RestControllerAdvice
public class ApiExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ErrorResponse> handleApi(ApiException e) {
		return error(e.getStatus(), e.getMessage());
	}

	@ExceptionHandler(BindException.class)
	public ResponseEntity<ErrorResponse> handleBind(BindException e) {
		FieldError field = e.getFieldError();
		return error(HttpStatus.BAD_REQUEST, field == null ? "Invalid request" : "Invalid " + field.getField());
	}

	@ExceptionHandler({ HandlerMethodValidationException.class, HttpMessageNotReadableException.class,
			MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class,
			MissingServletRequestPartException.class, MultipartException.class })
	public ResponseEntity<ErrorResponse> handleBadRequest(Exception e) {
		return error(HttpStatus.BAD_REQUEST, "Invalid request");
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ErrorResponse> handleTooLarge(MaxUploadSizeExceededException e) {
		return error(HttpStatus.PAYLOAD_TOO_LARGE, "File is too large");
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> handleConflict(DataIntegrityViolationException e) {
		return error(HttpStatus.CONFLICT, "Conflict");
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ErrorResponse> handleMethod(HttpRequestMethodNotSupportedException e) {
		return error(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed");
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException e) {
		return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type");
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException e) {
		return error(HttpStatus.NOT_FOUND, "Not found");
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleOther(Exception e) {
		StackTraceElement[] trace = e.getStackTrace();
		log.error("Unhandled error: {} at {}", e.getClass().getName(), trace.length > 0 ? trace[0] : "unknown");
		return error(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong");
	}

	private ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
		return ResponseEntity.status(status).body(new ErrorResponse(message));
	}
}
