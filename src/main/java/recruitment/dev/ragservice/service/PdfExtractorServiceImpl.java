package recruitment.dev.ragservice.service;


import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import recruitment.dev.ragservice.exception.RagException;

@Slf4j
@Service
public class PdfExtractorServiceImpl
        implements PdfExtractorService {

    @Override
    public String extractText(byte[] pdfContent) {

        if (pdfContent == null || pdfContent.length == 0) {
            throw new RagException("CV file is empty", "PDF_EMPTY", org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY);
        }

        try (
                PDDocument document =
                        Loader.loadPDF(pdfContent)
        ) {
            PDFTextStripper textStripper =
                    new PDFTextStripper();

            textStripper.setSortByPosition(true);

            String text =
                    textStripper.getText(document);

            if (text == null || text.isBlank()) {
                throw new RagException(
                        "The PDF contains no readable text. It appears to be scanned and requires OCR.",
                        "PDF_OCR_REQUIRED",
                        org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY
                );
            }

            return cleanText(text);

        } catch (RagException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("PDF extraction failed", exception);
            throw new RagException(
                    "The PDF cannot be read",
                    "PDF_UNREADABLE",
                    org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
                    exception
            );
        }
    }

    private String cleanText(String text) {
        return text
                .replace("\u0000", "")
                .replace('\u00A0', ' ')
                .replaceAll("[\\t\\r]+", " ")
                .replaceAll(" {2,}", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}
