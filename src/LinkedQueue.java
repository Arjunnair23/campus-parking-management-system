/**
 * LinkedQueue.java
 * -------------------------------------------------------
 * Generic Queue (FIFO) implemented using linked Nodes,
 * NOT an array. Used in this project to hold vehicles
 * waiting for a free parking slot.
 *
 * Covers: "Implementing ... queue using linked list"
 * -------------------------------------------------------
 */
import java.util.ArrayList;
import java.util.List;

public class LinkedQueue<T> {
    private Node<T> front;
    private Node<T> rear;
    private int size;
 
    public LinkedQueue() {
        front = null;
        rear = null;
        size = 0;
    }
 
    public void enqueue(T item) {
        Node<T> newNode = new Node<>(item);
        if (rear == null) {
            front = newNode;
            rear = newNode;
        } else {
            rear.next = newNode;
            rear = newNode;
        }
        size++;
    }
 
    public T dequeue() {
        if (isEmpty()) {
            throw new RuntimeException("Queue is empty - cannot dequeue.");
        }
        T data = front.data;
        front = front.next;
        if (front == null) {
            rear = null; // queue became empty
        }
        size--;
        return data;
    }
 
    public T peekFront() {
        if (isEmpty()) {
            throw new RuntimeException("Queue is empty - cannot peek.");
        }
        return front.data;
    }
 
    public boolean isEmpty() {
        return front == null;
    }
 
    public int getSize() {
        return size;
    }
 
    /** Display queue contents front-first. */
    public void display() {
        if (isEmpty()) {
            System.out.println("Waiting queue is empty.");
            return;
        }
        Node<T> temp = front;
        System.out.println("---- Waiting Queue (front -> rear) ----");
        while (temp != null) {
            System.out.println(temp.data);
            temp = temp.next;
        }
        System.out.println("-----------------------------------------");
    }

    /** Returns queue contents front-first as a List (used by the REST API). */
    public List<T> toList() {
        List<T> list = new ArrayList<>();
        Node<T> temp = front;
        while (temp != null) {
            list.add(temp.data);
            temp = temp.next;
        }
        return list;
    }
}