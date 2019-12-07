package component;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.control.ScrollBar;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

/**
 *   RenderEngine: rendering content to window:
 *     - text content
 *     - flickering cursor
 *     - selection region
 *     - scroll bar
 */
public class RenderEngine {
    private static final String fontType = "Verdana";
    private static final int INIT_FONT_SIZE = 15;
    private static final int X_INIT = 6;
    private static final VPos POS = VPos.TOP;

    private TextBuffer textBuffer;
    private int fontSize;
    private int span;
    private int lineHeight;
    private Group Root;
    private Group root;
    private ScrollBar scrollBar;
    private double windowHeight;

    private boolean isVisiable;
    private Rectangle cursor;
    private Text cursorPos;

    private List<Rectangle> renderPieces;
    private int renderFlag;
    private double x_start, y_start;
    private boolean reversed;

    private int x_pos;
    private int y_pos;

    private Deque<Text> queue;
    private Deque<Object> auxQueue;

    public RenderEngine(TextBuffer textBuffer, double span, Group Root, Group root, ScrollBar scrollBar, double windowHeight) {
        this.textBuffer = textBuffer;
        this.fontSize = INIT_FONT_SIZE;
        this.span = round(span);
        this.Root = Root;
        this.root = root;
        this.renderPieces = new ArrayList<>();
        initCursor();
        updateLineHeight();
        updateCursor();

        this.scrollBar = scrollBar;
        this.windowHeight = windowHeight;

        update();
    }


    /**
     *  group method for set, get, update operations.
     */
    // initialize cursor attributes.
    // binding animation to realized cursor flickering effect.
    private void initCursor() {
        cursor = new Rectangle();
        root.getChildren().add(cursor);
        cursor.setWidth(1.5);

        Timeline timeline = new Timeline();
        timeline.setCycleCount(Animation.INDEFINITE);

        KeyFrame keyFrame = new KeyFrame(Duration.seconds(0.5), new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if (isVisiable) {
                    cursor.setFill(Color.WHITE);
                    isVisiable = !isVisiable;
                } else {
                    cursor.setFill(Color.BLACK);
                    isVisiable = !isVisiable;
                }
                actionEvent.consume();
            }
        });

        timeline.getKeyFrames().add(keyFrame);
        timeline.play();
    }

    // update cursor height to keep consistent with line height.
    private void updateCursor() {
        cursor.setHeight(lineHeight);
    }

    // set text attributes(x coordinate, y coordinate, font size) dynamically
    private void setText(Text text, int x_pos, int y_pos) {
        text.setTextOrigin(POS);
        text.setFont(new Font(fontType, fontSize));
        text.setX(x_pos);
        text.setY(y_pos);
        text.toFront();
        if (text == cursorPos) {
            cursor.setX(x_pos + round(text.getLayoutBounds().getWidth())+1);
            cursor.setY(y_pos);
        }
    }

    // update line height triggered by font size changing (key events).
    private void updateLineHeight() {
        Text text = new Text("AM");
        text.setFont(new Font(fontType, fontSize));
        lineHeight = round(text.getLayoutBounds().getHeight());
    }

    // set updated font size.
    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
        updateLineHeight();
        updateCursor();
        update();
    }

    // get font size.
    public int getFontSize() {
        return fontSize;
    }

    // set span taking scroll bar width, two-ends margin into consideration.
    public void setSpan(double span) {
        this.span = round(span);
        update();
    }


    /**
     *  group methods to handle scroll bar events:
     *    - get and set scroll bar value.
     *    - moving subroot(containing text and cursor) y position to show target content.
     */
    // get content height for judge existence validity of scroll bar.
    private int getTextHeight() {
        return lineHeight * (textBuffer.getMaxLine() - 1);
    }

    // set window height triggered by window resizing event
    // window height attribute also used for  judge existence validity of scroll bar.
    public void setWindowHeight(double windowHeight) {
        this.windowHeight = windowHeight;
    }

    // set Y position of subroot to show target content.
    public void setRootYPos(int y) {
        root.setLayoutY(y);
    }

    // update scroll bar maxValue to be the difference between text height and window height if
    // scroll bar should be existent.
    private void updateScrollBarSize() {
        int textHeight = getTextHeight();
        if (textHeight <= windowHeight) {
            Root.getChildren().remove(scrollBar);
        } else {
            if (!Root.getChildren().contains(scrollBar)) {
                Root.getChildren().add(scrollBar);
            }
            double range = textHeight - windowHeight;
            scrollBar.setMax(range);
            scrollBar.setVisibleAmount(10);
        }
    }

    // get value of scroll bar(round to int).
    private int getScrollVal() {
        return Math.abs((int)scrollBar.getValue());
    }

    // core function to satisfying subroot Y position adjustment for two cases:
    //  - content deletion.
    //  - cursor(invisible) position changing.
    private void updateRootPos() {
        if (!Root.getChildren().contains(scrollBar)) {
            scrollBar.setValue(0);
            setRootYPos(0);
            return;
        }

        int sbUpper = getScrollVal();
        int sbLower = sbUpper + round(windowHeight);

        int textHeight = getTextHeight();
        if (textHeight < sbLower) {
            setRootYPos(-(sbUpper - (sbLower - textHeight)));
            scrollBar.setValue(scrollBar.getMax());
        }

        int cursorYUPos = round(cursor.getY());
        int cursorYDPos = cursorYUPos + round(cursor.getHeight());
        if (cursorYUPos >= sbUpper && cursorYDPos <= sbLower) {
            return;
        }
        if (cursorYUPos < sbUpper) {
            int mov = sbUpper - cursorYUPos;
            setRootYPos(-(sbUpper - mov));
            scrollBar.setValue(sbUpper - mov);
        } else {
            int mov = cursorYDPos - sbLower;
            setRootYPos(-(sbUpper + mov));
            scrollBar.setValue(sbUpper + mov);
        }

    }


    /**
     *  group methods to support cursor movement between lines requires by UP/DOWN arrow keys events.
     */
    // using cursor y coordinate to locate line number.
    private int getCurrentLineByCursor() {
        return Math.min((int)cursor.getY() / lineHeight + 1, textBuffer.getMaxLine()-1);
    }

    // moving cursor to next line.
    public void jumpToNextLine() {
        int currentLineNo = getCurrentLineByCursor();
        if (currentLineNo == textBuffer.getMaxLine()-1) {
            textBuffer.setCurToTail();
            return;
        }
        int curX = round(cursor.getX());
        textBuffer.setCurToTargetNo(currentLineNo+1);
        lineJumpHelper(curX);
    }

    // move cursor to previous line.
    public void jumpToPreLine() {
        int currentLineNo = getCurrentLineByCursor();
        if (currentLineNo == 1) {
            textBuffer.setCurToHead();
            return;
        }
        int curX = round(cursor.getX());
        textBuffer.setCurToTargetNo(currentLineNo-1);
        lineJumpHelper(curX);
    }

    // method to locate cursor to position in-line compared to target X coordinate.
    private void lineJumpHelper(int curX) {
        int accX = 0;
        while (true) {
            if (textBuffer.isEnd()) {
                textBuffer.decreCurrent(false);
                return;
            }
            if (textBuffer.currentPos_current().getText().equals("\n")) {
                textBuffer.decreCurrent(false);
                return;
            }
            Text text = textBuffer.currentPos_current();
            accX += round(text.getLayoutBounds().getWidth());
            if (accX > curX) {
                textBuffer.decreCurrent(false);
                return;
            }
            textBuffer.increCurrent(false);
        }
    }


    /**
     *  group methods to support cursor movement jumping between lines required by mouse clicking event.
     */
    // using y coordinate to location target line.
    private int getTargetLineByPos(double y) {
        return (int)y / lineHeight + 1;
    }

    // move cursor to target position (X, Y).
    public void jumpToXY(double x, double y) {
        if (!root.getChildren().contains(cursor)) {
            root.getChildren().add(cursor);
        }

        int targetLineNo = getTargetLineByPos(y+getScrollVal());
        if (targetLineNo >= textBuffer.getMaxLine()) {
            textBuffer.setCurToTail();
            return;
        }
        textBuffer.setCurToTargetNo(targetLineNo);
        lineJumpHelper(round(x));
    }


    /**
     *  group methods enable Selected Region Rendering functionality.
     */
    // helper method for setting start/end nodes corresponding to selection region border classified by given type variable.
    //   - true -> start node
    //   - false -> end node
    private void selectHelper(int curX, boolean type) {
        int accX = 0;
        while (true) {
            if (textBuffer.isEnd()) {
                textBuffer.decreCurrent(false);
                selectByType(type);
                return;
            }
            if (textBuffer.currentPos_current().getText().equals("\n")) {
                textBuffer.decreCurrent(false);
                selectByType(type);
                return;
            }
            Text text = textBuffer.currentPos_current();
            accX += round(text.getLayoutBounds().getWidth());
            if (accX > curX) {
                textBuffer.decreCurrent(false);
                selectByType(type);
                return;
            }
            textBuffer.increCurrent(false);
        }
    }

    // client method for end-nodes saving.
    // determine selection direction resulting setting flag value(reversed).
    public void selectEnd(double x, double y, boolean type) {
        root.getChildren().remove(cursor);

        if (type) {
            x_start = x;
            y_start = y;
        } else {
            if (x >= x_start || y >= y_start) {
                reversed = false;
            } else {
                reversed = true;
            }
        }

        int targetLineNo = getTargetLineByPos(y+getScrollVal());
        if (targetLineNo >= textBuffer.getMaxLine()) {
            textBuffer.setCurToTail();
            selectByType(type);
            return;
        }
        textBuffer.setCurToTargetNo(targetLineNo);
        selectHelper(round(x), type);
    }

    // call textBuffer supported method to save corresponding node.
    private void selectByType(boolean type) {
        if (type) {
            textBuffer.setDragStart();
        } else {
            textBuffer.setDragEnd();
        }
    }

    // iterative from start node to end node, add each text fragment to the collection object.
    public void renderSelectedRegion() {
        textBuffer.renderInit();
        renderFlag = 2;
        Text text;
        root.getChildren().removeAll(renderPieces);
        renderPieces.clear();
        if (!reversed) {
            while ((text = textBuffer.getTextToRender()) != null) {
              addRenderPieces(text);
            }
        } else {
            while ((text = textBuffer.getPreToRender()) != null) {
                addRenderPieces(text);
            }
        }

    }

    // create rectangle using as background of selected region.
    private void addRenderPieces(Text text) {
        Rectangle rectangle = new Rectangle();
        rectangle.setX(text.getX());
        rectangle.setY(text.getY());
        rectangle.setWidth(text.getLayoutBounds().getWidth());
        rectangle.setHeight(lineHeight);
        rectangle.setFill(Color.VIOLET);
        rectangle.toBack();
        root.getChildren().add(rectangle);
        renderPieces.add(rectangle);
    }


    /**
     *  group methods associated with system clipboard.
     */
    // copy operation.
    public void doCopy() {
        if (renderPieces.size() == 0) {
            System.out.println("Nothing is selected for copy operation.");
            return;
        }
        StringBuffer sb = new StringBuffer();

        textBuffer.renderInit();
        Text text;
        if (!reversed) {
            while ((text = textBuffer.getTextToRender()) != null) {
                sb.append(text.getText());
            }
        } else {
            while ((text = textBuffer.getPreToRender()) != null) {
                sb.append(text);
            }
            String reversedText = sb.toString();
            sb = new StringBuffer();
            for (int i = reversedText.length()-1; i >= 0; i -= 1) {
                sb.append(reversedText.charAt(i));
            }
        }
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(sb.toString());
        clipboard.setContent(content);
        System.out.println("Copy succeed.");
    }

    // paste operation.
    public Object[] doPaste() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        String content = clipboard.getString();
        if (content == null) {
            System.out.println("Nothing on clipboard.");
            return new Object[0];
        }
        Object[] eventInfo = new Object[2];
        eventInfo[0] = content.length();
        for (int i = 0; i < content.length(); i += 1) {
            textBuffer.add(new Text(String.valueOf(content.charAt(i))));
        }
        eventInfo[1] = textBuffer.getCurrentPosNode();

        update();
        return eventInfo;
    }


    /**
     *  core method: rendering window with the current text content and cursor rectangle.
     */
    public void update() {
        textBuffer.resetLineInfo();
        textBuffer.resetTraversalMan();
        x_pos = X_INIT;
        y_pos = 0;
        queue = new LinkedList<>();
        auxQueue = new LinkedList<>();
        cursorPos = null;

        renderFlag = Math.max(renderFlag-1, -1);
        if (renderFlag == 0) {
            root.getChildren().removeAll(renderPieces);
            renderPieces.clear();
        }

        Text text;
        while ((text = textBuffer.advance()) != null) {
            if (textBuffer.isCurrentPos()) {
                cursorPos = text;
            }
            if (x_pos == X_INIT) {
                textBuffer.putLineNo_StartNode_mapping(textBuffer.getCurrentNode());
            }
            if (text.getText().equals("\n")) {
                if (cursorPos == text) {
                    cursor.setX(X_INIT);
                    cursor.setY((textBuffer.getMaxLine() - 1) * lineHeight);
                }

                x_pos = X_INIT;
                y_pos += lineHeight;
                queue.clear();
                auxQueue.clear();
            } else if (text.getText().equals(" ")) {
                queue.clear();
                auxQueue.clear();
                setText(text, x_pos, y_pos);
                x_pos += round(text.getLayoutBounds().getWidth());

                if (cursorPos == text) {
                    if (x_pos > span) {
                        cursor.setX(span);
                        cursor.setY(y_pos);
                    } else {
                        cursor.setX(x_pos+1);
                        cursor.setY(y_pos);
                    }
                }
            } else {
                queue.addLast(text);
                auxQueue.addLast(textBuffer.getCurrentNode());
                setText(text, x_pos, y_pos);
                x_pos += round(text.getLayoutBounds().getWidth());
                if (x_pos > span) {
                    if (round(queue.getFirst().getX()) == X_INIT) {
                        text = queue.getLast();
                        queue.clear();
                        queue.addLast(text);
                        textBuffer.putLineNo_StartNode_mapping(textBuffer.getCurrentNode());
                        x_pos = X_INIT;
                        y_pos += lineHeight;
                        setText(text, x_pos, y_pos);
                        x_pos += round(text.getLayoutBounds().getWidth());
                    } else {
                        x_pos = X_INIT;
                        y_pos += lineHeight;
                        for (Text t : queue) {
                            setText(t, x_pos, y_pos);
                            x_pos += round(t.getLayoutBounds().getWidth());
                        }
                        textBuffer.putLineNo_StartNode_mapping(auxQueue.getFirst());
                        auxQueue.clear();
                    }
                }
            }
        }

        if (cursorPos == null) {
            cursor.setX(X_INIT);
            cursor.setY(0);
        }

        updateScrollBarSize();
        updateRootPos();
    }


    /**
     *  round double value to integer.
     */
    private int round(double val) {
        return (int) Math.round(val);
    }
}
