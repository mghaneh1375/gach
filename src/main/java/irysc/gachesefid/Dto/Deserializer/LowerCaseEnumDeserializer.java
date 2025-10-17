package irysc.gachesefid.Dto.Deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

public abstract class LowerCaseEnumDeserializer<T extends Enum<T>> extends JsonDeserializer<T> {

    private final Class<T> enumClass;

    protected LowerCaseEnumDeserializer(Class<T> enumClass) {
        this.enumClass = enumClass;
    }

    @Override
    public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();
        if (value == null) {
            return null;
        }
        return Enum.valueOf(enumClass, value.toUpperCase());
    }
}