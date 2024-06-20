package pein.johannes.parcelconnectbackend.carrier.dhl;

import com.google.cloud.Timestamp;
import lombok.extern.log4j.Log4j2;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.ClientCookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.util.EntityUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Service;
import pein.johannes.parcelconnectbackend.carrier.CarrierCommunicationService;
import pein.johannes.parcelconnectbackend.carrier.NeighborDTO;
import pein.johannes.parcelconnectbackend.firebase.ShipmentDTO;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

@Service
@Log4j2
public class DhlCommunicationService implements CarrierCommunicationService {
    private WebDriver driver;
    private WebDriverWait wait;
    private boolean cookiesAccepted;

    private BasicCookieStore cookieJar = new BasicCookieStore();

    public DhlCommunicationService(@Value("${webdriver.path}") String webdriverPath, @Value("${dhl.api.key}") String dhlApiKey) {
        System.setProperty("webdriver.chrome.driver", webdriverPath);
        setSeleniumWebdriver();
    }

    public String sendNeighborDeliveryRequest(String trackingNumber, NeighborDTO dto) {
        String shipmentToken;
        try{
            shipmentToken = getShipmentTokenAndSetCookies(trackingNumber);
        } catch (Exception e){
            return "Shipment token could not be retrieved.";
        }
        try {
            DhlPostNeighborDelivery postRequest = new DhlPostNeighborDelivery(trackingNumber, dto, shipmentToken, cookieJar);
            int responseStatus = executeNeighborDeliveryRequest(postRequest);
            if(responseStatus == 200){
                return "success";
            } else{
                return "Shipment could not be altered.";
            }
        } catch (Exception e){
            return "Request failed to send.";
        }
    }

//    public ShipmentDTO oldGetShipmentDTO(String trackingNumber, String uid) throws Exception {
//        cookieJar.clear();
//        HttpGet request = new HttpGet();
////        request.setURI(URI.create("https://api-eu.dhl.com/track/shipments?trackingNumber=" + trackingNumber + "&recipientPostalCode=22297&originCountryCode=de"));
//        request.setURI(URI.create("https://api-eu.dhl.com/track/shipments?trackingNumber=" + trackingNumber));
//
//        request.setHeader(new BasicHeader("DHL-API-Key", dhlApiKey));
//        String shipmentInfoString = executeGetShipmentInfo(request);
//
//        JSONObject shipmentInfoJSON = new JSONObject(shipmentInfoString);
//        ShipmentDTO shipmentDTO = createShipmentDTOFromJSON(shipmentInfoJSON, uid);
//
//        if(!shipmentDTO.getTrackingNumber().equals(trackingNumber)){
//            throw new Exception("Shipment numbers are not equal. Try specifying country code");
//        }
//        return shipmentDTO;
//    }

    public ShipmentDTO getShipmentDTO(String trackingNumber, String uid) {
        driver.get("https://www.dhl.de/en/privatkunden/pakete-empfangen/verfolgen.html?piececode=" + trackingNumber);

        String status = "null";

        try{
            wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("span.infoText")));
            status = driver.findElement(By.cssSelector("span.infoText")).getText().toLowerCase();
        } catch (Exception e ){
            System.out.println("Shipment status not found. Tracking number may be invalid.");
        }

        String expectedDelivery = "null";

        try{
            wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("span.highlighted")));
            expectedDelivery = driver.findElement(By.cssSelector("span.highlighted")).getText();
            System.out.println(expectedDelivery);
        } catch (Exception e){
            log.info("No expected delivery date found.");
        }

        boolean neighborDeliveryAvailable = false;

        if( !status.contains("neighbor") && !status.contains("preferred location")){
            try {
                log.info("calling token method");
                String shipmentToken = getShipmentTokenAndSetCookies(trackingNumber);
                if (shipmentToken != null) neighborDeliveryAvailable = true;
            } catch (Exception e){
                log.error("something went wrong with retrieving shipment token.");
            }
        }

        System.out.println("Neighbor delivery available: " + neighborDeliveryAvailable);

        return new ShipmentDTO("DHL", trackingNumber, status, expectedDelivery, uid, neighborDeliveryAvailable);
    }

    private int executeNeighborDeliveryRequest(DhlPostNeighborDelivery dhlPostNeighborDelivery) throws IOException {
        log.info("executing request");
        try (CloseableHttpClient httpClient = HttpClients.createDefault()){
            HttpClientContext context = HttpClientContext.create();
            context.setCookieStore(cookieJar);
            logRequest(dhlPostNeighborDelivery);
            log.info("Number of Cookies: " + context.getCookieStore().getCookies().size());
            log.info("Body: " + EntityUtils.toString(dhlPostNeighborDelivery.getEntity()));


            try (CloseableHttpResponse response = httpClient.execute(dhlPostNeighborDelivery, context)) {
                return response.getStatusLine().getStatusCode();
            }
        }
    }

//    private String executeGetShipmentInfo(HttpGet get) throws IOException {
//        try (CloseableHttpClient httpClient = HttpClients.createDefault()){
//            logRequest(get);
//
//            try (CloseableHttpResponse response = httpClient.execute(get)) {
//                return EntityUtils.toString(response.getEntity());
//            }
//        }
//    }

    private void logRequest(HttpRequest request){
        log.info("Trying to execute the following request: ");
        log.info(request);
        log.info("Number of Headers: " + request.getAllHeaders().length);
    }

    private void setSeleniumWebdriver(){
        ChromeOptions options = new ChromeOptions();
        options.addArguments(
                "--incognito",
                "--headless",
                "--disable-gpu",
                "--window-size=1920,1200",
                "--ignore-certificate-errors",
                "--disable-extensions",
                "--no-sandbox",
                "--disable-dev-shm-usage"
        );
        LoggingPreferences logPrefs = new LoggingPreferences();
        logPrefs.enable(LogType.PERFORMANCE, Level.ALL);
        options.setCapability(CapabilityType.LOGGING_PREFS, logPrefs);
        options.setCapability(CapabilityType.BROWSER_NAME, "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_2_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.150 Safari/537.36");
        options.setAcceptInsecureCerts(true);
        options.setExperimentalOption("w3c", false);

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, 1);

        driver.get("https://www.dhl.de/en/privatkunden/pakete-empfangen/verfolgen.html");
        wait.until(ExpectedConditions.elementToBeClickable(By.id("confirm-choices-handler")));
        // click on accept cookies checkbox
        driver.findElement(By.id("confirm-choices-handler")).click();
    }

    private String getShipmentTokenAndSetCookies(String trackingNumber) throws Exception {
        // navigate to dhl tracking
        driver.get("https://www.dhl.de/en/privatkunden/pakete-empfangen/verfolgen.html?piececode=" + trackingNumber);

        try{
            wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("span.icon.icon-expand")));
            // give background requests time to execute
            Thread.sleep(1000);
            // click on + button to see "Nachbarschaftsabgabe" :: loads url where shipmentToken is located
            driver.findElement(By.cssSelector("span.icon.icon-expand")).click();
        } catch (Exception e){
            log.info("Neighbor delivery option not found on dhl website.");
            throw e;
        }

        String shipmentToken;

        // waits for shipment token
        try{
            shipmentToken = getShipmentTokenFromRequestRedirectsLogs();
        } catch (Exception e){
            e.printStackTrace();
            log.error("errorrr");
            throw e;
        }

        // fetch csrf-token cookie manually, since automatic redirect per webdriver results in 502 response otherwise. ¯\_(ツ)_/¯
        driver.get("https://www.dhl.de/int-verfolgen/data/i18n?language=de");

        addCookiesToJar();
        return shipmentToken;
    }

    private String getShipmentTokenFromRequestRedirectsLogs() throws Exception {

        LogEntries logs = driver.manage().logs().get(LogType.PERFORMANCE);
        List<JSONObject> logEntriesContainingToken = new ArrayList<>();

        // iterate over log entries to find entries w/ shipment token
        int count = 0;
        do {
            log.info("searching logs");
            logs.iterator().forEachRemaining(logEntry -> {
                if (logEntry.toString().contains("www.dhl.de/int-verfuegen/offers?verfuegen_shipment_token")) {
                    try {
                        logEntriesContainingToken.add(new JSONObject(logEntry.getMessage()));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
            Thread.sleep(500);
            // load new logs
            logs = driver.manage().logs().get(LogType.PERFORMANCE);
            count++;
        } while (logEntriesContainingToken.isEmpty() && count < 3);

        if(!logEntriesContainingToken.isEmpty()){
            String url = logEntriesContainingToken.get(0).getJSONObject("message").getJSONObject("params").getJSONObject("request").getString("url");
            String shipmentToken = url.split("=")[1].split(";")[0];
            return shipmentToken;
        } else {
            throw new Exception("Shipment token could not be found.");
        }
    }

    private void addCookiesToJar(){
        Set<Cookie> cookies = driver.manage().getCookies();

        for (Cookie cookieFromParameterSet : cookies) {
            BasicClientCookie cookie = new BasicClientCookie(cookieFromParameterSet.getName(), cookieFromParameterSet.getValue());

            cookie.setAttribute(ClientCookie.DOMAIN_ATTR, "true");
            cookie.setAttribute(ClientCookie.PATH_ATTR, "true");
            cookie.setAttribute(ClientCookie.EXPIRES_ATTR, "true");

            cookie.setDomain(cookieFromParameterSet.getDomain());
            cookie.setPath(cookieFromParameterSet.getPath());
            cookie.setExpiryDate(cookieFromParameterSet.getExpiry());

            cookieJar.addCookie(cookie);
        }
    }
}
