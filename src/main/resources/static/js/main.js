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

window.addEventListener('DOMContentLoaded', () => {
  const forms = document.querySelectorAll('form[data-disable-on-submit="true"]');
  forms.forEach((form) => {
    form.addEventListener('submit', () => {
      const submitButton = form.querySelector('button[type="submit"], input[type="submit"]');
      if (submitButton) {
        submitButton.disabled = true;
        const originalText = submitButton.dataset.originalText || submitButton.textContent;
        submitButton.dataset.originalText = originalText;
        submitButton.textContent = submitButton.dataset.loadingText || 'Procesando...';
      }
    });
  });
});
