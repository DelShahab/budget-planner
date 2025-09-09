import { a as _s, $ as $$1 } from "./indexhtml-BRNpU1Lv.js";
class o extends _s {
  constructor() {
    super(...arguments), this.eventBusRemovers = [], this.messageHandlers = {};
  }
  createRenderRoot() {
    return this;
  }
  onEventBus(e, s) {
    this.eventBusRemovers.push($$1.on(e, s));
  }
  disconnectedCallback() {
    super.disconnectedCallback(), this.eventBusRemovers.forEach((e) => e());
  }
  onCommand(e, s) {
    this.messageHandlers[e] = s;
  }
  handleMessage(e) {
    return this.messageHandlers[e.command] ? (this.messageHandlers[e.command].call(this, e), true) : false;
  }
}
export {
  o
};
