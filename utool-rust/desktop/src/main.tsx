import { createRoot } from "react-dom/client";
import App from "./App";
import EventLog from "./EventLog";
import "./style.css";

const isEventLog = new URLSearchParams(window.location.search).get("view") === "event-log";
createRoot(document.getElementById("root")!).render(isEventLog ? <EventLog /> : <App />);
