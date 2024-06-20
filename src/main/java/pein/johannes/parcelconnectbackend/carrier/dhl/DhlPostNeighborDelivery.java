package pein.johannes.parcelconnectbackend.carrier.dhl;

import lombok.extern.log4j.Log4j2;
import org.apache.http.Header;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import pein.johannes.parcelconnectbackend.carrier.NeighborDTO;

import java.net.URI;
import java.util.Optional;

@Log4j2
public class DhlPostNeighborDelivery extends HttpPost{

    final String baseUrl = "https://www.dhl.de/int-verfuegen/offers/preferredneighbour/order?verfuegen_shipment_token=";

    public DhlPostNeighborDelivery(String trackingNumber, NeighborDTO dto, String shipmentToken, CookieStore cookieStore) throws Exception {
            super();
            setEntity(new StringEntity(buildRequestEntityAsJSONString(dto), ContentType.APPLICATION_JSON));
            setURI(URI.create(baseUrl + shipmentToken));
            Header[] headers = getDhlHeaders(trackingNumber, cookieStore);
            setHeaders(headers);
        }

    private Header[] getDhlHeaders(String trackingNumber, CookieStore cookieStore) throws Exception {
        // add crsf cookie as header
        Optional<Cookie> csrfCookie = cookieStore.getCookies().stream()
                .filter(c -> c.getName().equals("verfolgenCsrfToken"))
                .findFirst();
        if (!csrfCookie.isPresent()){
            log.error("Csrf Token not found in cookie store.");
            throw new Exception();
        }

        return new Header[]{
                new BasicHeader("accept", "application/json"),
                new BasicHeader("accept-encoding", "gzip, deflate, br"),
                new BasicHeader("accept-language", "de"),
                new BasicHeader("cache-control", "no-cache,no-store,must-revalidate"),
                new BasicHeader("connection", "keep-alive"),
                new BasicHeader("content-type", "application/json"),
                new BasicHeader("expires", "0"),
                new BasicHeader("host", "www.dhl.de"),
                new BasicHeader("origin", "https://www.dhl.de"),
                new BasicHeader("referer", "https://www.dhl.de/de/privatkunden/pakete-empfangen/verfolgen.html?" + trackingNumber),
                new BasicHeader("sec-fetch-dest", "empty"),
                new BasicHeader("sec-fetch-mode", "cors"),
                new BasicHeader("sec-fetch-site", "same-origin"),
                new BasicHeader("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_2_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.150 Safari/537.36"),
                new BasicHeader("verfolgen-csrf-token",csrfCookie.get().getValue())
        };
    }

    private String buildRequestEntityAsJSONString(NeighborDTO dto){
        return "{" +
                "\"firstName\":\"" + dto.getFirstName() + "\"," +
                "\"lastName\":\"" + dto.getLastName() + "\"," +
                "\"street\":\"" + dto.getStreet() + "\"," +
                "\"houseNumber\":\"" + dto.getHouseNumber() + "\"," +
                "\"addressAddition\":\"" + dto.getAddressAddition() + "\"," +
                "\"zipCode\":\"" + dto.getZipCode() + "\"," +
                "\"city\":\"" + dto.getCity() + "\"," +
                "\"termsAccepted\":" + dto.getTermsAccepted() + "," +
                "\"emailAddress\":\"" + dto.getEmailAddress() + "\"" +
                "}";
    }


}
