package app.girin.xrpl4j.sample

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import app.girin.xrpl4j.sample.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupObservers()
        setupView()
    }

    private fun setupObservers() {
        viewModel.keyPair.observe(this) { keyPair ->
            binding.address.text = keyPair.publicKey().deriveAddress().toString()
        }

        viewModel.address.observe(this) { address ->
            binding.address.text = address
        }

        viewModel.serverInfo.observe(this) { serverInfo ->
            Toast.makeText(this@MainActivity, serverInfo, Toast.LENGTH_LONG).show()
        }

        viewModel.accountInfo.observe(this) { accountInfo ->
            Toast.makeText(this@MainActivity, accountInfo, Toast.LENGTH_LONG).show()
        }

        viewModel.accountObjects.observe(this) { accountObjects ->
            Toast.makeText(this@MainActivity, accountObjects, Toast.LENGTH_LONG).show()
        }

        viewModel.accountNfts.observe(this) { accountNfts ->
            Toast.makeText(this@MainActivity, accountNfts, Toast.LENGTH_LONG).show()
        }

        viewModel.accountTransactions.observe(this) { accountTransactions ->
            Toast.makeText(this@MainActivity, accountTransactions, Toast.LENGTH_LONG).show()
        }
    }

    private fun setupView() {
        binding.loadMnemonic.setOnClickListener {
            val mnemonic = ""
            viewModel.loadMnemonic(mnemonic)
        }

        binding.serverIfno.setOnClickListener {
            viewModel.fetchServerInfo()
        }

        binding.accountInfo.setOnClickListener {
            val address = binding.address.text.toString()
            viewModel.fetchAccountInfo(address)
        }

        binding.accountObjects.setOnClickListener {
            val address = binding.address.text.toString()
            viewModel.fetchAccountObjects(address)
        }

        binding.accountNfts.setOnClickListener {
            val address = binding.address.text.toString()
            viewModel.fetchAccountNfts(address)
        }

        binding.accountTransactions.setOnClickListener {
            val address = binding.address.text.toString()
            viewModel.fetchAccountTransactions(address)
        }

        binding.send.setOnClickListener {
            val address = binding.address.text.toString()
            viewModel.send(from = address, to = address, amount = "0.1")
        }
    }
}
