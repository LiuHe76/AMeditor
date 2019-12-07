package component;

import javafx.scene.Group;
import javafx.scene.text.Text;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 *  TextBuffer:
 *    - organizing linked list for text input.
 *    - using hashmap to maintain line information to support fast mouse clicking operation.
 *    - supporting series of operations on underlying linked list.
 */
public class TextBuffer {
    private Node sentinel;
    private Node currentPos;
    private Node traversalMan;
    private Node helper;

    private Group root;
    private IOManager ioManager;

    private Map<Integer, Node> lineNo_to_startNode;
    private int maxLine;

    private Node dragStart;
    private Node dragEnd;

    private class Node {
        private Text text;
        private Node next, pre;

        public Node(Text text) {
            this.text = text;
        }

        public Node(Text text, Node next, Node pre) {
            this.text = text;
            this.next = next;
            this.pre = pre;
        }

        public String toString() {
            return "Node[text: " + text.getText() + "]";
        }
    }

    public TextBuffer(IOManager ioManager, Group root) throws IOException {
        sentinel = new Node(null);
        sentinel.next = sentinel;
        sentinel.pre = sentinel;
        currentPos = sentinel;

        lineNo_to_startNode = new HashMap<>();

        this.ioManager = ioManager;
        this.root = root;
        init(ioManager.new Reader());
    }


    /**
     *  initialize original linked list from given input file by calling read method supported by IOManager.Reader.
     */
    private void init(IOManager.Reader reader) throws IOException {
        char c;
        while ((c = reader.getNextCharacter()) != (char)-1) {
            add(new Text(String.valueOf(c)));
        }
        currentPos = sentinel;
    }


    /**
     *   add element to linked list right after current position.
     */
    public void add(Text text) {
        root.getChildren().add(text);
        Node node = new Node(text, currentPos.next, currentPos);
        currentPos.next.pre = node;
        currentPos.next = node;
        currentPos = currentPos.next;
    }


    /**
     *   delete element from linked list specified by current position.
     */
    public Node[] delete() {
        if (currentPos == sentinel) {
            return new Node[0];
        }
        root.getChildren().remove(currentPos.text);
        currentPos.next.pre = currentPos.pre;
        currentPos.pre.next = currentPos.next;
        Node to_return = currentPos;
        currentPos = currentPos.pre;
        return new Node[] {to_return, currentPos};
    }


    /**
     *   group methods to satisfy Region Rendering function triggered by mouse pressing and dragging event.
     */
    // method to record dragging start node.
    public void setDragStart() {
        this.dragStart = currentPos.next;
    }

    // method to record dragging end node.
    public void setDragEnd() {
        this.dragEnd = currentPos;
    }

    // set helper node to dragging start node for iteration.
    public void renderInit() {
        traversalMan = dragStart;
    }

    // return text content in current node and move formard.
    public Text getTextToRender() {
        if (traversalMan == dragEnd.next) {
            return null;
        }
        Text text = traversalMan.text;
        traversalMan = traversalMan.next;
        return text;
    }

    // return text content int current node and move forward.
    public Text getPreToRender() {
        if (traversalMan == dragEnd.pre) {
            return null;
        }
        Text text = traversalMan.text;
        traversalMan = traversalMan.pre;
        return text;
    }


    /**
     *  group method to support Content Rendering operation by RenderEngine client methods.
     */
    // helper method to locate current node for render cursor at the correct position.
    public boolean isCurrentPos() {
        return traversalMan == currentPos;
    }

    // initialize helper node to the beginning of linked list to start rendering.
    public void resetTraversalMan() {
        traversalMan = sentinel;
    }

    // return text content in the current node.
    public Text current() {
        if (traversalMan == sentinel) {
            return null;
        }
        return traversalMan.text;
    }

    // move helper node to next position and return corresponding text content.
    public Text advance() {
        traversalMan = traversalMan.next;
        return current();
    }

    // return current node.
    public Node getCurrentNode() {
        return traversalMan;
    }


    /**
     *  group method to support Cursor Location by mouse clicking event.
     */
    // return text in the current node.
    public Text currentPos_current() {
        return currentPos.text;
    }

    // judgement helper method for making sure cursor at most be set to the end of the content.
    public boolean isEnd() {
        return currentPos == sentinel;
    }

    // move forward current node to its previous position [right position for cursor to go].
    public void decreCurrent(boolean type) {
        if (type) {
            if (currentPos == sentinel) {
                return;
            }
            currentPos = currentPos.pre;
        } else {
            currentPos = currentPos.pre;
        }
    }

    // move backward current node to its next position to satisfy X position requirement.
    public void increCurrent(boolean type) {
        if (type) {
            if (currentPos.next == sentinel) {
                return;
            }
            currentPos = currentPos.next;
        } else {
            currentPos = currentPos.next;
        }
    }

    // two special cases may happen in Cursor Location traggered by mouse clicking event:
    //  - one for jumping out of bound at the bottom.
    public void setCurToTail() {
        currentPos = sentinel.pre;
    }

    // - one for jumpping out of bound at the very beginning.
    public void setCurToHead() {
        currentPos = sentinel;
    }


    /**
     *  group methods to store extra information in order to satisfy runtime bound for clicking operations.
     *  keep record line number information and mapping between line number and start node for each line.
     */
    // reset line number and mapping hashtable before Content Rendering.
    public void resetLineInfo() {
        maxLine = 1;
        lineNo_to_startNode.clear();
    }

    // put one pair of mapping in the hashtable and increment line number by one.
    public void putLineNo_StartNode_mapping(Object node) {
        lineNo_to_startNode.put(maxLine++, (Node) node);
    }

    // helper method to move current node at the level of lines.
    // a helper node initially stores next line for case handling.
    public void setCurToTargetNo(int no) {
        currentPos = lineNo_to_startNode.get(no);
        helper = lineNo_to_startNode.getOrDefault(no+1, null);
    }

    // return max line number(larger than real line number by one).
    public int getMaxLine() { return maxLine; }


     // save file by calling write method supported by IOManager.Writer.
    public void savefile() throws IOException {
        System.out.println("Saving file to " + ioManager.getFilename() + "...");
        save(ioManager.new Writer());
        System.out.println("Finish saving file.");
    }

    private void save(IOManager.Writer writer) throws IOException {
        Node node = sentinel.next;
        while (node != sentinel) {
            writer.writeNextCharacter(node.text.getText().charAt(0));
            node = node.next;
        }
        writer.close();
    }


    /**
     *  group method to support undo and redo features.
     */
    // return current node.
    public Node getCurrentPosNode() {
        return currentPos;
    }

    // set current node to given node.
    public void setcurNodeToGivenPos(Object node) {
        currentPos = (Node) node;
    }

    // add node to right after current node.
    public void addNode(Object node) {
        Node n = (Node) node;
        root.getChildren().add(n.text);
        currentPos.next.pre = n;
        currentPos.next = n;
        currentPos = currentPos.next;
    }
}
