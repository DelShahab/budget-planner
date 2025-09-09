import { O as Ol, J as Jt, M as Mi, _ as _r, A as Al, U as Ui, G as Ga, a as _s, $ as $$1, S as Sr, Y as Ya } from "./indexhtml-BRNpU1Lv.js";
import { g as g$1 } from "./state-eevs1asN-DiDcFo2w.js";
import { o } from "./base-panel-Cl8rL5aQ-pPCPCZ8L.js";
import { r as r$1 } from "./icons-DrBP115r-DjSoVei5.js";
const L = "copilot-log-panel{padding:var(--space-100);font:var(--font-xsmall);display:flex;flex-direction:column;gap:var(--space-50);overflow-y:auto}copilot-log-panel .row{display:flex;align-items:flex-start;padding:var(--space-50) var(--space-100);border-radius:var(--radius-2);gap:var(--space-100)}copilot-log-panel .row.information{background-color:var(--blue-50)}copilot-log-panel .row.warning{background-color:var(--yellow-50)}copilot-log-panel .row.error{background-color:var(--red-50)}copilot-log-panel .type{margin-top:var(--space-25)}copilot-log-panel .type.error{color:var(--red)}copilot-log-panel .type.warning{color:var(--yellow)}copilot-log-panel .type.info{color:var(--color)}copilot-log-panel .message{display:flex;flex-direction:column;flex-grow:1;gap:var(--space-25);overflow:hidden}copilot-log-panel .message>*{white-space:nowrap}copilot-log-panel .firstrow{display:flex;align-items:baseline;gap:.5em;flex-direction:column}copilot-log-panel .firstrowmessage{width:100%}copilot-log-panel button{padding:0;border:0;background:transparent}copilot-log-panel svg{height:12px;width:12px}copilot-log-panel .secondrow,copilot-log-panel .timestamp{font-size:var(--font-size-0);line-height:var(--line-height-1)}copilot-log-panel .expand span{height:12px;width:12px}";
var b = Object.defineProperty, I = Object.getOwnPropertyDescriptor, h = (e, t, a, o2) => {
  for (var s = o2 > 1 ? void 0 : o2 ? I(t, a) : t, p = e.length - 1, i; p >= 0; p--)
    (i = e[p]) && (s = (o2 ? i(t, a, s) : i(s)) || s);
  return o2 && s && b(t, a, s), s;
};
class _ {
  constructor() {
    this.showTimestamps = false, Sr(this);
  }
  toggleShowTimestamps() {
    this.showTimestamps = !this.showTimestamps;
  }
}
const g = new _();
let r = class extends o {
  constructor() {
    super(), this.unreadErrors = false, this.messages = [], this.nextMessageId = 1, this.transitionDuration = 0, this.catchErrors();
  }
  connectedCallback() {
    super.connectedCallback(), this.onCommand("log", (e) => {
      this.handleLogEventData({ type: e.data.type, message: e.data.message });
    }), this.onEventBus("log", (e) => this.handleLogEvent(e)), this.onEventBus("update-log", (e) => this.updateLog(e.detail)), this.onEventBus("notification-dismissed", (e) => this.handleNotification(e)), this.onEventBus("clear-log", () => this.clear()), this.transitionDuration = parseInt(
      window.getComputedStyle(this).getPropertyValue("--dev-tools-transition-duration"),
      10
    );
  }
  clear() {
    this.messages = [];
  }
  handleNotification(e) {
    this.log(e.detail.type, e.detail.message, true, e.detail.details, e.detail.link, void 0);
  }
  handleLogEvent(e) {
    this.handleLogEventData(e.detail);
  }
  handleLogEventData(e) {
    this.log(
      e.type,
      e.message,
      !!e.internal,
      e.details,
      e.link,
      Ol(e.expandedMessage),
      Ol(e.expandedDetails),
      e.id
    );
  }
  activate() {
    this.unreadErrors = false, this.updateComplete.then(() => {
      const e = this.renderRoot.querySelector(".message:last-child");
      e && e.scrollIntoView();
    });
  }
  format(e) {
    return e.message ? e.message.toString() : e.toString();
  }
  catchErrors() {
    const e = window.Vaadin.ConsoleErrors;
    window.Vaadin.ConsoleErrors = {
      push: (t) => {
        Jt.attentionRequiredPanelTag = v.tag, t[0].type !== void 0 && t[0].message !== void 0 ? this.log(t[0].type, t[0].message, !!t[0].internal, t[0].details, t[0].link) : this.log(Mi.ERROR, t.map((a) => this.format(a)).join(" "), false), e.push(t);
      }
    };
  }
  render() {
    return _r`<style>
        ${L}
      </style>
      ${this.messages.map((e) => this.renderMessage(e))} `;
  }
  renderMessage(e) {
    let t, a, o2;
    return e.type === Mi.ERROR ? (t = "error", o2 = r$1.exclamationMark, a = "Error") : e.type === Mi.WARNING ? (t = "warning", o2 = r$1.warning, a = "Warning") : (t = "info", o2 = r$1.info, a = "Info"), e.internal && (t += " internal"), _r`
      <div class="row ${e.type} ${e.details || e.link ? "has-details" : ""}">
        <span class="type ${t}" title="${a}">${o2}</span>
        <div class="message" @click=${() => this.toggleExpanded(e)}>
          <span class="firstrow">
            <span class="timestamp" ?hidden=${!g.showTimestamps}>${A(e.timestamp)}</span>
            <span class="firstrowmessage"
              >${e.expanded && e.expandedMessage ? e.expandedMessage : e.message}
            </span>
          </span>
          ${e.expanded ? _r` <span class="secondrow">${e.expandedDetails}</span>` : _r`<span class="secondrow" ?hidden="${!e.details && !e.link}"
                >${Ol(e.details)}
                ${e.link ? _r`<a class="ahreflike" href="${e.link}" target="_blank">Learn more</a>` : ""}</span
              >`}
        </div>
        <button
          aria-label="Expand details"
          theme="icon tertiary"
          class="expand"
          @click=${() => this.toggleExpanded(e)}
          ?hidden=${!e.expandedDetails}>
          <span>${e.expanded ? r$1.chevronDown : r$1.chevronRight}</span>
        </button>
      </div>
    `;
  }
  log(e, t, a, o2, s, p, i, y) {
    const $ = this.nextMessageId;
    this.nextMessageId += 1;
    const u = Al(t, 200);
    for (u !== t && !i && (i = t), this.messages.push({
      id: $,
      type: e,
      message: u,
      details: o2,
      link: s,
      dontShowAgain: false,
      deleted: false,
      expanded: false,
      expandedMessage: p,
      expandedDetails: i,
      timestamp: /* @__PURE__ */ new Date(),
      internal: a,
      userId: y
    }); this.messages.length > r.MAX_LOG_ROWS; )
      this.messages.shift();
    this.requestUpdate(), this.updateComplete.then(() => {
      const m = this.renderRoot.querySelector(".message:last-child");
      m ? (setTimeout(() => m.scrollIntoView({ behavior: "smooth" }), this.transitionDuration), this.unreadErrors = false) : e === Mi.ERROR && (this.unreadErrors = true);
    });
  }
  updateLog(e) {
    const t = this.messages.find((a) => a.userId === e.id);
    if (!t) {
      Ui(`Unable to find message with id ${e.id}`);
      return;
    }
    Object.assign(t, e), Ga(t.expandedDetails) && (t.expandedDetails = Ol(t.expandedDetails)), this.requestUpdate();
  }
  toggleExpanded(e) {
    e.expandedDetails && (e.expanded = !e.expanded, this.requestUpdate());
  }
};
r.MAX_LOG_ROWS = 1e3;
h([
  g$1()
], r.prototype, "unreadErrors", 2);
h([
  g$1()
], r.prototype, "messages", 2);
r = h([
  Ya("copilot-log-panel")
], r);
let f = class extends _s {
  createRenderRoot() {
    return this;
  }
  connectedCallback() {
    super.connectedCallback(), this.style.display = "flex";
  }
  render() {
    return _r`
      <button title="Clear log" aria-label="Clear log" theme="icon tertiary">
        <span
          @click=${() => {
      $$1.emit("clear-log", {});
    }}
          >${r$1.trash}</span
        >
      </button>
      <button title="Toggle timestamps" aria-label="Toggle timestamps" theme="icon tertiary">
        <span
          class="${g.showTimestamps ? "on" : "off"}"
          @click=${() => {
      g.toggleShowTimestamps();
    }}
          >${r$1.clock}</span
        >
      </button>
    `;
  }
};
f = h([
  Ya("copilot-log-panel-actions")
], f);
const v = {
  header: "Log",
  expanded: true,
  draggable: true,
  panelOrder: 0,
  panel: "bottom",
  floating: false,
  tag: "copilot-log-panel",
  actionsTag: "copilot-log-panel-actions"
}, P = {
  init(e) {
    e.addPanel(v);
  }
};
window.Vaadin.copilot.plugins.push(P);
const B = { hour: "numeric", minute: "numeric", second: "numeric", fractionalSecondDigits: 3 }, q = new Intl.DateTimeFormat(navigator.language, B);
function A(e) {
  return q.format(e);
}
export {
  f as Actions,
  r as CopilotLogPanel
};
