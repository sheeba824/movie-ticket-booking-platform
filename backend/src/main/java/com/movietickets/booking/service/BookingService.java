package com.movietickets.booking.service;

import com.movietickets.booking.dto.BookingRequest;
import com.movietickets.booking.dto.BookingResponse;
import com.movietickets.booking.dto.SeatReservationRequest;
import com.movietickets.booking.entity.Booking;
import com.movietickets.booking.entity.BookingSeat;
import com.movietickets.booking.entity.SeatLock;
import com.movietickets.booking.repository.BookingRepository;
import com.movietickets.booking.repository.BookingSeatRepository;
import com.movietickets.booking.repository.SeatLockRepository;
import com.movietickets.common.exception.CustomException;
import com.movietickets.common.exception.ErrorCode;
import com.movietickets.show.entity.Show;
import com.movietickets.show.repository.ShowRepository;
import com.movietickets.offer.service.PricingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Booking Service
 * Handles booking creation, seat selection, and booking management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {
    
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final SeatLockRepository seatLockRepository;
    private final ShowRepository showRepository;
    private final PricingService pricingService;
    private final SeatLockingManager seatLockingManager;
    private final BookingReferenceGenerator referenceGenerator;

    private static final long SEAT_LOCK_DURATION_SECONDS = 900; // 15 minutes

    /**
     * Initiate booking process
     *
     * @param userId User ID
     * @param request Booking request details
     * @return Booking session response
     */
    @Transactional
    public Map<String, Object> initiateBooking(UUID userId, BookingRequest request) {
        log.info("Initiating booking for user: {} show: {}", userId, request.getShowId());

        // Validate show exists and is scheduled
        Show show = showRepository.findById(request.getShowId())
            .orElseThrow(() -> new CustomException(
                ErrorCode.SHOW_NOT_FOUND,
                "Show not found"
            ));

        if (!show.getStatus().equals("SCHEDULED")) {
            throw new CustomException(
                ErrorCode.SHOW_NOT_AVAILABLE,
                "Show is not available for booking"
            );
        }

        if (show.getAvailableSeats() < request.getNumberOfSeats()) {
            throw new CustomException(
                ErrorCode.INSUFFICIENT_SEATS,
                "Not enough seats available"
            );
        }

        // Create booking session
        Map<String, Object> sessionResponse = new HashMap<>();
        sessionResponse.put("bookingSessionId", UUID.randomUUID());
        sessionResponse.put("showId", show.getId());
        sessionResponse.put("availableSeats", show.getAvailableSeats());
        sessionResponse.put("expiresAt", LocalDateTime.now().plusMinutes(15));

        return sessionResponse;
    }

    /**
     * Reserve seats for booking
     *
     * @param userId User ID
     * @param request Seat reservation request
     * @return Reservation details with pricing
     */
    @Transactional
    public Map<String, Object> reserveSeats(UUID userId, SeatReservationRequest request) {
        log.info("Reserving seats for user: {} seats: {}", userId, request.getSeats());

        Show show = showRepository.findById(request.getShowId())
            .orElseThrow(() -> new CustomException(
                ErrorCode.SHOW_NOT_FOUND,
                "Show not found"
            ));

        // Validate seat availability and lock status
        validateSeatsAvailability(show.getId(), request.getSeats());

        // Lock seats
        seatLockingManager.lockSeats(show.getId(), request.getSeats(), SEAT_LOCK_DURATION_SECONDS, userId);

        // Calculate pricing
        PricingDetails pricing = pricingService.calculatePricing(
            show,
            request.getSeats().size(),
            userId
        );

        Map<String, Object> response = new HashMap<>();
        response.put("bookingSessionId", request.getBookingSessionId());
        response.put("reservedSeats", request.getSeats());
        response.put("pricing", pricing);
        response.put("lockedUntil", LocalDateTime.now().plusMinutes(15));

        log.info("Seats reserved successfully for user: {}", userId);
        return response;
    }

    /**
     * Confirm booking after payment
     *
     * @param userId User ID
     * @param bookingSessionId Booking session ID
     * @param paymentTransactionId Payment transaction ID
     * @return Confirmed booking response
     */
    @Transactional
    public BookingResponse confirmBooking(UUID userId, UUID bookingSessionId, String paymentTransactionId) {
        log.info("Confirming booking for user: {} session: {}", userId, bookingSessionId);

        // Validate payment was successful (integration with payment service)

        // Create booking record
        Booking booking = new Booking();
        booking.setId(UUID.randomUUID());
        booking.setUserId(userId);
        booking.setBookingReference(referenceGenerator.generate());
        booking.setBookingStatus("CONFIRMED");
        booking.setConfirmationTime(LocalDateTime.now());

        booking = bookingRepository.save(booking);

        log.info("Booking confirmed with reference: {}", booking.getBookingReference());

        return BookingResponse.builder()
            .bookingId(booking.getId())
            .bookingReference(booking.getBookingReference())
            .bookingStatus("CONFIRMED")
            .bookingTime(booking.getBookingTime())
            .build();
    }

    /**
     * Cancel booking
     *
     * @param userId User ID
     * @param bookingId Booking ID
     * @param reason Cancellation reason
     */
    @Transactional
    public void cancelBooking(UUID userId, UUID bookingId, String reason) {
        log.info("Cancelling booking: {} for user: {}", bookingId, userId);

        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new CustomException(
                ErrorCode.BOOKING_NOT_FOUND,
                "Booking not found"
            ));

        // Verify ownership
        if (!booking.getUserId().equals(userId)) {
            throw new CustomException(
                ErrorCode.UNAUTHORIZED,
                "Unauthorized to cancel this booking"
            );
        }

        // Check cancellation eligibility
        if (booking.getShowTime().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new CustomException(
                ErrorCode.CANCELLATION_NOT_ALLOWED,
                "Cannot cancel booking within 1 hour of show time"
            );
        }

        // Update booking status
        booking.setBookingStatus("CANCELLED");
        booking.setCancellationTime(LocalDateTime.now());
        booking.setCancellationReason(reason);
        bookingRepository.save(booking);

        // Release locked seats
        List<BookingSeat> bookedSeats = bookingSeatRepository.findByBookingId(bookingId);
        bookedSeats.forEach(seat -> 
            seatLockingManager.releaseSeat(booking.getShowId(), seat.getSeatNumber())
        );

        log.info("Booking cancelled successfully: {}", bookingId);
    }

    /**
     * Get booking history for user
     *
     * @param userId User ID
     * @param status Booking status filter (optional)
     * @return List of bookings
     */
    public List<BookingResponse> getBookingHistory(UUID userId, String status) {
        log.info("Fetching booking history for user: {} status: {}", userId, status);

        List<Booking> bookings = status == null
            ? bookingRepository.findByUserId(userId)
            : bookingRepository.findByUserIdAndBookingStatus(userId, status);

        return bookings.stream()
            .map(this::mapToBookingResponse)
            .collect(Collectors.toList());
    }

    /**
     * Validate seat availability and lock status
     *
     * @param showId Show ID
     * @param seats List of seat numbers
     */
    private void validateSeatsAvailability(UUID showId, List<String> seats) {
        for (String seat : seats) {
            if (seatLockingManager.isLocked(showId, seat)) {
                throw new CustomException(
                    ErrorCode.SEAT_ALREADY_LOCKED,
                    "Seat " + seat + " is already locked by another user"
                );
            }
        }
    }

    /**
     * Map Booking entity to BookingResponse DTO
     *
     * @param booking Booking entity
     * @return BookingResponse DTO
     */
    private BookingResponse mapToBookingResponse(Booking booking) {
        return BookingResponse.builder()
            .bookingId(booking.getId())
            .bookingReference(booking.getBookingReference())
            .bookingStatus(booking.getBookingStatus())
            .totalAmount(booking.getTotalAmount())
            .finalAmount(booking.getFinalAmount())
            .bookingTime(booking.getBookingTime())
            .build();
    }
}
