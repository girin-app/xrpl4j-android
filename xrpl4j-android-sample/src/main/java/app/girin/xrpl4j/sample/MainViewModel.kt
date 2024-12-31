package app.girin.xrpl4j.sample

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import app.girin.xrpl4j.client.XrplClient
import com.google.common.primitives.UnsignedInteger
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.xrpl.xrpl4j.crypto.keys.KeyPair
import org.xrpl.xrpl4j.model.client.accounts.AccountInfoRequestParams
import org.xrpl.xrpl4j.model.client.accounts.AccountObjectsRequestParams
import org.xrpl.xrpl4j.model.transactions.Address
import org.xrpl.xrpl4j.model.transactions.MemoWrapper
import org.xrpl.xrpl4j.model.transactions.Payment
import org.xrpl.xrpl4j.model.transactions.XrpCurrencyAmount
import java.math.BigDecimal
import java.util.Optional

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val xrplUrl = "https://s1.ripple.com:51234/"

    private val _keyPair = MutableLiveData<KeyPair>()
    val keyPair: LiveData<KeyPair> get() = _keyPair

    private val _address = MutableLiveData<String>()
    val address: LiveData<String> get() = _address

    private val _serverInfo = MutableLiveData<String>()
    val serverInfo: LiveData<String> get() = _serverInfo

    private val _accountInfo = MutableLiveData<String>()
    val accountInfo: LiveData<String> get() = _accountInfo

    private val _accountObjects = MutableLiveData<String>()
    val accountObjects: LiveData<String> get() = _accountObjects

    private val _accountNfts = MutableLiveData<String>()
    val accountNfts: LiveData<String> get() = _accountNfts

    private val _accountTransactions = MutableLiveData<String>()
    val accountTransactions: LiveData<String> get() = _accountTransactions

    private val client = XrplClient(xrplUrl.toHttpUrl())

    fun loadMnemonic(mnemonic: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val generatedKeyPair = client.keyPair(mnemonic)
            val generatedAddress = generatedKeyPair.publicKey().deriveAddress().toString()

            _keyPair.postValue(generatedKeyPair)
            _address.postValue(generatedAddress)
        }
    }

    fun fetchServerInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = client.serverInformation()
            _serverInfo.postValue(Gson().toJson(result))
        }
    }

    fun fetchAccountInfo(address: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = client.accountInfo(AccountInfoRequestParams.of(Address.of(address)))
            _accountInfo.postValue(Gson().toJson(result))
        }
    }

    fun fetchAccountObjects(address: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = client.accountObjects(AccountObjectsRequestParams.of(Address.of(address)))
            _accountObjects.postValue(Gson().toJson(result))
        }
    }

    fun fetchAccountNfts(address: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = client.accountNfts(Address.of(address))
            _accountNfts.postValue(Gson().toJson(result))
        }
    }

    fun fetchAccountTransactions(address: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = client.accountTransactions(Address.of(address))
            _accountTransactions.postValue(Gson().toJson(result))
        }
    }

    fun send(from: String, to: String, amount: String, destinationTag: Optional<UnsignedInteger> = Optional.empty(), memos: List<MemoWrapper> = listOf()) {
        viewModelScope.launch(Dispatchers.IO) {
            keyPair.value?.let { keyPair ->
                val unsignedTx = Payment.builder()
                    .account(Address.of(from))
                    .fee(XrpCurrencyAmount.ofXrp(BigDecimal("0.0002")))
                    .sequence(client.accountInfo(AccountInfoRequestParams.of(Address.of(from))).accountData().sequence())
                    .signingPublicKey(keyPair.publicKey())
                    .destination(Address.of(to))
                    .amount(XrpCurrencyAmount.ofXrp(BigDecimal(amount)))
                    .destinationTag(destinationTag)
                    .memos(memos)
                    .build()
                client.submitTx(unsignedTx, keyPair)
            }
        }
    }
}
