/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 *@author Erik M. Buck
 */
package airviewer;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Pagination;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/**
 * This class encapsulates the Controller in the Model-View-Controller Design
 * Pattern. The Controller in primarily concerned with loading the Model,
 * accepting/handling user input, and providing feedback to users. There are no
 * dependencies on the underlying Model representation of PDF data. This class
 * is dependent on JavaFx Nodes that implement the user interface.
 *
 * @author Erik M. Buck (Reviewed by Ankita Saxena
 */
public class AIRViewerController implements Initializable {

    /**
     * The default path to a PDF document to open. This path is only used if the
     * user has not specified some other path either through command line
     * arguments or via an Open File user interface.
     */
    static final String DEFAULT_PATH = "sample.pdf";

    /**
     * The variable is initialized as a side effect of when loading the
     * application's user interface from a JavaFx FXML file. Assertions are used
     * to verify correct initialization.
     */
    @FXML
    private Pagination pagination;

    /**
     * The variable is initialized as a side effect of when loading the
     * application's user interface from a JavaFx FXML file. Assertions are used
     * to verify correct initialization.
     */
    @FXML
    private MenuItem openMenuItem;

    /**
     * The variable is initialized as a side effect of when loading the
     * application's user interface from a JavaFx FXML file. Assertions are used
     * to verify correct initialization.
     */
    @FXML
    private MenuItem saveAsMenuItem;

    /**
     * The variable is initialized as a side effect of when loading the
     * application's user interface from a JavaFx FXML file. Assertions are used
     * to verify correct initialization.
     */
    @FXML
    private MenuItem closeMenuItem;

    /**
     * The variable is initialized as a side effect of when loading the
     * application's user interface from a JavaFx FXML file. Assertions are used
     * to verify correct initialization.
     */
    @FXML
    private MenuItem extractTextMenuItem;

    /**
     * The variable is initialized as a side effect of when loading the
     * application's user interface from a JavaFx FXML file. Assertions are used
     * to verify correct initialization.
     */
    @FXML
    private MenuItem undoMenuItem;

    /**
     * The variable is initialized as a side effect of when loading the
     * application's user interface from a JavaFx FXML file. Assertions are used
     * to verify correct initialization.
     */
    @FXML
    private MenuItem redoMenuItem;

    @FXML
    private MenuItem addBoxAnnotationMenuItem;

    /**
     * The variable is initialized as a side effect of when loading the
     * application's user interface from a JavaFx FXML file. Assertions are used
     * to verify correct initialization.
     */
    @FXML
    private MenuItem addEllipseAnnotationMenuItem;

    /**
     * The variable is initialized as a side effect of when loading the
     * application's user interface from a JavaFx FXML file. Assertions are used
     * to verify correct initialization.
     */
    @FXML
    private MenuItem addTextAnnotationMenuItem;

    /**
     * The variable is initialized as a side effect of when loading the
     * application's user interface from a JavaFx FXML file. Assertions are used
     * to verify correct initialization.
     */
    @FXML
    private MenuItem deleteAnnotationMenuItem;

    /**
     * This is the Model that encapsulates a PDF document and provides images of
     * PDF content, the selection set, and other state information presented by
     * this class to refresh in a user interface. This class responds to user
     * inputs and mutates the Model as directed by the user. Note: There are
     * situations where the model is null e.g. when the user has not yet
     * identified a PDF file to load. Methods of this class implement reasonable
     * default behavior such as disabling inapplicable menu items when the model
     * is null. handle a null model.
     */
    private AIRViewerModel model;

    /**
     * This is the JavaFx node used to display an image of the PDF page
     * currently selected by the user. The image is obtain as needed from the
     * Model.
     */
    private ImageView currentPageImageView;

    /**
     * In order to overlay the currentPageImageView with JavaFx controls that
     * support user interface for editing operations like selection indication,
     * text editing, and dragging, the currentPageImageView is a child of
     * pageViewGroup. JavaFx Nodes representing user interface controls for
     * editing are added or removed as children of pageViewGroup (siblings of
     * currentPageImageView) when contextually appropriate.
     */
    private Group pageViewGroup;

    /**
     * The X position in the pageViewGroup coordinate system of the pointer
     * location when a drag operation was last started by the user.
     */
    private float dragStartX;

    /**
     * The "flipped" Y position in pageViewGroup coordinate system of the
     * pointer (mouse or finger) location when a drag operation was last started
     * by the user. PDF documents (and the Model subsystem of this application)
     * have the coordinate system origin in the lower left corner, but JavaFx
     * has the origin in the upper left corner. The Y coordinate is "flipped" by
     * subtracting the Java Fx coordinate from the height of the pageViewGroup
     * to convert from fromJavaFx coordinates to PDF coordinates.
     */
    private float dragStartFlippedY;

    /**
     * This is the cumulative distance along the X axis in the pageViewGroup
     * coordinate system that the pointer (mouse or finger) within the current
     * drag operation if any. This variable is used in conjunction with the
     * isDragging instance variable is only true while a drag operation is
     * currently happening.
     */
    private float cumulativeDragDeltaX;

    /**
     * This is the cumulative distance along the y axis in the pageViewGroup
     * coordinate system that the pointer (mouse or finger) within the current
     * drag operation if any. See the dragStartFlippedY description for an
     * explanation of the "flipped" coordinate system. This variable is used in
     * conjunction with the isDragging instance variable is only true while a
     * drag operation is currently happening.
     */
    private float cumulativeDragDeltaFlippedY;

    /**
     * This variable is initialized to be false false, is set to true when a
     * user pointer (mouse or finger) "drag" operations starts, and is set to
     * false immediately after a "drag" operation concludes.
     */
    private boolean isDragging;

    /**
     * Calling this method replaces any currently open Model with a new Model
     * instance encapsulating the PDF document at startPath in the file system.
     *
     * @param startPath A file system path to a PDF file to be loaded via
     * creation of a new Model. Only one Model can be open at a time. Calling
     * this method replaces any currently open Model with a new Model instance.
     * @return A new AIRViewerModel initialized with the PDF file at startPath
     * in the file system or null if no such PDF file could be loaded.
     */
    private AIRViewerModel promptLoadModel(String startPath) {

        AIRViewerModel loadedModel = null;
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open PDF File");
            fileChooser.setInitialFileName(startPath);

            @SuppressWarnings("unchecked")
            Window window = pagination.getScene().getWindow();
            assert null != window;

            File file = fileChooser.showOpenDialog(window);
            if (null != file) {
                String path = file.getCanonicalPath();
                loadedModel = new AIRViewerModel(Paths.get(path));
            }
        } catch (IOException ex) {
            Logger.getLogger(AIRViewerController.class.getName()).log(
                    Level.INFO,
                    "Unable to open <" + ex.getLocalizedMessage() + ">",
                    "");
            loadedModel = null;
        }

        return loadedModel;
    }

    /**
     * This method configures the editing support controls, "knobs", as needed
     * to reflect the user's selection of annotations within the Model (if any).
     * This method should be called any time the selection changes or
     * annotations are edited by the user e.g. dragged to no locations.
     */
    private void synchronizeSelectionKnobs() {
        if (null != model && null != currentPageImageView && null != pageViewGroup) {
            List<Rectangle> selectedAreas = model.getSelectedAreas();
            ArrayList<Node> victims = new ArrayList<>(pageViewGroup.getChildren());

            // Delete everything in the group that isn't currentPageImageView
            victims.stream().filter((n) -> (n != currentPageImageView)).forEach((n) -> {
                pageViewGroup.getChildren().remove(n);
            });

            // Add knobs to thegroup to indicate selection
            selectedAreas.stream().map((r) -> {
                Circle knobA = new Circle(r.getX(), (int) pageViewGroup.prefHeight(0) - r.getY(), 4);
                knobA.setStroke(Color.YELLOW);
                knobA.setStrokeWidth(2);
                pageViewGroup.getChildren().add(knobA);
                Circle knobB = new Circle(r.getX() + r.getWidth(), (int) pageViewGroup.prefHeight(0) - r.getY(), 4);
                knobB.setStroke(Color.YELLOW);
                knobB.setStrokeWidth(2);
                pageViewGroup.getChildren().add(knobB);
                Circle knobC = new Circle(r.getX() + r.getWidth(), (int) pageViewGroup.prefHeight(0) - (r.getY() + r.getHeight()), 4);
                knobC.setStroke(Color.YELLOW);
                knobC.setStrokeWidth(2);
                pageViewGroup.getChildren().add(knobC);
                Circle knobD = new Circle(r.getX(), (int) pageViewGroup.prefHeight(0) - (r.getY() + r.getHeight()), 4);
                return knobD;
            }).map((knobD) -> {
                knobD.setStroke(Color.YELLOW);
                return knobD;
            }).map((knobD) -> {
                knobD.setStrokeWidth(2);
                return knobD;
            }).forEach((knobD) -> {
                pageViewGroup.getChildren().add(knobD);
            });
            if (1 == model.getSelectionCount()) {
                Rectangle r = model.getSelectedAreas().get(0);
                TextField textEntry = new TextField(model.getSelectedContents().get(0));
                textEntry.setPrefWidth(r.getWidth());
                textEntry.setLayoutX(r.getX() - textEntry.getLayoutBounds().getMinY());
                textEntry.setLayoutY((pageViewGroup.prefHeight(0) - r.getY()) - textEntry.getLayoutBounds().getMinY());

                textEntry.setOnAction((ActionEvent event) -> {
                    System.out.println(textEntry.getText());
                    if (null != pagination) {
                        model.executeDocumentCommandWithNameAndArgs("ChangeSelectedAnnotationText",
                                new String[]{Integer.toString(pagination.getCurrentPageIndex()), textEntry.getText()});
                        refreshUserInterface();
                    }
                });

                pageViewGroup.getChildren().add(textEntry);
            }
        }

    }

    /**
     * This method configures the user interface by enabling or disabling menu
     * items, setting the image displayed for the currently selected page in a
     * PDF document, etc. The user interface state is determined by the Model
     * state, and one valid Model state is "no model" i.e. model is null. This
     * method should be called any time the Model subsystem changes state in any
     * way that should be presented in the user interface.
     */
    private void refreshUserInterface() {
        assert pagination != null : "fx:id=\"pagination\" was not injected: check the application's FXML file .";
        assert saveAsMenuItem != null : "fx:id=\"saveAsMenuItem\" was not injected: check the application's FXML file .";
        assert extractTextMenuItem != null : "fx:id=\"extractTextMenuItem\" was not injected: check the application's FXML file .";
        assert undoMenuItem != null : "fx:id=\"undoMenuItem\" was not injected: check the application's FXML file .";
        assert redoMenuItem != null : "fx:id=\"redoMenuItem\" was not injected: check the application's FXML file .";
        assert addBoxAnnotationMenuItem != null : "fx:id=\"addBoxAnnotationMenuItem\" was not injected: check the application's FXML file .";
        assert addEllipseAnnotationMenuItem != null : "fx:id=\"addEllipseAnnotationMenuItem\" was not injected: check the application's FXML file .";
        assert addTextAnnotationMenuItem != null : "fx:id=\"addTextAnnotationMenuItem\" was not injected: check the application's FXML file .";
        assert deleteAnnotationMenuItem != null : "fx:id=\"deleteAnnotationMenuItem\" was not injected: check the application's FXML file .";

        if (null == model) {
            pagination.setPageCount(0);
            pagination.setPageFactory(index -> {
                return makePageViewGroup(null);
            });
            pagination.setDisable(true);
            saveAsMenuItem.setDisable(true);
            extractTextMenuItem.setDisable(true);
            undoMenuItem.setDisable(true);
            redoMenuItem.setDisable(true);
            addBoxAnnotationMenuItem.setDisable(true);
            addEllipseAnnotationMenuItem.setDisable(true);
            addTextAnnotationMenuItem.setDisable(true);
            deleteAnnotationMenuItem.setDisable(true);

        } else {
            pagination.setPageCount(model.getPageCount());
            pagination.setDisable(false);
            saveAsMenuItem.setDisable(false);
            extractTextMenuItem.setDisable(false);
            undoMenuItem.setDisable(!model.getCanUndo());
            undoMenuItem.setText("Undo " + model.getSuggestedUndoTitle());
            redoMenuItem.setDisable(!model.getCanRedo());
            redoMenuItem.setText("Redo " + model.getSuggestedRedoTitle());
            addBoxAnnotationMenuItem.setDisable(false);
            addEllipseAnnotationMenuItem.setDisable(false);
            addTextAnnotationMenuItem.setDisable(false);
            deleteAnnotationMenuItem.setDisable(0 >= model.getSelectionCount());

            if (null != currentPageImageView) {
                int pageIndex = pagination.getCurrentPageIndex();
                currentPageImageView.setImage(model.getImage(pageIndex));
            }
        }
        synchronizeSelectionKnobs();
    }

    /**
     * This method creates the pageViewGroup if necessary and configures the
     * pageViewGroup with currentPageImageView as a child and appropriate event
     * handlers to support user interactions such as pointer (mouse or touch)
     * based selection of annotations, drag operations to reposition selected
     * annotations, etc. In order to overlay the currentPageImageView with
     * JavaFx controls that support user interface for editing operations like
     * selection indication, text editing, and dragging, the
     * currentPageImageView is a child of pageViewGroup. JavaFx Nodes
     * representing user interface controls for editing are added or removed as
     * children of pageViewGroup (siblings of currentPageImageView) when
     * contextually appropriate.
     *
     * @param anImage The image to be displayed by currentPageImageView or null
     * if there is no appropriate image.
     * @return A fully configured JavaFx Node (pageViewGroup)
     */
    private Group makePageViewGroup(Image anImage) {
        if (null == pageViewGroup) {
            pageViewGroup = new Group();
            currentPageImageView = new ImageView();
            pageViewGroup.getChildren().add(currentPageImageView);

            pageViewGroup.setOnMousePressed((MouseEvent me) -> {
                if (null != model && null != currentPageImageView) {

                    float flippedY = (float) currentPageImageView.getBoundsInParent().getHeight() - (float) me.getY();
                    float inPageX = (float) me.getX();
                    float inPageY = flippedY;

                    // Remember pressed location in case this turns into a drag
                    dragStartX = inPageX;
                    dragStartFlippedY = flippedY;
                    cumulativeDragDeltaX = 0;
                    cumulativeDragDeltaFlippedY = 0;

                    int pageIndex = pagination.getCurrentPageIndex();
                    if (!me.isMetaDown() && !me.isShiftDown()) {
                        model.deselectAll();
                    }
                    model.extendSelectionOnPageAtPoint(pageIndex,
                            inPageX, inPageY);

                    refreshUserInterface();
                }
            });

            pageViewGroup.setOnMouseDragged((MouseEvent me) -> {
                if (null != model && null != currentPageImageView) {

                    isDragging = true;
                    float flippedY = (float) currentPageImageView.getBoundsInParent().getHeight() - (float) me.getY();
                    float inPageX = (float) me.getX();
                    float inPageY = flippedY;
                    int pageIndex = pagination.getCurrentPageIndex();

                    // Stop registering undo commands so we don't register a slew of move commands
                    model.setIsUndoRegistrationInhibited(true);

                    cumulativeDragDeltaX += inPageX - dragStartX;
                    cumulativeDragDeltaFlippedY += flippedY - dragStartFlippedY;
                    model.executeDocumentCommandWithNameAndArgs("MoveSelectedAnnotation",
                            new String[]{Integer.toString(pageIndex),
                                Float.toString(inPageX - dragStartX),
                                Float.toString(flippedY - dragStartFlippedY)});
                    dragStartX = inPageX;
                    dragStartFlippedY = flippedY;
                    refreshUserInterface();
                }

            });

            pageViewGroup.setOnMouseReleased((MouseEvent me) -> {
                if (null != model && null != currentPageImageView) {
                    if (isDragging) {
                        isDragging = false;

                        int pageIndex = pagination.getCurrentPageIndex();
                        // Put everything back where where it was before drag started
                        model.executeDocumentCommandWithNameAndArgs("MoveSelectedAnnotation",
                                new String[]{Integer.toString(pageIndex),
                                    Float.toString(-cumulativeDragDeltaX),
                                    Float.toString(-cumulativeDragDeltaFlippedY)});

                        // Resume registering undo commands
                        model.setIsUndoRegistrationInhibited(false);

                        // Move everything to final position in one big undoable operation
                        model.executeDocumentCommandWithNameAndArgs("MoveSelectedAnnotation",
                                new String[]{Integer.toString(pageIndex),
                                    Float.toString(cumulativeDragDeltaX),
                                    Float.toString(cumulativeDragDeltaFlippedY)});

                        // register one cummulative command for undoing drag
                        refreshUserInterface();
                    }
                }

            });
        }

        assert null != currentPageImageView;

        currentPageImageView.setImage(anImage);

        if (null != model) {
            model.deselectAll();   // Clear selection when page changes
            refreshUserInterface();
        }

        return pageViewGroup;
    }

    /**
     * This method is called right after a Model is loaded to perform user
     * interface configuration changes that are only needed when the entire
     * Model changes as opposed to refreshUserInterface() which should be called
     * after every mutation (state change) within the Model.
     *
     * @param aModel The new Model to me used by the Controller subsystem.
     * @return aModel.
     */
    private AIRViewerModel reinitializeWithModel(AIRViewerModel aModel) {
        assert pagination != null : "fx:id=\"pagination\" was not injected: check the application's FXML file .";
        assert openMenuItem != null : "fx:id=\"openMenuItem\" was not injected: check the application's FXML file .";
        assert saveAsMenuItem != null : "fx:id=\"saveAsMenuItem\" was not injected: check the application's FXML file .";
        assert closeMenuItem != null : "fx:id=\"closeMenuItem\" was not injected: check the application's FXML file .";

        assert extractTextMenuItem != null : "fx:id=\"extractTextMenuItem\" was not injected: check the application's FXML file .";
        assert undoMenuItem != null : "fx:id=\"undoMenuItem\" was not injected: check the application's FXML file .";
        assert redoMenuItem != null : "fx:id=\"redoMenuItem\" was not injected: check the application's FXML file .";
        assert addBoxAnnotationMenuItem != null : "fx:id=\"addBoxAnnotationMenuItem\" was not injected: check the application's FXML file .";
        assert addEllipseAnnotationMenuItem != null : "fx:id=\"addEllipseAnnotationMenuItem\" was not injected: check the application's FXML file .";
        assert addTextAnnotationMenuItem != null : "fx:id=\"addTextAnnotationMenuItem\" was not injected: check the application's FXML file .";
        assert deleteAnnotationMenuItem != null : "fx:id=\"deleteAnnotationMenuItem\" was not injected: check the application's FXML file .";

        model = aModel;

        openMenuItem.setOnAction((ActionEvent e) -> {
            System.out.println("Open ...");
            reinitializeWithModel(promptLoadModel(AIRViewerController.DEFAULT_PATH));
        });
        openMenuItem.setDisable(false);
        closeMenuItem.setOnAction((ActionEvent e) -> {
            System.out.println("closeMenuItem ...");
            Platform.exit();
        });
        closeMenuItem.setDisable(false);

        if (null == model) {
            pagination.setPageFactory(index -> {
                return makePageViewGroup(null);
            });

        } else {

            pagination.setPageFactory(index -> {
                model.deselectAll(); // clear selection when changing page
                return makePageViewGroup(model.getImage(index));
            });
            model.deselectAll();

            saveAsMenuItem.setOnAction((ActionEvent event) -> {
                FileChooser fileChooser = new FileChooser();
                FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("PDF files (*.pdf)", "*.pdf");
                fileChooser.getExtensionFilters().add(extFilter);
                File file = fileChooser.showSaveDialog(pagination.getScene().getWindow());
                if (null != file) {
                    model.save(file);
                }
            });
            extractTextMenuItem.setOnAction((ActionEvent e) -> {
                System.out.println("extractTextMenuItem ...");
            });
            undoMenuItem.setOnAction((ActionEvent e) -> {
                model.undo();
                refreshUserInterface();
            });
            redoMenuItem.setOnAction((ActionEvent e) -> {
                model.redo();
                refreshUserInterface();
            });
            addBoxAnnotationMenuItem.setOnAction((ActionEvent e) -> {
                int pageIndex = pagination.getCurrentPageIndex();
                model.executeDocumentCommandWithNameAndArgs("AddBoxAnnotation",
                        new String[]{Integer.toString(pageIndex), "36.0", "36.0", "72.0", "72.0"});
                refreshUserInterface();
            });
            addEllipseAnnotationMenuItem.setOnAction((ActionEvent e) -> {
                int pageIndex = pagination.getCurrentPageIndex();
                model.executeDocumentCommandWithNameAndArgs("AddCircleAnnotation",
                        new String[]{Integer.toString(pageIndex), "288", "576", "144.0", "72.0", "Sample Text!"});
                refreshUserInterface();
            });
            addTextAnnotationMenuItem.setOnAction((ActionEvent e) -> {
                int pageIndex = pagination.getCurrentPageIndex();
                model.executeDocumentCommandWithNameAndArgs("AddTextAnnotation",
                        new String[]{Integer.toString(pageIndex), "36", "576", "144.0", "19.0", "A Bit More Sample Text!"});
                refreshUserInterface();
            });
            deleteAnnotationMenuItem.setOnAction((ActionEvent e) -> {
                int pageIndex = pagination.getCurrentPageIndex();
                model.executeDocumentCommandWithNameAndArgs("DeleteSelectedAnnotation",
                        new String[]{Integer.toString(pageIndex)});
                refreshUserInterface();
            });
        }

        refreshUserInterface();

        return model;
    }

    /**
     * Call this method to prompt the user to specify a file system path to a
     * PDF document to load as a new Model
     */
    public void promptUserToLoadModel() {
        reinitializeWithModel(promptLoadModel(DEFAULT_PATH));
    }

    /**
     * This override of the JavaFx initialize() Template Method verifies that
     * the user interface Nodes have been loaded from the applications FXML
     * file and configures initial application user interface state.
     *
     * @param url Unused: this is vestigial in the JavaFx API
     * @param rb Unused: this is vestigial in the JavaFx API
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        assert pagination != null : "fx:id=\"pagination\" was not injected: check the application's FXML file .";
        assert openMenuItem != null : "fx:id=\"openMenuItem\" was not injected: check the application's FXML file .";
        assert saveAsMenuItem != null : "fx:id=\"saveAsMenuItem\" was not injected: check the application's FXML file .";
        assert closeMenuItem != null : "fx:id=\"closeMenuItem\" was not injected: check the application's FXML file .";

        assert extractTextMenuItem != null : "fx:id=\"extractTextMenuItem\" was not injected: check the application's FXML file .";
        assert undoMenuItem != null : "fx:id=\"undoMenuItem\" was not injected: check the application's FXML file .";
        assert redoMenuItem != null : "fx:id=\"redoMenuItem\" was not injected: check the application's FXML file .";
        assert addBoxAnnotationMenuItem != null : "fx:id=\"addBoxAnnotationMenuItem\" was not injected: check the application's FXML file .";
        assert addEllipseAnnotationMenuItem != null : "fx:id=\"addEllipseAnnotationMenuItem\" was not injected: check the application's FXML file .";
        assert addTextAnnotationMenuItem != null : "fx:id=\"addTextAnnotationMenuItem\" was not injected: check the application's FXML file .";
        assert deleteAnnotationMenuItem != null : "fx:id=\"deleteAnnotationMenuItem\" was not injected: check the application's FXML file .";

        isDragging = false;
    }

}
