
package com.absolutecinema.profile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.absolutecinema.booking.BookingStatus;

public class PurchaseDetailItem {

    private Long bookingId;
    private String code;
    private String movieTitle;
    private LocalDateTime showtimeDate;
    private LocalDateTime purchaseDate;
    private String seats;
    private BigDecimal total;
    private BookingStatus bookingStatus;
    private List<PurchasePaymentItem> payments;

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }

    public LocalDateTime getShowtimeDate() {
        return showtimeDate;
    }

    public void setShowtimeDate(LocalDateTime showtimeDate) {
        this.showtimeDate = showtimeDate;
    }

    public LocalDateTime getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDateTime purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public String getSeats() {
        return seats;
    }

    public void setSeats(String seats) {
        this.seats = seats;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public BookingStatus getBookingStatus() {
        return bookingStatus;
    }

    public void setBookingStatus(BookingStatus bookingStatus) {
        this.bookingStatus = bookingStatus;
    }

    public List<PurchasePaymentItem> getPayments() {
        return payments;
    }

    public void setPayments(List<PurchasePaymentItem> payments) {
        this.payments = payments;
    }
}
