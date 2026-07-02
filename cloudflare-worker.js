/**
 * Cloudflare Worker: Google Drive Streaming CDN Proxy & Quota Bypass
 * 
 * Features:
 * 1. Chunked Streaming: Supports HTTP Range Requests required by ExoPlayer/JWPlayer for seamless seeking.
 * 2. CDN Caching: Caches streamed video chunks on Cloudflare's edge servers so repeat views don't hit Google Drive limits.
 * 3. Quota Bypass (API Token / Mirror Rotator): Automatically rotates Google Drive API keys or file mirrors
 *    to distribute the load and avoid Google Drive's "Download Quota Exceeded" errors.
 */

// Configure your Google Drive API Keys / Access Tokens here.
// The script will automatically rotate between these tokens on each request.
const GOOGLE_DRIVE_API_TOKENS = [
  "AIzaSyDaMy77bzRFekyPdJTuYTCYBKf-CpV8Pjs"
];

// Configure alternative File IDs if you have mirror copies of the movie in different drives
// (Query parameter 'id' acts as primary, but if a mirror is available, the script can failover)
const FILE_MIRRORS = {
  "18y_gOn2N_7Z6m66i485tA1_XpG3B": [
    "18y_gOn2N_7Z6m66i485tA1_XpG3B", // Primary File ID
    "1_mirror_sintel",               // Mirror 1 File ID
    "2_mirror_sintel"                // Mirror 2 File ID
  ]
};

addEventListener("fetch", event => {
  event.respondWith(handleRequest(event));
});

async function handleRequest(event) {
  const request = event.request;
  const url = new URL(request.url);

  // Health check endpoint
  if (url.pathname === "/" || url.pathname === "/health") {
    return new Response(JSON.stringify({
      status: "online",
      message: "Cloudflare Worker Google Drive Streaming Proxy is Active!",
      caching: "enabled",
      tokenRotation: `${GOOGLE_DRIVE_API_TOKENS.length} tokens configured`
    }), {
      headers: { 
        "Content-Type": "application/json",
        "Access-Control-Allow-Origin": "*"
      }
    });
  }

  // Streaming endpoint: /stream?id=FILE_ID
  if (url.pathname === "/stream") {
    const primaryFileId = url.searchParams.get("id");
    if (!primaryFileId) {
      return new Response("Missing 'id' parameter in stream request.", { status: 400 });
    }

    // Determine candidate File IDs (primary + mirrors)
    let candidateIds = [primaryFileId];
    if (FILE_MIRRORS[primaryFileId]) {
      candidateIds = FILE_MIRRORS[primaryFileId];
    }

    // Select a random API token for rotation
    const tokenIndex = Math.floor(Math.random() * GOOGLE_DRIVE_API_TOKENS.length);
    const apiKey = GOOGLE_DRIVE_API_TOKENS[tokenIndex] || "";

    // If using local mock streams or sample HTTP urls, redirect or fetch directly
    if (primaryFileId.startsWith("http://") || primaryFileId.startsWith("https://")) {
      return fetch(primaryFileId, { headers: request.headers });
    }

    // Try fetching with candidates (failover loop)
    for (const fileId of candidateIds) {
      try {
        const driveUrl = `https://www.googleapis.com/drive/v3/files/${fileId}?alt=media&key=${apiKey}`;

        // Forward headers (especially the Range header for seeking)
        const forwardHeaders = new Headers();
        const rangeHeader = request.headers.get("Range");
        if (rangeHeader) {
          forwardHeaders.set("Range", rangeHeader);
        }
        
        // Setup cache keys
        const cache = caches.default;
        const cacheKey = new Request(request.url + (rangeHeader ? `-${rangeHeader}` : ""), request);
        
        // Attempt to serve from CDN cache first
        let cachedResponse = await cache.match(cacheKey);
        if (cachedResponse) {
          // Add cache hit header
          const headers = new Headers(cachedResponse.headers);
          headers.set("X-Cache", "HIT");
          return new Response(cachedResponse.body, {
            status: cachedResponse.status,
            statusText: cachedResponse.statusText,
            headers
          });
        }

        // Fetch from Google Drive API
        const driveResponse = await fetch(driveUrl, {
          headers: forwardHeaders,
          cf: {
            cacheTtl: 86400, // Cache on edge for 24 hours
            cacheEverything: true
          }
        });

        // If Google Drive tells us Quota Exceeded (403), failover to next candidate ID
        if (driveResponse.status === 403 || driveResponse.status === 429) {
          console.warn(`Quota reached or rate limit hit for file ${fileId}. Retrying with mirror...`);
          continue;
        }

        // Prepare the proxied stream response
        const responseHeaders = new Headers(driveResponse.headers);
        responseHeaders.set("Access-Control-Allow-Origin", "*");
        responseHeaders.set("X-Cache", "MISS");
        responseHeaders.set("Cache-Control", "public, max-age=86400"); // CDN caching indicator

        const proxyResponse = new Response(driveResponse.body, {
          status: driveResponse.status,
          statusText: driveResponse.statusText,
          headers: responseHeaders
        });

        // Store chunk in cache if request was successful and range was used
        if (driveResponse.status === 200 || driveResponse.status === 206) {
          event.waitUntil(cache.put(cacheKey, proxyResponse.clone()));
        }

        return proxyResponse;

      } catch (err) {
        console.error(`Error during streaming from file ID ${fileId}:`, err);
      }
    }

    return new Response("Unable to retrieve file stream. All mirrors and quotas exhausted.", { status: 502 });
  }

  return new Response("Not Found", { status: 404 });
}
