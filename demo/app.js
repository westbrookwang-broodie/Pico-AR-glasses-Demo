const initialState = () => ({
  activeCard: "A",
  openCard: null,
  hiddenCards: new Set(),
  mode: "connect",
  contexts: new Set(),
  discussed: false,
  voiceOn: false,
  summarized: false,
  presence: new Set(["林", "K", "周"]),
  linkPairs: [["common", "question"], ["question", "new"]],
  connectStart: null,
  noteCounter: 0,
  records: ["A 被设为共同关注焦点"]
});

let state = initialState();

const cardData = {
  A: {
    label: "A · 关键词洞察",
    focus: "视觉、语言与动作模型结合后，机器从 “执行工具” 转向 “环境理解者”。",
    record: "A 进入共同讨论，成为当前共同朝向。"
  },
  B: {
    label: "B · 案例补充",
    focus: "空间计算被加入后，A 的应用场景更清楚。",
    record: "B 补充了空间计算场景，增强知识相近度。"
  },
  C: {
    label: "C · 趋势延展",
    focus: "人与空间协同被加入后，A 的趋势边界更清楚。",
    record: "C 补充了人机协同趋势，增强共同朝向。"
  }
};

const workspace = document.querySelector(".workspace");
const globalSvg = document.querySelector(".link-layer");
const board = document.querySelector(".focus-board");
const boardSvg = document.querySelector(".board-links");
const toast = document.querySelector("[data-toast]");
const cardDock = document.querySelector("[data-card-dock]");

const clamp = (value, min, max) => Math.min(Math.max(value, min), max);

function setText(selector, value) {
  document.querySelectorAll(selector).forEach((node) => {
    node.textContent = value;
  });
}

function phase() {
  if (state.summarized) return { step: "4/4", label: "已形成共振摘要", status: "共振摘要已生成 · 可复盘" };
  if (state.linkPairs.length > 2 || state.voiceOn) return { step: "3/4", label: "关注点正在连接", status: "共同关注形成中" };
  if (state.discussed) return { step: "2/4", label: "讨论已启动", status: "讨论中 · 等待连接关注点" };
  return { step: "1/4", label: "等待共同关注", status: `多人协作中 · ${state.presence.size} 人在场` };
}

function render() {
  const currentPhase = phase();
  workspace.classList.toggle("has-open-card", Boolean(state.openCard));

  setText("[data-presence-count]", state.presence.size);
  setText("[data-flow-label]", currentPhase.label);
  setText("[data-flow-step]", currentPhase.step);
  document.querySelector("[data-status-text]").innerHTML = currentPhase.status.includes("人在场")
    ? `多人协作中 · <span data-presence-count>${state.presence.size}</span> 人在场`
    : currentPhase.status;

  document.querySelectorAll("[data-card]").forEach((card) => {
    const id = card.dataset.card;
    const hidden = state.hiddenCards.has(id);
    card.classList.toggle("is-active", id === state.activeCard && !hidden);
    card.classList.toggle("is-open", id === state.openCard && !hidden);
    card.classList.toggle("is-card-hidden", hidden);
    card.setAttribute("aria-hidden", hidden ? "true" : "false");
    card.classList.toggle("is-added", state.contexts.has(id));
  });

  document.querySelector("[data-main-copy]").textContent = cardData[state.activeCard].focus;
  renderDock();
  updatePresencePopover();
  updateLines();
}

function addRecord(text) {
  state.records.push(text);
  if (state.records.length > 8) state.records.shift();
}

function renderDock() {
  const ids = [...state.hiddenCards].join(",");
  if (cardDock.dataset.cards === ids) return;
  cardDock.dataset.cards = ids;
  cardDock.innerHTML = "";
  [...state.hiddenCards].forEach((id) => {
    const button = document.createElement("button");
    const tagClass = id === "A" ? "tag-blue" : id === "B" ? "tag-green" : "tag-purple";
    button.className = "dock-card";
    button.type = "button";
    button.setAttribute("aria-label", `恢复 ${id} 卡`);
    button.innerHTML = `<span class="tag ${tagClass}">${id}</span><span>${cardData[id].label.split("·")[1].trim()}</span>`;
    button.addEventListener("click", () => openCard(id));
    cardDock.append(button);
  });
}

function showToast(message) {
  toast.textContent = message;
  toast.classList.add("is-visible");
  window.clearTimeout(showToast.timer);
  showToast.timer = window.setTimeout(() => {
    toast.classList.remove("is-visible");
  }, 1900);
}

function chooseCard(card) {
  openCard(card);
}

function openCard(card) {
  state.hiddenCards.delete(card);
  state.activeCard = card;
  state.openCard = state.openCard === card ? null : card;
  addRecord(state.openCard ? `${cardData[card].label} 打开为焦点卡` : `${cardData[card].label} 收起`);
  showToast(state.openCard ? `${cardData[card].label} 已打开` : `${cardData[card].label} 已收起`);
  closePopovers();
  render();
}

function hideCard(card) {
  state.hiddenCards.add(card);
  if (state.openCard === card) state.openCard = null;
  if (state.activeCard === card) {
    state.activeCard = ["A", "B", "C"].find((id) => !state.hiddenCards.has(id)) || card;
  }
  addRecord(`${cardData[card].label} 隐藏到托盘`);
  showToast(`${cardData[card].label} 已隐藏`);
  closePopovers();
  render();
}

function collapseCard() {
  if (!state.openCard) {
    showToast("当前没有打开的卡片");
    closePopovers();
    return;
  }
  addRecord(`${cardData[state.openCard].label} 收起`);
  state.openCard = null;
  showToast("打开的卡片已收起");
  closePopovers();
  render();
}

function addContext(card) {
  state.hiddenCards.delete(card);
  state.activeCard = card;
  state.openCard = card;
  state.contexts.add(card);
  addRecord(cardData[card].record);
  showToast(`${cardData[card].label} 已加入共同讨论`);
  render();
}

function bringFocus() {
  state.hiddenCards.delete("A");
  state.activeCard = "A";
  state.openCard = "A";
  state.discussed = true;
  const contextLabel = [...state.contexts].map((id) => id).join("、");
  addRecord(contextLabel ? `A 与 ${contextLabel} 合并为共同讨论焦点` : cardData.A.record);
  showToast(contextLabel ? `A 已带入讨论，并关联 ${contextLabel}` : "A 已带入讨论");
  render();
}

function setMode(mode) {
  state.mode = mode;
  if (mode !== "select") state.openCard = null;
  state.connectStart = null;
  document.querySelectorAll(".note").forEach((note) => note.classList.remove("is-connecting"));
  document.querySelectorAll("[data-mode]").forEach((button) => {
    button.classList.toggle("is-active", button.dataset.mode === mode);
  });

  if (mode === "voice") {
    toggleVoice();
    return;
  }

  if (state.voiceOn) {
    state.voiceOn = false;
    document.body.classList.remove("is-voice");
    document.querySelector('[data-mode="voice"]').classList.remove("is-listening");
  }

  if (mode === "more") {
    togglePopover("more", document.querySelector('[data-mode="more"]'));
  } else {
    closePopovers();
    showToast(`${modeName(mode)}模式`);
  }
  render();
}

function modeName(mode) {
  return { select: "选择", annotate: "标注", connect: "连接", voice: "语音", more: "更多" }[mode] || "演示";
}

function toggleVoice() {
  state.voiceOn = !state.voiceOn;
  document.body.classList.toggle("is-voice", state.voiceOn);
  document.querySelector('[data-mode="voice"]').classList.toggle("is-listening", state.voiceOn);
  closePopovers();

  if (state.voiceOn) {
    addRecord("语音捕捉到：空间反馈会影响机器行动策略");
    showToast("语音正在生成新焦点");
    window.clearTimeout(toggleVoice.timer);
    toggleVoice.timer = window.setTimeout(() => {
      if (!document.querySelector('[data-node="voice-focus"]')) {
        createNote("voice-focus", "语音焦点", "note-blue", 33, 66);
        state.linkPairs.push(["voice-focus", "question"]);
      }
      render();
    }, 650);
  } else {
    addRecord("语音捕捉暂停");
    showToast("语音已暂停");
  }
  render();
}

function handleNoteClick(note) {
  const id = note.dataset.node;

  if (state.mode === "connect") {
    if (!state.connectStart) {
      state.connectStart = id;
      note.classList.add("is-connecting");
      showToast(`连接起点：${note.textContent.trim()}`);
      return;
    }

    if (state.connectStart !== id) {
      const pair = [state.connectStart, id];
      const exists = state.linkPairs.some(([a, b]) => (a === pair[0] && b === pair[1]) || (a === pair[1] && b === pair[0]));
      if (exists) {
        showToast("这条关注连接已经存在");
      } else {
        state.linkPairs.push(pair);
        addRecord(`建立连接：${nodeLabel(pair[0])} → ${nodeLabel(pair[1])}`);
        showToast("关注连接已建立");
      }
    }

    state.connectStart = null;
    document.querySelectorAll(".note").forEach((node) => node.classList.remove("is-connecting"));
    render();
    return;
  }

  document.querySelectorAll(".note").forEach((node) => node.classList.remove("is-selected"));
  note.classList.add("is-selected");

  if (state.mode === "annotate") {
    addRecord(`标注 ${note.textContent.trim()}：需要继续追问`);
    showToast("标注已更新当前焦点");
  } else {
    addRecord(`选中关注点：${note.textContent.trim()}`);
    showToast(`已选中 ${note.textContent.trim()}`);
  }
  render();
}

function nodeLabel(id) {
  const node = document.querySelector(`[data-node="${id}"]`);
  return node ? node.textContent.trim() : id;
}

function createNote(id, label, className, left, top) {
  const note = document.createElement("button");
  note.className = `note ${className}`;
  note.type = "button";
  note.dataset.node = id;
  note.style.left = `${left}%`;
  note.style.top = `${top}%`;
  note.innerHTML = `<span class="pin"></span><span>${label}</span>`;
  board.append(note);
  makeDraggable(note);
  return note;
}

function addNewNote() {
  state.noteCounter += 1;
  const id = `idea-${state.noteCounter}`;
  createNote(id, `观点 ${state.noteCounter}`, "note-green", 62 + state.noteCounter * 3, 62);
  state.linkPairs.push(["question", id]);
  addRecord(`新增观点 ${state.noteCounter}，并连接到疑问点`);
  showToast("新的观点卡已加入画布");
  closePopovers();
  render();
}

function resetDemo() {
  state = initialState();
  document.querySelectorAll('[data-node^="idea-"], [data-node="voice-focus"]').forEach((node) => node.remove());
  const positions = {
    common: ["17%", "41%"],
    question: ["47%", "55%"],
    new: ["76%", "41%"]
  };
  Object.entries(positions).forEach(([id, [left, top]]) => {
    const node = document.querySelector(`[data-node="${id}"]`);
    node.style.left = left;
    node.style.top = top;
  });
  document.body.classList.remove("is-voice");
  document.querySelector('[data-mode="voice"]').classList.remove("is-listening");
  document.querySelectorAll("[data-mode]").forEach((button) => {
    button.classList.toggle("is-active", button.dataset.mode === state.mode);
  });
  closePopovers();
  showToast("演示已重置");
  render();
}

function makeSummary() {
  state.summarized = true;
  state.discussed = true;
  addRecord("摘要：场景、趋势、疑问被归并为共同焦点");
  closePopovers();
  showToast("共振摘要已生成");
  render();
}

function makeDraggable(note) {
  let dragging = false;
  let start = null;

  note.addEventListener("pointerdown", (event) => {
    dragging = true;
    start = {
      x: event.clientX,
      y: event.clientY,
      left: parseFloat(note.style.left),
      top: parseFloat(note.style.top)
    };
    note.setPointerCapture(event.pointerId);
  });

  note.addEventListener("pointermove", (event) => {
    if (!dragging || !start) return;
    const boardRect = board.getBoundingClientRect();
    const dx = ((event.clientX - start.x) / boardRect.width) * 100;
    const dy = ((event.clientY - start.y) / boardRect.height) * 100;
    note.style.left = `${clamp(start.left + dx, 12, 88)}%`;
    note.style.top = `${clamp(start.top + dy, 32, 76)}%`;
    updateLines();
  });

  note.addEventListener("pointerup", (event) => {
    if (!dragging) return;
    dragging = false;
    note.releasePointerCapture(event.pointerId);
    const moved = Math.abs(event.clientX - start.x) + Math.abs(event.clientY - start.y);
    if (moved < 8) {
      handleNoteClick(note);
    } else {
      addRecord(`移动 ${note.textContent.trim()}，画布连接同步更新`);
      showToast("便签位置已更新");
      render();
    }
    start = null;
  });
}

function pointFor(el, side, containerRect = workspace.getBoundingClientRect()) {
  const rect = el.getBoundingClientRect();
  const x = side === "left" ? rect.left : side === "right" ? rect.right : rect.left + rect.width / 2;
  const y = side === "top" ? rect.top : side === "bottom" ? rect.bottom : rect.top + rect.height / 2;
  return { x: x - containerRect.left, y: y - containerRect.top };
}

function setLine(name, from, to) {
  const line = globalSvg.querySelector(`[data-link="${name}"]`);
  if (!line) return;
  line.setAttribute("x1", from.x);
  line.setAttribute("y1", from.y);
  line.setAttribute("x2", to.x);
  line.setAttribute("y2", to.y);
}

function setDot(name, point) {
  const dot = globalSvg.querySelector(`[data-dot="${name}"]`);
  if (!dot) return;
  dot.setAttribute("cx", point.x);
  dot.setAttribute("cy", point.y);
  dot.setAttribute("r", "7");
}

function updateLines() {
  const a = document.querySelector('[data-card="A"]');
  const b = document.querySelector('[data-card="B"]');
  const c = document.querySelector('[data-card="C"]');
  const question = document.querySelector('[data-node="question"]');
  if (!a || !b || !c || !question) return;

  const containerRect = workspace.getBoundingClientRect();
  const bRight = pointFor(b, "right", containerRect);
  const aLeft = pointFor(a, "left", containerRect);
  const aRight = pointFor(a, "right", containerRect);
  const cLeft = pointFor(c, "left", containerRect);
  const aBottom = pointFor(a, "bottom", containerRect);
  const questionRect = question.getBoundingClientRect();
  const boardPoint = {
    x: questionRect.left + questionRect.width / 2 - containerRect.left,
    y: questionRect.top - containerRect.top
  };

  setLine("b-a", bRight, aLeft);
  setLine("a-c", aRight, cLeft);
  setLine("a-board", aBottom, boardPoint);
  setDot("b", bRight);
  setDot("a-left", aLeft);
  setDot("a-right", aRight);
  setDot("c", cLeft);
  setDot("a-bottom", aBottom);
  setDot("board", boardPoint);
  updateCardLinkVisibility();
  drawBoardLinks();
}

function updateCardLinkVisibility() {
  const hiddenA = state.hiddenCards.has("A");
  const hiddenB = state.hiddenCards.has("B");
  const hiddenC = state.hiddenCards.has("C");
  const map = {
    "b-a": hiddenA || hiddenB,
    "a-c": hiddenA || hiddenC,
    "a-board": hiddenA,
    b: hiddenB,
    "a-left": hiddenA || hiddenB,
    "a-right": hiddenA || hiddenC,
    c: hiddenC,
    "a-bottom": hiddenA,
    board: hiddenA
  };
  Object.entries(map).forEach(([name, hidden]) => {
    const line = globalSvg.querySelector(`[data-link="${name}"]`);
    const dot = globalSvg.querySelector(`[data-dot="${name}"]`);
    if (line) line.classList.toggle("is-hidden-link", hidden);
    if (dot) dot.classList.toggle("is-hidden-link", hidden);
  });
}

function noteCenter(note) {
  const rect = note.getBoundingClientRect();
  const base = board.getBoundingClientRect();
  return {
    x: rect.left + rect.width / 2 - base.left,
    y: rect.top + rect.height / 2 - base.top
  };
}

function drawBoardLinks() {
  boardSvg.innerHTML = "";
  state.linkPairs.forEach(([fromId, toId]) => {
    const from = document.querySelector(`[data-node="${fromId}"]`);
    const to = document.querySelector(`[data-node="${toId}"]`);
    if (!from || !to) return;
    const start = noteCenter(from);
    const end = noteCenter(to);
    const line = document.createElementNS("http://www.w3.org/2000/svg", "line");
    line.setAttribute("x1", start.x);
    line.setAttribute("y1", start.y);
    line.setAttribute("x2", end.x);
    line.setAttribute("y2", end.y);
    boardSvg.append(line);
    [start, end].forEach((point) => {
      const circle = document.createElementNS("http://www.w3.org/2000/svg", "circle");
      circle.setAttribute("cx", point.x);
      circle.setAttribute("cy", point.y);
      circle.setAttribute("r", "6");
      boardSvg.append(circle);
    });
  });
}

function togglePopover(name, anchor) {
  const popover = document.querySelector(`[data-popover="${name}"]`);
  if (!popover) return;
  const shouldOpen = popover.hidden;
  closePopovers();
  if (!shouldOpen) return;
  const anchorRect = anchor.getBoundingClientRect();
  const baseRect = workspace.getBoundingClientRect();
  popover.hidden = false;
  popover.style.left = `${anchorRect.left - baseRect.left}px`;
  popover.style.top = `${anchorRect.bottom - baseRect.top + 10}px`;
  if (name === "presence") updatePresencePopover();
}

function closePopovers() {
  document.querySelectorAll(".popover").forEach((popover) => {
    popover.hidden = true;
  });
}

function updatePresencePopover() {
  document.querySelectorAll("[data-toggle-person]").forEach((button) => {
    button.classList.toggle("is-on", state.presence.has(button.dataset.togglePerson));
  });
}

function updateClock() {
  const now = new Date();
  const time = now.toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit", hour12: false });
  document.querySelector("[data-clock]").textContent = `${time} · K`;
}

document.querySelectorAll("[data-card]").forEach((card) => {
  card.addEventListener("click", (event) => {
    if (event.target.closest("button")) return;
    chooseCard(card.dataset.card);
  });
  card.addEventListener("keydown", (event) => {
    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      chooseCard(card.dataset.card);
    }
  });
});

document.querySelectorAll("[data-action]").forEach((button) => {
  button.addEventListener("click", (event) => {
    const action = button.dataset.action;
    const topic = button.dataset.topic;
    if (action === "add-context") {
      event.stopPropagation();
      addContext(topic);
    }
    if (action === "open-card") {
      event.stopPropagation();
      openCard(topic);
    }
    if (action === "hide-card") {
      event.stopPropagation();
      hideCard(topic);
    }
    if (action === "bring-focus") bringFocus();
    if (action === "presence") togglePopover("presence", button);
    if (action === "collapse-card") collapseCard();
    if (action === "new-note") addNewNote();
    if (action === "reset-demo") resetDemo();
    if (action === "make-summary") makeSummary();
  });
});

document.querySelectorAll("[data-mode]").forEach((button) => {
  button.addEventListener("click", () => setMode(button.dataset.mode));
});

document.querySelectorAll(".note").forEach(makeDraggable);

document.querySelectorAll("[data-tab]").forEach((tab) => {
  tab.addEventListener("click", () => {
    document.querySelectorAll("[data-tab]").forEach((item) => item.classList.remove("is-current"));
    tab.classList.add("is-current");
    addRecord(`切换视图：${tab.textContent.trim()}`);
    showToast(`已切换：${tab.textContent.trim()}`);
    render();
  });
});

document.querySelectorAll("[data-toggle-person]").forEach((button) => {
  button.addEventListener("click", () => {
    const person = button.dataset.togglePerson;
    if (state.presence.has(person) && state.presence.size > 1) {
      state.presence.delete(person);
    } else {
      state.presence.add(person);
    }
    addRecord(`${person} ${state.presence.has(person) ? "加入" : "暂离"}现场`);
    showToast(`${state.presence.size} 人在场`);
    render();
  });
});

document.addEventListener("click", (event) => {
  if (event.target.closest(".popover") || event.target.closest("[data-action='presence']") || event.target.closest("[data-mode='more']")) {
    return;
  }
  closePopovers();
});

window.addEventListener("resize", updateLines);
window.addEventListener("load", () => {
  updateClock();
  render();
});

window.setInterval(updateClock, 30000);
requestAnimationFrame(render);
