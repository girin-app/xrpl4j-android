package app.girin.xrpl4j.client;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.xrpl.xrpl4j.model.client.XrplResult;
import org.xrpl.xrpl4j.model.jackson.ObjectMapperFactory;

public class JsonParser {
  private static final ObjectMapper objectMapper = ObjectMapperFactory.create();

  public static <T extends XrplResult> T parseResponse(JsonNode json, Class<T> resultType) throws Exception {
    try {
      JavaType javaType = objectMapper.constructType(resultType);
      return parseResponse(json, javaType);
    } catch (Exception e) {
      throw new Exception(e);
    }
  }

  public static <T extends XrplResult> T parseResponse(JsonNode json, JavaType javaType) throws Exception {
    JsonNode resultNode = json.get("result");
    try {
      return objectMapper.readValue(resultNode.toString(), javaType);
    } catch (Exception e) {
      throw new Exception(e);
    }
  }
}
