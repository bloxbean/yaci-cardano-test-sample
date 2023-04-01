package com.test;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.api.ProtocolParamsSupplier;
import com.bloxbean.cardano.client.api.TransactionProcessor;
import com.bloxbean.cardano.client.api.UtxoSupplier;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.function.Output;
import com.bloxbean.cardano.client.function.TxBuilder;
import com.bloxbean.cardano.client.function.TxBuilderContext;
import com.bloxbean.cardano.client.function.helper.MintCreators;
import com.bloxbean.cardano.client.transaction.spec.Asset;
import com.bloxbean.cardano.client.transaction.spec.MultiAsset;
import com.bloxbean.cardano.client.transaction.spec.Policy;
import com.bloxbean.cardano.client.transaction.spec.Transaction;

import java.math.BigInteger;
import java.util.Arrays;

import static com.bloxbean.cardano.client.function.helper.BalanceTxBuilders.balanceTx;
import static com.bloxbean.cardano.client.function.helper.InputBuilders.createFromSender;
import static com.bloxbean.cardano.client.function.helper.SignerProviders.signerFrom;

public class MintService {
    private UtxoSupplier utxoSupplier;
    private ProtocolParamsSupplier protocolParamsSupplier;
    private TransactionProcessor transactionProcessor;

    public MintService(UtxoSupplier utxoSupplier, ProtocolParamsSupplier protocolParamsSupplier,
                       TransactionProcessor transactionProcessor) {
        this.utxoSupplier = utxoSupplier;
        this.protocolParamsSupplier = protocolParamsSupplier;
        this.transactionProcessor = transactionProcessor;
    }

    public Result<String> mint(Account minter, Policy policy, String assetName, long amount) throws Exception {
        String minterAddress = minter.baseAddress();
        MultiAsset multiAsset = MultiAsset
                .builder()
                .policyId(policy.getPolicyId())
                .assets(Arrays.asList(new Asset(assetName, BigInteger.valueOf(amount))))
                .build();

        Output output = Output.builder()
                .address(minterAddress)
                .policyId(policy.getPolicyId())
                .assetName(assetName)
                .qty(BigInteger.valueOf(amount))
                .build();

        TxBuilder txBuilder = output.mintOutputBuilder()
                .buildInputs(createFromSender(minterAddress, minterAddress))
                .andThen(MintCreators.mintCreator(policy.getPolicyScript(), multiAsset))
                .andThen(balanceTx(minterAddress, 2));

        Transaction transaction = TxBuilderContext.init(utxoSupplier, protocolParamsSupplier)
                .buildAndSign(txBuilder, signerFrom(minter).andThen(signerFrom(policy)));

        return transactionProcessor.submitTransaction(transaction.serialize());
    }
}
