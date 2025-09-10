import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;


public class CrptApi {

    private final RateLimiter rateLimiter;
    private final String token;
    private final HttpClient client;

    public enum DocumentType {
        AGGREGATION_DOCUMENT,
        AGGREGATION_DOCUMENT_CSV ,
        AGGREGATION_DOCUMENT_XML ,
        DISAGGREGATION_DOCUMENT,
        DISAGGREGATION_DOCUMENT_CSV ,
        DISAGGREGATION_DOCUMENT_XML ,
        REAGGREGATION_DOCUMENT,
        REAGGREGATION_DOCUMENT_CSV,
        REAGGREGATION_DOCUMENT_XML ,
        LP_INTRODUCE_GOODS,
        LP_SHIP_GOODS ,
        LP_SHIP_GOODS_CSV,
        LP_SHIP_GOODS_XML,
        LP_INTRODUCE_GOODS_CSV ,
        LP_INTRODUCE_GOODS_XML ,
        LP_ACCEPT_GOODS ,
        LP_ACCEPT_GOODS_XML,
        LK_REMARK,
        LK_REMARK_CSV,
        LK_REMARK_XML,
        LK_RECEIPT ,
        LK_RECEIPT_XML ,
        LK_RECEIPT_CSV ,
        LP_GOODS_IMPORT,
        LP_GOODS_IMPORT_CSV,
        LP_GOODS_IMPORT_XML,
        LP_CANCEL_SHIPMENT,
        LP_CANCEL_SHIPMENT_CSV,
        LP_CANCEL_SHIPMENT_XML,
        LK_KM_CANCELLATION,
        LK_KM_CANCELLATION_CSV ,
        LK_KM_CANCELLATION_XML ,
        LK_APPLIED_KM_CANCELLATION,
        LK_APPLIED_KM_CANCELLATION_CSV,
        LK_APPLIED_KM_CANCELLATION_XML,
        LK_CONTRACT_COMMISSIONING,
        LK_CONTRACT_COMMISSIONING_CSV,
        LK_CONTRACT_COMMISSIONING_XML,
        LK_INDI_COMMISSIONING,
        LK_INDI_COMMISSIONING_CSV,
        LK_INDI_COMMISSIONING_XML,
        LP_SHIP_RECEIPT ,
        LP_SHIP_RECEIPT_CSV,
        LP_SHIP_RECEIPT_XML,
        OST_DESCRIPTION,
        OST_DESCRIPTION_CSV,
        OST_DESCRIPTION_XML,
        CROSSBORDER,
        CROSSBORDER_CSV ,
        CROSSBORDER_XML ,
        LP_INTRODUCE_OST,
        LP_INTRODUCE_OST_CSV ,
        LP_INTRODUCE_OST_XML ,
        LP_RETURN ,
        LP_RETURN_CSV,
        LP_RETURN_XML,
        LP_SHIP_GOODS_CROSSBORDER ,
        LP_SHIP_GOODS_CROSSBORDER_CSV ,
        LP_SHIP_GOODS_CROSSBORDER_XML ,
        LP_CANCEL_SHIPMENT_CROSSBORDER
    }

    private record ProductGroup(String name, int code) { }

    public final static ProductGroup CLOTHES = new ProductGroup("clothes",1);
    public final static ProductGroup SHOES = new ProductGroup("shoes",2);
    public final static ProductGroup TOBACCO = new ProductGroup("tobacco",3);
    public final static ProductGroup PERFUMERY = new ProductGroup("perfumery",4);
    public final static ProductGroup TIRES = new ProductGroup("tires",5);
    public final static ProductGroup ELECTRONICS = new ProductGroup("electronics",6);
    public final static ProductGroup PHARMA = new ProductGroup("pharma",7);
    public final static ProductGroup MILK = new ProductGroup("milk",8);
    public final static ProductGroup BICYCLE = new ProductGroup("bicycle",9);
    public final static ProductGroup WHEELCHAIRS = new ProductGroup("wheelchairs",10);



    public enum DocumentFormat {
        MANUAL,
        XML,
        CSV
    }




    public static class CrtpDocument {

        private final ProductGroup productGroup;
        private final DocumentFormat format;
        private final String productDocument;
        private final DocumentType documentType;
        private JSONObject json;

        public CrtpDocument(ProductGroup productGroup, DocumentFormat documentFormat, String productDocument, DocumentType documentType) {
            this.productGroup = productGroup;
            this.format = documentFormat;
            this.productDocument = productDocument;
            this.documentType = documentType;


            createJSON();
        }

        private String getProductGroup(){
            return productGroup.name;
        }

        private void createJSON(){
            json = new JSONObject();
            json.append("document_format",format);
            json.append("product_document",productDocument);
            json.append("product_group",productGroup.code);
            json.append("type",documentType);
        }

        public JSONObject getJSON() {
            return json;
        }

    }

    public CrptApi(String token) {
        this.token = token;
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(10)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofSeconds(5))
                .build();
        rateLimiter = RateLimiter.of("api-rate-limiter", config);
        client = HttpClient.newHttpClient();
    }

    public CrptApi(String token, Duration timeoutDuration, int requestLimit) {
        this.token = token;
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(requestLimit)
                .limitRefreshPeriod(timeoutDuration)
                .timeoutDuration(Duration.ofSeconds(5))
                .build();
        rateLimiter = RateLimiter.of("api-rate-limiter", config);
        client = HttpClient.newHttpClient();

    }

    public String createDocument(CrtpDocument document, String signature) {

        RateLimiter.waitForPermission(rateLimiter);

        JSONObject json = document.getJSON();
        json.append("signature",signature);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create?pg="+document.getProductGroup()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();


        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to create document", e);
        }
    }

}
