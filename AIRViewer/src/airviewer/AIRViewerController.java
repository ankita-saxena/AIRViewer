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
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Pagination;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 *
 * @author erik
 */
public class AIRViewerController implements Initializable {

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

    private AIRViewerModel promptLoadModel(String startPath) {

        AIRViewerModel loadedModel = null;
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open PDF File");
            fileChooser.setInitialFileName(startPath);
            Stage stage = (Stage) pagination.getScene().getWindow();
            File file = fileChooser.showOpenDialog(stage);
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

    private void synchronizeSelectionKnobs() {
        if (null != model && null != currentPageImageView && null != pageImageGroup) {
            List<Rectangle> selectedAreas = model.getSelectedAreas();
            ArrayList<Node> victims = new ArrayList<>(pageImageGroup.getChildren());
            
            // Delete everything in teh group that isn't currentPageImageView
            victims.stream().filter((n) -> (n != currentPageImageView)).forEach((n) -> {
                pageImageGroup.getChildren().remove(n);
            });
            
            // Add knobs to thegroup to indicate selection
            for (Rectangle r : selectedAreas) {
                Circle knobA = new Circle(r.getX(),  (int)pageImageGroup.prefHeight(0) - r.getY(), 4);
                knobA.setStroke(Color.YELLOW);
                knobA.setStrokeWidth(2);
                pageImageGroup.getChildren().add(knobA);
                Circle knobB = new Circle(r.getX() + r.getWidth(), (int)pageImageGroup.prefHeight(0) - r.getY(), 4);
                knobB.setStroke(Color.YELLOW);
                knobB.setStrokeWidth(2);
                pageImageGroup.getChildren().add(knobB);
                Circle knobC = new Circle(r.getX() + r.getWidth(),  (int)pageImageGroup.prefHeight(0) - (r.getY() + r.getHeight()), 4);
                knobC.setStroke(Color.YELLOW);
                knobC.setStrokeWidth(2);
                pageImageGroup.getChildren().add(knobC);
                Circle knobD = new Circle(r.getX(),  (int)pageImageGroup.prefHeight(0) - (r.getY() + r.getHeight()), 4);
                knobD.setStroke(Color.YELLOW);
                knobD.setStrokeWidth(2);
                pageImageGroup.getChildren().add(knobD);
            }
        }

    }

    private void refreshUserInterface() {
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

        if (null != model) {
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
                currentPageImageView.setOnMousePressed(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent me) {
                        float flippedY = (float) currentPageImageView.getBoundsInParent().getHeight() - (float) me.getY();
                        System.out.println("Mouse pressed X: " + me.getX()
                                + " Y: " + Float.toString(flippedY));

                        float xInPage = (float) me.getX();
                        float yInPage = flippedY;

                        if (null != model) {
                            int pageIndex = pagination.getCurrentPageIndex();
                            if (!me.isMetaDown() && !me.isShiftDown()) {
                                model.deselectAll();
                            }
                            model.extendSelectionOnPageAtPoint(pageIndex,
                                    xInPage, yInPage);
                            refreshUserInterface();
                        }
                    }
                });
            }

            synchronizeSelectionKnobs();

        } else {
            pagination.setPageCount(0);
            pagination.setPageFactory(index -> {
                if (null == pageImageGroup) {
                    pageImageGroup = new Group();
                }
                currentPageImageView = new ImageView();
                pageImageGroup.getChildren().clear();
                pageImageGroup.getChildren().add(currentPageImageView);
                return pageImageGroup;
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

        }
    }

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

        if (null != model) {
            Stage stage = AIRViewer.getPrimaryStage();
            assert null != stage;

            model.deselectAll();

            pagination.setPageCount(model.numPages());
            pagination.setPageFactory(index -> {
                if (null == pageImageGroup) {
                    pageImageGroup = new Group();
                }
                currentPageImageView = new ImageView(model.getImage(index));
                pageImageGroup.getChildren().clear();
                pageImageGroup.getChildren().add(currentPageImageView);
                model.deselectAll();
                refreshUserInterface();
                return pageImageGroup;
            });
            saveAsMenuItem.setOnAction((ActionEvent event) -> {
                FileChooser fileChooser = new FileChooser();
                FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("PDF files (*.pdf)", "*.pdf");
                fileChooser.getExtensionFilters().add(extFilter);
                File file = fileChooser.showSaveDialog((Stage) pagination.getScene().getWindow());
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
            addBoxAnnotationMenuItem.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent e) {
                    int pageIndex = pagination.getCurrentPageIndex();
                    model.executeDocumentCommandWithNameAndArgs("AddBoxAnnotation",
                            new String[]{Integer.toString(pageIndex), "36.0", "36.0", "72.0", "72.0"});
                    refreshUserInterface();
                }
            });
            addEllipseAnnotationMenuItem.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent e) {
                    int pageIndex = pagination.getCurrentPageIndex();
                    model.executeDocumentCommandWithNameAndArgs("AddCircleAnnotation",
                            new String[]{Integer.toString(pageIndex), "288", "576", "144.0", "72.0", "Sample Text!"});
                    refreshUserInterface();
                }
            });
            addTextAnnotationMenuItem.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent e) {
                    int pageIndex = pagination.getCurrentPageIndex();
                    model.executeDocumentCommandWithNameAndArgs("AddTextAnnotation",
                            new String[]{Integer.toString(pageIndex), "36", "576", "144.0", "19.0", "A Bit More Sample Text!"});
                    refreshUserInterface();
                }
            });
            deleteAnnotationMenuItem.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent e) {
                    int pageIndex = pagination.getCurrentPageIndex();
                    model.executeDocumentCommandWithNameAndArgs("DeleteSelectedAnnotation",
                            new String[]{Integer.toString(pageIndex)});
                    refreshUserInterface();
                }
            });
        }

        refreshUserInterface();

        return model;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        assert pagination != null : "fx:id=\"pagination\" was not injected: check your FXML file 'simple.fxml'.";

        Stage stage = AIRViewer.getPrimaryStage();
        stage.addEventHandler(WindowEvent.WINDOW_SHOWING, (WindowEvent window) -> {
            reinitializeWithModel(promptLoadModel(DEFAULT_PATH));
        });

    }

}
