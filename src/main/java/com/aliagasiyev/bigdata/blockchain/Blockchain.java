package com.aliagasiyev.bigdata.blockchain;

public class Blockchain {
    private final Node head;
    private int size;

    private static class Node {
        Block block;
        Node next;
        Node(Block block) { this.block = block; }
    }

    public Blockchain() {
        Block genesis = new Block(0, System.currentTimeMillis(), "Genesis Block", "0");
        head = new Node(genesis);
        size = 1;
    }

    public void addBlock(String data) {
        Block latest = getLatestBlock();
        Block newBlock = new Block(latest.getIndex() + 1, System.currentTimeMillis(), data, latest.getHash());
        Node node = new Node(newBlock);
        Node current = head;
        while (current.next != null) current = current.next;
        current.next = node;
        size++;
    }

    public Block getLatestBlock() {
        Node current = head;
        while (current.next != null) current = current.next;
        return current.block;
    }

    public int size() { return size; }

    public boolean isValid() {
        Node current = head;
        while (current.next != null) {
            Block prev = current.block;
            Block next = current.next.block;
            if (!next.getPreviousHash().equals(prev.getHash())) return false;
            if (!next.getHash().equals(next.getHash())) return false; // redundant, but for demo
            current = current.next;
        }
        return true;
    }
}