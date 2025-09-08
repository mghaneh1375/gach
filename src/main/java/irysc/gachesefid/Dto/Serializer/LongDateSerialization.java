package irysc.gachesefid.Dto.Serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import irysc.gachesefid.Utility.Utility;

import java.io.IOException;

public class LongDateSerialization extends JsonSerializer<Long> {
    @Override
    public void serialize(Long date, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeString(date == null ? "" : Utility.getSolarDate(date));
    }
}
