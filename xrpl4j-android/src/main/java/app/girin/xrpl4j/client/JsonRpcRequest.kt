package app.girin.xrpl4j.client

import org.xrpl.xrpl4j.model.client.XrplRequestParams

data class JsonRpcRequest(
  val method: String,
  val params: List<XrplRequestParams?> = emptyList(),
  val id: Int = 1,
  val jsonrpc: String = "2.0",
)
