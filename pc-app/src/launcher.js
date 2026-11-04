'use strict';

const fs = require('fs');
const path = require('path');
const { spawn } = require('child_process');

function findBrowserExecutable() {
  const candidates = [];

  if (process.platform === 'win32') {
    const pf = process.env['ProgramFiles'] || 'C:\\Program Files';
    const pfx86 = process.env['ProgramFiles(x86)'] || 'C:\\Program Files (x86)';
    const localAppData = process.env['LOCALAPPDATA'] || '';

    candidates.push(
      path.join(pfx86, 'Microsoft', 'Edge', 'Application', 'msedge.exe'),
      path.join(pf, 'Microsoft', 'Edge', 'Application', 'msedge.exe'),
      path.join(localAppData, 'Microsoft', 'Edge', 'Application', 'msedge.exe'),
      path.join(pf, 'Google', 'Chrome', 'Application', 'chrome.exe'),
      path.join(pfx86, 'Google', 'Chrome', 'Application', 'chrome.exe'),
      path.join(localAppData, 'Google', 'Chrome', 'Application', 'chrome.exe')
    );
  } else if (process.platform === 'darwin') {
    candidates.push(
      '/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge',
      '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'
    );
  } else {
    candidates.push(
      '/usr/bin/microsoft-edge',
      '/usr/bin/google-chrome',
      '/usr/bin/chromium-browser',
      '/usr/bin/chromium'
    );
  }

  for (const exePath of candidates) {
    if (exePath && fs.existsSync(exePath)) {
      return exePath;
    }
  }
  return null;
}

function openNativeAppWindow(url) {
  const exePath = findBrowserExecutable();

  if (exePath) {
    const args = [
      `--app=${url}`,
      '--window-size=1380,880',
      '--disable-extensions',
      '--no-first-run',
      '--no-default-browser-check'
    ];
    const child = spawn(exePath, args, {
      detached: true,
      stdio: 'ignore'
    });
    child.unref();
    return { modo: 'NATIVO_APP', ejecutable: exePath, url };
  }

  if (process.platform === 'win32') {
    const child = spawn('cmd.exe', ['/c', 'start', '', 'msedge', `--app=${url}`], {
      detached: true,
      stdio: 'ignore'
    });
    child.unref();
    return { modo: 'CMD_MSEDGE_APP', ejecutable: 'msedge', url };
  }

  return { modo: 'SOLO_SERVIDOR', ejecutable: null, url };
}

module.exports = {
  findBrowserExecutable,
  openNativeAppWindow
};
