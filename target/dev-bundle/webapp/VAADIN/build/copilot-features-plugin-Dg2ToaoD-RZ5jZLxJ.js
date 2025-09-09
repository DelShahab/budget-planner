import { _ as _r, M as Mi, Y as Ya } from "./indexhtml-BRNpU1Lv.js";
import { g } from "./state-eevs1asN-DiDcFo2w.js";
import { o } from "./base-panel-Cl8rL5aQ-pPCPCZ8L.js";
import { showNotification as N } from "./copilot-notification-CBUUP7Vd-BeZwEQQG.js";
import { r as r$1 } from "./icons-DrBP115r-DjSoVei5.js";
const v = "copilot-features-panel{padding:var(--space-100);font:var(--font-xsmall);display:grid;grid-template-columns:auto 1fr;gap:var(--space-50);height:auto}copilot-features-panel a{display:flex;align-items:center;gap:var(--space-50);white-space:nowrap}copilot-features-panel a svg{height:12px;width:12px;min-height:12px;min-width:12px}";
var b = Object.defineProperty, F = Object.getOwnPropertyDescriptor, d = (e, t, a, o2) => {
  for (var r = o2 > 1 ? void 0 : o2 ? F(t, a) : t, s = e.length - 1, l; s >= 0; s--)
    (l = e[s]) && (r = (o2 ? l(t, a, r) : l(r)) || r);
  return o2 && r && b(t, a, r), r;
};
const n = window.Vaadin.devTools;
let i = class extends o {
  constructor() {
    super(...arguments), this.features = [], this.handleFeatureFlags = (e) => {
      this.features = e.data.features;
    };
  }
  connectedCallback() {
    super.connectedCallback(), this.onCommand("featureFlags", this.handleFeatureFlags);
  }
  render() {
    return _r` <style>
        ${v}
      </style>
      ${this.features.map(
      (e) => _r`
          <copilot-toggle-button
            .title="${e.title}"
            ?checked=${e.enabled}
            @on-change=${(t) => this.toggleFeatureFlag(t, e)}>
          </copilot-toggle-button>
          <a class="ahreflike" href="${e.moreInfoLink}" title="Learn more" target="_blank"
            >learn more ${r$1.linkExternal}</a
          >
        `
    )}`;
  }
  toggleFeatureFlag(e, t) {
    const a = e.target.checked;
    n.frontendConnection ? (n.frontendConnection.send("setFeature", { featureId: t.id, enabled: a }), N({
      type: Mi.INFORMATION,
      message: `“${t.title}” ${a ? "enabled" : "disabled"}`,
      details: t.requiresServerRestart ? "This feature requires a server restart" : void 0,
      dismissId: `feature${t.id}${a ? "Enabled" : "Disabled"}`
    })) : n.log("error", `Unable to toggle feature ${t.title}: No server connection available`);
  }
};
d([
  g()
], i.prototype, "features", 2);
i = d([
  Ya("copilot-features-panel")
], i);
const $ = {
  header: "Features",
  expanded: true,
  draggable: true,
  panelOrder: 20,
  panel: "right",
  floating: false,
  tag: "copilot-features-panel"
}, w = {
  init(e) {
    e.addPanel($);
  }
};
window.Vaadin.copilot.plugins.push(w);
export {
  i as CopilotFeaturesPanel
};
