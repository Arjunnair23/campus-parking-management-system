/**
 * LinkedStack.java
 * -------------------------------------------------------
 * Generic Stack (LIFO) implemented using linked Nodes,
 * NOT an array. Used in this project to hold the history
 * of recently exited vehicles, and reused by
 * ExpressionHandler for parenthesis checking.
 *
 * Covers: "Implementing stack ... using linked list"
 * -------------------------------------------------------
 */
import java.util.ArrayList;
import java.util.List;

public class LinkedStack<T> {
    private Node<T> top;
    private int size;
 
    public LinkedStack() {
        top = null;
        size = 0;
    }
 
    public void push(T item) {
        Node<T> newNode = new Node<>(item);
        newNode.next = top;
        top = newNode;
        size++;
    }
 
    public T pop() {
        if (isEmpty()) {
            throw new RuntimeException("Stack is empty - cannot pop.");
        }
        T data = top.data;
        top = top.next;
        size--;
        return data;
    }
 
    public T peek() {
        if (isEmpty()) {
            throw new RuntimeException("Stack is empty - cannot peek.");
        }
        return top.data;
    }
 
    public boolean isEmpty() {
        return top == null;
    }
 
    public int getSize() {
        return size;
    }
 
    /** Display stack contents top-first (most recent first). */
    public void display() {
        if (isEmpty()) {
            System.out.println("Stack is empty.");
            return;
        }
        Node<T> temp = top;
        System.out.println("---- Stack (top -> bottom) ----");
        while (temp != null) {
            System.out.println(temp.data);
            temp = temp.next;
        }
        System.out.println("--------------------------------");
    }

    /** Returns stack contents top-first as a List (used by the REST API). */
    public List<T> toList() {
        List<T> list = new ArrayList<>();
        Node<T> temp = top;
        while (temp != null) {
            list.add(temp.data);
            temp = temp.next;
        }
        return list;
    }
}