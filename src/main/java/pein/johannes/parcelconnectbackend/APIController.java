package pein.johannes.parcelconnectbackend;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.gson.JsonArray;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.web.bind.annotation.*;
import pein.johannes.parcelconnectbackend.carrier.dhl.DhlCommunicationService;
import pein.johannes.parcelconnectbackend.carrier.NeighborDTO;
import pein.johannes.parcelconnectbackend.emailprovider.google.GmailService;
import pein.johannes.parcelconnectbackend.firebase.ShipmentDTO;
import pein.johannes.parcelconnectbackend.firebase.FirebaseService;
import pein.johannes.parcelconnectbackend.firebase.Note;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@RestController
public class APIController {
    private final FirebaseService firebaseService;
    private final DhlCommunicationService dhlCommunicationService;

    public APIController(FirebaseService firebaseService, DhlCommunicationService dhlCommunicationService, GmailService gmailService) {
        this.firebaseService = firebaseService;
        this.dhlCommunicationService = dhlCommunicationService;
//        dhlCommunicationService.getShipmentDTO("00340433954778466418", "W5VXHwpqUC3eq5v4vxcC");
    }


    @PostMapping("/dhl-neighbor-delivery")
    public String dhlNeighborDelivery(@RequestBody String body) throws JSONException {
        JSONObject json = new JSONObject(body);

        String trackingNumber = json.getString("shipmentNumber");

        NeighborDTO neighborDTO = new NeighborDTO(
                json.getString("firstName"),
                json.getString("lastName"),
                json.getString("street"),
                json.getString("houseNumber"),
                json.getString("zipCode")
        );

        System.out.println("Could call adress change for shipment " + trackingNumber);
        System.out.println("to neighbor " + neighborDTO.getFirstName() + " " + neighborDTO.getLastName());

//        String response = dhlCommunicationService.sendNeighborDeliveryRequest(trackingNumber, neighborDTO);
//        System.out.println("response from dhl: " + response);

        String response = "success";

        if(response.equals("success")){
            String shipmentId = json.getString("shipmentId");
            String ownerUid = json.getString("ownerUid");
            String recipientUid = json.getString("recipientUid");

            try {
                ShipmentDTO shipmentDTO = dhlCommunicationService.getShipmentDTO(trackingNumber, ownerUid);
                if (shipmentDTO != null){
                    return firebaseService.updateShipment(shipmentDTO, shipmentId, "TO_NEIGHBOR", recipientUid);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return "Internal shipment update failed. Neighbor delivery was still triggered.";
            }
        }
        return "Neighbor delivery currently not available.";
    }

    @PostMapping("/update-shipment-card-state")
    public String updateShipmentCardState(@RequestBody String body){
        try {
            JSONObject json = new JSONObject(body);

            String shipmentId = json.getString("shipmentId");
            String shipmentCardState = json.getString("shipmentCardState");

            System.out.println("Setting state to: " + shipmentCardState);
            return firebaseService.updateShipmentCardState(shipmentId, shipmentCardState);
        } catch (Exception e){
            e.printStackTrace();
            return "Data transferal error.";
        }
    }

    @PostMapping("/update-neighbor-group-shipments")
    public String updateShipments(@RequestBody String body){
        try{
            JSONArray arr = new JSONArray(body);
            for (int i=0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);

                String trackingNumber = obj.getString("trackingNumber");
                String uid = obj.getString("ownerUid");
                ShipmentDTO shipmentDTO = dhlCommunicationService.getShipmentDTO(trackingNumber, uid);

                String shipmentId = obj.getString("shipmentId");
                firebaseService.updateShipment(shipmentDTO, shipmentId, null, null);
            }
            return "success";
        } catch (Exception e){
            e.printStackTrace();
            return "error";
        }
    }

    @PostMapping("/register-shipment")
    public String registerShipment(@RequestBody String body){
        try {
            JSONObject json = new JSONObject(body);

            String trackingNumber = json.getString("trackingNumber");
            String ownerUid = json.getString("ownerUid");

            ShipmentDTO shipmentDTO = dhlCommunicationService.getShipmentDTO(trackingNumber, ownerUid);

            return firebaseService.createShipment(shipmentDTO);
        } catch (Exception e){
            e.printStackTrace();
            return "Shipment could not be registered.";
        }
    }


    @RequestMapping("/send-notification")
    @ResponseBody
    public String sendNotification(@RequestBody Note note,
                                   @RequestParam String topic) throws FirebaseMessagingException {
        return firebaseService.sendNotification(note, topic);
    }
}
