# Stage 1: Build Angular App
FROM node:20 AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY angular.json ngsw-config.json ./
COPY tsconfig*.json ./
COPY ./src ./src
COPY ./public ./public

RUN npx ng build

# Stage 2: Run
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

RUN rm -rf ./*

COPY --from=build /app/dist/insy/browser ./

COPY nginx.conf /etc/nginx/conf.d/default.conf

CMD ["nginx", "-g", "daemon off;"]
