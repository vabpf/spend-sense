const fs = require('fs');
const path = require('path');

let input = '';
process.stdin.on('data', chunk => {
  input += chunk;
});

process.stdin.on('end', () => {
  const logPath = path.join(__dirname, 'statusline_payload.json');
  fs.writeFileSync(logPath, JSON.stringify({
    timestamp: new Date().toISOString(),
    env: process.env,
    stdin: input
  }, null, 2));

  // Output a simple statusline so the terminal doesn't crash or show nothing
  process.stdout.write(" [Antigravity Capturing Statusline...] \n");
});
