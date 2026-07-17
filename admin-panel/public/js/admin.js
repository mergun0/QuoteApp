document.addEventListener("click", (event) => {
  const toggle = event.target.closest("[data-sidebar-toggle]");
  if (toggle) {
    document.getElementById("sidebar")?.classList.toggle("open");
  }

  const passwordToggle = event.target.closest("[data-toggle-password]");
  if (passwordToggle) {
    const input = document.getElementById("password");
    if (input) {
      input.type = input.type === "password" ? "text" : "password";
      passwordToggle.textContent = input.type === "password" ? "Göster" : "Gizle";
    }
  }
});

document.addEventListener("submit", (event) => {
  const button = event.submitter;
  if (button && button.dataset.confirm && !window.confirm(button.dataset.confirm)) {
    event.preventDefault();
    return;
  }
  if (event.target.matches("[data-submit-lock]") && button) {
    button.disabled = true;
    button.textContent = "İşleniyor...";
  }
});
