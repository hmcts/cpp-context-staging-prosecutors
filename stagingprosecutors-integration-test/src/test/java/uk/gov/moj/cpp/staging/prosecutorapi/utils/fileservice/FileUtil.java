package uk.gov.moj.cpp.staging.prosecutorapi.utils.fileservice;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.google.common.io.Resources;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for reading json response from a file.
 */
public class FileUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);

    public static String resourceToString(final String path, final Object... placeholders) {
        try (final InputStream systemResourceAsStream = getSystemResourceAsStream(path)) {
            return format(IOUtils.toString(systemResourceAsStream), placeholders);
        } catch (final IOException e) {
            LOGGER.error("Error consuming file from location {}", path, e);
            fail("Error consuming file from location " + path);
            throw new UncheckedIOException(e);
        }
    }

    public static byte[] getDocumentBytesFromFile(String filepath) {
        byte[] documentBytes;
        try {
            documentBytes = Files.readAllBytes(Paths.get(ClassLoader.getSystemResource(filepath).toURI()));
        } catch (Exception e) {
            LOGGER.error("Error reading file from location {}", filepath, e);
            throw new RuntimeException("Error reading file from location " + filepath);
        }

        return documentBytes;
    }

    public static String getStringFromResource(final String path) {
        String request = null;
        try {
            request = Resources.toString(Resources.getResource(path), Charset.defaultCharset());
        } catch (final Exception e) {
            fail("Error consuming file from location " + path);
        }
        return request;
    }

    public static JsonObject jsonFromString(final String jsonObjectStr) {
        JsonReader jsonReader = JsonObjects.createReader(new StringReader(jsonObjectStr));
        JsonObject object = jsonReader.readObject();
        jsonReader.close();

        return object;
    }

}
