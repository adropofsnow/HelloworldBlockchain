package com.xingkaichun.helloworldblockchain.core.tools;

import com.xingkaichun.helloworldblockchain.core.BlockchainDatabase;
import com.xingkaichun.helloworldblockchain.core.model.Block;
import com.xingkaichun.helloworldblockchain.core.model.script.InputScript;
import com.xingkaichun.helloworldblockchain.core.model.script.OutputScript;
import com.xingkaichun.helloworldblockchain.core.model.transaction.*;
import com.xingkaichun.helloworldblockchain.crypto.AccountUtil;
import com.xingkaichun.helloworldblockchain.netcore.dto.*;
import com.xingkaichun.helloworldblockchain.setting.Setting;

import java.util.ArrayList;
import java.util.List;

/**
 * dto转model工具
 *
 * @author 邢开春 409060350@qq.com
 */
public class Dto2ModelTool {

    public static Block blockDto2Block(BlockchainDatabase blockchainDataBase, BlockDto blockDto) {
        String previousBlockHash = blockDto.getPreviousHash();
        Block previousBlock = blockchainDataBase.queryBlockByBlockHash(previousBlockHash);

        Block block = new Block();
        block.setTimestamp(blockDto.getTimestamp());
        block.setPreviousBlockHash(previousBlockHash);
        block.setNonce(blockDto.getNonce());

        long blockHeight = previousBlock==null? Setting.GenesisBlockSetting.HEIGHT+1:previousBlock.getHeight()+1;
        block.setHeight(blockHeight);

        List<Transaction> transactionList = transactionDto2Transaction(blockchainDataBase,blockDto.getTransactions());
        block.setTransactions(transactionList);

        String merkleTreeRoot = BlockTool.calculateBlockMerkleTreeRoot(block);
        block.setMerkleTreeRoot(merkleTreeRoot);

        block.setHash(BlockTool.calculateBlockHash(block));

        /**
         * 预先校验区块工作量共识。伪造工作量共识是一件十分耗费资源的事情，因此预先校验工作量共识可以抵消绝大部分的攻击。
         * 也可以选择跳过此处预检，后续业务有完整的校验检测。
         * 此处预检，只是想预先抵消绝大部分的攻击。
         */
        if(!blockchainDataBase.getConsensus().checkConsensus(blockchainDataBase,block)){
            throw new RuntimeException("区块预检失败。");
        }
        return block;
    }

    private static List<Transaction> transactionDto2Transaction(BlockchainDatabase blockchainDataBase, List<TransactionDto> transactionDtoList) {
        List<Transaction> transactionList = new ArrayList<>();
        if(transactionDtoList != null){
            for(TransactionDto transactionDto:transactionDtoList){
                Transaction transaction = transactionDto2Transaction(blockchainDataBase,transactionDto);
                transactionList.add(transaction);
            }
        }
        return transactionList;
    }

    public static Transaction transactionDto2Transaction(BlockchainDatabase blockchainDataBase, TransactionDto transactionDto) {
        List<TransactionInput> inputs = new ArrayList<>();
        List<TransactionInputDto> transactionInputDtoList = transactionDto.getInputs();
        if(transactionInputDtoList != null){
            for (TransactionInputDto transactionInputDto:transactionInputDtoList){
                TransactionOutputId transactionOutputId = new TransactionOutputId();
                transactionOutputId.setTransactionHash(transactionInputDto.getTransactionHash());
                transactionOutputId.setTransactionOutputIndex(transactionInputDto.getTransactionOutputIndex());
                TransactionOutput unspentTransactionOutput = blockchainDataBase.queryUnspentTransactionOutputByTransactionOutputId(transactionOutputId);
                if(unspentTransactionOutput == null){
                    throw new RuntimeException("非法交易。交易输入并不是一笔未花费交易输出。");
                }
                TransactionInput transactionInput = new TransactionInput();
                transactionInput.setUnspentTransactionOutput(TransactionTool.transactionOutput2UnspentTransactionOutput(unspentTransactionOutput));
                transactionInput.setInputScript(inputScriptDto2InputScript(transactionInputDto.getInputScript()));
                inputs.add(transactionInput);
            }
        }

        List<TransactionOutput> outputs = new ArrayList<>();
        List<TransactionOutputDto> dtoOutputs = transactionDto.getOutputs();
        if(dtoOutputs != null){
            for(TransactionOutputDto transactionOutputDto:dtoOutputs){
                TransactionOutput transactionOutput = transactionOutputDto2TransactionOutput(transactionOutputDto);
                outputs.add(transactionOutput);
            }
        }

        Transaction transaction = new Transaction();
        TransactionType transactionType = obtainTransactionDto(transactionDto);
        transaction.setTransactionType(transactionType);
        transaction.setTransactionHash(TransactionTool.calculateTransactionHash(transactionDto));
        transaction.setInputs(inputs);
        transaction.setOutputs(outputs);
        return transaction;
    }

    public static TransactionOutput transactionOutputDto2TransactionOutput(TransactionOutputDto transactionOutputDto) {
        TransactionOutput transactionOutput = new TransactionOutput();
        String publicKeyHash = ScriptTool.getPublicKeyHashByPayToPublicKeyHashOutputScript(transactionOutputDto.getOutputScript());
        String address = AccountUtil.addressFromStringPublicKeyHash(publicKeyHash);
        transactionOutput.setAddress(address);
        transactionOutput.setValue(transactionOutputDto.getValue());
        transactionOutput.setOutputScript(outputScriptDto2OutputScript(transactionOutputDto.getOutputScript()));
        return transactionOutput;
    }

    private static TransactionType obtainTransactionDto(TransactionDto transactionDto) {
        if(transactionDto.getInputs() == null || transactionDto.getInputs().size()==0){
            return TransactionType.GENESIS;
        }
        return TransactionType.STANDARD;
    }

    private static OutputScript outputScriptDto2OutputScript(OutputScriptDto outputScriptDto) {
        if(outputScriptDto == null){
            return null;
        }
        OutputScript outputScript = new OutputScript();
        outputScript.addAll(outputScriptDto);
        return outputScript;
    }

    private static InputScript inputScriptDto2InputScript(InputScriptDto inputScriptDto) {
        if(inputScriptDto == null){
            return null;
        }
        InputScript inputScript = new InputScript();
        inputScript.addAll(inputScriptDto);
        return inputScript;
    }
}
