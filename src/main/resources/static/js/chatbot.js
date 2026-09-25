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
            sendChatbotMessage();
        });

        // Handle Enter vs Shift+Enter
        textarea.addEventListener("keydown", (e) => {
            if (e.key === "Enter" && !e.shiftKey) {
                e.preventDefault();
                sendChatbotMessage();
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

        // Check for small talk / conversational intents
        function getSmallTalkResponse(text) {
            const clean = text.toLowerCase().trim().replace(/[!.,?]+$/, "").trim();

            // Greetings
            if (/^(hi+|hello+|hey+|howdy|hola|greetings|good\s*(morning|afternoon|evening|day)|hey\s*there|hello\s*there)(\s+(veritas|bot|assistant))?$/.test(clean)) {
                return "Hey there! Paste a news headline or article and I'll check if it looks genuine or fake.";
            }

            // Thanks
            if (/^(thanks?|thank\s*you(\s*(so\s*much|a\s*lot))?|thx|ty)(\s+(veritas|bot|assistant))?$/.test(clean)) {
                return "You're welcome! Send me another headline anytime.";
            }

            // Identity / Capability
            if (/^(who\s*(are\s*you|made\s*you)|what\s*(can\s*you\s*do|do\s*you\s*do|is\s*this|is\s*veritas)|tell\s*me\s*about\s*yourself)$/.test(clean)) {
                return "I'm the Veritas Assistant. I analyze news text and tell you whether it looks genuine, fake, or uncertain, along with why.";
            }

            // Farewell
            if (/^(bye+|goodbye+|see\s*ya|cya|take\s*care|good\s*night)(\s+(veritas|bot|assistant))?$/.test(clean)) {
                return "Goodbye! Stay safe from misinformation.";
            }

            // Help
            if (/^(help|how\s*to\s*use|how\s*does\s*(this|it)\s*work|guide)$/.test(clean)) {
                return "Simply paste a news headline or full article text here. I will analyze its linguistic style, source attribution, and statistical signals to give you an instant credibility verdict!";
            }

            return null;
        }

        // Send user message and decide whether to reply conversationally or query /api/analyze
        function sendChatbotMessage() {
            if (isAnalyzing) return;

            const text = textarea.value.trim();
            if (!text) return;

            // Render User Bubble
            appendUserBubble(text);

            // Reset input field
            textarea.value = "";
            textarea.style.height = "40px";

            // 1. Check for small talk / conversational greetings
            const smallTalkReply = getSmallTalkResponse(text);
            if (smallTalkReply) {
                appendBotBubble(escapeHtml(smallTalkReply));
                return;
            }

            // 2. Check if text is too short or vague to be actual news content (< 6 words)
            const words = text.split(/\s+/).filter(w => w.length > 0);
            if (words.length < 6) {
                appendBotBubble("That looks a bit short to analyze properly. Could you paste a full headline or a few sentences from the article?");
                return;
            }

            // 3. Actual news content: Render Placeholder "Analyzing..." Bubble & call /api/analyze
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

        const sendMessage = sendChatbotMessage;

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
