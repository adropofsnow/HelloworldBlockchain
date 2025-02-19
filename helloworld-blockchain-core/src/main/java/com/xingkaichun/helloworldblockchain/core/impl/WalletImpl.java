package com.xingkaichun.helloworldblockchain.core.impl;

import com.xingkaichun.helloworldblockchain.core.BlockchainDatabase;
import com.xingkaichun.helloworldblockchain.core.CoreConfiguration;
import com.xingkaichun.helloworldblockchain.core.Wallet;
import com.xingkaichun.helloworldblockchain.core.model.transaction.TransactionOutput;
import com.xingkaichun.helloworldblockchain.core.model.wallet.BuildTransactionRequest;
import com.xingkaichun.helloworldblockchain.core.model.wallet.BuildTransactionResponse;
import com.xingkaichun.helloworldblockchain.core.model.wallet.Recipient;
import com.xingkaichun.helloworldblockchain.core.tools.EncodeDecodeTool;
import com.xingkaichun.helloworldblockchain.core.tools.WalletTool;
import com.xingkaichun.helloworldblockchain.crypto.AccountUtil;
import com.xingkaichun.helloworldblockchain.crypto.ByteUtil;
import com.xingkaichun.helloworldblockchain.crypto.model.Account;
import com.xingkaichun.helloworldblockchain.util.FileUtil;
import com.xingkaichun.helloworldblockchain.util.KvDbUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 默认实现
 *
 * @author 邢开春 409060350@qq.com
 */
public class WalletImpl extends Wallet {

    private CoreConfiguration coreConfiguration;
    private static final String WALLET_DATABASE_NAME = "WalletDatabase";

    public WalletImpl(CoreConfiguration coreConfiguration) {
        this.coreConfiguration = coreConfiguration;
    }


    @Override
    public List<Account> getAllAccounts() {
        List<Account> accountList = new ArrayList<>();
        //获取所有
        List<byte[]> bytesAccountList = KvDbUtil.gets(getWalletDatabasePath(),1,100000000);
        if(bytesAccountList != null){
            for(byte[] bytesAccount:bytesAccountList){
                Account account = EncodeDecodeTool.decodeToAccount(bytesAccount);
                accountList.add(account);
            }
        }
        return accountList;
    }

    @Override
    public Account createAccount() {
        return AccountUtil.randomAccount();
    }

    @Override
    public Account createAndSaveAccount() {
        Account account = createAccount();
        saveAccount(account);
        return account;
    }

    @Override
    public void saveAccount(Account account) {
        KvDbUtil.put(getWalletDatabasePath(),getKeyByAccount(account), EncodeDecodeTool.encodeAccount(account));
    }

    @Override
    public void deleteAccountByAddress(String address) {
        KvDbUtil.delete(getWalletDatabasePath(),getKeyByAddress(address));
    }

    private byte[] getKeyByAddress(String address){
        return ByteUtil.stringToUtf8Bytes(address);
    }
    private byte[] getKeyByAccount(Account account){
        return getKeyByAddress(account.getAddress());
    }

    @Override
    public BuildTransactionResponse buildTransaction(BlockchainDatabase blockchainDataBase, BuildTransactionRequest request) {
        List<Account> allAccountList = getAllAccounts();
        if(allAccountList == null || allAccountList.isEmpty()){
            BuildTransactionResponse response = new BuildTransactionResponse();
            response.setBuildTransactionSuccess(false);
            response.setMessage("钱包中的余额不足支付。");
            return response;
        }

        BuildTransactionResponse response = new BuildTransactionResponse();
        response.setMessage("请输入足够的金额");
        response.setBuildTransactionSuccess(false);

        //创建一个地址用于存放找零
        Account payerChangeAccount = createAccount();
        saveAccount(payerChangeAccount);

        List<String> privateKeyList = new ArrayList<>();
        for(Account account:allAccountList){
            privateKeyList.add(account.getPrivateKey());
            response = buildTransactionDto(blockchainDataBase,privateKeyList,request.getRecipientList(),payerChangeAccount.getAddress(),request.getFee());
            if(response.isBuildTransactionSuccess()){
                return response;
            }
        }
        return response;
    }

    private String getWalletDatabasePath() {
        return FileUtil.newPath(coreConfiguration.getCorePath(), WALLET_DATABASE_NAME);
    }

    private BuildTransactionResponse buildTransactionDto(BlockchainDatabase blockchainDataBase, List<String> payerPrivateKeyList, List<Recipient> recipientList, String payerChangeAddress, long fee) {
        Map<String, TransactionOutput> privateKeyUtxoMap = new HashMap<>();
        BuildTransactionResponse response = new BuildTransactionResponse();
        response.setMessage("请输入足够的金额");
        response.setBuildTransactionSuccess(false);

        for(String privateKey : payerPrivateKeyList){
            String address = AccountUtil.accountFromStringPrivateKey(privateKey).getAddress();
            TransactionOutput utxo = queryUnspentTransactionOutputByAddress(blockchainDataBase, address);
            if(utxo == null || utxo.getValue() <= 0){
                continue;
            }
            privateKeyUtxoMap.put(privateKey,utxo);
            response = WalletTool.buildTransactionDto(privateKeyUtxoMap,recipientList,payerChangeAddress,fee);
            if(response.isBuildTransactionSuccess()){
                break;
            }
        }
        return response;
    }

    private TransactionOutput queryUnspentTransactionOutputByAddress(BlockchainDatabase blockchainDataBase, String address) {
        return blockchainDataBase.queryUnspentTransactionOutputByAddress(address);
    }
}
