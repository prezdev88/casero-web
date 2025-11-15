document.documentElement.classList.add('js-enabled');

document.addEventListener('DOMContentLoaded', () => {
  const toggle = document.querySelector('.nav-toggle');
  const nav = document.querySelector('#main-menu');

  if (!toggle || !nav) {
    return;
  }

  const setNavState = (isOpen) => {
    toggle.setAttribute('aria-expanded', String(isOpen));
    nav.classList.toggle('is-open', isOpen);
  };

  toggle.addEventListener('click', () => {
    const isOpen = toggle.getAttribute('aria-expanded') === 'true';
    setNavState(!isOpen);
  });

  nav.addEventListener('click', (event) => {
    if (event.target instanceof HTMLElement && event.target.tagName === 'A') {
      setNavState(false);
    }
  });
});

if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    const swUrl = document.documentElement.dataset.swUrl || '/sw.js';
    navigator.serviceWorker.register(swUrl).catch((error) => {
      console.error('Service worker registration failed', error);
    });
  });
}
