import { useMemo, useState } from "react";
import { analyzePrivacy, chat, fetchAudit, mintDevToken } from "./api.js";

const SAMPLE_PROMPT = `John Smith from john.smith@example.com transferred
€20,000 from account 123456789.
Transaction reference TX998877.`;

function newConversationId() {
  if (typeof crypto !== "undefined" && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return `conv-${Date.now()}`;
}

function highlightOriginal(text, entities) {
  if (!text || !entities?.length) {
    return text || "";
  }
  const sorted = [...entities].sort((a, b) => a.start - b.start);
  const parts = [];
  let cursor = 0;
  for (const entity of sorted) {
    if (entity.start < cursor || entity.end > text.length || entity.start >= entity.end) {
      continue;
    }
    if (entity.start > cursor) {
      parts.push({ key: `t-${cursor}`, text: text.slice(cursor, entity.start) });
    }
    parts.push({
      key: `e-${entity.start}-${entity.type}`,
      text: text.slice(entity.start, entity.end),
      type: entity.type,
      action: entity.action,
    });
    cursor = entity.end;
  }
  if (cursor < text.length) {
    parts.push({ key: `t-${cursor}`, text: text.slice(cursor) });
  }
  return parts;
}

export default function App() {
  const [userId, setUserId] = useState("demo-user");
  const [tenantId, setTenantId] = useState("demo-tenant");
  const [reidentify, setReidentify] = useState(true);
  const [token, setToken] = useState("");
  const [conversationId, setConversationId] = useState(newConversationId);
  const [prompt, setPrompt] = useState(SAMPLE_PROMPT);

  // Client-held original only (D-003) — never loaded from the server.
  const [original, setOriginal] = useState("");
  const [entities, setEntities] = useState([]);
  const [sanitizedText, setSanitizedText] = useState("");
  const [finalResponse, setFinalResponse] = useState("");
  const [privacySummary, setPrivacySummary] = useState(null);
  const [auditEvents, setAuditEvents] = useState([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [status, setStatus] = useState("Mint a demo token (or paste a JWT), then run Analyze or Chat.");

  const highlighted = useMemo(() => highlightOriginal(original, entities), [original, entities]);

  async function handleMint() {
    setError("");
    setBusy(true);
    try {
      const result = await mintDevToken({ userId, tenantId, reidentify });
      setToken(result.accessToken);
      setStatus(
        `Token minted for ${userId} / ${tenantId} (reidentify=${reidentify}). Original prompts stay in the browser only.`
      );
    } catch (err) {
      setError(
        `${err.message}. If /api/v1/dev/token is disabled, set DEV_TOKEN_ENABLED=true or paste a JWT.`
      );
    } finally {
      setBusy(false);
    }
  }

  async function handleAnalyze() {
    if (!token.trim()) {
      setError("JWT required.");
      return;
    }
    if (!prompt.trim()) {
      setError("Enter a prompt.");
      return;
    }
    setError("");
    setBusy(true);
    setFinalResponse("");
    setPrivacySummary(null);
    try {
      const heldOriginal = prompt;
      setOriginal(heldOriginal);
      const analysis = await analyzePrivacy({
        token: token.trim(),
        conversationId,
        text: heldOriginal,
      });
      setEntities(analysis.entities || []);
      setSanitizedText(analysis.sanitizedText || "");
      const audit = await fetchAudit({ token: token.trim(), conversationId });
      setAuditEvents(audit.events || []);
      setStatus("Analyze complete — LLM was not called. Original text is held only in this browser session.");
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  async function handleChat() {
    if (!token.trim()) {
      setError("JWT required.");
      return;
    }
    if (!prompt.trim()) {
      setError("Enter a prompt.");
      return;
    }
    setError("");
    setBusy(true);
    try {
      const heldOriginal = prompt;
      setOriginal(heldOriginal);
      const analysis = await analyzePrivacy({
        token: token.trim(),
        conversationId,
        text: heldOriginal,
      });
      setEntities(analysis.entities || []);
      setSanitizedText(analysis.sanitizedText || "");

      const chatResult = await chat({
        token: token.trim(),
        conversationId,
        message: heldOriginal,
      });
      setFinalResponse(chatResult.response || "");
      setPrivacySummary(chatResult.privacy || null);

      const audit = await fetchAudit({ token: token.trim(), conversationId });
      setAuditEvents(audit.events || []);
      setStatus(
        `Chat complete. Response re-identification follows the JWT reidentify claim used when the token was minted.`
      );
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="app">
      <header className="hero">
        <p className="eyebrow">Local enterprise PoC</p>
        <h1>LLM Privacy Gateway</h1>
        <p className="lede">
          Watch protected values stay client-side while the gateway detects, transforms, and
          only then calls the local LLM.
        </p>
      </header>

      <section className="controls" aria-label="Session controls">
        <div className="control-grid">
          <label>
            User ID
            <input value={userId} onChange={(e) => setUserId(e.target.value)} disabled={busy} />
          </label>
          <label>
            Tenant ID
            <input value={tenantId} onChange={(e) => setTenantId(e.target.value)} disabled={busy} />
          </label>
          <label>
            Conversation ID
            <input
              value={conversationId}
              onChange={(e) => setConversationId(e.target.value)}
              disabled={busy}
            />
          </label>
          <label className="checkbox">
            <input
              type="checkbox"
              checked={reidentify}
              onChange={(e) => setReidentify(e.target.checked)}
              disabled={busy}
            />
            Mint token with reidentify
          </label>
        </div>

        <label className="token-field">
          JWT
          <textarea
            rows={2}
            value={token}
            onChange={(e) => setToken(e.target.value)}
            placeholder="Paste Bearer token or mint below"
            disabled={busy}
          />
        </label>

        <div className="actions">
          <button type="button" onClick={handleMint} disabled={busy}>
            Mint demo token
          </button>
          <button
            type="button"
            className="secondary"
            onClick={() => setConversationId(newConversationId())}
            disabled={busy}
          >
            New conversation
          </button>
        </div>
      </section>

      <section className="composer" aria-label="Prompt">
        <div className="composer-head">
          <h2>Prompt</h2>
          <button
            type="button"
            className="secondary"
            onClick={() => setPrompt(SAMPLE_PROMPT)}
            disabled={busy}
          >
            Load sample
          </button>
        </div>
        <textarea
          rows={6}
          value={prompt}
          onChange={(e) => setPrompt(e.target.value)}
          disabled={busy}
        />
        <div className="actions">
          <button type="button" onClick={handleAnalyze} disabled={busy}>
            Analyze only
          </button>
          <button type="button" className="primary" onClick={handleChat} disabled={busy}>
            Analyze + Chat
          </button>
        </div>
        {status ? <p className="status">{status}</p> : null}
        {error ? <p className="error" role="alert">{error}</p> : null}
      </section>

      <section className="panes" aria-label="Privacy visualization">
        <article className="pane">
          <h2>1. Original</h2>
          <p className="pane-note">Held in the browser for this session only.</p>
          <pre className="pane-body">
            {Array.isArray(highlighted)
              ? highlighted.map((part) =>
                  part.type ? (
                    <mark key={part.key} data-type={part.type} title={`${part.type} → ${part.action}`}>
                      {part.text}
                    </mark>
                  ) : (
                    <span key={part.key}>{part.text}</span>
                  )
                )
              : highlighted || "—"}
          </pre>
        </article>

        <article className="pane">
          <h2>2. Detected entities</h2>
          <p className="pane-note">Types, policy actions, and opaque tokens.</p>
          <div className="pane-body">
            {entities.length === 0 ? (
              <p className="empty">No entities yet.</p>
            ) : (
              <ul className="entity-list">
                {entities.map((entity, index) => (
                  <li key={`${entity.type}-${entity.start}-${index}`}>
                    <span className="badge">{entity.type}</span>
                    <span className="action">{entity.action}</span>
                    <code>{entity.token || "—"}</code>
                    <span className="span">
                      [{entity.start}, {entity.end})
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </article>

        <article className="pane">
          <h2>3. Sanitized LLM input</h2>
          <p className="pane-note">What the provider is allowed to see.</p>
          <pre className="pane-body">{sanitizedText || "—"}</pre>
        </article>

        <article className="pane">
          <h2>4. Final response</h2>
          <p className="pane-note">
            User-facing text after response inspection
            {privacySummary
              ? ` · detected ${privacySummary.entitiesDetected}, tokenized ${privacySummary.entitiesTokenized}, redacted ${privacySummary.entitiesRedacted}`
              : ""}
            .
          </p>
          <pre className="pane-body">{finalResponse || "—"}</pre>
        </article>
      </section>

      <section className="audit" aria-label="Audit metadata">
        <h2>Audit metadata</h2>
        <p className="pane-note">No plaintext PII — operation, entity type, action, success only.</p>
        {auditEvents.length === 0 ? (
          <p className="empty">No events for this conversation yet.</p>
        ) : (
          <div className="audit-table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Time</th>
                  <th>Operation</th>
                  <th>Entity</th>
                  <th>Action</th>
                  <th>OK</th>
                  <th>Metadata</th>
                </tr>
              </thead>
              <tbody>
                {auditEvents.map((event) => (
                  <tr key={event.id}>
                    <td>{event.timestamp}</td>
                    <td>{event.operation}</td>
                    <td>{event.entityType || "—"}</td>
                    <td>{event.action || "—"}</td>
                    <td>{event.success ? "yes" : "no"}</td>
                    <td>{event.metadata || "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}
