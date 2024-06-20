package pein.johannes.parcelconnectbackend.carrier;

import lombok.Data;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

@Data
public class NeighborDTO {
    final String firstName;
    final String lastName;
    final String street;
    final String houseNumber;
    final String zipCode;
    // following parameters are set to default
    final String addressAddition = "";
    final String city = "";
    final String termsAccepted = "true";
    final String emailAddress = "";

    public NeighborDTO(String firstName, String lastName, String street, String houseNumber, String zipCode) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.street = street;
        this.houseNumber = houseNumber;
        this.zipCode = zipCode;
    }
}
