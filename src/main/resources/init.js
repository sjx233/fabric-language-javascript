"use strict";

const { nanoTime } = Java.type("java.lang.System");
const Level = Java.type("org.apache.logging.log4j.Level");
const Logger = Java.type("org.apache.logging.log4j.Logger");
const LogManager = Java.type("org.apache.logging.log4j.LogManager");

Reflect.defineProperty(globalThis, "console", {
  writable: true,
  configurable: true,
  value: (() => {
    const logger = LogManager.getLogger("JavaScript");
    const logLevels = new Map;
    logLevels.set("assert", Level.ERROR);
    logLevels.set("debug", Level.DEBUG);
    logLevels.set("error", Level.ERROR);
    logLevels.set("info", Level.INFO);
    logLevels.set("log", Level.INFO);
    logLevels.set("trace", Level.TRACE);
    logLevels.set("warn", Level.WARN);
    logLevels.set("dir", Level.INFO);
    logLevels.set("dirxml", Level.INFO);
    logLevels.set("count", Level.INFO);
    logLevels.set("countReset", Level.WARN);
    logLevels.set("group", Level.INFO);
    logLevels.set("groupCollapsed", Level.INFO);
    logLevels.set("timeLog", Level.INFO);
    logLevels.set("timeEnd", Level.INFO);
    logLevels.set("reportWarning", Level.WARN);
    const countMap = new Map;
    const groupStack = [];
    const timerTable = new Map;
    const format = args => {
      if (args.length === 1) return args;
      const [target, current, ...rest] = args;
      let success = false;
      const newTarget = target.replace(/%[sdifoOc]/, specifier => {
        success = true;
        switch (specifier) {
          case "%s":
          case "%o":
          case "%O":
            return String(current);
          case "%d":
          case "%i":
            return typeof current === "symbol" ? NaN : parseInt(current, 10);
          case "%f":
            return typeof current === "symbol" ? NaN : parseFloat(current);
          case "%c":
            return "";
          default:
            throw new Error("Unreachable");
        }
      });
      if (!success) return args;
      return format([newTarget, ...rest]);
    };
    const print = (logLevel, args) => {
      logger.log(logLevels.get(logLevel), "  ".repeat(groupStack.length) + args.join(" "));
    };
    const log = (logLevel, args) => {
      if (args.length === 0) return;
      print(logLevel, format(args));
    };
    return Object.assign(Object.create({}), {
      assert(condition = false, ...data) {
        if (condition) return;
        const message = "Assertion failed";
        if (typeof data[0] !== "string") data.unshift(message);
        else data[0] = `${message}: ${data[0]}`;
        log("assert", data);
      },
      clear() {
        groupStack.length = 0;
      },
      debug(...data) {
        log("debug", data);
      },
      error(...data) {
        log("error", data);
      },
      info(...data) {
        log("info", data);
      },
      log(...data) {
        log("log", data);
      },
      table(tabularData) {
        log("log", [tabularData]);
      },
      trace(...data) {
        log("trace", data);
      },
      warn(...data) {
        log("warn", data);
      },
      dir(item, options) {
        print("dir", [String(item)], options);
      },
      dirxml(...data) {
        log("dirxml", data.map(String));
      },
      count(label = "default") {
        const value = countMap.has(label) ? countMap.get(label) + 1 : 1;
        countMap.set(label, value);
        log("count", [`${label}: ${value}`]);
      },
      countReset(label = "default") {
        if (countMap.has(label)) countMap.set(label, 0);
        else log("countReset", [`Count for '${label}' does not exist`]);
      },
      group(...data) {
        const group = data.length ? format(data) : "group";
        print("group", [group]);
        groupStack.push(group);
      },
      groupCollapsed(...data) {
        const group = data.length ? format(data) : "group";
        print("groupCollapsed", [group]);
        groupStack.push(group);
      },
      groupEnd() {
        groupStack.pop();
      },
      time(label = "default") {
        if (timerTable.has(label)) {
          print("reportWarning", [`Timer '${label}' already exists`]);
          return;
        }
        timerTable.set(label, nanoTime());
      },
      timeLog(label = "default", ...data) {
        const curTime = nanoTime();
        const startTime = timerTable.get(label);
        if (startTime === undefined) {
          print("reportWarning", [`Timer '${label}' does not exist`]);
          return;
        }
        const duration = ((curTime - startTime) * 0.000001).toFixed(6) + "ms";
        data.unshift(`${label}: ${duration}`);
        print("timeLog", data);
      },
      timeEnd(label = "default") {
        const curTime = nanoTime();
        const startTime = timerTable.get(label);
        if (startTime === undefined) {
          print("reportWarning", [`Timer '${label}' does not exist`]);
          return;
        }
        timerTable.delete(label);
        const duration = ((curTime - startTime) * 0.000001).toFixed(6) + "ms";
        print("timeEnd", [`${label}: ${duration}`]);
      }
    });
  })()
});
console.log("Initialized JavaScript runtime");
