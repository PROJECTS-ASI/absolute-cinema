
document.addEventListener("DOMContentLoaded", () => {

    // =====================================
    // CONFIGURACIÓN INICIAL
    // =====================================

    const seatMap = document.getElementById("seat-map");
    const selectedSeatsContainer = document.getElementById("selected-seats");
    const ticketPriceElement = document.getElementById("ticket-price");
    const totalElement = document.getElementById("booking-total");
    const continueButton = document.getElementById("continue-booking");
    const timerElement = document.getElementById("booking-timer");

    // Si no estamos en la pantalla de reservas, no ejecutar.
    if (!seatMap) return;

    const rows = Number(seatMap.dataset.rows) || 8;
    const columns = Number(seatMap.dataset.columns) || 12;

    const ticketPrice = 15.00;

    // Asientos ocupados de demostración.
    // Posteriormente estos datos vendrán del backend.
    const soldSeats = [
        "A3", "A4", "B7", "C2",
        "C3", "D9", "E5", "F6",
        "G10", "H1"
    ];

    // Asientos bloqueados temporalmente de demostración.
    const lockedSeats = [
        "A8", "B4", "D5", "F9"
    ];

    // Asientos seleccionados por el usuario.
    const selectedSeats = new Set();


    // =====================================
    // GENERAR MAPA DE ASIENTOS
    // =====================================

    function generateSeatMap() {

        seatMap.innerHTML = "";

        for (let row = 0; row < rows; row++) {

            const rowLabel = String.fromCharCode(65 + row);

            for (let column = 1; column <= columns; column++) {

                const seatCode = rowLabel + column;

                const seat = document.createElement("button");

                seat.type = "button";
                seat.classList.add("seat");
                seat.dataset.seat = seatCode;
                seat.textContent = column;

                if (soldSeats.includes(seatCode)) {

                    seat.classList.add("seat--sold");
                    seat.disabled = true;
                    seat.title = "Asiento vendido";

                } else if (lockedSeats.includes(seatCode)) {

                    seat.classList.add("seat--locked");
                    seat.disabled = true;
                    seat.title = "Asiento bloqueado";

                } else {

                    seat.classList.add("seat--available");
                    seat.title = "Asiento disponible";

                    seat.addEventListener("click", () => {
                        toggleSeat(seat);
                    });
                }

                seatMap.appendChild(seat);
            }
        }
    }


    // =====================================
    // SELECCIONAR / DESELECCIONAR ASIENTOS
    // =====================================

    function toggleSeat(seat) {

        const seatCode = seat.dataset.seat;

        if (seat.disabled) return;

        if (selectedSeats.has(seatCode)) {

            selectedSeats.delete(seatCode);

            seat.classList.remove("seat--selected");
            seat.classList.add("seat--available");

            seat.title = "Asiento disponible";

        } else {

            selectedSeats.add(seatCode);

            seat.classList.remove("seat--available");
            seat.classList.add("seat--selected");

            seat.title = "Asiento seleccionado";
        }

        updateSummary();
    }


    // =====================================
    // ACTUALIZAR RESUMEN DE RESERVA
    // =====================================

    function updateSummary() {

        selectedSeatsContainer.innerHTML = "";

        if (selectedSeats.size === 0) {

            const emptyMessage = document.createElement("span");

            emptyMessage.classList.add("selected-seats-empty");
            emptyMessage.textContent = "Aún no has seleccionado asientos";

            selectedSeatsContainer.appendChild(emptyMessage);

        } else {

            const orderedSeats = Array.from(selectedSeats).sort();

            orderedSeats.forEach(seatCode => {

                const tag = document.createElement("span");

                tag.classList.add("selected-seat-tag");
                tag.textContent = seatCode;

                selectedSeatsContainer.appendChild(tag);
            });
        }

        const total = selectedSeats.size * ticketPrice;

        ticketPriceElement.textContent = formatCurrency(ticketPrice);
        totalElement.textContent = formatCurrency(total);

        continueButton.disabled = selectedSeats.size === 0;
    }


    // =====================================
    // FORMATO DE MONEDA
    // =====================================

    function formatCurrency(amount) {

        return "S/ " + amount.toFixed(2);
    }


    // =====================================
    // TEMPORIZADOR VISUAL DE 5 MINUTOS
    // =====================================

    let remainingSeconds = 5 * 60;

    function updateTimer() {

        const minutes = Math.floor(remainingSeconds / 60);
        const seconds = remainingSeconds % 60;

        timerElement.textContent =
            String(minutes).padStart(2, "0") + ":" +
            String(seconds).padStart(2, "0");
    }

    if (timerElement) {

        updateTimer();

        const timerInterval = setInterval(() => {

            if (remainingSeconds > 0) {

                remainingSeconds--;
                updateTimer();

            } else {

                clearInterval(timerInterval);

                timerElement.textContent = "00:00";
                continueButton.disabled = true;

                alert("El tiempo de selección ha terminado.");
            }

        }, 1000);
    }


    // =====================================
    // BOTÓN CONTINUAR
    // =====================================

    continueButton.addEventListener("click", () => {

        if (selectedSeats.size === 0) return;

        const seats = Array.from(selectedSeats).sort();

        alert(
            "Asientos seleccionados: " + seats.join(", ") +
            "\nTotal: " + formatCurrency(seats.length * ticketPrice) +
            "\n\nEsta es una demostración visual. " +
            "La confirmación se conectará posteriormente con el backend."
        );
    });


    // =====================================
    // INICIAR INTERFAZ
    // =====================================

    generateSeatMap();
    updateSummary();

});
