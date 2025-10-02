package irysc.gachesefid.Dto.Serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import irysc.gachesefid.Utility.StaticValues;

import java.io.IOException;

public class AgeSerializer extends JsonSerializer<Long> {
    @Override
    public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
        } else {
            gen.writeString(((System.currentTimeMillis() - value) / StaticValues.ONE_YEAR_MIL_SEC) + "");
        }
    }
}
