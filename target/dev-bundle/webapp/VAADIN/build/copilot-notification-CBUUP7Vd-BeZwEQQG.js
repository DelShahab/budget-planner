import { b as _, $ as $$1, z as zi } from "./indexhtml-BRNpU1Lv.js";
const a = 5e3;
let o = 1;
function c(i) {
  _.notifications.includes(i) && (i.dontShowAgain && i.dismissId && m(i.dismissId), _.removeNotification(i), $$1.emit("notification-dismissed", i));
}
function d(i) {
  return zi.getDismissedNotifications().includes(i);
}
function m(i) {
  d(i) || zi.addDismissedNotification(i);
}
function r(i) {
  return !(i.dismissId && (d(i.dismissId) || _.notifications.find((t) => t.dismissId === i.dismissId)));
}
function N(i) {
  r(i) && u(i);
}
function u(i) {
  const t = o;
  o += 1;
  const e = { ...i, id: t, dontShowAgain: false, animatingOut: false };
  _.setNotifications([..._.notifications, e]), !i.link && !i.dismissId && setTimeout(() => {
    c(e);
  }, a);
}
export {
  c as dismissNotification,
  N as showNotification
};
