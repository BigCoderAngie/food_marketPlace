// Countdown for "seller has X min to accept" (SRS change #3). Elements: <span data-deadline="epochMillis">.
(function () {
  function tick() {
    document.querySelectorAll('[data-deadline]').forEach(function (el) {
      var left = Math.max(0, Number(el.dataset.deadline) - Date.now());
      var m = Math.floor(left / 60000), s = Math.floor((left % 60000) / 1000);
      el.textContent = left > 0 ? m + ':' + String(s).padStart(2, '0') : 'time is up';
      el.classList.toggle('urgent', left < 120000);
      if (left === 0 && el.dataset.reload && !el.dataset.done) {
        el.dataset.done = '1';
        setTimeout(function () { location.reload(); }, 35000);
      }
    });
  }
  tick();
  setInterval(tick, 1000);

  // Show pickup or delivery fields on checkout.
  document.querySelectorAll('input[name=fulfilment]').forEach(function (r) {
    function sync() {
      var v = document.querySelector('input[name=fulfilment]:checked');
      document.querySelectorAll('[data-for]').forEach(function (box) {
        box.classList.toggle('hidden', !v || box.dataset.for !== v.value);
      });
      var fee = document.getElementById('delivery-fee');
      if (fee) fee.classList.toggle('hidden', !v || v.value !== 'DELIVERY');
    }
    r.addEventListener('change', sync);
    sync();
  });

  // Ask before destructive actions.
  document.querySelectorAll('[data-confirm]').forEach(function (el) {
    el.addEventListener('click', function (e) { if (!confirm(el.dataset.confirm)) e.preventDefault(); });
  });

  // Active seller/customer orders refresh themselves so new orders show up.
  // Never while the person is typing into a form.
  var auto = document.querySelector('[data-autorefresh]');
  function busy() {
    var a = document.activeElement;
    if (a && /INPUT|TEXTAREA|SELECT/.test(a.tagName)) return true;
    return Array.prototype.some.call(document.querySelectorAll('input[type=text], textarea'), function (i) { return i.value; })
      || Array.prototype.some.call(document.querySelectorAll('details'), function (d) { return d.open; });
  }
  function refresh() { if (busy()) setTimeout(refresh, 15000); else location.reload(); }
  if (auto) setTimeout(refresh, Number(auto.dataset.autorefresh) * 1000);
})();
