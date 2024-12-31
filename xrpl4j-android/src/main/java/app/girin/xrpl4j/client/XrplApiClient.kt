package app.girin.xrpl4j.client

import com.fasterxml.jackson.databind.JsonNode
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.xrpl.xrpl4j.model.jackson.ObjectMapperFactory
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface XrplApiClient {
  companion object {
    fun create(url: HttpUrl): XrplApiClient {
      val objectMapper = ObjectMapperFactory.create()
      val builder = Retrofit.Builder().baseUrl(url).addConverterFactory(JacksonConverterFactory.create(objectMapper))
      val interceptor = HttpLoggingInterceptor()
      interceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
      val client = OkHttpClient.Builder().addInterceptor(interceptor).connectTimeout(10, TimeUnit.SECONDS).build()
      builder.client(client)
      return builder.build().create(XrplApiClient::class.java)
    }
  }

  @POST("/")
  @Headers("Accept: application/json", "Content-Type: application/json")
  suspend fun postRpcRequest(@Body rpcRequest: JsonRpcRequest): JsonNode
}
