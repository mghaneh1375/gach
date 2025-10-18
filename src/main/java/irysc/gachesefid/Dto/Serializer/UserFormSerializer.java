package irysc.gachesefid.Dto.Serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.base.CaseFormat;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.FormField;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import static irysc.gachesefid.Controllers.UserController.getWantedList;
import static irysc.gachesefid.Controllers.UserController.translateRole;

public class UserFormSerializer extends JsonSerializer<List<Document>> {
    @Override
    public void serialize(List<Document> document, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if(document == null) return;

        JSONArray formsJSON = new JSONArray();
        for (Document form : document) {
            JSONObject jsonObject1 = new JSONObject();
            String role = form.getString("role");

            FormField[] wantedList = getWantedList(role);
            if (wantedList == null)
                continue;

            jsonObject1.put("role", role)
                    .put("roleFa", translateRole(role));

            JSONArray data = new JSONArray();
            for (FormField field : wantedList) {
                JSONObject jsonObject2 = new JSONObject()
                        .put("key", field.key)
                        .put("value", form.getOrDefault(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, field.key), ""))
                        .put("help", field.help)
                        .put("title", field.title)
                        .put("isJustNum", field.isJustNum);

                if (field.pairValues != null) {
                    JSONArray jsonArray1 = new JSONArray();
                    for (PairValue p : field.pairValues)
                        jsonArray1.put(
                                new JSONObject()
                                        .put("id", p.getKey())
                                        .put("item", p.getValue())
                        );

                    jsonObject2.put("keyVals", jsonArray1);
                }

                data.put(jsonObject2);
            }

            jsonObject1.put("data", data);
            formsJSON.put(jsonObject1);
        }

        jsonGenerator.writeStartObject();

        jsonGenerator.writeFieldName("forms");
        writeJSONArray(jsonGenerator, formsJSON);

        jsonGenerator.writeEndObject();
    }

    private void writeJSONArray(JsonGenerator gen, JSONArray jsonArray) throws IOException {
        gen.writeStartArray();

        for (int i = 0; i < jsonArray.length(); i++) {
            Object item = jsonArray.get(i);
            writeJSONValue(gen, item);
        }

        gen.writeEndArray();
    }

    private void writeJSONValue(JsonGenerator gen, Object value) throws IOException {
        if (value == null || value == JSONObject.NULL) {
            gen.writeNull();
        } else if (value instanceof String) {
            gen.writeString((String) value);
        } else if (value instanceof Number) {
            if (value instanceof Integer) {
                gen.writeNumber((Integer) value);
            } else if (value instanceof Long) {
                gen.writeNumber((Long) value);
            } else if (value instanceof Double) {
                gen.writeNumber((Double) value);
            } else if (value instanceof Float) {
                gen.writeNumber((Float) value);
            } else {
                gen.writeNumber(value.toString());
            }
        } else if (value instanceof Boolean) {
            gen.writeBoolean((Boolean) value);
        } else if (value instanceof JSONObject) {
            writeJSONObject(gen, (JSONObject) value);
        } else if (value instanceof JSONArray) {
            writeJSONArray(gen, (JSONArray) value);
        } else {
            gen.writeString(value.toString());
        }
    }

    private void writeJSONObject(JsonGenerator gen, JSONObject jsonObject) throws IOException {
        gen.writeStartObject();

        for (String key : jsonObject.keySet()) {
            gen.writeFieldName(key);
            Object value = jsonObject.get(key);
            writeJSONValue(gen, value);
        }

        gen.writeEndObject();
    }
}
