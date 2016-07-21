/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
 *
 * @author erik
 */
public class AIRViewerController implements Initializable {

    /**
     *
     */
    static final String DEFAULT_PATH = "sample.pdf";

    @FXML
    private Pagination pagination;

    @FXML
    private MenuItem openMenuItem;

    @FXML
    private MenuItem saveAsMenuItem;

    @FXML
    private MenuItem closeMenuItem;

    @FXML
    private MenuItem extractTextMenuItem;

    @FXML
    private MenuItem undoMenuItem;

    @FXML
    private MenuItem redoMenuItem;

    @FXML
    private MenuItem addBoxAnnotationMenuItem;

    @FXML
    private MenuItem addEllipseAnnotationMenuItem;

    @FXML
    private MenuItem addTextAnnotationMenuItem;

    @FXML
    private MenuItem deleteAnnotationMenuItem;

    private AIRViewerModel model;

    private ImageView currentPageImageView;

    private Group pageImageGroup;

    private float dragStartX;
    private float dragStartFlippedY;
    private float cumulativeDragDeltaX;
    private float cumulativeDragDeltaFlippedY;
    private boolean isDragging;

    /**
     *
     * @param startPath
     * @return
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
     *
     */
    private void synchronizeSelectionKnobs() {
        if (null != model && null != currentPageImageView && null != pageImageGroup) {
            List<Rectangle> selectedAreas = model.getSelectedAreas();
            ArrayList<Node> victims = new ArrayList<>(pageImageGroup.getChildren());

            // Delete everything in the group that isn't currentPageImageView
            victims.stream().filter((n) -> (n != currentPageImageView)).forEach((n) -> {
                pageImageGroup.getChildren().remove(n);
            });

            // Add knobs to thegroup to indicate selection
            selectedAreas.stream().map((r) -> {
                Circle knobA = new Circle(r.getX(), (int) pageImageGroup.prefHeight(0) - r.getY(), 4);
                knobA.setStroke(Color.YELLOW);
                knobA.setStrokeWidth(2);
                pageImageGroup.getChildren().add(knobA);
                Circle knobB = new Circle(r.getX() + r.getWidth(), (int) pageImageGroup.prefHeight(0) - r.getY(), 4);
                knobB.setStroke(Color.YELLOW);
                knobB.setStrokeWidth(2);
                pageImageGroup.getChildren().add(knobB);
                Circle knobC = new Circle(r.getX() + r.getWidth(), (int) pageImageGroup.prefHeight(0) - (r.getY() + r.getHeight()), 4);
                knobC.setStroke(Color.YELLOW);
                knobC.setStrokeWidth(2);
                pageImageGroup.getChildren().add(knobC);
                Circle knobD = new Circle(r.getX(), (int) pageImageGroup.prefHeight(0) - (r.getY() + r.getHeight()), 4);
                return knobD;
            }).map((knobD) -> {
                knobD.setStroke(Color.YELLOW);
                return knobD;
            }).map((knobD) -> {
                knobD.setStrokeWidth(2);
                return knobD;
            }).forEach((knobD) -> {
                pageImageGroup.getChildren().add(knobD);
            });
            if (1 == model.getSelectionSize()) {
                Rectangle r = model.getSelectedAreas().get(0);
                TextField textEntry = new TextField(model.getSelectedContents().get(0));
                textEntry.setPrefWidth(r.getWidth());
                textEntry.setLayoutX(r.getX() - textEntry.getLayoutBounds().getMinY());
                textEntry.setLayoutY((pageImageGroup.prefHeight(0) - r.getY()) - textEntry.getLayoutBounds().getMinY());

                textEntry.setOnAction((ActionEvent event) -> {
                    System.out.println(textEntry.getText());
                    if (null != pagination) {
                        model.executeDocumentCommandWithNameAndArgs("ChangeSelectedAnnotationText",
                                new String[]{Integer.toString(pagination.getCurrentPageIndex()), textEntry.getText()});
                        refreshUserInterface();
                    }
                });

                pageImageGroup.getChildren().add(textEntry);
            }
        }

    }

    /**
     * This method is called any time the user interface needs to be refreshed
     * to match changes to the model.
     */
    private void refreshUserInterface() {
        assert pagination != null : "fx:id=\"pagination\" was not injected: check your FXML file 'simple.fxml'.";
        assert saveAsMenuItem != null : "fx:id=\"saveAsMenuItem\" was not injected: check your FXML file 'simple.fxml'.";
        assert extractTextMenuItem != null : "fx:id=\"extractTextMenuItem\" was not injected: check your FXML file 'simple.fxml'.";
        assert undoMenuItem != null : "fx:id=\"undoMenuItem\" was not injected: check your FXML file 'simple.fxml'.";
        assert redoMenuItem != null : "fx:id=\"redoMenuItem\" was not injected: check your FXML file 'simple.fxml'.";
        assert addBoxAnnotationMenuItem != null : "fx:id=\"addBoxAnnotationMenuItem\" was not injected: check your FXML file 'simple.fxml'.";
        assert addEllipseAnnotationMenuItem != null : "fx:id=\"addEllipseAnnotationMenuItem\" was not injected: check your FXML file 'simple.fxml'.";
        assert addTextAnnotationMenuItem != null : "fx:id=\"addTextAnnotationMenuItem\" was not injected: check your FXML file 'simple.fxml'.";
        assert deleteAnnotationMenuItem != null : "fx:id=\"deleteAnnotationMenuItem\" was not injected: check your FXML file 'simple.fxml'.";

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
            pagination.setPageCount(model.numPages());
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
            deleteAnnotationMenuItem.setDisable(0 >= model.getSelectionSize());

            if (null != currentPageImageView) {
                int pageIndex = pagination.getCurrentPageIndex();
                currentPageImageView.setImage(model.getImage(pageIndex));
            }
        }
        synchronizeSelectionKnobs();
    }

    /**
     *
     * @param anImage
     * @return
     */
    private Group makePageViewGroup(Image anImage) {
        if (null == pageImageGroup) {
            pageImageGroup = new Group();
            currentPageImageView = new ImageView();
            pageImageGroup.getChildren().add(currentPageImageView);

            pageImageGroup.setOnMousePressed((MouseEvent me) -> {
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

            pageImageGroup.setOnMouseDragged((MouseEvent me) -> {
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

            pageImageGroup.setOnMouseReleased((MouseEvent me) -> {
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

        return pageImageGroup;
    }

    /**
     * THismethod is called right after a model is loaded.
     *
     * @param aModel
     * @return
     */
    private AIRViewerModel reinitializeWithModel(AIRViewerModel aModel) {
        assert pagination != null : "fx:id=\"pagination\" was not injected: check your FXML file 'simple.fxml'.";
        assert openMenuItem != null : "fx:id=\"openMenuItem\" was not injected: check your FXML file 'simple.fxml'.";
        assert saveAsMenuItem != null : "fx:id=\"saveAsMenuItem\" was not injected: check your FXML file 'simple.fxml'.";
        assert closeMenuItem != null : "fx:id=\"closeMenuItem\" was not injected: check your FXML file 'simple.fxml'.";

        assert extractTextMenuItem != null : "fx:id=\"extractTextMenuItem\" was not injected: check your FXML file 'simple.fxml'.";
        assert undoMenuItem != null : "fx:id=\"undoMenuItem\" was not injected: check your FXML file 'simple.fxml'.";
        assert redoMenuItem != null : "fx:id=\"redoMenuItem\" was not injected: check your FXML file 'simple.fxml'.";
        assert addBoxAnnotationMenuItem != null : "fx:id=\"addBoxAnnotationMenuItem\" was not injected: check your FXML file 'simple.fxml'.";
        assert addEllipseAnnotationMenuItem != null : "fx:id=\"addEllipseAnnotationMenuItem\" was not injected: check your FXML file 'simple.fxml'.";
        assert addTextAnnotationMenuItem != null : "fx:id=\"addTextAnnotationMenuItem\" was not injected: check your FXML file 'simple.fxml'.";
        assert deleteAnnotationMenuItem != null : "fx:id=\"deleteAnnotationMenuItem\" was not injected: check your FXML file 'simple.fxml'.";

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

    public void promptUserToLoadModel() {
        reinitializeWithModel(promptLoadModel(DEFAULT_PATH));
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        assert pagination != null : "fx:id=\"pagination\" was not injected: check your FXML file 'simple.fxml'.";
        isDragging = false;
    }

}
