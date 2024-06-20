package pein.johannes.parcelconnectbackend.firebase;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.stereotype.Service;

import com.google.cloud.firestore.Firestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static pein.johannes.parcelconnectbackend.utils.Helper.convertShipmentDTOToMap;


@Service
public class FirebaseService {

    private final FirebaseMessaging firebaseMessaging;
    private final Firestore db = FirestoreClient.getFirestore();

    public FirebaseService(FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
    }

    public String sendNotification(Note note, String token) throws FirebaseMessagingException {

        Notification notification = Notification
                .builder()
                .setTitle(note.getSubject())
                .setBody(note.getContent())
                .build();

        Message message = Message
                .builder()
                .setTopic(token)
                .setNotification(notification)
                .putAllData(note.getData())
                .build();

        return firebaseMessaging.send(message);
    }

    public String updateShipmentCardState(String shipmentId, String shipmentCardState){
        System.out.println(shipmentId);
        if (!shipmentId.equals("null") && !shipmentCardState.equals("null")){
            try{
                DocumentReference docRef = db.collection("shipments").document(shipmentId);

                Map<String, Object> data = new HashMap<>();
                data.put("shipmentCardState", shipmentCardState);
                docRef.update(data);

                System.out.println("setting shipment " + shipmentId + "to " + data);
                return "success";
            } catch (Exception e){
                e.printStackTrace();
                return "Shipment could not be found.";
            }
        }

        return "Shipment or ShipmentState null.";
    }



    public String createShipment(ShipmentDTO shipmentDTO){
        if(shipmentDTO != null) {
            try {
                String trackingNumber = shipmentDTO.getTrackingNumber();

                if (doesShipmentExist(trackingNumber)){
                        return "Shipment already exists.";
                }

                Map<String, Object> shipment = convertShipmentDTOToMap(shipmentDTO);
                shipment.put("shipmentCardState", "NEW");
                shipment.put("recipientUid", shipmentDTO.getOwnerUid());

                ApiFuture<DocumentReference> newDoc = db.collection("shipments").add(shipment);

                String shipmentId = newDoc.get().getId();

                DocumentReference docRef = db.collection("users").document(shipmentDTO.getOwnerUid()).collection("userShipments").document();

                Map<String, Object> additionalFields = new HashMap<>();
                additionalFields.put("shipmentId", shipmentId);

                docRef.set(additionalFields);

                return "success";
            }
            catch(Exception e){
                e.printStackTrace();
                return "Shipment could not be registered.";
            }
        }
        return "Shipment could not be registered.";
    }

    public String updateShipment(ShipmentDTO shipmentDTO, String shipmentId, String shipmentCardState, String newRecipientUid){
        if(shipmentDTO != null) {
            try {
                DocumentReference docRef = db.collection("shipments").document(shipmentId);
                Map<String, Object> shipment = convertShipmentDTOToMap(shipmentDTO);
                if(shipmentCardState != null){
                    shipment.put("shipmentCardState", shipmentCardState);
                }
                if(newRecipientUid != null){
                    shipment.put("recipientUid", newRecipientUid);
                }

                //asynchronously write data
                ApiFuture<WriteResult> result = docRef.update(shipment);
                // result.get() blocks on response
                System.out.println("Update time : " + result.get().getUpdateTime());

                return "success";
            }
            catch(Exception e){
                e.printStackTrace();
                return "error: shipment update failed";
            }
        }
        return "error: transferred shipment is null";
    }

    public boolean doesShipmentExist(String trackingNumber) {
        AtomicBoolean shipmentExist = new AtomicBoolean(false);

        db.collection("shipments").listDocuments().forEach(doc -> {
            try {
                String shipmentNumber = doc.get().get().get("shipmentNumber").toString();
                if (shipmentNumber.equals(trackingNumber)){
                    shipmentExist.set(true);
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        });

        return shipmentExist.get();
    }


}
