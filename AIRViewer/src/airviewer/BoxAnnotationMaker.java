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
 * @author Erik M. Buck (Reviewed by Ankita Saxena
 */
package airviewer;

import java.awt.geom.AffineTransform;
import java.io.IOException;
import static java.lang.Float.parseFloat;
import static java.lang.Integer.parseInt;
import java.rmi.server.UID;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationSquareCircle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;
import org.apache.pdfbox.util.Matrix;

/**
 * This is a helper class for creating the "Appearance Stream" for Box
 * annotations. This code is necessary because of bug PDFBox-2019. The PDF
 * Specification Section 8.4.1 lists the Appearance Streams as "Optional" in the
 * Appearance Dictionary for each annotation. "(Optional; PDF 1.2) An appearance
 * dictionary specifying how the annotation is presented visually on the page
 * (see Section 8.4.4, “Appearance Streams” and also implementation note 79 in
 * Appendix H). Individual annotation handlers may ignore this entry and provide
 * their own appearances."
 *
 * @author Erik M. Buck (Reviewed by Ankita Saxena
 */
public class BoxAnnotationMaker {

    /**
     *
     * @param document
     * @param arguments
     * @return
     */
    public static List<PDAnnotation> make(PDDocument document,
            ArrayList<String> arguments) {
        assert null != arguments && arguments.size() == 5;
        assert null != document;

        List<PDAnnotation> result;

        try {
            int pageNumber = parseInt(arguments.get(0));
            float lowerLeftX = parseFloat(arguments.get(1));
            float lowerLeftY = parseFloat(arguments.get(2));
            float width = parseFloat(arguments.get(3));
            float height = parseFloat(arguments.get(4));
            String contents = "";
            PDFont font = PDType1Font.HELVETICA_OBLIQUE;
            float fontSize = 16; // Or whatever font size you want.
            //float textWidth = font.getStringWidth(contents) * fontSize / 1000.0f;
            float textHeight = 32;

            try {
                PDPage page = document.getPage(pageNumber);
                PDColor red = new PDColor(new float[]{1, 0, 0}, PDDeviceRGB.INSTANCE);
                PDBorderStyleDictionary borderThick = new PDBorderStyleDictionary();
                borderThick.setWidth(72 / 12);  // 12th inch
                PDRectangle position = new PDRectangle();
                position.setLowerLeftX(lowerLeftX);
                position.setLowerLeftY(lowerLeftY);
                position.setUpperRightX(lowerLeftX + width);
                position.setUpperRightY(lowerLeftY + height);

                PDAnnotationSquareCircle aSquare = new PDAnnotationSquareCircle(
                        PDAnnotationSquareCircle.SUB_TYPE_SQUARE);
                aSquare.setAnnotationName(new UID().toString());
                aSquare.setContents(contents);
                aSquare.setColor(red);  // Outline in red, not setting a fill
                PDColor fillColor = new PDColor(new float[]{.8f, .8f, .8f}, PDDeviceRGB.INSTANCE);
                aSquare.setInteriorColor(fillColor);
                aSquare.setBorderStyle(borderThick);
                aSquare.setRectangle(position);
                result = new ArrayList<>(page.getAnnotations()); // copy
                page.getAnnotations().add(aSquare);

                // The following lines are needed for PDFRenderer to render 
                // annotations. Preview and Acrobat don't seem to need these.
                if (null == aSquare.getAppearance()) {
                    aSquare.setAppearance(new PDAppearanceDictionary());
                    PDAppearanceStream annotationAppearanceStream = new PDAppearanceStream(document);
                    position.setLowerLeftX(lowerLeftX - borderThick.getWidth() * 0.5f);
                    position.setLowerLeftY(lowerLeftY - borderThick.getWidth() * 0.5f);
                    position.setUpperRightX(lowerLeftX + width + borderThick.getWidth() * 0.5f);
                    position.setUpperRightY(lowerLeftY + height + borderThick.getWidth() * 0.5f);
                    annotationAppearanceStream.setBBox(position);
                    annotationAppearanceStream.setMatrix(new AffineTransform());
                    annotationAppearanceStream.setResources(page.getResources());

                    try (PDPageContentStream appearanceContent = new PDPageContentStream(
                            document, annotationAppearanceStream)) {
                        Matrix transform = new Matrix();
                        appearanceContent.transform(transform);
                        appearanceContent.addRect(lowerLeftX, lowerLeftY, width, height);
                        appearanceContent.setLineWidth(borderThick.getWidth());
                        appearanceContent.setNonStrokingColor(fillColor);
                        appearanceContent.setStrokingColor(red);
                        appearanceContent.fillAndStroke();
                        appearanceContent.beginText();

                        // Center text vertically, left justified
                        appearanceContent.newLineAtOffset(lowerLeftX, lowerLeftY + height * 0.5f - fontSize * 0.5f);
                        appearanceContent.setFont(font, fontSize);
                        appearanceContent.setNonStrokingColor(red);
                        appearanceContent.showText(contents);
                        appearanceContent.endText();
                    }
                    aSquare.getAppearance().setNormalAppearance(annotationAppearanceStream);
                }
                //System.out.println(page.getAnnotations().toString());

            } catch (IOException ex) {
                Logger.getLogger(DocumentCommandWrapper.class.getName()).log(Level.SEVERE, null, ex);
                result = null;
            }
        } catch (NumberFormatException | NullPointerException ex) {
            System.err.println("\tNon number encountered where floating point number expected.");
            result = null;
        }

        return result;
    }
}
