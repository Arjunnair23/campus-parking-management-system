// ---------------- Login guard ----------------
// No token -> go to the login page. Every API call carries the token,
// and a 401 from the server (expired/invalid login) sends us back to login.
const TOKEN_KEY = "parkingToken";
const NAME_KEY = "parkingUserName";
const TOKEN = localStorage.getItem(TOKEN_KEY);

function goToLogin() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(NAME_KEY);
  window.location.replace("login.html");
}

if (!TOKEN) goToLogin();

const _fetch = window.fetch.bind(window);
window.fetch = async (url, opts = {}) => {
  opts.headers = Object.assign({}, opts.headers, { Authorization: "Bearer " + TOKEN });
  const res = await _fetch(url, opts);
  if (res.status === 401) goToLogin();
  return res;
};

// Shows "Full Name (@username)". The @username part always comes from the server.
function userLabel(fullName, username) {
  if (!username) return fullName || "";
  if (!fullName || fullName.toLowerCase() === username.toLowerCase()) return "@" + username;
  return `${fullName} (@${username})`;
}

// Show the saved name immediately, then ask the server who is really logged in
// (the saved name could be stale) and correct it.
document.getElementById("userName").textContent = localStorage.getItem(NAME_KEY) || "";
fetch("/api/me")
  .then(r => r.json())
  .then(me => {
    if (me && me.username) {
      localStorage.setItem(NAME_KEY, me.fullName || me.username);
      document.getElementById("userName").textContent = userLabel(me.fullName, me.username);
    }
  })
  .catch(() => { /* keep the saved name if the server cannot be reached */ });
document.getElementById("logoutBtn").addEventListener("click", async () => {
  try { await fetch("/api/logout", { method: "POST" }); } catch (e) { /* ignore */ }
  goToLogin();
});

// script.js
// Talks to the Java backend (ApiServer.java) over relative paths,
// since the frontend is served by the same server -> no CORS issues.

const API = ""; // same-origin, relative paths are enough

async function refreshAll() {
  await Promise.all([refreshStatus(), refreshParked(), refreshQueue(), refreshExits(), loadTree()]);
}

async function refreshStatus() {
  try {
    const res = await fetch(`${API}/api/status`);
    const data = await res.json();
    document.getElementById("occupiedCount").textContent = data.occupied;
    document.getElementById("totalCount").textContent = data.totalSlots;
    document.getElementById("waitingCount").textContent = data.waiting;
  } catch (e) {
    console.error("Status fetch failed", e);
  }
}

async function refreshParked() {
  const res = await fetch(`${API}/api/parked`);
  const vehicles = await res.json();
  const body = document.getElementById("parkedTableBody");
  document.getElementById("parkedTableCount").textContent = `(${vehicles.length})`;

  if (vehicles.length === 0) {
    body.innerHTML = `<tr class="empty-row"><td colspan="5">No vehicles currently parked.</td></tr>`;
    return;
  }
  body.innerHTML = vehicles.map(v => `
    <tr>
      <td>${v.slotNumber}</td>
      <td>${escapeHtml(v.vehicleNumber)}</td>
      <td>${escapeHtml(v.ownerName)}</td>
      <td>${escapeHtml(v.vehicleType)}</td>
      <td>${escapeHtml(v.entryTime)}</td>
    </tr>
  `).join("");
}

async function refreshQueue() {
  const res = await fetch(`${API}/api/queue`);
  const vehicles = await res.json();
  const body = document.getElementById("queueTableBody");
  document.getElementById("queueTableCount").textContent = `(${vehicles.length})`;

  if (vehicles.length === 0) {
    body.innerHTML = `<tr class="empty-row"><td colspan="4">Waiting queue is empty.</td></tr>`;
    return;
  }
  body.innerHTML = vehicles.map(v => `
    <tr>
      <td>${escapeHtml(v.vehicleNumber)}</td>
      <td>${escapeHtml(v.ownerName)}</td>
      <td>${escapeHtml(v.vehicleType)}</td>
      <td>${escapeHtml(v.entryTime)}</td>
    </tr>
  `).join("");
}

async function refreshExits() {
  const res = await fetch(`${API}/api/exits`);
  const vehicles = await res.json();
  const body = document.getElementById("exitTableBody");
  document.getElementById("exitTableCount").textContent = `(${vehicles.length})`;

  if (vehicles.length === 0) {
    body.innerHTML = `<tr class="empty-row"><td colspan="4">No exits yet.</td></tr>`;
    return;
  }
  body.innerHTML = vehicles.map(v => `
    <tr>
      <td>${escapeHtml(v.vehicleNumber)}</td>
      <td>${escapeHtml(v.ownerName)}</td>
      <td>${escapeHtml(v.vehicleType)}</td>
      <td>${v.slotNumber}</td>
    </tr>
  `).join("");
}

function escapeHtml(str) {
  const div = document.createElement("div");
  div.textContent = str ?? "";
  return div.innerHTML;
}

function showMsg(elId, text, ok) {
  const el = document.getElementById(elId);
  el.textContent = text;
  el.className = "msg " + (ok ? "success" : "error");
}

// ---------------- Form handlers ----------------

document.getElementById("entryForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const vehicleNumber = document.getElementById("entryVehicleNumber").value.trim();
  const ownerName = document.getElementById("entryOwnerName").value.trim();
  const vehicleType = document.getElementById("entryVehicleType").value;

  const res = await fetch(`${API}/api/entry`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ vehicleNumber, ownerName, vehicleType })
  });
  const data = await res.json();
  showMsg("entryMsg", data.message, data.success);
  if (data.success) e.target.reset();
  refreshAll();
});

document.getElementById("exitForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const vehicleNumber = document.getElementById("exitVehicleNumber").value.trim();

  const res = await fetch(`${API}/api/exit`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ vehicleNumber })
  });
  const data = await res.json();
  showMsg("exitMsg", data.message, data.success);
  if (data.success) e.target.reset();
  refreshAll();
});

document.getElementById("searchForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const vehicleNumber = document.getElementById("searchVehicleNumber").value.trim();

  const res = await fetch(`${API}/api/search?vehicleNumber=${encodeURIComponent(vehicleNumber)}`);
  const data = await res.json();
  if (data.found) {
    const v = data.vehicle;
    showMsg("searchMsg", `Found: Slot ${v.slotNumber} - ${v.ownerName} (${v.vehicleType})`, true);
  } else {
    showMsg("searchMsg", "Vehicle not found among parked vehicles.", false);
  }
});

// ---------------- Phase 2: BST + Graph ----------------

function slotLabel(v) {
  return `S${v.slotNumber}: ${v.vehicleNumber}`;
}

async function loadTree() {
  const res = await fetch(`${API}/api/tree`);
  const t = await res.json();
  document.getElementById("treeInorder").textContent =
    t.inorder.map(slotLabel).join("  →  ") || "(empty)";
  document.getElementById("treePreorder").textContent =
    t.preorder.map(slotLabel).join("  →  ") || "(empty)";
  document.getElementById("treePostorder").textContent =
    t.postorder.map(slotLabel).join("  →  ") || "(empty)";
}

async function loadGraph(start) {
  const res = await fetch(`${API}/api/graph?start=${encodeURIComponent(start || "Entrance")}`);
  const g = await res.json();
  document.getElementById("graphBfs").textContent = g.bfs.length
    ? g.bfs.join("  →  ")
    : "Zone not found.";
  document.getElementById("graphAdj").innerHTML = Object.entries(g.adjacency)
    .map(([zone, nbrs]) => `<li><b>${escapeHtml(zone)}</b> — ${nbrs.map(escapeHtml).join(", ")}</li>`)
    .join("");
}

document.getElementById("slotForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const slot = document.getElementById("slotInput").value.trim();
  const res = await fetch(`${API}/api/slot?slot=${encodeURIComponent(slot)}`);
  const data = await res.json();
  if (data.found) {
    const v = data.vehicle;
    showMsg("slotMsg", `Slot ${v.slotNumber}: ${v.vehicleNumber} - ${v.ownerName} (${v.vehicleType})`, true);
  } else {
    showMsg("slotMsg", `No vehicle at slot ${slot}.`, false);
  }
});

document.getElementById("graphForm").addEventListener("submit", (e) => {
  e.preventDefault();
  loadGraph(document.getElementById("graphStart").value);
});

// ---------------- Phase 3: sorting ----------------

async function runSort() {
  const algo = document.getElementById("sortAlgo").value;
  const by = document.getElementById("sortBy").value;
  const res = await fetch(`${API}/api/sort?algo=${encodeURIComponent(algo)}&by=${encodeURIComponent(by)}`);
  const r = await res.json();

  document.getElementById("sortSummary").textContent =
    `${r.algorithm} by ${r.by}: ${r.comparisons} comparisons, ${r.micros} µs`;

  const body = document.getElementById("sortTableBody");
  if (r.vehicles.length === 0) {
    body.innerHTML = `<tr class="empty-row"><td colspan="6">No vehicles to sort.</td></tr>`;
    return;
  }
  body.innerHTML = r.vehicles.map((v, i) => `
    <tr>
      <td>${i + 1}</td>
      <td>${v.slotNumber}</td>
      <td>${escapeHtml(v.vehicleNumber)}</td>
      <td>${escapeHtml(v.ownerName)}</td>
      <td>${escapeHtml(v.vehicleType)}</td>
      <td>${escapeHtml(v.entryTime)}</td>
    </tr>
  `).join("");
}

document.getElementById("sortForm").addEventListener("submit", (e) => {
  e.preventDefault();
  runSort();
});

// ---------------- Phase 3: searching ----------------

document.getElementById("findForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const number = document.getElementById("findNumber").value.trim();
  const algo = document.getElementById("findAlgo").value;
  const stats = document.getElementById("findStats");

  const res = await fetch(`${API}/api/find?algo=${encodeURIComponent(algo)}&number=${encodeURIComponent(number)}`);
  const r = await res.json();

  if (!r.success) {
    showMsg("findMsg", r.message, false);
    stats.textContent = "";
    return;
  }
  if (r.found) {
    const v = r.vehicle;
    showMsg("findMsg",
      `Found at position ${r.position} of ${r.total}: Slot ${v.slotNumber} - ${v.vehicleNumber} - ${v.ownerName} (${v.vehicleType})`,
      true);
  } else {
    showMsg("findMsg", `Not found among ${r.total} parked vehicles.`, false);
  }
  let text = `${r.algorithm}: ${r.comparisons} comparisons, ${r.micros} µs`;
  if (r.prepComparisons > 0) text += ` (plus ${r.prepComparisons} comparisons to sort first)`;
  stats.textContent = text;
});

// ---------------- Phase 3C: comparison ----------------

function fmt(n) { return Number(n).toLocaleString(); }

document.getElementById("compareForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const note = document.getElementById("compareNote");
  note.textContent = "Running...";
  const res = await fetch(`${API}/api/compare`);
  const data = await res.json();
  note.textContent = "Comparison counts are exact; times vary a little between runs.";

  document.getElementById("batchHead").innerHTML = `${data.lookups} lookups:<br>linear`;
  document.getElementById("compareBody").innerHTML = data.rows.map(r => {
    const binaryWins = r.batchSortBinary < r.batchLinear;
    return `
    <tr>
      <td>${fmt(r.n)}</td>
      <td>${fmt(r.mergeCmp)} (${fmt(Math.round(r.mergeMicros))})</td>
      <td>${fmt(r.insertionCmp)} (${fmt(Math.round(r.insertionMicros))})</td>
      <td>${r.linearAvg}</td>
      <td>${r.binaryAvg}</td>
      <td>${fmt(r.batchLinear)}</td>
      <td>${fmt(r.batchSortBinary)}</td>
      <td>${binaryWins ? "sort + binary" : "linear"}</td>
    </tr>`;
  }).join("");
  document.getElementById("compareWrap").style.display = "block";
});

document.getElementById("batchForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const numbers = document.getElementById("batchNumbers").value;
  const res = await fetch(`${API}/api/batch?numbers=${encodeURIComponent(numbers)}`);
  const b = await res.json();

  const wrap = document.getElementById("batchWrap");
  if (!b.success) {
    showMsg("batchMsg", b.message, false);
    wrap.style.display = "none";
    document.getElementById("batchTotals").textContent = "";
    return;
  }
  const found = b.items.filter(i => i.found).length;
  showMsg("batchMsg", `${found} of ${b.items.length} found among ${b.parked} parked vehicles.`, found > 0);
  document.getElementById("batchBody").innerHTML = b.items.map(i => `
    <tr>
      <td>${escapeHtml(i.number)}</td>
      <td>${i.found ? "Slot " + i.slot : "not found"}</td>
      <td>${i.linearCmp}</td>
      <td>${i.binaryCmp}</td>
    </tr>`).join("");
  wrap.style.display = "block";
  const cheaper = b.binaryWithSort < b.linearTotal ? "sort once + binary search" : "linear search";
  document.getElementById("batchTotals").textContent =
    `Linear total: ${b.linearTotal} comparisons. Sort once + binary: ${b.sortCost} + ${b.binaryTotal} = ${b.binaryWithSort}. Cheaper here: ${cheaper}.`;
});

// ---------------- Phase 4A: greedy reservation planner ----------------

const RES_EXAMPLE = [
  "LONG1 08:00 18:00",
  "KA05AB1234 09:00 10:00",
  "KA05CD5678 10:00 11:00",
  "KA05EF9012 11:00 12:30",
  "KA05GH3456 13:00 14:00"
].join("\n");

document.getElementById("resExample").addEventListener("click", () => {
  document.getElementById("resRequests").value = RES_EXAMPLE;
  document.getElementById("resSlots").value = 1;
});

document.getElementById("reserveForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const requests = document.getElementById("resRequests").value
    .split("\n").map(l => l.trim()).filter(l => l).join(";");
  const slots = document.getElementById("resSlots").value;

  const res = await fetch(`${API}/api/reserve?slots=${encodeURIComponent(slots)}&requests=${encodeURIComponent(requests)}`);
  const r = await res.json();

  const wrap = document.getElementById("resWrap");
  if (!r.success) {
    showMsg("resMsg", r.message, false);
    wrap.style.display = "none";
    document.getElementById("resRejected").textContent = "";
    document.getElementById("resCompare").textContent = "";
    return;
  }
  const g = r.greedy;
  showMsg("resMsg", `Accepted ${g.count} of ${r.total} bookings using ${r.slots} reserved slot${r.slots > 1 ? "s" : ""}.`, true);
  document.getElementById("resBody").innerHTML = g.accepted.length
    ? g.accepted.map(a => `
      <tr>
        <td>R${a.slot}</td>
        <td>${escapeHtml(a.vehicle)}</td>
        <td>${a.start} - ${a.end}</td>
      </tr>`).join("")
    : `<tr class="empty-row"><td colspan="3">No bookings accepted.</td></tr>`;
  wrap.style.display = "block";
  document.getElementById("resRejected").textContent = g.rejected.length
    ? "Rejected (overlap): " + g.rejected.map(x => `${x.vehicle} ${x.start}-${x.end}`).join(", ")
    : "No bookings rejected.";
  document.getElementById("resCompare").textContent =
    `Greedy (earliest finish first) accepted ${g.count}; simple first-come-first-served would accept ${r.fcfs.count}.`;
});

// ---------------- Init ----------------

refreshAll();
loadGraph("Entrance");
setInterval(refreshAll, 5000); // keep the dashboard fresh automatically