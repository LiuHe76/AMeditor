package component;

import entity.*;

import java.util.Deque;
import java.util.LinkedList;

/**
 *  HistoryManager: manage undo and redo operations.
 *  undo record size should be no more than 100.
 *  redo record should be clear when new operation is performed.
 */
public class HistoryManager {
    private static final int LIMIT = 100;

    private Deque<Event> eventlist;
    private Deque<Event> undonelist;

    private TextBuffer textBuffer;

    public HistoryManager(TextBuffer textBuffer) {
        eventlist = new LinkedList<>();
        undonelist = new LinkedList<>();

        this.textBuffer = textBuffer;
    }

    /**
     *  client method to put a piece of event record to eventlist stack with the maximum of 100.
     *  each time a new event is added, undonelist supporting redo operation is cleared.
     */
    public void put(Event e) {
        eventlist.addLast(e);
        if (eventlist.size() > LIMIT) {
            eventlist.removeFirst();
        }
        clearRedo();
    }

    // method for undo operation.
    public void undo() {
        if (eventlist.size() == 0) {
            System.out.println("Nothing for rollback.");
            return;
        }
        Event e = eventlist.removeLast();
        if (e instanceof AddEvent) {
            textBuffer.setcurNodeToGivenPos(((AddEvent) e).getElement());
            Object[] nodeinfo = textBuffer.delete();
            DeleteEvent re_e = new DeleteEvent();
            re_e.setPre(nodeinfo[1]);
            re_e.setElement(nodeinfo[0]);
            undonelist.addLast(re_e);
        } else if (e instanceof DeleteEvent) {
            textBuffer.setcurNodeToGivenPos(((DeleteEvent) e).getPre());
            textBuffer.addNode(((DeleteEvent) e).getElement());
            AddEvent re_e = new AddEvent();
            re_e.setElement(((DeleteEvent) e).getElement());
            undonelist.addLast(re_e);
        } else if (e instanceof PasteEvent) {
            Deque<Object> nodes = new LinkedList<>();
            textBuffer.setcurNodeToGivenPos(((PasteEvent) e).getLastNode());
            for (int i = 0; i < ((PasteEvent) e).getLength(); i += 1) {
                nodes.addFirst(textBuffer.delete()[0]);
            }
            SequenceDeleteEvent re_e = new SequenceDeleteEvent();
            re_e.setNodes(nodes);
            re_e.setStart(textBuffer.getCurrentPosNode());
            undonelist.addLast(re_e);
        }

    }

    // method for redo operation.
    public void redo() {
        if (undonelist.size() == 0) {
            System.out.println("Nothing for rollback recovery");
            return;
        }
        Event e = undonelist.removeLast();
        if (e instanceof AddEvent) {
            textBuffer.setcurNodeToGivenPos(((AddEvent) e).getElement());
            Object[] nodeinfo = textBuffer.delete();
            DeleteEvent un_e = new DeleteEvent();
            un_e.setPre(nodeinfo[1]);
            un_e.setElement(nodeinfo[0]);
            eventlist.addLast(un_e);
        } else if (e instanceof DeleteEvent) {
            textBuffer.setcurNodeToGivenPos(((DeleteEvent) e).getPre());
            textBuffer.addNode(((DeleteEvent) e).getElement());
            AddEvent un_e = new AddEvent();
            un_e.setElement(((DeleteEvent) e).getElement());
            eventlist.addLast(un_e);
        } else if (e instanceof SequenceDeleteEvent) {
            textBuffer.setcurNodeToGivenPos(((SequenceDeleteEvent) e).getStart());
            Deque<Object> nodes = ((SequenceDeleteEvent) e).getNodes();
            for (Object node : nodes) {
                textBuffer.addNode(node);
            }
            PasteEvent un_e = new PasteEvent();
            un_e.setLastNode(nodes.getLast());
            un_e.setLength(nodes.size());
            eventlist.addLast(un_e);
        }
    }

    /**
     *  clear undonelist supporting redo operation.
     */
    private void clearRedo() {
        undonelist.clear();
    }
}
