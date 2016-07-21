/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package airviewer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 *
 * @author erik
 */
public class AIRViewer extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        Parent root = FXMLLoader.load(getClass().getResource("AIRFXMLDocument.fxml"));
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    @Override
    public void stop() {
        System.out.println("Stage is closing");
        // Todo: Save file if document has been edited since last save
    }

    /**
     * This function executes the commands in the file with file system path
     * stored in args[0] and applies the commands to a PDF file with file system
     * path stored in args[1]. If either the path to the script cannot be read
     * or the path to the PDF file cannot be read, IOException is thrown. If the
     * PDF file is not a valid PDF file, error messages are reported to
     * System.err. Commands that are executed are echoed to System.out. Commands
     * that fail may or may not report errors to System.err depending on the
     * command.
     *
     * @param args A String array containing exactly two strings interpreted as
     * a file system path to a script file and a fille system path to a valid
     * PDF file.
     * @throws IOException
     */
    private static void runScript(String[] args) throws IOException {
        assert null != args && 2 == args.length;

        String regex = "\"([^\"]*)\"|([^\\s#]+)|([#]+.*)";

        Pattern pattern = Pattern.compile(regex);
        BufferedReader br = new BufferedReader(new FileReader(args[0]));

        DocumentCommandWrapper commandDocument = DocumentCommandWrapper.loadDosumentAtPath(args[1]);
        String line;
        while ((line = br.readLine()) != null) {
            System.out.println(line); // echo the input

            Matcher m = pattern.matcher(line);
            ArrayList<String> commandAndArgs = new ArrayList<>();
            while (m.find()) {
                if (m.group(1) != null) {
                    commandAndArgs.add(m.group(1));
                } else if(m.group(3) != null) {
                    // This is a comment: ignore it
                } else {
                    commandAndArgs.add(m.group(2));
                }
            }

            if (0 < commandAndArgs.size()) {
                String[] commandAndArgsArray = commandAndArgs.toArray(new String[commandAndArgs.size()]);
                commandDocument.executeDocumentCommandWithNameAndArgs(commandAndArgsArray[0],
                        Arrays.copyOfRange(commandAndArgsArray, 1, commandAndArgsArray.length));
            }
        }

    }

    /**
     * This function parses the command line arguments and invokes operations
     * based on the arguments. Calling this function with no arguments or
     * incorrect arguments results in the function outputting usage information
     * and then calls System.exit(-1). Otherwise, this function calls
     * System.exit(0).
     *
     * In the most general case, two command line arguments are provided:
     * \<pathToScript\> \<pathToPDF_File\>
     * pathToScript should be a file system path to a file containing ASCII text
     * specifying one command per line. A command is composed of space delimited
     * strings. The first string is the name of a command. All other strings on
     * the same line are arguments to the command. Strings within " " may
     * contain spaces and are still treated as a single string argument.
     * pathToPDF_File should be a file system path to a valid PDF file to which
     * commands in pathToScript will be applied.
     *
     * @param args ARguments provided on the command line.
     * @throws IOException
     */
    public static void commandLineMain(String[] args) {
        if (1 == args.length && ("-h".equals(args[0]) || "--help".equals(args[0]))) {
//            String names = AbstractDocumentCommandWrapper.getCommandNames();
//            System.err.println(names);

        } else if (2 == args.length) {
            try {
                runScript(args);
            } catch (IOException ex) {
                System.err.println("Unable to run script: <"+args[0]+">");
                Logger.getLogger(AIRViewer.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            System.err.println(
                    "<Usage: pathToScript pathToPDF_File>");
            System.err.println(
                    "\tApply the commands in pathToScript to pathToPDF_File. Commands may include, save, AddBoxAnnotation, Undo, Redo, etc.");
            System.err.println(
                    "\n\t- OR -\n");
            System.err.println(
                    "<Usage: -h>");
            System.err.println(
                    "\tOutput a list of annotation commands and descriptions.");
            System.err.println(
                    "\n\t- OR -\n");
            System.err.println(
                    "<Usage: -t pathToPDF_File>");
            System.err.println(
                    "\tAnnotate pathToPDF_File with bult-in test cases and save to a similar file path with a '~' appended.");
            System.exit(-1);
        }

        System.exit(0);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (0 != args.length) {
            commandLineMain(args);
        } else {
            launch(args);
        }
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }
}
