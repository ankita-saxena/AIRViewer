/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package airviewer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdfwriter.ContentStreamWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;

/**
 *
 * @author erik
 */
public class TextInAnnotationReplacer {

    static void replaceText(PDDocument document, PDAnnotation anAnnotation, String newContents) {

        if (null != anAnnotation.getAppearance()
                && null != anAnnotation.getAppearance().getNormalAppearance()) {
            try {
                PDAppearanceStream annotationAppearanceStream = anAnnotation.getAppearance().getNormalAppearance().getAppearanceStream();

                PDFStreamParser parser = new PDFStreamParser(annotationAppearanceStream);
                parser.parse();
                List<Object> tokens = parser.getTokens();
                for (int j = 0; j < tokens.size(); j++) {
                    Object next = tokens.get(j);
                    if (next instanceof Operator) {
                        Operator op = (Operator) next;
                        //Tj and TJ are the two operators that display strings in a PDF
                        if (op.getName().equals("Tj")) {
                            // Tj takes one operand and that is the string to display so lets update that operator
                            COSString previous = (COSString) tokens.get(j - 1);
                            previous.setValue(newContents.getBytes());
                        } else if (op.getName().equals("TJ")) {
                            COSArray previous = (COSArray) tokens.get(j - 1);
                            for (int k = 0; k < previous.size(); k++) {
                                Object arrElement = previous.getObject(k);
                                if (arrElement instanceof COSString) {
                                    COSString cosString = (COSString) arrElement;
                                    cosString.setValue(newContents.getBytes());
                                }
                            }
                        }
                    }
                }

                OutputStream out = annotationAppearanceStream.getStream().createOutputStream(); 
                ContentStreamWriter tokenWriter = new ContentStreamWriter(out);
                tokenWriter.writeTokens(tokens);
                out.close();
                
                anAnnotation.getAppearance().setNormalAppearance(annotationAppearanceStream);
            } catch (IOException ex) {
                Logger.getLogger(TextInAnnotationReplacer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
