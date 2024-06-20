package pein.johannes.parcelconnectbackend.utils;

import com.google.cloud.Timestamp;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import pein.johannes.parcelconnectbackend.firebase.ShipmentDTO;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Helper {

//    public static ShipmentDTO createShipmentDTOFromJSON(JSONObject shipmentInfoJSON, String ownerUid, String shipmentCardState) throws Exception {
//        String trackingNumber = shipmentInfoJSON.getJSONArray("shipments").getJSONObject(0).getString("id");
//        String status = shipmentInfoJSON.getJSONArray("shipments").getJSONObject(0).getJSONObject("status").getString("statusCode");
//
//        String timestampString = shipmentInfoJSON.getJSONArray("shipments").getJSONObject(0).getJSONObject("status").getString("timestamp");
//        DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
//        Date date = format.parse(timestampString);
//        Timestamp timestamp = Timestamp.of(date);
//
//        ShipmentDTO shipmentDTO = new ShipmentDTO("DHL", trackingNumber, status, timestamp, ownerUid);
//
//        return shipmentDTO;
//    }

    public static Map<String, Object> convertShipmentDTOToMap(ShipmentDTO shipmentDTO){
        Map<String, Object> data = new HashMap<>();
        data.put("carrier", shipmentDTO.getCarrier());
        data.put("shipmentNumber", shipmentDTO.getTrackingNumber());
        data.put("expectedDelivery", shipmentDTO.getExpectedDelivery());
        data.put("status", shipmentDTO.getStatus());
        data.put("ownerUid", shipmentDTO.getOwnerUid());
        data.put("isNeighborDeliveryAvailable", shipmentDTO.isNeighborDeliveryAvailable());

        return data;
    }

}
