package cloud.anypoint.redis.internal.operation;

import static cloud.anypoint.redis.internal.util.ErrorDecorator.mapErrors;
import cloud.anypoint.redis.internal.connection.LettuceRedisConnection;
import cloud.anypoint.redis.internal.exception.ArgumentException;
import cloud.anypoint.redis.internal.exception.OutOfRangeException;
import cloud.anypoint.redis.internal.metadata.*;
import io.lettuce.core.RedisCommandExecutionException;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.param.*;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class ListCommandOperations {
    private final Logger LOGGER = LoggerFactory.getLogger(ListCommandOperations.class);

    @DisplayName("LPUSH")
    @Throws({AllCommandsErrorTypeProvider.class, WrongTypeErrorTypeProvider.class, ArgumentErrorTypeProvider.class})
    public void lpush(@Connection LettuceRedisConnection connection,
                      String key,
                      @Content List<String> members,
                      CompletionCallback<Long, Void> callback) {
        LOGGER.debug("LPUSH {}", key);
        try {
            mapErrors(connection.commands().lpush(key, members.stream().toArray(String[]::new)), "LPUSH", key)
                    .subscribe(
                        result -> callback.success(Result.<Long, Void>builder()
                            .output(result)
                            .build()),
                        callback::error);
        }
        catch (IllegalArgumentException e) {
            callback.error(new ArgumentException("LPUSH", e));
        }
    }

    @DisplayName("LPOP")
    @MediaType(value = "application/java", strict = true)
    @OutputResolver(output = OptionalCountOutputTypeResolver.class)
    @Throws({AllCommandsErrorTypeProvider.class, WrongTypeErrorTypeProvider.class})
    public void lpop(@Connection LettuceRedisConnection connection,
                     String key,
                     @MetadataKeyId @Optional Integer count,
                     CompletionCallback<Object, Void> callback) {
        LOGGER.debug("LPOP {}", key);
        Mono<Object> cmd = connection.commands().lpop(key).map(val -> Arrays.asList(val));
        if (null != count) {
//            if (count < 0)
            cmd = connection.commands().lpop(key, count).collectList().map(Function.identity());
        }
        mapErrors(cmd, "LPOP", key).subscribe(
            result -> callback.success(Result.<Object, Void>builder()
                    .output(result)
                    .build()),
            callback::error);
    }

    @DisplayName("LSET")
    @Throws({AllCommandsErrorTypeProvider.class, OutOfRangeErrorTypeProvider.class, WrongTypeErrorTypeProvider.class})
    public void lset(@Connection LettuceRedisConnection connection,
                     String key,
                     Long index,
                     @Content String element,
                     CompletionCallback<Void, Void> callback) {
        LOGGER.debug("LSET {}", key);
        Mono<String> cmd = connection.commands().lset(key, index, element);
        mapErrors(cmd, "LSET", key)
            .onErrorMap(RedisCommandExecutionException.class, OutOfRangeException::new)
            .subscribe(
                result -> callback.success(Result.<Void, Void>builder().build()),
                callback::error);
    }
}
