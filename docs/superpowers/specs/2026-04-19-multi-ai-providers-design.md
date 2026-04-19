# Design Spec: Multi-AI Provider Support & Secure Storage

## 1. Overview
Extend SpendSense to support multiple AI providers (NVIDIA NIM, Google Gemini, OpenCode) for regex generation using a unified OpenAI-compatible architecture. Implement secure storage for API keys.

## 2. Architecture Changes

### 2.1. Dynamic API Client
- **ChatCompletionApi:** Refactor `OpenRouterApi` into a generic interface for OpenAI-compatible chat completions.
- **DynamicBaseUrlInterceptor:** Implement an OkHttp interceptor to swap `baseUrl` and inject headers (Authorization, etc.) dynamically based on the selected provider.
- **Header Strategy:**
    - Standard: `Authorization: Bearer <key>`
    - OpenRouter: `Authorization: Bearer <key>`, `HTTP-Referer`, `X-Title`
    - OpenCode (Free): No Authorization header required.

### 2.2. Secure Storage
- **SecurePreferences:** Implement using Android's `EncryptedSharedPreferences`.
- **Key-Value Mapping:** Store keys using the provider's `id` or a unique slug as the key.
- **Database:** `AiProviderEntity` will be updated (if needed) to remove the `apiKey` field, or it will be treated as a reference/placeholder.

### 2.3. Data Initialization
- Pre-populate Room database with provider presets:
    - **OpenCode:** `oc/minimax-m2.5-free`, `oc/trinity-large-preview-free`, `oc/nemotron-3-super-free`.
    - **NVIDIA:** Preset with `https://integrate.api.nvidia.com/v1`.
    - **Google:** Preset with `https://generativelanguage.googleapis.com/v1beta/openai/`.

## 3. UI Changes

### 3.1. Regex Generator
- Add a `ProviderSelector` (Dropdown) to choose from configured AI providers.
- Pass the selected provider ID to the `RegexGeneratorViewModel`.

### 3.2. AI Provider Settings
- Update the provider list to show preset status (e.g., "Active - No Key Required" for OpenCode).
- Add secure input for API keys (NVIDIA, Google, OpenRouter).

## 4. Testing Strategy
- **Unit Tests:** Verify `DynamicBaseUrlInterceptor` correctly modifies requests.
- **Integration Tests:** Test `SecurePreferences` read/write.
- **Manual Verification:**
    - Generate regex using OpenCode (no key).
    - Add NVIDIA key and generate regex.
    - Confirm keys are not stored in plaintext in the Room DB.
