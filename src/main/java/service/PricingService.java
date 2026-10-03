package service;

import model.Room;
import policy.PricingStrategy;
import policy.StandardPricingStrategy;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PricingService {
    private PricingStrategy pricingStrategy;

    public PricingService() {
        this.pricingStrategy = new StandardPricingStrategy();
    }

    public PricingService(PricingStrategy pricingStrategy) {
        this.pricingStrategy = pricingStrategy;
    }

    public void setPricingStrategy(PricingStrategy pricingStrategy) {
        this.pricingStrategy = pricingStrategy;
    }

    public BigDecimal calculateTotalPrice(Room room, LocalDate checkIn, LocalDate checkOut) {
        return pricingStrategy.calculatePrice(room, checkIn, checkOut, LocalDate.now());
    }
}