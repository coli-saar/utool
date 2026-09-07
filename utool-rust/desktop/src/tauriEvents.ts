/**
 * Work around tauri-apps/tauri#15799.
 *
 * Tauri can resolve listen() before its WebView-side listener entry has been
 * installed. If React cleans up the effect in that interval, Tauri's internal
 * unregisterListener throws while looking up the missing entry. The throw also
 * prevents @tauri-apps/api from sending the backend unlisten command.
 *
 * Treat only that missing-entry TypeError as the no-op Tauri intended. The API
 * then continues and removes the listener from the backend normally.
 */
export function installTauriUnlistenRaceGuard(): void {
  const internals = window.__TAURI_EVENT_PLUGIN_INTERNALS__;
  const unregisterListener = internals.unregisterListener;

  internals.unregisterListener = (event, eventId) => {
    try {
      unregisterListener(event, eventId);
    } catch (reason) {
      const missingListenerEntry = reason instanceof TypeError
        && String(reason).includes("handlerId");
      if (!missingListenerEntry) throw reason;
    }
  };
}
