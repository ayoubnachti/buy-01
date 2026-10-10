# Production image: build the Angular app, then serve the static files with nginx.
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci --no-audit --no-fund
COPY . .
# Angular writes to dist/<project>/browser; move it to a fixed path
RUN npx ng build && mv dist/*/browser /app/out

FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/out /usr/share/nginx/html