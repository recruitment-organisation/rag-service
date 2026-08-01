package recruitment.dev.ragservice.dto.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Minimal representation of Spring Data's paginated JSON response used by Feign clients. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PageResult<T>(List<T> content) {
    public PageResult {
        content = content == null ? List.of() : List.copyOf(content);
    }
}
