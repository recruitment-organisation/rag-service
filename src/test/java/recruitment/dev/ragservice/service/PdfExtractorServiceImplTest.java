package recruitment.dev.ragservice.service;

import org.junit.jupiter.api.Test;
import recruitment.dev.ragservice.exception.RagException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PdfExtractorServiceImplTest {

    @Test
    void shouldRejectEmptyPdfContent() {
        RagException exception = assertThrows(RagException.class,
                () -> new PdfExtractorServiceImpl().extractText(new byte[0]));

        assertEquals("PDF_EMPTY", exception.getErrorCode());
    }
}
