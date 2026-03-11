package bg.sofia.uni.fmi.mjt.code.check.client;

/**
 * ResponseFormatter is responsible for the Presentation Layer of the application.
 * The Server acts as a universal backend provider, returning raw, structured JSON data.
 * This ensures the Server remains agnostic of the front-end implementation (CLI, Web, or Mobile).
 * -
 * Identifies the command context from the JSON 'command' field.
 * Human-Readable Transformation (Stringify): Converts raw JSON objects, lists,
 * and maps into aesthetically pleasing console outputs (tables, line-numbered
 * code blocks, status indicators).
 * -
 * Intercepts 'ERROR' statuses and formats them with consistent
 * UI patterns (e.g., using prefix icons like or [ERROR]).
 * -
 * By keeping this logic in the Client, we ensure that the Server remains lightweight
 * and scalable for future HTTP/Web integration, while providing a rich User Experience
 * in the current CLI environment.
 */
public class ResponseFormatter {
    // to do
    // I didn't have enough time to complete it
}
