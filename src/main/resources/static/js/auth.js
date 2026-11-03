document.querySelectorAll('[data-toggle-password]').forEach(function (button) {
  button.addEventListener('click', function () {
    var input = document.getElementById(button.dataset.togglePassword);
    var show = input.type === 'password';
    input.type = show ? 'text' : 'password';
    button.querySelector('.toggle-label').textContent = show ? 'Ocultar' : 'Ver';
  });
});