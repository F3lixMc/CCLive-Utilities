(() => {
  'use strict';
  const header = document.getElementById('demo-header');
  const trigger = document.getElementById('demo-profile-button');
  const menu = document.getElementById('demo-profile-menu');
  const title = document.getElementById('demo-page-title');
  const descriptions = {
    'CCLive': 'Deine Community. Deine Statistiken. Alles an einem Ort.',
    'Downloads': 'Modversionen, Downloads und Changelogs finden hier ihren Platz.',
    'Creator Hub': 'Streams und Creator aus der CCLive Community – hier entsteht eure Bühne.',
    'Leaderboards': 'Die Ranglisten der aktuellen und vergangenen Seasons.',
    'Spieler Statistiken': 'Spielerprofile, Fortschritt und Statistiken im Überblick.',
    'Mein Profil': 'Deine Statistiken, deine Kits und später deine Profileinstellungen.',
    'Meine Chats': 'Deine Gespräche und Gruppen – später verbunden mit der Mod.',
    'Freunde': 'Deine Freunde aus der CCLive Community.',
    'Einstellungen': 'Profil, Sichtbarkeit und Kontoeinstellungen.',
    'CCLive Control Center': 'In der echten Website bleibt dieser Bereich ausschließlich berechtigten Konten zugänglich.',
    'Logout': 'In dieser Designvorschau wird keine echte Anmeldung beendet.'
  };
  function closeMenu(restoreFocus = false) {
    menu.hidden = true;
    trigger.setAttribute('aria-expanded', 'false');
    if (restoreFocus) trigger.focus();
  }
  trigger.addEventListener('click', () => {
    const open = menu.hidden;
    menu.hidden = !open;
    trigger.setAttribute('aria-expanded', String(open));
    if (open) menu.querySelector('button').focus();
  });
  document.addEventListener('click', event => {
    if (!event.target.closest('.demo-session')) closeMenu();
    const button = event.target.closest('[data-page]');
    if (button?.dataset.page === 'CCLive Control Center') { location.href = '/control-center-demo.html'; return; }
    if (!button) return;
    const page = button.dataset.page;
    document.querySelector('.demo-hero').hidden = ['Leaderboards', 'Spieler Statistiken'].includes(page);
    document.getElementById('demo-leaderboards').hidden = page !== 'Leaderboards';
    document.getElementById('demo-player-stats').hidden = page !== 'Spieler Statistiken';
    closeMenu();
    document.getElementById('demo-eyebrow').textContent = page === 'CCLive' ? 'CCLive Utilities' : page;
    title.textContent = 'Coming Soon';
    document.getElementById('demo-description').textContent = descriptions[page];
    document.getElementById('demo-state').textContent = page === 'Logout' ? 'Demo: Dein tatsächlicher Anmeldestatus bleibt unverändert.' : 'Diese Ansicht ist eine Designvorschau ohne Kontofunktionen.';
    document.querySelectorAll('.demo-navigation button').forEach(item => {
      if (item.dataset.page === page) item.setAttribute('aria-current', 'page');
      else item.removeAttribute('aria-current');
    });
    window.scrollTo({ top: 0, behavior: 'instant' });
    const pageTitle = page === 'Leaderboards' ? document.getElementById('demo-season-title') : page === 'Spieler Statistiken' ? document.getElementById('demo-stats-season-title') : title;
    pageTitle.tabIndex = -1;
    pageTitle.focus({ preventScroll: true });
    syncSubboardHeight();
  });
  document.addEventListener('keydown', event => {
    if (event.key === 'Escape' && !menu.hidden) closeMenu(true);
  });
  document.addEventListener('focusin', event => {
    if (!event.target.closest('.demo-session')) closeMenu();
  });
  const seasons = { current: 'Season X', previous: 'Season X−1', preseason: 'Preseason X' };
  const boards = {
    kills: { title: 'Alltime Kills', label: 'Kills', values: ['950.000', '840.000', '710.000', '625.000', '514.000', '402.000', '310.000', '245.000', '182.000', '96.000'] },
    floor: { title: 'Ebene 1 Kills', label: 'Kills', values: ['81.000', '73.000', '68.000', '54.000', '42.000', '38.000', '31.000', '24.000', '19.000', '12.000'] },
    resources: { title: 'Abgebaute Ressourcen', label: 'Ressourcen', values: ['920.000', '810.000', '705.000', '601.000', '510.000', '405.000', '302.000', '201.000', '105.000', '90.000'] },
    coins: { title: 'Aktuelle Coins', label: 'Aktuelle Coins', values: ['950.000K', '840.000K', '710.000K', '625.000K', '514.000K', '402.000K', '310.000K', '245.000K', '182.000K', '96.000K'] },
    time: { title: 'Spielzeit', label: 'Zeit', values: ['320h 15m', '298h 40m', '271h 05m', '250h 12m', '219h 30m', '187h 45m', '160h 20m', '142h 10m', '110h 05m', '98h 30m'] },
    smith: { title: 'Schmiedeskill', label: 'Level / XP', values: ['100 / 950.000K', '95 / 840.000K', '90 / 710.000K', '85 / 625.000K', '80 / 514.000K', '75 / 402.000K', '70 / 310.000K', '65 / 245.000K', '60 / 182.000K', '55 / 96.000K'] }
  };
  // Demo-Reihenfolge; endgültige Freischaltfolge wird bei der Datenanbindung geprüft.
  const zones = ['Alle Farmzonen', 'Eichenwald', 'Kohle Mine', 'Kupfermine', 'Strand', 'Fichtenwald', 'Bambuswald', 'Eisenmine', 'Pilzwald', 'Dunkeleichenwald', 'Goldmine', 'Mangrovensumpf', 'Diamantmine', 'Schwefelfelder', 'Quartzmine', 'Obsidianmine', 'Karmesinwald', 'Wirrwald', 'Antike Mine', 'Echomine', 'Amethystmine'];
  const subboardIndex = { floor: 0, resources: 0 };
  function tableHead(target, columns) {
    const row = document.createElement('tr');
    columns.forEach(text => { const th = document.createElement('th'); th.scope = 'col'; th.textContent = text; row.appendChild(th); });
    target.replaceChildren(row);
  }
  function tableRow(values) {
    const row = document.createElement('tr');
    values.forEach(text => { const td = document.createElement('td'); td.textContent = text; row.appendChild(td); });
    return row;
  }
  function syncSubboardHeight() {
    const aside = document.getElementById('demo-subboards');
    const list = document.getElementById('demo-subboard-list');
    if (aside.hidden || window.innerWidth <= 600) {
      aside.style.removeProperty('height');
      list.style.removeProperty('--subboard-list-height');
      return;
    }
    const height = document.querySelector('.demo-board-content').getBoundingClientRect().height;
    if (!height) return;
    aside.style.height = height + 'px';
    const summary = aside.querySelector('summary').getBoundingClientRect().height;
    const note = document.getElementById('demo-zone-order-note');
    const noteHeight = note.hidden ? 0 : note.getBoundingClientRect().height + 12;
    list.style.setProperty('--subboard-list-height', Math.max(120, height - summary - noteHeight - 30) + 'px');
  }
  new ResizeObserver(syncSubboardHeight).observe(document.querySelector('.demo-board-content'));
  window.addEventListener('resize', syncSubboardHeight);
  function renderBoard() {
    const season = document.getElementById('demo-lb-season').value;
    const type = document.getElementById('demo-lb-type').value;
    const board = boards[type];
    const subboards = type === 'floor' ? Array.from({ length: 100 }, (_, i) => 'Ebene ' + (i + 1)) : type === 'resources' ? zones : [];
    const index = subboardIndex[type] || 0;
    const heading = type === 'floor' ? subboards[index] + ' Kills' : type === 'resources' ? (index === 0 ? 'Insgesamt abgebaute Ressourcen' : subboards[index]) : board.title;
    document.getElementById('demo-season-title').textContent = seasons[season];
    document.getElementById('demo-season-badge').textContent = season === 'current' ? 'Aktuelle Season' : 'Vergangene Season';
    document.getElementById('demo-board-title').textContent = heading;
    const panorama = document.getElementById('demo-board-panorama');
    const image = type === 'floor' && index === 0 ? '/images/floor-1.png' : type === 'resources' && subboards[index] === 'Eichenwald' ? '/images/eichenwald.png' : null;
    document.getElementById('demo-board-banner').classList.toggle('has-panorama', !!image);
    panorama.hidden = !image;
    if (image) {
      if (panorama.getAttribute('src') !== image) panorama.src = image;
    } else panorama.removeAttribute('src');

    document.getElementById('demo-mob-info').hidden = type !== 'floor';
    const drops = document.getElementById('demo-drop-info');
    drops.hidden = type !== 'floor';
    if (!drops.hidden) {
      drops.textContent = window.CCLiveProgressEffects.line(window.CCLiveDemoProgress, index + 1, window.CCLiveDemoStatueMaxima, window.CCLiveDemoCardMaxima);
      drops.title = 'Vorläufige Wikiwerte: Karten Level 5 ohne angegebenen Prestige-Bezug; Statuen möglicherweise noch vor Level 100.';
    }

    if (type === 'floor') {
      const info = window.CCLiveDemoMobFloors[String(index + 1)];
      const line = document.getElementById('demo-mob-info');
      line.replaceChildren();
      const mobLabel = document.createElement('strong'); mobLabel.textContent = 'Mobart: ';
      const hpLabel = document.createElement('strong'); hpLabel.textContent = 'HP: ';
      const separator = document.createElement('span'); separator.textContent = '|'; separator.setAttribute('aria-hidden', 'true');
      line.append(mobLabel, document.createTextNode(info.mobs.join(' / ') + ' '), separator, hpLabel, document.createTextNode(info.hp.toLocaleString('de-DE')));
    }
    document.getElementById('demo-ranking-caption').textContent = heading + ' – Beispieldaten';
    const columns = ['Rang', 'Spielername', ...(type === 'smith' ? ['Level', 'XP'] : [board.label])];
    tableHead(document.getElementById('demo-own-head'), columns);
    tableHead(document.getElementById('demo-ranking-head'), columns);
    document.getElementById('demo-own-body').replaceChildren(tableRow(['—', 'firestarter03', ...(type === 'smith' ? ['—', '—'] : ['—'])]));
    const body = document.getElementById('demo-ranking-body');
    body.replaceChildren();
    board.values.forEach((value, i) => {
      body.appendChild(tableRow([String(i + 1).padStart(2, '0'), 'Beispielspieler_' + (i + 1), ...(type === 'smith' ? value.split(' / ') : [value])]));
    });
    document.querySelectorAll('.demo-ranking').forEach(table => { table.classList.toggle('has-xp', type === 'smith'); });
    document.getElementById('demo-board-layout').classList.toggle('has-subboards', subboards.length > 0);
    document.getElementById('demo-subboards').hidden = subboards.length === 0;
    document.getElementById('demo-zone-order-note').hidden = type !== 'resources';
    document.getElementById('demo-subboards-title').textContent = type === 'floor' ? 'Ebene auswählen' : 'Farmzone auswählen';
    const list = document.getElementById('demo-subboard-list');
    list.replaceChildren();
    subboards.forEach((name, i) => {
      const button = document.createElement('button'); button.type = 'button'; button.textContent = name; button.dataset.subboard = i;
      if (i === index) button.setAttribute('aria-current', 'true');
      list.appendChild(button);
    });
    for (const [id, delta] of [['demo-board-prev', -1], ['demo-board-next', 1]]) {
      const button = document.getElementById(id);
      button.hidden = !subboards.length;
      button.disabled = index + delta < 0 || index + delta >= subboards.length;
      button.setAttribute('aria-label', type === 'floor' ? (delta < 0 ? 'Vorherige Ebene' : 'Nächste Ebene') : (delta < 0 ? 'Vorherige Farmzone' : 'Nächste Farmzone'));
    }
    syncSubboardHeight();
    const selected = list.querySelector('[aria-current]');
    if (selected) list.scrollTop = selected.offsetTop - list.clientHeight / 2 + selected.offsetHeight / 2;
  }
  document.getElementById('demo-lb-season').addEventListener('change', renderBoard);
  document.getElementById('demo-lb-type').addEventListener('change', renderBoard);
  document.getElementById('demo-subboard-list').addEventListener('click', event => {
    const button = event.target.closest('[data-subboard]'); if (!button) return;
    subboardIndex[document.getElementById('demo-lb-type').value] = Number(button.dataset.subboard);
    renderBoard();
    document.querySelector('[data-subboard="' + button.dataset.subboard + '"]').focus({ preventScroll: true });
  });
  for (const [id, delta] of [['demo-board-prev', -1], ['demo-board-next', 1]]) {
    document.getElementById(id).addEventListener('click', () => {
      const type = document.getElementById('demo-lb-type').value;
      if (!['floor', 'resources'].includes(type)) return;
      const last = type === 'floor' ? 99 : zones.length - 1;
      subboardIndex[type] = Math.max(0, Math.min(last, subboardIndex[type] + delta));
      renderBoard();
    });
  }
  const demoPlayers = ['firestarter03', ...Array.from({ length: 18 }, (_, i) => 'Beispielspieler_' + (i + 1))];
  let selectedDemoPlayer = demoPlayers[0];
  function demoHead(target, name) {
    target.querySelector('img')?.remove();
    const fallback = target.querySelector('svg');
    if (fallback) fallback.style.display = '';
    if (name !== 'firestarter03') return;
    const image = document.createElement('img'); image.alt = ''; image.src = 'images/firestarter03-head.png';
    image.addEventListener('load', () => {
      if (fallback) fallback.style.display = 'none';
      else if (target.firstChild?.nodeType === Node.TEXT_NODE) target.firstChild.textContent = '';
    });
    image.addEventListener('error', () => image.remove());
    target.appendChild(image);
  }
  demoHead(document.querySelector('.demo-user-icon'), 'firestarter03');
  function renderPlayerList() {
    const query = document.getElementById('demo-player-search').value.trim().toLocaleLowerCase('de-DE');
    const matches = demoPlayers.filter(name => name.toLocaleLowerCase('de-DE').includes(query));
    const list = document.getElementById('demo-player-list');
    list.replaceChildren();
    document.getElementById('demo-player-count').textContent = query ? matches.length + ' / ' + demoPlayers.length : String(demoPlayers.length);
    document.getElementById('demo-player-search-empty').hidden = matches.length > 0;
    matches.forEach(name => {
      const button = document.createElement('button'); button.type = 'button'; button.dataset.player = name;
      const avatar = document.createElement('span'); avatar.className = 'demo-list-avatar'; avatar.setAttribute('aria-hidden', 'true'); avatar.textContent = name.slice(0, 1).toUpperCase(); demoHead(avatar, name);
      const text = document.createElement('span'); text.textContent = name;
      button.append(avatar, text);
      if (name === selectedDemoPlayer) button.setAttribute('aria-current', 'true');
      list.appendChild(button);
    });
  }
  document.getElementById('demo-player-search').addEventListener('input', renderPlayerList);
  document.getElementById('demo-player-list').addEventListener('click', event => {
    const button = event.target.closest('[data-player]'); if (!button) return;
    selectedDemoPlayer = button.dataset.player;
    document.getElementById('demo-stats-name').textContent = selectedDemoPlayer;
    demoHead(document.querySelector('.demo-player-avatar'), selectedDemoPlayer);
    document.querySelectorAll('#demo-player-list button').forEach(item => {
      if (item.dataset.player === selectedDemoPlayer) item.setAttribute('aria-current', 'true');
      else item.removeAttribute('aria-current');
    });
  });
  const progressDemo = {
    cards: Object.entries(window.CCLiveDemoProgress.karten).map(([name, entry], i) => ({ name, aliases: window.CCLiveDemoCardMaxima?.[name]?.wiki_name || '', floors: entry.floor, prestige: i % 4, level: i % 5 + 1 })),
    statues: Object.entries(window.CCLiveDemoProgress.statuen).map(([name, entry], i) => ({ name, aliases: window.CCLiveDemoStatueMaxima?.[name]?.wiki_name || '', floors: entry.floor, level: i * 7 % 100 + 1 }))
  };
  function matchesProgress(entry, input, kind) {
    let query = input.trim().toLocaleLowerCase('de-DE');
    let matches = true;
    query = query.replace(/\b(ebene|prestige|level)\s*:?\s*(\d+)\b/g, (_, field, number) => {
      const value = Number(number);
      if (field === 'ebene') matches = matches && entry.floors.includes(value);
      else if (field === 'prestige') matches = matches && kind === 'cards' && entry.prestige === value;
      else matches = matches && entry.level === value;
      return ' ';
    });
    if (!matches) return false;
    const names = (entry.name + ' ' + entry.aliases).toLocaleLowerCase('de-DE');
    const numbers = [...entry.floors, entry.level, ...(kind === 'cards' ? [entry.prestige] : [])];
    return query.split(/\s+/).filter(Boolean).every(term => /^\d+$/.test(term) ? numbers.includes(Number(term)) : names.includes(term));
  }
  function renderProgress(kind) {
    const all = progressDemo[kind];
    const input = document.getElementById('demo-' + kind + '-search').value;
    const entries = all.filter(entry => matchesProgress(entry, input, kind)).sort((a,b) => Math.min(...a.floors) - Math.min(...b.floors) || a.name.localeCompare(b.name, 'de'));
    document.getElementById('demo-' + kind + '-count').textContent = input.trim() ? entries.length + ' / ' + all.length : String(all.length);
    const body = document.getElementById('demo-' + kind + '-body'); body.replaceChildren();
    if (!entries.length) {
      const row = document.createElement('tr'), cell = document.createElement('td'); cell.colSpan = kind === 'cards' ? 4 : 3; cell.textContent = 'Keine passenden Einträge.'; row.appendChild(cell); body.appendChild(row); return;
    }
    entries.forEach(entry => {
      const row = document.createElement('tr');
      [entry.name, entry.floors.join(', '), ...(kind === 'cards' ? [entry.prestige] : []), entry.level].forEach(value => {
        const cell = document.createElement('td'); cell.textContent = value; row.appendChild(cell);
      });
      body.appendChild(row);
    });
  }
  for (const kind of ['cards', 'statues']) {
    document.getElementById('demo-' + kind + '-search').addEventListener('input', () => renderProgress(kind));
    renderProgress(kind);
  }
  function renderStats() {
    const season = document.getElementById('demo-stats-season').value;
    document.getElementById('demo-stats-season-title').textContent = seasons[season];
    document.getElementById('demo-stats-season-badge').textContent = season === 'current' ? 'Aktuelle Season' : 'Vergangene Season';
    document.getElementById('demo-stats-name').textContent = selectedDemoPlayer;
    demoHead(document.querySelector('.demo-player-avatar'), selectedDemoPlayer);
  }
  document.getElementById('demo-stats-season').addEventListener('change', renderStats);
  renderPlayerList();
  renderBoard();
  renderStats();
  const syncScroll = () => {
    // Separate thresholds prevent flickering around the transition point.
    if (window.scrollY > 70) header.classList.add('is-compact');
    else if (window.scrollY < 25) header.classList.remove('is-compact');
  };
  window.addEventListener('scroll', syncScroll, { passive: true });
  syncScroll();
  document.getElementById('demo-back-top').addEventListener('click', () => {
    window.scrollTo({ top: 0, behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'instant' : 'smooth' });
  });
})();
