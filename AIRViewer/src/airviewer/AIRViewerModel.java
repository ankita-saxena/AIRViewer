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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import static java.lang.Float.parseFloat;
import java.nio.file.Path;
import java.rmi.server.UID;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.rendering.PDFRenderer;
import static java.lang.Integer.parseInt;

/**
 * This class encapsulates the Model in the Model-View-Controller Design Pattern
 * and is primarily concerned with PDF document loading, rendering, editing, and
 * saving. The Model works even when the View and Controller subsystems have not
 * been created e.g. when the application is being used via a command line
 * script. As an implementation detail, the Open Source PDFBox library is used,
 * but no dependencies on any particular PDF support library should leak out of
 * the Model subsystem.
 *
 * Most of the functionality of this class is inherited from
 * DocumentCommandWrapper. This class adds methods like save() that are
 * specifically needed by the Controller subsystem but not already present in
 * DocumentCommandWrapper or only available as protected methods of
 * DocumentCommandWrapper. This class also defines and registers Command
 * subclasses to support moving, changing, and deleting selected annotations as
 * opposed to the inherited commands that do not use selection as a criteria.
 *
 * @author Erik M. Buck (Reviewed by Ankita Saxena
 */
public class AIRViewerModel extends DocumentCommandWrapper {

    /**
     * The object used to report errors other than "expected" errors like
     * incorrect strings that may have been input by users or provided by
     * scripts.
     */
    private static final Logger LOGGER = Logger.getLogger(AIRViewerModel.class.getName());

    /**
     * The PDFBox renderer used to convert individual PDF "pages" into images.
     */
    private final PDFRenderer renderer;

    /**
     * Constructor: Loads the PDF document at the path (file system path).
     *
     * @param path A file system path to a PDF file.
     * @throws IOException If the PDF file cannot be read or does not contain
     * valid PDF data.
     */
    AIRViewerModel(Path path) throws IOException {
        super(PDDocument.load(path.toFile()), "");
        renderer = new PDFRenderer(wrappedDocument);
        AbstractDocumentCommandWrapper.registerCommandFactoryWithName((AbstractDocumentCommandWrapper owner, ArrayList<String> args) -> new MoveSelectedAnnotationDocumentCommand(owner, args), "MoveSelectedAnnotation");
        AbstractDocumentCommandWrapper.registerCommandFactoryWithName((AbstractDocumentCommandWrapper owner, ArrayList<String> args) -> new DeleteSelectedAnnotationDocumentCommand(owner, args), "DeleteSelectedAnnotation");
        AbstractDocumentCommandWrapper.registerCommandFactoryWithName((AbstractDocumentCommandWrapper owner, ArrayList<String> args) -> new ChangeSelectedTextAnnotationDocumentCommand(owner, args), "ChangeSelectedAnnotationText");
    }

    /**
     *
     * @param pageIndex Must be pageIndex >= 0 && pageIndex < numPages() 
     * @return An image produced by rendering the PDF page specified by pageIndex. The page index
     * is an index starting with zero in an array of "pages" obtained from the
     * loaded PDf document. Returns null if no such image can be produced.
     *
     */
    public Image getImage(int pageIndex) {
        assert pageIndex >= 0 && pageIndex < getPageCount();

        Image result = null;

        try {
            BufferedImage pageImage = renderer.renderImage(pageIndex);
            result = SwingFXUtils.toFXImage(pageImage, null);
        } catch (IOException ex) {
            throw new UncheckedIOException(
                    "Unable to render the page at index:<" + Integer.toString(pageIndex) + ">", ex);
        }

        return result;
    }

    /**
     * Save the loaded PDF document (if any) by writing all of its content and
     * annotations as PDF data into file. This operation replaces the entire
     * content of file. Both regular PDF content streams and PDF annotation
     * streams are stored.
     *
     * @param file The file into which PDF data is written.
     */
    public void save(File file) {
        try {
            wrappedDocument.save(file);
        } catch (IOException ex) {
            Logger.getLogger(AIRViewerModel.class.getName()).log(Level.SEVERE,
                    "Unable to save PDF data.", ex);
        }
    }

    /**
     * Some PDF editors (and hand written files) produce unnamed annotations,
     * but the Model relies on annotations having unique names. Call this method
     * to ensure that all annotations on the specified page in wrappedDocument
     * have unique names regardless of the presence or absence of pre-existing
     * names.
     *
     * @param pageIndex pageIndex >= 0 && pageIndex < numPages()
     * @return A list of uniquely named annotations.
     */
    private List<PDAnnotation> getAllSanitizedAnnotationsOnPage(int pageIndex) {
        List<PDAnnotation> result = null;

        try {
            PDPage page = wrappedDocument.getPage(pageIndex);
            result = page.getAnnotations();
            for (PDAnnotation a : result) {
                // Other programs neglect to provide a name, so provide one
                // to uniquely identify selectde annotations.
                a.setAnnotationName(new UID().toString());

            }
        } catch (IOException ex) {
            Logger.getLogger(AbstractDocumentCommandWrapper.class.getName()).log(Level.SEVERE, null, ex);
        }

        return result;
    }

    /**
     * A Command to change the text "Contents" of an existing annotation.
     */
    public class ChangeSelectedTextAnnotationDocumentCommand extends AbstractDocumentCommand {

        /**
         *
         * @param anOwner
         * @param args
         */
        public ChangeSelectedTextAnnotationDocumentCommand(AbstractDocumentCommandWrapper anOwner, ArrayList<String> args) {
            super(anOwner, args);
            assert 2 == args.size();
        }

        /**
         *
         * @param anOwner
         * @param annotations
         * @param args
         */
        public ChangeSelectedTextAnnotationDocumentCommand(AbstractDocumentCommandWrapper anOwner, List<PDAnnotation> annotations, ArrayList<String> args) {
            super(anOwner, annotations, args);
            assert 1 == annotations.size();
            assert 2 == args.size();
        }

        /**
         *
         * @return If execute() succeeds, a Command that is the reciprocal of
         * the receiver is returned. Otherwise, null is returned.
         */
        @Override
        public AbstractDocumentCommand execute() {
            assert null != owner;
            assert null != arguments;

            AbstractDocumentCommand result = null;

            if (2 == arguments.size()) {
                List<PDAnnotation> candidates;

                if (null != annotations && 0 < annotations.size()) {
                    // In this case, the annotations to change are in annotations.
                    // We are probably undoing or redoing
                    candidates = annotations;
                } else {
                    // We should the change the selected annotations
                    candidates = owner.getSelectedAnnotations();
                }

                if (0 < candidates.size()) {

                    //int pageNumber = parseInt(arguments.get(0));
                    ArrayList<String> newArgs = new ArrayList<>(arguments);
                    newArgs.set(1, candidates.get(0).getContents());
                    result = new ChangeSelectedTextAnnotationDocumentCommand(owner, new ArrayList<>(candidates), newArgs);

                    candidates.stream().map((a) -> {
                        a.setContents(arguments.get(1));
                        return a;
                    }).forEach((a) -> {
                        TextInAnnotationReplacer.replaceText(owner.wrappedDocument, a, arguments.get(1));
                    });
                }
            } else {
                System.err.printf("<%s> Expected 2 arguments but received %d.%n",
                        getName(), arguments.size());

            }
            return result;
        }

        /**
         *
         * @return The name of the command as it will appear in a user interface
         * for undo and redo operations e.g. "Undo Delete Annotation" where the
         * string after "Undo " is returned from getName().
         */
        @Override
        public String getName() {
            return "Change Annotation Text";
        }

    }

    /**
     * A Command to move the selected annotations (if any).
     */
    public class MoveSelectedAnnotationDocumentCommand extends AbstractDocumentCommand {

        /**
         *
         * @param anOwner
         * @param args
         */
        public MoveSelectedAnnotationDocumentCommand(AbstractDocumentCommandWrapper anOwner, ArrayList<String> args) {
            super(anOwner, args);
            assert 3 == args.size();
        }

        /**
         *
         * @param anOwner
         * @param annotations
         * @param args
         */
        public MoveSelectedAnnotationDocumentCommand(AbstractDocumentCommandWrapper anOwner, List<PDAnnotation> annotations, ArrayList<String> args) {
            super(anOwner, annotations, args);
            assert 1 == annotations.size();
            assert 3 == args.size();
        }

        /**
         *
         * @return If execute() succeeds, a Command that is the reciprocal of
         * the receiver is returned. Otherwise, null is returned.
         */
        @Override
        public AbstractDocumentCommand execute() {
            assert null != owner;
            assert 3 == arguments.size();

            AbstractDocumentCommand result = null;
            List<PDAnnotation> candidates;

            if (null != annotations && 0 < annotations.size()) {
                // In this case, the annotations to move are in annotations.
                // We are probably undoing or redoing
                candidates = annotations;
            } else {
                // We should the move the selected annotations
                candidates = owner.getSelectedAnnotations();
            }

            if (0 < candidates.size()) {

                 float dx = parseFloat(arguments.get(1));
                float dy = parseFloat(arguments.get(2));

                ArrayList<String> newArgs = new ArrayList<>(arguments);
                newArgs.set(1, Float.toString(-dx));
                newArgs.set(2, Float.toString(-dy));
                result = new MoveSelectedAnnotationDocumentCommand(owner, new ArrayList<>(candidates), newArgs);

                candidates.stream().forEach((a) -> {
                    PDRectangle position = a.getRectangle();
                    position.setLowerLeftX(position.getLowerLeftX() + dx);
                    position.setLowerLeftY(position.getLowerLeftY() + dy);
                    position.setUpperRightX(position.getUpperRightX() + dx);
                    position.setUpperRightY(position.getUpperRightY() + dy);
                    a.setRectangle(position);
                });
            }

            return result;
        }

        /**
         *
         * @return The name of the command as it will appear in a user interface
         * for undo and redo operations e.g. "Undo Delete Annotation" where the
         * string after "Undo " is returned from getName().
         */
        @Override
        public String getName() {
            return "Move Annotation";
        }

    }

    /**
     * Instances of this class encapsulate commands to annotate PDF documents
     * encapsulated by deleting an existing annotation.
     */
    public class DeleteSelectedAnnotationDocumentCommand extends AbstractDocumentCommand {

        /**
         *
         * @param anOwner
         * @param args
         */
        public DeleteSelectedAnnotationDocumentCommand(AbstractDocumentCommandWrapper anOwner, ArrayList<String> args) {
            super(anOwner, args);
        }

        /**
         *
         * @return If execute() succeeds, a Command that is the reciprocal of
         * the receiver is returned. Otherwise, null is returned.
         */
        @Override
        public AbstractDocumentCommand execute() {
            List<PDAnnotation> oldAnnotations;
            List<PDAnnotation> selectedAnnotations = owner.getSelectedAnnotations();
            AbstractDocumentCommand result = null;

            if (0 < selectedAnnotations.size()) {
                assert 0 < arguments.size();

                try {
                    int pageNumber = parseInt(arguments.get(0));
                    PDPage page = owner.wrappedDocument.getPage(pageNumber);
                    oldAnnotations = getAllSanitizedAnnotationsOnPage(pageNumber);

                    if (null != oldAnnotations) {
                        result = new ReplaceAnnotationDocumentCommand(owner, new ArrayList<>(oldAnnotations), arguments);
                        selectedAnnotations.stream().map((a) -> {
                            List<PDAnnotation> itemsToRemove = new ArrayList<>();
                            oldAnnotations.stream().map((p) -> {
                                assert null != p && null != p.getAnnotationName();
                                return p;
                            }).forEach((p) -> {
                                assert null != a && null != a.getAnnotationName();
                                if (p.getAnnotationName().equals(a.getAnnotationName())) {
                                    itemsToRemove.add(p);
                                }
                            });
                            return itemsToRemove;
                        }).forEach((itemsToRemove) -> {
                            itemsToRemove.stream().forEach((pa) -> {
                                oldAnnotations.remove(pa);
                            });
                        });
                        page.setAnnotations(oldAnnotations);
                    }
                    owner.deselectAll();
                } catch (NumberFormatException | NullPointerException ex) {
                    System.err.println("Non number encountered where floating point number expected.");
                    Logger.getLogger(DocumentCommandWrapper.AddBoxAnnotationDocumentCommand.class.getName()).log(Level.SEVERE, null, ex);
                    result = null;
                }
            }

            return result;
        }

        /**
         *
         * @return The name of the command as it will appear in a user interface
         * for undo and redo operations e.g. "Undo Delete Annotation" where the
         * string after "Undo " is returned from getName().
         */
        @Override
        public String getName() {
            return "Delete Annotation";
        }
    }
}
