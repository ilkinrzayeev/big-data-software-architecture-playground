package com.aliagasiyev.bigdata.messaging;

import java.util.concurrent.atomic.AtomicInteger;

public class MessageQueue<T> {
    private static class Node<T> {
        T value;
        Node<T> next;
        Node(T value) { this.value = value; }
    }

    private Node<T> head;
    private Node<T> tail;
    private final AtomicInteger size = new AtomicInteger(0);
    private final int capacity;
    private final Object lock = new Object();

    public MessageQueue(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("Capacity must be positive");
        this.capacity = capacity;
    }

    public void enqueue(T value) {
        if (value == null) throw new NullPointerException("Null values not allowed");
        synchronized (lock) {
            if (size.get() == capacity) throw new IllegalStateException("Queue is full");
            Node<T> node = new Node<>(value);
            if (tail == null) {
                head = tail = node;
            } else {
                tail.next = node;
                tail = node;
            }
            size.incrementAndGet();
        }
    }

    public T dequeue() {
        synchronized (lock) {
            if (head == null) throw new IllegalStateException("Queue is empty");
            T value = head.value;
            head = head.next;
            if (head == null) tail = null;
            size.decrementAndGet();
            return value;
        }
    }

    public int size() {
        return size.get();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public boolean isFull() {
        return size() == capacity;
    }
}