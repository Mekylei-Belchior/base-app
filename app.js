const http = require('http');

const server = http.createServer((req, res) => {
  if (req.url === '/health') {
    res.writeHead(200);
    return res.end('ok');
  }

  res.writeHead(200);
  res.end('Hello from k3s 🚀');
});

server.listen(3000);
