document.documentElement.classList.add('js-enabled');

const THEME_KEY = 'theme';

const applyTheme = (theme) => {
  document.documentElement.setAttribute('data-theme', theme);
};

const getStoredTheme = () => localStorage.getItem(THEME_KEY);

const getPreferredTheme = () => (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');

const setInitialTheme = () => {
  const storedTheme = getStoredTheme();
  applyTheme(storedTheme || getPreferredTheme());
};

const updateThemeToggleIcon = (button) => {
  if (!button) return;
  const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
  button.textContent = isDark ? '☀️' : '🌙';
  button.setAttribute('aria-label', isDark ? 'Cambiar a tema claro' : 'Cambiar a tema oscuro');
};

setInitialTheme();

document.addEventListener('DOMContentLoaded', () => {
  const toggle = document.querySelector('.nav-toggle');
  const nav = document.querySelector('#main-menu');
  const themeToggle = document.getElementById('theme-toggle');

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

  if (themeToggle) {
    updateThemeToggleIcon(themeToggle);
    themeToggle.addEventListener('click', () => {
      const currentTheme = document.documentElement.getAttribute('data-theme') === 'dark' ? 'dark' : 'light';
      const nextTheme = currentTheme === 'dark' ? 'light' : 'dark';
      applyTheme(nextTheme);
      localStorage.setItem(THEME_KEY, nextTheme);
      updateThemeToggleIcon(themeToggle);
    });
  }
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

window.addEventListener('DOMContentLoaded', () => {
  const moneyInputs = document.querySelectorAll('input[data-money-input]');
  if (moneyInputs.length === 0) {
    return;
  }

  const formatter = new Intl.NumberFormat('es-CL');
  const moneyForms = new Set();

  const formatMoneyValue = (input) => {
    const digitsOnly = input.value.replace(/\D/g, '');
    const normalized = digitsOnly.replace(/^0+/, '');

    if (!normalized) {
      input.value = '';
      input.dataset.rawValue = '';
      return;
    }

    const numericValue = Number(normalized);
    if (Number.isNaN(numericValue)) {
      input.value = '';
      input.dataset.rawValue = '';
      return;
    }

    input.dataset.rawValue = normalized;
    input.value = formatter.format(numericValue);
  };

  moneyInputs.forEach((input) => {
    if (input.form) {
      moneyForms.add(input.form);
    }

    if (input.value) {
      formatMoneyValue(input);
    }

    input.addEventListener('input', () => formatMoneyValue(input));
  });

  moneyForms.forEach((form) => {
    form.addEventListener('submit', () => {
      const formMoneyInputs = form.querySelectorAll('input[data-money-input]');
      formMoneyInputs.forEach((input) => {
        input.value = input.dataset.rawValue || '';
      });
    });
  });
});
