package dto;

import model.enums.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;

public class RoomSearchCriteria {
    private LocalDate checkIn;
    private LocalDate checkOut;
    private int numberOfGuests;
    private RoomType type;
    private BigDecimal maxPrice;
    private Integer minCapacity;

    public RoomSearchCriteria() {}

    public RoomSearchCriteria(LocalDate checkIn, LocalDate checkOut, int numberOfGuests) {
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.numberOfGuests = numberOfGuests;
    }

    public boolean hasTypeFilter() { return type != null; }
    public boolean hasMaxPriceFilter() { return maxPrice != null; }
    public boolean hasMinCapacityFilter() { return minCapacity != null; }

    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }
    public int getNumberOfGuests() { return numberOfGuests; }
    public void setNumberOfGuests(int numberOfGuests) { this.numberOfGuests = numberOfGuests; }
    public RoomType getType() { return type; }
    public void setType(RoomType type) { this.type = type; }
    public BigDecimal getMaxPrice() { return maxPrice; }
    public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }
    public Integer getMinCapacity() { return minCapacity; }
    public void setMinCapacity(Integer minCapacity) { this.minCapacity = minCapacity; }
}