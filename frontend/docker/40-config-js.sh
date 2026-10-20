#!/bin/sh
set -eu
: "${API_URL:?API_URL doit etre defini (ex : https://api.exemple.fr)}"
printf 'window.APP_CONFIG = { apiUrl: "%s" };
' "$API_URL" > /usr/share/nginx/html/config.js
