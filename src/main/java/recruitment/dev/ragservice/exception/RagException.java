package recruitment.dev.ragservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class RagException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public RagException(String message) {
        this(message, "RAG_ERROR", HttpStatus.BAD_REQUEST);
    }

    public RagException(String message, Throwable cause) {
        this(message, "RAG_ERROR", HttpStatus.BAD_REQUEST, cause);
    }

    public RagException(String message, HttpStatus status) {
        this(message, "RAG_ERROR", status);
    }

    public RagException(String message, HttpStatus status, Throwable cause) {
        this(message, "RAG_ERROR", status, cause);
    }

    public RagException(String message, String errorCode, HttpStatus status) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public RagException(String message, String errorCode, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
    }
}
