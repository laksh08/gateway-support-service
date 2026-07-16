package gateway.exception;

import java.net.URI;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * RFC 7807 Problem Details error handler for all uncaught exceptions.
 *
 * <p>Maps {@link GatewayException} subclasses to their specific HTTP status codes and structured
 * {@code application/problem+json} responses.
 */
@Component
@Order(-2)
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final List<HttpMessageWriter<?>> messageWriters;

    public GlobalExceptionHandler(
            org.springframework.http.codec.ServerCodecConfigurer serverCodecConfigurer) {
        this.messageWriters = serverCodecConfigurer.getWriters();
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ProblemDetail problem = toProblemDetail(ex);
        HttpStatus status = HttpStatus.resolve(problem.getStatus());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        if (status.is5xxServerError()) {
            log.error("[{}] {}", exchange.getRequest().getId(), ex.getMessage(), ex);
        } else {
            log.warn("[{}] {} – {}", exchange.getRequest().getId(), status, ex.getMessage());
        }

        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyValue(problem)
                .flatMap(
                        response ->
                                response.writeTo(
                                        exchange,
                                        new ServerResponse.Context() {
                                            @Override
                                            public List<HttpMessageWriter<?>> messageWriters() {
                                                return messageWriters;
                                            }

                                            @Override
                                            public List<ViewResolver> viewResolvers() {
                                                return List.of();
                                            }
                                        }));
    }

    private ProblemDetail toProblemDetail(Throwable ex) {
        if (ex instanceof GatewayException gex) {
            ProblemDetail pd =
                    ProblemDetail.forStatusAndDetail(gex.status(), gex.getMessage());
            pd.setType(URI.create("urn:gateway:error:" + gex.errorCode()));
            pd.setTitle(gex.errorCode());
            pd.setProperty("timestamp", Instant.now().toString());
            return pd;
        }
        if (ex instanceof ResponseStatusException rse) {
            ProblemDetail pd =
                    ProblemDetail.forStatusAndDetail(
                            HttpStatus.resolve(rse.getStatusCode().value()),
                            rse.getReason() != null ? rse.getReason() : rse.getMessage());
            pd.setProperty("timestamp", Instant.now().toString());
            return pd;
        }
        ProblemDetail pd =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        pd.setType(URI.create("urn:gateway:error:INTERNAL_ERROR"));
        pd.setTitle("Internal Server Error");
        pd.setProperty("timestamp", Instant.now().toString());
        return pd;
    }
}
