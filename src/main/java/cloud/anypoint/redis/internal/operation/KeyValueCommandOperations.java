package cloud.anypoint.redis.internal.operation;

import static cloud.anypoint.redis.internal.util.ErrorDecorator.mapErrors;
import cloud.anypoint.redis.api.attributes.ScanAttributes;
import cloud.anypoint.redis.internal.exception.ArgumentException;
import cloud.anypoint.redis.internal.exception.NilValueException;
import cloud.anypoint.redis.internal.connection.LettuceRedisConnection;
import cloud.anypoint.redis.internal.metadata.ArgumentErrorTypeProvider;
import cloud.anypoint.redis.internal.metadata.NilErrorTypeProvider;
import cloud.anypoint.redis.internal.metadata.AllCommandsErrorTypeProvider;
import cloud.anypoint.redis.internal.metadata.WrongTypeErrorTypeProvider;
import io.lettuce.core.*;
import org.mule.runtime.core.api.util.StringUtils;
import org.mule.runtime.extension.api.annotation.dsl.xml.ParameterDsl;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.param.*;
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
    @Throws(AllCommandsErrorTypeProvider.class)
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
        mapErrors(cmd, "SET", key).subscribe(
            result -> callback.success(Result.<String, Void>builder()
                .output(result)
                .build()),
            callback::error);
    }

    @DisplayName("MSET")
    @Throws(AllCommandsErrorTypeProvider.class)
    public void mset(@Connection LettuceRedisConnection connection,
                     @Content Map<String, String> keyValues,
                     CompletionCallback<Void, Void> callback) {
        LOGGER.debug("MSET {}", keyValues.keySet());
        mapErrors(connection.commands().mset(keyValues), "MSET")
            .subscribe(result -> callback.success(Result.<Void, Void>builder().build()),
                callback::error);
    }

    @DisplayName("COPY")
    @Throws(AllCommandsErrorTypeProvider.class)
    public void copy(@Connection LettuceRedisConnection connection,
                     String source,
                     String destination,
                     @Optional Integer destinationDb,
                     @Optional boolean replace,
                     CompletionCallback<Boolean, Void> callback) {
        LOGGER.debug("COPY {} {}", source, destination);
        Mono<Boolean> cmd = connection.commands().copy(source, destination);
        if (null != destinationDb || replace) {
            CopyArgs args = new CopyArgs().replace(replace);
            if (null != destinationDb) {
                args = args.destinationDb(destinationDb);
            }
            cmd = connection.commands().copy(source, destination, args);
        }
        mapErrors(cmd, "COPY").subscribe(
            result -> callback.success(Result.<Boolean, Void>builder()
                .output(result)
                .build()),
            callback::error);
    }

    @DisplayName("APPEND")
    @Throws({WrongTypeErrorTypeProvider.class, AllCommandsErrorTypeProvider.class})
    public void append(@Connection LettuceRedisConnection connection,
                       String key,
                       @Content String value,
                       CompletionCallback<Long, Void> callback) {
        LOGGER.debug("APPEND {}", key);
        mapErrors(connection.commands().append(key, value), "APPEND")
            .subscribe(
                result -> callback.success(Result.<Long, Void>builder()
                    .output(result)
                    .build()),
                callback::error);
    }

    @DisplayName("INCR")
    @Throws({WrongTypeErrorTypeProvider.class, AllCommandsErrorTypeProvider.class})
    public void incr(@Connection LettuceRedisConnection connection,
                     String key,
                     CompletionCallback<Long, Void> callback) {
        LOGGER.debug("INCR {}", key);
        mapErrors(connection.commands().incr(key), "INCR", key)
            .subscribe(
                result -> callback.success(Result.<Long, Void>builder()
                    .output(result)
                    .build()),
                callback::error);
    }

    @DisplayName("DECR")
    @Throws({WrongTypeErrorTypeProvider.class, AllCommandsErrorTypeProvider.class})
    public void decr(@Connection LettuceRedisConnection connection,
                     String key,
                     CompletionCallback<Long, Void> callback) {
        LOGGER.debug("DECR {}", key);
        mapErrors(connection.commands().decr(key), "DECR", key)
            .subscribe(
                result -> callback.success(Result.<Long, Void>builder()
                    .output(result)
                    .build()),
                callback::error);
    }

    @DisplayName("GET")
    @MediaType(value = MediaType.TEXT_PLAIN, strict = false)
    @Throws({NilErrorTypeProvider.class, AllCommandsErrorTypeProvider.class})
    public void get(@Connection LettuceRedisConnection connection,
                    String key,
                    CompletionCallback<String, Void> callback) {
        LOGGER.debug("GET {}", key);
        mapErrors(connection.commands().get(key), "GET", key)
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
    @Throws({NilErrorTypeProvider.class, WrongTypeErrorTypeProvider.class, AllCommandsErrorTypeProvider.class})
    public void getrange(@Connection LettuceRedisConnection connection,
                         String key,
                         Integer start,
                         Integer end,
                         CompletionCallback<String, Void> callback) {
        LOGGER.debug("GETRANGE {} {} {}", key, start, end);
        Mono<String> cmd = connection.commands().getrange(key, start, end);
        mapErrors(cmd, "GETRANGE", key)
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
    @Throws({NilErrorTypeProvider.class, WrongTypeErrorTypeProvider.class, AllCommandsErrorTypeProvider.class})
    public void getdel(@Connection LettuceRedisConnection connection,
                       String key,
                       CompletionCallback<String, Void> callback) {
        LOGGER.debug("GETDEL {}", key);
        Mono<String> cmd = connection.commands().getdel(key);
        mapErrors(cmd, "GETDEL", key)
            // TODO: Add validator parameter to make this optional
            .switchIfEmpty(Mono.error(new NilValueException("GETDEL", key)))
            .subscribe(
                result -> callback.success(Result.<String, Void>builder()
                    .output(result)
                    .build()),
                callback::error
            );
    }

    @DisplayName("GETEX")
    @MediaType(value = MediaType.TEXT_PLAIN, strict = false)
    @Throws({NilErrorTypeProvider.class, AllCommandsErrorTypeProvider.class})
    public void getex(@Connection LettuceRedisConnection connection,
                      String key,
                      @DisplayName("EX") @Optional Long ex,
                      @DisplayName("PX") @Optional Long px,
                      @DisplayName("EXAT") @Optional Long exat,
                      @DisplayName("PXAT") @Optional Long pxat,
                      @DisplayName("PERSIST") @Optional boolean persist,
                      CompletionCallback<String, Void> callback) {
        LOGGER.debug("GETEX {}", key);
        GetExArgs args = new GetExArgs();
        int exclusiveArgCount = 0;
        if (null != ex) {
            args = args.ex(ex);
            exclusiveArgCount++;
        }
        if (null != px) {
            args = args.px(px);
            exclusiveArgCount++;
        }
        if (null != exat) {
            args = args.exAt(exat);
            exclusiveArgCount++;
        }
        if (null != pxat) {
            args = args.pxAt(pxat);
            exclusiveArgCount++;
        }
        if (persist) {
            args = args.persist();
            exclusiveArgCount++;
        }
        if (exclusiveArgCount > 1) {
            callback.error(new ArgumentException("GETEX", new IllegalArgumentException("only one of EX, PX, EXAT, PXAT, or PERSIST is supported")));
            return;
        }

        Mono<String> cmd = connection.commands().getex(key, args);
        mapErrors(cmd, "GETEX", key)
            // TODO: Add validator parameter to make this optional
            .switchIfEmpty(Mono.error(new NilValueException("GETEX", key)))
            .subscribe(
                result -> callback.success(Result.<String, Void>builder()
                    .output(result)
                    .build()),
                callback::error);
    }

    @DisplayName("MGET")
    @Throws({ArgumentErrorTypeProvider.class, AllCommandsErrorTypeProvider.class})
    public void mget(@Connection LettuceRedisConnection connection,
                     @ParameterDsl(allowReferences = false) List<String> keys,
                     CompletionCallback<List<String>, Void> callback) {
        LOGGER.debug("MGET {}", keys);
        try {
            Mono<List<String>> cmd = connection.commands().mget(keys.stream().toArray(String[]::new))
                .map(kv -> kv.getValueOrElse(null))
                .collectList();
            mapErrors(cmd, "MGET").subscribe(
                result -> callback.success(Result.<List<String>, Void>builder()
                    .output(result)
                    .build()),
                callback::error);
        } catch (IllegalArgumentException e) {
            callback.error(new ArgumentException("MGET", e));
        }
    }

    @DisplayName("TOUCH")
    @Throws({ArgumentErrorTypeProvider.class, AllCommandsErrorTypeProvider.class})
    public void touch(@Connection LettuceRedisConnection connection,
                      List<String> keys,
                      CompletionCallback<Void, Void> callback) {
        try {
            Mono<Long> cmd = connection.commands().touch(keys.stream().toArray(String[]::new));
            mapErrors(cmd, "TOUCH")
                .subscribe(result -> callback.success(Result.<Void, Void>builder().build()),
                    callback::error);
        } catch (IllegalArgumentException e) {
            callback.error(new ArgumentException("TOUCH", e));
        }
    }

    @DisplayName("GETSET")
    @MediaType(value = MediaType.TEXT_PLAIN, strict = false)
    @Throws({WrongTypeErrorTypeProvider.class, AllCommandsErrorTypeProvider.class})
    public void getset(@Connection LettuceRedisConnection connection,
                       String key,
                       @Content String value,
                       CompletionCallback<String, Void> callback) {
        LOGGER.debug("GETSET {}", key);
        mapErrors(connection.commands().getset(key, value), "GETSET", key)
                .subscribe(
                        result -> callback.success(Result.<String, Void>builder()
                                .output(result)
                                .build()),
                        callback::error
                );
    }

    @DisplayName("DEL")
    @Throws(AllCommandsErrorTypeProvider.class)
    public void del(@Connection LettuceRedisConnection connection,
                    List<String> keys,
                    CompletionCallback<Long, Void> callback) {
        LOGGER.debug("DEL {}", keys);
        mapErrors(connection.commands().del(keys.stream().toArray(String[]::new)), "DEL")
            .subscribe(
                result -> callback.success(Result.<Long, Void>builder()
                    .output(result)
                    .build()),
                callback::error
            );
    }

    @DisplayName("TTL")
    @Throws(AllCommandsErrorTypeProvider.class)
    public void ttl(@Connection LettuceRedisConnection connection,
                    String key,
                    CompletionCallback<Long, Void> callback) {
        LOGGER.debug("TTL {}", key);
        mapErrors(connection.commands().ttl(key), "TTL", key)
            .subscribe(
                result -> callback.success(Result.<Long, Void>builder()
                    .output(result)
                    .build()),
                callback::error);
    }

    @DisplayName("PTTL")
    @Throws(AllCommandsErrorTypeProvider.class)
    public void pttl(@Connection LettuceRedisConnection connection,
                    String key,
                    CompletionCallback<Long, Void> callback) {
        LOGGER.debug("PTTL {}", key);
        mapErrors(connection.commands().pttl(key), "PTTL", key)
            .subscribe(
                result -> callback.success(Result.<Long, Void>builder()
                    .output(result)
                    .build()),
                callback::error);
    }

    @DisplayName("EXPIRE")
    @Throws({ArgumentErrorTypeProvider.class, AllCommandsErrorTypeProvider.class})
    public void expire(@Connection LettuceRedisConnection connection,
                       String key,
                       Integer seconds,
                       @Optional @DisplayName("NX") boolean nx,
                       @Optional @DisplayName("XX") boolean xx,
                       @Optional @DisplayName("GT") boolean gt,
                       @Optional @DisplayName("LT") boolean lt,
                       CompletionCallback<Boolean, Void> callback) {
        LOGGER.debug("EXPIRE {} {}", key, seconds);
        if (nx && xx || gt && lt) {
            callback.error(new ArgumentException("EXPIRE", new IllegalArgumentException("NX and XX, GT or LT options at the same time are not compatible")));
            return;
        }
        ExpireArgs args = new ExpireArgs();
        if (nx) { args = args.nx(); }
        if (xx) { args = args.xx(); }
        if (gt) { args = args.gt(); }
        if (lt) { args = args.lt(); }
        mapErrors(connection.commands().expire(key, seconds, args), "EXPIRE", key)
            .subscribe(
                result -> callback.success(Result.<Boolean, Void>builder()
                    .output(result)
                    .build()),
                callback::error);
    }

    @DisplayName("PEXPIRE")
    @Throws({ArgumentErrorTypeProvider.class, AllCommandsErrorTypeProvider.class})
    public void pexpire(@Connection LettuceRedisConnection connection,
                       String key,
                       Integer milliseconds,
                       @Optional @DisplayName("NX") boolean nx,
                       @Optional @DisplayName("XX") boolean xx,
                       @Optional @DisplayName("GT") boolean gt,
                       @Optional @DisplayName("LT") boolean lt,
                       CompletionCallback<Boolean, Void> callback) {
        LOGGER.debug("PEXPIRE {} {}", key, milliseconds);
        if (nx && xx || gt && lt) {
            callback.error(new ArgumentException("PEXPIRE", new IllegalArgumentException("NX and XX, GT or LT options at the same time are not compatible")));
            return;
        }
        ExpireArgs args = new ExpireArgs();
        if (nx) { args = args.nx(); }
        if (xx) { args = args.xx(); }
        if (gt) { args = args.gt(); }
        if (lt) { args = args.lt(); }
        mapErrors(connection.commands().pexpire(key, milliseconds, args), "PEXPIRE", key)
            .subscribe(
                result -> callback.success(Result.<Boolean, Void>builder()
                    .output(result)
                    .build()),
                callback::error);
    }

    @DisplayName("PERSIST")
    @Throws(AllCommandsErrorTypeProvider.class)
    public void persist(@Connection LettuceRedisConnection connection,
                        String key,
                        CompletionCallback<Boolean, Void> callback) {
        LOGGER.debug("PERSIST {}", key);
        mapErrors(connection.commands().persist(key), "PERSIST", key)
            .subscribe(
                result -> callback.success(Result.<Boolean, Void>builder()
                    .output(result)
                    .build()),
                callback::error);
    }

    @DisplayName("SCAN")
    @Throws(AllCommandsErrorTypeProvider.class)
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
        mapErrors(connection.commands().scan(ScanCursor.of(cursor.toString()), args), "SCAN")
            .subscribe(
                result -> callback.success(
                    Result.<List<String>, ScanAttributes>builder()
                        .output(result.getKeys())
                        .attributes(new ScanAttributes() {{
                            LOGGER.trace("cursor {}", result.getCursor());
                            setCursor(Integer.parseInt(result.getCursor()));
                        }})
                        .build()),
                callback::error
            );
    }


}
