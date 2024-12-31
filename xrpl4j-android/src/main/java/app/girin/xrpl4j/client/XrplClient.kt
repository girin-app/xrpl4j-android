package app.girin.xrpl4j.client

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.common.annotations.Beta
import com.google.common.collect.Range
import com.google.common.primitives.UnsignedInteger
import com.google.common.primitives.UnsignedLong
import okhttp3.HttpUrl
import org.bitcoinj.crypto.DeterministicHierarchy
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.crypto.HDPath
import org.bitcoinj.wallet.DeterministicSeed
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.xrpl.xrpl4j.codec.addresses.UnsignedByteArray
import org.xrpl.xrpl4j.codec.binary.XrplBinaryCodec
import org.xrpl.xrpl4j.crypto.keys.KeyPair
import org.xrpl.xrpl4j.crypto.keys.PrivateKey
import org.xrpl.xrpl4j.crypto.keys.PublicKey
import org.xrpl.xrpl4j.crypto.signing.MultiSignedTransaction
import org.xrpl.xrpl4j.crypto.signing.SignatureService
import org.xrpl.xrpl4j.crypto.signing.SingleSignedTransaction
import org.xrpl.xrpl4j.crypto.signing.bc.BcSignatureService
import org.xrpl.xrpl4j.model.client.XrplMethods
import org.xrpl.xrpl4j.model.client.accounts.AccountChannelsRequestParams
import org.xrpl.xrpl4j.model.client.accounts.AccountChannelsResult
import org.xrpl.xrpl4j.model.client.accounts.AccountCurrenciesRequestParams
import org.xrpl.xrpl4j.model.client.accounts.AccountCurrenciesResult
import org.xrpl.xrpl4j.model.client.accounts.AccountInfoRequestParams
import org.xrpl.xrpl4j.model.client.accounts.AccountInfoResult
import org.xrpl.xrpl4j.model.client.accounts.AccountLinesRequestParams
import org.xrpl.xrpl4j.model.client.accounts.AccountLinesResult
import org.xrpl.xrpl4j.model.client.accounts.AccountNftsRequestParams
import org.xrpl.xrpl4j.model.client.accounts.AccountNftsResult
import org.xrpl.xrpl4j.model.client.accounts.AccountObjectsRequestParams
import org.xrpl.xrpl4j.model.client.accounts.AccountObjectsResult
import org.xrpl.xrpl4j.model.client.accounts.AccountOffersRequestParams
import org.xrpl.xrpl4j.model.client.accounts.AccountOffersResult
import org.xrpl.xrpl4j.model.client.accounts.AccountTransactionsRequestParams
import org.xrpl.xrpl4j.model.client.accounts.AccountTransactionsResult
import org.xrpl.xrpl4j.model.client.accounts.GatewayBalancesRequestParams
import org.xrpl.xrpl4j.model.client.accounts.GatewayBalancesResult
import org.xrpl.xrpl4j.model.client.amm.AmmInfoRequestParams
import org.xrpl.xrpl4j.model.client.amm.AmmInfoResult
import org.xrpl.xrpl4j.model.client.channels.ChannelVerifyRequestParams
import org.xrpl.xrpl4j.model.client.channels.ChannelVerifyResult
import org.xrpl.xrpl4j.model.client.common.LedgerSpecifier
import org.xrpl.xrpl4j.model.client.fees.FeeResult
import org.xrpl.xrpl4j.model.client.ledger.LedgerEntryRequestParams
import org.xrpl.xrpl4j.model.client.ledger.LedgerEntryResult
import org.xrpl.xrpl4j.model.client.ledger.LedgerRequestParams
import org.xrpl.xrpl4j.model.client.ledger.LedgerResult
import org.xrpl.xrpl4j.model.client.nft.NftBuyOffersRequestParams
import org.xrpl.xrpl4j.model.client.nft.NftBuyOffersResult
import org.xrpl.xrpl4j.model.client.nft.NftInfoRequestParams
import org.xrpl.xrpl4j.model.client.nft.NftInfoResult
import org.xrpl.xrpl4j.model.client.nft.NftSellOffersRequestParams
import org.xrpl.xrpl4j.model.client.nft.NftSellOffersResult
import org.xrpl.xrpl4j.model.client.oracle.GetAggregatePriceRequestParams
import org.xrpl.xrpl4j.model.client.oracle.GetAggregatePriceResult
import org.xrpl.xrpl4j.model.client.path.BookOffersRequestParams
import org.xrpl.xrpl4j.model.client.path.BookOffersResult
import org.xrpl.xrpl4j.model.client.path.DepositAuthorizedRequestParams
import org.xrpl.xrpl4j.model.client.path.DepositAuthorizedResult
import org.xrpl.xrpl4j.model.client.path.RipplePathFindRequestParams
import org.xrpl.xrpl4j.model.client.path.RipplePathFindResult
import org.xrpl.xrpl4j.model.client.serverinfo.ServerInfoResult
import org.xrpl.xrpl4j.model.client.transactions.SubmitMultiSignedRequestParams
import org.xrpl.xrpl4j.model.client.transactions.SubmitMultiSignedResult
import org.xrpl.xrpl4j.model.client.transactions.SubmitRequestParams
import org.xrpl.xrpl4j.model.client.transactions.SubmitResult
import org.xrpl.xrpl4j.model.client.transactions.TransactionRequestParams
import org.xrpl.xrpl4j.model.client.transactions.TransactionResult
import org.xrpl.xrpl4j.model.immutables.FluentCompareTo
import org.xrpl.xrpl4j.model.jackson.ObjectMapperFactory
import org.xrpl.xrpl4j.model.ledger.LedgerObject
import org.xrpl.xrpl4j.model.transactions.Address
import org.xrpl.xrpl4j.model.transactions.Transaction
import java.util.Objects

@Beta
class XrplClient(val url: HttpUrl) {
    private val binaryCodec = XrplBinaryCodec.getInstance()
    private val objectMapper = ObjectMapperFactory.create()

    fun keyPair(mnemonic: String): KeyPair {
        val seed = DeterministicSeed(mnemonic, null, "", 0L)
        val path = HDPath.parsePath("44H/144H/0H/0/0")
        val rootKey = HDKeyDerivation.createMasterPrivateKey(seed.seedBytes)
        val derivedKey: DeterministicKey = DeterministicHierarchy(rootKey).get(path, true, true)
        val privateKey = PrivateKey.fromPrefixedBytes(UnsignedByteArray.of(byteArrayOf(0) + derivedKey.privKeyBytes))
        val publicKey = PublicKey.builder().value(UnsignedByteArray.fromHex(derivedKey.publicKeyAsHex)).build()
        return KeyPair.builder().privateKey(privateKey).publicKey(publicKey).build()
    }

    suspend fun sendTransaction(txString: String, keyPair: KeyPair, sequence: Int): SubmitResult<Transaction> {
        val objectMapper = ObjectMapperFactory.create()
        val txJsonNode = objectMapper.readTree(txString)
        (txJsonNode as? ObjectNode)?.let {
            it.put("Sequence", sequence)
            it.put("SigningPubKey", keyPair.publicKey().base16Value())
        }
        val transaction = objectMapper.treeToValue(txJsonNode, Transaction::class.java)
        return this.submitTx(transaction, keyPair)
    }

    suspend fun <T : Transaction> submitTx(message: T, keyPair: KeyPair): SubmitResult<T> {
        val signatureService: SignatureService<PrivateKey> = BcSignatureService()
        val signedTx: SingleSignedTransaction<T> = signatureService.sign(keyPair.privateKey(), message)
        val res = submit(signedTx)
        return res
    }

    suspend fun <T : Transaction?> submit(signedTransaction: SingleSignedTransaction<T>): SubmitResult<T> {
        Objects.requireNonNull(signedTransaction)

        if (LOGGER.isDebugEnabled) {
            LOGGER.debug("About to submit signedTransaction: {}", signedTransaction)
        }

        val signedJson: String = objectMapper.writeValueAsString(signedTransaction.signedTransaction())
        val signedBlob: String = binaryCodec.encode(signedJson)
        val request = JsonRpcRequest(XrplMethods.SUBMIT, listOf(SubmitRequestParams.of(signedBlob)))

        if (LOGGER.isDebugEnabled) {
            LOGGER.debug("About to submit app.girin.xrpl4j.client.JsonRpcRequest: {}", request)
        }

        val apiClient = XrplApiClient.create(url)
        val response = apiClient.postRpcRequest(request)
        val resultType: JavaType = objectMapper.typeFactory.constructParametricType(SubmitResult::class.java, signedTransaction.javaClass)
        return JsonParser.parseResponse(response, resultType)
    }

    suspend fun <T : Transaction?> submitMultisigned(transaction: MultiSignedTransaction<T>): SubmitMultiSignedResult<T> {
        Objects.requireNonNull(transaction)

        val request = JsonRpcRequest(XrplMethods.SUBMIT_MULTISIGNED, listOf(SubmitMultiSignedRequestParams.of(transaction.signedTransaction())))
        val apiClient = XrplApiClient.create(url)
        val response = apiClient.postRpcRequest(request)

        val resultType = objectMapper.typeFactory.constructParametricType(SubmitMultiSignedResult::class.java, transaction.javaClass)
        return JsonParser.parseResponse(response, resultType)
    }

    suspend fun fee(): FeeResult {
        val request = JsonRpcRequest(XrplMethods.FEE)
        val apiClient = XrplApiClient.create(url)
        val response = apiClient.postRpcRequest(request)

        return JsonParser.parseResponse(response, FeeResult::class.java)
    }

    suspend fun mostRecentlyValidatedLedgerIndex(): UnsignedInteger {
        val request = JsonRpcRequest(XrplMethods.LEDGER, listOf(LedgerRequestParams.builder().ledgerSpecifier(LedgerSpecifier.VALIDATED).build()))
        val apiClient = XrplApiClient.create(url)
        val response = apiClient.postRpcRequest(rpcRequest = request)

        val ledgerResult = JsonParser.parseResponse(response, LedgerResult::class.java)
        return ledgerResult.ledgerIndexSafe().unsignedIntegerValue()
    }

//    /**
//     * Get the [TransactionResult] for the transaction with the hash transactionHash.
//     *
//     * @param transactionHash [Hash256] of the transaction to get the TransactionResult for.
//     *
//     * @return the [TransactionResult] for a validated transaction and empty response for a [Transaction] that
//     * is expired or not found.
//     */
//    protected suspend fun getValidatedTransaction(
//        transactionHash: Hash256
//    ): Optional<out TransactionResult<out Transaction>> {
//        Objects.requireNonNull(transactionHash)
//
//        try {
//            val transactionResult: TransactionResult<out Transaction> = this.transaction(
//                TransactionRequestParams.of(transactionHash)
//            )
//            return Optional.ofNullable(transactionResult).filter(Predicate<TransactionResult<Transaction?>> { obj: TransactionResult<Transaction?> -> obj.validated() })
//        } catch (e: Exception) {
//            // The transaction was not found on ledger, warn on this, but otherwise return.
//            LOGGER.warn(e.message, e)
//            return Optional.empty<TransactionResult<out Transaction>>()
//        }
//    }

    protected suspend fun ledgerGapsExistBetween(
        submittedLedgerSequence: UnsignedLong,
        lastLedgerSequence: UnsignedLong?,
    ): Boolean {
        var lastLedgerSequence: UnsignedLong? = lastLedgerSequence
        Objects.requireNonNull(submittedLedgerSequence)
        Objects.requireNonNull<UnsignedLong?>(lastLedgerSequence)

        val serverInfo: ServerInfoResult
        try {
            serverInfo = this.serverInformation()
        } catch (e: Exception) {
            LOGGER.error(e.message, e)
            return true // Assume ledger gaps exist so this can be retried.
        }

        // Ensure the lastLedgerSequence is (at least) as large as submittedLedgerSequence
        if (FluentCompareTo.`is`<UnsignedLong>(lastLedgerSequence).lessThan(submittedLedgerSequence)) {
            lastLedgerSequence = submittedLedgerSequence
        }

        val submittedToLast: Range<UnsignedLong?> = Range.closed<UnsignedLong?>(submittedLedgerSequence, lastLedgerSequence)
        return serverInfo.info().completeLedgers().stream().noneMatch { range: Range<UnsignedLong?> -> range.encloses(submittedToLast) }
    }

    //    /**
//     * Check if the transaction is final on the ledger or not.
//     *
//     * @param transactionHash            [Hash256] of the submitted transaction to check the status for.
//     * @param submittedOnLedgerIndex     [LedgerIndex] on which the transaction with hash transactionHash was
//     * submitted. This can be obtained from submit() response of the tx as
//     * validatedLedgerIndex.
//     * @param lastLedgerSequence         The ledger index/sequence of type [UnsignedInteger] after which the
//     * transaction will expire and won't be applied to the ledger.
//     * @param transactionAccountSequence The sequence number of the account submitting the [Transaction]. A
//     * [Transaction] is only valid if the Sequence number is exactly 1 greater
//     * than the previous transaction from the same account.
//     * @param account                    The unique [Address] of the account that initiated this transaction.
//     *
//     * @return `true` if the [Transaction] is final/validated else `false`.
//     */
//    fun isFinal(
//        transactionHash: Hash256, submittedOnLedgerIndex: LedgerIndex, lastLedgerSequence: UnsignedInteger, transactionAccountSequence: UnsignedInteger?, account: Address?
//    ): Finality {
//        return getValidatedTransaction(transactionHash).map<ImmutableFinality> { transactionResult: TransactionResult<out Transaction?> ->
//            // Note from https://xrpl.org/transaction-metadata.html#transaction-metadata:
//            // "Any transaction that gets included in a ledger has metadata, regardless of whether it is
//            // successful." However, we handle missing metadata as a failure, just in case rippled doesn't perfectly
//            // conform
//            val isTesSuccess: Boolean = transactionResult.metadata().map<String>(Function<TransactionMetadata, String> { obj: TransactionMetadata -> obj.transactionResult() }).filter(Predicate<String> { anObject: String? -> "tesSUCCESS".equals(anObject) }).map<Boolean>(Function<String, Boolean> { `$`: String? -> true }).isPresent()
//
//            val metadataExists: Boolean = transactionResult.metadata().isPresent()
//            if (isTesSuccess) {
//                LOGGER.debug("Transaction with hash: {} was validated with success", transactionHash)
//                return@map Finality.builder().finalityStatus(FinalityStatus.VALIDATED_SUCCESS).resultCode(transactionResult.metadata().get().transactionResult()).build()
//            } else if (!metadataExists) {
//                return@map Finality.builder().finalityStatus(FinalityStatus.VALIDATED_UNKNOWN).build()
//            } else {
//                LOGGER.debug("Transaction with hash: {} was validated with failure", transactionHash)
//                return@map Finality.builder().finalityStatus(FinalityStatus.VALIDATED_FAILURE).resultCode(transactionResult.metadata().get().transactionResult()).build()
//            }
//        }.orElseGet(Supplier<ImmutableFinality> {
//            try {
//                val isTransactionExpired: Boolean = FluentCompareTo.`is`<UnsignedInteger>(mostRecentlyValidatedLedgerIndex).greaterThan(lastLedgerSequence)
//                if (!isTransactionExpired) {
//                    LOGGER.debug("Transaction with hash: {} has not expired yet, check again", transactionHash)
//                    return@orElseGet Finality.builder().finalityStatus(FinalityStatus.NOT_FINAL).build()
//                } else {
//                    val isMissingLedgers = ledgerGapsExistBetween(
//                        UnsignedLong.valueOf(submittedOnLedgerIndex.toString()), UnsignedLong.valueOf(lastLedgerSequence.toString())
//                    )
//                    if (isMissingLedgers) {
//                        LOGGER.debug(
//                            "Transaction with hash: {} has expired and rippled is missing some to confirm if it" + " was validated", transactionHash
//                        )
//                        return@orElseGet Finality.builder().finalityStatus(FinalityStatus.NOT_FINAL).build()
//                    } else {
//                        val accountInfoResult: AccountInfoResult = this.accountInfo(
//                            AccountInfoRequestParams.of(account)
//                        )
//                        val accountSequence: UnsignedInteger = accountInfoResult.accountData().sequence()
//                        if (FluentCompareTo.`is`<UnsignedInteger>(transactionAccountSequence).lessThan(accountSequence)) {
//                            // a different transaction with this sequence has a final outcome.
//                            // this represents an unexpected case
//                            return@orElseGet Finality.builder().finalityStatus(FinalityStatus.EXPIRED_WITH_SPENT_ACCOUNT_SEQUENCE).build()
//                        } else {
//                            LOGGER.debug(
//                                "Transaction with hash: {} has expired, consider resubmitting with updated" + " lastledgersequence and fee", transactionHash
//                            )
//                            return@orElseGet Finality.builder().finalityStatus(FinalityStatus.EXPIRED).build()
//                        }
//                    }
//                }
//            } catch (e: JsonRpcClientErrorException) {
//                LOGGER.warn(e.getMessage(), e)
//                return@orElseGet Finality.builder().finalityStatus(FinalityStatus.NOT_FINAL).build()
//            }
//        })
//    }
    suspend fun serverInformation(): ServerInfoResult {
        val request = JsonRpcRequest(XrplMethods.SERVER_INFO)
        val apiClient = XrplApiClient.create(url)
        val response = apiClient.postRpcRequest(request)
        return JsonParser.parseResponse(response, ServerInfoResult::class.java)
    }

    suspend fun accountChannels(params: AccountChannelsRequestParams?): AccountChannelsResult {
        val request = JsonRpcRequest(XrplMethods.ACCOUNT_CHANNELS, listOf(params))
        val apiClient = XrplApiClient.create(url)
        val response = apiClient.postRpcRequest(request)
        return JsonParser.parseResponse(response, AccountChannelsResult::class.java)
    }

    suspend fun accountCurrencies(params: AccountCurrenciesRequestParams?): AccountCurrenciesResult {
        val request = JsonRpcRequest(XrplMethods.ACCOUNT_CURRENCIES, listOf(params))
        val apiClient = XrplApiClient.create(url)
        val response = apiClient.postRpcRequest(request)
        return JsonParser.parseResponse(response, AccountCurrenciesResult::class.java)
    }

    suspend fun accountInfo(params: AccountInfoRequestParams?): AccountInfoResult {
        val request = JsonRpcRequest(XrplMethods.ACCOUNT_INFO, listOf(params))
        val apiClient = XrplApiClient.create(url)
        val response = apiClient.postRpcRequest(request)
        return JsonParser.parseResponse(response, AccountInfoResult::class.java)
    }

    suspend fun accountNfts(account: Address): AccountNftsResult {
        val params = AccountNftsRequestParams.builder().account(account).build()
        val request = JsonRpcRequest(XrplMethods.ACCOUNT_NFTS, listOf(params))
        val apiClient = XrplApiClient.create(url)
        val response = apiClient.postRpcRequest(request)
        return JsonParser.parseResponse(response, AccountNftsResult::class.java)
    }

    suspend fun nftBuyOffers(params: NftBuyOffersRequestParams?): NftBuyOffersResult {
        val request = JsonRpcRequest(XrplMethods.NFT_BUY_OFFERS, listOf(params))
        val apiClient = XrplApiClient.create(url)
        val response = apiClient.postRpcRequest(request)
        return JsonParser.parseResponse(response, NftBuyOffersResult::class.java)
    }

    suspend fun nftSellOffers(params: NftSellOffersRequestParams?): NftSellOffersResult {
        val request = JsonRpcRequest(XrplMethods.NFT_SELL_OFFERS, listOf(params))
        val apiClient = XrplApiClient.create(url)
        val response = apiClient.postRpcRequest(request)
        return JsonParser.parseResponse(response, NftSellOffersResult::class.java)
    }

    suspend fun nftInfo(params: NftInfoRequestParams?): NftInfoResult {
        val request = JsonRpcRequest(XrplMethods.NFT_INFO, listOf(params))
        val apiClient = XrplApiClient.create(url)
        val response = apiClient.postRpcRequest(request)
        return JsonParser.parseResponse(response, NftInfoResult::class.java)
    }

    suspend fun accountObjects(params: AccountObjectsRequestParams?): AccountObjectsResult {
        val request = JsonRpcRequest(XrplMethods.ACCOUNT_OBJECTS, listOf(params))
        val apiClient = XrplApiClient.create(url)
        val response = apiClient.postRpcRequest(request)
        return JsonParser.parseResponse(response, AccountObjectsResult::class.java)
    }

    suspend fun accountOffers(params: AccountOffersRequestParams?): AccountOffersResult {
        val request = JsonRpcRequest(XrplMethods.ACCOUNT_OFFERS, listOf(params))
        val apiClient = XrplApiClient.create(url)
        val response = apiClient.postRpcRequest(request)
        return JsonParser.parseResponse(response, AccountOffersResult::class.java)
    }

    suspend fun depositAuthorized(params: DepositAuthorizedRequestParams): DepositAuthorizedResult {
        val request = JsonRpcRequest(XrplMethods.DEPOSIT_AUTHORIZED, listOf(params))
        val apiClient = XrplApiClient.create(url)
        val response = apiClient.postRpcRequest(request)
        return JsonParser.parseResponse(response, DepositAuthorizedResult::class.java)
    }

    suspend fun accountTransactions(address: Address?): AccountTransactionsResult {
        val params = AccountTransactionsRequestParams.unboundedBuilder().account(address).build()
        val request = JsonRpcRequest(XrplMethods.ACCOUNT_TX, listOf(params))
        val apiClient = XrplApiClient.create(url)
        val response = apiClient.postRpcRequest(request)
        return JsonParser.parseResponse(response, AccountTransactionsResult::class.java)
    }

    suspend fun <T : Transaction> transaction(params: TransactionRequestParams, transactionType: Class<T>): TransactionResult<T> {
        val request = JsonRpcRequest(XrplMethods.TX, listOf(params))
        val apiClient = XrplApiClient.create(url)
        val response = apiClient.postRpcRequest(request)
        val resultType = objectMapper.typeFactory.constructParametricType(TransactionResult::class.java, transactionType)
        return JsonParser.parseResponse(response, resultType)
    }

    suspend fun ledger(params: LedgerRequestParams?): LedgerResult {
        val request = JsonRpcRequest(XrplMethods.LEDGER, listOf(params))
        val apiClient = XrplApiClient.create(url)
        val response = apiClient.postRpcRequest(request)
        return JsonParser.parseResponse(response, LedgerResult::class.java)
    }

    suspend fun <T : LedgerObject> ledgerEntry(params: LedgerEntryRequestParams<T>): LedgerEntryResult<T> {
        val request = JsonRpcRequest(XrplMethods.LEDGER_ENTRY, listOf(params))
        val apiClient = XrplApiClient.create(url)
        val response = apiClient.postRpcRequest(request)
        val resultType = objectMapper.typeFactory.constructParametricType(LedgerEntryResult::class.java, params.ledgerObjectClass())
        return JsonParser.parseResponse(response, resultType)
    }

    suspend fun ripplePathFind(params: RipplePathFindRequestParams?): RipplePathFindResult {
        val request = JsonRpcRequest(XrplMethods.RIPPLE_PATH_FIND, listOf(params))
        val apiClient = XrplApiClient.create(url)
        val response = apiClient.postRpcRequest(request)
        return JsonParser.parseResponse(response, RipplePathFindResult::class.java)
    }

    suspend fun bookOffers(params: BookOffersRequestParams?): BookOffersResult {
        val request = JsonRpcRequest(XrplMethods.BOOK_OFFERS, listOf(params))
        val apiClient = XrplApiClient.create(url)
        val response = apiClient.postRpcRequest(request)
        return JsonParser.parseResponse(response, BookOffersResult::class.java)
    }

    suspend fun accountLines(params: AccountLinesRequestParams?): AccountLinesResult {
        val request = JsonRpcRequest(XrplMethods.ACCOUNT_LINES, listOf(params))
        val apiClient = XrplApiClient.create(url)
        val response = apiClient.postRpcRequest(request)
        return JsonParser.parseResponse(response, AccountLinesResult::class.java)
    }

    suspend fun channelVerify(params: ChannelVerifyRequestParams?): ChannelVerifyResult {
        val request = JsonRpcRequest(XrplMethods.CHANNEL_VERIFY, listOf(params))
        val apiClient = XrplApiClient.create(url)
        val response = apiClient.postRpcRequest(request)
        return JsonParser.parseResponse(response, ChannelVerifyResult::class.java)
    }

    suspend fun gatewayBalances(params: GatewayBalancesRequestParams?): GatewayBalancesResult {
        val request = JsonRpcRequest(XrplMethods.GATEWAY_BALANCES, listOf(params))
        val apiClient = XrplApiClient.create(url)
        val response = apiClient.postRpcRequest(request)
        return JsonParser.parseResponse(response, GatewayBalancesResult::class.java)
    }

    suspend fun ammInfo(params: AmmInfoRequestParams?): AmmInfoResult {
        val request = JsonRpcRequest(XrplMethods.AMM_INFO, listOf(params))
        val apiClient = XrplApiClient.create(url)
        val response = apiClient.postRpcRequest(request)
        return JsonParser.parseResponse(response, AmmInfoResult::class.java)
    }

    suspend fun getAggregatePrice(params: GetAggregatePriceRequestParams?): GetAggregatePriceResult {
        val request = JsonRpcRequest(XrplMethods.GET_AGGREGATE_PRICE, listOf(params))
        val apiClient = XrplApiClient.create(url)
        val response = apiClient.postRpcRequest(request)
        return JsonParser.parseResponse(response, GetAggregatePriceResult::class.java)
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(XrplClient::class.java)
    }
}
