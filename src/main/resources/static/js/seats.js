// Solo visual: resumen y total. El envío real lo hace el formulario HTML (campo seatIds).
document.addEventListener("DOMContentLoaded", () => {
  const form = document.getElementById("seat-form");
  if (!form) return;

  const price = parseFloat(form.dataset.price) || 0;
  const max = parseInt(form.dataset.max, 10) || 8;
  const boxes = Array.from(form.querySelectorAll('input[name="seatIds"]'));
  const list = document.getElementById("seat-list");
  const total = document.getElementById("seat-total");
  const submit = document.getElementById("seat-submit");
  const limit = document.getElementById("seat-limit");

  function update() {
    const chosen = boxes.filter(b => b.checked);
    const labels = chosen.map(b => b.closest(".seat").title);
    list.textContent = labels.length ? labels.join(", ") : "Aún no elegiste asientos.";
    total.textContent = "S/ " + (chosen.length * price).toFixed(2);
    submit.disabled = chosen.length === 0;
  }

  boxes.forEach(box => box.addEventListener("change", () => {
    const count = boxes.filter(b => b.checked).length;
    limit.hidden = count <= max;
    if (count > max) {           // pasó el máximo: deshacer este clic
      box.checked = false;
      limit.hidden = false;
    }
    update();
  }));

  // Al volver con "atrás" el navegador puede conservar marcas: recalcular siempre
  window.addEventListener("pageshow", update);
  update();
});