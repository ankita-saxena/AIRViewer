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
package airviewer;

import java.io.File;
import java.io.IOException;
import static java.lang.Float.parseFloat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.apache.pdfbox.text.PDFTextStripper;
import static java.lang.Integer.parseInt;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author erik
 */
public class DocumentCommandWrapper extends AbstractDocumentCommandWrapper {

    /**
     *
     */
    private String path;

    /**
     *
     * @param aDocument
     * @param aPath
     */
    public DocumentCommandWrapper(PDDocument aDocument, String aPath) {
        super(aDocument);

        assert null != aPath;
        path = aPath;

        AbstractDocumentCommandWrapper.registerCommandClassWithName(new makeCommand() {
            @Override
            public AbstractDocumentCommand make(AbstractDocumentCommandWrapper owner, ArrayList<String> args) {
                return new SaveDocumentCommand(owner, args);
            }
        }, "Save");
        AbstractDocumentCommandWrapper.registerCommandClassWithName(new makeCommand() {
            @Override
            public AbstractDocumentCommand make(AbstractDocumentCommandWrapper owner, ArrayList<String> args) {
                return new SaveTextDocumentCommand(owner, args);
            }
        }, "SaveText");
        AbstractDocumentCommandWrapper.registerCommandClassWithName(new makeCommand() {
            @Override
            public AbstractDocumentCommand make(AbstractDocumentCommandWrapper owner, ArrayList<String> args) {
                return new AddBoxAnnotationDocumentCommand(owner, args);
            }
        }, "AddBoxAnnotation");
        AbstractDocumentCommandWrapper.registerCommandClassWithName(new makeCommand() {
            @Override
            public AbstractDocumentCommand make(AbstractDocumentCommandWrapper owner, ArrayList<String> args) {
                return new AddCircleAnnotationDocumentCommand(owner, args);
            }
        }, "AddCircleAnnotation");
        AbstractDocumentCommandWrapper.registerCommandClassWithName(new makeCommand() {
            @Override
            public AbstractDocumentCommand make(AbstractDocumentCommandWrapper owner, ArrayList<String> args) {
                return new AddTextAnnotationDocumentCommand(owner, args);
            }
        }, "AddTextAnnotation");
        AbstractDocumentCommandWrapper.registerCommandClassWithName(new makeCommand() {
            @Override
            public AbstractDocumentCommand make(AbstractDocumentCommandWrapper owner, ArrayList<String> args) {
                return new MoveAnnotationDocumentCommand(owner, args);
            }
        }, "MoveAnnotation");
        AbstractDocumentCommandWrapper.registerCommandClassWithName(new makeCommand() {
            @Override
            public AbstractDocumentCommand make(AbstractDocumentCommandWrapper owner, ArrayList<String> args) {
                return new DeleteAnnotationDocumentCommand(owner, args);
            }
        }, "DeleteAnnotation");
        AbstractDocumentCommandWrapper.registerCommandClassWithName(new makeCommand() {
            @Override
            public AbstractDocumentCommand make(AbstractDocumentCommandWrapper owner, ArrayList<String> args) {
                return new DeleteSelectedAnnotationDocumentCommand(owner, args);
            }
        }, "DeleteSelectedAnnotation");
    }

    /**
     * Reads information from the valid PDF file if any at aPath and returns a
     * DocumentCommandWrapper instance ready to add or delete annotations to the
     * loaded information. Trace: REQ-041000, REQ-041060
     *
     * @param aPath A valid file system path to a valid PDF document.
     * @return An instance of DocumentCommandWrapper configured to apply
     * Commands to the document at aPath.
     * @throws IOException
     */
    /*@   requires aPath is valid 
      @   assignable \nothing;
      @   ensures wrappedDocument.nonAnnotationContent == \old(<wrappedDocument>).nonAnnotationContent
      @   ensures wrappedDocument.annotationContent ==  \old(<wrappedDocument>).annotationContent
      @   ensures [file at path].nonAnnotationContent == \old(<[file at path]>).nonAnnotationContent
      @   ensures [file at path].annotationContent == \old(<[file at path]>).annotationContent
     */
    public static DocumentCommandWrapper loadDosumentAtPath(String aPath) throws IOException {
        DocumentCommandWrapper result = null;
        PDDocument document = PDDocument.load(new File(aPath));

        if (null != document) {
            result = new DocumentCommandWrapper(document, aPath);
        }

        return result;
    }

    /**
     * Save the PDF information including annotations encapsulated by the
     * receiver to a file at the specified path. The saved information contains
     * the same annotation and non-annotation information as the file (if any)
     * from which information was loaded to initialize the receiver with the
     * following exceptions: - Annotations added via the receiver since
     * information was loaded are saved in the specified file. - Annotations
     * deleted via the receiver since information was loaded are not saved in
     * the specified file. - Annotations whose attributes have been changed via
     * the receiver since information was loaded are save with changed
     * attributes in the specified file. Trace: REQ-041000, REQ-041050
     *
     * @param aPath A valid complete file system path including the name of the
     * document to save. The path must be writable in a file system with
     * sufficient available storage capacity to store the information
     * encapsulated by the receiver.
     * @throws java.io.IOException
     */
    /*@   requires aPath is valid 
      @   assignable \nothing;
      @   [replaces any pre-existing file at aPath]
      @   ensures wrappedDocument.nonAnnotationContent == \old(<wrappedDocument>).nonAnnotationContent
      @   ensures wrappedDocument.annotationContent ==  \old(<wrappedDocument>).annotationContent
      @   ensures [file at path].nonAnnotationContent == wrappedDocument.nonAnnotationContent
      @   ensures [file at path].annotationContent == wrappedDocument.annotationContent
     */
    public void save(String aPath) throws IOException {
        wrappedDocument.save(aPath.substring(0, aPath.lastIndexOf('.')) + "~.pdf");
    }

    /**
     * Instances of this class encapsulate commands to annotate PDF documents by
     * adding boxes. Boxes are rectangular regions of color on a page.
     */
    public class AddBoxAnnotationDocumentCommand extends AbstractDocumentCommand {

        /**
         *
         * @param anOwner
         * @param args
         */
        public AddBoxAnnotationDocumentCommand(AbstractDocumentCommandWrapper anOwner, ArrayList<String> args) {
            super(anOwner, args);
        }

        /**
         *
         * @return If execute() succeeds, a Command that is the reciprocal of
         * the receiver is returned. Otherwise, null is returned.
         */
        @Override
        public AbstractDocumentCommand execute() {
            AbstractDocumentCommand result = null;

            assert null != arguments && arguments.size() == 5;
            List<PDAnnotation> previousAnnotations = BoxAnnotationMaker.make(owner.wrappedDocument, arguments);
            result = new ReplaceAnnotationDocumentCommand(owner, previousAnnotations, arguments);

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
            return "Add Box Annotation";
        }
    }

    /**
     * Instances of this class encapsulate commands to annotate PDF documents;
     * by adding an ellipse containing a text string. Ellipses are inscribed in
     * a rectangular regions of a page, and the text string is centered in the
     * ellipse.
     */
    public class AddCircleAnnotationDocumentCommand extends AbstractDocumentCommand {

        /**
         *
         * @param anOwner
         * @param args
         */
        public AddCircleAnnotationDocumentCommand(AbstractDocumentCommandWrapper anOwner, ArrayList<String> args) {
            super(anOwner, args);
        }

        /**
         *
         * @return If execute() succeeds, a Command that is the reciprocal of
         * the receiver is returned. Otherwise, null is returned.
         */
        @Override
        public AbstractDocumentCommand execute() {
            AbstractDocumentCommand result = null;

            assert null != arguments && arguments.size() == 6;
            List<PDAnnotation> previousAnnotations = EllipseAnnotationMaker.make(owner.wrappedDocument, arguments);
            result = new ReplaceAnnotationDocumentCommand(owner, previousAnnotations, arguments);

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
            return "Add Ellipse Annotation";
        }
    }

    /**
     * Instances of this class encapsulate commands to annotate PDF documents by
     * adding text.
     */
    public class AddTextAnnotationDocumentCommand extends AbstractDocumentCommand {

        /**
         *
         * @param anOwner
         * @param args
         */
        public AddTextAnnotationDocumentCommand(AbstractDocumentCommandWrapper anOwner, ArrayList<String> args) {
            super(anOwner, args);
        }

        /**
         *
         * @return If execute() succeeds, a Command that is the reciprocal of
         * the receiver is returned. Otherwise, null is returned.
         */
        @Override
        public AbstractDocumentCommand execute() {
            AbstractDocumentCommand result = null;

            assert null != arguments && arguments.size() == 6;
            List<PDAnnotation> previousAnnotations = TextAnnotationMaker.make(owner.wrappedDocument, arguments);
            result = new ReplaceAnnotationDocumentCommand(owner, previousAnnotations, arguments);

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
            return "Add Text Annotation";
        }
    }

    /**
     * Instances of this class encapsulate commands to annotate PDF documents
     * encapsulated by deleting an existing annotation.
     */
    public class DeleteAnnotationDocumentCommand extends AbstractDocumentCommand {

        /**
         *
         * @param anOwner
         * @param args
         */
        public DeleteAnnotationDocumentCommand(AbstractDocumentCommandWrapper anOwner, ArrayList<String> args) {
            super(anOwner, args);
        }

        /**
         *
         * @return If execute() succeeds, a Command that is the reciprocal of
         * the receiver is returned. Otherwise, null is returned.
         */
        @Override
        public AbstractDocumentCommand execute() {
            assert 3 == arguments.size();

            AbstractDocumentCommand result = null;

            // We have to find the annotation to delete
            int pageNumber = parseInt(arguments.get(0));
            float x = parseFloat(arguments.get(1));
            float y = parseFloat(arguments.get(2));

            PDPage page = owner.wrappedDocument.getPage(pageNumber);

            try {
                List<PDAnnotation> oldAnnotations = page.getAnnotations();
                PDAnnotation victim = owner.getLastAnnotationAtPoint(oldAnnotations, x, y);
                if (null != victim) {
                    result = new ReplaceAnnotationDocumentCommand(owner, new ArrayList<>(oldAnnotations), arguments);
                    oldAnnotations.remove(victim);
                    page.setAnnotations(oldAnnotations);
                }
            } catch (IOException ex) {
                Logger.getLogger(DocumentCommandWrapper.class.getName()).log(Level.SEVERE, null, ex);
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

    /**
     *
     */
    public class MoveAnnotationDocumentCommand extends AbstractDocumentCommand {

        /**
         *
         * @param anOwner
         * @param args
         */
        public MoveAnnotationDocumentCommand(AbstractDocumentCommandWrapper anOwner, ArrayList<String> args) {
            super(anOwner, args);
            assert 5 == args.size();
        }

        /**
         *
         * @param anOwner
         * @param annotations
         * @param args
         */
        public MoveAnnotationDocumentCommand(AbstractDocumentCommandWrapper anOwner, List<PDAnnotation> annotations, ArrayList<String> args) {
            super(anOwner, annotations, args);
            assert 1 == annotations.size();
            assert 5 == args.size();
        }

        /**
         *
         * @return If execute() succeeds, a Command that is the reciprocal of
         * the receiver is returned. Otherwise, null is returned.
         */
        @Override
        public AbstractDocumentCommand execute() {
            AbstractDocumentCommand result = null;
            PDAnnotation candidate = null;

            assert 5 == arguments.size();

            int pageNumber = parseInt(arguments.get(0));
            float x = parseFloat(arguments.get(1));
            float y = parseFloat(arguments.get(2));
            float dx = parseFloat(arguments.get(3));
            float dy = parseFloat(arguments.get(4));

            if (null == annotations || 0 == annotations.size()) {
                try {
                    // We have to find the annotation to move
                    PDPage page = owner.wrappedDocument.getPage(pageNumber);
                    List<PDAnnotation> oldAnnotations = page.getAnnotations();
                    candidate = owner.getLastAnnotationAtPoint(oldAnnotations, x, y);
                } catch (IOException ex) {
                    Logger.getLogger(DocumentCommandWrapper.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                // We can just delete annotation
                candidate = annotations.get(0);
            }

            if (null != candidate) {

                ArrayList<String> newArgs = new ArrayList<>(arguments);
                newArgs.set(3, Float.toString(-dx));
                newArgs.set(4, Float.toString(-dy));
                ArrayList<PDAnnotation> candidateList = new ArrayList<>();
                candidateList.add(candidate);
                result = new MoveAnnotationDocumentCommand(owner, candidateList, newArgs);

                PDRectangle position = candidate.getRectangle();
                position.setLowerLeftX(position.getLowerLeftX() + dx);
                position.setLowerLeftY(position.getLowerLeftY() + dy);
                position.setUpperRightX(position.getUpperRightX() + dx);
                position.setUpperRightY(position.getUpperRightY() + dy);
                candidate.setRectangle(position);
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
     *
     */
    public class ReplaceAnnotationDocumentCommand extends AbstractDocumentCommand {

        /**
         *
         * @param anOwner
         * @param someAnnotations
         * @param args
         */
        protected ReplaceAnnotationDocumentCommand(AbstractDocumentCommandWrapper anOwner, List<PDAnnotation> someAnnotations, ArrayList<String> args) {
            super(anOwner, someAnnotations, args);
        }

        /**
         *
         * @return If execute() succeeds, a Command that is the reciprocal of
         * the receiver is returned. Otherwise, null is returned.
         */
        @Override
        public AbstractDocumentCommand execute() {
            assert null != annotations && 1 == annotations.size();
            assert null != arguments && 0 < arguments.size();

            AbstractDocumentCommand result = null;
            try {
                int pageNumber = parseInt(arguments.get(0));

                PDPage page = owner.wrappedDocument.getPage(pageNumber);
                result = new ReplaceAnnotationDocumentCommand(owner, new ArrayList<PDAnnotation>(page.getAnnotations()), arguments);
                page.setAnnotations(annotations);

            } catch (IOException ex) {
                Logger.getLogger(DocumentCommandWrapper.class.getName()).log(Level.SEVERE, null, ex);
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
            return "Undelete Annotation";
        }

    }

    /**
     *
     */
    public class SaveDocumentCommand extends AbstractDocumentCommand {

        /**
         *
         * @param anOwner
         * @param args
         */
        protected SaveDocumentCommand(AbstractDocumentCommandWrapper anOwner, ArrayList<String> args) {
            super(anOwner, args);
            assert 1 == args.size();
        }

        /**
         *
         * @return If execute() succeeds, a Command that is the reciprocal of
         * the receiver is returned. Otherwise, null is returned.
         */
        @Override
        public AbstractDocumentCommand execute() {
            assert null != arguments && 1 == arguments.size();

            try {
                owner.wrappedDocument.save(arguments.get(0));
            } catch (IOException ex) {
                Logger.getLogger(DocumentCommandWrapper.class.getName()).log(Level.SEVERE, null, ex);
            }

            return null; // Prevent "Save" item on undo stack
        }

        /**
         *
         * @return The name of the command as it will appear in a user interface
         * for undo and redo operations e.g. "Undo Delete Annotation" where the
         * string after "Undo " is returned from getName().
         */
        @Override
        public String getName() {
            return "Save Annotations and Non-annotations";
        }

    }

    /**
     *
     */
    public class SaveTextDocumentCommand extends AbstractDocumentCommand {

        /**
         *
         * @param anOwner
         * @param args
         */
        protected SaveTextDocumentCommand(AbstractDocumentCommandWrapper anOwner, ArrayList<String> args) {
            super(anOwner, args);
            assert 1 == args.size();
        }

        /**
         *
         * @return If execute() succeeds, a Command that is the reciprocal of
         * the receiver is returned. Otherwise, null is returned.
         */
        @Override
        public AbstractDocumentCommand execute() {
            assert null != arguments && 1 == arguments.size();

            try {
                PDFTextStripper stripper = new PDFTextStripper();
                String parsedText = stripper.getText(owner.wrappedDocument);
                Files.write(Paths.get(arguments.get(0)), parsedText.getBytes(), StandardOpenOption.CREATE);
            } catch (IOException ex) {
                Logger.getLogger(DocumentCommandWrapper.class.getName()).log(Level.SEVERE, null, ex);
            }

            return null; // Prevent "Save" item on undo stack
        }

        /**
         *
         * @return The name of the command as it will appear in a user interface
         * for undo and redo operations e.g. "Undo Delete Annotation" where the
         * string after "Undo " is returned from getName().
         */
        @Override
        public String getName() {
            return "Save Non-annotation Text";
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
         * @param someAnnotations
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
            assert 0 < arguments.size();

            int pageNumber = parseInt(arguments.get(0));
            List<PDAnnotation> oldAnnotations;
            List<PDAnnotation> selectedAnnotations = owner.getSelectedAnnotations();
            AbstractDocumentCommand result = null;

            if (0 < selectedAnnotations.size()) {
                try {
                    PDPage page = owner.wrappedDocument.getPage(pageNumber);
                    oldAnnotations = page.getAnnotations();
                    result = new ReplaceAnnotationDocumentCommand(owner, new ArrayList<>(oldAnnotations), arguments);
                    for (PDAnnotation a : selectedAnnotations) {
                        List<PDAnnotation> itemsToRemove = oldAnnotations.stream().filter(
                                p -> p.getAnnotationName().equals(a.getAnnotationName())).collect(Collectors.toCollection(ArrayList::new));

                        if (null != itemsToRemove) {
                            for (PDAnnotation pa : itemsToRemove) {
                                oldAnnotations.remove(pa);
                            }
                        }
                    }
                    page.setAnnotations(oldAnnotations);
                    owner.deselectAll();
                } catch (IOException ex) {
                    Logger.getLogger(DocumentCommandWrapper.class.getName()).log(Level.SEVERE, null, ex);
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
