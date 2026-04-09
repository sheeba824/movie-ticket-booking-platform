package com.movietickets.offer.service;

import com.movietickets.show.entity.Show;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Pricing Service
 * Handles dynamic pricing calculation and offer application
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PricingService {

    /**
     * Calculate pricing for booking
     *
     * @param show Show details
     * @param seatCount Number of seats
     * @param userId User ID (for personalized offers)
     * @return Pricing details
     */
    public PricingDetails calculatePricing(Show show, int seatCount, java.util.UUID userId) {
        log.info("Calculating pricing for show: {} seats: {}", show.getId(), seatCount);

        BigDecimal basePrice = show.getBasePrice();
        BigDecimal subtotal = basePrice.multiply(BigDecimal.valueOf(seatCount));
        BigDecimal discount = BigDecimal.ZERO;

        List<OfferDetail> appliedOffers = new ArrayList<>();

        // Apply afternoon discount (20% off for 2 PM - 5 PM shows)
        if (isAfternoonShow(show.getShowTime())) {
            BigDecimal afternoonDiscount = subtotal.multiply(BigDecimal.valueOf(0.20));
            appliedOffers.add(new OfferDetail(
                "AFTERNOON_DISCOUNT",
                "20% off on afternoon shows (2 PM - 5 PM)",
                20,
                afternoonDiscount
            ));
            discount = discount.add(afternoonDiscount);
        }

        // Apply third ticket discount (50% off on 3rd ticket onwards)
        if (seatCount >= 3) {
            BigDecimal thirdTicketDiscount = basePrice.multiply(BigDecimal.valueOf(seatCount - 2))
                .multiply(BigDecimal.valueOf(0.50));
            appliedOffers.add(new OfferDetail(
                "THIRD_TICKET_DISCOUNT",
                "50% off on 3rd ticket onwards",
                50,
                thirdTicketDiscount
            ));
            discount = discount.add(thirdTicketDiscount);
        }

        BigDecimal finalAmount = subtotal.subtract(discount);

        return PricingDetails.builder()
            .basePrice(basePrice)
            .subtotal(subtotal)
            .offers(appliedOffers)
            .totalDiscount(discount)
            .finalAmount(finalAmount)
            .currency("INR")
            .build();
    }

    /**
     * Apply promotional code
     *
     * @param promoCode Promo code
     * @param bookingAmount Booking amount
     * @return Additional discount amount
     */
    public BigDecimal applyPromoCode(String promoCode, BigDecimal bookingAmount) {
        log.info("Applying promo code: {} for amount: {}", promoCode, bookingAmount);

        // Validate and apply promo code (implementation depends on PromotionRepository)
        // Example promotional codes:
        // SUMMER50 -> 50% off, max discount 100
        // WELCOME25 -> 25% off, max discount 50

        if ("SUMMER50".equals(promoCode)) {
            BigDecimal discount = bookingAmount.multiply(BigDecimal.valueOf(0.50));
            BigDecimal maxDiscount = BigDecimal.valueOf(100);
            return discount.min(maxDiscount);
        }

        return BigDecimal.ZERO;
    }

    /**
     * Check if show is afternoon show
     *
     * @param showTime Show time
     * @return True if show is between 2 PM and 5 PM
     */
    private boolean isAfternoonShow(LocalDateTime showTime) {
        int hour = showTime.getHour();
        return hour >= 14 && hour < 17; // 2 PM to 5 PM (14:00 to 16:59)
    }

    /**
     * PricingDetails DTO
     */
    public static class PricingDetails {
        public BigDecimal basePrice;
        public BigDecimal subtotal;
        public List<OfferDetail> offers;
        public BigDecimal totalDiscount;
        public BigDecimal finalAmount;
        public String currency;

        public static PricingDetailsBuilder builder() {
            return new PricingDetailsBuilder();
        }

        public static class PricingDetailsBuilder {
            private BigDecimal basePrice;
            private BigDecimal subtotal;
            private List<OfferDetail> offers;
            private BigDecimal totalDiscount;
            private BigDecimal finalAmount;
            private String currency;

            public PricingDetailsBuilder basePrice(BigDecimal basePrice) {
                this.basePrice = basePrice;
                return this;
            }

            public PricingDetailsBuilder subtotal(BigDecimal subtotal) {
                this.subtotal = subtotal;
                return this;
            }

            public PricingDetailsBuilder offers(List<OfferDetail> offers) {
                this.offers = offers;
                return this;
            }

            public PricingDetailsBuilder totalDiscount(BigDecimal totalDiscount) {
                this.totalDiscount = totalDiscount;
                return this;
            }

            public PricingDetailsBuilder finalAmount(BigDecimal finalAmount) {
                this.finalAmount = finalAmount;
                return this;
            }

            public PricingDetailsBuilder currency(String currency) {
                this.currency = currency;
                return this;
            }

            public PricingDetails build() {
                PricingDetails details = new PricingDetails();
                details.basePrice = this.basePrice;
                details.subtotal = this.subtotal;
                details.offers = this.offers;
                details.totalDiscount = this.totalDiscount;
                details.finalAmount = this.finalAmount;
                details.currency = this.currency;
                return details;
            }
        }
    }

    /**
     * OfferDetail DTO
     */
    public static class OfferDetail {
        public String type;
        public String description;
        public int percentage;
        public BigDecimal amount;

        public OfferDetail(String type, String description, int percentage, BigDecimal amount) {
            this.type = type;
            this.description = description;
            this.percentage = percentage;
            this.amount = amount;
        }
    }
}
