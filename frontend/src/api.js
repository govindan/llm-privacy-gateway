const API_BASE = "";

async function parseError(response) {
  let detail = `${response.status} ${response.statusText}`;
  try {
    const body = await response.json();
    if (body?.code || body?.message) {
      detail = [body.code, body.message, body.correlationId]
        .filter(Boolean)
        .join(" — ");
    }
  } catch {
    // keep status text
  }
  throw new Error(detail);
}

async function request(path, { method = "GET", token, body } = {}) {
  const headers = { Accept: "application/json" };
  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
  }
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  const response = await fetch(`${API_BASE}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (!response.ok) {
    await parseError(response);
  }
  if (response.status === 204) {
    return null;
  }
  return response.json();
}

export function mintDevToken({ userId, tenantId, reidentify }) {
  return request("/api/v1/dev/token", {
    method: "POST",
    body: { userId, tenantId, reidentify },
  });
}

export function analyzePrivacy({ token, conversationId, text }) {
  return request("/api/v1/privacy/analyze", {
    method: "POST",
    token,
    body: { conversationId, text },
  });
}

export function chat({ token, conversationId, message }) {
  return request("/api/v1/chat", {
    method: "POST",
    token,
    body: { conversationId, message },
  });
}

export function fetchAudit({ token, conversationId }) {
  return request(`/api/v1/audit/conversations/${encodeURIComponent(conversationId)}`, {
    token,
  });
}
