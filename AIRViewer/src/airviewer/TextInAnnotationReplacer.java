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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdfwriter.ContentStreamWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
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
                            previous.setValue(newContents.getBytes(Charset.forName("UTF-8")));
                        } else if (op.getName().equals("TJ")) {
                            COSArray previous = (COSArray) tokens.get(j - 1);
                            for (int k = 0; k < previous.size(); k++) {
                                Object arrElement = previous.getObject(k);
                                if (arrElement instanceof COSString) {
                                    COSString cosString = (COSString) arrElement;
                                    cosString.setValue(newContents.getBytes(Charset.forName("UTF-8")));
                                }
                            }
                        }
                    }
                }

                try (OutputStream out = annotationAppearanceStream.getStream().createOutputStream()) {
                    ContentStreamWriter tokenWriter = new ContentStreamWriter(out);
                    tokenWriter.writeTokens(tokens);
                }
                
                anAnnotation.getAppearance().setNormalAppearance(annotationAppearanceStream);
            } catch (IOException ex) {
                Logger.getLogger(TextInAnnotationReplacer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
