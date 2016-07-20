/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package airviewer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.rendering.PDFRenderer;

/**
 *
 * @author erik
 */
public class AIRViewerModel extends DocumentCommandWrapper {

    private static final Logger logger = Logger.getLogger(AIRViewerModel.class.getName());

    private PDFRenderer renderer;

    AIRViewerModel(Path path) throws IOException {
        super(PDDocument.load(path.toFile()), "");
        renderer = new PDFRenderer(wrappedDocument);
    }

    int numPages() {
        return wrappedDocument.getPages().getCount();
    }

    Image getImage(int pageNumber) {
        BufferedImage pageImage;
        try {
            pageImage = renderer.renderImage(pageNumber);
        } catch (IOException ex) {
            throw new UncheckedIOException("AIRViewer throws IOException", ex);
        }
        return SwingFXUtils.toFXImage(pageImage, null);
    }

    public void save(File file) {
        try {
            wrappedDocument.save(file);
        } catch (IOException ex) {
            Logger.getLogger(AIRViewerModel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
