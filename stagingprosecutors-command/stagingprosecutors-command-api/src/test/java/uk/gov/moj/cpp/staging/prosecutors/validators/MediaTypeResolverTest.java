package uk.gov.moj.cpp.staging.prosecutors.validators;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.moj.cpp.staging.prosecutors.converter.MediaTypeResolver;

import org.junit.jupiter.api.Test;

public class MediaTypeResolverTest {

    private MediaTypeResolver mediaTypeResolver = new MediaTypeResolver();

    @Test
    public void shouldResolveMediaType() {
        final String mediaType = mediaTypeResolver.resolveMediaType("FISH, JONES and PACKET 45MD0000220 Temp Bundle for Dispatch test.pdf");
        assertThat(mediaType, is("application/pdf"));
    }

    @Test
    public void shouldResolveMediaTypeForUpperCase() {
        final String mediaType = mediaTypeResolver.resolveMediaType("sample.Docx");
        assertThat(mediaType, is("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
    }

    @Test
    public void shouldThrowInvalidFileFormatForNonExistingFileExtension() {
        assertThrows(BadRequestException.class, () -> mediaTypeResolver.resolveMediaType("sample"));
    }

    @Test
    public void shouldThrowInvalidFileFormatForNonExistingFileName() {
        assertThrows(BadRequestException.class, () -> mediaTypeResolver.resolveMediaType(""));
    }

    @Test
    public void shouldThrowInvalidFileFormatForUnSupportedFileFormat() {
        assertThrows(BadRequestException.class, () -> mediaTypeResolver.resolveMediaType("sample.unsupported"));
    }
}