package com.test;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.transaction.spec.Policy;
import com.bloxbean.cardano.client.util.PolicyUtil;
import com.bloxbean.cardano.yaci.test.Funding;
import com.bloxbean.cardano.yaci.test.YaciCardanoContainer;
import org.junit.jupiter.api.Test;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.adaToLovelace;
import static com.bloxbean.cardano.yaci.test.api.Assertions.assertMe;

class MintServiceTest {

    @Test
    void mint() throws Exception {
        Account minterAccount = new Account(Networks.testnet());
        String minterAddress = minterAccount.baseAddress();

        YaciCardanoContainer yaciCardanoContainer = new YaciCardanoContainer()
                .withInitialFunding(new Funding(minterAccount.baseAddress(), 20000))
                .withLogConsumer(outputFrame -> System.out.println(outputFrame.getUtf8String()));

        yaciCardanoContainer.start();

        MintService mintService = new MintService(yaciCardanoContainer.getUtxoSupplier(),
                yaciCardanoContainer.getProtocolParamsSupplier(),
                yaciCardanoContainer.getTransactionProcessor());

        Policy policy = PolicyUtil.createMultiSigScriptAllPolicy("policy1", 1);

        Result<String> result = mintService.mint(minterAccount, policy, "TestToken", 1000L);
        yaciCardanoContainer.getTestHelper().waitForTransactionHash(result);

        //asserts
        assertMe(yaciCardanoContainer)
                .utxos(minterAddress).hasLovelaceBalance(bal -> bal.compareTo(adaToLovelace(20000)) < 0);
        assertMe(yaciCardanoContainer).hasAssetBalance(minterAddress, policy.getPolicyId(), "TestToken", 1000);

    }
}
