// Veritas Assistant - Floating Chatbot Widget

(function () {
    let greetingShown = false;
    let isAnalyzing = false;

    document.addEventListener("DOMContentLoaded", () => {
        const toggleBtn = document.getElementById("chatbot-toggle-btn");
        const closeBtn = document.getElementById("chatbot-close-btn");
        const panel = document.getElementById("chatbot-panel");
        const sendBtn = document.getElementById("chatbot-send-btn");
        const textarea = document.getElementById("chatbot-input");
        const messages = document.getElementById("chatbot-messages");

        if (!toggleBtn || !panel || !textarea || !sendBtn || !messages) {
            return;
        }

        // Toggle open / close
        toggleBtn.addEventListener("click", () => {
            const isClosed = panel.classList.contains("closed");
            if (isClosed) {
                panel.classList.remove("closed");
                toggleBtn.innerHTML = '<i class="fa-solid fa-xmark"></i>';
                // Trigger one-time greeting
                if (!greetingShown) {
                    showGreeting();
                    greetingShown = true;
                }
                setTimeout(() => textarea.focus(), 150);
            } else {
                panel.classList.add("closed");
                toggleBtn.innerHTML = '<i class="fa-solid fa-comments"></i>';
            }
        });

        // Close button inside header
        if (closeBtn) {
            closeBtn.addEventListener("click", () => {
                panel.classList.add("closed");
                toggleBtn.innerHTML = '<i class="fa-solid fa-comments"></i>';
            });
        }

        // Handle Send click
        sendBtn.addEventListener("click", () => {
            sendMessage();
        });

        // Handle Enter vs Shift+Enter
        textarea.addEventListener("keydown", (e) => {
            if (e.key === "Enter" && !e.shiftKey) {
                e.preventDefault();
                sendMessage();
            }
        });

        // Auto-resize textarea as user types
        textarea.addEventListener("input", () => {
            textarea.style.height = "auto";
            textarea.style.height = Math.min(textarea.scrollHeight, 80) + "px";
        });

        // One-time greeting message
        function showGreeting() {
            appendBotBubble(`
                <strong>👋 Hello! I am the Veritas Assistant.</strong><br>
                Paste any news headline or article excerpt here to get an instant credibility verdict, confidence score, and key indicators.
            `);
        }

        // Send user message and query /api/analyze
        function sendMessage() {
            if (isAnalyzing) return;

            const text = textarea.value.trim();
            if (!text) return;

            // Render User Bubble
            appendUserBubble(text);

            // Reset input field
            textarea.value = "";
            textarea.style.height = "40px";

            // Render Placeholder "Analyzing..." Bubble
            isAnalyzing = true;
            sendBtn.disabled = true;
            const placeholderEl = appendAnalyzingBubble();

            // Call existing POST /api/analyze
            fetch("/api/analyze", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    text: text,
                    inputType: "TEXT"
                })
            })
            .then(async (response) => {
                if (!response.ok) {
                    const errData = await response.json().catch(() => ({}));
                    throw new Error(errData.message || "Analysis request failed.");
                }
                return response.json();
            })
            .then((data) => {
                renderAnalysisReply(placeholderEl, data);
            })
            .catch((error) => {
                renderErrorReply(placeholderEl, error.message || "Failed to analyze text. Please try again.");
            })
            .finally(() => {
                isAnalyzing = false;
                sendBtn.disabled = false;
                scrollToBottom();
            });
        }

        // Render User Bubble
        function appendUserBubble(text) {
            const div = document.createElement("div");
            div.className = "chatbot-msg user";
            div.textContent = text;
            messages.appendChild(div);
            scrollToBottom();
        }

        // Render Bot Bubble
        function appendBotBubble(htmlContent, extraClass = "") {
            const div = document.createElement("div");
            div.className = "chatbot-msg bot " + extraClass;
            div.innerHTML = htmlContent;
            messages.appendChild(div);
            scrollToBottom();
            return div;
        }

        // Render Placeholder Analyzing Bubble
        function appendAnalyzingBubble() {
            const div = document.createElement("div");
            div.className = "chatbot-msg bot analyzing";
            div.innerHTML = `
                <span>Analyzing news content</span>
                <div class="chatbot-dots">
                    <span></span><span></span><span></span>
                </div>
            `;
            messages.appendChild(div);
            scrollToBottom();
            return div;
        }

        // Replace Placeholder with Analysis Result
        function renderAnalysisReply(placeholderEl, data) {
            placeholderEl.classList.remove("analyzing");

            const prediction = (data.prediction || "").toUpperCase();
            let verdictText = prediction.replace("_", " ");
            let borderClass = "bot-msg-uncertain";
            let verdictClass = "verdict-uncertain";

            if (prediction.includes("GENUINE")) {
                borderClass = "bot-msg-genuine";
                verdictClass = "verdict-genuine";
            } else if (prediction.includes("FAKE")) {
                borderClass = "bot-msg-fake";
                verdictClass = "verdict-fake";
            }

            placeholderEl.classList.add(borderClass);

            const confidence = typeof data.confidenceScore === "number" ? data.confidenceScore + "%" : "N/A";
            const riskLevel = data.riskLevel || "N/A";

            // Up to 3 indicators as bullet points
            const rawIndicators = data.indicators || [];
            const topIndicators = rawIndicators.slice(0, 3);
            let indicatorsHtml = "";
            if (topIndicators.length > 0) {
                indicatorsHtml = '<ul class="chatbot-indicators">';
                topIndicators.forEach((ind) => {
                    const clean = escapeHtml(ind.replace(" (+)", "").replace(" (-)", ""));
                    indicatorsHtml += `<li>${clean}</li>`;
                });
                indicatorsHtml += '</ul>';
            }

            placeholderEl.innerHTML = `
                <div class="chatbot-result-header">
                    <span class="chatbot-result-verdict ${verdictClass}">${escapeHtml(verdictText)}</span>
                    <div class="chatbot-result-metrics">
                        <span class="chatbot-metric-pill" title="Credibility Confidence">${escapeHtml(confidence)}</span>
                        <span class="chatbot-metric-pill" title="Risk Level">${escapeHtml(riskLevel)} RISK</span>
                    </div>
                </div>
                ${indicatorsHtml}
            `;
        }

        // Replace Placeholder with Error Message
        function renderErrorReply(placeholderEl, errorMessage) {
            placeholderEl.classList.remove("analyzing");
            placeholderEl.classList.add("bot-msg-error");
            placeholderEl.innerHTML = `
                <strong><i class="fa-solid fa-triangle-exclamation"></i> Unable to analyze</strong><br>
                <span>${escapeHtml(errorMessage)}</span>
            `;
        }

        function scrollToBottom() {
            messages.scrollTop = messages.scrollHeight;
        }

        function escapeHtml(str) {
            if (!str) return "";
            return str
                .replace(/&/g, "&amp;")
                .replace(/</g, "&lt;")
                .replace(/>/g, "&gt;")
                .replace(/"/g, "&quot;")
                .replace(/'/g, "&#039;");
        }
    });
})();
