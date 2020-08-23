const System = Java.type("java.lang.System");

export function onPreLaunch() {
  console.time();
  console.warn("cursed language");
  console.timeEnd();
  System.exit(0);
}
