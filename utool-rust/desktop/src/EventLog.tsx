import { invoke } from "@tauri-apps/api/core";
import { listen } from "@tauri-apps/api/event";
import { useEffect, useMemo, useState } from "react";
import type { EventEntry } from "./types";

function formatElapsed(elapsedMs: number): string {
  if (elapsedMs < 1) return `${(elapsedMs * 1000).toFixed(elapsedMs < 0.1 ? 1 : 0)} µs`;
  if (elapsedMs < 1000) return `${elapsedMs.toFixed(elapsedMs < 10 ? 2 : 1)} ms`;
  return `${(elapsedMs / 1000).toFixed(3)} s`;
}

function readableLabel(key: string): string {
  const words = key.replace(/([a-z0-9])([A-Z])/g, "$1 $2").replaceAll(/[_-]+/g, " ");
  return words.charAt(0).toUpperCase() + words.slice(1);
}

function readableValue(value: unknown): string {
  if (Array.isArray(value)) return value.map(readableValue).join(", ");
  if (value !== null && typeof value === "object") {
    return Object.entries(value).map(([key, item]) => `${readableLabel(key)}: ${readableValue(item)}`).join("; ");
  }
  return String(value);
}

function argumentLines(arguments_: unknown): string[] {
  if (arguments_ === null || typeof arguments_ !== "object" || Array.isArray(arguments_)) {
    return [`Details: ${readableValue(arguments_)}`];
  }
  return Object.entries(arguments_).map(([key, value]) => `${readableLabel(key)}: ${readableValue(value)}`);
}

function eventText(event: EventEntry): string {
  const lines = [
    `${new Date(event.timestampMs).toISOString()}  ${event.status.toUpperCase()}  ${event.action}`,
    `Window: ${event.windowTitle}`,
    `Runtime: ${formatElapsed(event.elapsedMs)}`,
    ...argumentLines(event.arguments),
  ];
  if (event.error) lines.push(`Error: ${event.error}`);
  return lines.join("\n");
}

async function copyText(text: string): Promise<void> {
  if (navigator.clipboard) {
    try {
      await navigator.clipboard.writeText(text);
      return;
    } catch {
      // Fall through to the selection-based WebView implementation.
    }
  }
  const textarea = document.createElement("textarea");
  textarea.value = text;
  textarea.style.position = "fixed";
  textarea.style.opacity = "0";
  document.body.appendChild(textarea);
  textarea.select();
  document.execCommand("copy");
  textarea.remove();
}

export default function EventLog() {
  const [events, setEvents] = useState<EventEntry[]>([]);
  const [copied, setCopied] = useState<number | "all" | null>(null);

  useEffect(() => {
    let disposed = false;
    const pending = listen<EventEntry>("event-log-updated", ({ payload }) => {
      setEvents((current) => current.some((event) => event.id === payload.id) ? current : [...current, payload]);
    });
    void invoke<EventEntry[]>("event_entries").then((entries) => {
      if (!disposed) setEvents((current) => {
        const merged = new Map([...entries, ...current].map((event) => [event.id, event]));
        return [...merged.values()].sort((left, right) => left.id - right.id);
      });
    });
    return () => {
      disposed = true;
      void pending.then((unlisten) => unlisten());
    };
  }, []);

  const allText = useMemo(() => events.map(eventText).join("\n\n"), [events]);
  const copy = async (id: number | "all", text: string) => {
    await copyText(text);
    setCopied(id);
    window.setTimeout(() => setCopied((current) => current === id ? null : current), 1200);
  };

  return <main className="event-log-window">
    <header className="event-log-header">
      <div><h1>Event Log</h1><p>{events.length} events from this session</p></div>
      <button disabled={events.length === 0} onClick={() => void copy("all", allText)}>{copied === "all" ? "Copied" : "Copy All"}</button>
    </header>
    <section className="event-list" aria-live="polite">
      {events.length === 0 && <div className="event-empty">No events have been recorded yet.</div>}
      {[...events].reverse().map((event) => {
        const text = eventText(event);
        return <article key={event.id} className={`event-entry ${event.status}`}>
          <div className="event-summary">
            <time>{new Date(event.timestampMs).toLocaleTimeString()}</time>
            <span className="event-status">{event.status}</span>
            <strong>{event.action}</strong>
            <span className="event-window">{event.windowTitle}</span>
            <span className="event-runtime">{formatElapsed(event.elapsedMs)}</span>
            <button onClick={() => void copy(event.id, text)}>{copied === event.id ? "Copied" : "Copy"}</button>
          </div>
          <textarea readOnly value={text} aria-label={`${event.action} event details`} onFocus={(target) => target.currentTarget.select()} />
        </article>;
      })}
    </section>
  </main>;
}
