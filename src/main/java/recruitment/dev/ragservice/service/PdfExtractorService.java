package recruitment.dev.ragservice.service;

public interface PdfExtractorService {
    String extractText(byte[] pdfContent);
}
