FROM node:20-alpine

WORKDIR /app

# Melhor uso de cache
COPY package*.json ./
RUN npm install --production || true

COPY . .

EXPOSE 3000

CMD ["node", "app.js"]
