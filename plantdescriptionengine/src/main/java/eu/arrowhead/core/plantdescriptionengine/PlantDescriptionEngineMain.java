package eu.arrowhead.core.plantdescriptionengine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;

import se.arkalix.ArSystem;
import se.arkalix.descriptor.EncodingDescriptor;
import se.arkalix.descriptor.SecurityDescriptor;
import se.arkalix.http.HttpStatus;
import se.arkalix.http.service.HttpService;
import se.arkalix.http.service.HttpServiceRequest;
import se.arkalix.http.service.HttpServiceResponse;
import se.arkalix.dto.DtoWriteException;
import se.arkalix.security.X509KeyStore;
import se.arkalix.security.X509TrustStore;
import se.arkalix.util.concurrent.Future;

public class PlantDescriptionEngineMain {

    private static PlantDescriptionDto currentDescription = null;

    private static void toFile(PlantDescriptionDto description) throws DtoWriteException, IOException {
        final String filename = "plant-description.json";
        final FileOutputStream out = new FileOutputStream(new File(filename));
        final DtoWriter writer = new DtoWriter(out);
        description.writeJson(writer);
        out.close();
    }

    /**
     * @param password       Password of the private key associated with the
     *                       certificate in key store.
     * @param keyStorePath   Path to the keystore representing the systems own
     *                       identity.
     * @param trustStorePath Path to the trust store representing all identities
     *                       that are to be trusted by the system.
     * @return An Arrowhead Framework system.
     * @throws IOException
     * @throws GeneralSecurityException
     */
    private static ArSystem getArSystem(char[] password, String keyStorePath, String trustStorePath)
            throws GeneralSecurityException, IOException {

        X509KeyStore keyStore = null;
        X509TrustStore trustStore = null;

        keyStore = new X509KeyStore.Loader()
            .keyPassword(password)
            .keyStorePath(Path.of(keyStorePath))
            .keyStorePassword(password).load();
        trustStore = X509TrustStore.read(Path.of(trustStorePath), password);

        var system = new ArSystem.Builder()
            .keyStore(keyStore)
            .trustStore(trustStore)
            .localPort(28081)
            .build();

        return system;
    }

    private static Future<HttpServiceResponse> onDescriptionPost(
        HttpServiceRequest request, HttpServiceResponse response
    ) {
        return request.bodyAs(PlantDescriptionDto.class)
        .map(body -> {
            try {
                // Write the plant description to disk
                toFile(body);
            } catch (IOException e) {
                e.printStackTrace();
                return response.status(HttpStatus.INTERNAL_SERVER_ERROR);
            }
            currentDescription = body;
            return response.status(HttpStatus.CREATED).body(body);
        });
    }

    private static Future<?> onDescriptionsGet(
        HttpServiceRequest request, HttpServiceResponse response
    ) {
        if (currentDescription == null) {
            response.status(HttpStatus.NOT_FOUND);
        } else {
            response.status(HttpStatus.OK).body(currentDescription);
        }
        return Future.done();
    }

    private static HttpService getServices() {
        return new HttpService()
            .name("plant-description-management-service")
            .encodings(EncodingDescriptor.JSON)
            .security(SecurityDescriptor.CERTIFICATE)
            .basePath("/pde")
            .get("/mgmt/pd", (request, response) ->
                onDescriptionsGet(request, response))
            .post("/mgmt/pd/#id", (request, response) ->
                onDescriptionPost(request, response));
    }

    public static void main(final String[] args) {

        if (args.length != 2) {
            System.err.println("Requires two command line arguments: <keyStorePath> and <trustStorePath>");
            System.exit(1);
        }

        final var password = new char[] { '1', '2', '3', '4', '5', '6' };
        ArSystem system = null;

        try {
            system = getArSystem(password, args[0], args[1]);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            System.exit(74);
        }

        System.out.println("Providing services...");
        system.provide(getServices())
            .onFailure(Throwable::printStackTrace);
    }
}
