package model;

import model.enums.RoomStatus;
import model.enums.RoomType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Room {
    private String roomNumber;
    private RoomType type;
    private int capacity;
    private BigDecimal pricePerNight;
    private RoomStatus status;
    private Integer floor;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Room() {
        this.status = RoomStatus.AVAILABLE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Room(String roomNumber, RoomType type, int capacity, BigDecimal pricePerNight) {
        this();
        this.roomNumber = roomNumber;
        this.type = type;
        this.capacity = capacity;
        this.pricePerNight = pricePerNight;
    }

    public Room(String roomNumber, RoomType type, int capacity, BigDecimal pricePerNight,
                RoomStatus status, Integer floor, String description) {
        this(roomNumber, type, capacity, pricePerNight);
        this.status = status;
        this.floor = floor;
        this.description = description;
    }

    public boolean isAvailable() {
        return status == RoomStatus.AVAILABLE;
    }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public RoomType getType() { return type; }
    public void setType(RoomType type) { this.type = type; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public BigDecimal getPricePerNight() { return pricePerNight; }
    public void setPricePerNight(BigDecimal pricePerNight) { this.pricePerNight = pricePerNight; }
    public RoomStatus getStatus() { return status; }
    public void setStatus(RoomStatus status) { this.status = status; }
    public Integer getFloor() { return floor; }
    public void setFloor(Integer floor) { this.floor = floor; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}