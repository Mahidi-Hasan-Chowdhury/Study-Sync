package bd.edu.seu.studysync.service;

import bd.edu.seu.studysync.utility.PdfTextExtractor;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final PdfTextExtractor pdfTextExtractor;
    private static final String UPLOAD_DIR = "uploads/";

    public String saveFile(MultipartFile file) throws IOException {
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(UPLOAD_DIR + fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return fileName;
    }

    public String extractText(String fileName) throws IOException {
        File file = new File(UPLOAD_DIR + fileName);
        String lowerCaseName = fileName.toLowerCase();

        if (lowerCaseName.endsWith(".pdf")) {
            return pdfTextExtractor.extractText(file);
        } else if (lowerCaseName.endsWith(".docx")) {
            return extractTextFromDocx(file);
        } else if (lowerCaseName.endsWith(".pptx")) {
            return extractTextFromPptx(file);
        } else {
            throw new IllegalArgumentException("Unsupported file type");
        }
    }

    private String extractTextFromDocx(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String extractTextFromPptx(File file) throws IOException {
        StringBuilder text = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file);
             XMLSlideShow ppt = new XMLSlideShow(fis)) {
            for (XSLFSlide slide : ppt.getSlides()) {
                // Get title
                String title = slide.getTitle();
                if (title != null) {
                    text.append(title).append("\n");
                }
                // Get content
                for (XSLFTextShape shape : slide.getPlaceholders()) {
                     text.append(shape.getText()).append("\n");
                }
                // Also check other shapes that might contain text
                 for (var shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape) {
                         text.append(((XSLFTextShape) shape).getText()).append("\n");
                    }
                }
            }
        }
        return text.toString();
    }
}
