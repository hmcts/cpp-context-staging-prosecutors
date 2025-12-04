package uk.gov.moj.cpp.staging.prosecutorapi.utils.fileservice;

import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.utils.test.FileServiceTestClient;
import uk.gov.justice.services.test.utils.common.host.TestHostProvider;
import uk.gov.justice.services.test.utils.core.jdbc.JdbcConnectionProvider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.UUID;

public class FileServiceClient {

    private static final FileServiceTestClient client = new FileServiceTestClient();
    private static final JdbcConnectionProvider connectionProvider = new JdbcConnectionProvider();
    private static Properties properties = new Properties();
    private static String host;
    private static String user;
    private static String password;
    private static String driverClassName;
    private static String connectionString;

    static {
        try {
            properties.load(FileServiceClient.class.getClassLoader().getResourceAsStream("fileservice-db.properties"));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        final String connectionStringTemplate = properties.getProperty("connectionStringTemplate", "jdbc:postgresql://%s/fileservice");
        host = TestHostProvider.getHost();
        user = properties.getProperty("user", "fileservice");
        password = properties.getProperty("password", "fileservice");
        driverClassName = properties.getProperty("driverClassName", "org.postgresql.Driver");
        connectionString = String.format(connectionStringTemplate, host);
    }

    public static UUID create(final String fileName, final String mimeType, byte[] content) throws SQLException, FileServiceException {
        try (final Connection connection = connectionProvider.getConnection(connectionString, user, password, driverClassName)) {
            UUID fileStoreId = client.create(fileName, mimeType, new ByteArrayInputStream(content), connection);
            connection.close();
            return fileStoreId;
        }
    }

}
