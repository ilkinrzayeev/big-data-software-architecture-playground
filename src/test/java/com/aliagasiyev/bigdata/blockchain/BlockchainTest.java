package com.aliagasiyev.bigdata.blockchain;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

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

    @Test
    void addingManyBlocksWorks() {
        Blockchain chain = new Blockchain();
        int n = 1000;
        for (int i = 0; i < n; i++) {
            chain.addBlock("Block " + i);
        }
        assertEquals(n + 1, chain.size());
        assertEquals("Block " + (n - 1), chain.getLatestBlock().getData());
    }

    @Test
    void allBlockHashesAreUnique() {
        Blockchain chain = new Blockchain();
        int n = 100;
        Set<String> hashes = new HashSet<>();
        for (int i = 0; i < n; i++) {
            chain.addBlock("Block " + i);
        }
        for (int i = 0; i < chain.size(); i++) {
            String hash = chain.getBlockAt(i).getHash();
            assertFalse(hashes.contains(hash));
            hashes.add(hash);
        }
    }

    @Test
    void blockIndexIsSequential() {
        Blockchain chain = new Blockchain();
        int n = 50;
        for (int i = 0; i < n; i++) {
            chain.addBlock("Block " + i);
        }
        for (int i = 0; i < chain.size(); i++) {
            assertEquals(i, chain.getBlockAt(i).getIndex());
        }
    }

    @Test
    void blockTimestampsAreNonDecreasing() {
        Blockchain chain = new Blockchain();
        int n = 20;
        for (int i = 0; i < n; i++) {
            chain.addBlock("Block " + i);
        }
        long prev = 0;
        for (int i = 0; i < chain.size(); i++) {
            long ts = chain.getBlockAt(i).getTimestamp();
            assertTrue(ts >= prev);
            prev = ts;
        }
    }

    @Test
    void getBlockAtThrowsForInvalidIndex() {
        Blockchain chain = new Blockchain();
        assertThrows(IndexOutOfBoundsException.class, () -> chain.getBlockAt(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> chain.getBlockAt(100));
    }

    @Test
    void chainRemainsValidAfterManyAdds() {
        Blockchain chain = new Blockchain();
        for (int i = 0; i < 500; i++) {
            chain.addBlock("Block " + i);
            assertTrue(chain.isValid());
        }
    }

    @Test
    void blockEqualsAndHashCodeWork() {
        Block b1 = new Block(1, 12345L, "data", "prevHash");
        Block b2 = new Block(1, 12345L, "data", "prevHash");
        assertEquals(b1, b2);
        assertEquals(b1.hashCode(), b2.hashCode());
    }

    // Concurrency test (not real blockchain, but for demo)
    @Test
    void concurrentAddBlock() throws InterruptedException {
        Blockchain chain = new Blockchain();
        int threads = 10;
        int blocksPerThread = 20;
        Thread[] arr = new Thread[threads];
        for (int t = 0; t < threads; t++) {
            arr[t] = new Thread(() -> {
                for (int i = 0; i < blocksPerThread; i++) {
                    chain.addBlock(Thread.currentThread().getName() + "-" + i);
                }
            });
        }
        for (Thread t : arr) t.start();
        for (Thread t : arr) t.join();
        assertEquals(1 + threads * blocksPerThread, chain.size());
    }

    @Test
    void addBlockWithEmptyData() {
        Blockchain chain = new Blockchain();
        chain.addBlock("");
        Block latest = chain.getLatestBlock();
        assertEquals("", latest.getData());
        assertTrue(chain.isValid());
    }

    @Test
    void addBlockWithLargeData() {
        Blockchain chain = new Blockchain();
        String largeData = "x".repeat(10_000);
        chain.addBlock(largeData);
        Block latest = chain.getLatestBlock();
        assertEquals(largeData, latest.getData());
        assertTrue(chain.isValid());
    }

    @Test
    void blocksWithSameDataHaveDifferentHashes() {
        Blockchain chain = new Blockchain();
        chain.addBlock("same");
        chain.addBlock("same");
        Block b1 = chain.getBlockAt(1);
        Block b2 = chain.getBlockAt(2);
        assertNotEquals(b1.getHash(), b2.getHash());
        assertTrue(chain.isValid());
    }

    @Test
    void addBlockWithSpecialCharacters() {
        Blockchain chain = new Blockchain();
        String special = "!@#$%^&*()_+{}|:\"<>?~`";
        chain.addBlock(special);
        Block latest = chain.getLatestBlock();
        assertEquals(special, latest.getData());
        assertTrue(chain.isValid());
    }

    @Test
    void tamperingWithPreviousHashBreaksValidity() throws Exception {
        Blockchain chain = new Blockchain();
        chain.addBlock("A");
        chain.addBlock("B");
        Block b1 = chain.getBlockAt(1);
        java.lang.reflect.Field prevHashField = Block.class.getDeclaredField("previousHash");
        prevHashField.setAccessible(true);
        prevHashField.set(b1, "tampered");
        assertFalse(chain.isValid());
    }
}