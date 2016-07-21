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
import java.io.IOException;
import java.rmi.server.UID;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;

/**
 * Instances of this class encapsulate a dictionary mapping String names to
 * Command class Factories as well as Undo and Redo stacks that enable unlimited
 * undo or redo of operations performed by instances of the Command class. There
 * are methods to execute commands based on their names and automatically manage
 * the Undo and Redo stacks.
 *
 * This class also keep track of which annotations are "selected", but the
 * meaning of "selected" is up to subclasses to define.
 *
 * @author Erik M. Buck (Reviewed by Ankita Saxena
 */
public abstract class AbstractDocumentCommandWrapper {

    /**
     * The map from Command names to Command factories. Register Command
     * factories by calling registerCommandFactoryWithName().
     */
    private static HashMap<String, MakeCommand> nameToFactoryMap
            = nameToFactoryMap = new HashMap<>();

    /**
     * This is the interface that Command factories stored in nameToFactoryMap
     * must implement.
     */
    public static interface MakeCommand {

        /**
         * Returns a new instance of a AbstractDocumentCommand subclass.
         *
         * @param anOwner The AbstractDocumentCommandWrapper that has the
         * wrappedDocument that the Command will operate upon
         * @param args A list of String arguments (if any) to the Command when
         * it executes.
         * @return
         */
        public AbstractDocumentCommand make(AbstractDocumentCommandWrapper anOwner, ArrayList<String> args);
    }

    /**
     * The PDF document that commands will use when the commands are executed.
     * The wrapped document will never be null.
     */
    protected final PDDocument wrappedDocument;

    /**
     * A stack that stores Commands that should be executed to "undo" previously
     * executed commands.
     */
    private final Stack<AbstractDocumentCommand> undoStack;

    /**
     * A stack that stores Commands that should be executed to "redo" previously
     * undone commands.
     */
    private final Stack<AbstractDocumentCommand> redoStack;

    /**
     * There are times when it makes sense to NOT push commands onto the undo
     * stack. For example, a drag operation that moves an annotation may take
     * place as many small movements on the way to a final drag destination. It
     * doesn't make sense to push dozens or hundreds of tiny move commands onto
     * the undo stack. Cluttering the stack will annoy users who probably
     * consider the entire drag sequence as a single operation. In other words,
     * when the user undoes a drag, the user probably wants to undo all of the
     * small movements on the way to the final drag destination. The solution is
     * to inhibit undo registration during the drag and then un-inhibit undo
     * registration at the end of the drag and execute a single undo-able
     * command that encodes the entire movement.
     */
    private boolean isUndoRegistrationInhibited;

    /**
     * A collection of "selected" annotations. Add annotations to the collection
     * by calling extendSelectionOnPageAtPoint(). See also deselectAll().
     */
    private final List<PDAnnotation> selectedAnnotations;

    /**
     * Constructor: Post condition, none of wrappedDocument, undoStack,
     * redoStack, or selectedAnnotations are null, and they never will be
     * because they are final.
     *
     * @param aDocument The PDFBox document that encapsulates PDF data.
     */
    protected AbstractDocumentCommandWrapper(PDDocument aDocument) {
        wrappedDocument = aDocument;
        undoStack = new Stack<>();
        redoStack = new Stack<>();
        selectedAnnotations = new ArrayList<>();
    }

    /**
     *
     * @return The number of pages in the wrapped PDF document.
     */
    public int getPageCount() {
        return wrappedDocument.getPages().getCount();
    }

    /**
     * Clear selectedAnnotations.
     */
    public void deselectAll() {
        getSelectedAnnotations().clear();
    }

    /**
     * This method finds the "last" (upper most) annotation that contains the
     * specified x and y coordinates and adds the annotation to
     * selectedAnnotations if it is not already in selectedAnnotations. As a
     * side effect, this method adds unique annotation names to any selected
     * annotation that does not already have a name. At all times, every
     * selected annotation must have a name - subclasses may rely on this
     * property.
     *
     * @param pageIndex Must be pageIndex >= 0 && pageIndex < getPageCount()
     * @param x An X coordinate in the PDF coordinate system
     * @param y A Y coordinate in the PDF coordinate system
     */
    public void extendSelectionOnPageAtPoint(int pageIndex, float x, float y) {
        assert 0 <= pageIndex && pageIndex < getPageCount();

        PDAnnotation candidate = getLastAnnotationOnPageAtPoint(pageIndex, x, y);
        if (null != candidate && !selectedAnnotations.contains(candidate)) {
            if (null == candidate.getAnnotationName()) {
                // Other programs neglect to provide a name, so provide one 
                // to uniquely identify selectde annotations.
                candidate.setAnnotationName(new UID().toString());
            }

            getSelectedAnnotations().add(candidate);
            //System.out.println("Selected: " + selectedAnnotations.toString());

        }
    }

    /**
     *
     * @return A list of Rectangles with the property that each rectangle
     * encloses one of the selected annotations if any. These rectangles may be
     * displayed to users or otherwise used to indicate areas of the page that
     * contain annotations.
     */
    public List<Rectangle> getSelectedAreas() {
        ArrayList<Rectangle> result = new ArrayList<>();

        getSelectedAnnotations().stream().map((a) -> a.getRectangle()).map((aBBox) -> new Rectangle((int) aBBox.getLowerLeftX(),
                (int) aBBox.getLowerLeftY(),
                (int) aBBox.getWidth(), (int) aBBox.getHeight())).forEach((intBBox) -> {
            result.add(intBBox);
        });
        return result;
    }

    /**
     * Each annotation has a "contents" that is a possibly null String, and this
     * method returns a list of all the contents of all the selected
     * annotations.
     *
     * @return
     */
    public List<String> getSelectedContents() {
        ArrayList<String> result = new ArrayList<>();

        getSelectedAnnotations().stream().forEach((a) -> {
            result.add(a.getContents());
        });
        return result;
    }

    /**
     *
     * @return The number of selected annotations.
     */
    public int getSelectionCount() {
        return getSelectedAnnotations().size();
    }

    /**
     *
     * @return The collection of selected annotations.
     */
    protected List<PDAnnotation> getSelectedAnnotations() {
        return selectedAnnotations;
    }

    /**
     * Set isUndoRegistrationInhibited: See isUndoRegistrationInhibited for more
     * information.
     *
     * @param aFlag true to prevent future pushes of Commands onto the undo
     * stack and false to enable future pushes.
     */
    void setIsUndoRegistrationInhibited(boolean aFlag) {
        isUndoRegistrationInhibited = aFlag;
    }

    /**
     *
     * @return false if the Undo stack is empty and true otherwise.
     */
    public boolean getCanUndo() {
        return 0 < undoStack.size();
    }

    /**
     *
     * @return false if the Redo stack is empty and true otherwise.
     */
    public boolean getCanRedo() {
        return 0 < redoStack.size();
    }

    /**
     *
     * @return A String describing what will happen if undo() is called e.g.
     * this string may be used as the title of an undo menu item.
     */
    public String getSuggestedUndoTitle() {
        if (0 < undoStack.size()) {
            return undoStack.lastElement().getUndoName();
        }

        return "";
    }

    /**
     *
     * @return A String describing what will happen if redo() is called e.g.
     * this string may be used as the title of an redo menu item.
     */
    public String getSuggestedRedoTitle() {
        if (0 < redoStack.size()) {
            return redoStack.lastElement().getUndoName();
        }

        return "";
    }

    /**
     * Call this method to register a Command factory with a String name so that
     * instances of the Command can be made based on the name of the command.
     *
     * @param command A Command factory. See the MakeCommand interface.
     * @param aName
     */
    protected static void registerCommandFactoryWithName(MakeCommand command, String aName) {
        nameToFactoryMap.put(aName, command);
    }

    /**
     * Call this method to execute the command and if not inhibited, push a
     * reciprocal command onto the Undo stack.
     *
     * @param command The Command to execute
     * @return true if the command produced a valid reciprocal Command (which
     * usually means the command executed successfully) and false otherwise.
     */
    private boolean executeDocumentCommand(AbstractDocumentCommand command) {
        boolean result = false;
        AbstractDocumentCommand reciprocal = command.execute();
        if (null != reciprocal) {
            reciprocal.setUndoName(command.getName());
            if (!isUndoRegistrationInhibited) {
                undoStack.push(reciprocal);
            }
            result = true;
        }

        return result;
    }

    /**
     * Call this method to create a Command using the Factory registered with
     * aName and execute the command. If not inhibited, this method has the side
     * effect of pushing a reciprocal command onto the Undo stack.
     *
     * @param aName The name of a Command factory registered via
     * registerCommandFactoryWithName()
     * @param args A list of String arguments to the command to be executed
     * @return true if the command produced a valid reciprocal Command (which
     * usually means the command executed successfully) and false otherwise.
     */
    public boolean executeDocumentCommandWithNameAndArgs(String aName, ArrayList<String> args) {
        boolean result = false;

        if (nameToFactoryMap.containsKey(aName)) {
            MakeCommand makeFunction = nameToFactoryMap.get(aName);
            try {
                AbstractDocumentCommand command = makeFunction.make(this, args);
                result = executeDocumentCommand(command);

            } catch (IllegalArgumentException ex) {
                Logger.getLogger(AbstractDocumentCommandWrapper.class
                        .getName()).log(Level.SEVERE, null, ex);
            }

        } else {
            System.err.println("Command does not exist: <" + aName + ">");
        }

        return result;
    }

    /**
     * Call this method to create a Command using the Factory registered with
     * aName and execute the command. If not inhibited, this method has the side
     * effect of pushing a reciprocal command onto the Undo stack.
     *
     * @param aName The name of a Command factory registered via
     * registerCommandFactoryWithName()
     * @param args An arry of String arguments to the command to be executed
     * @return true if the command produced a valid reciprocal Command (which
     * usually means the command executed successfully) and false otherwise.
     */
    public boolean executeDocumentCommandWithNameAndArgs(String aName, String[] args) {
        ArrayList<String> commandArgs = new ArrayList<>(Arrays.asList(args));
        return executeDocumentCommandWithNameAndArgs(aName, commandArgs);
    }

    /**
     * Pops the Command (if any) at the top of the Undo stack and executes that
     * Command. As a side effect, a reciprocal Command is pushed onto the Redo
     * stack.
     *
     * @return true if the executed command produced a valid reciprocal Command
     * (which usually means the command executed successfully) and false
     * otherwise.
     */
    public boolean undo() {
        boolean result = false;

        if (0 < undoStack.size()) {
            AbstractDocumentCommand command = undoStack.pop();
            AbstractDocumentCommand reciprocal = command.execute();
            if (null != reciprocal) {
                reciprocal.setUndoName(command.getUndoName());
                redoStack.push(reciprocal);
                result = true;
            }
        }

        return result;
    }

    /**
     * Pops the Command (if any) at the top of the Redo stack and executes that
     * Command. As a side effect, a reciprocal Command is pushed onto the Undo
     * stack.
     *
     * @return true if the executed command produced a valid reciprocal Command
     * (which usually means the command executed successfully) and false
     * otherwise.
     */
    public boolean redo() {
        boolean result = false;

        if (0 < redoStack.size()) {
            AbstractDocumentCommand command = redoStack.pop();
            AbstractDocumentCommand reciprocal = command.execute();
            if (null != reciprocal) {
                reciprocal.setUndoName(command.getUndoName());
                undoStack.push(reciprocal);
                result = true;
            }
        }

        return result;

    }

    /**
     * This method finds and returns the "last" (upper most) annotation that
     * contains the specified x and y coordinates
     *
     * @param pageIndex Must be pageIndex >= 0 && pageIndex < getPageCount()
     * @param x
     * An X coordinate in the PDF coordinate system
     * @param y A Y coordinate in the PDF coordinate system
     * @return The annotation (if any) that was found at {x,y} on the page with
     * pageIndex or null otherwise.
     */
    protected PDAnnotation getLastAnnotationOnPageAtPoint(int pageIndex, float x, float y) {
        assert 0 <= pageIndex && pageIndex < getPageCount();

        try {
            PDPage page = wrappedDocument.getPage(pageIndex);
            List<PDAnnotation> annotations = page.getAnnotations();

            return getLastAnnotationAtPoint(annotations, x, y);
        } catch (IOException ex) {
            Logger.getLogger(AbstractDocumentCommandWrapper.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    /**
     * This method finds and returns the "last" (upper most) annotation in
     * annotations that contains the specified x and y coordinates
     *
     * @param annotations A list of candidate annotations
     * @param x An X coordinate in the PDF coordinate system
     * @param y A Y coordinate in the PDF coordinate system
     * @return The annotation if any that was found at {x,y} on the page with
     * pageIndex
     */
    protected PDAnnotation getLastAnnotationAtPoint(List<PDAnnotation> annotations, float x, float y) {
        PDAnnotation result = null;

        for (int i = annotations.size() - 1; i >= 0; --i) {
            PDAnnotation candidate = annotations.get(i);
            if (!candidate.isHidden() && !candidate.isReadOnly()) {
                PDRectangle boundingBox = candidate.getRectangle();

                if (boundingBox.contains(x, y)) {
                    return candidate;

                }
            }
        }

        return result;
    }

    /**
     * This class defines the interface, variables, and common operations needed
     * to order to encapsulates commands that may be executed following the
     * Command Design Pattern. Concrete subclasses must implements execute()
     * When executed to perform operations specific to the concrete subclass and
     * return a reciprocal command to be executed if/when the undo is required.
     * If undo is inappropriate for some reason, concrete implementations of
     * execute() may return null.
     *
     * @author Buck, Saxena
     *
     */
    protected abstract class AbstractDocumentCommand {

        /**
         * The AbstractDocumentCommandWrapper that has the wrappedDocument that
         * the Command will operate upon.
         */
        protected final AbstractDocumentCommandWrapper owner;

        /**
         * A list of String arguments (if any) to the Command when it executes.
         */
        protected final ArrayList<String> arguments;

        /**
         * A list of annotations (if any) to which the Command will be applied.
         */
        protected final List<PDAnnotation> annotations;

        /**
         * A String that describes what will happen if the command is executed.
         * The undoName should be suitable for display in an Undo menu item or
         * similar user interface.
         */
        protected String undoName;

        /**
         * Constructor: Initialize with the specified owner and arguments.
         * annotations is set to null.
         *
         * @param anOwner The AbstractDocumentCommandWrapper that has the
         * wrappedDocument that the Command will operate upon
         * @param args A list of String arguments (if any) to the Command when
         * it executes.
         */
        protected AbstractDocumentCommand(AbstractDocumentCommandWrapper anOwner, ArrayList<String> args) {
            owner = anOwner;
            arguments = args;
            annotations = null;
            undoName = "";

            assert null != owner;
            assert null != arguments;
        }

        /**
         * Constructor: Initialize with the specified owner and annotations.
         * arguments is set to null.
         *
         * @param anOwner The AbstractDocumentCommandWrapper that has the
         * wrappedDocument that the Command will operate upon
         * @param someAnnotations A list of annotations (if any) to which the
         * Command will be applied.
         */
        protected AbstractDocumentCommand(AbstractDocumentCommandWrapper anOwner, List<PDAnnotation> someAnnotations) {
            owner = anOwner;
            arguments = null;
            annotations = someAnnotations;
            undoName = "";

            assert null != owner;
            assert null != annotations;
        }

        /**
         * Constructor: Initialize with the specified owner and annotations and
         * arguments.
         *
         * @param anOwner The AbstractDocumentCommandWrapper that has the
         * wrappedDocument that the Command will operate upon
         * @param someAnnotations A list of annotations (if any) to which the
         * Command will be applied.
         * @param args A list of String arguments (if any) to the Command when
         * it executes.
         */
        protected AbstractDocumentCommand(AbstractDocumentCommandWrapper anOwner, List<PDAnnotation> someAnnotations, ArrayList<String> args) {
            owner = anOwner;
            arguments = args;
            annotations = someAnnotations;
            undoName = "";

            assert null != owner;
            assert null != arguments;
            assert null != annotations;
        }

        /**
         * Set the String that describes what will happen if the command is
         * executed. The undoName should be suitable for display in an Undo menu
         * item or similar user interface.
         *
         * @param aName A description of the command
         */
        public void setUndoName(String aName) {
            undoName = aName;
        }

        /**
         * Get the String that describes what will happen if the command is
         * executed. The undoName should be suitable for display in an Undo menu
         * item or similar user interface.
         *
         * @return A description of the command
         */
        public String getUndoName() {
            if (null == undoName) {
                undoName = "";
            }
            return undoName;
        }

        /**
         *
         * @return If execute() succeeds, a Command that is the reciprocal of
         * the receiver is returned. Otherwise, null is returned.
         */
        public abstract AbstractDocumentCommand execute();

        /**
         *
         * @return The name of the command as it will appear in a user interface
         * for undo and redo operations e.g. "Undo Delete Annotation" where the
         * string after "Undo " is returned from getName().
         */
        public abstract String getName();

    }

    /**
     * A Command that calls its owner's undo() method when executed.
     */
    public class UndoDocumentCommand extends AbstractDocumentCommand {

        /**
         * Constructor: Initialize with the specified owner and arguments.
         * annotations is set to null.
         *
         * @param anOwner The AbstractDocumentCommandWrapper that has the
         * wrappedDocument that the Command will operate upon
         * @param args A list of String arguments (if any) to the Command when
         * it executes.
         */
        public UndoDocumentCommand(AbstractDocumentCommandWrapper anOwner, ArrayList<String> args) {
            super(anOwner, args);
        }

        /**
         *
         * @return If execute() succeeds, a Command that is the reciprocal of
         * the receiver is returned. Otherwise, null is returned.
         */
        @Override
        public AbstractDocumentCommand execute() {
            owner.undo();
            return null;
        }

        /**
         *
         * @return The name of the command as it will appear in a user interface
         * for undo and redo operations e.g. "Undo Delete Annotation" where the
         * string after "Undo " is returned from getName().
         */
        @Override
        public String getName() {
            return "Undo";
        }
    }

    /**
     * A Command that calls its owner's redo() method when executed.
     */
    public class RedoDocumentCommand extends AbstractDocumentCommand {

        /**
         * Constructor: Initialize with the specified owner and arguments.
         * annotations is set to null.
         *
         * @param anOwner The AbstractDocumentCommandWrapper that has the
         * wrappedDocument that the Command will operate upon
         * @param args A list of String arguments (if any) to the Command when
         * it executes.
         */
        public RedoDocumentCommand(AbstractDocumentCommandWrapper anOwner, ArrayList<String> args) {
            super(anOwner, args);
        }

        /**
         *
         * @return If execute() succeeds, a Command that is the reciprocal of
         * the receiver is returned. Otherwise, null is returned.
         */
        @Override
        public AbstractDocumentCommand execute() {
            owner.redo();
            return null;
        }

        /**
         *
         * @return The name of the command as it will appear in a user interface
         * for undo and redo operations e.g. "Undo Delete Annotation" where the
         * string after "Undo " is returned from getName().
         */
        @Override
        public String getName() {
            return "Undo";
        }
    }
}
