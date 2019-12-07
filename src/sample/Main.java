package sample;

import component.HistoryManager;
import component.IOManager;
import component.RenderEngine;
import component.TextBuffer;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.ScrollBar;
import javafx.scene.input.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;

import entity.*;

public class Main extends Application {
    private static final int INIT_WINDOW_HEIGHT = 500;
    private static final int INIT_WINDOW_WIDTH = 500;
    private static final int MARGIN = 5;

    private static String filename;

    private TextBuffer textBuffer;
    private IOManager ioManager;
    private RenderEngine renderEngine;
    private HistoryManager historyManager;

    private Group root;
    private Scene scene;
    private Group textGroup;
    private ScrollBar scrollBar;

    /**
     *  Initialize user interface components.
     */
    private void ElementInit() {
        root = new Group();
        scene = new Scene(root, INIT_WINDOW_HEIGHT, INIT_WINDOW_WIDTH, Color.WHITE);

        textGroup = new Group();
        root.getChildren().add(textGroup);

        scrollBar = new ScrollBar();
        scrollBar.setOrientation(Orientation.VERTICAL);
        scrollBar.setMin(0);
        scrollBar.setLayoutY(0);

        scrollBar.setPrefHeight(INIT_WINDOW_HEIGHT);
        scrollBar.setLayoutX(INIT_WINDOW_WIDTH - scrollBar.getLayoutBounds().getWidth());
    }

    /**
     *  Initialize functional components:
     *    - IOManager: managing file input and output.
     *    - TextBuffer: organizing linked list for text input and supporting series of operations on it.
     *    - RenderEngine: rendering content to window.
     *    - HistoryManager: manage undo and redo operations.
     */
    private void ComponentInit(String filename) throws IOException {
        ioManager = new IOManager(filename);
        textBuffer = new TextBuffer(ioManager, textGroup);

        double span = INIT_WINDOW_WIDTH - MARGIN - scrollBar.getLayoutBounds().getWidth();
        renderEngine = new RenderEngine(textBuffer, span, root, textGroup, scrollBar, scene.getHeight());

        historyManager = new HistoryManager(textBuffer);
    }


    /**
     *  initiate main window.
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        ElementInit();
        ComponentInit(filename);

        EventsBinding();

        primaryStage.setScene(scene);
        primaryStage.setTitle("AMeditor");
        primaryStage.show();
    }

    /**
     *   define required events:
     *     - key events
     *     - mouse events
     *     - window resizing
     *     - scroll bar events
     */
    private void EventsBinding() {
        scene.setOnKeyTyped(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                if (!keyEvent.isShortcutDown()) {
                    char keyChar = keyEvent.getCharacter().charAt(0);
                    if (keyChar == '\b') {
                        return;
                    } else {
                        if (keyChar == '\r') {
                            keyChar = '\n';
                        }
                        textBuffer.add(new Text(String.valueOf(keyChar)));
                        renderEngine.update();

                        AddEvent e = new AddEvent();
                        e.setElement(textBuffer.getCurrentPosNode());
                        historyManager.put(e);
                    }
                }
                keyEvent.consume();
            }

        });

        scene.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                if (!keyEvent.isShortcutDown()) {
                    if (keyEvent.getCode() == KeyCode.BACK_SPACE) {
                        DeleteEvent e = new DeleteEvent();
                        Object[] nodeinfo = textBuffer.delete();
                        if (nodeinfo.length != 0) {
                            e.setElement(nodeinfo[0]);
                            e.setPre(nodeinfo[1]);
                            historyManager.put(e);
                        }
                    } else if (keyEvent.getCode() == KeyCode.LEFT) {
                        textBuffer.decreCurrent(true);
                    } else if (keyEvent.getCode() == KeyCode.RIGHT) {
                        textBuffer.increCurrent(true);
                    } else if (keyEvent.getCode() == KeyCode.UP) {
                        renderEngine.jumpToPreLine();
                    } else if (keyEvent.getCode() == KeyCode.DOWN) {
                        renderEngine.jumpToNextLine();
                    }
                    renderEngine.update();
                } else {
                    if (keyEvent.getCode() == KeyCode.S) {
                        try {
                            textBuffer.savefile();
                        } catch (IOException e) {
                            System.out.println("Failed to save the file.");
                        }
                    } else if (keyEvent.getCode() == KeyCode.PLUS || keyEvent.getCode() == KeyCode.EQUALS) {
                        renderEngine.setFontSize(renderEngine.getFontSize() + 5);
                    } else if (keyEvent.getCode() == KeyCode.MINUS) {
                        renderEngine.setFontSize(Math.max(10, renderEngine.getFontSize() - 5));
                    } else if (keyEvent.getCode() == KeyCode.C) {
                        renderEngine.doCopy();
                    } else if (keyEvent.getCode() == KeyCode.V) {
                        Object[] eventInfo = renderEngine.doPaste();
                        if (eventInfo.length != 0) {
                            PasteEvent e = new PasteEvent();
                            e.setLength((Integer) eventInfo[0]);
                            e.setLastNode(eventInfo[1]);
                            historyManager.put(e);
                        }
                    } else if (keyEvent.getCode() == KeyCode.Z) {
                        historyManager.undo();
                        renderEngine.update();
                    } else if (keyEvent.getCode() == KeyCode.Y) {
                        historyManager.redo();
                        renderEngine.update();
                    }
                }

                keyEvent.consume();
            }
        });

        scene.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {

                double x_click_pos = mouseEvent.getX();
                double y_click_pos = mouseEvent.getY();
                renderEngine.jumpToXY(x_click_pos, y_click_pos);

                renderEngine.update();
                mouseEvent.consume();
            }
        });

        scene.widthProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                double curWidth = t1.doubleValue();
                scrollBar.setLayoutX(curWidth - scrollBar.getLayoutBounds().getWidth());
                double span = curWidth - MARGIN - scrollBar.getLayoutBounds().getWidth();
                renderEngine.setSpan(span);
            }
        });

        scene.heightProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                double curHeight = t1.doubleValue();
                renderEngine.setWindowHeight(curHeight);
                scrollBar.setPrefHeight(curHeight);
                renderEngine.update();
            }
        });

        scrollBar.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                // round to int value
                int val = t1.intValue();
                renderEngine.setRootYPos(-val);
            }
        });

        scene.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                double x_click_pos = mouseEvent.getX();
                double y_click_pos = mouseEvent.getY();

                renderEngine.selectEnd(x_click_pos, y_click_pos, true);

                mouseEvent.consume();
            }
        });

        scene.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                double x_click_pos = mouseEvent.getX();
                double y_click_pos = mouseEvent.getY();

                renderEngine.selectEnd(x_click_pos, y_click_pos, false);
                renderEngine.renderSelectedRegion();

                mouseEvent.consume();
            }
        });

    }

    /**
     *  Application entrance for simple text editor.
     *  Functionality specification and several design rules strictly follow the document from CS61B 2016 Spring Project 2 (Ref: http://datastructur.es/sp16/materials/proj/proj2/proj2.html#change-log)
     *  Usage from Terminal: java AMeditor filename.
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            throw new RuntimeException("Exact one argument for filename should be correctly specified.");
        }

        // test files to adapt different os
//        filename = "test/test_for_linux.txt";
//        filename = "test/test_for_windows.txt";
        filename = args[0];

        launch(args);
    }
}
