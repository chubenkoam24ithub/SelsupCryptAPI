import selsup.test.ctyptapi.CryptApi;
import selsup.test.ctyptapi.exception.ApiException;
import selsup.test.ctyptapi.json.Document;
import selsup.test.ctyptapi.json.Product;

import java.util.Base64;
import java.util.concurrent.TimeUnit;

public class Test {

    public static void main(String[] args) {
        CryptApi api = new CryptApi(TimeUnit.SECONDS, 10);
        Document doc = new Document(
                "7724211288",
                "2025-06-16",
                "DOC123",
                "2025-06-15",
                "LOCAL",
                new Product[] {
                        new Product(
                                "010469022978096621qSF7qja3aRqM2406402",
                                "04690229780966",
                                "CONFORMITY_CERTIFICATE",
                                "2025-06-15",
                                "CERT123"
                        )
                }
        );
        String signature = Base64.getEncoder().encodeToString("SIGNED_DATA".getBytes());
        try {
            String documentId = api.createDocument(doc, signature);
            System.out.println("Создан документ с ID: " + documentId);
        } catch (ApiException e) {
            System.err.println("Ошибка: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
