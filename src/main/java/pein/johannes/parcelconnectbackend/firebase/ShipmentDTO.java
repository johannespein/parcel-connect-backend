package pein.johannes.parcelconnectbackend.firebase;

import lombok.Data;

@Data
public class ShipmentDTO {

    private String carrier;
    private String trackingNumber;
    private String status;
    private String expectedDelivery;
    private String ownerUid;
    private String shipmentCardStatus;
    private boolean neighborDeliveryAvailable;


    public ShipmentDTO(String carrier, String trackingNumber, String status, String expectedDelivery, String ownerUid, boolean neighborDeliveryAvailable) {
        this.carrier = carrier;
        this.trackingNumber = trackingNumber;
        this.status = status;
        this.expectedDelivery = expectedDelivery;
        this.ownerUid = ownerUid;
        this.neighborDeliveryAvailable = neighborDeliveryAvailable;
    }



}
