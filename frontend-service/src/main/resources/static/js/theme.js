/* Applies the saved light/dark choice before the page paints (no flash of the wrong theme). */
(function () {
  try {
    var saved = window.localStorage.getItem("fsn-theme");
    if (saved === "light" || saved === "dark") {
      document.documentElement.setAttribute("data-theme", saved);
    }
  } catch (e) {
    /* storage unavailable (private mode): follow the system setting */
  }
})();
