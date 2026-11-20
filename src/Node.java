/**
 * Node.java
 * -------------------------------------------------------
 * Generic singly-linked node. Reused as the building block
 * for ParkedVehicleList, LinkedStack, and LinkedQueue - so
 * the stack and queue are genuinely "implemented using a
 * linked list" rather than wrapping an array or ArrayList.
 * -------------------------------------------------------
 */
public class Node<T> {
    T data;
    Node<T> next;
 
    public Node(T data) {
        this.data = data;
        this.next = null;
    }
}
