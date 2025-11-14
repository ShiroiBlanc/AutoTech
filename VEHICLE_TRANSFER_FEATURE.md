# Vehicle Transfer Feature

## Overview
Added functionality to transfer vehicle ownership between customers when a vehicle has already been registered in the system but its owner has changed.

## Changes Made

### 1. VehicleService.java - New Methods

#### `transferVehicle(int vehicleId, int newCustomerId)`
- Updates the customer_id of a vehicle to reassign it to a different customer
- Returns boolean indicating success
- SQL: `UPDATE vehicles SET customer_id = ? WHERE id = ?`

#### `getVehicleByPlateNumber(String plateNumber)`
- Retrieves a vehicle by its plate number
- Useful for finding existing vehicles during transfer
- Returns Vehicle object or null if not found

#### `getAllVehicles()`
- Retrieves all vehicles in the system
- Returns List<Vehicle> ordered by ID
- Can be used for global vehicle search/management

### 2. CustomersController.java - UI Updates

#### Vehicle Management Dialog Enhancement
- Added "Transfer" button alongside "Edit" and "Delete" buttons in the vehicle actions column
- Transfer button opens the vehicle transfer dialog

#### New Dialog: `showTransferVehicleDialog(Vehicle vehicle, Customer currentOwner)`
- Shows comprehensive transfer confirmation dialog
- Displays:
  - Current owner information (name and hex ID)
  - Vehicle details (type, brand, model, year, plate number, hex ID)
  - ComboBox to select new owner (excludes current owner from list)
- Features:
  - Transfer button disabled until new owner is selected
  - Two-step confirmation process:
    1. Select new owner and click Transfer
    2. Confirm the transfer with full details displayed
  - Success/error notifications
  - Automatically refreshes vehicle management dialog after successful transfer

## Usage Instructions

### To Transfer a Vehicle:

1. Navigate to the Customers module
2. Click "View" on the customer who currently owns the vehicle
3. In the Customer Details view, click "Manage Vehicles"
4. In the Vehicle Management dialog, find the vehicle you want to transfer
5. Click the "Transfer" button for that vehicle
6. In the Transfer dialog:
   - Review the current owner and vehicle information
   - Select the new owner from the dropdown list
   - Click "Transfer"
7. Confirm the transfer in the confirmation dialog
8. The vehicle will be reassigned to the new owner

### Use Cases:

- **Vehicle sold to another customer**: Transfer the vehicle from seller to buyer
- **Ownership change**: Handle any ownership transfer scenario
- **Correction**: Fix incorrect vehicle assignments

## Technical Details

### Database Impact
- Updates the `customer_id` column in the `vehicles` table
- Transaction is atomic (single UPDATE statement)
- No cascade effects on other tables

### Validation
- New owner must be selected before transfer can proceed
- Current owner is excluded from the new owner selection list
- Two-step confirmation prevents accidental transfers

### User Feedback
- Status label updated with transfer information
- Success alert shows transfer completion
- Error alerts for any issues during transfer
- Dialog automatically refreshes to show updated vehicle list

## Benefits

1. **Data Integrity**: No need to delete and re-add vehicles when ownership changes
2. **History Preservation**: Vehicle ID and history remain intact
3. **Efficient**: Single UPDATE operation, no data recreation
4. **User-Friendly**: Clear, guided process with confirmations
5. **Safe**: Two-step confirmation prevents mistakes
6. **Informative**: Shows all relevant details before confirming transfer

## Future Enhancements (Optional)

- Check for active service bookings before transfer
- Warn if transferring vehicle with pending bookings
- Transfer history/audit log
- Bulk vehicle transfer capability
- Search for vehicles across all customers by plate number
