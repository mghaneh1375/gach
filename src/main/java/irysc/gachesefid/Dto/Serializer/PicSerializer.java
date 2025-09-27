package irysc.gachesefid.Dto.Serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import irysc.gachesefid.DB.UserRepository;
import irysc.gachesefid.Utility.StaticValues;

import java.io.IOException;

public class PicSerializer extends JsonSerializer<String> {
    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
        } else {
            gen.writeString(StaticValues.STATICS_SERVER + UserRepository.FOLDER + "/" + value);
        }
    }
}
