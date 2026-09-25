// Veritas Application Controller

// Active Tab state
let currentTab = 'home';
let currentInputMode = 'text';

// History State
let historyPage = 0;
let historySize = 10;
let historyTotalPages = 1;
let historyQueryTimeout = null;
let currentModalRecordId = null;

// Chart Instances
let chartTimeline = null;
let chartDistribution = null;
let chartRisk = null;

// DOM Elements & Event Listeners
document.addEventListener("DOMContentLoaded", () => {
    // Character Counter
    const newsText = document.getElementById("news-text");
    const charCount = document.getElementById("char-count");
    if (newsText && charCount) {
        newsText.addEventListener("input", (e) => {
            const count = e.target.value.length;
            charCount.textContent = count.toLocaleString();
            if (count > 50000) {
                charCount.style.color = "var(--fake-color)";
            } else {
                charCount.style.color = "var(--text-secondary)";
            }
        });
    }

    // Handle initial browser routing based on URL path
    const path = window.location.pathname.replace(/^\/|\/$/g, '');
    const validTabs = ['home', 'analyze', 'dashboard', 'history', 'about'];
    if (validTabs.includes(path)) {
        switchTab(path, false);
    } else {
        switchTab('home', false);
    }
});

// Single Page Navigation
function navigate(tab, event) {
    if (event) {
        event.preventDefault();
    }
    switchTab(tab, true);
}

function switchTab(tabId, updateHistoryState = true) {
    currentTab = tabId;
    
    // Update active class on nav links
    document.querySelectorAll(".nav-item").forEach(item => {
        if (item.getAttribute("data-tab") === tabId) {
            item.classList.add("active");
        } else {
            item.classList.remove("active");
        }
    });

    // Toggle Section Views
    document.querySelectorAll(".tab-pane").forEach(pane => {
        if (pane.id === `tab-${tabId}`) {
            pane.classList.add("active");
        } else {
            pane.classList.remove("active");
        }
    });

    // Trigger tab-specific loaders
    if (tabId === 'dashboard') {
        loadDashboardStats();
    } else if (tabId === 'history') {
        loadHistoryList();
    }

    // Update browser URL address bar
    if (updateHistoryState) {
        window.history.pushState({ tab: tabId }, "", `/${tabId === 'home' ? '' : tabId}`);
    }
}

// Listen to browser Back/Forward navigation
window.onpopstate = function(event) {
    if (event.state && event.state.tab) {
        switchTab(event.state.tab, false);
    } else {
        switchTab('home', false);
    }
};

// Input modes (Text area / URL scan)
function switchInputMode(mode) {
    currentInputMode = mode;
    const btnText = document.getElementById("tab-input-text");
    const btnUrl = document.getElementById("tab-input-url");
    const groupText = document.getElementById("group-text-input");
    const groupUrl = document.getElementById("group-url-input");

    if (mode === 'text') {
        btnText.classList.add("active");
        btnUrl.classList.remove("active");
        groupText.classList.remove("hidden");
        groupUrl.classList.add("hidden");
    } else {
        btnText.classList.remove("active");
        btnUrl.classList.add("active");
        groupText.classList.add("hidden");
        groupUrl.classList.remove("hidden");
    }
}

// Running News Analysis
function runAnalysis(event) {
    event.preventDefault();
    
    const requestData = {};
    if (currentInputMode === 'text') {
        const text = document.getElementById("news-text").value.trim();
        if (text.length < 20) {
            showAlert("News text content is too short. Please write at least 20 characters.");
            return;
        }
        requestData.text = text;
        requestData.inputType = "TEXT";
    } else {
        const url = document.getElementById("news-url").value.trim();
        if (!url) {
            showAlert("Please enter a valid URL.");
            return;
        }
        requestData.url = url;
        requestData.inputType = "URL";
    }

    // Toggle views (Form -> Loader)
    document.getElementById("analyze-input-view").classList.add("hidden");
    document.getElementById("analyze-loader-view").classList.remove("hidden");

    // Dynamic scanning labels to look interactive
    const statusMsg = document.getElementById("scanner-status");
    const stepMsgs = [
        "Normalizing whitespace and tokens...",
        "Measuring stylistic ratios (exclamations, capitalized terms)...",
        "Evaluating Naive Bayes vocabulary probability weights...",
        "Identifying sensational headlines and clickbait signatures...",
        "Compiling final credibility confidence scores..."
    ];
    let msgIdx = 0;
    const msgInterval = setInterval(() => {
        if (msgIdx < stepMsgs.length) {
            statusMsg.textContent = stepMsgs[msgIdx++];
        }
    }, 900);

    // Call API
    fetch("/api/analyze", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify(requestData)
    })
    .then(response => {
        clearInterval(msgInterval);
        if (!response.ok) {
            return response.json().then(err => { throw new Error(err.message || "Analysis failed."); });
        }
        return response.json();
    })
    .then(result => {
        renderAnalysisResult(result);
    })
    .catch(error => {
        clearInterval(msgInterval);
        document.getElementById("analyze-loader-view").classList.add("hidden");
        document.getElementById("analyze-input-view").classList.remove("hidden");
        showAlert(error.message || "An error occurred during analysis.");
    });
}

function renderAnalysisResult(result) {
    document.getElementById("analyze-loader-view").classList.add("hidden");
    document.getElementById("analyze-result-view").classList.remove("hidden");

    // General text details
    document.getElementById("res-headline").textContent = result.title;
    document.getElementById("res-source-type").textContent = result.inputType === "URL" ? "Web Article Scan" : "Copied Text Area";
    document.getElementById("res-timestamp").textContent = result.formattedTimestamp;

    // Classification outcomes
    const predBadge = document.getElementById("res-prediction-badge");
    predBadge.textContent = result.prediction.replace("_", " ");
    
    // Change prediction text color class
    predBadge.className = "prediction-text";
    if (result.prediction === "LIKELY_GENUINE") {
        predBadge.classList.add("text-genuine");
    } else if (result.prediction === "LIKELY_FAKE") {
        predBadge.classList.add("text-fake");
    } else {
        predBadge.classList.add("text-uncertain");
    }

    // Risk level badge
    const riskBadge = document.getElementById("res-risk-badge");
    riskBadge.textContent = `${result.riskLevel} RISK`;
    riskBadge.className = "badge";
    if (result.riskLevel === "LOW") {
        riskBadge.classList.add("badge-low");
    } else if (result.riskLevel === "HIGH") {
        riskBadge.classList.add("badge-high");
    } else {
        riskBadge.classList.add("badge-medium");
    }

    // Credibility Score ring/gauge
    const scorePct = document.getElementById("res-score-pct");
    scorePct.textContent = `${result.confidenceScore}%`;

    const circle = document.getElementById("res-score-ring");
    const radius = circle.r.baseVal.value;
    const circumference = radius * 2 * Math.PI;
    circle.style.strokeDasharray = `${circumference} ${circumference}`;
    
    // Set circle stroke color depending on credibility score
    if (result.confidenceScore >= 60) {
        circle.style.stroke = "var(--genuine-color)";
    } else if (result.confidenceScore >= 40) {
        circle.style.stroke = "var(--uncertain-color)";
    } else {
        circle.style.stroke = "var(--fake-color)";
    }
    
    // Progress fill offset
    const offset = circumference - (result.confidenceScore / 100) * circumference;
    circle.style.strokeDashoffset = offset;

    // Dynamic indicators list
    const indicatorsUl = document.getElementById("res-indicators-list");
    indicatorsUl.innerHTML = "";
    result.indicators.forEach(ind => {
        const li = document.createElement("li");
        
        // Determine check/x icon based on positive/negative features
        const isPositive = ind.includes("(+)") || ind.includes("verifiable") || ind.includes("includes");
        const iconClass = isPositive ? "fa-solid fa-circle-check indicator-icon success" : "fa-solid fa-circle-xmark indicator-icon danger";
        
        li.innerHTML = `<i class="${iconClass}"></i> <span>${ind.replace(" (+)", "").replace(" (-)", "")}</span>`;
        indicatorsUl.appendChild(li);
    });

    // Statistic Values
    document.getElementById("stats-words").textContent = result.wordCount.toLocaleString();
    document.getElementById("stats-sentences").textContent = result.sentenceCount.toLocaleString();
    document.getElementById("stats-chars").textContent = result.characterCount.toLocaleString();
    document.getElementById("stats-exclamations").textContent = result.exclamationCount;
    document.getElementById("stats-questions").textContent = result.questionCount;
    document.getElementById("stats-caps").textContent = result.capitalizedWordsCount;
}

function resetAnalysis() {
    // Clear inputs
    document.getElementById("news-text").value = "";
    document.getElementById("news-url").value = "";
    document.getElementById("char-count").textContent = "0";

    // Toggle views
    document.getElementById("analyze-result-view").classList.add("hidden");
    document.getElementById("analyze-input-view").classList.remove("hidden");
}

// Analytical Dashboard charts
function loadDashboardStats() {
    fetch("/api/dashboard")
    .then(response => response.json())
    .then(stats => {
        const emptyState = document.getElementById("dashboard-empty-state");
        const contentView = document.getElementById("dashboard-content-view");

        if (stats.totalAnalyses === 0) {
            emptyState.classList.remove("hidden");
            contentView.classList.add("hidden");
            return;
        }

        emptyState.classList.add("hidden");
        contentView.classList.remove("hidden");

        // Metrics Panel
        document.getElementById("dash-total").textContent = stats.totalAnalyses.toLocaleString();
        document.getElementById("dash-fake").textContent = stats.fakeCount.toLocaleString();
        document.getElementById("dash-genuine").textContent = stats.genuineCount.toLocaleString();
        document.getElementById("dash-avg-conf").textContent = `${stats.averageConfidence}%`;

        // Render Charts
        renderTimelineChart(stats.timelineLabels, stats.timelineCounts);
        renderDistributionChart([stats.fakeCount, stats.genuineCount, stats.uncertainCount]);
        renderRiskChart(stats.riskLabels, stats.riskCounts);
    })
    .catch(error => {
        showAlert("Failed to retrieve dashboard statistics.");
    });
}

function renderTimelineChart(labels, data) {
    const ctx = document.getElementById("chart-timeline").getContext("2d");
    if (chartTimeline) chartTimeline.destroy();

    chartTimeline = new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [{
                label: 'Analyses Run',
                data: data,
                borderColor: '#4f46e5',
                backgroundColor: 'rgba(79, 70, 229, 0.1)',
                borderWidth: 3,
                fill: true,
                tension: 0.3,
                pointBackgroundColor: '#4f46e5',
                pointRadius: 4
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: {
                y: { beginAtZero: true, grid: { color: '#e2e8f0' }, ticks: { stepSize: 1 } },
                x: { grid: { display: false } }
            }
        }
    });
}

function renderDistributionChart(data) {
    const ctx = document.getElementById("chart-distribution").getContext("2d");
    if (chartDistribution) chartDistribution.destroy();

    chartDistribution = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: ['Likely Fake', 'Likely Genuine', 'Uncertain'],
            datasets: [{
                data: data,
                backgroundColor: ['#ef4444', '#10b981', '#f59e0b'],
                borderWidth: 2,
                borderColor: '#ffffff'
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { position: 'bottom', labels: { boxWidth: 12, padding: 16 } }
            },
            cutout: '65%'
        }
    });
}

function renderRiskChart(labels, data) {
    const ctx = document.getElementById("chart-risk").getContext("2d");
    if (chartRisk) chartRisk.destroy();

    chartRisk = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [{
                data: data,
                backgroundColor: ['#fee2e2', '#fef3c7', '#d1fae5'],
                borderColor: ['#ef4444', '#f59e0b', '#10b981'],
                borderWidth: 1,
                borderRadius: 6
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: {
                y: { beginAtZero: true, grid: { color: '#e2e8f0' }, ticks: { stepSize: 1 } },
                x: { grid: { display: false } }
            }
        }
    });
}

// Log History List operations
function loadHistoryList() {
    const search = document.getElementById("history-search").value.trim();
    const prediction = document.getElementById("filter-prediction").value;
    const risk = document.getElementById("filter-risk").value;

    let url = `/api/history?page=${historyPage}&size=${historySize}`;
    if (search) url += `&query=${encodeURIComponent(search)}`;
    if (prediction) url += `&prediction=${prediction}`;
    if (risk) url += `&riskLevel=${risk}`;

    fetch(url)
    .then(response => response.json())
    .then(data => {
        renderHistoryRows(data.content);
        renderPagination(data);
    })
    .catch(error => {
        showAlert("Failed to retrieve history logs.");
    });
}

function triggerHistorySearch() {
    clearTimeout(historyQueryTimeout);
    historyQueryTimeout = setTimeout(() => {
        historyPage = 0; // reset page on filter change
        loadHistoryList();
    }, 300);
}

function renderHistoryRows(records) {
    const tbody = document.getElementById("history-tbody");
    tbody.innerHTML = "";

    const emptyState = document.getElementById("history-empty-state");
    if (records.length === 0) {
        emptyState.classList.remove("hidden");
        return;
    }
    emptyState.classList.add("hidden");

    records.forEach(rec => {
        const tr = document.createElement("tr");

        // Format prediction cell
        let predClass = "text-uncertain";
        if (rec.prediction === "LIKELY_GENUINE") predClass = "text-genuine";
        else if (rec.prediction === "LIKELY_FAKE") predClass = "text-fake";

        // Format risk badge
        let riskClass = "badge-medium";
        if (rec.riskLevel === "LOW") riskClass = "badge-low";
        else if (rec.riskLevel === "HIGH") riskClass = "badge-high";

        // Truncate preview
        const excerpt = rec.originalText ? rec.originalText : "";

        tr.innerHTML = `
            <td style="white-space: nowrap;">${rec.formattedTimestamp}</td>
            <td>
                <span class="table-title-preview" title="${rec.title}">${rec.title}</span>
                <span class="table-text-excerpt">${excerpt}</span>
            </td>
            <td><span class="result-source-tag">${rec.inputType}</span></td>
            <td><strong class="${predClass}">${rec.prediction.replace("_", " ")}</strong></td>
            <td><strong>${rec.confidenceScore}%</strong></td>
            <td><span class="badge ${riskClass}">${rec.riskLevel}</span></td>
            <td class="text-right" style="white-space: nowrap;">
                <button onclick="inspectRecord(${rec.id})" class="btn btn-small btn-secondary" title="Inspect log">
                    <i class="fa-solid fa-eye"></i> Details
                </button>
                <button onclick="deleteRecord(${rec.id}, event)" class="btn btn-small btn-secondary" style="color: var(--fake-color); margin-left: 4px;" title="Delete log">
                    <i class="fa-solid fa-trash-can"></i>
                </button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

function renderPagination(pageObj) {
    const pagPanel = document.getElementById("history-pagination");
    if (pageObj.totalElements === 0) {
        pagPanel.classList.add("hidden");
        return;
    }
    pagPanel.classList.remove("hidden");

    historyTotalPages = pageObj.totalPages;

    // Bounds display
    const startIdx = (pageObj.number * pageObj.size) + 1;
    const endIdx = Math.min(startIdx + pageObj.numberOfElements - 1, pageObj.totalElements);

    document.getElementById("pag-start").textContent = startIdx;
    document.getElementById("pag-end").textContent = endIdx;
    document.getElementById("pag-total-items").textContent = pageObj.totalElements;

    // Disabled status
    const prevBtn = document.getElementById("pag-prev");
    const nextBtn = document.getElementById("pag-next");
    
    prevBtn.disabled = pageObj.first;
    nextBtn.disabled = pageObj.last;
}

function changeHistoryPage(direction) {
    const targetPage = historyPage + direction;
    if (targetPage >= 0 && targetPage < historyTotalPages) {
        historyPage = targetPage;
        loadHistoryList();
    }
}

// Inspect History record details
function inspectRecord(id) {
    fetch(`/api/history/${id}`)
    .then(response => {
        if (!response.ok) throw new Error("Could not find record details.");
        return response.json();
    })
    .then(record => {
        currentModalRecordId = record.id;
        const modalBody = document.getElementById("modal-content");
        
        let predClass = "text-uncertain";
        if (record.prediction === "LIKELY_GENUINE") predClass = "text-genuine";
        else if (record.prediction === "LIKELY_FAKE") predClass = "text-fake";

        let riskClass = "badge-medium";
        if (record.riskLevel === "LOW") riskClass = "badge-low";
        else if (record.riskLevel === "HIGH") riskClass = "badge-high";

        // Build HTML
        let indicatorsHtml = "";
        record.indicators.forEach(ind => {
            const isPositive = ind.includes("(+)") || ind.includes("verifiable") || ind.includes("includes");
            const iconClass = isPositive ? "fa-solid fa-circle-check success" : "fa-solid fa-circle-xmark danger";
            indicatorsHtml += `<li><i class="fa-solid ${iconClass} indicator-icon"></i> <span>${ind}</span></li>`;
        });

        modalBody.innerHTML = `
            <div class="result-layout" style="grid-template-columns: 1fr;">
                <div>
                    <h2 class="res-title-text">${record.title}</h2>
                    <div style="margin: 8px 0 16px 0; display: flex; gap: 8px; align-items: center;">
                        <span class="result-source-tag">${record.inputType}</span>
                        <span class="meta-val" style="font-size: 13px;">${record.formattedTimestamp}</span>
                    </div>
                    
                    <div class="card" style="padding: 16px; margin-bottom: 20px; background-color: #f8fafc;">
                        <div class="score-description-block" style="flex-direction: row; gap: 40px;">
                            <div>
                                <span class="meta-label">PREDICTION</span>
                                <h3 class="${predClass}" style="font-size: 20px; font-weight: 800;">${record.prediction.replace("_", " ")}</h3>
                            </div>
                            <div>
                                <span class="meta-label">CONFIDENCE</span>
                                <h3 style="font-size: 20px; font-weight: 800;">${record.confidenceScore}%</h3>
                            </div>
                            <div>
                                <span class="meta-label">RISK LEVEL</span>
                                <div><span class="badge ${riskClass}">${record.riskLevel}</span></div>
                            </div>
                        </div>
                    </div>

                    <div style="margin-bottom: 24px;">
                        <h4>Extracted Style Indicators</h4>
                        <ul class="indicators-list" style="margin-top: 10px;">
                            ${indicatorsHtml}
                        </ul>
                    </div>

                    <div class="card stats-card" style="margin-bottom: 24px;">
                        <h4>Linguistic Style Statistics</h4>
                        <div class="score-description-block" style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-top: 12px;">
                            <div>
                                <span class="meta-label">WORDS</span>
                                <div class="meta-val">${record.wordCount.toLocaleString()}</div>
                            </div>
                            <div>
                                <span class="meta-label">CHARACTERS</span>
                                <div class="meta-val">${record.characterCount.toLocaleString()}</div>
                            </div>
                            <div>
                                <span class="meta-label">SENTENCES</span>
                                <div class="meta-val">${record.sentenceCount.toLocaleString()}</div>
                            </div>
                            <div>
                                <span class="meta-label">EXCLAMATIONS</span>
                                <div class="meta-val">${record.exclamationCount}</div>
                            </div>
                            <div>
                                <span class="meta-label">QUESTIONS</span>
                                <div class="meta-val">${record.questionCount}</div>
                            </div>
                            <div>
                                <span class="meta-label">ALL_CAPS WORDS</span>
                                <div class="meta-val">${record.capitalizedWordsCount}</div>
                            </div>
                        </div>
                    </div>

                    <div>
                        <h4>Analyzed Content Excerpt</h4>
                        <textarea class="form-textarea" readonly style="height: 150px; font-size: 13px; margin-top: 8px; background-color: #f8fafc; color: var(--text-secondary);">${record.originalText || ""}</textarea>
                    </div>
                </div>
            </div>
        `;
        
        document.getElementById("details-modal").classList.remove("hidden");
    })
    .catch(error => {
        showAlert("Failed to load details: " + error.message);
    });
}

function closeDetailsModal() {
    currentModalRecordId = null;
    document.getElementById("details-modal").classList.add("hidden");
}

function deleteCurrentModalRecord() {
    if (!currentModalRecordId) return;
    const idToDelete = currentModalRecordId;
    closeDetailsModal();
    deleteRecord(idToDelete);
}

function clearAllHistory() {
    if (!confirm("Are you sure you want to permanently delete ALL history records? This action cannot be undone.")) {
        return;
    }

    fetch('/api/history', {
        method: "DELETE"
    })
    .then(response => {
        if (!response.ok) throw new Error("Failed to clear history.");
        historyPage = 0;
        loadHistoryList();
        showAlert("All history logs have been cleared successfully.", "success");
    })
    .catch(error => {
        showAlert("Failed to clear history: " + error.message);
    });
}

// Delete History record
function deleteRecord(id, event) {
    if (event) event.stopPropagation();

    if (!confirm("Are you sure you want to permanently delete this analysis record from the history log?")) {
        return;
    }

    fetch(`/api/history/${id}`, {
        method: "DELETE"
    })
    .then(response => {
        if (!response.ok) throw new Error("Deletion failed.");
        
        // Refresh history page
        loadHistoryList();
        
        // Show success alert
        showAlert("Record successfully deleted from database.", "success");
    })
    .catch(error => {
        showAlert("Failed to delete record: " + error.message);
    });
}

// General Alerts utility
function showAlert(message, type = "danger") {
    const alertBanner = document.getElementById("alert-banner");
    const alertMsg = document.getElementById("alert-message");
    
    alertMsg.textContent = message;
    alertBanner.className = "alert-banner animate-fade-in";
    
    if (type === "success") {
        alertBanner.style.backgroundColor = "#d1fae5";
        alertBanner.style.color = "#065f46";
        alertBanner.style.borderColor = "#a7f3d0";
        alertBanner.style.borderLeft = "5px solid var(--genuine-color)";
    } else {
        alertBanner.style.backgroundColor = "#fef2f2";
        alertBanner.style.color = "#991b1b";
        alertBanner.style.borderColor = "#fee2e2";
        alertBanner.style.borderLeft = "5px solid var(--fake-color)";
    }

    // Auto dismiss after 6 seconds
    setTimeout(() => {
        closeAlert();
    }, 6000);
}

function closeAlert() {
    const alertBanner = document.getElementById("alert-banner");
    if (alertBanner) {
        alertBanner.classList.add("hidden");
    }
}
