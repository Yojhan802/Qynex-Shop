# Sin build step: es HTML/CSS/JS plano, así que solo se copia tal cual a nginx.
FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY . /usr/share/nginx/html

EXPOSE 80
