package recruitment.dev.ragservice.client;

import feign.FeignException;
import org.springframework.http.HttpStatus;
import recruitment.dev.ragservice.exception.RagException;

final class RagFeignFallbacks {

    private RagFeignFallbacks() {
    }

    static RagException unavailable(String dependency, Throwable cause) {
        if (cause instanceof RagException exception) {
            return exception;
        }
        if (cause instanceof FeignException.NotFound) {
            return new RagException(
                    "A required resource was not found in " + dependency,
                    "REMOTE_RESOURCE_NOT_FOUND",
                    HttpStatus.NOT_FOUND,
                    cause
            );
        }
        return new RagException(
                "Required dependency is unavailable: " + dependency,
                "REMOTE_SERVICE_UNAVAILABLE",
                HttpStatus.SERVICE_UNAVAILABLE,
                cause
        );
    }
}
