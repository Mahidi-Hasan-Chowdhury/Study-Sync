package bd.edu.seu.studysync.utility;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
public class PdfTextExtractor {

    /**
     * Extracts text from a PDF file
     * @param pdfFile The PDF file to extract text from
     * @return Extracted text as String
     * @throws IOException if PDF reading fails
     */
    public String extractText(File pdfFile) throws IOException {
        // PDFBox 3.x uses Loader.loadPDF() instead of PDDocument.load()
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            // Clean up the text (remove excessive whitespace)
            return text.replaceAll("\\s+", " ").trim();
        }
    }
}