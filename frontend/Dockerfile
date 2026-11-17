# Frontend estático servido por nginx. nginx también hace de proxy hacia los microservicios,
# así el navegador solo habla con un origen (http://localhost:8080) y no hay problemas de CORS.
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY index.html /usr/share/nginx/html/
COPY css /usr/share/nginx/html/css
COPY js /usr/share/nginx/html/js
EXPOSE 80
