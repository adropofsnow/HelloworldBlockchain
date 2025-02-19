package com.xingkaichun.helloworldblockchain.core.tools;

import com.xingkaichun.helloworldblockchain.core.BlockchainCore;
import com.xingkaichun.helloworldblockchain.core.model.script.InputScript;
import com.xingkaichun.helloworldblockchain.core.model.script.OutputScript;
import com.xingkaichun.helloworldblockchain.core.model.transaction.TransactionOutput;
import com.xingkaichun.helloworldblockchain.core.model.wallet.BuildTransactionResponse;
import com.xingkaichun.helloworldblockchain.core.model.wallet.Recipient;
import com.xingkaichun.helloworldblockchain.crypto.AccountUtil;
import com.xingkaichun.helloworldblockchain.netcore.dto.TransactionDto;
import com.xingkaichun.helloworldblockchain.netcore.dto.TransactionInputDto;
import com.xingkaichun.helloworldblockchain.netcore.dto.TransactionOutputDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 钱包工具类
 *
 * @author 邢开春 409060350@qq.com
 */
public class WalletTool {

    public static long obtainBalance(BlockchainCore blockchainCore, String address) {
        TransactionOutput utxo = blockchainCore.getBlockchainDataBase().queryUnspentTransactionOutputByAddress(address);
        if(utxo != null){
            return utxo.getValue();
        }
        return 0L;
    }

    public static long obtainSpent(BlockchainCore blockchainCore, String address) {
        TransactionOutput utxo = blockchainCore.getBlockchainDataBase().querySpentTransactionOutputByAddress(address);
        if(utxo != null){
            return utxo.getValue();
        }
        return 0L;
    }

    public static long obtainReceipt(BlockchainCore blockchainCore, String address) {
        //交易输出总金额
        TransactionOutput txo = blockchainCore.getBlockchainDataBase().queryTransactionOutputByAddress(address);
        return txo==null?0:txo.getValue();
    }

    public static BuildTransactionResponse buildTransactionDto(Map<String,TransactionOutput> privateKeyUtxoMap, List<Recipient> recipientList, String payerChangeAddress, long fee) {
        //最少付款总金额
        long minInputValues = 0;
        if(recipientList != null){
            //支付钱款
            for(Recipient recipient : recipientList){
                minInputValues += recipient.getValue();
            }
        }
        //交易手续费
        minInputValues += fee;

        //创建交易输出
        List<TransactionOutputDto> transactionOutputDtoList = new ArrayList<>();
        List<TransactionOutput> innerTransactionOutputList = new ArrayList<>();

        if(recipientList != null){
            for(Recipient recipient : recipientList){
                TransactionOutputDto transactionOutputDto = new TransactionOutputDto();
                transactionOutputDto.setValue(recipient.getValue());
                OutputScript outputScript = ScriptTool.createPayToPublicKeyHashOutputScript(recipient.getAddress());
                transactionOutputDto.setOutputScript(Model2DtoTool.outputScript2OutputScriptDto(outputScript));
                transactionOutputDtoList.add(transactionOutputDto);

                TransactionOutput innerTransactionOutput = new TransactionOutput();
                innerTransactionOutput.setAddress(recipient.getAddress());
                innerTransactionOutput.setValue(recipient.getValue());
                innerTransactionOutput.setOutputScript(outputScript);
                innerTransactionOutputList.add(innerTransactionOutput);
            }
        }

        //获取足够的金额
        //交易输入列表
        List<TransactionOutput> inputs = new ArrayList<>();
        List<String> inputPrivateKeyList = new ArrayList<>();
        //交易输入总金额
        long inputValues = 0;
        boolean haveEnoughMoneyToPay = false;
        for(Map.Entry<String,TransactionOutput> entry: privateKeyUtxoMap.entrySet()){
            String privateKey = entry.getKey();
            TransactionOutput utxo = entry.getValue();
            if(utxo == null){
                break;
            }
            inputValues += utxo.getValue();
            //交易输入
            inputs.add(utxo);
            inputPrivateKeyList.add(privateKey);
            if(inputValues >= minInputValues){
                haveEnoughMoneyToPay = true;
                break;
            }
        }

        if(!haveEnoughMoneyToPay){
            BuildTransactionResponse buildTransactionResponse = new BuildTransactionResponse();
            buildTransactionResponse.setBuildTransactionSuccess(false);
            buildTransactionResponse.setMessage("账户没有足够的金额去支付");
            return buildTransactionResponse;
        }

        //构建交易输入
        List<TransactionInputDto> transactionInputDtoList = new ArrayList<>();
        for(TransactionOutput input:inputs){
            TransactionInputDto transactionInputDto = new TransactionInputDto();
            transactionInputDto.setTransactionHash(input.getTransactionHash());
            transactionInputDto.setTransactionOutputIndex(input.getTransactionOutputIndex());
            transactionInputDtoList.add(transactionInputDto);
        }

        //构建交易
        TransactionDto transactionDto = new TransactionDto();
        transactionDto.setInputs(transactionInputDtoList);
        transactionDto.setOutputs(transactionOutputDtoList);

        //总收款金额
        long outputValues = 0;
        if(recipientList != null){
            for(Recipient recipient : recipientList){
                outputValues += recipient.getValue();
            }
        }

        //找零
        long change = inputValues - outputValues - fee;
        TransactionOutput payerChangeTransactionOutput = null;
        if(change > 0){
            TransactionOutputDto transactionOutputDto = new TransactionOutputDto();
            transactionOutputDto.setValue(change);
            OutputScript outputScript = ScriptTool.createPayToPublicKeyHashOutputScript(payerChangeAddress);
            transactionOutputDto.setOutputScript(Model2DtoTool.outputScript2OutputScriptDto(outputScript));
            transactionOutputDtoList.add(transactionOutputDto);

            payerChangeTransactionOutput = new TransactionOutput();
            payerChangeTransactionOutput.setAddress(payerChangeAddress);
            payerChangeTransactionOutput.setValue(change);
            payerChangeTransactionOutput.setOutputScript(outputScript);
        }

        //签名
        for(int i=0;i<transactionInputDtoList.size();i++){
            String privateKey = inputPrivateKeyList.get(i);
            String publicKey = AccountUtil.accountFromStringPrivateKey(privateKey).getPublicKey();
            TransactionInputDto transactionInputDto = transactionInputDtoList.get(i);
            String signature = TransactionTool.signature(privateKey,transactionDto);
            InputScript inputScript = ScriptTool.createPayToPublicKeyHashInputScript(signature, publicKey);
            transactionInputDto.setInputScript(Model2DtoTool.inputScript2InputScriptDto(inputScript));
        }


        BuildTransactionResponse buildTransactionResponse = new BuildTransactionResponse();
        buildTransactionResponse.setBuildTransactionSuccess(true);
        buildTransactionResponse.setMessage("构建交易成功");
        buildTransactionResponse.setTransactionHash(TransactionTool.calculateTransactionHash(transactionDto));
        buildTransactionResponse.setFee(fee);
        buildTransactionResponse.setPayerChangeTransactionOutput(payerChangeTransactionOutput);
        buildTransactionResponse.setTransactionInputs(inputs);
        buildTransactionResponse.setTransactionOutputs(innerTransactionOutputList);
        buildTransactionResponse.setTransaction(transactionDto);
        return buildTransactionResponse;
    }
}
