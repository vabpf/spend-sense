#!/usr/bin/env node

const { spawn } = require('child_process');
const fs = require('fs');
const path = require('path');

let inputBuffer = '';

// Capture stdin from the Antigravity CLI statusline piping
process.stdin.on('data', (chunk) => {
  inputBuffer += chunk.toString('utf8');
});

process.stdin.on('end', () => {
  let mappedData = {};

  try {
    const rawData = JSON.parse(inputBuffer);

    // Save the raw payload for analysis/debugging
    const debugPath = path.join(__dirname, 'statusline_payload.json');
    fs.writeFileSync(debugPath, JSON.stringify({
      timestamp: new Date().toISOString(),
      payload: rawData
    }, null, 2));

    // Map Antigravity CLI status fields to Claude Code's schema:
    // ccstatusline looks for model.display_name, cwd, tokens, etc.
    mappedData = {
      model: {
        display_name: rawData.modelName || rawData.model?.name || rawData.model || 'Gemini 3.5 Flash',
      },
      cwd: rawData.cwd || process.cwd(),
      session_id: rawData.sessionId || rawData.session_id || 'agy-session',
      workspace: {
        project_path: rawData.workspacePath || rawData.cwd || process.cwd()
      },
      // Preserve any existing fields that match
      ...rawData
    };
  } catch (err) {
    // If the input wasn't valid JSON, fallback to standard defaults
    mappedData = {
      model: {
        display_name: 'Antigravity CLI'
      },
      cwd: process.cwd(),
      session_id: 'agy-session',
      workspace: {
        project_path: process.cwd()
      }
    };
  }

  // Spawn ccstatusline and pipe the mapped JSON into it
  const ccstatusline = spawn('npx', ['-y', 'ccstatusline@latest'], {
    stdio: ['pipe', 'inherit', 'inherit'],
    shell: true
  });

  ccstatusline.stdin.write(JSON.stringify(mappedData));
  ccstatusline.stdin.end();
});
