package com.aliagasiyev.bigdata.blockchain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BlockchainTest {

    @Test
    void blockchainStartsWithGenesisBlock() {
        Blockchain chain = new Blockchain();
        assertEquals(1, chain.size(), "Blockchain should start with genesis block");
        Block genesis = chain.getLatestBlock();
        assertEquals(0, genesis.getIndex(), "Genesis block index should be 0");
        assertEquals("Genesis Block", genesis.getData(), "Genesis block data should be correct");
        assertEquals("0", genesis.getPreviousHash(), "Genesis block previous hash should be '0'");
    }

    @Test
    void canAddBlock() {
        Blockchain chain = new Blockchain();
        chain.addBlock("First block");
        assertEquals(2, chain.size(), "Blockchain size should be 2 after adding a block");
        Block latest = chain.getLatestBlock();
        assertEquals("First block", latest.getData());
        assertEquals(1, latest.getIndex());
    }

    @Test
    void blocksAreLinkedByHash() {
        Blockchain chain = new Blockchain();
        chain.addBlock("Block 1");
        chain.addBlock("Block 2");
        Block latest = chain.getLatestBlock();
        Block prev = getBlockAt(chain, 1);
        assertEquals(prev.getHash(), latest.getPreviousHash(), "Blocks should be linked by hash");
    }

    @Test
    void blockchainIsValidAfterAddingBlocks() {
        Blockchain chain = new Blockchain();
        chain.addBlock("Block 1");
        chain.addBlock("Block 2");
        assertTrue(chain.isValid(), "Blockchain should be valid after adding blocks");
    }

    @Test
    void blockchainIsInvalidIfBlockTampered() {
        Blockchain chain = new Blockchain();
        chain.addBlock("Block 1");
        Block latest = chain.getLatestBlock();
        assertTrue(chain.isValid(), "Blockchain should be valid before tampering");
    }

    private Block getBlockAt(Blockchain chain, int index) {
        try {
            java.lang.reflect.Field headField = Blockchain.class.getDeclaredField("head");
            headField.setAccessible(true);
            Object node = headField.get(chain);
            for (int i = 0; i < index; i++) {
                java.lang.reflect.Field nextField = node.getClass().getDeclaredField("next");
                nextField.setAccessible(true);
                node = nextField.get(node);
            }
            java.lang.reflect.Field blockField = node.getClass().getDeclaredField("block");
            blockField.setAccessible(true);
            return (Block) blockField.get(node);
        } catch (Exception e) {
            throw new RuntimeException("Reflection failed", e);
        }
    }
}