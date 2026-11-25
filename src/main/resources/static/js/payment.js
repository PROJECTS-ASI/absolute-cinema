document.addEventListener("DOMContentLoaded", () => {
  const app = document.getElementById("payment-app");
  if (!app) return;

  const WARNING_THRESHOLD_SECONDS = 60;

  // Estado en memoria (nunca se persiste DNI, número ni CVV)
  const state = {
    bookingId: parsePositiveInt(app.dataset.bookingId),
    bookingStatus: (app.dataset.bookingStatus || "PENDING").toUpperCase(),
    total: parseDecimal(app.dataset.total),
    expiresAtMs: parseDateTimeMs(app.dataset.expiresAt),
    method: getSelectedMethodValue(),
    isExpired: false,
    isSubmitting: false,
    isApproved: false,
    touched: {
      cardHolder: false,
      cardDni: false,
      cardNumber: false,
      cardExpiry: false,
      cardCvv: false,
      manualAmount: false
    }
  };

  // Referencias del DOM
  const form = document.getElementById("payment-form");
  const submitBtn = document.getElementById("submit-payment-btn");
  const submitLabel = document.getElementById("submit-payment-label");
  const submitSpinner = document.getElementById("submit-payment-spinner");

  const methodRadios = Array.from(document.querySelectorAll('input[name="paymentMethod"]'));
  const methodOptions = Array.from(document.querySelectorAll(".method-option"));
  const cardPanel = document.getElementById("card-method-panel");
  const manualPanel = document.getElementById("manual-method-panel");

  const cardHolderInput = document.getElementById("card-holder");
  const cardDniInput = document.getElementById("card-dni");
  const cardNumberInput = document.getElementById("card-number");
  const cardExpiryInput = document.getElementById("card-expiry");
  const cardCvvInput = document.getElementById("card-cvv");
  const cardCvvLabel = document.getElementById("card-cvv-label");
  const cardBrandBadge = document.getElementById("card-brand-badge");

  const manualAmountInput = document.getElementById("manual-amount");

  const cardHolderError = document.getElementById("card-holder-error");
  const cardDniError = document.getElementById("card-dni-error");
  const cardNumberError = document.getElementById("card-number-error");
  const cardExpiryError = document.getElementById("card-expiry-error");
  const cardCvvError = document.getElementById("card-cvv-error");
  const manualAmountError = document.getElementById("manual-amount-error");

  const timerBox = document.getElementById("booking-timer-box");
  const countdownDisplay = document.getElementById("countdown-display");
  const expiringWarning = document.getElementById("expiring-warning");
  const expiredBanner = document.getElementById("expired-banner");
  const expiredMessage = document.getElementById("expired-message");

  const resultBox = document.getElementById("payment-result-box");
  const resultTitle = document.getElementById("payment-result-title");
  const resultMessage = document.getElementById("payment-result-message");
  const retryPaymentBtn = document.getElementById("retry-payment-btn");
  const resultCatalogBtn = document.getElementById("result-catalog-btn");

  const successBox = document.getElementById("payment-success-box");
  const successMessage = document.getElementById("payment-success-message");

  // ---------- Utilidades de formato y parsing ----------
  function parsePositiveInt(val) {
    const num = Number.parseInt(val, 10);
    return Number.isFinite(num) && num > 0 ? num : null;
  }

  function parseDecimal(val) {
    if (val === undefined || val === null || String(val).trim() === "") return null;
    const normalized = String(val).replace(",", ".").trim();
    const num = Number(normalized);
    return Number.isFinite(num) ? Math.round(num * 100) / 100 : null;
  }

  function parseDateTimeMs(raw) {
    if (!raw || typeof raw !== "string") return null;
    const trimmed = raw.trim();
    const parsed = Date.parse(trimmed);
    if (!Number.isNaN(parsed)) return parsed;

    const match = trimmed.match(/^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})(?::(\d{2}))?/);
    if (!match) return null;
    const localDate = new Date(
      Number(match[1]),
      Number(match[2]) - 1,
      Number(match[3]),
      Number(match[4]),
      Number(match[5]),
      Number(match[6] || "0")
    );
    const ms = localDate.getTime();
    return Number.isNaN(ms) ? null : ms;
  }

  function formatMoney(amount) {
    const safe = typeof amount === "number" && Number.isFinite(amount) ? amount : 0;
    return `S/ ${safe.toFixed(2)}`;
  }

  function getSelectedMethodValue() {
    const checked = document.querySelector('input[name="paymentMethod"]:checked');
    return checked ? checked.value : "CARD";
  }

  // ---------- Resumen dinámico ----------
  function renderSummary() {
    const posterUrl = app.dataset.posterUrl;
    const posterEl = document.getElementById("summary-poster");
    if (posterEl && posterUrl) {
      posterEl.src = posterUrl.startsWith("/") ? posterUrl : `/${posterUrl}`;
    }

    setText("summary-movie-title", app.dataset.movieTitle || "Película no disponible");
    const genreOrRating = app.dataset.genre || app.dataset.rating || "General";
    setText("summary-genre", genreOrRating);
    setText("summary-duration", app.dataset.duration ? ` · ${app.dataset.duration} min` : "");
    setText("summary-format", app.dataset.format || "2D");

    const startsAtMs = parseDateTimeMs(app.dataset.startsAt);
    if (startsAtMs !== null) {
      const d = new Date(startsAtMs);
      setText("summary-date", d.toLocaleDateString("es-PE", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric"
      }));
      setText("summary-time", d.toLocaleTimeString("es-PE", {
        hour: "2-digit",
        minute: "2-digit",
        hour12: false
      }));
    }

    setText("summary-room", app.dataset.roomName || "Sala");

    const rawSeats = (app.dataset.seats || "").trim();
    const seatList = rawSeats ? rawSeats.split(",").map(s => s.trim()).filter(Boolean) : [];
    setText("summary-seats", seatList.length ? seatList.join(", ") : "Sin asientos");

    const parsedCount = parsePositiveInt(app.dataset.ticketCount);
    const count = parsedCount !== null ? parsedCount : seatList.length;
    setText("summary-quantity", String(count));

    const unitPrice = parseDecimal(app.dataset.unitPrice);
    setText("summary-unit-price", formatMoney(unitPrice !== null ? unitPrice : 0));

    if (state.total !== null) {
      setText("summary-total", formatMoney(state.total));
    }
  }

  function setText(id, value) {
    const el = document.getElementById(id);
    if (el) el.textContent = value;
  }

  // ---------- Contador de vencimiento ----------
  function updateCountdown() {
    if (state.isApproved) return;

    if (state.bookingStatus !== "PENDING") {
      markBookingExpired("Esta reserva ya no se encuentra pendiente de pago.");
      return;
    }

    if (state.expiresAtMs === null) {
      countdownDisplay.textContent = "--:--";
      return;
    }

    const remainingMs = state.expiresAtMs - Date.now();
    const totalSeconds = Math.max(0, Math.floor(remainingMs / 1000));
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;

    countdownDisplay.textContent = `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;

    timerBox.classList.remove("is-warning", "is-expired");
    if (totalSeconds <= 0) {
      timerBox.classList.add("is-expired");
      markBookingExpired("El tiempo de reserva ha llegado a cero y los asientos ya no están bloqueados.");
      return;
    }

    if (totalSeconds <= WARNING_THRESHOLD_SECONDS) {
      timerBox.classList.add("is-warning");
      expiringWarning.hidden = false;
    } else {
      expiringWarning.hidden = true;
    }
  }

  function markBookingExpired(reason) {
    if (state.isExpired) return;
    state.isExpired = true;
    expiringWarning.hidden = true;
    expiredBanner.hidden = false;
    if (reason && expiredMessage) {
      expiredMessage.textContent = reason;
    }
    syncFormAvailability();
  }

  // ---------- Validaciones de tarjeta y método ----------
  function detectCardBrand(digitsOnly) {
    if (!digitsOnly) return "UNKNOWN";
    if (/^4/.test(digitsOnly)) return "VISA";
    if (/^3[47]/.test(digitsOnly)) return "AMEX";
    return "UNKNOWN";
  }

  function formatCardNumber(digitsOnly, brand) {
    if (brand === "AMEX") {
      const p1 = digitsOnly.slice(0, 4);
      const p2 = digitsOnly.slice(4, 10);
      const p3 = digitsOnly.slice(10, 15);
      return [p1, p2, p3].filter(Boolean).join(" ");
    }
    const groups = digitsOnly.slice(0, 16).match(/.{1,4}/g);
    return groups ? groups.join(" ") : "";
  }

  function passesLuhn(digitsOnly) {
    if (!/^\d{13,19}$/.test(digitsOnly)) return false;
    let sum = 0;
    let shouldDouble = false;
    for (let i = digitsOnly.length - 1; i >= 0; i--) {
      let digit = Number(digitsOnly.charAt(i));
      if (shouldDouble) {
        digit *= 2;
        if (digit > 9) digit -= 9;
      }
      sum += digit;
      shouldDouble = !shouldDouble;
    }
    return sum % 10 === 0;
  }

  function expectedCardDigitsLength(brand) {
    if (brand === "AMEX") return 15;
    if (brand === "VISA") return 16;
    return 16;
  }

  function expectedCvvLength(brand) {
    return brand === "AMEX" ? 4 : 3;
  }

  function updateCardBrandUI() {
    const digits = cardNumberInput.value.replace(/\D/g, "");
    const brand = detectCardBrand(digits);
    const cvvLen = expectedCvvLength(brand);

    cardCvvInput.maxLength = cvvLen;
    cardCvvInput.placeholder = brand === "AMEX" ? "1234" : "123";
    cardCvvLabel.textContent = brand === "AMEX" ? "CVV (4 dígitos)" : "CVV (3 dígitos)";

    cardBrandBadge.classList.remove("is-visa", "is-amex", "is-invalid", "is-unknown");
    if (!digits) {
      cardBrandBadge.textContent = "Marca no identificada";
      cardBrandBadge.classList.add("is-unknown");
    } else if (brand === "VISA") {
      cardBrandBadge.textContent = "Visa";
      cardBrandBadge.classList.add("is-visa");
    } else if (brand === "AMEX") {
      cardBrandBadge.textContent = "American Express";
      cardBrandBadge.classList.add("is-amex");
    } else {
      cardBrandBadge.textContent = "Marca desconocida";
      cardBrandBadge.classList.add("is-invalid");
    }
  }

  function validateCardHolder(showError) {
    const raw = cardHolderInput.value;
    const trimmed = raw.trim();
    const isValid = trimmed.length >= 2;
    let msg = "";
    if (!isValid) {
      msg = !raw ? "El titular de la tarjeta es obligatorio." : "Ingresa un nombre válido (no solo espacios).";
    }
    setFieldValidation(cardHolderInput, cardHolderError, isValid, msg, showError);
    return isValid;
  }

  function validateCardDni(showError) {
    const digits = cardDniInput.value;
    const isValid = /^\d{8}$/.test(digits);
    const msg = isValid ? "" : "El DNI debe contener exactamente 8 dígitos numéricos.";
    setFieldValidation(cardDniInput, cardDniError, isValid, msg, showError);
    return isValid;
  }

  function validateCardNumber(showError) {
    const digits = cardNumberInput.value.replace(/\D/g, "");
    const brand = detectCardBrand(digits);
    const requiredLen = expectedCardDigitsLength(brand);

    let isValid = false;
    let msg = "";

    if (!digits) {
      msg = "El número de tarjeta es obligatorio.";
    } else if (brand === "UNKNOWN") {
      msg = "Marca desconocida. Solo se admiten tarjetas Visa o American Express.";
    } else if (digits.length !== requiredLen) {
      msg = brand === "AMEX"
        ? "American Express debe tener 15 dígitos."
        : "Visa debe tener 16 dígitos.";
    } else if (!passesLuhn(digits)) {
      msg = "El número de tarjeta no supera la validación Luhn.";
    } else {
      isValid = true;
    }

    setFieldValidation(cardNumberInput, cardNumberError, isValid, msg, showError);
    return isValid;
  }

  function validateCardExpiry(showError) {
    const value = cardExpiryInput.value.trim();
    const match = value.match(/^(0[1-9]|1[0-2])\/(\d{2})$/);
    if (!match) {
      setFieldValidation(
        cardExpiryInput,
        cardExpiryError,
        false,
        "Ingresa una fecha válida en formato MM/AA.",
        showError
      );
      return false;
    }

    const month = Number(match[1]);
    const year = 2000 + Number(match[2]);
    const now = new Date();
    const currentYear = now.getFullYear();
    const currentMonth = now.getMonth() + 1;

    const isExpiredCard = year < currentYear || (year === currentYear && month < currentMonth);
    if (isExpiredCard) {
      setFieldValidation(
        cardExpiryInput,
        cardExpiryError,
        false,
        "La tarjeta ingresada se encuentra vencida.",
        showError
      );
      return false;
    }

    setFieldValidation(cardExpiryInput, cardExpiryError, true, "", showError);
    return true;
  }

  function validateCardCvv(showError) {
    const digits = cardNumberInput.value.replace(/\D/g, "");
    const brand = detectCardBrand(digits);
    const requiredLen = expectedCvvLength(brand);
    const cvv = cardCvvInput.value;
    const regex = new RegExp(`^\\d{${requiredLen}}$`);
    const isValid = regex.test(cvv);
    const msg = isValid ? "" : `El CVV debe tener ${requiredLen} dígitos para ${brand === "AMEX" ? "American Express" : "Visa"}.`;
    setFieldValidation(cardCvvInput, cardCvvError, isValid, msg, showError);
    return isValid;
  }

  function validateManualAmount(showError) {
    const raw = manualAmountInput.value.trim();
    if (!raw) {
      setFieldValidation(manualAmountInput, manualAmountError, false, "Ingresa el importe a pagar.", showError);
      return false;
    }
    if (!/^\d+(\.\d{1,2})?$/.test(raw)) {
      setFieldValidation(
        manualAmountInput,
        manualAmountError,
        false,
        "Ingresa un importe numérico válido (hasta 2 decimales).",
        showError
      );
      return false;
    }

    const amount = Number(raw);
    if (!Number.isFinite(amount) || amount <= 0) {
      setFieldValidation(manualAmountInput, manualAmountError, false, "El importe debe ser mayor a cero.", showError);
      return false;
    }

    if (state.total === null || Math.abs(amount - state.total) > 0.001) {
      setFieldValidation(
        manualAmountInput,
        manualAmountError,
        false,
        `El importe debe coincidir exactamente con el total de la reserva (${formatMoney(state.total || 0)}).`,
        showError
      );
      return false;
    }

    setFieldValidation(manualAmountInput, manualAmountError, true, "", showError);
    return true;
  }

  function setFieldValidation(inputEl, errorEl, isValid, message, showError) {
    inputEl.classList.remove("is-valid", "is-invalid");
    if (showError) {
      if (isValid) {
        inputEl.classList.add("is-valid");
        errorEl.textContent = "";
      } else {
        inputEl.classList.add("is-invalid");
        errorEl.textContent = message;
      }
    } else {
      errorEl.textContent = "";
      if (isValid && inputEl.value.trim().length > 0) {
        inputEl.classList.add("is-valid");
      }
    }
  }

  function areAllFieldsValid(showErrors) {
    if (state.method === "MANUAL_AMOUNT") {
      return validateManualAmount(showErrors);
    }
    const vHolder = validateCardHolder(showErrors);
    const vDni = validateCardDni(showErrors);
    const vNum = validateCardNumber(showErrors);
    const vExp = validateCardExpiry(showErrors);
    const vCvv = validateCardCvv(showErrors);
    return vHolder && vDni && vNum && vExp && vCvv;
  }

  function syncFormAvailability() {
    const blocked = state.isExpired || state.isSubmitting || state.isApproved || state.bookingId === null;
    submitBtn.disabled = blocked;

    const controls = form.querySelectorAll("input, button[type='submit']");
    controls.forEach(ctrl => {
      if (ctrl === submitBtn) {
        ctrl.disabled = blocked;
      } else {
        ctrl.disabled = state.isExpired || state.isSubmitting || state.isApproved;
      }
    });

    submitLabel.hidden = state.isSubmitting;
    submitSpinner.hidden = !state.isSubmitting;
  }

  function switchPaymentMethod(newMethod) {
    state.method = newMethod === "MANUAL_AMOUNT" ? "MANUAL_AMOUNT" : "CARD";
    methodOptions.forEach(opt => {
      opt.classList.toggle("is-selected", opt.dataset.method === state.method);
    });
    cardPanel.hidden = state.method !== "CARD";
    manualPanel.hidden = state.method !== "MANUAL_AMOUNT";
    clearResultAlert();
    areAllFieldsValid(false);
  }

  function clearResultAlert() {
    resultBox.hidden = true;
    resultMessage.textContent = "";
    retryPaymentBtn.hidden = true;
    resultCatalogBtn.hidden = true;
  }

  function showRejectedPayment(message) {
    successBox.hidden = true;
    resultBox.hidden = false;
    resultTitle.textContent = "Pago rechazado";
    resultMessage.textContent = message || "No se pudo aprobar el pago. Verifica los datos e intenta nuevamente.";
    retryPaymentBtn.hidden = state.isExpired;
    resultCatalogBtn.hidden = !state.isExpired;
  }

  function clearSensitiveCardData() {
    cardHolderInput.value = "";
    cardDniInput.value = "";
    cardNumberInput.value = "";
    cardExpiryInput.value = "";
    cardCvvInput.value = "";
    updateCardBrandUI();
  }

  // ---------- Envío al backend (POST /payments) ----------
  async function handleSubmit(event) {
    event.preventDefault();
    if (state.isSubmitting || state.isApproved) return;

    updateCountdown();
    if (state.isExpired) return;

    if (state.method === "MANUAL_AMOUNT") {
      state.touched.manualAmount = true;
    } else {
      state.touched.cardHolder = true;
      state.touched.cardDni = true;
      state.touched.cardNumber = true;
      state.touched.cardExpiry = true;
      state.touched.cardCvv = true;
    }

    if (!areAllFieldsValid(true)) {
      return;
    }

    const amountToSend = state.method === "MANUAL_AMOUNT"
      ? Number(manualAmountInput.value.trim())
      : state.total;

    const detectedBrand = state.method === "CARD"
      ? detectCardBrand(cardNumberInput.value.replace(/\D/g, ""))
      : null;

    // Nunca se envía DNI, número de tarjeta ni CVV al backend
    const payload = {
      bookingId: state.bookingId,
      method: state.method,
      amount: amountToSend,
      cardBrand: detectedBrand === "VISA" || detectedBrand === "AMEX" ? detectedBrand : null
    };

    const headers = {
      "Content-Type": "application/json",
      "Accept": "application/json"
    };
    const csrfToken = app.dataset.csrfToken;
    const csrfHeader = app.dataset.csrfHeader;
    if (csrfToken && csrfHeader) {
      headers[csrfHeader] = csrfToken;
    }

    state.isSubmitting = true;
    clearResultAlert();
    syncFormAvailability();

    try {
      const response = await fetch("/payments", {
        method: "POST",
        headers,
        body: JSON.stringify(payload)
      });

      const contentType = response.headers.get("content-type") || "";
      let data = null;
      if (contentType.includes("application/json")) {
        data = await response.json().catch(() => null);
      } else {
        const text = await response.text().catch(() => "");
        if (text) {
          try {
            data = JSON.parse(text);
          } catch {
            data = { message: text };
          }
        }
      }

      if (!response.ok) {
        const backendMsg = (data && (data.message || data.reason || data.error)) || "";
        const normalizedMsg = String(backendMsg).toLowerCase();

        if (normalizedMsg.includes("vencid") || normalizedMsg.includes("expir")) {
          markBookingExpired(backendMsg || "La reserva ha vencido.");
          return;
        }

        if (response.status === 404) {
          showRejectedPayment(backendMsg || "No se encontró la reserva asociada al pago.");
          return;
        }

        if (response.status === 400) {
          showRejectedPayment(backendMsg || "El pago fue rechazado. Revisa el importe y la vigencia de la reserva.");
          return;
        }

        if (response.status >= 500) {
          showRejectedPayment("Ocurrió un inconveniente temporal en el servidor. Intenta nuevamente en unos segundos.");
          return;
        }

        showRejectedPayment(backendMsg || "No se pudo completar el pago.");
        return;
      }

      if (data && String(data.status).toUpperCase() === "REJECTED") {
        showRejectedPayment(data.rejectionReason || data.message || "El pago fue rechazado por el sistema.");
        return;
      }

      state.isApproved = true;
      clearSensitiveCardData();
      successBox.hidden = false;
      successMessage.textContent = "Pago aprobado correctamente. Redirigiendo a la confirmación...";

      const confirmUrl = app.dataset.confirmationUrl || `/bookings/${state.bookingId}/confirmation`;
      window.setTimeout(() => {
        window.location.assign(confirmUrl);
      }, 500);

    } catch {
      showRejectedPayment("No hubo conexión con el servidor. Verifica tu red e inténtalo de nuevo.");
    } finally {
      if (!state.isApproved) {
        state.isSubmitting = false;
        syncFormAvailability();
      }
    }
  }

  // ---------- Listeners ----------
  methodRadios.forEach(radio => {
    radio.addEventListener("change", () => {
      switchPaymentMethod(radio.value);
    });
  });

  cardHolderInput.addEventListener("input", () => {
    state.touched.cardHolder = true;
    validateCardHolder(true);
  });

  cardDniInput.addEventListener("input", () => {
    const cleaned = cardDniInput.value.replace(/\D/g, "").slice(0, 8);
    if (cardDniInput.value !== cleaned) {
      cardDniInput.value = cleaned;
    }
    state.touched.cardDni = true;
    validateCardDni(true);
  });

  cardNumberInput.addEventListener("input", () => {
    const digits = cardNumberInput.value.replace(/\D/g, "");
    const brand = detectCardBrand(digits);
    const maxDigits = expectedCardDigitsLength(brand);
    const limited = digits.slice(0, maxDigits);
    const formatted = formatCardNumber(limited, brand);

    if (cardNumberInput.value !== formatted) {
      cardNumberInput.value = formatted;
    }

    updateCardBrandUI();
    state.touched.cardNumber = true;
    validateCardNumber(true);

    if (state.touched.cardCvv && cardCvvInput.value.length > 0) {
      validateCardCvv(true);
    }
  });

  cardExpiryInput.addEventListener("input", () => {
    const digits = cardExpiryInput.value.replace(/\D/g, "").slice(0, 4);
    const formatted = digits.length > 2 ? `${digits.slice(0, 2)}/${digits.slice(2)}` : digits;
    if (cardExpiryInput.value !== formatted) {
      cardExpiryInput.value = formatted;
    }
    state.touched.cardExpiry = true;
    validateCardExpiry(true);
  });

  cardCvvInput.addEventListener("input", () => {
    const digits = cardNumberInput.value.replace(/\D/g, "");
    const brand = detectCardBrand(digits);
    const maxCvv = expectedCvvLength(brand);
    const cleaned = cardCvvInput.value.replace(/\D/g, "").slice(0, maxCvv);
    if (cardCvvInput.value !== cleaned) {
      cardCvvInput.value = cleaned;
    }
    state.touched.cardCvv = true;
    validateCardCvv(true);
  });

  manualAmountInput.addEventListener("input", () => {
    let cleaned = manualAmountInput.value.replace(/,/g, ".").replace(/[^0-9.]/g, "");
    const parts = cleaned.split(".");
    if (parts.length > 2) {
      cleaned = `${parts[0]}.${parts.slice(1).join("")}`;
    }
    if (parts.length === 2 && parts[1].length > 2) {
      cleaned = `${parts[0]}.${parts[1].slice(0, 2)}`;
    }
    if (manualAmountInput.value !== cleaned) {
      manualAmountInput.value = cleaned;
    }
    state.touched.manualAmount = true;
    validateManualAmount(true);
  });

  retryPaymentBtn.addEventListener("click", () => {
    clearResultAlert();
    if (state.method === "CARD") {
      cardHolderInput.focus();
    } else {
      manualAmountInput.focus();
    }
  });

  form.addEventListener("submit", handleSubmit);

  // Inicialización
  renderSummary();
  updateCardBrandUI();
  switchPaymentMethod(state.method);
  updateCountdown();
  syncFormAvailability();

  const timerInterval = window.setInterval(() => {
    updateCountdown();
    if (state.isExpired || state.isApproved) {
      window.clearInterval(timerInterval);
    }
  }, 1000);
});