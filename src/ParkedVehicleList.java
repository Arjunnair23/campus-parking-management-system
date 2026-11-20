/**
 * ParkedVehicleList.java
 * -------------------------------------------------------
 * Singly linked list storing the records of vehicles that
 * are currently parked on campus.
 *
 * Covers: "Implementing linked list for data storage" and
 * "Performing basic operations (insert, delete, traverse)"
 * -------------------------------------------------------
 */
import java.util.ArrayList;
import java.util.List;

public class ParkedVehicleList {
    private Node<Vehicle> head;
    private int size;
 
    public ParkedVehicleList() {
        head = null;
        size = 0;
    }
 
    /** Insert a new vehicle record at the end of the list. */
    public void insert(Vehicle v) {
        Node<Vehicle> newNode = new Node<>(v);
        if (head == null) {
            head = newNode;
        } else {
            Node<Vehicle> temp = head;
            while (temp.next != null) {
                temp = temp.next;
            }
            temp.next = newNode;
        }
        size++;
    }
 
    /** Delete a vehicle record by vehicle number. Returns the removed Vehicle, or null if not found. */
    public Vehicle delete(String vehicleNumber) {
        if (head == null) return null;
 
        if (head.data.getVehicleNumber().equalsIgnoreCase(vehicleNumber)) {
            Vehicle removed = head.data;
            head = head.next;
            size--;
            return removed;
        }
 
        Node<Vehicle> prev = head;
        Node<Vehicle> curr = head.next;
        while (curr != null) {
            if (curr.data.getVehicleNumber().equalsIgnoreCase(vehicleNumber)) {
                prev.next = curr.next;
                size--;
                return curr.data;
            }
            prev = curr;
            curr = curr.next;
        }
        return null; // not found
    }
 
    /** Linear search by exact vehicle number. */
    public Vehicle search(String vehicleNumber) {
        Node<Vehicle> temp = head;
        while (temp != null) {
            if (temp.data.getVehicleNumber().equalsIgnoreCase(vehicleNumber)) {
                return temp.data;
            }
            temp = temp.next;
        }
        return null;
    }
 
    /** Checks whether a vehicle number already exists (duplicate-entry guard). */
    public boolean exists(String vehicleNumber) {
        return search(vehicleNumber) != null;
    }
 
    /** Traverse and print every currently parked vehicle. */
    public void traverse() {
        if (head == null) {
            System.out.println("No vehicles currently parked.");
            return;
        }
        Node<Vehicle> temp = head;
        System.out.println("---- Currently Parked Vehicles ----");
        while (temp != null) {
            System.out.println(temp.data);
            temp = temp.next;
        }
        System.out.println("------------------------------------");
    }
 
    public int getSize() {
        return size;
    }

    public boolean isEmpty() {
        return head == null;
    }

    /** Returns parked vehicles head-first as a List (used by the REST API). */
    public List<Vehicle> toList() {
        List<Vehicle> list = new ArrayList<>();
        Node<Vehicle> temp = head;
        while (temp != null) {
            list.add(temp.data);
            temp = temp.next;
        }
        return list;
    }
}
 