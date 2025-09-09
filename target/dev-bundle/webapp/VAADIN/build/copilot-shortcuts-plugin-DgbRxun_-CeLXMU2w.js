import { Y as Ya, _ as _r, C as Cl } from "./indexhtml-BRNpU1Lv.js";
import { o as o$1 } from "./base-panel-Cl8rL5aQ-pPCPCZ8L.js";
import { h } from "./unsafe-html-B3_veTIk-oQOntmqn.js";
import { r as r$1 } from "./icons-DrBP115r-DjSoVei5.js";
const f = "copilot-shortcuts-panel{font:var(--font-xsmall);padding:var(--space-200);display:flex;flex-direction:column;gap:var(--space-50)}copilot-shortcuts-panel h3{font:var(--font-xsmall-strong);margin:0;padding:0}copilot-shortcuts-panel h3:not(:first-of-type){margin-top:var(--space-200)}copilot-shortcuts-panel ul{list-style:none;margin:0;padding:0 var(--space-50);display:flex;flex-direction:column}copilot-shortcuts-panel ul li{display:flex;align-items:center;gap:var(--space-150);padding:var(--space-75) 0}copilot-shortcuts-panel ul li:not(:last-of-type){border-bottom:1px dashed var(--border-color)}copilot-shortcuts-panel ul li svg{height:16px;width:16px}copilot-shortcuts-panel ul li .kbds{flex:1;text-align:right}copilot-shortcuts-panel kbd{display:inline-block;border-radius:var(--radius-1);border:1px solid var(--border-color);min-width:1em;min-height:1em;text-align:center;margin:0 .1em;padding:.25em;box-sizing:border-box;font-size:var(--font-size-1);font-family:var(--font-family);line-height:1}";
var v = (i, r, n, a) => {
  for (var t = r, s = i.length - 1, p; s >= 0; s--)
    (p = i[s]) && (t = p(t) || t);
  return t;
};
let c = class extends o$1 {
  render() {
    return _r`<style>
        ${f}
      </style>
      <h3>Global</h3>
      <ul>
        <li>${r$1.vaadinLogo} Copilot ${o(Cl.toggleCopilot)}</li>
        <li>${r$1.terminal} Command window ${o(Cl.toggleCommandWindow)}</li>
        <li>${r$1.undo} Undo ${o(Cl.undo)}</li>
        <li>${r$1.redo} Redo ${o(Cl.redo)}</li>
      </ul>
      <h3>Selected component</h3>
      <ul>
        <li>${r$1.code} Go to source ${o(Cl.goToSource)}</li>
        <li>${r$1.copy} Duplicate ${o(Cl.duplicate)}</li>
        <li>${r$1.userUp} Select parent ${o(Cl.selectParent)}</li>
        <li>${r$1.userLeft} Select previous sibling ${o(Cl.selectPreviousSibling)}</li>
        <li>${r$1.userRight} Select first child / next sibling ${o(Cl.selectNextSibling)}</li>
        <li>${r$1.trash} Delete ${o(Cl.delete)}</li>
      </ul>`;
  }
};
c = v([
  Ya("copilot-shortcuts-panel")
], c);
function o(i) {
  return _r`<span class="kbds">${h(i)}</span>`;
}
const x = {
  header: "Keyboard Shortcuts",
  expanded: true,
  expandable: false,
  draggable: true,
  panelOrder: 0,
  floating: false,
  tag: "copilot-shortcuts-panel",
  width: 400,
  height: 475,
  floatingPosition: {
    top: 50,
    left: 50
  }
}, $ = {
  init(i) {
    i.addPanel(x);
  }
};
window.Vaadin.copilot.plugins.push($);
