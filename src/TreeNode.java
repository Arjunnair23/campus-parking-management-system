/**
 * TreeNode.java
 * -------------------------------------------------------
 * Generic binary tree node. Used as the building block for
 * VehicleBST - this is the "Implementing binary tree for
 * structured data storage" requirement, with the BST below
 * being the specific (and more useful) kind of binary tree
 * we build on top of it.
 * -------------------------------------------------------
 */
public class TreeNode<T> {
    T data;
    TreeNode<T> left;
    TreeNode<T> right;

    public TreeNode(T data) {
        this.data = data;
        this.left = null;
        this.right = null;
    }
}
