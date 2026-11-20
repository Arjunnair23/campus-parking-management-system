// auth.js - used by login.html and register.html

const TOKEN_KEY = "parkingToken";
const NAME_KEY = "parkingUserName";

function showAuthMsg(text, ok) {
  const el = document.getElementById("authMsg");
  el.textContent = text;
  el.className = "msg " + (ok ? "success" : "error");
}

// Already logged in? On the LOGIN page, skip straight to the dashboard.
// (The register page stays usable, so you can create another account.)
if (localStorage.getItem(TOKEN_KEY) && document.getElementById("loginForm")) {
  window.location.replace("index.html");
}

const loginForm = document.getElementById("loginForm");
if (loginForm) {
  loginForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    const username = document.getElementById("loginUsername").value.trim();
    const password = document.getElementById("loginPassword").value;

    try {
      const res = await fetch("/api/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, password })
      });
      const data = await res.json();
      if (data.success) {
        localStorage.setItem(TOKEN_KEY, data.token);
        localStorage.setItem(NAME_KEY, data.fullName);
        window.location.replace("index.html");
      } else {
        showAuthMsg(data.message || "Login failed.", false);
      }
    } catch (err) {
      showAuthMsg("Could not reach the server. Is it running?", false);
    }
  });
}

const registerForm = document.getElementById("registerForm");
if (registerForm) {
  registerForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    const fullName = document.getElementById("regFullName").value.trim();
    const username = document.getElementById("regUsername").value.trim();
    const password = document.getElementById("regPassword").value;
    const confirm = document.getElementById("regConfirm").value;

    if (password !== confirm) {
      showAuthMsg("Passwords do not match.", false);
      return;
    }

    try {
      const res = await fetch("/api/register", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ fullName, username, password })
      });
      const data = await res.json();
      if (data.success) {
        // Sign out of any previous session first, so the login page does not
        // skip straight back into the old account.
        const oldToken = localStorage.getItem(TOKEN_KEY);
        if (oldToken) {
          fetch("/api/logout", { method: "POST", headers: { Authorization: "Bearer " + oldToken } })
            .catch(() => {});
        }
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(NAME_KEY);
        showAuthMsg(data.message + " Redirecting to login...", true);
        setTimeout(() => window.location.replace("login.html"), 1200);
      } else {
        showAuthMsg(data.message || "Registration failed.", false);
      }
    } catch (err) {
      showAuthMsg("Could not reach the server. Is it running?", false);
    }
  });
}