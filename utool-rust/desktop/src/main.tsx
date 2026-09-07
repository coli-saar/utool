import { createRoot } from "react-dom/client";
import { getCurrentWindow } from "@tauri-apps/api/window";
import App from "./App";
import EventLog from "./EventLog";
import { installTauriUnlistenRaceGuard } from "./tauriEvents";
import "./style.css";

installTauriUnlistenRaceGuard();

const parameters = new URLSearchParams(window.location.search);
const isEventLog = parameters.get("view") === "event-log";

async function start() {
  if (parameters.has("startup")) {
    try {
      await getCurrentWindow().show();
      document.documentElement.dataset.startupShownAt = String(performance.now());
      await new Promise<void>((resolve) => requestAnimationFrame(() => resolve()));
    } catch (reason) {
      console.error("Could not reveal the startup window", reason);
    }
  }
  createRoot(document.getElementById("root")!).render(isEventLog ? <EventLog /> : <App />);
}

void start();
