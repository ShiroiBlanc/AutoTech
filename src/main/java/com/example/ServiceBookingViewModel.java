package com.example;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ServiceBookingViewModel {
    private int id;
    private String hexId;
    private Customer customer;
    private Vehicle vehicle;
    private Mechanic mechanic; // Keep for backward compatibility
    private List<Mechanic> mechanics; // New field for multiple mechanics
    private LocalDate date;
    private String time;
    private String serviceType;
    private String serviceDescription;
    private String status;
    private final BooleanProperty selected = new SimpleBooleanProperty(false);

    public ServiceBookingViewModel() {
        this.mechanics = new ArrayList<>();
    }
    
    public boolean isSelected() {
        return selected.get();
    }
    
    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }
    
    public BooleanProperty selectedProperty() {
        return selected;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
    
    public String getHexId() {
        return hexId;
    }
    
    public void setHexId(String hexId) {
        this.hexId = hexId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public Mechanic getMechanic() {
        // Return first mechanic for backward compatibility
        return mechanics.isEmpty() ? mechanic : mechanics.get(0);
    }

    public void setMechanic(Mechanic mechanic) {
        this.mechanic = mechanic;
        // Also add to list if not already there
        if (mechanic != null && !mechanics.contains(mechanic)) {
            mechanics.clear();
            mechanics.add(mechanic);
        }
    }
    
    public List<Mechanic> getMechanics() {
        return new ArrayList<>(mechanics);
    }
    
    public void setMechanics(List<Mechanic> mechanics) {
        this.mechanics = new ArrayList<>(mechanics);
        // Set first mechanic for backward compatibility
        if (!mechanics.isEmpty()) {
            this.mechanic = mechanics.get(0);
        }
    }
    
    public void addMechanic(Mechanic mechanic) {
        if (mechanic != null && !mechanics.contains(mechanic)) {
            mechanics.add(mechanic);
            if (this.mechanic == null) {
                this.mechanic = mechanic;
            }
        }
    }
    
    public void removeMechanic(Mechanic mechanic) {
        mechanics.remove(mechanic);
        if (mechanic != null && mechanic.equals(this.mechanic)) {
            this.mechanic = mechanics.isEmpty() ? null : mechanics.get(0);
        }
    }
    
    public String getMechanicsNames() {
        if (mechanics.isEmpty()) {
            return mechanic != null ? mechanic.getName() : "";
        }
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < mechanics.size(); i++) {
            if (i > 0) names.append(", ");
            names.append(mechanics.get(i).getName());
        }
        return names.toString();
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getServiceDescription() {
        return serviceDescription;
    }

    public void setServiceDescription(String serviceDescription) {
        this.serviceDescription = serviceDescription;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    
    // Convenience methods for table display
    public String getCustomerName() {
        return customer != null ? customer.getName() : "";
    }
    
    public String getVehicleInfo() {
        return vehicle != null ? vehicle.getBrand() : "";
    }
    
    public String getMechanicName() {
        return mechanic != null ? mechanic.getName() : "";
    }
}