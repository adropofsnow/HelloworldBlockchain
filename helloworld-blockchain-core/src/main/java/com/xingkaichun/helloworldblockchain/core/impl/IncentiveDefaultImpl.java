package com.xingkaichun.helloworldblockchain.core.impl;

import com.xingkaichun.helloworldblockchain.core.BlockchainDatabase;
import com.xingkaichun.helloworldblockchain.core.Incentive;
import com.xingkaichun.helloworldblockchain.core.model.Block;
import com.xingkaichun.helloworldblockchain.core.tools.BlockTool;
import com.xingkaichun.helloworldblockchain.setting.Setting;
import com.xingkaichun.helloworldblockchain.util.LogUtil;

/**
 *
 * @author 邢开春 409060350@qq.com
 */
public class IncentiveDefaultImpl extends Incentive {

    @Override
    public long incentiveValue(BlockchainDatabase blockchainDataBase, Block block) {
        //给予矿工的挖矿津贴
        long minerSubsidy = getMinerSubsidy(block);
        //给予矿工的交易手续费
        long minerFee = BlockTool.getBlockFee(block);
        //总的激励
        return minerSubsidy + minerFee;
    }

    @Override
    public boolean checkIncentive(BlockchainDatabase blockchainDataBase, Block block) {
        long writeIncentiveValue = BlockTool.getIncentiveValue(block);
        long targetIncentiveValue = incentiveValue(blockchainDataBase,block);
        if(writeIncentiveValue != targetIncentiveValue){
            LogUtil.debug("区块数据异常，挖矿奖励数据异常。");
            return false;
        }
        return true;
    }

    private static long getMinerSubsidy(Block block) {
        long initCoin = Setting.IncentiveSetting.BLOCK_INIT_INCENTIVE;
        long multiple = (block.getHeight() - 1L) / Setting.IncentiveSetting.INCENTIVE_HALVING_INTERVAL;
        while (multiple > 0){
            initCoin = initCoin / 2L;
            --multiple;
        }
        return initCoin;
    }
}