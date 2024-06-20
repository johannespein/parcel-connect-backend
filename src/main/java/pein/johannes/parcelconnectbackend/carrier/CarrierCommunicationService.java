package pein.johannes.parcelconnectbackend.carrier;

import pein.johannes.parcelconnectbackend.firebase.ShipmentDTO;

public interface CarrierCommunicationService {
    ShipmentDTO getShipmentDTO(String trackingNumber, String uid) throws Exception;
    String sendNeighborDeliveryRequest(String trackingNumber, NeighborDTO dto);
}
