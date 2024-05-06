package cloud.anypoint.redis.internal.operation;

import static cloud.anypoint.redis.internal.util.ErrorDecorator.mapErrors;

import cloud.anypoint.redis.internal.connection.LettuceRedisConnection;
import cloud.anypoint.redis.internal.exception.NilValueException;
import cloud.anypoint.redis.internal.metadata.NilErrorTypeProvider;
import cloud.anypoint.redis.internal.metadata.TimeoutErrorTypeProvider;
import cloud.anypoint.redis.internal.metadata.WrongTypeErrorTypeProvider;
import cloud.anypoint.redis.internal.metadata.ZrankOutputTypeResolver;
import io.lettuce.core.ScoredValue;
import io.lettuce.core.ZAddArgs;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class SortedSetCommandOperations {
    private final Logger LOGGER = LoggerFactory.getLogger(SortedSetCommandOperations.class);

    @DisplayName("ZADD")
    @Throws({TimeoutErrorTypeProvider.class, WrongTypeErrorTypeProvider.class})
    public void zadd(@Connection LettuceRedisConnection connection,
                     String key,
                     @Content Map<String, Double> memberScores,
                     @DisplayName("XX") boolean xx,
                     @DisplayName("NX") boolean nx,
                     @DisplayName("GT") boolean gt,
                     @DisplayName("LT") boolean lt,
                     @DisplayName("CH") boolean ch,
                     CompletionCallback<Long, Void> callback) {
        LOGGER.debug("ZADD {}", key);
        ScoredValue<String>[] scoredValues = memberScores.entrySet().stream()
                .map((entry) -> ScoredValue.just(entry.getValue(), entry.getKey()))
                .toArray(ScoredValue[]::new);
        ZAddArgs args = new ZAddArgs();
        if (xx) {
            args = args.xx();
        }
        if (nx) {
            args = args.nx();
        }
        if (gt) {
            args = args.gt();
        }
        if (lt) {
            args = args.lt();
        }
        if (ch) {
            args = args.ch();
        }
        Mono<Long> cmd = connection.commands().zadd(key, args, scoredValues);
        mapErrors(cmd, "ZADD", key)
                .subscribe(
                        result -> callback.success(
                                Result.<Long, Void>builder()
                                        .output(result)
                                        .build()),
                        callback::error);
    }

    @DisplayName("ZSCORE")
    @Throws({TimeoutErrorTypeProvider.class, WrongTypeErrorTypeProvider.class})
    public void zscore(@Connection LettuceRedisConnection connection,
                       String key,
                       String member,
                       CompletionCallback<Double, Void> callback) {
        LOGGER.debug("ZSCORE {} {}", key, member);
        mapErrors(connection.commands().zscore(key, member), "ZSCORE", key)
            .subscribe(
                result -> callback.success(Result.<Double, Void>builder()
                        .output(result)
                        .build()),
                callback::error);
    }

    @DisplayName("ZRANK")
    @MediaType(value = "application/java", strict = true)
    @OutputResolver(output = ZrankOutputTypeResolver.class)
    @Throws({TimeoutErrorTypeProvider.class, NilErrorTypeProvider.class, WrongTypeErrorTypeProvider.class})
    public void zrank(@Connection LettuceRedisConnection connection,
                      String key,
                      String member,
                      @MetadataKeyId @Optional boolean withScore,
                      CompletionCallback<Object, Void> callback) {
        LOGGER.debug("ZRANK {} {}", key, member);
        Mono<Object> cmd = connection.commands().zrank(key, member).map(Function.identity());
        if (withScore) {
            cmd = connection.commands().zrankWithScore(key, member).map(scoredValue -> new HashMap<String, Object>() {{
                put("rank", scoredValue.getValue());
                put("score", scoredValue.getScore());
            }});
        }

        mapErrors(cmd, "ZRANK", key)
            .switchIfEmpty(Mono.error(new NilValueException("ZRANK", key)))
            .subscribe(
                result -> callback.success(Result.<Object, Void>builder()
                    .output(result)
                    .build()),
                callback::error);
    }
}
