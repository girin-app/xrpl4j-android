package app.girin.xrpl4j.client

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.xrpl.xrpl4j.model.client.XrplResult
import org.xrpl.xrpl4j.model.jackson.ObjectMapperFactory

object JsonParser {
  private val objectMapper: ObjectMapper = ObjectMapperFactory.create()

  @Throws(Exception::class)
  fun <T : XrplResult> parseResponse(json: JsonNode, resultType: Class<T>): T {
    return try {
      val javaType = objectMapper.constructType(resultType)
      parseResponse(json, javaType)
    } catch (e: Exception) {
      throw Exception(e)
    }
  }

  @Throws(Exception::class)
  fun <T : XrplResult> parseResponse(json: JsonNode, javaType: JavaType): T {
    val resultNode = json.get("result")
    return try {
      objectMapper.readValue(resultNode.toString(), javaType)
    } catch (e: Exception) {
      throw Exception(e)
    }
  }
}