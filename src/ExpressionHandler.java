/**
 * ExpressionHandler.java
 * -------------------------------------------------------
 * Basic-level stack application: checks whether an
 * expression's brackets are balanced. This represents the
 * "simple expression handling (basic level)" requirement.
 * (Full infix-to-postfix fee-formula evaluation is a
 * natural extension for a later phase.)
 * -------------------------------------------------------
 */
public class ExpressionHandler {
 
    /** Returns true if all (), {}, [] pairs in the expression are balanced. */
    public static boolean isBalanced(String expression) {
        LinkedStack<Character> stack = new LinkedStack<>();
 
        for (char ch : expression.toCharArray()) {
            if (ch == '(' || ch == '{' || ch == '[') {
                stack.push(ch);
            } else if (ch == ')' || ch == '}' || ch == ']') {
                if (stack.isEmpty()) {
                    return false; // closing bracket with nothing to match
                }
                char top = stack.pop();
                if (!matches(top, ch)) {
                    return false; // wrong pair, e.g. "(]"
                }
            }
            // any other character is ignored (digits, operators, letters)
        }
        return stack.isEmpty(); // true only if every opener was closed
    }
 
    private static boolean matches(char open, char close) {
        return (open == '(' && close == ')') ||
               (open == '{' && close == '}') ||
               (open == '[' && close == ']');
    }
}