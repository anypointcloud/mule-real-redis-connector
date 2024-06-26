package cloud.anypoint.redis.internal.metadata;

import cloud.anypoint.redis.api.CommandReturnType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;

import java.util.Collections;
import java.util.Set;

public class DynamicCommandOutputTypeResolver implements TypeKeysResolver, OutputTypeResolver<CommandReturnType> {
    @Override
    public MetadataType getOutputType(MetadataContext metadataContext, CommandReturnType commandReturnType) throws MetadataResolvingException, ConnectionException {
        switch (commandReturnType) {
            case STATUS:
                return metadataContext.getTypeBuilder().stringType().build();
            case STRING:
                return metadataContext.getTypeBuilder().stringType().build();
            case LONG:
                return metadataContext.getTypeBuilder().numberType().build();
            case ARRAY:
                return metadataContext.getTypeBuilder().arrayType().of(metadataContext.getTypeBuilder().stringType()).build();
            default:
                return metadataContext.getTypeBuilder().anyType().build();
        }
    }

    @Override
    public String getCategoryName() {
        return "Redis Reply";
    }

    @Override
    public Set<MetadataKey> getKeys(MetadataContext metadataContext) throws MetadataResolvingException, ConnectionException {
        return Collections.emptySet();
    }

    @Override
    public String getResolverName() {
        return TypeKeysResolver.super.getResolverName();
    }
}
