/**
 * Vehicle.java
 * -------------------------------------------------------
 * Defines the input data record used throughout the system.
 * (Requirement: "Defining project input data - list of elements/records")
 * -------------------------------------------------------
 */
public class Vehicle {
    private String vehicleNumber;   // unique ID, e.g. "KA-05-AB-1234"
    private String ownerName;
    private String vehicleType;     // CAR, BIKE, STAFF, VISITOR
    private int slotNumber;         // assigned parking slot
    private String entryTime;       // simple string timestamp for now
 
    public Vehicle(String vehicleNumber, String ownerName, String vehicleType,
                    int slotNumber, String entryTime) {
        this.vehicleNumber = vehicleNumber;
        this.ownerName = ownerName;
        this.vehicleType = vehicleType;
        this.slotNumber = slotNumber;
        this.entryTime = entryTime;
    }
 
    public String getVehicleNumber() { return vehicleNumber; }
    public String getOwnerName() { return ownerName; }
    public String getVehicleType() { return vehicleType; }
    public int getSlotNumber() { return slotNumber; }
    public String getEntryTime() { return entryTime; }
 
    @Override
    public String toString() {
        return String.format("Slot %-3d | %-12s | %-15s | %-6s | Entered: %s",
                slotNumber, vehicleNumber, ownerName, vehicleType, entryTime);
    }
}
 