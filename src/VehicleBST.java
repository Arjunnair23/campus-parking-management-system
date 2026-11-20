import java.util.ArrayList;
import java.util.List;

/**
 * VehicleBST.java
 * -------------------------------------------------------
 * Binary Search Tree of parked vehicles, keyed by slot
 * number. This is kept IN ADDITION to ParkedVehicleList
 * (the linked list from Phase 1) rather than replacing it:
 * the linked list stays the primary record store, and this
 * BST acts as a faster index for slot-number lookups -
 * O(log n) average case vs the linked list's O(n) search.
 *
 * Covers:
 *   - "Implementing binary tree for structured data storage"
 *     (a BST IS a binary tree - this one just also keeps the
 *     binary-search-tree ordering property)
 *   - "Performing tree traversals (inorder, preorder, postorder)"
 *   - "Implementing binary search tree for efficient data access"
 * -------------------------------------------------------
 */
public class VehicleBST {
    private TreeNode<Vehicle> root;

    /** Insert a vehicle, keyed by its slot number. */
    public void insert(Vehicle v) {
        root = insertRec(root, v);
    }

    private TreeNode<Vehicle> insertRec(TreeNode<Vehicle> node, Vehicle v) {
        if (node == null) return new TreeNode<>(v);

        if (v.getSlotNumber() < node.data.getSlotNumber()) {
            node.left = insertRec(node.left, v);
        } else if (v.getSlotNumber() > node.data.getSlotNumber()) {
            node.right = insertRec(node.right, v);
        } else {
            node.data = v; // same slot re-used -> overwrite record
        }
        return node;
    }

    /** O(log n) average-case search by slot number (vs O(n) in the linked list). */
    public Vehicle search(int slotNumber) {
        TreeNode<Vehicle> node = root;
        while (node != null) {
            if (slotNumber == node.data.getSlotNumber()) return node.data;
            node = (slotNumber < node.data.getSlotNumber()) ? node.left : node.right;
        }
        return null;
    }

    /** Remove the vehicle record at the given slot (called when that vehicle exits). */
    public void delete(int slotNumber) {
        root = deleteRec(root, slotNumber);
    }

    private TreeNode<Vehicle> deleteRec(TreeNode<Vehicle> node, int slotNumber) {
        if (node == null) return null;

        if (slotNumber < node.data.getSlotNumber()) {
            node.left = deleteRec(node.left, slotNumber);
        } else if (slotNumber > node.data.getSlotNumber()) {
            node.right = deleteRec(node.right, slotNumber);
        } else {
            // Node found
            if (node.left == null) return node.right;
            if (node.right == null) return node.left;

            // Two children: replace with inorder successor (smallest in right subtree)
            TreeNode<Vehicle> successor = node.right;
            while (successor.left != null) successor = successor.left;
            node.data = successor.data;
            node.right = deleteRec(node.right, successor.data.getSlotNumber());
        }
        return node;
    }

    /** Inorder: naturally yields vehicles sorted by slot number (BST property). */
    public List<Vehicle> inorder() {
        List<Vehicle> list = new ArrayList<>();
        inorderRec(root, list);
        return list;
    }
    private void inorderRec(TreeNode<Vehicle> node, List<Vehicle> list) {
        if (node == null) return;
        inorderRec(node.left, list);
        list.add(node.data);
        inorderRec(node.right, list);
    }

    /** Preorder: root-left-right - useful for copying/exporting tree structure. */
    public List<Vehicle> preorder() {
        List<Vehicle> list = new ArrayList<>();
        preorderRec(root, list);
        return list;
    }
    private void preorderRec(TreeNode<Vehicle> node, List<Vehicle> list) {
        if (node == null) return;
        list.add(node.data);
        preorderRec(node.left, list);
        preorderRec(node.right, list);
    }

    /** Postorder: left-right-root - useful for safe deletion/cleanup order. */
    public List<Vehicle> postorder() {
        List<Vehicle> list = new ArrayList<>();
        postorderRec(root, list);
        return list;
    }
    private void postorderRec(TreeNode<Vehicle> node, List<Vehicle> list) {
        if (node == null) return;
        postorderRec(node.left, list);
        postorderRec(node.right, list);
        list.add(node.data);
    }

    public boolean isEmpty() { return root == null; }
}