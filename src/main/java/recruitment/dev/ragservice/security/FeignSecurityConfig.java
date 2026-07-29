package recruitment.dev.ragservice.security;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.http.HttpHeaders;
@RequiredArgsConstructor
@Configuration
public class FeignSecurityConfig {


    @Bean
    public RequestInterceptor bearerTokenRequestInterceptor() {

        return requestTemplate -> {

            ServletRequestAttributes attributes =
                    (ServletRequestAttributes)
                            RequestContextHolder.getRequestAttributes();

            if (attributes == null) {
                return;
            }

            HttpServletRequest request =
                    attributes.getRequest();

            String authorizationHeader =
                    request.getHeader(
                            HttpHeaders.AUTHORIZATION
                    );

            if (
                    authorizationHeader != null
                            && authorizationHeader.startsWith("Bearer ")
            ) {
                requestTemplate.header(
                        HttpHeaders.AUTHORIZATION,
                        authorizationHeader
                );
            }
        };
    }
}