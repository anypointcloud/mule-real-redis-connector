package cloud.anypoint.redis.internal.operation;

import static cloud.anypoint.redis.internal.util.ErrorDecorator.mapWrongTypeError;
import cloud.anypoint.redis.api.ScanAttributes;
import cloud.anypoint.redis.internal.NilValueException;
import cloud.anypoint.redis.internal.connection.LettuceRedisConnection;
import cloud.anypoint.redis.internal.metadata.NilErrorTypeProvider;
import cloud.anypoint.redis.internal.metadata.WrongTypeErrorTypeProvider;
import io.lettuce.core.KeyScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.SetArgs;
import org.mule.runtime.core.api.util.StringUtils;
import org.mule.runtime.extension.api.annotation.error.Throws;
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

import java.util.List;
import java.util.Map;

public class KeyValueCommandOperations {
    private final Logger LOGGER = LoggerFactory.getLogger(KeyValueCommandOperations.class);
    @DisplayName("SET")
    @MediaType(value = MediaType.TEXT_PLAIN, strict = false)
    public void set(@Connection LettuceRedisConnection connection,
                    String key,
                    @Content String value,
                    @Optional @DisplayName("XX") boolean xx,
                    @Optional @DisplayName("NX") boolean nx,
                    @Optional @DisplayName("GET") boolean get,
                    @Optional @DisplayName("EX") Integer ex,
                    @Optional @DisplayName("PX") Integer px,
                    @Optional @DisplayName("EXAT") Integer exat,
                    @Optional @DisplayName("PXAT") Integer pxat,
                    @Optional @DisplayName("KEEPTTL") boolean keepttl,
                    CompletionCallback<String, Void> callback) {
        LOGGER.debug("SET {}", key);
        SetArgs args = new SetArgs();
        if (xx) {
            args = args.xx();
        }
        if (nx) {
            args = args.nx();
        }
        if (null != ex) {
            args = args.ex(ex);
        }
        if (null != px) {
            args = args.px(px);
        }
        if (null != exat) {
            args = args.exAt(exat);
        }
        if (null != pxat) {
            args = args.pxAt(pxat);
        }
        if (keepttl) {
            args = args.keepttl();
        }
        Mono<String> cmd = connection.commands().set(key, value, args);
        if (get) {
            cmd = connection.commands().setGet(key, value, args);
        }
        cmd.subscribe(
                result -> callback.success(Result.<String, Void>builder()
                        .output(result)
                        .build()),
                callback::error);
    }

    @DisplayName("MSET")
    public void mset(@Connection LettuceRedisConnection connection,
                     @Content Map<String, String> keyValues,
                     CompletionCallback<Void, Void> callback) {
        LOGGER.debug("MSET {}", keyValues.keySet());
        connection.commands().mset(keyValues)
                .subscribe(result -> callback.success(Result.<Void, Void>builder().build()),
                        callback::error);
    }

    @DisplayName("APPEND")
    @Throws(WrongTypeErrorTypeProvider.class)
    public void append(@Connection LettuceRedisConnection connection,
                       String key,
                       @Content String value,
                       CompletionCallback<Long, Void> callback) {
        LOGGER.debug("APPEND {}", key);
        connection.commands().append(key, value)
                .subscribe(result -> callback.success(Result.<Long, Void>builder()
                        .output(result)
                        .build()),
                    callback::error);
    }

    @DisplayName("GET")
    @MediaType(value = MediaType.TEXT_PLAIN, strict = false)
    @Throws({NilErrorTypeProvider.class, WrongTypeErrorTypeProvider.class})
    public void get(@Connection LettuceRedisConnection connection,
                    String key,
                    CompletionCallback<String, Void> callback) {
        LOGGER.debug("GET {}", key);
        mapWrongTypeError(connection.commands().get(key), "GET", key)
                // TODO: Add validator parameter to make this optional
                .switchIfEmpty(Mono.error(new NilValueException("GET", key)))
                .subscribe(
                    result -> {
                        LOGGER.trace("GET result {}", result);
                        callback.success(Result.<String, Void>builder()
                                .output(result)
                                .build());
                    },
                    callback::error);
    }

    @DisplayName("GETRANGE")
    @MediaType(value = MediaType.TEXT_PLAIN, strict = false)
    @Throws({NilErrorTypeProvider.class, WrongTypeErrorTypeProvider.class})
    public void getrange(@Connection LettuceRedisConnection connection,
                         String key,
                         Integer start,
                         Integer end,
                         CompletionCallback<String, Void> callback) {
        LOGGER.debug("GETRANGE {} {} {}", key, start, end);
        Mono<String> cmd = connection.commands().getrange(key, start, end);
        mapWrongTypeError(cmd, "GETRANGE", key)
                // TODO: Add validator parameter to make this optional
                .switchIfEmpty(Mono.error(new NilValueException("GETRANGE", key)))
                .subscribe(
                    result -> callback.success(Result.<String, Void>builder()
                        .output(result)
                        .build()),
                    callback::error);
    }

    @DisplayName("GETDEL")
    @MediaType(value = MediaType.TEXT_PLAIN, strict = false)
    @Throws({NilErrorTypeProvider.class, WrongTypeErrorTypeProvider.class})
    public void getdel(@Connection LettuceRedisConnection connection,
                       String key,
                       CompletionCallback<String, Void> callback) {
        LOGGER.debug("GETDEL {}", key);
        Mono<String> cmd = connection.commands().getdel(key);
        mapWrongTypeError(cmd, "GETDEL", key)
                // TODO: Add validator parameter to make this optional
                .switchIfEmpty(Mono.error(new NilValueException("GETDEL", key)))
                .subscribe(
                    result -> callback.success(Result.<String, Void>builder()
                        .output(result)
                        .build()),
                    callback::error
                );
    }

    @DisplayName("MGET")
    public void mget(@Connection LettuceRedisConnection connection,
                     List<String> keys,
                     CompletionCallback<List<String>, Void> callback) {
        LOGGER.debug("MGET {}", keys);
        connection.commands().mget(keys.stream().toArray(String[]::new))
                .map(kv -> kv.getValueOrElse(null))
                .collectList()
                .subscribe(
                        result -> callback.success(Result.<List<String>, Void>builder()
                                .output(result)
                                .build()),
                        callback::error
                );
    }

    @DisplayName("GETSET")
    @MediaType(value = MediaType.TEXT_PLAIN, strict = false)
    @Throws(WrongTypeErrorTypeProvider.class)
    public void getset(@Connection LettuceRedisConnection connection,
                       String key,
                       @Content String value,
                       CompletionCallback<String, Void> callback) {
        LOGGER.debug("GETSET {}", key);
        mapWrongTypeError(connection.commands().getset(key, value), "GETSET", key)
                .subscribe(
                        result -> callback.success(Result.<String, Void>builder()
                                .output(result)
                                .build()),
                        callback::error
                );
    }

    @DisplayName("DEL")
    public void del(@Connection LettuceRedisConnection connection,
                    List<String> keys,
                    CompletionCallback<Long, Void> callback) {
        LOGGER.debug("DEL {}", keys);
        connection.commands().del(keys.stream().toArray(String[]::new))
                .subscribe(
                        result -> callback.success(Result.<Long, Void>builder()
                                .output(result)
                                .build()),
                        callback::error
                );
    }

    @DisplayName("TTL")
    public void ttl(@Connection LettuceRedisConnection connection,
                    String key,
                    CompletionCallback<Long, Void> callback) {
        LOGGER.debug("TTL {}", key);
        connection.commands().ttl(key)
                .subscribe(
                        result -> callback.success(Result.<Long, Void>builder()
                                .output(result)
                                .build()),
                        callback::error
                );
    }

    @DisplayName("PTTL")
    public void pttl(@Connection LettuceRedisConnection connection,
                    String key,
                    CompletionCallback<Long, Void> callback) {
        LOGGER.debug("PTTL {}", key);
        connection.commands().pttl(key)
                .subscribe(
                        result -> callback.success(Result.<Long, Void>builder()
                                .output(result)
                                .build()),
                        callback::error
                );
    }

    @DisplayName("SCAN")
    public void scan(@Connection LettuceRedisConnection connection,
                     Integer cursor,
                     @Optional String match,
                     @Optional Integer count,
                     @Optional String type,
                     CompletionCallback<List<String>, ScanAttributes> callback) {
        KeyScanArgs args = new KeyScanArgs();
        if (!StringUtils.isEmpty(match)) {
            args.match(match);
        }
        if (null != count) {
            args.limit(count);
        }
        if (!StringUtils.isEmpty(type)) {
            args.type(type);
        }
        LOGGER.debug("SCAN {}", cursor);
        connection.commands().scan(ScanCursor.of(cursor.toString()), args)
                .subscribe(
                    result -> callback.success(
                        Result.<List<String>, ScanAttributes>builder()
                            .output(result.getKeys())
                            .attributes(new ScanAttributes() {{
                                LOGGER.debug("cursor {}", result.getCursor());
                                setCursor(Integer.parseInt(result.getCursor()));
                            }})
                            .build()),
                    callback::error
                );
    }
}