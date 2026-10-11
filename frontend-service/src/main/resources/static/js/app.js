/* FinSphere Nexus: small progressive enhancements. Every page works without this file. */
(function () {
  "use strict";

  var INDIA = "en-IN";

  function onReady(callback) {
    if (document.readyState === "loading") {
      document.addEventListener("DOMContentLoaded", callback);
    } else {
      callback();
    }
  }

  function all(selector, root) {
    return Array.prototype.slice.call((root || document).querySelectorAll(selector));
  }

  /* ------------------------------------------------------------ theme */
  function currentTheme() {
    var explicit = document.documentElement.getAttribute("data-theme");
    if (explicit === "light" || explicit === "dark") {
      return explicit;
    }
    return window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
  }

  function setupThemeToggles() {
    var toggles = all("[data-theme-toggle]");
    function describeAll() {
      var next = currentTheme() === "dark" ? "light" : "dark";
      toggles.forEach(function (button) {
        button.setAttribute("aria-label", "Switch to " + next + " theme");
        var label = button.querySelector(".visually-hidden");
        if (label) {
          label.textContent = "Switch to " + next + " theme";
        }
      });
    }
    toggles.forEach(function (button) {
      button.addEventListener("click", function () {
        var next = currentTheme() === "dark" ? "light" : "dark";
        document.documentElement.setAttribute("data-theme", next);
        try {
          window.localStorage.setItem("fsn-theme", next);
        } catch (e) {
          /* not persisted, still applied to this page */
        }
        describeAll();
      });
    });
    describeAll();
  }

  /* ------------------------------------------------------------ navigation drawer (phones and tablets) */
  function setupDrawer() {
    var shell = document.querySelector("[data-shell]");
    var openButton = document.querySelector("[data-nav-open]");
    var nav = document.getElementById("app-nav");
    if (!shell || !openButton || !nav) {
      return;
    }
    function open() {
      shell.setAttribute("data-nav-open", "true");
      openButton.setAttribute("aria-expanded", "true");
      var first = nav.querySelector("a, button");
      if (first) {
        first.focus();
      }
    }
    function close() {
      if (shell.getAttribute("data-nav-open") !== "true") {
        return;
      }
      shell.removeAttribute("data-nav-open");
      openButton.setAttribute("aria-expanded", "false");
      openButton.focus();
    }
    openButton.addEventListener("click", open);
    all("[data-nav-close]").forEach(function (element) {
      element.addEventListener("click", close);
    });
    document.addEventListener("keydown", function (event) {
      if (event.key === "Escape") {
        close();
      }
    });
  }

  /* ------------------------------------------------------------ loading screen for slow pages and saves */
  var loading = (function () {
    var BAR_AFTER = 150;      /* a thin bar once the wait is noticeable */
    var SCREEN_AFTER = 700;   /* the full screen, which also stops a second click */
    var SLOW_AFTER = 8000;    /* say that it is taking longer than usual */
    var STUCK_AFTER = 20000;  /* offer to stop waiting (the gateway answers by 30 s at the latest) */
    var root;
    var message;
    var hint;
    var stopButton;
    var announcer;
    var region;
    var timers = [];
    var active = false;
    var saving = false;

    function later(callback, delay) {
      timers.push(window.setTimeout(function () {
        try {
          callback();
        } catch (e) {
          /* a display problem must never block the page */
        }
      }, delay));
    }

    function restoreForms() {
      all("form[data-submitting]").forEach(function (form) {
        form.removeAttribute("data-submitting");
        all("button[type=submit]", form).forEach(function (button) {
          button.removeAttribute("aria-busy");
          if (button.hasAttribute("data-label")) {
            button.textContent = button.getAttribute("data-label");
          }
        });
      });
    }

    function reset() {
      timers.forEach(function (timer) {
        window.clearTimeout(timer);
      });
      timers = [];
      active = false;
      if (root) {
        root.hidden = true;
        root.removeAttribute("data-state");
        hint.hidden = true;
        hint.textContent = "";
        stopButton.hidden = true;
      }
      if (region) {
        region.removeAttribute("inert");
      }
      document.body.removeAttribute("aria-busy");
      if (announcer) {
        announcer.textContent = "";
      }
      restoreForms();
    }

    function start(text, isSave) {
      if (!root || active) {
        return;
      }
      active = true;
      saving = !!isSave;
      message.textContent = text;
      later(function () {
        root.hidden = false;
        root.setAttribute("data-state", "bar");
      }, BAR_AFTER);
      later(function () {
        root.setAttribute("data-state", "screen");
        if (region) {
          region.setAttribute("inert", "");
        }
        document.body.setAttribute("aria-busy", "true");
        if (announcer) {
          announcer.textContent = text;
        }
      }, SCREEN_AFTER);
      later(function () {
        hint.textContent = "This is taking longer than usual.";
        hint.hidden = false;
      }, SLOW_AFTER);
      later(function () {
        hint.textContent = saving
          ? "The server has not answered yet. If you stop waiting, check the list before trying again, so nothing is saved twice."
          : "The server has not answered yet. You can keep waiting, or stop and try again.";
        stopButton.hidden = false;
        stopButton.focus();
      }, STUCK_AFTER);
    }

    function init() {
      root = document.querySelector("[data-loading-screen]");
      region = document.querySelector("[data-loading-region]");
      announcer = document.querySelector("[data-loading-announce]");
      if (!root) {
        return;
      }
      message = root.querySelector("[data-loading-message]");
      hint = root.querySelector("[data-loading-hint]");
      stopButton = root.querySelector("[data-loading-stop]");
      stopButton.addEventListener("click", function () {
        try {
          window.stop();
        } catch (e) {
          /* older browsers: the page simply stays as it is */
        }
        reset();
      });
      /* Back and forward restore pages from memory exactly as they were left, loading screen included */
      window.addEventListener("pageshow", function (event) {
        if (event.persisted) {
          reset();
        }
      });
    }

    return { init: init, start: start, reset: reset };
  })();

  function opensInThisTab(link, event) {
    if (event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey
        || event.altKey || link.hasAttribute("download") || link.hasAttribute("data-no-loading")) {
      return false;
    }
    var target = link.getAttribute("target");
    return !target || target === "_self";
  }

  function setupLoadingScreen() {
    loading.init();
    document.addEventListener("click", function (event) {
      var link = event.target && event.target.closest ? event.target.closest("a[href]") : null;
      if (!link || !opensInThisTab(link, event)) {
        return;
      }
      var url;
      try {
        url = new URL(link.href, window.location.href);
      } catch (e) {
        return;
      }
      if (url.origin !== window.location.origin) {
        return;
      }
      /* only the fragment changes: the browser scrolls, nothing loads */
      if (url.pathname === window.location.pathname && url.search === window.location.search && url.hash) {
        return;
      }
      loading.start(link.getAttribute("data-loading") || "Loading…", false);
    });
    /* After the form's own checks (confirmation, double submit): a prevented submit shows nothing */
    document.addEventListener("submit", function (event) {
      var form = event.target;
      if (event.defaultPrevented || !form || form.hasAttribute("data-no-loading")) {
        return;
      }
      var target = form.getAttribute("target");
      if (target && target !== "_self") {
        return;
      }
      var isSave = (form.getAttribute("method") || "get").toLowerCase() === "post";
      loading.start(form.getAttribute("data-loading") || (isSave ? "Saving…" : "Loading…"), isSave);
    });
  }

  /* ------------------------------------------------------------ forms */
  function setupForms() {
    all("form[data-submit-once]").forEach(function (form) {
      form.addEventListener("submit", function (event) {
        var question = form.getAttribute("data-confirm");
        if (question && !window.confirm(question)) {
          event.preventDefault();
          return;
        }
        if (form.getAttribute("data-submitting") === "true") {
          event.preventDefault();
          return;
        }
        form.setAttribute("data-submitting", "true");
        all("button[type=submit]", form).forEach(function (button) {
          button.setAttribute("aria-busy", "true");
          var busy = button.getAttribute("data-busy-label");
          if (busy) {
            if (!button.hasAttribute("data-label")) {
              button.setAttribute("data-label", button.textContent);
            }
            button.textContent = busy;
          }
        });
      });
    });

    all("form[data-auto-submit] select").forEach(function (select) {
      select.addEventListener("change", function () {
        if (!select.value) {
          return;
        }
        if (typeof select.form.requestSubmit === "function") {
          select.form.requestSubmit();
        } else {
          loading.start(select.form.getAttribute("data-loading") || "Loading…");
          select.form.submit();
        }
      });
    });
    all("[data-hide-when-scripted]").forEach(function (element) {
      element.hidden = true;
    });

    all("[data-reveal]").forEach(function (button) {
      var input = document.getElementById(button.getAttribute("data-reveal"));
      if (!input) {
        return;
      }
      var label = button.querySelector(".visually-hidden");
      var use = button.querySelector("use");
      button.addEventListener("click", function () {
        var show = input.type === "password";
        input.type = show ? "text" : "password";
        button.setAttribute("aria-pressed", show ? "true" : "false");
        if (label) {
          label.textContent = show ? "Hide password" : "Show password";
        }
        if (use) {
          use.setAttribute("href", show ? "#i-eye-off" : "#i-eye");
        }
      });
    });

    all("[data-back]").forEach(function (button) {
      if (window.history.length > 1) {
        button.hidden = false;
        button.addEventListener("click", function () {
          window.history.back();
        });
      }
    });
  }

  /* ------------------------------------------------------------ slot board: tap a free slot to pick it */
  function setupSlotBoard() {
    var board = document.querySelector("[data-slot-board]");
    var select = document.querySelector("[data-slot-select]");
    if (!board) {
      return;
    }
    var tiles = all("button[data-slot]", board);
    function mark(value) {
      tiles.forEach(function (tile) {
        tile.setAttribute("aria-pressed", tile.getAttribute("data-slot") === value ? "true" : "false");
      });
    }
    if (!select) {
      tiles.forEach(function (tile) {
        tile.disabled = true;
      });
      return;
    }
    tiles.forEach(function (tile) {
      tile.addEventListener("click", function () {
        select.value = tile.getAttribute("data-slot");
        mark(select.value);
        select.dispatchEvent(new Event("change"));
        var form = select.form;
        if (form && form.scrollIntoView) {
          var reduce = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
          form.scrollIntoView({ behavior: reduce ? "auto" : "smooth", block: "nearest" });
        }
      });
    });
    select.addEventListener("change", function () {
      mark(select.value);
    });
    mark(select.value);
  }

  /* ------------------------------------------------------------ money in paise, so previews round exactly like the service */
  function paise(value) {
    var number = parseFloat(value);
    return isFinite(number) ? Math.round(number * 100) : NaN;
  }

  function rupees(amountInPaise) {
    var value = amountInPaise / 100;
    var whole = amountInPaise % 100 === 0;
    return "₹" + value.toLocaleString(INDIA, {
      minimumFractionDigits: whole ? 0 : 2,
      maximumFractionDigits: 2
    });
  }

  function output(preview, name, text) {
    var element = preview.querySelector('[data-out="' + name + '"]');
    if (element) {
      element.textContent = text;
    }
  }

  /* Auction: commission, dividend per member, what everyone pays, what the winner receives */
  function setupAuctionPreview() {
    all("form[data-auction-preview]").forEach(function (form) {
      var preview = form.querySelector("[data-preview]");
      var bidInput = form.querySelector("[name=bidAmount]");
      if (!preview || !bidInput) {
        return;
      }
      var value = paise(form.getAttribute("data-value"));
      var percent = parseFloat(form.getAttribute("data-commission"));
      var members = parseInt(form.getAttribute("data-members"), 10);
      var installment = paise(form.getAttribute("data-installment"));
      function update() {
        var bid = paise(bidInput.value);
        if (!isFinite(bid) || bid < 0 || bid > value || !(members > 0)) {
          preview.hidden = true;
          return;
        }
        var commission = Math.round(value * percent / 100);
        var surplus = Math.max(bid - commission, 0);
        var dividend = Math.round(surplus / members);
        var payable = Math.max(installment - dividend, 0);
        output(preview, "commission", rupees(commission));
        output(preview, "dividend", rupees(dividend));
        output(preview, "payable", rupees(payable));
        output(preview, "prize", rupees(value - bid));
        preview.hidden = false;
      }
      bidInput.addEventListener("input", update);
      update();
    });
  }

  /* Loan: cash handed over, daily installment and last collection day (leave days skipped) */
  function isLeaveDay(date, leave) {
    var day = date.getUTCDay();
    return (leave === "SUNDAY" && day === 0) || (leave === "WEEKEND" && (day === 0 || day === 6));
  }

  function lastCollection(startIso, installments, leave) {
    var parts = startIso.split("-");
    var date = new Date(Date.UTC(+parts[0], +parts[1] - 1, +parts[2]));
    var counted = 0;
    for (var guard = 0; guard < 20000; guard++) {
      if (!isLeaveDay(date, leave)) {
        counted++;
        if (counted === installments) {
          return date;
        }
      }
      date.setUTCDate(date.getUTCDate() + 1);
    }
    return null;
  }

  function setupLoanPreview() {
    all("form[data-loan-preview]").forEach(function (form) {
      var preview = form.querySelector("[data-preview]");
      if (!preview) {
        return;
      }
      var principalInput = form.querySelector("[name=principalAmount]");
      var interestInput = form.querySelector("[name=interestDeduction]");
      var tenureInput = form.querySelector("[name=tenureDays]");
      var startInput = form.querySelector("[name=startDate]");
      function leave() {
        var checked = form.querySelector("[name=leaveType]:checked");
        return checked ? checked.value : "NONE";
      }
      function update() {
        var principal = paise(principalInput.value);
        var interest = paise(interestInput.value || "0");
        var tenure = parseInt(tenureInput.value, 10);
        if (!(principal > 0) || !(interest >= 0) || interest >= principal || !(tenure >= 1) || tenure > 3650) {
          preview.hidden = true;
          return;
        }
        output(preview, "disbursed", rupees(principal - interest));
        output(preview, "daily", rupees(Math.round(principal / tenure)));
        var end = startInput.value ? lastCollection(startInput.value, tenure, leave()) : null;
        output(preview, "endDate", end ? end.toLocaleDateString(INDIA, {
          day: "numeric", month: "short", year: "numeric", timeZone: "UTC"
        }) : "Choose the first collection date");
        preview.hidden = false;
      }
      all("input", form).forEach(function (input) {
        input.addEventListener("input", update);
        input.addEventListener("change", update);
      });
      update();
    });
  }

  /* New scheme: commission per month and whether the installments add up to the chit value */
  function setupSchemePreview() {
    all("form[data-scheme-preview]").forEach(function (form) {
      var preview = form.querySelector("[data-preview]");
      if (!preview) {
        return;
      }
      function field(name) {
        return form.querySelector("[name=" + name + "]");
      }
      function update() {
        var value = paise(field("totalValue").value);
        var installment = paise(field("monthlyInstallment").value);
        var members = parseInt(field("totalMembers").value, 10);
        var months = parseInt(field("durationMonths").value, 10);
        var percent = parseFloat(field("commissionPercentage").value);
        var start = field("startDate").value;
        if (!(value > 0) || !(installment > 0) || !(members >= 2) || !(months >= 1) || !(percent >= 0)) {
          preview.hidden = true;
          return;
        }
        var pool = installment * members;
        output(preview, "commission", rupees(Math.round(value * percent / 100)));
        output(preview, "pool", rupees(pool));
        if (start) {
          var parts = start.split("-");
          var last = new Date(Date.UTC(+parts[0], +parts[1] - 1 + months - 1, 1));
          output(preview, "lastMonth", last.toLocaleDateString(INDIA, { month: "short", year: "numeric", timeZone: "UTC" }));
        }
        output(preview, "check", pool === value
          ? "Installments from every member add up to the chit value."
          : "Installments from every member add up to " + rupees(pool) + ", not the chit value of " + rupees(value) + ". Check the numbers if that is not intended.");
        preview.hidden = false;
      }
      all("input", form).forEach(function (input) {
        input.addEventListener("input", update);
      });
      update();
    });
  }

  onReady(function () {
    [setupLoadingScreen, setupThemeToggles, setupDrawer, setupForms, setupSlotBoard, setupAuctionPreview,
      setupLoanPreview, setupSchemePreview].forEach(function (setup) {
      try {
        setup();
      } catch (e) {
        if (window.console && window.console.error) {
          window.console.error("FinSphere: " + setup.name + " failed", e);
        }
      }
    });
  });
})();
